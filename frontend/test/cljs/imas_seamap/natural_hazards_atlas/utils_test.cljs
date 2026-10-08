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

(deftest time-in-range-uses-utc-year
  ;; THREDDS times are UTC. The year must not depend on the browser's time zone
  ;; (fails in Hobart / any non-UTC zone if the local year is used).
  (is (nhatutils/time-in-range? (js/Date.UTC 2039 11 31 23 30) 2020 2039))
  (is (nhatutils/time-in-range? (js/Date.UTC 2020 0 1 0 30) 2020 2039))
  (is (not (nhatutils/time-in-range? (js/Date.UTC 2040 0 1 0 30) 2020 2039))))

(deftest selected-time-period-respects-map-id
  ;; In compare mode each map has its own period (the event writes it per map).
  (let [db {:current-view {:selected-time-period-id "all" :is-historic? false}
            :independent-map-state
            {:map-2 {:current-view {:selected-time-period-id "short" :is-historic? false}}}}]
    (is (= "all" (:id (nhatutils/current-view-selected-time-period db))))
    (is (= "short" (:id (nhatutils/current-view-selected-time-period db :map-2))))))
