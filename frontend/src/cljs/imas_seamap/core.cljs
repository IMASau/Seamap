;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.core
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
            [imas-seamap.views :as views]
            [imas-seamap.config :as config]
            [imas-seamap.components :as components]))


(def config-handlers
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
    :map/filtered-dynamic-pills           msubs/filtered-dynamic-pills
    :sok/habitat-statistics               soksubs/habitat-statistics
    :sok/habitat-statistics-download-url  soksubs/habitat-statistics-download-url
    :sok/bathymetry-statistics            soksubs/bathymetry-statistics
    :sok/bathymetry-statistics-download-url soksubs/bathymetry-statistics-download-url
    :sok/habitat-observations             soksubs/habitat-observations
    :sok/amp-boundaries                   soksubs/amp-boundaries
    :sok/imcra-boundaries                 soksubs/imcra-boundaries
    :sok/meow-boundaries                  soksubs/meow-boundaries
    :sok/valid-boundaries                 soksubs/valid-boundaries
    :sok/boundaries                       soksubs/boundaries
    :sok/active-boundary                  soksubs/active-boundary
    :sok/active-boundaries?               soksubs/active-boundaries?
    :sok/active-zones?                    soksubs/active-zones?
    :sok/open?                            soksubs/open?
    :sok/boundary-layer-filter            soksubs/boundary-layer-filter-fn
    :sok/region-report-url                soksubs/region-report-url
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
    :ui/right-sidebar                     subs/right-sidebar
    :ui/open-pill                         subs/open-pill
    :ui/mouse-pos                         subs/mouse-pos
    :ui/settings-overlay                  subs/settings-overlay
    :app/loading?                         subs/app-loading?
    :app/load-normal-msg                  subs/load-normal-msg
    :app/load-error-msg                   subs/load-error-msg
    :info/message                         subs/user-message
    :autosave?                            subs/autosave?
    :url-base                             subs/url-base}

   :events
   {:boot                                 [events/boot (re-frame/inject-cofx :save-code) (re-frame/inject-cofx :hash-code) (re-frame/inject-cofx :local-storage/get [:seamap-app-state])]
    :construct-urls                       events/construct-urls
    :merge-state                          [events/merge-state]
    :re-boot                              [events/re-boot]
    :ajax/default-success-handler         (fn [db [_ arg]] (js/console.log arg) db)
    :ajax/default-err-handler             (fn [db [_ arg]] (js/console.error arg) db)
    ;;; we ignore success/failure of cookie setting; these are fired by default, so just ignore:
    :cookie-set-no-on-success             identity
    :cookie-set-no-on-failure             identity
    :load-hash-state                      [events/load-hash-state]
    :get-save-state                       [events/get-save-state]
    :get-save-state-success               [events/get-save-state-success]
    :initialise-db                        [events/initialise-db]
    :initialise-layers                    [events/initialise-layers]
    :loading-failed                       events/loading-failed
    :update-dynamic-pills                 events/update-dynamic-pills
    :help-layer/toggle                    events/help-layer-toggle
    :help-layer/open                      events/help-layer-open
    :help-layer/close                     events/help-layer-close
    :welcome-layer/open                   [events/welcome-layer-open (re-frame/inject-cofx :cookie/get [:seen-welcome])]
    :welcome-layer/close                  [events/welcome-layer-close]
    :create-save-state                    [events/create-save-state]
    :create-save-state-success            [events/create-save-state-success]
    :create-save-state-failure            [events/create-save-state-failure]
    :toggle-autosave                      [events/toggle-autosave]
    :maybe-autosave                       [events/maybe-autosave]
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
    :transect.draw/enable                 [events/transect-drawing-start]
    :transect.draw/disable                events/transect-drawing-finish
    :transect.draw/clear                  events/transect-drawing-clear
    :transect.draw/toggle                 [events/transect-drawing-toggle]
    :transect.plot/show                   events/transect-visibility-show
    :transect.plot/hide                   events/transect-visibility-hide
    :transect.plot/mousemove              events/transect-onmousemove
    :transect.plot/mouseout               events/transect-onmouseout
    :transect.plot/toggle-visibility      events/transect-visibility-toggle
    :map.feature/show                     mevents/show-popup
    :map/clicked                          [mevents/map-click-dispatcher]
    :map/feature-info-dispatcher          [mevents/feature-info-dispatcher]
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
    :map.layer/show-info                  [events/layer-show-info]
    :map.layer/close-info                 events/layer-close-info
    :map.layer/update-metadata            events/layer-receive-metadata
    :map.layer/metadata-error             events/layer-receive-metadata-err
    :map.layer/download                   events/download-show-link
    :map.layer/opacity-changed            [mevents/layer-set-opacity]
    :map.layer/metadata-click             (fn [db [_ {:keys [_link _layer]}]] db)
    :map.layers/filter                    [mevents/map-set-layer-filter]
    :map.layers/others-filter             mevents/map-set-others-layer-filter
    :map.layer/get-legend                 [mevents/get-layer-legend]
    :map.layer/get-legend-success         mevents/get-layer-legend-success
    :map.layer/get-legend-error           mevents/get-layer-legend-error
    :map.layer.legend/toggle              [mevents/toggle-legend-display]
    :map.layer.selection/enable           [mevents/map-start-selecting]
    :map.layer.selection/disable          mevents/map-cancel-selecting
    :map.layer.selection/clear            mevents/map-clear-selection
    :map.layer.selection/maybe-clear      [mevents/map-maybe-clear-selection]
    :map.layer.selection/finalise         [mevents/map-finalise-selection]
    :map.layer.selection/toggle           [mevents/map-toggle-selecting]
    :map.rich-layer/tab                   [mevents/rich-layer-tab]
    :map.rich-layer/alternate-views-selected      [mevents/rich-layer-alternate-views-selected]
    :map.rich-layer/timeline-selected             [mevents/rich-layer-timeline-selected]
    :map.rich-layer/control-selected              [mevents/rich-layer-control-selected]
    :map.rich-layer/reset-filters                 [mevents/rich-layer-reset-filters]
    :map.rich-layer/configure                     [mevents/rich-layer-configure]
    :map.rich-layer/get-cql-filter-values         [mevents/rich-layer-get-cql-filter-values]
    :map.rich-layer/get-cql-filter-values-success mevents/rich-layer-get-cql-filter-values-success
    :map.region-stats/select-habitat      mevents/region-stats-select-habitat
    :map/update-base-layers               mevents/update-base-layers
    :map/update-base-layer-groups         mevents/update-base-layer-groups
    :map/update-grouped-base-layers       mevents/update-grouped-base-layers
    :map/update-layers                    mevents/update-layers
    :map/update-organisations             mevents/update-organisations
    :map/update-classifications           mevents/update-classifications
    :map/update-descriptors               mevents/update-descriptors
    :map/update-categories                mevents/update-categories
    :map/update-keyed-layers              mevents/update-keyed-layers
    :map/update-rich-layers               mevents/update-rich-layers
    :map/update-region-reports            mevents/update-region-reports
    :map/update-preview-layer             mevents/update-preview-layer
    :map/initialise-display               [mevents/show-initial-layers]
    :map/join-keyed-layers                mevents/join-keyed-layers
    :map/join-rich-layers                 mevents/join-rich-layers
    :map/pan-to-layer                     [mevents/zoom-to-layer]
    :map/zoom-in                          [mevents/map-zoom-in]
    :map/zoom-out                         [mevents/map-zoom-out]
    :map.print/error                      [mevents/map-print-error]
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
    :sok/update-active-zone-id            [sokevents/update-active-zone-id]
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
    :sok/get-filtered-bounds              [sokevents/get-filtered-bounds]
    :sok/got-filtered-bounds              [sokevents/got-filtered-bounds]
    :sok/habitat-toggle-show-layers       [sokevents/habitat-toggle-show-layers]
    :sok/bathymetry-toggle-show-layers    [sokevents/bathymetry-toggle-show-layers]
    :sok/habitat-observations-toggle-show-layers [sokevents/habitat-observations-toggle-show-layers]
    :sm/update-featured-maps              smevents/update-featured-maps
    :sm/featured-map                      [smevents/featured-map]
    :sm.featured-map/open                 [smevents/featured-map-open]
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
    :ui.right-sidebar/push                events/right-sidebar-push
    :ui.right-sidebar/pop                 events/right-sidebar-pop
    :ui.right-sidebar/bring-to-front      events/right-sidebar-bring-to-front
    :ui.right-sidebar/remove              events/right-sidebar-remove
    :ui/open-pill                         events/open-pill
    :ui/mouse-pos                         events/mouse-pos
    :ui/settings-overlay                  events/settings-overlay
    :imas-seamap.components/selection-list-reorder [events/selection-list-reorder]
    :left-drawer/toggle                   [events/left-drawer-toggle]
    :left-drawer/open                     [events/left-drawer-open]
    :left-drawer/close                    [events/left-drawer-close]
    :left-drawer/tab                      [events/left-drawer-tab]
    :layers-search-omnibar/toggle         events/layers-search-omnibar-toggle
    :layers-search-omnibar/open           events/layers-search-omnibar-open
    :layers-search-omnibar/close          events/layers-search-omnibar-close
    :download-click                       (fn [db [_ {:keys [_link]}]] db)}})

(def events-for-analytics
  [:help-layer/open
   :map.layer/load-error
   :map/clicked
   :map/pan-to-layer
   :map.layer/metadata-click
   :map/add-layer
   :map/remove-layer
   :map/toggle-layer-visibility
   :transect.plot/toggle-visibility
   :transect/query
   :download-click])

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
                              :maybe-autosave
                              :cookie-set-no-on-success
                              :map/view-updated))
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
  (js/document.body.classList.add "seamap")
  (.render
   root
   (r/as-element [hotkeys-provider
                  {:renderDialog
                   (fn [state context-actions]
                     (r/as-element
                      [components/hotkeys-render-dialog
                       {:state           (js->clj state :keywordize-keys true)
                        :context-actions (js->clj context-actions :keywordize-keys true)}]))}
                  [:f> views/layout-app]])))

(defn ^:export show-db []
  @re-frame.db/app-db)

(defn ^:export init [api-url-base media-url-base wordpress-url-base img-url-base]
  (register-handlers! config-handlers)
  (re-frame/dispatch-sync [:boot api-url-base media-url-base wordpress-url-base img-url-base])
  (dev-setup)
  (mount-root))

(defn ^:dev/after-load re-render
  []
  ;; The `:dev/after-load` metadata causes this function to be called
  ;; after shadow-cljs hot-reloads code.
  ;; This function is called implicitly by its annotation.
  (mount-root))

