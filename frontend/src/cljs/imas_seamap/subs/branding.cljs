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

(re-frame/reg-sub
 :branding/config
 "Returns the complete branding configuration for the current deployment."
 :<- [:deployment/config]
 (fn [config _]
   (:branding config)))

(re-frame/reg-sub
 :branding/logo
 "Returns the logo configuration (src, alt, height)."
 :<- [:branding/config]
 (fn [branding _]
   (:logo branding)))

(re-frame/reg-sub
 :branding/url
 "Returns the main website URL for the deployment."
 :<- [:branding/config]
 (fn [branding _]
   (:url branding)))

(re-frame/reg-sub
 :branding/title
 "Returns the deployment title."
 :<- [:branding/config]
 (fn [branding _]
   (:title branding)))

;;; =============================================================================
;;; LABEL SUBSCRIPTIONS
;;; =============================================================================

(re-frame/reg-sub
 :branding/label
 "Get a deployment-specific label by key.

  Usage:
    @(re-frame/subscribe [:branding/label :region-control])
    => 'Find Data in Region' (for TMA)
    => 'Select region' (for Australia)"
 :<- [:branding/config]
 (fn [branding [_ label-key]]
   (get-in branding [:labels label-key])))

(re-frame/reg-sub
 :branding/labels
 "Returns all labels for the deployment."
 :<- [:branding/config]
 (fn [branding _]
   (:labels branding)))

;;; =============================================================================
;;; COLOR SUBSCRIPTIONS
;;; =============================================================================

(re-frame/reg-sub
 :branding/colors
 "Returns the color overrides for the deployment.
  Used to generate CSS custom properties."
 :<- [:deployment/config]
 (fn [config _]
   (:colors config)))

(re-frame/reg-sub
 :branding/css-class
 "Returns the CSS class for the deployment (e.g., 'seamap', 'tas-marine-atlas')."
 :<- [:deployment/config]
 (fn [config _]
   (:css-class config)))

(re-frame/reg-sub
 :branding/drawer-class
 "Returns the drawer CSS class for the deployment."
 :<- [:deployment/config]
 (fn [config _]
   (:drawer-class config)))

;;; =============================================================================
;;; WELCOME CONTENT SUBSCRIPTIONS
;;; =============================================================================

(re-frame/reg-sub
 :branding/welcome
 "Returns the welcome dialogue configuration.
  Includes title and content (which may be a keyword or hiccup)."
 :<- [:branding/config]
 (fn [branding _]
   (:welcome branding)))

(re-frame/reg-sub
 :branding/welcome-content
 "Returns the resolved welcome content (hiccup).
  If content is a keyword, looks it up in the content registry."
 :<- [:branding/welcome]
 (fn [welcome _]
   (let [content (:content welcome)]
     (if (keyword? content)
       (content/get-content content)
       content))))

(re-frame/reg-sub
 :branding/welcome-title
 "Returns the welcome dialogue title."
 :<- [:branding/welcome]
 (fn [welcome _]
   (:title welcome)))

;;; =============================================================================
;;; LINKS SUBSCRIPTIONS
;;; =============================================================================

(re-frame/reg-sub
 :branding/link
 "Get a deployment-specific external link by key.

  Usage:
    @(re-frame/subscribe [:branding/link :sea-country])
    => 'https://storymaps.arcgis.com/...' (for TMA)
    => nil (for other deployments)"
 :<- [:branding/config]
 (fn [branding [_ link-key]]
   (get-in branding [:links link-key])))

(re-frame/reg-sub
 :branding/links
 "Returns all external links for the deployment."
 :<- [:branding/config]
 (fn [branding _]
   (:links branding)))

;;; =============================================================================
;;; MAP CONFIG SUBSCRIPTIONS
;;; =============================================================================

(re-frame/reg-sub
 :map/config
 "Returns the map configuration (CRS, center, zoom)."
 :<- [:deployment/config]
 (fn [config _]
   (:map config)))

(re-frame/reg-sub
 :map/crs
 "Returns the map CRS for the deployment."
 :<- [:map/config]
 (fn [map-config _]
   (:crs map-config)))

(re-frame/reg-sub
 :map/initial-center
 "Returns the initial map center coordinates."
 :<- [:map/config]
 (fn [map-config _]
   (:center map-config)))

(re-frame/reg-sub
 :map/initial-zoom
 "Returns the initial map zoom level."
 :<- [:map/config]
 (fn [map-config _]
   (:zoom map-config)))

;;; =============================================================================
;;; UI LAYOUT SUBSCRIPTIONS
;;; =============================================================================

(re-frame/reg-sub
 :ui/tabs
 "Returns the ordered list of left drawer tabs for the deployment."
 :<- [:deployment/config]
 (fn [config _]
   (:tabs config)))

(re-frame/reg-sub
 :ui/controls
 "Returns the ordered list of map controls for the deployment."
 :<- [:deployment/config]
 (fn [config _]
   (:controls config)))

(re-frame/reg-sub
 :ui/drawers
 "Returns the right drawer slot mappings for the deployment."
 :<- [:deployment/config]
 (fn [config _]
   (:drawers config)))
