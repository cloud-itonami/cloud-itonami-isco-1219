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

## License

AGPL-3.0-or-later.
