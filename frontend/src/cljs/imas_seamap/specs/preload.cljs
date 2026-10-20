;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.specs.preload
  (:require [re-frame.db :refer [app-db]]
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

;;; Validation runs on every state change. Checking is cheap, but explaining
;;; (finding every problem against a full layer catalogue) takes far longer,
;;; and printing them all can freeze the page. So an invalid state is explained
;;; at most once a second, using the latest state, and printed only when the set
;;; of problems has changed since it was last printed.

(def ^:private explain-interval-ms 1000)

(defonce ^:private last-problems (atom nil))
(defonce ^:private latest-invalid (atom nil))
(defonce ^:private explain-timer (atom nil))

;; Where a problem is, not what it says: cheap to compare.
(defn- problem-signature [{:keys [path in]}]
  [path in])

(defn- explain-latest! []
  (reset! explain-timer nil)
  (try
    (let [explain-data (s/explain-data :seamap/app-state @latest-invalid)
          problems     (set (map problem-signature (::s/problems explain-data)))]
      (when (not= problems @last-problems)
        (reset! last-problems problems)
        (js/console.error "app-state is in an invalid state:" (count problems)
                          "problems (not repeated until they change)")
        (spec-utils/explain-console explain-data)))
    (catch js/Object e
      (js/console.error e))))

;;; Don't raise an error though, just report to the user
(defn validate-state [val]
  (try
    (if (s/valid? :seamap/app-state val)
      (do (when-let [timer @explain-timer] (js/clearTimeout timer))
          (reset! explain-timer nil)
          (reset! last-problems nil))
      (do (reset! latest-invalid val)
          (when-not @explain-timer
            (reset! explain-timer (js/setTimeout explain-latest! explain-interval-ms)))))
    (catch js/Object e
      (js/console.error e)))
  true)

(set-validator! app-db validate-state)
