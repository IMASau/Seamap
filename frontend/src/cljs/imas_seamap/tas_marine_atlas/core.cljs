;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tas-marine-atlas.core
  (:require [imas-seamap.core :refer [root]]
            [reagent.core :as r]
            [re-frame.core :as re-frame]
            [re-frame.db]
            [com.smxemail.re-frame-cookie-fx]
            [day8.re-frame.async-flow-fx]
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
            [imas-seamap.story-maps.events :as smevents]
            [imas-seamap.story-maps.subs :as smsubs]
            [imas-seamap.protocols]
            [imas-seamap.subs :as subs]
            [imas-seamap.tas-marine-atlas.subs :as tmasubs]
            [imas-seamap.tas-marine-atlas.views :refer [layout-app]]
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
    :sok/boundary-layer-filter            (fn [] #(identity nil))
    :sm/featured-maps                     smsubs/featured-maps
    :sm/featured-map                      smsubs/featured-map
    :sm.featured-map/open?                smsubs/featured-map-open?
    :sorting/info                         subs/sorting-info
    :download/info                        subs/download-info
    :transect/info                        subs/transect-info
    :transect/results                     subs/transect-results
    :transect.plot/show?                  subs/transect-show?
    :help-layer/open?                     subs/help-layer-open?
    :welcome-layer/open?                  tmasubs/welcome-layer-open?
    :left-drawer/open?                    subs/left-drawer-open?
    :left-drawer/tab                      subs/left-drawer-tab
    :layers-search-omnibar/open?          subs/layers-search-omnibar-open?
    :ui.catalogue/tab                     subs/catalogue-tab
    :ui.catalogue/nodes                   subs/catalogue-nodes
    :ui/preview-layer-url                 subs/preview-layer-url
    :ui/sidebar                           subs/sidebar-state
    :ui/mouse-pos                         subs/mouse-pos
    :ui/settings-overlay                  subs/settings-overlay
    :app/loading?                         subs/app-loading?
    :app/load-normal-msg                  subs/load-normal-msg
    :app/load-error-msg                   subs/load-error-msg
    :info/message                         subs/user-message
    :autosave?                            subs/autosave?
    :url-base                             subs/url-base
    :data-in-region/open?                 tmasubs/data-in-region-open?
    :data-in-region/data                  tmasubs/data-in-region}

   :events
   {:boot                                 [tmaevents/boot (re-frame/inject-cofx :save-code) (re-frame/inject-cofx :hash-code) (re-frame/inject-cofx :local-storage/get [:seamap-app-state])]
    :construct-urls                       tmaevents/construct-urls
    :merge-state                          [tmaevents/merge-state]
    :re-boot                              [tmaevents/re-boot]
    :ajax/default-success-handler         (fn [db [_ arg]] (js/console.log arg) db)
    :ajax/default-err-handler             (fn [db [_ arg]] (js/console.error arg) db)
    ;;; we ignore success/failure of cookie setting; these are fired by default, so just ignore:
    :cookie-set-no-on-success             identity
    :cookie-set-no-on-failure             identity
    :load-hash-state                      [events/load-hash-state]
    :get-save-state                       [events/get-save-state]
    :get-save-state-success               [events/get-save-state-success]
    :initialise-db                        [events/initialise-db]
    :initialise-layers                    [tmaevents/initialise-layers]
    :loading-failed                       events/loading-failed
    :help-layer/toggle                    events/help-layer-toggle
    :help-layer/open                      events/help-layer-open
    :help-layer/close                     events/help-layer-close
    :welcome-layer/close                  tmaevents/welcome-layer-close
    :create-save-state                    [tmaevents/create-save-state]
    :create-save-state-success            [events/create-save-state-success]
    :create-save-state-failure            [events/create-save-state-failure]
    :toggle-autosave                      [events/toggle-autosave]
    :maybe-autosave                       [tmaevents/maybe-autosave]
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
    :map.layer.selection/enable           tmaevents/map-start-selecting
    :map.layer.selection/disable          mevents/map-cancel-selecting
    :map.layer.selection/clear            [tmaevents/map-clear-selection]
    :map.layer.selection/finalise         [tmaevents/map-finalise-selection]
    :map.layer.selection/toggle           [mevents/map-toggle-selecting]
    :map.rich-layer/tab                   [mevents/rich-layer-tab]
    :map.rich-layer/alternate-views-selected [mevents/rich-layer-alternate-views-selected]
    :map.rich-layer/timeline-selected        [mevents/rich-layer-timeline-selected]
    :map.rich-layer/reset-filters            [mevents/rich-layer-reset-filters]
    :map.rich-layer/configure                [mevents/rich-layer-configure]
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
    :map/update-preview-layer             mevents/update-preview-layer
    :map/initialise-display               [tmaevents/show-initial-layers]
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
    :sm/update-featured-maps              smevents/update-featured-maps
    :sm/featured-map                      [smevents/featured-map]
    :sm.featured-map/open                 [smevents/featured-map-open]
    :ui/show-loading                      tmaevents/loading-screen
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
    :data-in-region/open                  [tmaevents/data-in-region-open]
    :data-in-region/get                   [tmaevents/get-data-in-region]
    :data-in-region/got                   tmaevents/got-data-in-region}})

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
                              :load-hash-state
                              :map/update-map-view
                              :map/initialise-display
                              :transect/maybe-query
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

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (Blueprint/FocusStyleManager.onlyShowFocusOnTabs)
  (js/document.body.classList.add "tas-marine-atlas")
  (.render
   root
   (r/as-element [hotkeys-provider
                  {:renderDialog
                   (fn [state context-actions]
                     (r/as-element
                      [components/hotkeys-render-dialog
                       {:state           (js->clj state :keywordize-keys true)
                        :context-actions (js->clj context-actions :keywordize-keys true)}]))}
                  [:f> layout-app]])))

(defn ^:export show-db []
  @re-frame.db/app-db)

(defn ^:export init [api-url-base media-url-base wordpress-url-base img-url-base]
  (register-handlers! config-handlers)
  (re-frame/dispatch-sync [:boot api-url-base media-url-base wordpress-url-base img-url-base])
  (dev-setup)
  (mount-root))

;; (defn ^:dev/after-load re-render
;;   []
;;   ;; The `:dev/after-load` metadata causes this function to be called
;;   ;; after shadow-cljs hot-reloads code.
;;   ;; This function is called implicitly by its annotation.
;;   (mount-root))

