(ns figwheel
  (:require [figwheel-sidecar.repl-api :refer [start-figwheel! cljs-repl reset-autobuild]]))

;;; reset-autobuild included so it's accessible when we :cljs/quit back to the clj repl

(start-figwheel! "dev")
(cljs-repl)
