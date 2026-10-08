(ns imas-seamap.natural-hazards-atlas.subs-test
  (:require [cljs.test :refer [deftest is testing]]
            [imas-seamap.natural-hazards-atlas.subs :as nhasubs]))

(def ^:private ds-map-1
  {:cmip_phase "C6" :scientific_model "M" :scenario nil :season "annual" :is_historical true
   :color_scale_range_min 1 :color_scale_range_max 5})

(def ^:private layer {:hazardlayer {:datasets [ds-map-1]}})

(deftest color-scale-range-survives-a-map-without-data
  ;; Map 2 is set to a model with no dataset for this layer. Its missing range
  ;; must not blank map 1's (CLJS (min 1 nil) => nil, giving ",max").
  (let [c6 {:name "C6"} m {:name "M"} other {:name "NoData"} annual {:name "annual"}]
    (is (= {:color-scale-range-min 1 :color-scale-range-max 5}
           (nhasubs/hazard-layers-color-scale-range
            [[layer] c6 m nil annual true c6 other nil annual true]
            [nil layer])))))

(deftest color-scale-range-merges-both-maps
  (let [ds-2  (assoc ds-map-1 :scientific_model "M2" :color_scale_range_min 0 :color_scale_range_max 9)
        layer {:hazardlayer {:datasets [ds-map-1 ds-2]}}
        c6 {:name "C6"} annual {:name "annual"}]
    (testing "existing behaviour: the range spans both maps"
      (is (= {:color-scale-range-min 0 :color-scale-range-max 9}
             (nhasubs/hazard-layers-color-scale-range
              [[layer] c6 {:name "M"} nil annual true c6 {:name "M2"} nil annual true]
              [nil layer]))))))
