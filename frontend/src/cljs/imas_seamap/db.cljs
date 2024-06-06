;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.db)

(def default-db
  {:initialised     false               ; Flag to prevent early updates
   :map             {:center          [-27.819644755 132.133333]
                     :initial-bounds? true
                     :size            {}
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
                     :active-layers   []
                     :hidden-layers   #{}
                     :preview-layer   nil
                     :viewport-only?  false
                     :keyed-layers    {}
                     :rich-layers     {}
                     :legends         {}
                     :controls        {:transect false
                                       :download nil}}
   :state-of-knowledge {:boundaries {:active-boundary nil
                                     :active-boundary-layer nil
                                     :amp             {:active-network   nil
                                                       :active-park      nil
                                                       :active-zone      nil
                                                       :active-zone-iucn nil
                                                       :active-zone-id   nil}
                                     :imcra           {:active-provincial-bioregion nil
                                                       :active-mesoscale-bioregion  nil}
                                     :meow            {:active-realm     nil
                                                       :active-province  nil
                                                       :active-ecoregion nil}}
                        :statistics {:habitat              {:results      []
                                                            :loading?     false
                                                            :show-layers? false}
                                     :bathymetry           {:results      []
                                                            :loading?     false
                                                            :show-layers? false}
                                     :habitat-observations {:global-archive nil
                                                            :sediment       nil
                                                            :squidle        nil
                                                            :loading?       false
                                                            :show-layers?   false}}
                        :open-pill  nil}
   :story-maps      {:featured-maps []
                     :featured-map  nil
                     :open?         false}
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
   :display         {:mouse-pos             {}
                     :help-overlay          false
                     :welcome-overlay       false
                     :settings-overlay      false
                     :left-drawer           true
                     :left-drawer-tab       "catalogue"
                     :layers-search-omnibar false
                     :catalogue             {:main {:tab      "cat"
                                                    :expanded #{}}}
                     :sidebar               {:collapsed false
                                             :selected  "tab-activelayers"}}
   :autosave?       false
   :config          {:url-paths {:layer                 "layers/"
                                 :base-layer            "baselayers/"
                                 :base-layer-group      "baselayergroups/"
                                 :organisation          "organisations/"
                                 :classification        "classifications/"
                                 :region-stats          "habitat/regions/"
                                 :descriptor            "descriptors/"
                                 :save-state            "savestates"
                                 :category              "categories/"
                                 :keyed-layers          "keyedlayers/"
                                 :rich-layers           "richlayers/"
                                 :region-reports        "regionreports/"
                                 :amp-boundaries        "habitat/ampboundaries"
                                 :imcra-boundaries      "habitat/imcraboundaries"
                                 :meow-boundaries       "habitat/meowboundaries"
                                 :habitat-statistics    "habitat/habitatstatistics"
                                 :bathymetry-statistics "habitat/bathymetrystatistics"
                                 :habitat-observations  "habitat/habitatobservations"
                                 :cql-filter-values     "habitat/cqlfiltervalues"
                                 :layer-previews        "layer_previews/"
                                 :story-maps            "wp-json/wp/v2/story_map?acf_format=standard"
                                 :region-report-pages   "region-reports/"}
                     :urls      nil
                     :url-base {:api-url-base       "http://localhost:8000/api/"
                                :media-url-base     "http://localhost:8000/media/"
                                :wordpress-url-base "http://localhost:8888/"
                                :img-url-base       "/img/"}}})
