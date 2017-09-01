(ns imas-seamap.db)

(goog-define api-url-base "http://localhost:8000/api/")

(def default-db
  {:map             {:center          [-23.116667 132.133333]
                     :zoom            4
                     :zoom-cutover    5
                     :bounds          {}
                     :layers          []
                     :priorities      []
                     :priority-cutoff 2 ; priorities <= this value will be displayed in auto mode
                     :groups          []
                     :active-layers   []
                     :logic           {:type    :map.layer-logic/automatic
                                       :trigger :map.logic.trigger/automatic}
                     :controls        {:transect false}}
   :layer-state     {}
   :filters         {:layers       ""
                     :other-layers ""}
   :transect        {:query      nil
                     :show?      false
                     :habitat    nil
                     :bathymetry nil}
   :habitat-colours {}
   :habitat-titles  {}
   :display         {:help-overlay false
                     :sidebar      {:collapsed false
                                    :selected  "tab-home"}}
   :config          {:layer-url      (str api-url-base "layers/")
                     :group-url      (str api-url-base "groups/")
                     :priority-url   (str api-url-base "priorities/")
                     :descriptor-url (str api-url-base "descriptors/")}})
