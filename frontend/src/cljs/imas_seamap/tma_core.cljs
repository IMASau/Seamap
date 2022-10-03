;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tma-core
  (:require ["react-dom/client" :refer [createRoot]]
            [goog.dom :as gdom]
            [reagent.core :as r]
            [re-frame.core :as re-frame]
            [re-frame.db]
            [com.smxemail.re-frame-cookie-fx]
            [day8.re-frame.async-flow-fx :as async-flow-fx]
            [day8.re-frame.http-fx]
            ["@blueprintjs/core" :as Blueprint]
            [imas-seamap.analytics :refer [analytics-for]]
            [imas-seamap.blueprint :refer [hotkeys-provider]]
            [imas-seamap.events :as events]
            [imas-seamap.tas-marine-atlas.events :as tmaevents]
            [imas-seamap.fx]
            [imas-seamap.interceptors :refer [debug-excluding]]
            [imas-seamap.map.events :as mevents]
            [imas-seamap.map.subs :as msubs]
            [imas-seamap.state-of-knowledge.events :as sokevents]
            [imas-seamap.state-of-knowledge.subs :as soksubs]
            [imas-seamap.story-maps.events :as smevents]
            [imas-seamap.story-maps.subs :as smsubs]
            [imas-seamap.protocols]
            [imas-seamap.subs :as subs]
            [imas-seamap.tas-marine-atlas.views :refer [layout-app]]
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
    :map.layers/lookup                    msubs/map-layer-lookup
    ;:map.layers/params                    msubs/map-layer-extra-params-fn
    :map.layer/info                       subs/map-layer-info
    :map.layer/legend                     msubs/layer-legend
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
    :sm/featured-maps                     smsubs/featured-maps
    :sm/featured-map                      smsubs/featured-map
    :sm.featured-map/open?                smsubs/featured-map-open?
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
    :ui/mouse-pos                         subs/mouse-pos
    :app/loading?                         subs/app-loading?
    :app/load-normal-msg                  subs/load-normal-msg
    :app/load-error-msg                   subs/load-error-msg
    :info/message                         subs/user-message
    :autosave?                            subs/autosave?
    :url-base                             subs/url-base}

   :events
   {:boot                                 [tmaevents/boot (re-frame/inject-cofx :save-code) (re-frame/inject-cofx :hash-code) (re-frame/inject-cofx :cookie/get [:cookie-state])]
    :construct-urls                       tmaevents/construct-urls
    :merge-state                          [tmaevents/merge-state]
    :re-boot                              [tmaevents/re-boot]
    :ajax/default-success-handler         (fn [db [_ arg]] (js/console.log arg) db)
    :ajax/default-err-handler             (fn [db [_ arg]] (js/console.error arg) db)
    :load-hash-state                      [tmaevents/load-hash-state]
    :get-save-state                       [tmaevents/get-save-state]
    :get-save-state-success               [tmaevents/get-save-state-success]
    :initialise-db                        [tmaevents/initialise-db]
    :initialise-layers                    [tmaevents/initialise-layers]
    :loading-failed                       tmaevents/loading-failed
    :help-layer/toggle                    tmaevents/help-layer-toggle
    :help-layer/open                      tmaevents/help-layer-open
    :help-layer/close                     tmaevents/help-layer-close
    :welcome-layer/open                   [tmaevents/welcome-layer-open (re-frame/inject-cofx :cookie/get [:seen-welcome])]
    :welcome-layer/close                  [tmaevents/welcome-layer-close]
    :create-save-state                    [tmaevents/create-save-state]
    :create-save-state-success            [tmaevents/create-save-state-success]
    :create-save-state-failure            [tmaevents/create-save-state-failure]
    :toggle-autosave                      [tmaevents/toggle-autosave]
    :maybe-autosave                       [tmaevents/maybe-autosave]
    :info/show-message                    [tmaevents/show-message]
    :info/clear-message                   tmaevents/clear-message
    :transect/query                       [tmaevents/transect-query]
    :transect/maybe-query                 [tmaevents/transect-maybe-query]
    :transect.query/cancel                tmaevents/transect-query-cancel
    :transect.query/failure               [tmaevents/transect-query-error]
    :transect.query/habitat               [tmaevents/transect-query-habitat]
    :transect.query/bathymetry            [tmaevents/transect-query-bathymetry]
    :transect.query.bathymetry/success    tmaevents/transect-query-bathymetry-success
    :transect.query.habitat/success       tmaevents/transect-query-habitat-success
    :transect.draw/enable                 [tmaevents/transect-drawing-start]
    :transect.draw/disable                tmaevents/transect-drawing-finish
    :transect.draw/clear                  tmaevents/transect-drawing-clear
    :transect.draw/toggle                 [tmaevents/transect-drawing-toggle]
    :transect.plot/show                   tmaevents/transect-visibility-show
    :transect.plot/hide                   tmaevents/transect-visibility-hide
    :transect.plot/mousemove              tmaevents/transect-onmousemove
    :transect.plot/mouseout               tmaevents/transect-onmouseout
    :transect.plot/toggle-visibility      tmaevents/transect-visibility-toggle
    :map.feature/show                     mevents/show-popup
    :map/clicked                          [mevents/map-click-dispatcher]
    :map/get-feature-info                 [mevents/get-feature-info]
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
    :map.layer/show-info                  [tmaevents/layer-show-info]
    :map.layer/close-info                 tmaevents/layer-close-info
    :map.layer/update-metadata            tmaevents/layer-receive-metadata
    :map.layer/metadata-error             tmaevents/layer-receive-metadata-err
    :map.layer/download                   tmaevents/download-show-link
    :map.layer/opacity-changed            [mevents/layer-set-opacity]
    :map.layers/filter                    [mevents/map-set-layer-filter]
    :map.layers/others-filter             mevents/map-set-others-layer-filter
    :map.layer/get-legend                 [mevents/get-layer-legend]
    :map.layer/get-legend-success         mevents/get-layer-legend-success
    :map.layer/get-legend-error           mevents/get-layer-legend-error
    :map.layer.legend/toggle              [mevents/toggle-legend-display]
    :map.layer.selection/enable           [mevents/map-start-selecting]
    :map.layer.selection/disable          mevents/map-cancel-selecting
    :map.layer.selection/clear            mevents/map-clear-selection
    :map.layer.selection/finalise         [mevents/map-finalise-selection]
    :map.layer.selection/toggle           [mevents/map-toggle-selecting]
    :map.region-stats/select-habitat      mevents/region-stats-select-habitat
    :map/update-base-layers               mevents/update-base-layers
    :map/update-base-layer-groups         mevents/update-base-layer-groups
    :map/update-layers                    [mevents/update-layers]
    :map/update-organisations             mevents/update-organisations
    :map/update-classifications           mevents/update-classifications
    :map/update-descriptors               mevents/update-descriptors
    :map/update-categories                mevents/update-categories
    :map/update-keyed-layers              mevents/update-keyed-layers
    :map/update-preview-layer             mevents/update-preview-layer
    :map/initialise-display               [mevents/show-initial-layers]
    :map/pan-to-layer                     [mevents/zoom-to-layer]
    :map/zoom-in                          [mevents/map-zoom-in]
    :map/zoom-out                         [mevents/map-zoom-out]
    :map/pan-direction                    [mevents/map-pan-direction]
    :map/update-leaflet-map               mevents/update-leaflet-map
    :map/update-map-view                  mevents/update-map-view
    :map/view-updated                     [mevents/map-view-updated]
    :map/popup-closed                     [mevents/destroy-popup]
    :map/toggle-ignore-click              mevents/toggle-ignore-click
    :map/toggle-viewport-only             [mevents/toggle-viewport-only]
    :map/pan-to-popup                     [mevents/pan-to-popup]
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
    :sok/habitat-toggle-show-layers       [sokevents/habitat-toggle-show-layers]
    :sok/bathymetry-toggle-show-layers    [sokevents/bathymetry-toggle-show-layers]
    :sok/habitat-observations-toggle-show-layers [sokevents/habitat-observations-toggle-show-layers]
    :sm/update-featured-maps              smevents/update-featured-maps
    :sm/featured-map                      [smevents/featured-map]
    :sm.featured-map/open                 [smevents/featured-map-open]
    :ui/show-loading                      tmaevents/loading-screen
    :ui/hide-loading                      tmaevents/application-loaded
    :ui.catalogue/select-tab              [tmaevents/catalogue-select-tab]
    :ui.catalogue/toggle-node             [tmaevents/catalogue-toggle-node]
    :ui.catalogue/add-node                [tmaevents/catalogue-add-node]
    :ui.catalogue/catalogue-add-nodes-to-layer [tmaevents/catalogue-add-nodes-to-layer]
    :ui.drawing/cancel                    tmaevents/global-drawing-cancel
    :ui.download/close-dialogue           tmaevents/close-download-dialogue
    :ui.search/focus                      [tmaevents/focus-search]
    :ui.sidebar/open                      [tmaevents/sidebar-open]
    :ui.sidebar/close                     tmaevents/sidebar-close
    :ui.sidebar/toggle                    tmaevents/sidebar-toggle
    :ui/mouse-pos                         tmaevents/mouse-pos
    :imas-seamap.components/selection-list-reorder [tmaevents/selection-list-reorder]
    :left-drawer/toggle                   [tmaevents/left-drawer-toggle]
    :left-drawer/open                     [tmaevents/left-drawer-open]
    :left-drawer/close                    [tmaevents/left-drawer-close]
    :left-drawer/tab                      [tmaevents/left-drawer-tab]
    :layers-search-omnibar/toggle         tmaevents/layers-search-omnibar-toggle
    :layers-search-omnibar/open           tmaevents/layers-search-omnibar-open
    :layers-search-omnibar/close          tmaevents/layers-search-omnibar-close}})

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
  [(when ^boolean goog.DEBUG (debug-excluding
                              :transect.plot/mousemove
                              :ui/mouse-pos
                              :map.layer/load-start
                              :map.layer/tile-load-start
                              :map.layer/load-error
                              :map.layer/load-finished
                              :map/update-preview-layer
                              :boot
                              :ui/show-loading
                              :ui/hide-loading
                              :initialise-layers
                              :map/update-layers
                              :map/update-base-layers
                              :map/update-base-layer-groups
                              :map/update-descriptors
                              :map/update-classifications
                              :map/update-organisations
                              :map/update-categories
                              :map/update-keyed-layers
                              :sok/update-amp-boundaries
                              :sok/update-imcra-boundaries
                              :sok/update-meow-boundaries
                              :load-hash-state
                              :map/update-map-view
                              :map/initialise-display
                              :sok/get-habitat-statistics
                              :sok/get-bathymetry-statistics
                              :sok/get-habitat-observations
                              :transect/maybe-query
                              :welcome-layer/open
                              :map/update-leaflet-map
                              :maybe-autosave))
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

(defonce root (createRoot (gdom/getElement "app")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (Blueprint/FocusStyleManager.onlyShowFocusOnTabs)
  (.render
   root
   (r/as-element [hotkeys-provider
                  [:f> layout-app]])))

(defn ^:export show-db []
  @re-frame.db/app-db)

(defn ^:export init [api-url-base media-url-base wordpress-url-base img-url-base]
  (register-handlers! config)
  (re-frame/dispatch-sync [:boot api-url-base media-url-base wordpress-url-base img-url-base])
  (dev-setup)
  (mount-root))

(defn ^:dev/after-load re-render
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code.
  ;; This function is called implicitly by its annotation.
  (mount-root))

