;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.subs.features
  "Feature flag subscriptions for deployment-specific functionality.

   These subscriptions provide a clean way to check which features are enabled
   for the current deployment, eliminating the need to pass flags like `tma?`
   through multiple component layers."
  (:require [re-frame.core :as re-frame]))

;;; =============================================================================
;;; DEPLOYMENT CONFIG SUBSCRIPTIONS
;;; =============================================================================

;; Returns the complete deployment configuration from app-db.
;; This includes all settings: features, branding, map config, etc.
(re-frame/reg-sub
 :deployment/config
 (fn [db _]
   (get db :deployment-config)))

;; Returns the current deployment ID (e.g., :seamap-australia)
(re-frame/reg-sub
 :deployment/id
 :<- [:deployment/config]
 (fn [config _]
   (:id config)))

;;; =============================================================================
;;; FEATURE FLAG SUBSCRIPTIONS
;;; =============================================================================

;; Check if a specific feature is enabled for the current deployment.
;;
;; Usage in components:
;;   (let [has-sok? @(re-frame/subscribe [:feature/enabled? :state-of-knowledge])]
;;     (when has-sok?
;;       [state-of-knowledge-panel]))
;;
;; Or using the helper function:
;;   (when (feature? :state-of-knowledge)
;;     [state-of-knowledge-panel])
(re-frame/reg-sub
 :feature/enabled?
 :<- [:deployment/config]
 (fn [config [_ feature-key]]
   (contains? (:features config) feature-key)))

;; Returns the set of all enabled features for the current deployment.
;; Useful for debugging or displaying feature information.
(re-frame/reg-sub
 :feature/all
 :<- [:deployment/config]
 (fn [config _]
   (:features config)))

;; Check if a specific feature is disabled (inverse of :feature/enabled?).
;; Convenience subscription for conditional logic.
(re-frame/reg-sub
 :feature/disabled?
 :<- [:deployment/config]
 (fn [config [_ feature-key]]
   (not (contains? (:features config) feature-key))))

;;; =============================================================================
;;; HELPER FUNCTIONS (for use in components)
;;; =============================================================================

(defn feature?
  "Convenience function to check if a feature is enabled.
   Returns a boolean value immediately (derefs the subscription).

  Usage:
    (when (feature? :plot-component)
      [plot-display])

  This is cleaner than:
    (when @(re-frame/subscribe [:feature/enabled? :plot-component])
      [plot-display])"
  [feature-key]
  @(re-frame/subscribe [:feature/enabled? feature-key]))

(defn features
  "Returns the set of all enabled features.
   Convenience function that derefs the subscription."
  []
  @(re-frame/subscribe [:feature/all]))
