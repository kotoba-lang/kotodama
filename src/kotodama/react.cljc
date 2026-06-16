(ns kotodama.react
  "A genuine ReAct tool-calling loop per actor:

      :agent → (tool calls?) → :tools → :agent → … → END

  Generic over the actor's capability: pass a `:requirements` fn and a `:validate`
  fn and kotodama exposes them as the two canonical tools (inspect_requirements,
  validate_line). Built on langgraph-clj's create-react-agent, so it runs
  identically on JVM and in the browser (.cljc). Inference is Murakumo-only at
  runtime (pass a Murakumo ChatModel; a mock in tests)."
  (:require [langgraph.prebuilt :as pre]))

(defn capability-tools
  "Two canonical capability tools from an actor's domain functions.
   :requirements (fn [] -> {:required [..] :checks [..] ...})
   :validate     (fn [line-map] -> verdict {:missing :checks :ok ...})"
  [{:keys [requirements validate]}]
  [{:name "inspect_requirements"
    :description "Return the required fields and checks for this actor."
    :schema {:type "object" :properties {}}
    :fn (fn [_] (requirements))}
   {:name "validate_line"
    :description "Validate a buyer's line (field -> value map). Returns missing fields, per-check pass/fail, overall ok."
    :schema {:type "object"
             :properties {:line {:type "object" :description "field -> value map"}}
             :required ["line"]}
    :fn (fn [{:keys [line]}] (validate (or line {})))}])

(defn react-actor
  "Compiles a ReAct loop actor.
   opts: {:model ChatModel :tools [..] :system \"..\" :compile-opts {..}}
   (tools default to none; pass capability-tools for the canonical pair)."
  [{:keys [model tools system compile-opts]}]
  (pre/create-react-agent
   {:model model :tools (or tools []) :system system :compile-opts (or compile-opts {})}))
