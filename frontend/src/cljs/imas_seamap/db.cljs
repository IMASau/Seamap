(ns imas-seamap.db)

(def default-db
  {:map {:pos [51.505 -0.09]
         :zoom 3
         :layer-idx 0
         :controls {:transect false}
         :markers [{:pos [51.505 -0.09] :title "One"}
                   {:pos [51.507 -0.10] :title "Two"}]}
   :transect nil
   :config {:catalogue-url "http://localhost:8000/layers/"}})
