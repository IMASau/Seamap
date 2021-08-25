;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as re-frame]
            [re-frame.db]
            [com.smxemail.re-frame-cookie-fx]
            [day8.re-frame.async-flow-fx :as async-flow-fx]
            [day8.re-frame.http-fx]
            ["@blueprintjs/core" :as Blueprint]
            [imas-seamap.analytics :refer [analytics-for]]
            [imas-seamap.blueprint :refer [hotkeys-provider]]
            [imas-seamap.events :as events]
            [imas-seamap.fx]
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
    :map/organisations                    msubs/organisations
    :map.layers/filter                    msubs/map-layers-filter
    :map.layers/others-filter             msubs/map-other-layers-filter
    :map.layers/priorities                msubs/map-layer-priorities
    :map.layers/logic                     msubs/map-layer-logic
    :map.layers/lookup                    msubs/map-layer-lookup
    :map.layers/params                    msubs/map-layer-extra-params-fn
    :map.layer/info                       subs/map-layer-info
    :map.layer.selection/info             msubs/layer-selection-info
    :map.feature/info                     subs/feature-info
    :map/region-stats                     msubs/region-stats
    :sorting/info                         subs/sorting-info
    :download/info                        subs/download-info
    :transect/info                        subs/transect-info
    :transect/results                     subs/transect-results
    :transect.plot/show?                  subs/transect-show?
    :help-layer/open?                     subs/help-layer-open?
    :welcome-layer/open?                  subs/welcome-layer-open?
    :ui.catalogue/tab                     subs/catalogue-tab
    :ui.catalogue/nodes                   subs/catalogue-nodes
    :ui/sidebar                           subs/sidebar-state
    :app/loading?                         subs/app-loading?
    :app/load-normal-msg                  subs/load-normal-msg
    :app/load-error-msg                   subs/load-error-msg
    :info/message                         subs/user-message}

   :events
   {:boot                                 [events/boot (re-frame/inject-cofx :hash-state)]
    :re-boot                              events/re-boot
    :ajax/default-success-handler         (fn [db [_ arg]] (js/console.log arg) db)
    :ajax/default-err-handler             (fn [db [_ arg]] (js/console.error arg) db)
    :initialise-db                        [events/initialise-db]
    :initialise-layers                    [events/initialise-layers]
    :loading-failed                       events/loading-failed
    :help-layer/toggle                    events/help-layer-toggle
    :help-layer/open                      events/help-layer-open
    :help-layer/close                     events/help-layer-close
    :welcome-layer/open                   [events/welcome-layer-open (re-frame/inject-cofx :cookie/get [:seen-welcome])]
    :welcome-layer/close                  [events/welcome-layer-close]
    :copy-share-url                       [events/copy-share-url]
    :info/show-message                    events/show-message
    :info/clear-message                   events/clear-message
    :transect/query                       [events/transect-query]
    :transect/maybe-query                 [events/transect-maybe-query]
    :transect.query/cancel                events/transect-query-cancel
    :transect.query/failure               [events/transect-query-error]
    :transect.query/habitat               [events/transect-query-habitat]
    :transect.query/bathymetry            [events/transect-query-bathymetry]
    :transect.query.bathymetry/success    events/transect-query-bathymetry-success
    :transect.query.habitat/success       events/transect-query-habitat-success
    :transect.draw/enable                 events/transect-drawing-start
    :transect.draw/disable                events/transect-drawing-finish
    :transect.draw/clear                  events/transect-drawing-clear
    :transect.draw/toggle                 [events/transect-drawing-toggle]
    :transect.plot/show                   events/transect-visibility-show
    :transect.plot/hide                   events/transect-visibility-hide
    :transect.plot/mousemove              events/transect-onmousemove
    :transect.plot/mouseout               events/transect-onmouseout
    :transect.plot/toggle-visibility      events/transect-visibility-toggle
    :map/clicked                          [mevents/map-click-dispatcher]
    :map/got-featureinfo                  mevents/got-feature-info
    :map/got-featureinfo-err              mevents/got-feature-info-error
    :map/toggle-layer                     [mevents/toggle-layer]
    :map.layer/load-start                 mevents/layer-started-loading
    :map.layer/load-error                 mevents/layer-loading-error
    :map.layer/load-finished              mevents/layer-finished-loading
    :map.layer/show-info                  [events/layer-show-info]
    :map.layer/close-info                 events/layer-close-info
    :map.layer/update-metadata            events/layer-receive-metadata
    :map.layer/metadata-error             events/layer-receive-metadata-err
    :map.layer/download                   events/download-show-link
    :map.layers/filter                    mevents/map-set-layer-filter
    :map.layers/others-filter             mevents/map-set-others-layer-filter
    :map.layers.logic/toggle              [mevents/map-layer-logic-toggle]
    :map.layers.logic/manual              mevents/map-layer-logic-manual
    :map.layers.logic/automatic           mevents/map-layer-logic-automatic
    :map.layer.legend/toggle              [mevents/toggle-legend-display]
    :map.layer.selection/enable           mevents/map-start-selecting
    :map.layer.selection/disable          mevents/map-cancel-selecting
    :map.layer.selection/clear            mevents/map-clear-selection
    :map.layer.selection/finalise         [mevents/map-finalise-selection]
    :map.layer.selection/toggle           [mevents/map-toggle-selecting]
    :map.region-stats/select-habitat      mevents/region-stats-select-habitat
    :map/update-layers                    mevents/update-layers
    :map/update-groups                    mevents/update-groups
    :map/update-organisations             mevents/update-organisations
    :map/update-classifications           mevents/update-classifications
    :map/update-priorities                mevents/update-priorities
    :map/update-descriptors               mevents/update-descriptors
    :map/initialise-display               [mevents/show-initial-layers]
    :map/pan-to-layer                     [mevents/zoom-to-layer]
    :map/zoom-in                          mevents/map-zoom-in
    :map/zoom-out                         mevents/map-zoom-out
    :map/pan-direction                    mevents/map-pan-direction
    :map/view-updated                     [mevents/map-view-updated]
    :map/popup-closed                     mevents/destroy-popup
    :ui/show-loading                      events/loading-screen
    :ui/hide-loading                      events/application-loaded
    :ui.catalogue/select-tab              [events/catalogue-select-tab]
    :ui.catalogue/toggle-node             [events/catalogue-toggle-node]
    :ui.drawing/cancel                    events/global-drawing-cancel
    :ui.download/close-dialogue           events/close-download-dialogue
    :ui.search/focus                      [events/focus-search]
    :ui.sidebar/open                      [events/sidebar-open]
    :ui.sidebar/close                     events/sidebar-close
    :ui.sidebar/toggle                    events/sidebar-toggle}})

(def events-for-analytics
  [:help-layer/open
   :map.layer/load-error
   :map.layers.logic/toggle
   :map/clicked
   :map/pan-to-layer
   :map/toggle-layer
   :transect.plot/toggle-visibility
   :transect/query])

(def standard-interceptors
  [(when ^boolean goog.DEBUG (debug-excluding :transect.plot/mousemove))
   (when-not ^boolean goog.DEBUG (analytics-for events-for-analytics))])

(defn register-handlers! [{:keys [subs events]}]
  (doseq [[sym handler] subs]
    (re-frame/reg-sub sym handler))
  (doseq [[sym handler] events]
    (if (sequential? handler)
      (re-frame/reg-event-fx
       sym
       [(rest handler) standard-interceptors]
       (first handler))
      (re-frame/reg-event-db
       sym
       standard-interceptors
       handler))))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (Blueprint/FocusStyleManager.onlyShowFocusOnTabs)
  (rdom/render
   [hotkeys-provider
    [:f> views/layout-app]]
   (.getElementById js/document "app")))

(when goog/DEBUG
  (defn show-db []
    @re-frame.db/app-db))

(defn ^:export init []
  (register-handlers! config)
  (re-frame/dispatch-sync [:boot])
  (dev-setup)
  (mount-root))

(defn figwheel-reload []
  (register-handlers! config)
  (mount-root))
