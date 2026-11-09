;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.specs.preload
  (:require [re-frame.core :as re-frame]
            [re-frame.db :refer [app-db]]
            [cljs.spec.alpha :as s]
            [cljs.spec.test.alpha :as stest]
            [imas-seamap.specs.app-state]
            [imas-seamap.analytics]
            [imas-seamap.specs.components]
            [imas-seamap.specs.state-of-knowledge.views]
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

(defn- validate-on-change [_ _ old new]
  (when-not (identical? old new)
    (validate-state new)))

(defn enable-validation! []
  (js/console.info ::enabling-app-db-validation)
  (validate-state @app-db)
  (add-watch app-db ::validate validate-on-change))

(defn disable-validation! []
  (remove-watch app-db ::validate))

;; The app-db starts out invalid and passes through many transitional states
;; during boot, so validating from the outset just floods the console.  Every
;; boot flow (plain, hash-state and save-state, across all app variants) halts
;; on :ui/hide-loading, so treat that as "boot complete" and only start
;; validating then.  :boot and :re-boot put the db back into a transitional
;; state, so switch validation off again until the flow finishes.
(re-frame/reg-global-interceptor
 (re-frame/->interceptor
  :id    ::toggle-validation
  :after (fn [context]
           (case (first (re-frame/get-coeffect context :event))
             (:boot :re-boot) (disable-validation!)
             :ui/hide-loading (enable-validation!)
             nil)
           context)))
