(ns imas-seamap.db)

(def default-db
  {:map {:center [-23.116667 132.133333]
         :zoom 4
         :zoom-cutover 5
         :bounds {}
         :layers []
         :active-layers #{}
         :controls {:transect false}}
   :transect {:query nil, :show? false}
   :display {:help-overlay false}
   :config {:catalogue-url "http://localhost:8000/api/layers/"}})
