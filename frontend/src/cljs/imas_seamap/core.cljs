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
            [imas-seamap.state-of-knowledge.events :as sokevents]
            [imas-seamap.state-of-knowledge.subs :as soksubs]
            [imas-seamap.protocols]
            [imas-seamap.subs :as subs]
            [imas-seamap.views :as views]
            [imas-seamap.config :as config]))


(def config
  {:subs
   {:map/props                            msubs/map-props
    :map/layers                           msubs/map-layers
    :map/base-layers                      msubs/map-base-layers
    :map/organisations                    msubs/organisations
    :map/display-categories               msubs/display-categories
    :map/categories-map                   msubs/categories-map
    :map.layers/filter                    msubs/map-layers-filter
    :map.layers/others-filter             msubs/map-other-layers-filter
    :map.layers/priorities                msubs/map-layer-priorities
    :map.layers/lookup                    msubs/map-layer-lookup
    ;:map.layers/params                    msubs/map-layer-extra-params-fn
    :map.layer/info                       subs/map-layer-info
    :map.layer.selection/info             msubs/layer-selection-info
    :map.feature/info                     subs/feature-info
    ;:map/region-stats                     msubs/region-stats
    :map/viewport-only?                   msubs/viewport-only?
    :sok/habitat-statistics               soksubs/habitat-statistics
    :sok/habitat-statistics-download-url  soksubs/habitat-statistics-download-url
    :sok/bathymetry-statistics            soksubs/bathymetry-statistics
    :sok/bathymetry-statistics-download-url soksubs/bathymetry-statistics-download-url
    :sok/habitat-observations             soksubs/habitat-observations
    :sok/amp-boundaries                   soksubs/amp-boundaries
    :sok/imcra-boundaries                 soksubs/imcra-boundaries
    :sok/meow-boundaries                  soksubs/meow-boundaries
    :sok/valid-amp-boundaries             soksubs/valid-amp-boundaries
    :sok/valid-imcra-boundaries           soksubs/valid-imcra-boundaries
    :sok/valid-meow-boundaries            soksubs/valid-meow-boundaries
    :sok/boundaries                       soksubs/boundaries
    :sok/active-boundary                  soksubs/active-boundary
    :sok/active-boundaries?               soksubs/active-boundaries?
    :sok/active-zones?                    soksubs/active-zones?
    :sok/open?                            soksubs/open?
    :sok/open-pill                        soksubs/open-pill
    :sok/boundary-layer-filter            soksubs/boundary-layer-filter-fn
    :sorting/info                         subs/sorting-info
    :download/info                        subs/download-info
    :transect/info                        subs/transect-info
    :transect/results                     subs/transect-results
    :transect.plot/show?                  subs/transect-show?
    :help-layer/open?                     subs/help-layer-open?
    :welcome-layer/open?                  subs/welcome-layer-open?
    :left-drawer/open?                    subs/left-drawer-open?
    :left-drawer/tab                      subs/left-drawer-tab
    :layers-search-omnibar/open?          subs/layers-search-omnibar-open?
    :ui.catalogue/tab                     subs/catalogue-tab
    :ui.catalogue/nodes                   subs/catalogue-nodes
    :ui/preview-layer-url                 subs/preview-layer-url
    :ui/sidebar                           subs/sidebar-state
    :app/loading?                         subs/app-loading?
    :app/load-normal-msg                  subs/load-normal-msg
    :app/load-error-msg                   subs/load-error-msg
    :info/message                         subs/user-message}

   :events
   {:boot                                 [events/boot (re-frame/inject-cofx :save-code) (re-frame/inject-cofx :hash-code)]
    :re-boot                              events/re-boot
    :ajax/default-success-handler         (fn [db [_ arg]] (js/console.log arg) db)
    :ajax/default-err-handler             (fn [db [_ arg]] (js/console.error arg) db)
    :load-hash-state                      events/load-hash-state
    :get-save-state                       [events/get-save-state]
    :load-save-state                      [events/load-save-state]
    :initialise-db                        [events/initialise-db]
    :initialise-layers                    [events/initialise-layers]
    :loading-failed                       events/loading-failed
    :help-layer/toggle                    events/help-layer-toggle
    :help-layer/open                      events/help-layer-open
    :help-layer/close                     events/help-layer-close
    :welcome-layer/open                   [events/welcome-layer-open (re-frame/inject-cofx :cookie/get [:seen-welcome])]
    :welcome-layer/close                  [events/welcome-layer-close]
    :create-save-state                    [events/create-save-state]
    :create-save-state-success            [events/create-save-state-success]
    :create-save-state-failure            [events/create-save-state-failure]
    :info/show-message                    [events/show-message]
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
    :map/toggle-layer-visibility          [mevents/toggle-layer-visibility]
    :map/add-layer                        [mevents/add-layer]
    :map/remove-layer                     [mevents/remove-layer]
    :map/add-layer-from-omnibar           [mevents/add-layer-from-omnibar]
    :map/base-layer-changed               [mevents/base-layer-changed]
    :map.layer/load-start                 mevents/layer-started-loading
    :map.layer/tile-load-start            mevents/layer-tile-started-loading
    :map.layer/load-error                 mevents/layer-loading-error
    :map.layer/load-finished              mevents/layer-finished-loading
    :map.layer/show-info                  [events/layer-show-info]
    :map.layer/close-info                 events/layer-close-info
    :map.layer/update-metadata            events/layer-receive-metadata
    :map.layer/metadata-error             events/layer-receive-metadata-err
    :map.layer/download                   events/download-show-link
    :map.layer/opacity-changed            [mevents/layer-set-opacity]
    :map.layers/filter                    [mevents/map-set-layer-filter]
    :map.layers/others-filter             mevents/map-set-others-layer-filter
    :map.layer.legend/toggle              [mevents/toggle-legend-display]
    :map.layer.selection/enable           mevents/map-start-selecting
    :map.layer.selection/disable          mevents/map-cancel-selecting
    :map.layer.selection/clear            mevents/map-clear-selection
    :map.layer.selection/finalise         [mevents/map-finalise-selection]
    :map.layer.selection/toggle           [mevents/map-toggle-selecting]
    :map.region-stats/select-habitat      mevents/region-stats-select-habitat
    :map/update-base-layers               mevents/update-base-layers
    :map/update-base-layer-groups         mevents/update-base-layer-groups
    :map/update-layers                    mevents/update-layers
    :map/update-groups                    mevents/update-groups
    :map/update-organisations             mevents/update-organisations
    :map/update-classifications           mevents/update-classifications
    :map/update-priorities                mevents/update-priorities
    :map/update-descriptors               mevents/update-descriptors
    :map/update-categories                mevents/update-categories
    :map/update-preview-layer             mevents/update-preview-layer
    :map/initialise-display               [mevents/show-initial-layers]
    :map/pan-to-layer                     [mevents/zoom-to-layer]
    :map/zoom-in                          mevents/map-zoom-in
    :map/zoom-out                         mevents/map-zoom-out
    :map/pan-direction                    mevents/map-pan-direction
    :map/view-updated                     [mevents/map-view-updated]
    :map/popup-closed                     mevents/destroy-popup
    :map/toggle-ignore-click              mevents/toggle-ignore-click
    :map/toggle-viewport-only             [mevents/toggle-viewport-only]
    :sok/update-amp-boundaries            sokevents/update-amp-boundaries
    :sok/update-imcra-boundaries          sokevents/update-imcra-boundaries
    :sok/update-meow-boundaries           sokevents/update-meow-boundaries
    :sok/update-active-boundary-layer     [sokevents/update-active-boundary-layer]
    :sok/update-active-boundary           [sokevents/update-active-boundary]
    :sok/update-active-network            [sokevents/update-active-network]
    :sok/update-active-park               [sokevents/update-active-park]
    :sok/update-active-zone               [sokevents/update-active-zone]
    :sok/update-active-zone-iucn          [sokevents/update-active-zone-iucn]
    :sok/update-active-provincial-bioregion [sokevents/update-active-provincial-bioregion]
    :sok/update-active-mesoscale-bioregion [sokevents/update-active-mesoscale-bioregion]
    :sok/update-active-realm              [sokevents/update-active-realm]
    :sok/update-active-province           [sokevents/update-active-province]
    :sok/update-active-ecoregion          [sokevents/update-active-ecoregion]
    :sok/reset-active-boundaries          [sokevents/reset-active-boundaries]
    :sok/reset-active-zones               [sokevents/reset-active-zones]
    :sok/get-habitat-statistics           [sokevents/get-habitat-statistics]
    :sok/got-habitat-statistics           sokevents/got-habitat-statistics
    :sok/get-bathymetry-statistics        [sokevents/get-bathymetry-statistics]
    :sok/got-bathymetry-statistics        sokevents/got-bathymetry-statistics
    :sok/get-habitat-observations         [sokevents/get-habitat-observations]
    :sok/got-habitat-observations         sokevents/got-habitat-observations
    :sok/close                            [sokevents/close]
    :sok/open-pill                        sokevents/open-pill
    :sok/get-filtered-bounds              [sokevents/get-filtered-bounds]
    :sok/got-filtered-bounds              [sokevents/got-filtered-bounds]
    :ui/show-loading                      events/loading-screen
    :ui/hide-loading                      events/application-loaded
    :ui.catalogue/select-tab              [events/catalogue-select-tab]
    :ui.catalogue/toggle-node             [events/catalogue-toggle-node]
    :ui.catalogue/add-node                [events/catalogue-add-node]
    :ui.catalogue/catalogue-add-nodes-to-layer [events/catalogue-add-nodes-to-layer]
    :ui.drawing/cancel                    events/global-drawing-cancel
    :ui.download/close-dialogue           events/close-download-dialogue
    :ui.search/focus                      [events/focus-search]
    :ui.sidebar/open                      [events/sidebar-open]
    :ui.sidebar/close                     events/sidebar-close
    :ui.sidebar/toggle                    events/sidebar-toggle
    :imas-seamap.components/selection-list-reorder [events/selection-list-reorder]
    :left-drawer/toggle                   events/left-drawer-toggle
    :left-drawer/open                     events/left-drawer-open
    :left-drawer/close                    events/left-drawer-close
    :left-drawer/tab                      [events/left-drawer-tab]
    :layers-search-omnibar/toggle         events/layers-search-omnibar-toggle
    :layers-search-omnibar/open           events/layers-search-omnibar-open
    :layers-search-omnibar/close          events/layers-search-omnibar-close}})

(def events-for-analytics
  [:help-layer/open
   :map.layer/load-error
   :map/clicked
   :map/pan-to-layer
   :map/toggle-layer
   :map/toggle-layer-visibility
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

(defn ^:export show-db []
  @re-frame.db/app-db)

(defn ^:export init []
  (register-handlers! config)
  (re-frame/dispatch-sync [:boot])
  (dev-setup)
  (mount-root))

