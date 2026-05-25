;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.natural-hazards-atlas.db)

(def default-db
  {:initialised     false               ; Flag to prevent early updates
   :map             {:center          [-41.7 145.4]
                     :initial-bounds? true
                     :size            {}
                     :zoom            8
                     :zoom-cutover    10
                     :bounds          {}
                     :categories      []
                     :layers          []
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
                     :rich-layers     {:rich-layers  []
                                       :states       {}
                                       :async-datas  {}
                                       :layer-lookup {}}
                     :legends         {}
                     :controls        {:transect false
                                       :download nil}}
   :site-configuration nil
   :story-maps      {:featured-maps []
                     :featured-map  nil}
   :data-in-region  {:data     nil
                     :query-id nil}
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
                     :catalogue             {:main {:tab      "hazards"
                                                    :expanded #{}}
                                             :region {:tab      "hazards"
                                                      :expanded #{}}}
                     :sidebar               {:collapsed false
                                             :selected  "tab-activelayers"}
                     :right-sidebars        []
                     :open-pill             nil
                     :outage-message-open?  false
                     :split-layer-range-value nil
                     :split-layer-container-x nil
                     :current-time            nil
                     :available-times         []
                     :time-is-playing?        false
                     :time-is-loading?        false}
   :dynamic-pills {:dynamic-pills []
                   :states        {}
                   :async-datas   {}}
   :current-view  {:models                    []
                   :scenarios                 []
                   :seasonal-datas            []
                   :selected-model-id         nil
                   :selected-scenario-id      nil
                   :selected-seasonal-data-id nil
                   :selected-time-period-id   "all"}
   :autosave?       false
   :config          {:url-paths {:site-configuration    "siteconfiguration/"
                                 :layer                 "nhatlayers/" ; Has the same information as the "layers/" API endpoint, but with extra Thredds styling information that we're not using in Seamap
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
                                 :dynamic-pills         "dynamicpills/"
                                 :layer-legend          "nhatlayerlegend/" ; Uses the extra Thredds styling information we have in NHAT to build a more accurate legend than the "layerlegend/" API endpoint
                                 :layer-previews        "layer_previews/"
                                 :story-maps            "wp-json/wp/v2/story_map?acf_format=standard"
                                 :data-in-region        "habitat/datainregion"
                                 :cql-filter-values     "habitat/cqlfiltervalues"
                                 :dynamic-pill-region-control-values "habitat/dynamicpillregioncontrolvalues"
                                 :scientific-models     "nhatscientificmodels/"
                                 :scenarios             "nhatscenarios/"
                                 :seasons               "nhatseasons/"}
                     :urls      nil
                     :url-base {:api-url-base       "http://localhost:8000/api/"
                                :media-url-base     "http://localhost:8000/media/"
                                :wordpress-url-base "http://localhost:8888/"
                                :img-url-base       "/img/"}}})
