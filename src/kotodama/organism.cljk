(ns kotodama.organism
  "The generic functional-organism runtime — ONE validate→reason→emit StateGraph
  template that drives ANY actor family. Domain logic is INJECTED:

    (actor {:taxon T :validate (fn [T input] -> verdict) :emit (fn [T verdict reasoning] -> result)})

  - validate : the actor's own domain check (real work) — or a prior-consensus shortcut
  - reason   : Murakumo-grounded one-line reasoning (fail-open to a template)
  - emit     : the actor's runtime-contract result map

  State persists as-of on the kotoba Datom log via the langgraph checkpointer
  (pass {:checkpointer cp} in :compile-opts), giving each actor a lived history.
  Runtime contract: (run actor input opts) -> the :result map."
  (:require [kotoba.lang.text :as str]
            [langgraph.graph :as g]
            [langchain.model :as lcm]
            [langchain.message :as msg]
            [kotodama.life :as life]))

;; ── reasoning (Murakumo-only, fail-open; generic over taxon/verdict shape) ──

(defn template-reasoning
  "Deterministic, grounded fallback line (no LLM). Tolerant of missing keys."
  [taxon verdict]
  (str (or (:title taxon) (:id taxon) "actor") " (" (:code taxon) "): "
       (when-let [d (:domain verdict)] (str (name d) " — "))
       (if (:ok verdict) "verdict OK" "verdict NOT-OK")
       (when (seq (:missing verdict))
         (str "; missing " (str/join ", " (:missing verdict))))))

(defn reason-text
  "Murakumo-grounded reasoning about the verdict; fail-open to template when no
  model is wired or the call throws (Charter: Murakumo-only, graceful fallback)."
  [model taxon verdict]
  (if (nil? model)
    (template-reasoning taxon verdict)
    (try
      (let [resp (lcm/-generate
                  model
                  [(msg/system "You are a commodity/entity actor. Reply with ONE concise sentence.")
                   (msg/user (str "Actor " (:code taxon) " \"" (or (:title taxon) (:id taxon)) "\". "
                                  "ok=" (:ok verdict) " missing=" (vec (:missing verdict))
                                  ". Summarize the verdict."))]
                  {})
            t (msg/text resp)]
        (if (str/blank? t) (template-reasoning taxon verdict) t))
      (catch #?(:clj Exception :cljs :default) _
        (template-reasoning taxon verdict)))))

;; ── default emit (generic): identity-ish merge; concrete actors override ────

(defn default-emit
  "Generic result: the verdict plus the taxon's :code/:id and the reasoning."
  [taxon verdict reasoning]
  (cond-> (assoc verdict :reasoning reasoning)
    (:code taxon) (assoc :code (:code taxon))
    (:id taxon)   (assoc :id (:id taxon))))

(def ^:private shortcut-verdict
  {:ok true :missing [] :checks [] :domain :prior-shortcut :shortcut true})

;; ── actor builder ───────────────────────────────────────────────────────────

(defn actor
  "Compiles a functional organism graph.
   opts:
     :taxon            the actor's data record (passed to validate/emit/reason)
     :validate         (fn [taxon input] -> verdict {:ok :missing :checks :domain ...})  REQUIRED
     :emit             (fn [taxon verdict reasoning] -> result map)  [default: default-emit]
     :model            optional ChatModel (Murakumo); nil -> template reasoning
     :prior-shortcut?  predicate on prior-consensus  [default: life/prior-shortcut?]
     :compile-opts     forwarded to langgraph (e.g. {:checkpointer cp})"
  [{:keys [taxon validate emit model prior-shortcut? compile-opts]
    :or {emit default-emit prior-shortcut? life/prior-shortcut? compile-opts {}}}]
  (when-not (fn? validate)
    (throw (ex-info "kotodama.organism/actor requires a :validate fn" {})))
  (let [validate-node
        (fn [{:keys [taxon input prior-consensus]}]
          (if (and (some? prior-consensus) (prior-shortcut? prior-consensus))
            {:verdict shortcut-verdict :log [(str (:code taxon) ":validate:prior_shortcut")]}
            (let [verdict (validate taxon input)]
              {:verdict verdict
               :log [(str (:code taxon) ":validate ok=" (:ok verdict)
                          (when (seq (:missing verdict)) (str " missing=" (:missing verdict))))]})))
        reason-node
        (fn [{:keys [taxon verdict]}]
          {:reasoning (reason-text model taxon verdict) :log [(str (:code taxon) ":reason")]})
        emit-node
        (fn [{:keys [taxon verdict reasoning]}]
          {:result (cond-> (emit taxon verdict reasoning)
                     (:shortcut verdict) (assoc :shortcut true))
           :log [(str (:code taxon) ":emit")]})]
    (-> (g/state-graph {:channels {:input           {:default {}}
                                   :taxon           {:default taxon}
                                   :prior-consensus {:default nil}
                                   :verdict         {:default nil}
                                   :reasoning       {:default nil}
                                   :result          {:default nil}
                                   :log             {:reducer into :default []}}})
        (g/add-node :validate validate-node)
        (g/add-node :reason reason-node)
        (g/add-node :emit emit-node)
        (g/set-entry-point :validate)
        (g/add-edge :validate :reason)
        (g/add-edge :reason :emit)
        (g/set-finish-point :emit)
        (g/compile-graph compile-opts))))

(defn run
  "Invokes a compiled organism actor on `input`; returns the :result. A
  :prior-consensus key in input is lifted onto graph state (opt-in shortcut)
  and removed from the domain input. :thread-id makes successive runs accrete
  on one as-of history (the organism's life)."
  ([actor input] (run actor input {}))
  ([actor input {:keys [thread-id]}]
   (let [pc    (:prior-consensus input)
         clean (dissoc input :prior-consensus)]
     (:result (g/invoke actor {:input clean :prior-consensus pc}
                        (cond-> {} thread-id (assoc :thread-id thread-id)))))))

;; ── Murakumo model (production inference path) ───────────────────────────────

(def murakumo-url "http://127.0.0.1:4000/v1/chat/completions")

(defn murakumo-model
  "OpenAI-compatible ChatModel pointed at the local Murakumo LiteLLM gateway
  (ADR-2605215000). host-caps must inject :http-fn/:json-write/:json-read."
  [{:keys [model] :or {model "gemma3:4b"} :as host-caps}]
  (lcm/openai-model (merge {:url murakumo-url :model model} host-caps)))
