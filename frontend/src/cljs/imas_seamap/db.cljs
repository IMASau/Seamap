(ns imas-seamap.db)

(def default-db
  {:name "re-frame"
   :map {:pos [51.505 -0.09]
         :zoom 13
         :markers [{:pos [51.505 -0.09] :title "One"}
                   {:pos [51.507 -0.10] :title "Two"}]}})
