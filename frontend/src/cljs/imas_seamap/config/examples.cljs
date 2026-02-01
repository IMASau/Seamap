;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.config.examples
  "Examples of how to use the new configuration-driven architecture.

   This namespace demonstrates the patterns from Phases 1-2:
   - How to access deployment configurations
   - How to use feature flag subscriptions
   - How to access branding information"
  (:require [re-frame.core :as re-frame]
            [imas-seamap.config.deployments :as deployments]
            [imas-seamap.subs.features :as features]))

;;; =============================================================================
;;; PHASE 1: Accessing Deployment Configurations (compile-time)
;;; =============================================================================

(comment
  ;; Get a specific deployment configuration
  (def australia-config (deployments/get-deployment :seamap-australia))

  ;; Check what features are enabled
  (:features australia-config)
  ;; => #{:state-of-knowledge :featured-maps :floating-pills ...}

  ;; Check if a feature is enabled (compile-time)
  (deployments/feature-enabled? australia-config :state-of-knowledge)
  ;; => true

  ;; Get branding information
  (get-in australia-config [:branding :logo :src])
  ;; => "img/Seamap2_V2_RGB.png"

  ;; Get all deployments
  deployments/deployments
  ;; => {:seamap-australia {...} :tas-marine-atlas {...} ...}
  )

;;; =============================================================================
;;; PHASE 2: Using Feature Subscriptions (runtime, in components)
;;; =============================================================================

(comment
  ;; Example component using feature flags
  (defn my-component []
    (let [has-sok? @(re-frame/subscribe [:feature/enabled? :state-of-knowledge])
          logo @(re-frame/subscribe [:branding/logo])
          region-label @(re-frame/subscribe [:branding/label :region-control])]
      [:div
       [:h1 "Example Component"]

       ;; Conditionally render based on feature flag
       (when has-sok?
         [:div "State of Knowledge feature is enabled!"])

       ;; Use deployment-specific branding
       [:img {:src (:src logo) :alt (:alt logo)}]
       [:button region-label]]))

  ;; Using the convenience helper function
  (defn another-component []
    [:div
     ;; This is cleaner than using subscribe directly
     (when (features/feature? :plot-component)
       [plot-display])

     (when (features/feature? :floating-pills)
       [floating-pills])])

  ;; Getting all enabled features
  (defn debug-component []
    (let [all-features @(re-frame/subscribe [:feature/all])
          deployment-id @(re-frame/subscribe [:deployment/id])]
      [:div
       [:h2 "Current Deployment: " (name deployment-id)]
       [:h3 "Enabled Features:"]
       [:ul
        (for [feature all-features]
          ^{:key feature}
          [:li (name feature)])]]))
  )

;;; =============================================================================
;;; MIGRATION PATTERNS: Replacing tma? flag
;;; =============================================================================

(comment
  ;; BEFORE (Phase 0 - current code):
  (defn old-component [{:keys [layer tma?]}]
    [:div.layer-card
     [layer-control
      {:tooltip (if tma? "Layer info" "Layer info / Download data")}]])

  ;; AFTER (Phase 2 - using feature flags):
  (defn new-component [{:keys [layer]}]
    (let [label @(re-frame/subscribe [:branding/label :layer-info])]
      [:div.layer-card
       [layer-control {:tooltip label}]]))

  ;; Or with the convenience helper:
  (defn new-component-alt [{:keys [layer]}]
    (let [show-download? (features/feature? :data-download)]
      [:div.layer-card
       [layer-control
        {:tooltip (if show-download?
                    "Layer info / Download data"
                    "Layer info")}]]))
  )

;;; =============================================================================
;;; TESTING: How to test with different configurations
;;; =============================================================================

(comment
  ;; To test a component with a specific deployment config:
  ;; 1. Dispatch :boot event with the deployment config
  (re-frame/dispatch-sync
   [:boot
    "http://localhost:8000/api/"
    "http://localhost:8000/media/"
    "http://localhost:8888/"
    "/img/"
    (deployments/get-deployment :seamap-australia)])

  ;; 2. Now all subscriptions will use Australia's config
  @(re-frame/subscribe [:feature/enabled? :state-of-knowledge])
  ;; => true

  ;; 3. To test with a different deployment:
  (re-frame/dispatch-sync
   [:boot
    "http://localhost:8000/api/"
    "http://localhost:8000/media/"
    "http://localhost:8888/"
    "/img/"
    (deployments/get-deployment :tas-marine-atlas)])

  @(re-frame/subscribe [:feature/enabled? :state-of-knowledge])
  ;; => false (TMA doesn't have state-of-knowledge)

  @(re-frame/subscribe [:feature/enabled? :data-in-region])
  ;; => true (TMA has data-in-region)
  )

;;; =============================================================================
;;; VALIDATION: Check that all deployments have consistent configs
;;; =============================================================================

(defn validate-deployments
  "Validates that all deployment configurations are properly structured.
   Returns a vector of error messages, or empty vector if valid."
  []
  (let [required-keys #{:id :css-class :drawer-class :branding :map :features :drawers :tabs :controls}]
    (reduce
     (fn [errors [deployment-id config]]
       (let [missing-keys (set/difference required-keys (set (keys config)))]
         (if (seq missing-keys)
           (conj errors (str "Deployment " deployment-id " is missing keys: " missing-keys))
           errors)))
     []
     deployments/deployments)))

(comment
  ;; Run validation
  (validate-deployments)
  ;; => [] (empty means no errors)

  ;; Check specific deployment features
  (doseq [[id config] deployments/deployments]
    (println id "has" (count (:features config)) "features:"))
  ;; seamap-australia has 9 features
  ;; tas-marine-atlas has 3 features
  ;; seamap-antarctica has 4 features
  ;; futures-of-seafood has 2 features
  )
