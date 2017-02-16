(ns imas-seamap.db)

(def default-db
  {:map {:pos [51.505 -0.09]
         :zoom 3
         :layer-idx 0
         :controls {:transect false}}
   :transect nil
   :config {:catalogue-url "http://localhost:8000/api/layers/"}})
