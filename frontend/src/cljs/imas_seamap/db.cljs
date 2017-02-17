(ns imas-seamap.db)

(def default-db
  {:map {:pos [-23.116667 132.133333]
         :zoom 4
         :layers []
         :active-layers #{}
         :controls {:transect false}}
   :transect nil
   :config {:catalogue-url "http://localhost:8000/api/layers/"}})
