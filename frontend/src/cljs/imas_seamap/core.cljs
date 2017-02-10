(ns imas-seamap.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frame.std-interceptors :refer [debug]]
            [re-frisk.core :refer [enable-re-frisk!]]
            [imas-seamap.events :as events]
            [imas-seamap.events.map :as mevents]
            [imas-seamap.subs :as subs]
            [imas-seamap.views :as views]
            [imas-seamap.config :as config]))


(def config
  {:subs
   {:map/props                            subs/map-props}

   :events
   {:ajax                                 events/ajax
    :ajax/default-success-handler         (fn [db [_ arg]] (js/console.log arg) db)
    :ajax/default-err-handler             (fn [db [_ arg]] (js/console.error arg) db)
    :initialise-db                        events/initialise-db
    :initialise-layers                    events/initialise-layers
    :transect/query                       events/not-yet-implemented ; send off the query; store geojson arg in db
    :transect.draw/enable                 events/not-yet-implemented ; enable controls on map
    :transect.draw/disable                events/not-yet-implemented ; disable controls on map
    :transect.draw/clear                  events/not-yet-implemented ; remove transect from map
    :map/toggle-transect                  mevents/toggle-transect
    :map/update-layers                    mevents/update-layers}})

(def standard-interceptors
  [(when ^boolean goog.DEBUG debug)])

(defn register-handlers! [{:keys [subs events]}]
  (doseq [[sym handler] subs]
    (re-frame/reg-sub sym handler))
  (doseq [[sym handler] events]
    (re-frame/reg-event-db
     sym
     standard-interceptors
     handler)))

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
  (re-frame/dispatch [:initialise-layers])
  (dev-setup)
  (mount-root))
