(ns imas-seamap.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [day8.re-frame.http-fx]
            [oops.core :refer [gcall]]
            [re-frisk.core :refer [enable-re-frisk!]]
            [imas-seamap.events :as events]
            [imas-seamap.interceptors :refer [debug-excluding]]
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
    :map.layers/filter                    msubs/map-layers-filter
    :map.layers/others-filter             msubs/map-other-layers-filter
    :map.layers/logic                     msubs/map-layer-logic
    :map.feature/info                     subs/feature-info
    :transect/info                        subs/transect-info
    :transect/results                     subs/transect-results
    :transect.plot/show?                  subs/transect-show?
    :help-layer/open?                     subs/help-layer-open?
    :info/message                         subs/user-message}

   :events
   {:ajax/default-success-handler         (fn [db [_ arg]] (js/console.log arg) db)
    :ajax/default-err-handler             (fn [db [_ arg]] (js/console.error arg) db)
    :initialise-db                        events/initialise-db
    :initialise-layers                    [events/initialise-layers]
    :help-layer/toggle                    events/help-layer-toggle
    :help-layer/open                      events/help-layer-open
    :help-layer/close                     events/help-layer-close
    :info/show-message                    events/show-message
    :transect/query                       [events/transect-query]
    :transect.query/failure               [events/transect-query-error]
    :transect.query/habitat               [events/transect-query-habitat]
    :transect.query/bathymetry            [events/transect-query-bathymetry]
    :transect.query.bathymetry/success    events/transect-query-bathymetry-success
    :transect.query.habitat/success       events/transect-query-habitat-success
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
    :map/got-featureinfo                  mevents/got-feature-info
    :map/toggle-layer                     [mevents/toggle-layer]
    :map.layers/filter                    mevents/map-set-layer-filter
    :map.layers/others-filter             mevents/map-set-others-layer-filter
    :map.layers.logic/toggle              mevents/map-layer-logic-toggle
    :map.layers.logic/manual              mevents/map-layer-logic-manual
    :map.layers.logic/automatic           mevents/map-layer-logic-automatic
    :map/update-layers                    mevents/update-layers
    :map/pan-to-layer                     [mevents/zoom-to-layer]
    :map/view-updated                     mevents/map-view-updated}})

(def standard-interceptors
  [(when ^boolean goog.DEBUG (debug-excluding :transect.plot/mousemove))])

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
  (gcall "Blueprint.FocusStyleManager.onlyShowFocusOnTabs")
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
