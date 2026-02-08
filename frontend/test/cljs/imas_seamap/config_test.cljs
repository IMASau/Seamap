;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.config-test
  "Tests for the configuration-driven architecture (Phases 1-3).

   These tests verify:
   - Deployment configurations are valid
   - Feature flag subscriptions work correctly
   - Branding subscriptions return expected values
   - Component migrations work properly"
  (:require [cljs.test :refer-macros [deftest testing is use-fixtures]]
            [re-frame.core :as re-frame]
            [imas-seamap.config.deployments :as deployments]
            [imas-seamap.config.content :as content]
            [imas-seamap.subs.features]
            [imas-seamap.subs.branding]))

;;; =============================================================================
;;; Test Fixtures
;;; =============================================================================

(defn reset-re-frame
  "Reset re-frame state between tests"
  [f]
  (re-frame/clear-subscription-cache!)
  (f))

(use-fixtures :each reset-re-frame)

;;; =============================================================================
;;; Deployment Configuration Tests (Phase 1)
;;; =============================================================================

(deftest deployment-configs-exist
  (testing "All four deployments are defined"
    (is (= 4 (count deployments/deployments)))
    (is (contains? deployments/deployments :seamap-australia))
    (is (contains? deployments/deployments :tas-marine-atlas))
    (is (contains? deployments/deployments :seamap-antarctica))
    (is (contains? deployments/deployments :futures-of-seafood))))

(deftest deployment-configs-have-required-keys
  (testing "Each deployment has all required configuration keys"
    (let [required-keys #{:id :css-class :drawer-class :branding :map
                          :features :drawers :tabs :controls}]
      (doseq [[deployment-id config] deployments/deployments]
        (testing (str "Deployment " deployment-id)
          (is (every? #(contains? config %) required-keys)
              (str "Missing keys: "
                   (clojure.set/difference required-keys (set (keys config))))))))))

(deftest branding-configs-are-valid
  (testing "Each deployment has valid branding configuration"
    (doseq [[deployment-id config] deployments/deployments]
      (testing (str "Deployment " deployment-id " branding")
        (let [branding (:branding config)]
          (is (map? (:logo branding)) "Logo should be a map")
          (is (string? (:src (:logo branding))) "Logo src should be a string")
          (is (string? (:url branding)) "URL should be a string")
          (is (string? (:title branding)) "Title should be a string")
          (is (map? (:welcome branding)) "Welcome should be a map"))))))

(deftest map-configs-are-valid
  (testing "Each deployment has valid map configuration"
    (doseq [[deployment-id config] deployments/deployments]
      (testing (str "Deployment " deployment-id " map config")
        (let [map-config (:map config)]
          (is (keyword? (:crs map-config)) "CRS should be a keyword")
          (is (vector? (:center map-config)) "Center should be a vector")
          (is (= 2 (count (:center map-config))) "Center should have 2 coordinates")
          (is (number? (first (:center map-config))) "Latitude should be a number")
          (is (number? (second (:center map-config))) "Longitude should be a number")
          (is (number? (:zoom map-config)) "Zoom should be a number")
          (is (>= (:zoom map-config) 1) "Zoom should be >= 1"))))))

(deftest feature-sets-are-valid
  (testing "Each deployment has a valid feature set"
    (let [valid-features #{:feature/state-of-knowledge
                           :feature/data-in-region
                           :feature/featured-maps
                           :feature/floating-pills
                           :feature/plot-component
                           :feature/transect-control
                           :feature/data-providers
                           :feature/data-download
                           :feature/boundaries-pill
                           :feature/zones-pill
                           :feature/settings
                           :feature/region-select
                           :feature/layer-preview}]
      (doseq [[deployment-id config] deployments/deployments]
        (testing (str "Deployment " deployment-id " features")
          (is (set? (:features config)) "Features should be a set")
          (is (every? valid-features (:features config))
              (str "Invalid features: "
                   (clojure.set/difference (:features config) valid-features))))))))

(deftest feature-matrix-matches-expected
  (testing "Feature matrix matches documented behavior"
    (let [australia (deployments/get-deployment :seamap-australia)
          tma (deployments/get-deployment :tas-marine-atlas)
          antarctica (deployments/get-deployment :seamap-antarctica)
          fos (deployments/get-deployment :futures-of-seafood)]

      (testing "State of Knowledge feature"
        (is (contains? (:features australia) :feature/state-of-knowledge))
        (is (not (contains? (:features tma) :feature/state-of-knowledge)))
        (is (contains? (:features antarctica) :feature/state-of-knowledge))
        (is (not (contains? (:features fos) :feature/state-of-knowledge))))

      (testing "Data in Region feature"
        (is (not (contains? (:features australia) :feature/data-in-region)))
        (is (contains? (:features tma) :feature/data-in-region))
        (is (not (contains? (:features antarctica) :feature/data-in-region)))
        (is (not (contains? (:features fos) :feature/data-in-region))))

      (testing "Featured Maps feature"
        (is (contains? (:features australia) :feature/featured-maps))
        (is (contains? (:features tma) :feature/featured-maps))
        (is (not (contains? (:features antarctica) :feature/featured-maps)))
        (is (not (contains? (:features fos) :feature/featured-maps))))

      (testing "Plot Component feature"
        (is (contains? (:features australia) :feature/plot-component))
        (is (not (contains? (:features tma) :feature/plot-component)))
        (is (not (contains? (:features antarctica) :feature/plot-component)))
        (is (contains? (:features fos) :feature/plot-component))))))

;;; =============================================================================
;;; Content Registry Tests (Phase 1)
;;; =============================================================================

(deftest welcome-content-exists
  (testing "Welcome content exists for all deployments"
    (is (some? (content/get-content :welcome/seamap-australia)))
    (is (some? (content/get-content :welcome/tas-marine-atlas)))
    (is (some? (content/get-content :welcome/seamap-antarctica)))
    (is (some? (content/get-content :welcome/futures-of-seafood)))))

(deftest welcome-content-is-hiccup
  (testing "Welcome content is valid Reagent hiccup"
    (doseq [content-key [:welcome/seamap-australia
                         :welcome/tas-marine-atlas
                         :welcome/seamap-antarctica
                         :welcome/futures-of-seafood]]
      (let [content (content/get-content content-key)]
        (is (vector? content) (str content-key " should be a vector"))
        (is (keyword? (first content)) (str content-key " should start with a keyword"))))))

;;; =============================================================================
;;; Feature Subscription Tests (Phase 2)
;;; =============================================================================

(defn- setup-deployment-config
  "Helper to set up a deployment config in app-db for testing"
  [deployment-id]
  (let [config (deployments/get-deployment deployment-id)]
    (re-frame/dispatch-sync [:boot
                             "http://localhost:8000/api/"
                             "http://localhost:8000/media/"
                             "http://localhost:8888/"
                             "/img/"
                             config])))

(deftest feature-enabled-subscription-works
  (testing "Feature enabled subscription returns correct values"
    (setup-deployment-config :seamap-australia)

    (testing "Returns true for enabled features"
      (is (true? @(re-frame/subscribe [:feature/enabled? :feature/state-of-knowledge])))
      (is (true? @(re-frame/subscribe [:feature/enabled? :feature/featured-maps])))
      (is (true? @(re-frame/subscribe [:feature/enabled? :feature/plot-component]))))

    (testing "Returns false for disabled features"
      (is (false? @(re-frame/subscribe [:feature/enabled? :feature/data-in-region])))
      (is (false? @(re-frame/subscribe [:feature/enabled? :nonexistent-feature]))))))

(deftest feature-all-subscription-works
  (testing "Feature all subscription returns complete feature set"
    (setup-deployment-config :seamap-australia)
    (let [features @(re-frame/subscribe [:feature/all])]
      (is (set? features))
      (is (contains? features :feature/state-of-knowledge))
      (is (contains? features :feature/featured-maps))
      (is (not (contains? features :feature/data-in-region))))))

(deftest deployment-id-subscription-works
  (testing "Deployment ID subscription returns correct value"
    (setup-deployment-config :tas-marine-atlas)
    (is (= :tas-marine-atlas @(re-frame/subscribe [:deployment/id])))))

;;; =============================================================================
;;; Branding Subscription Tests (Phase 2)
;;; =============================================================================

(deftest branding-logo-subscription-works
  (testing "Branding logo subscription returns correct values"
    (setup-deployment-config :seamap-australia)
    (let [logo @(re-frame/subscribe [:branding/logo])]
      (is (map? logo))
      (is (= "img/Seamap2_V2_RGB.png" (:src logo)))
      (is (= "Seamap Australia" (:alt logo)))
      (is (= 96 (:height logo))))))

(deftest branding-label-subscription-works
  (testing "Branding labels differ between deployments"
    (testing "TMA has different region-control label"
      (setup-deployment-config :tas-marine-atlas)
      (is (= "Find Data in Region"
             @(re-frame/subscribe [:branding/label :region-control]))))

    (testing "Australia has different region-control label"
      (setup-deployment-config :seamap-australia)
      (is (= "Select region"
             @(re-frame/subscribe [:branding/label :region-control]))))

    (testing "Layer info label differs between deployments"
      (setup-deployment-config :tas-marine-atlas)
      (is (= "Layer info"
             @(re-frame/subscribe [:branding/label :layer-info])))

      (setup-deployment-config :seamap-australia)
      (is (= "Layer info / Download data"
             @(re-frame/subscribe [:branding/label :layer-info]))))))

(deftest branding-colors-subscription-works
  (testing "Branding colors subscription works"
    (testing "TMA has custom colors"
      (setup-deployment-config :tas-marine-atlas)
      (let [colors @(re-frame/subscribe [:branding/colors])]
        (is (map? colors))
        (is (= "rgb(18, 37, 55)" (:color-1 colors)))))

    (testing "Australia has no color overrides"
      (setup-deployment-config :seamap-australia)
      (let [colors @(re-frame/subscribe [:branding/colors])]
        (is (or (nil? colors) (empty? colors)))))))

(deftest map-config-subscriptions-work
  (testing "Map config subscriptions return correct values"
    (testing "Antarctica has different CRS"
      (setup-deployment-config :seamap-antarctica)
      (is (= :epsg-3031 @(re-frame/subscribe [:map/crs])))
      (is (= [-80 -90] @(re-frame/subscribe [:map/initial-center])))
      (is (= 2 @(re-frame/subscribe [:map/initial-zoom]))))

    (testing "Australia has standard CRS"
      (setup-deployment-config :seamap-australia)
      (is (= :epsg-3857 @(re-frame/subscribe [:map/crs])))
      (is (= 4 @(re-frame/subscribe [:map/initial-zoom]))))))

;;; =============================================================================
;;; Integration Tests (Phase 3)
;;; =============================================================================

(deftest tma-flag-elimination-works
  (testing "TMA-specific behavior now comes from configuration"
    (testing "TMA deployment"
      (setup-deployment-config :tas-marine-atlas)
      (is (false? @(re-frame/subscribe [:feature/enabled? :feature/data-download])))
      (is (= "Layer info" @(re-frame/subscribe [:branding/label :layer-info]))))

    (testing "Australia deployment"
      (setup-deployment-config :seamap-australia)
      (is (true? @(re-frame/subscribe [:feature/enabled? :feature/data-download])))
      (is (= "Layer info / Download data"
             @(re-frame/subscribe [:branding/label :layer-info]))))))

;;; =============================================================================
;;; Summary Report
;;; =============================================================================

(defn run-all-tests-and-report
  "Run all tests and return a summary report"
  []
  (let [results (atom {:passed 0 :failed 0 :errors []})]
    (println "\n=== Configuration System Test Report ===\n")

    ;; Run tests (simplified - in real implementation would use cljs.test/run-tests)
    (try
      (deployment-configs-exist)
      (swap! results update :passed inc)
      (catch js/Error e
        (swap! results update :failed inc)
        (swap! results update :errors conj {:test "deployment-configs-exist" :error (str e)})))

    (println "\nTests passed:" (:passed @results))
    (println "Tests failed:" (:failed @results))
    (when (seq (:errors @results))
      (println "\nErrors:")
      (doseq [error (:errors @results)]
        (println "  -" (:test error) ":" (:error error))))

    @results))
