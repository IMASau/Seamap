;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.db)

(goog-define api-url-base "http://localhost:8000/api/")
(goog-define img-url-base "/img/")

(def default-db
  {:initialised     false               ; Flag to prevent early updates
   :map             {:center          [-27.819644755 132.133333]
                     :zoom            4
                     :zoom-cutover    10
                     :bounds          {}
                     :categories      []
                     :layers          []
                     ;; Given we have to find by name, there's an argument for making this a map of name->layer - UPDATE: possibly changed with basemap layer grouping feature
                     :base-layers     []
                     :base-layer-groups []
                     :grouped-base-layers []
                     :active-base-layer nil
                     :organisations   []
                     :priorities      []
                     :priority-cutoff 2 ; priorities <= this value will be displayed in auto mode
                     :groups          []
                     :active-layers   []
                     :hidden-layers   #{}
                     :preview-layer   nil
                     :logic           {:type    :map.layer-logic/automatic
                                       :trigger :map.logic.trigger/automatic}
                     :controls        {:transect false
                                       :download nil}}
   :state-of-knowledge {:boundaries {:active-boundary nil
                                     :amp             {:networks         []
                                                       :parks            []
                                                       :zones            []
                                                       :zones-iucn       []
                                                       :active-network   nil
                                                       :active-park      nil
                                                       :active-zone      nil
                                                       :active-zone-iucn nil}
                                     :imcra           {:provincial-bioregions       []
                                                       :mesoscale-bioregions        []
                                                       :active-provincial-bioregion nil
                                                       :active-mesoscale-bioregion  nil}
                                     :meow            {:realms           []
                                                       :provinces        []
                                                       :ecoregions       []
                                                       :active-realm     nil
                                                       :active-province  nil
                                                       :active-ecoregion nil}}
                        :statistics {:habitat              {:results  []
                                                            :loading? false}
                                     :bathymetry           {:results  []
                                                            :loading? false}
                                     :habitat-observations {:global-archive nil
                                                            :sediment       nil
                                                            :squidle        nil
                                                            :loading?       false}}
                        :open?      false
                        :pill-open? false}
   :layer-state     {:loading-state {}
                     :tile-count    {}
                     :error-count   {}
                     :legend-shown  #{}
                     :opacity       {}}
   :filters         {:layers       ""
                     :other-layers ""}
   :transect        {:query      nil
                     :show?      false
                     :habitat    nil
                     :bathymetry nil}
   :region-stats    {:habitat-layer nil}
   :habitat-colours {}
   :habitat-titles  {}
   :display         {:help-overlay          false
                     :welcome-overlay       false
                     :left-drawer           false
                     :layers-search-omnibar false
                     :drawer-panel-stack    []
                     :catalogue             {}
                     :sidebar               {:collapsed false
                                             :selected  "tab-activelayers"}}
   :config          {:layer-url             (str api-url-base "layers/")
                     :base-layer-url        (str api-url-base "baselayers/")
                     :base-layer-group-url  (str api-url-base "baselayergroups/")
                     :group-url             (str api-url-base "groups/")
                     :organisation-url      (str api-url-base "organisations/")
                     :classification-url    (str api-url-base "classifications/")
                     :priority-url          (str api-url-base "priorities/")
                     :region-stats-url      (str api-url-base "habitat/regions/")
                     :descriptor-url        (str api-url-base "descriptors/")
                     :save-state-url        (str api-url-base "savestates")
                     :category-url          (str api-url-base "categories/")
                     :amp-boundaries-url    (str api-url-base "habitat/ampboundaries")
                     :imcra-boundaries-url  (str api-url-base "habitat/imcraboundaries")
                     :meow-boundaries-url   (str api-url-base "habitat/meowboundaries")
                     :habitat-statistics-url (str api-url-base "habitat/habitatstatistics")
                     :bathymetry-statistics-url (str api-url-base "habitat/bathymetrystatistics")
                     :habitat-observations-url (str api-url-base "habitat/habitatobservations")
                     :init-catalogue-state  {:tab      "cat"
                                             :expanded #{}}}})
