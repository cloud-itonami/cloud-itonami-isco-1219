# cloud-itonami-isco-1219

Open Occupation Blueprint for **ISCO-08 1219**: Business Services and Administration Managers Not Elsewhere Classified.

This repository designs a forkable OSS business for an independent business-administration manager serving small clients: a document-handling robot performs record scanning and compliance walkthroughs under a governor-gated actor, so the practice keeps its own administration and compliance records instead of renting a closed back-office SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a document-handling robot performs record scanning, filing and compliance-checklist walkthroughs under an actor that proposes
actions and an independent **Business Admin Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
payroll processing, or regulatory filing submission) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
administration scope + compliance checklist + delegation plan
        |
        v
Admin Management Advisor -> Business Admin Governor -> coordinate/review, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `1219`). Required capabilities:

- :robotics
- :identity
- :forms
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation (`:maturity :implemented`)

Full itonami Actor pattern (per ADR-2607011000 / CLAUDE.md's Actors
section, alongside `cloud-itonami-isco-6130`, `-8160`, `-2166`, `-2641`,
`-2651`, `-2652` and `-2654`): a real
[`kotoba-lang/langgraph`](https://github.com/kotoba-lang/langgraph)
`StateGraph`, with the Advisor and Governor as distinct graph nodes and
human-in-the-loop interrupt/resume via checkpointing.

```text
:intake -> :advise -> :govern -> :decide -+-> :commit            (:ok? true)
                                           +-> :request-approval   (:escalate? true, interrupt-before)
                                           +-> :hold               (:hard? true)
```

- `src/business_admin/store.cljc` — `Store` protocol + `MemStore`:
  registered clients, committed records, an append-only audit ledger.
- `src/business_admin/advisor.cljc` — `Advisor` protocol; `mock-advisor`
  (deterministic, default) proposes an administration operation from a
  request; `llm-advisor` wraps a `langchain.model/ChatModel` — either way
  the advisor only ever produces a `:propose`-effect proposal, never a
  committed record, and LLM parse failures always yield `confidence 0.0`
  (forces escalation, never fabricated confidence).
- `src/business_admin/governor.cljc` — `BusinessAdminGovernor/check`: a
  pure function, wired as its own `:govern` node. Hard invariants
  (unregistered client, a proposal whose `:effect` isn't `:propose`)
  always route to `:hold`. Escalation invariants (`:process-payroll`,
  `:submit-regulatory-filing`, or low advisor confidence) always route to
  `:request-approval` — an `interrupt-before` node that the graph
  checkpoints and only resumes on explicit human approval
  (`actor/approve!`), matching the README's robotics-premise statement
  that payroll processing and regulatory filing submission always
  require human sign-off.
- `src/business_admin/actor.cljc` — `build-graph`, `run-request!`,
  `approve!`: the `langgraph.graph/state-graph` wiring itself.

```bash
clojure -M:test
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation).

## License

AGPL-3.0-or-later.
