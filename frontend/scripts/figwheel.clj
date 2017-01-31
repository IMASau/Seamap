(ns figwheel
  (:require [figwheel-sidecar.repl-api :as api]))

(api/start-figwheel! "dev")
(api/cljs-repl)
