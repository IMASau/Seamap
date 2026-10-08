(ns imas-seamap.map.utils-test
  (:require [cljs.test :refer [deftest is testing]]
            [clojure.string :as string]
            [imas-seamap.map.utils :as mutils]))

;; Real THREDDS (ncWMS) GetFeatureInfo responses from thredds-nhat-dev,
;; heatwave_frequency historical annual, 8 Oct 2026.
(def ^:private land-response
  "<FeatureInfoResponse>
    <longitude>146.5</longitude>
    <latitude>-42.0</latitude>
    <Feature>
        <layer>ensemble_median</layer>
        <FeatureInfo>
            <id>ensemble_median</id>
            <time>1970-06-01T00:00:00.000Z</time>
            <value>9.75</value>
        </FeatureInfo>
    </Feature>
</FeatureInfoResponse>")

;; A point outside the data (at sea): no <Feature> element at all.
(def ^:private sea-response
  "<FeatureInfoResponse>
    <longitude>148.30444335937503</longitude>
    <latitude>-42.39912215986002</latitude>
</FeatureInfoResponse>")

(def ^:private hazard-layer
  {:name        "heatwave_frequency"
   :hazardlayer {:human_readable_units "days"}})

(deftest hazard-layer-feature-info-response->display-test
  (testing "a point with data shows the value and units"
    (let [{:keys [body]} (mutils/hazard-layer-feature-info-response->display land-response hazard-layer)]
      (is (string/includes? body "heatwave_frequency"))
      (is (string/includes? body "9.75 days"))))
  (testing "a point outside the data returns nil (nothing to show) rather than throwing"
    (is (nil? (mutils/hazard-layer-feature-info-response->display sea-response hazard-layer)))))
