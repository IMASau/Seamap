;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.dev.deployment-switcher
  "Development-only UI for testing the configuration-driven architecture.

   Provides a floating panel that allows switching between deployments
   and inspecting the current configuration state."
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b]
            [imas-seamap.config.deployments :as deployments]
            [imas-seamap.config :as config]))

;;; =============================================================================
;;; State
;;; =============================================================================

(defonce switcher-state
  (reagent/atom {:open? false
                 :show-details? false}))

;;; =============================================================================
;;; Helper Functions
;;; =============================================================================

(defn switch-deployment!
  "Switch to a different deployment by re-booting the app with new config"
  [deployment-id]
  (let [config (deployments/get-deployment deployment-id)]
    (re-frame/dispatch-sync
     [:boot
      "http://localhost:8000/api/"
      "http://localhost:8000/media/"
      "http://localhost:8888/"
      "/img/"
      config])
    (js/console.log "Switched to deployment:" deployment-id)))

(defn get-feature-count [deployment-id]
  (let [config (deployments/get-deployment deployment-id)]
    (count (:features config))))

;;; =============================================================================
;;; UI Components
;;; =============================================================================

(defn- deployment-card
  "Card showing deployment info and switch button"
  [deployment-id]
  (let [config (deployments/get-deployment deployment-id)
        current-id @(re-frame/subscribe [:deployment/id])
        is-current? (= deployment-id current-id)
        feature-count (count (:features config))]
    [b/card
     {:class (str "deployment-card" (when is-current? " current"))}
     [:div.deployment-header
      [:h4 (:title (:branding config))]
      (when is-current?
        [b/tag {:intent "success" :minimal true} "CURRENT"])]

     [:div.deployment-info
      [:div.info-row
       [:span.label "ID:"]
       [:span.value (name deployment-id)]]
      [:div.info-row
       [:span.label "CSS Class:"]
       [:span.value (:css-class config)]]
      [:div.info-row
       [:span.label "Features:"]
       [:span.value feature-count]]
      [:div.info-row
       [:span.label "Map CRS:"]
       [:span.value (name (:crs (:map config)))]]]

     [b/button
      {:text (if is-current? "Current Deployment" "Switch to This")
       :intent (if is-current? "success" "primary")
       :disabled is-current?
       :fill true
       :on-click #(switch-deployment! deployment-id)}]]))

(defn- feature-list
  "Display list of enabled features"
  []
  (let [features @(re-frame/subscribe [:feature/all])]
    [:div.feature-list
     [:h4 "Enabled Features (" (count features) ")"]
     (if (seq features)
       [:ul
        (for [feature (sort features)]
          ^{:key feature}
          [:li
           [b/tag {:minimal true :icon "tick"} (name feature)]])]
       [:p.bp3-text-muted "No features enabled"])]))

(defn- branding-info
  "Display current branding information"
  []
  (let [logo @(re-frame/subscribe [:branding/logo])
        title @(re-frame/subscribe [:branding/title])
        css-class @(re-frame/subscribe [:branding/css-class])
        colors @(re-frame/subscribe [:branding/colors])]
    [:div.branding-info
     [:h4 "Branding Configuration"]
     [:div.info-row
      [:span.label "Title:"]
      [:span.value title]]
     [:div.info-row
      [:span.label "CSS Class:"]
      [:span.value css-class]]
     [:div.info-row
      [:span.label "Logo:"]
      [:span.value (:src logo)]]
     (when (seq colors)
       [:div.info-row
        [:span.label "Custom Colors:"]
        [:span.value (count colors) " overrides"]])]))

(defn- subscription-tester
  "Interactive subscription tester"
  []
  (let [test-state (reagent/atom {:feature-key :state-of-knowledge
                                   :label-key :layer-info})]
    (fn []
      [:div.subscription-tester
       [:h4 "Test Subscriptions"]

       [:div.test-section
        [:h5 "Feature Test"]
        [:div.bp3-form-group
         [:label.bp3-label "Feature Key:"]
         [b/text-input
          {:value (name (:feature-key @test-state))
           :on-change #(swap! test-state assoc :feature-key
                              (keyword (.. % -target -value)))}]]
        [:div.test-result
         [:strong "Result: "]
         (let [enabled? @(re-frame/subscribe [:feature/enabled? (:feature-key @test-state)])]
           [b/tag
            {:intent (if enabled? "success" "danger")}
            (str enabled?)])]]

       [:div.test-section
        [:h5 "Branding Label Test"]
        [:div.bp3-form-group
         [:label.bp3-label "Label Key:"]
         [b/text-input
          {:value (name (:label-key @test-state))
           :on-change #(swap! test-state assoc :label-key
                              (keyword (.. % -target -value)))}]]
        [:div.test-result
         [:strong "Result: "]
         (let [label @(re-frame/subscribe [:branding/label (:label-key @test-state)])]
           [:span.value (or label "(not found)")])]]])))

(defn- switcher-panel-content
  "Main content of the switcher panel"
  []
  [:div.deployment-switcher-content
   [:div.section
    [:h3 "Available Deployments"]
    [:div.deployments-grid
     (for [deployment-id [:seamap-australia :tas-marine-atlas
                          :seamap-antarctica :futures-of-seafood]]
       ^{:key deployment-id}
       [deployment-card deployment-id])]]

   (when (:show-details? @switcher-state)
     [:<>
      [:div.section
       [feature-list]]

      [:div.section
       [branding-info]]

      [:div.section
       [subscription-tester]]])])

(defn switcher-panel
  "Main deployment switcher panel component"
  []
  (when config/debug?  ; Only show in dev mode
    [:div.deployment-switcher
     {:class (when (:open? @switcher-state) "open")}

     ;; Toggle button
     [:button.switcher-toggle
      {:on-click #(swap! switcher-state update :open? not)
       :title "Toggle Deployment Switcher"}
      [b/icon {:icon (if (:open? @switcher-state) "cross" "settings")
               :size 20}]]

     ;; Panel
     (when (:open? @switcher-state)
       [b/card {:class "switcher-panel" :elevation 3}
        [:div.panel-header
         [:h2 "Deployment Switcher"]
         [:div.header-actions
          [b/switch
           {:checked (:show-details? @switcher-state)
            :label "Show Details"
            :on-change #(swap! switcher-state update :show-details? not)}]
          [b/button
           {:icon "cross"
            :minimal true
            :on-click #(swap! switcher-state assoc :open? false)}]]]

        [switcher-panel-content]])]))

;;; =============================================================================
;;; Styles (to be added to app.scss)
;;; =============================================================================

(def suggested-css
  "/* Deployment Switcher Styles (add to src/ui/css/app.scss) */
.deployment-switcher {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 10000;
}

.deployment-switcher .switcher-toggle {
  position: absolute;
  top: 0;
  right: 0;
  width: 50px;
  height: 50px;
  background: #137CBD;
  color: white;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
  transition: transform 0.2s;
}

.deployment-switcher .switcher-toggle:hover {
  transform: scale(1.1);
}

.deployment-switcher .switcher-panel {
  position: fixed;
  top: 80px;
  right: 20px;
  width: 800px;
  max-height: 80vh;
  overflow-y: auto;
}

.deployment-switcher .panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  border-bottom: 1px solid #e1e8ed;
  padding-bottom: 10px;
}

.deployment-switcher .header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.deployment-switcher .deployments-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.deployment-switcher .deployment-card {
  padding: 15px;
}

.deployment-switcher .deployment-card.current {
  border: 2px solid #0F9960;
}

.deployment-switcher .deployment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.deployment-switcher .deployment-info .info-row {
  display: flex;
  justify-content: space-between;
  padding: 5px 0;
  font-size: 0.9em;
}

.deployment-switcher .deployment-info .label {
  font-weight: 600;
  color: #5C7080;
}

.deployment-switcher .section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e1e8ed;
}

.deployment-switcher .feature-list ul {
  list-style: none;
  padding: 0;
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.deployment-switcher .test-section {
  margin: 10px 0;
  padding: 10px;
  background: #f5f8fa;
  border-radius: 3px;
}

.deployment-switcher .test-result {
  margin-top: 10px;
  padding: 10px;
  background: white;
  border-radius: 3px;
}")
