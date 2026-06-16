# kotodama 言霊

The generic **functional-organism runtime** on the kotoba-clj stack. Domain logic
is *injected*; kotodama only provides the reusable machinery that turns any actor
into a living organism persisted as-of on the kotoba Datom log.

This is the runtime extracted from `etzhayyim/kototama` (per the ADR superseding
2606131645): the **UNSPSC-specific** implementation (capability / taxonomy / fleet
/ data) moves to a concrete actor in `etzhayyim/root` `20-actors/unspsc`; the
**generic** organism machinery lives here, reusable by any actor family.

## Layers

```
kotoba (Rust substrate: Datom / Pregel / WASM host)
  ▲ kotoba-db XRPC (langchain-clj) / checkpointer (langgraph-clj)
kotodama (THIS: generic organism runtime — life + organism graph + ReAct)
  ▲ inject :validate / :emit / capability
concrete actor (e.g. etzhayyim/root 20-actors/unspsc) — domain data + logic
```

## Namespaces

- `kotodama.life` — joucho (mood) fold, heartbeat cadence, Stage-D prior-consensus
  (`prior-consensus` / `prior-shortcut?`). Pure, generic.
- `kotodama.organism` — `(actor {:taxon :validate :emit :model :compile-opts})` builds a
  `validate → reason → emit` StateGraph; `(run actor input {:thread-id ..})` invokes it.
  Murakumo-grounded reasoning (fail-open template); opt-in prior-consensus shortcut.
- `kotodama.react` — `capability-tools` + `react-actor` (a genuine ReAct loop over the
  actor's capability, on langgraph-clj's create-react-agent).

## Use

```clojure
(require '[kotodama.organism :as org])
(def a (org/actor {:taxon  {:code "10101500" :title "Live Animal"}
                   :validate (fn [taxon input] (my-capability/run taxon input))
                   :emit     (fn [taxon verdict reasoning] {:code (:code taxon) :did ... :ok (:ok verdict) ...})
                   ;; :model    (org/murakumo-model host-caps)   ; nil → deterministic template
                   ;; :compile-opts {:checkpointer kotoba-checkpointer}  ; → kotoba Datom persistence
                   }))
(org/run a {:quantity 12 :unit "head"} {:thread-id "unspsc-10101500"})
```

```bash
clojure -X:test        # 7 tests / 17 assertions, domain-free mock actor
```

Apache-2.0. Inference stays Murakumo-only at runtime (ADR-2605215000).
