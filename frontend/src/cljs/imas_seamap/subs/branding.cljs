;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.subs.branding
  "Branding subscriptions for deployment-specific UI elements.

   These subscriptions provide access to deployment-specific branding:
   logos, colors, labels, welcome content, etc."
  (:require [re-frame.core :as re-frame]
            [imas-seamap.config.content :as content]))

;;; =============================================================================
;;; BRANDING CONFIG SUBSCRIPTIONS
;;; =============================================================================

;; Returns the complete branding configuration for the current deployment.
(re-frame/reg-sub
 :branding/config
 :<- [:deployment/config]
 (fn [config _]
   (:branding config)))

;; Returns the logo configuration (src, alt, height).
(re-frame/reg-sub
 :branding/logo
 :<- [:branding/config]
 (fn [branding _]
   (:logo branding)))

;; Returns the main website URL for the deployment.
(re-frame/reg-sub
 :branding/url
 :<- [:branding/config]
 (fn [branding _]
   (:url branding)))

;; Returns the deployment title.
(re-frame/reg-sub
 :branding/title
 :<- [:branding/config]
 (fn [branding _]
   (:title branding)))

;;; =============================================================================
;;; LABEL SUBSCRIPTIONS
;;; =============================================================================

;; Get a deployment-specific label by key.
;;
;; Usage:
;;   @(re-frame/subscribe [:branding/label :region-control])
;;   => "Find Data in Region" (for TMA)
;;   => "Select region" (for Australia)
(re-frame/reg-sub
 :branding/label
 :<- [:branding/config]
 (fn [branding [_ label-key]]
   (get-in branding [:labels label-key])))

;; Returns all labels for the deployment.
(re-frame/reg-sub
 :branding/labels
 :<- [:branding/config]
 (fn [branding _]
   (:labels branding)))

;;; =============================================================================
;;; COLOR SUBSCRIPTIONS
;;; =============================================================================

;; Returns the color overrides for the deployment.
;; Used to generate CSS custom properties.
(re-frame/reg-sub
 :branding/colors
 :<- [:deployment/config]
 (fn [config _]
   (:colors config)))

;; Returns the CSS class for the deployment (e.g., "seamap", "tas-marine-atlas").
(re-frame/reg-sub
 :branding/css-class
 :<- [:deployment/config]
 (fn [config _]
   (:css-class config)))

;; Returns the drawer CSS class for the deployment.
(re-frame/reg-sub
 :branding/drawer-class
 :<- [:deployment/config]
 (fn [config _]
   (:drawer-class config)))

;;; =============================================================================
;;; WELCOME CONTENT SUBSCRIPTIONS
;;; =============================================================================

;; Returns the welcome dialogue configuration.
;; Includes title and content (which may be a keyword or hiccup).
(re-frame/reg-sub
 :branding/welcome
 :<- [:branding/config]
 (fn [branding _]
   (:welcome branding)))

;; Returns the resolved welcome content (hiccup).
;; If content is a keyword, looks it up in the content registry.
(re-frame/reg-sub
 :branding/welcome-content
 :<- [:branding/welcome]
 (fn [welcome _]
   (let [content (:content welcome)]
     (if (keyword? content)
       (content/get-content content)
       content))))

;; Returns the welcome dialogue title.
(re-frame/reg-sub
 :branding/welcome-title
 :<- [:branding/welcome]
 (fn [welcome _]
   (:title welcome)))

;;; =============================================================================
;;; LINKS SUBSCRIPTIONS
;;; =============================================================================

;; Get a deployment-specific external link by key.
;;
;; Usage:
;;   @(re-frame/subscribe [:branding/link :sea-country])
;;   => "https://storymaps.arcgis.com/..." (for TMA)
;;   => nil (for other deployments)
(re-frame/reg-sub
 :branding/link
 :<- [:branding/config]
 (fn [branding [_ link-key]]
   (get-in branding [:links link-key])))

;; Returns all external links for the deployment.
(re-frame/reg-sub
 :branding/links
 :<- [:branding/config]
 (fn [branding _]
   (:links branding)))

;;; =============================================================================
;;; MAP CONFIG SUBSCRIPTIONS
;;; =============================================================================

;; Returns the map configuration (CRS, center, zoom).
(re-frame/reg-sub
 :map/config
 :<- [:deployment/config]
 (fn [config _]
   (:map config)))

;; Returns the map CRS for the deployment.
(re-frame/reg-sub
 :map/crs
 :<- [:map/config]
 (fn [map-config _]
   (:crs map-config)))

;; Returns the initial map center coordinates.
(re-frame/reg-sub
 :map/initial-center
 :<- [:map/config]
 (fn [map-config _]
   (:center map-config)))

;; Returns the initial map zoom level.
(re-frame/reg-sub
 :map/initial-zoom
 :<- [:map/config]
 (fn [map-config _]
   (:zoom map-config)))

;;; =============================================================================
;;; UI LAYOUT SUBSCRIPTIONS
;;; =============================================================================

;; Returns the ordered list of left drawer tabs for the deployment.
(re-frame/reg-sub
 :ui/tabs
 :<- [:deployment/config]
 (fn [config _]
   (:tabs config)))

;; Returns the ordered list of map controls for the deployment.
(re-frame/reg-sub
 :ui/controls
 :<- [:deployment/config]
 (fn [config _]
   (:controls config)))

;; Returns the right drawer slot mappings for the deployment.
(re-frame/reg-sub
 :ui/drawers
 :<- [:deployment/config]
 (fn [config _]
   (:drawers config)))
