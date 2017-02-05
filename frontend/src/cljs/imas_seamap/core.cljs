(ns imas-seamap.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frisk.core :refer [enable-re-frisk!]]
            [imas-seamap.events :as events]
            [imas-seamap.subs :as subs]
            [imas-seamap.views :as views]
            [imas-seamap.config :as config]))


(def config
  {:subs   {:map/props     subs/map-props}
   :events {:initialise-db events/-initialise-db}})

(defn register-handlers! [{:keys [subs events]}]
  (doseq [[sym handler] subs]
    (re-frame/reg-sub sym handler))
  (doseq [[sym handler] events]
    (re-frame/reg-event-db sym handler)))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (enable-re-frisk!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/layout-app]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (register-handlers! config)
  (re-frame/dispatch-sync [:initialise-db])
  (dev-setup)
  (mount-root))
