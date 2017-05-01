(ns imas-seamap.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frame.std-interceptors :refer [debug]]
            [day8.re-frame.http-fx]
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
   {:map/props                            msubs/map-props
    :map/layers                           msubs/map-layers
    :transect/info                        subs/transect-info
    :transect/results                     subs/transect-results
    :transect.plot/show?                  subs/transect-show?
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
    :transect.query/habitat               events/transect-query-habitat
    :transect.query/bathymetry            events/transect-query-bathymetry
    :transect.query.bathymetry/success    events/transect-query-bathymetry-success
    :transect.query/error                 events/transect-query-error
    :transect.draw/enable                 events/transect-drawing-start
    :transect.draw/disable                events/transect-drawing-finish
    :transect.draw/clear                  events/not-yet-implemented
    :transect.plot/show                   events/transect-visibility-show
    :transect.plot/hide                   events/transect-visibility-hide
    :transect.plot/mousemove              events/transect-onmousemove
    :transect.plot/mouseout               events/transect-onmouseout
    :transect.plot/toggle-visibility      events/transect-visibility-toggle
    :map/clicked                          [mevents/get-feature-info]
    ;; :map/got-featureinfo                  js/console.warn
    :map/toggle-layer                     mevents/toggle-layer
    :map/update-layers                    mevents/update-layers
    :map/pan-to-layer                     mevents/zoom-to-layer
    :map/view-updated                     mevents/map-view-updated}})

(def standard-interceptors
  [(when ^boolean goog.DEBUG debug)])

(defn register-handlers! [{:keys [subs events]}]
  (doseq [[sym handler] subs]
    (re-frame/reg-sub sym handler))
  (doseq [[sym handler] events]
    (if (sequential? handler)
      (re-frame/reg-event-fx
       sym
       standard-interceptors
       (first handler))
      (re-frame/reg-event-db
       sym
       standard-interceptors
       handler))))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    ;(enable-re-frisk!)
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
