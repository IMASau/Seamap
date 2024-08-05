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

(re-frame/reg-fx
 :local-storage/set
 (fn [{:keys [name value]}]
   (js/window.localStorage.setItem (cljs.core/name name) value)))

(re-frame/reg-fx
 :local-storage/remove
 (fn [{:keys [name]}]
   (js/window.localStorage.removeItem (cljs.core/name name))))

(re-frame/reg-cofx
 :local-storage/get
 (fn [cofx names]
   (let [values
         (reduce
          (fn [acc name]
            (assoc acc name (js/window.localStorage.getItem (cljs.core/name name))))
          {} names)]
     (assoc cofx :local-storage/get values))))

; An atom that holds a map of timeout IDs to their corresponding JavaScript
; timeout handles.
;
; Source: https://github.com/day8/re-frame/issues/233#issuecomment-252738662
(defonce timeouts
  (atom {}))

; Dispatches an event after a specified delay.
; Arguments:
; * id: Debounce identifier
; * event-vec: Event to be dispatched
; * n: Delay in milliseconds
;
; Clears any existing timeout for the given id, sets a new timeout, and dispatches
; the event after the specified delay.
; Removes the id from the timeouts atom once the event is dispatched.
;
; Source: https://github.com/day8/re-frame/issues/233#issuecomment-252738662
(re-frame/reg-fx
 :dispatch-debounce
 (fn [[id event-vec n]]
   (js/clearTimeout (@timeouts id))
   (swap!
    timeouts assoc id
    (js/setTimeout
     (fn []
       (re-frame/dispatch event-vec)
       (swap! timeouts dissoc id))
     n))))

; A re-frame effect handler that stops a debounced event from being dispatched.
; Arguments:
; - id: Debounce identifier.
; 
; Clears any existing timeout for the given id and removes the id from the
; timeouts atom.
;
; Source: https://github.com/day8/re-frame/issues/233#issuecomment-252738662
(re-frame/reg-fx
 :stop-debounce
 (fn [id]
   (js/clearTimeout (@timeouts id))
   (swap! timeouts dissoc id)))
