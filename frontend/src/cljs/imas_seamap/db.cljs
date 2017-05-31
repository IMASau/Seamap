(ns imas-seamap.db)

(def default-db
  {:map {:center [-23.116667 132.133333]
         :zoom 4
         :zoom-cutover 5
         :bounds {}
         :layers []
         :active-layers #{}
         :logic {:type    :map.layer-logic/automatic
                 :trigger :map.logic.trigger/automatic}
         :controls {:transect false}}
   :filters {:layers ""}
   :transect {:query nil
              :show? false
              :habitat nil
              :bathymetry nil}
   :display {:help-overlay false}
   :config {:catalogue-url "/api/layers/"}})
