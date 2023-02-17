;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.fx
  (:require [imas-seamap.blueprint :as b]
            [imas-seamap.utils :refer [uuid4?]]
            [re-frame.core :as re-frame]))

(defn set-location-anchor [anchor]
  (set! js/location -hash anchor))

(re-frame/reg-fx :put-hash set-location-anchor)


(defn show-message [[message intent-or-opts]]
  (let [msg (merge {:intent    b/INTENT-WARNING
                    :onDismiss #(re-frame/dispatch [:info/clear-message])
                    :message   message}
                   (if (map? intent-or-opts) intent-or-opts {:intent intent-or-opts}))]
    (. b/toaster show (clj->js msg))))

(re-frame/reg-fx :message show-message)

(defn cofx-hash-code [cofx _]
  (let [hash (subs (. js/location -hash) 1)
        hash-code (when-not (uuid4? hash) hash)] ; Use hash-code if save-code does not exist
    (assoc cofx :hash-code hash-code)))

(re-frame/reg-cofx :hash-code cofx-hash-code)


(defn cofx-save-code [cofx _]
  (let [hash (subs (. js/location -hash) 1)
        save-code (when (uuid4? hash) hash)] ; Use save-code if one exists
    (assoc cofx :save-code save-code)))

(re-frame/reg-cofx :save-code cofx-save-code)
