;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tas-marine-atlas.views
  "Tasmania's Marine Atlas components that have no counterpart in the shared layout.
   Registered in the component registry so the deployment config can place them."
  (:require [imas-seamap.blueprint :as b]
            [imas-seamap.components :as components]
            [imas-seamap.config.components :as components-registry]
            [imas-seamap.map.subs :as msubs]
            [imas-seamap.views :refer [layer-catalogue]]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(defn data-in-region-drawer
  "Right drawer listing the layers with data in the selected region, as a
   catalogue (catalogue id :region)."
  [_sidebar]
  (let [{:keys [catalogue-layers active-layers visible-layers rich-layers-by-layer-id]} @(re-frame/subscribe [:map/layers])
        rich-layer-fn   #(get rich-layers-by-layer-id (:id %))
        loading-ids     @(re-frame/subscribe [::msubs/loading-layers])
        error-ids       @(re-frame/subscribe [::msubs/error-layers])
        expanded-ids    @(re-frame/subscribe [::msubs/expanded-layers])
        layer-opacities @(re-frame/subscribe [::msubs/layer-opacities])
        {:keys [status layers]} @(re-frame/subscribe [:data-in-region/data])
        ;; Filter out layers in region that have no category (ie, currently just placeholders)
        layers (filter (set layers) catalogue-layers)
        layer-props
        {:active-layers  active-layers
         :visible-layers visible-layers
         :loading-fn     #(contains? loading-ids (:id %))
         :error-fn       #(contains? error-ids (:id %))
         :expanded-fn    #(contains? expanded-ids (:id %))
         :opacity-fn     #(get layer-opacities (:id %) 100)
         :rich-layer-fn  rich-layer-fn}]
    [components/drawer
     {:title       "Data in Region"
      :position    "right"
      :size        "368px"
      :isOpen      true
      :onClose     #(re-frame/dispatch [:data-in-region/open false])
      :hasBackdrop false
      :className   "data-in-region-drawer"}
     (case status

       :data-in-region/loaded
       [layer-catalogue :region layers layer-props]

       :data-in-region/loading
       [b/non-ideal-state
        {:icon  (reagent/as-element [b/spinner {:intent "success"}])}]

       :data-in-region/none
       [b/non-ideal-state
        {:title       "No Data"
         :description "We are unable to display any region data at this time."
         :icon        "info-sign"}])]))

(components-registry/register-components!
 {:data-in-region/panel data-in-region-drawer})
