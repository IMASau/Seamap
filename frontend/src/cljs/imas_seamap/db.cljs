(ns imas-seamap.db)

(goog-define api-url-base "/api/")

(def default-db
  {:map {:center [-23.116667 132.133333]
         :zoom 4
         :zoom-cutover 5
         :bounds {}
         :layers []
         :priorities []
         :groups []
         :active-layers []
         :logic {:type    :map.layer-logic/automatic
                 :trigger :map.logic.trigger/automatic}
         :controls {:transect false}}
   :filters {:layers ""
             :other-layers ""}
   :transect {:query nil
              :show? false
              :habitat nil
              :bathymetry nil}
   :display {:help-overlay false}
   :config {:layer-url (str api-url-base "layers/")
            :group-url (str api-url-base "groups/")
            :priority-url (str api-url-base "priorities/")}})
