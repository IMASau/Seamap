(ns imas-seamap.utils)

;;; http://blog.jayfields.com/2011/01/clojure-select-keys-select-values-and.html
(defn select-values [map ks]
  (reduce #(conj %1 (map %2)) [] ks))

(defn bbox-intersects? [b1 b2]
  (not
   (or (> (:west b1)  (:east b2))
       (< (:east b1)  (:west b2))
       (> (:south b1) (:north b2))
       (< (:north b1) (:south b2)))))
