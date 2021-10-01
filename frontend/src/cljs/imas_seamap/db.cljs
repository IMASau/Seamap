;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.db)

(goog-define api-url-base "http://localhost:8000/api/")
(goog-define img-url-base "/img/")

(def default-db
  {:map             {:center          [-27.819644755 132.133333]
                     :zoom            4
                     :zoom-cutover    10
                     :bounds          {}
                     :layers          []
                     :base-layers     [{:name "OSM"
                                        :url "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                                        :attribution "&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors"}
                                       {:name "ESRI World shaded-relief"
                                        :url "https://server.arcgisonline.com/ArcGIS/rest/services/World_Shaded_Relief/MapServer/tile/{z}/{y}/{x}"
                                        :attribution "Tiles &copy; Esri &mdash; Source: Esri"}
                                       {:name "CartoDB Dark"
                                        :url "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
                                        :attribution "&copy; <a href=\"https://www.openstreetmap.org/copyright\">OpenStreetMap</a> contributors &copy; <a href=\"https://carto.com/attributions\">CARTO</a>"}
                                       {:name "ESRI World Imagery"
                                        :url "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
                                        :attribution "Tiles &copy; Esri &mdash; Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"}
                                       {:name "Stadia OSM Bright"
                                        :url "https://tiles.stadiamaps.com/tiles/osm_bright/{z}/{x}/{y}{r}.png"
                                        :attribution "&copy; <a href=\"https://stadiamaps.com/\">Stadia Maps</a>, &copy; <a href=\"https://openmaptiles.org/\">OpenMapTiles</a> &copy; <a href=\"http://openstreetmap.org\">OpenStreetMap</a> contributors"}
                                       {:name "GEBCO Greyscale"
                                        :url "https://tiles.arcgis.com/tiles/C8EMgrsFcRFL6LrL/arcgis/rest/services/GEBCO_grayscale_basemap_NCEI/MapServer/WMTS/tile/1.0.0/GEBCO_grayscale_basemap_NCEI/default/default028mm/{z}/{y}/{x}.jpeg"
                                        :attribution "&copy; General Bathymetric Chart of the Oceans (GEBCO); NOAA National Centers for Environmental Information (NCEI)"}
                                       ]
                     :organisations   []
                     :priorities      []
                     :priority-cutoff 2 ; priorities <= this value will be displayed in auto mode
                     :groups          []
                     :active-layers   []
                     :logic           {:type    :map.layer-logic/automatic
                                       :trigger :map.logic.trigger/automatic}
                     :controls        {:transect false
                                       :download nil}}
   :layer-state     {:loading-state {}
                     :seen-errors   #{}
                     :legend-shown  #{}
                     :base-layer    nil}
   :filters         {:layers       ""
                     :other-layers ""}
   :transect        {:query      nil
                     :show?      false
                     :habitat    nil
                     :bathymetry nil}
   :region-stats    {:habitat-layer nil}
   :habitat-colours {}
   :habitat-titles  {}
   :display         {:help-overlay    false
                     :welcome-overlay false
                     :catalogue       {:tab      "org"
                                       :expanded #{}}
                     :sidebar         {:collapsed false
                                       :selected  "tab-habitat"}}
   :config          {:layer-url          (str api-url-base "layers/")
                     :group-url          (str api-url-base "groups/")
                     :organisation-url   (str api-url-base "organisations/")
                     :classification-url (str api-url-base "classifications/")
                     :priority-url       (str api-url-base "priorities/")
                     :region-stats-url   (str api-url-base "habitat/regions/")
                     :descriptor-url     (str api-url-base "descriptors/")}})
