(ns imas-seamap.specs.preload
  (:require [re-frame.db :refer [app-db]]
            [cljs.spec :as s]
            [cljs.spec.test :as stest]
            [imas-seamap.specs.app-state]
            [imas-seamap.specs.events]
            [imas-seamap.specs.spec-utils :as spec-utils]))


(js/console.info ::instrumenting)
(s/check-asserts true)

(spec-utils/patch-spec-checking-fn)
(stest/instrument)

(defn validate-state [val]
  (s/assert :seamap/app-state val))

(set-validator! app-db validate-state)
