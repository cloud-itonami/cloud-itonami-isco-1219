(ns business-admin.governor
  "BusinessAdminGovernor — the independent safety/traceability layer for
  the ISCO-08 1219 independent business-administration-management actor.
  Wired as its own `:govern` node in `business-admin.actor`'s StateGraph,
  downstream of `:advise` — the Advisor has no notion of client
  provenance or payroll/regulatory-filing risk, so this MUST be a
  separate system able to reject a proposal (itonami actor pattern, per
  ADR-2607011000 / CLAUDE.md Actors section).

  `check` is a pure function of (request, context, proposal, store) ->
  verdict; it never mutates the store. The StateGraph's `:decide` node
  routes on the verdict:
    :hard? true                → :hold  (irreversible, no write)
    :escalate? true            → :request-approval (interrupt-before)
    otherwise                  → :commit

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance  — the request's client must be registered.
    2. no-actuation        — proposal :effect must be :propose.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off, per the
  README robotics-premise: payroll processing and regulatory filing
  submission always require human sign-off):
    3. :op :process-payroll.
    4. :op :submit-regulatory-filing.
    5. low confidence (< `confidence-floor`)."
  (:require [business-admin.store :as store]))

(def confidence-floor 0.6)
(def ^:private escalating-ops #{:process-payroll :submit-regulatory-filing})

(defn- hard-violations [{:keys [proposal]} client-record]
  (cond-> []
    (nil? client-record)
    (conj {:rule :no-client :detail "未登録 client"})

    (not= :propose (:effect proposal))
    (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `business-admin.store/Store`. Returns
  `{:ok? bool :violations [...] :confidence n :hard? bool :escalate? bool}`."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        hard (hard-violations {:proposal proposal} client-record)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        risky-op? (contains? escalating-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not risky-op?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? risky-op?))}))
