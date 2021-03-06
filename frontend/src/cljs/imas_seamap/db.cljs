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
                     :legend-shown  #{}}
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
