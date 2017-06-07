(ns imas-seamap.imgview.subs)

(defn survey-images [db [_ survey-key]]
  (get-in db [:survey survey-key]))
