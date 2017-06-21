(ns imas-seamap.specs.preload
  (:require [re-frame.db :refer [app-db]]
            [cljs.spec.alpha :as s]
            [cljs.spec.test.alpha :as stest]
            [imas-seamap.specs.app-state]
            [imas-seamap.analytics]
            [imas-seamap.specs.events]
            [imas-seamap.specs.spec-utils :as spec-utils]))


(spec-utils/patch-spec-checking-fn)

(js/console.info ::instrumenting (stest/instrument))
(js/console.info ::check-asserts (s/check-asserts true))

;;; Don't raise an error though, just report to the user
(defn validate-state [val]
  (try
    (when-not (s/valid? :seamap/app-state val)
      (js/console.error "app-state is in an invalid state")
      (spec-utils/explain-console (s/explain-data :seamap/app-state val)))
    (catch js/Object e
      (js/console.error e)))
  true)

(set-validator! app-db validate-state)
