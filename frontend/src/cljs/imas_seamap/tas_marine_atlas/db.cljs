;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tas-marine-atlas.db)

(def default-db
  {:initialised     false               ; Flag to prevent early updates
   :map             {:center          [-42.20157676555315 146.74253188097842]
                     :initial-bounds? true
                     :size            {}
                     :zoom            7
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
   :story-maps      {:featured-maps []
                     :featured-map  nil
                     :open?         false}
   :data-in-region  {:open?    false
                     :data     nil
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
                     :welcome-overlay       true
                     :settings-overlay      false
                     :left-drawer           true
                     :left-drawer-tab       "catalogue"
                     :layers-search-omnibar false
                     :catalogue             {:main {:tab      "cat"
                                                    :expanded #{}}
                                             :region {:tab      "cat"
                                                      :expanded #{}}}
                     :sidebar               {:collapsed false
                                             :selected  "tab-activelayers"}
                     :right-sidebars        []
                     :open-pill             nil}
   :dynamic-pills {:values []
                   :states {}}
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
                                 :dynamic-pills         "dynamicpills/"
                                 :layer-previews        "layer_previews/"
                                 :story-maps            "wp-json/wp/v2/story_map?acf_format=standard"
                                 :data-in-region        "habitat/datainregion"
                                 :cql-filter-values     "habitat/cqlfiltervalues"}
                     :urls      nil
                     :url-base {:api-url-base       "http://localhost:8000/api/"
                                :media-url-base     "http://localhost:8000/media/"
                                :wordpress-url-base "http://localhost:8888/"
                                :img-url-base       "/img/"}}})
