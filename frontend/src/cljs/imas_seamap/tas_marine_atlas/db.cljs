;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tas-marine-atlas.db)

(def default-db
  {:initialised     false               ; Flag to prevent early updates
   :map             {:center          [-42.20157676555315 146.74253188097842]
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
                     :legends         {}
                     :controls        {:transect false
                                       :download nil}}
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
                     :settings-overlay      false
                     :left-drawer           false
                     :left-drawer-tab       "catalogue"
                     :layers-search-omnibar false
                     :catalogue             {:tab      "cat"
                                             :expanded #{}}
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
                                 :layer-previews        "layer_previews/"
                                 :story-maps            "wp-json/wp/v2/story_map?acf_format=standard"}
                     :urls      nil
                     :url-base {:api-url-base       "http://localhost:8000/api/"
                                :media-url-base     "http://localhost:8000/media/"
                                :wordpress-url-base "http://localhost:8888/"
                                :img-url-base       "/img/"}}})
