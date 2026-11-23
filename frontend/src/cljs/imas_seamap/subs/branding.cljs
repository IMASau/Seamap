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
;;; RUNTIME URL RESOLUTION
;;; =============================================================================

(def ^:private url-base-keys
  {:url-base/api       :api-url-base
   :url-base/media     :media-url-base
   :url-base/wordpress :wordpress-url-base
   :url-base/img       :img-url-base})

(defn resolve-url
  "Resolve a branding URL value against the runtime url-base map.

   A string is returned as-is; a keyword such as :url-base/wordpress returns that
   base; a vector [:url-base/wordpress \"path/\"] joins the base and the path."
  [url-base v]
  (cond
    (string? v)  v
    (keyword? v) (get url-base (url-base-keys v))
    (vector? v)  (let [[base path] v] (str (resolve-url url-base base) path))
    :else        v))

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

;; Returns the main website URL for the deployment, resolved against the runtime
;; URL bases (see `resolve-url`).
(re-frame/reg-sub
 :branding/url
 :<- [:branding/config]
 :<- [:url-base]
 (fn [[branding url-base] _]
   (resolve-url url-base (:url branding))))

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
;; If content is a keyword, looks it up in the content registry, passing the
;; runtime URL bases so content can link to them.
(re-frame/reg-sub
 :branding/welcome-content
 :<- [:branding/welcome]
 :<- [:url-base]
 (fn [[welcome url-base] _]
   (let [content (:content welcome)]
     (if (keyword? content)
       (content/get-content content url-base)
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

;; Get a deployment-specific external link by key, resolved against the runtime
;; URL bases (see `resolve-url`).
;;
;; Usage:
;;   @(re-frame/subscribe [:branding/link :sea-country])
;;   => "https://storymaps.arcgis.com/..." (for TMA)
;;   => nil (for other deployments)
(re-frame/reg-sub
 :branding/link
 :<- [:branding/config]
 :<- [:url-base]
 (fn [[branding url-base] [_ link-key]]
   (resolve-url url-base (get-in branding [:links link-key]))))

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

;; Returns the complete layout configuration for the deployment (Phase 4).
;; Slot-specific views should destructure this rather than have one sub per slot.
(re-frame/reg-sub
 :layout/config
 :<- [:deployment/config]
 (fn [config _]
   (:layout config)))
