(ns imas-seamap.natural-hazards-atlas.utils-test
  (:require [cljs.test :refer [deftest is testing]]
            [imas-seamap.natural-hazards-atlas.utils :as nhatutils]))

(deftest current-view-selected-time-period-test
  (let [db {:current-view {:selected-time-period-id "medium"
                           :is-historic?            false}}]
    (testing "two-arity returns the selected time period"
      (is (= "medium" (:id (nhatutils/current-view-selected-time-period db nil)))))
    (testing "one-arity defaults map-id and returns the same time period"
      (is (= (nhatutils/current-view-selected-time-period db nil)
             (nhatutils/current-view-selected-time-period db))))))
