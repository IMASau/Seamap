(ns imas-seamap.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frame.std-interceptors :refer [debug]]
            [re-frisk.core :refer [enable-re-frisk!]]
            [imas-seamap.events :as events]
            [imas-seamap.map.events :as mevents]
            [imas-seamap.map.subs :as msubs]
            [imas-seamap.protocols]
            [imas-seamap.subs :as subs]
            [imas-seamap.views :as views]
            [imas-seamap.config :as config]))


(def config
  {:subs
   {:map/props                            subs/map-props
    :map/layers                           msubs/map-layers
    :transect/info                        subs/transect-info
    :help-layer/open?                     subs/help-layer-open?}

   :events
   {:ajax                                 events/ajax
    :ajax/default-success-handler         (fn [db [_ arg]] (js/console.log arg) db)
    :ajax/default-err-handler             (fn [db [_ arg]] (js/console.error arg) db)
    :initialise-db                        events/initialise-db
    :initialise-layers                    events/initialise-layers
    :help-layer/toggle                    events/help-layer-toggle
    :help-layer/open                      events/help-layer-open
    :help-layer/close                     events/help-layer-close
    :transect/query                       events/transect-query
    :transect.draw/enable                 events/transect-drawing-start
    :transect.draw/disable                events/transect-drawing-finish
    :transect.draw/clear                  events/not-yet-implemented
    :map/toggle-layer                     mevents/toggle-layer
    :map/update-layers                    mevents/update-layers
    :map/view-updated                     mevents/map-view-updated}})

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

(defn figwheel-reload []
  (register-handlers! config)
  (mount-root))
