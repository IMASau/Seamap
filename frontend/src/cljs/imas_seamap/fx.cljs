;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.fx
  (:require [clojure.string :as string]
            [imas-seamap.blueprint :as b :refer [toaster]]
            [imas-seamap.utils :refer [parse-state]]
            [re-frame.core :as re-frame]))

(defn set-location-anchor [anchor]
  (set! js/location -hash anchor))

(re-frame/reg-fx :put-hash set-location-anchor)


(defn show-message [[message intent-or-opts]]
  (let [msg (merge {:intent    b/INTENT-WARNING
                    :onDismiss #(re-frame/dispatch [:info/clear-message])
                    :message   message}
                   (if (map? intent-or-opts) intent-or-opts {:intent intent-or-opts}))]
    (. toaster show (clj->js msg))))

(re-frame/reg-fx :message show-message)


(defn cofx-hash-state [cofx _]
  (let [hash (subs (. js/location -hash) 1)
        matches  (re-matches #"^view=(.*)$" hash) ;; Checking for shortcode
        shortcode (when matches (nth matches 1)) ;; Use shortcode if one exists
        hash-val (when-not matches  hash)] ;; Use hash-val if shortcode does not exist
    (js/console.log hash-val)
    (cond-> (assoc cofx :shortcode shortcode)
      (seq hash-val)
      (assoc :hash-state (parse-state hash-val)))))

(re-frame/reg-cofx :hash-state cofx-hash-state)
