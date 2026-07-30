;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.fx
  (:require [goog.net.Cookies]
            [imas-seamap.blueprint :as b]
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

;;; Cookies (replaces com.smxemail/re-frame-cookie-fx which is
;;; abandoned / relies on deprecated functionality in the closure
;;; library):

;; Helper to get a new Cookies instance
(defn- get-cookies-instance []
  (.getInstance goog.net.Cookies))

;; Helper to convert a keyword to a string, or return the input if already a string
(defn- kw->str [k]
  (if (keyword? k) (name k) k))

;; Define a default expiration (e.g., 1 year from now)
(defn- default-expires-date []
  (let [now (js/Date.)
        one-year-ms (* 365 24 60 60 1000)] ; milliseconds in one year
    (js/Date. (+ (.getTime now) one-year-ms))))

;; --- COFX Handler: :cookies/get ---
;; Usage: (re-frame/inject-cofx :cookies/get [:seen-welcome])
;; This will add {:cookies/get {:seen-welcome "cookie-value"} to the cofx map.
(re-frame/reg-cofx
 :cookie/get
 (fn [cofx names]
   (let [cookies (get-cookies-instance)]
     (assoc cofx
            :cookie/get
            (reduce #(into %1 {(keyword %2) (.get cookies (name %2))})
                    {}
                    names))))) ; Associates values under the original keywordised names

;; --- COFX Handler: :cookies/set ---
;; Usage: (re-frame/inject-cofx :cookies/set {:name :seen-welcome :value true :expires (js/Date. 0)})
;; Options like :expires, :path, :domain, :secure, :sameSite are optional.
;; :expires can be a js/Date object or a number (milliseconds from epoch).
(re-frame/reg-fx
  :cookie/set
  (fn [{:keys [name value expires path domain secure sameSite] :as opts}]
    (let [cookies (get-cookies-instance)
          cookie-name-str (kw->str name)
          cookie-value-str (str value) ; Cookie values are typically strings
          effective-expires (or expires (default-expires-date))
          effective-path (or path "/")]
      (.set cookies
            cookie-name-str
            cookie-value-str
            effective-expires
            effective-path
            (or domain nil)   ; Pass nil if not specified
            (or secure nil)   ; Pass nil if not specified
            (or sameSite nil))))) ; Pass nil if not specified (e.g., "Lax", "Strict", "None")
