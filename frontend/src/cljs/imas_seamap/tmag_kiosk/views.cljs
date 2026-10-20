;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.views
  "The kiosk's layout, after the seamap-kiosk prototype (kiosk_vertical_slice):
  a full-bleed map with the kiosk's content in fixed cards over it. With no
  story open, a story list sits bottom-right; with one open, the story card
  takes the right-hand column, and the step's media card the left. The map
  keeps Leaflet's default controls and interaction; only the layout comes from
  the prototype. Styles: src/ui/css/tmag-kiosk.scss."
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [imas-seamap.blueprint :as b]
            [imas-seamap.views :as views]
            [imas-seamap.tmag-kiosk.map.views :refer [map-component]]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- html [content]
  [:div {:dangerouslySetInnerHTML {:__html content}}])

(defn- html->text
  "Plain text of a WordPress HTML fragment, for one-line summaries. DOMParser
  doesn't run scripts or fetch anything."
  [content]
  (when (seq content)
    (.. (js/DOMParser.) (parseFromString content "text/html") -body -textContent)))

;;; Browse: no story open

(defn- story-list []
  (let [stories @(re-frame/subscribe [:kiosk/stories])]
    [:section.browse-card
     [:div.eyebrow "Island to Ice"]
     [:h1 "Choose a story"]
     [:div.browse-story-list
      (if (seq stories)
        (for [{:keys [id title summary image read-time steps]} stories]
          ^{:key id}
          [:button.browse-story
           {:on-click #(re-frame/dispatch [:kiosk.story/open id])}
           (if image
             [:img {:src image :alt ""}]
             [:span.story-thumb-fallback])
           [:span.browse-story-copy
            [:b title]
            [:span (html->text summary)]
            [:small (count steps) " map " (if (= 1 (count steps)) "reveal" "reveals")
             (when read-time (str " · " read-time " min"))]]])
        [:p.browse-loading [:span.spinner] "Loading stories…"])]]))

;;; Story: one open

(defn- layer-key []
  (let [names @(re-frame/subscribe [:kiosk/layer-names])]
    (when (seq names)
      [:aside.layer-key
       [:h2 "On the map"]
       (for [name names]
         ^{:key name}
         [:div.key-row name])])))

(defn- story-card []
  (let [{:keys [title steps]} @(re-frame/subscribe [:kiosk/story])
        index      @(re-frame/subscribe [:kiosk/step-index])
        last-step? (= index (dec (count steps)))
        {step-title :title :keys [content]} (get steps index)]
    [:section.story-card
     [:div.eyebrow title]
     ;; Keyed by step so the copy fades in afresh on each reveal.
     ^{:key index}
     [:div.story-copy
      (when (seq step-title) [:h1 step-title])
      [:div.narrative [html content]]]
     [layer-key]
     [:div.step-number "Reveal " (inc index) " of " (count steps)]
     [:div.progress-dots
      (for [i (range (count steps))]
        ^{:key i} [:span {:class (when (<= i index) "done")}])]
     [:div.story-actions
      [:button.back-button
       {:disabled (zero? index)
        :on-click #(re-frame/dispatch [:kiosk.step/go (dec index)])}
       "‹ Back"]
      [:button.next-button
       {:on-click #(re-frame/dispatch (if last-step?
                                        [:kiosk/start-again]
                                        [:kiosk.step/go (inc index)]))}
       (if last-step? "Finish" "Next ›")]]]))

;;; Media: the step's photos and videos, each at a place on the map (see
;;; `imas-seamap.tmag-kiosk.map.views/media-markers`). One fixed card, whatever
;;; the item, so it never resizes under the visitor's hand.

(defn- video? [url]
  (boolean (re-find #"(?i)\.(mp4|m4v|webm|mov)(\?|$)" url)))

;; Labels and units for known `overlay` readings; others show as authored.
(def ^:private readings
  {:temperature {:label "Temperature" :unit " °C" :decimals 1}
   :depth       {:label "Depth" :unit " m" :decimals 0}})

(defn- reading [[k v]]
  (let [{:keys [label unit decimals]} (readings (keyword k))]
    [:div.media-reading
     [:span (or label (string/capitalize (string/replace (name k) #"[_-]" " ")))]
     [:b (if (and (number? v) decimals)
           (str (.toLocaleString v "en-AU" #js {:maximumFractionDigits decimals}) unit)
           (str v))]]))

(def ^:private max-media-dots 8)

;; Every step has media, so the card is there for the whole story: it shows
;; with the story card and stays put between steps. Only its contents change:
;; they're empty while a flight is under way (see `:kiosk/media`) and fade in
;; on landing, and again as the visitor moves between items.
(defn- media-card []
  (let [media @(re-frame/subscribe [:kiosk/media])
        index @(re-frame/subscribe [:kiosk/media-index])
        total (count media)
        index (if (pos? total) (mod index total) 0)
        {:keys [url caption overlay]} (get media index)]
    [:section.media-card
     [:div.media-window
      (when url
        ;; Keyed by item, so a video stops when the visitor moves on.
        ^{:key url}
        [:div.media-item
         (if (video? url)
           [:video {:src url :controls true :preload "metadata" :playsInline true}]
           [:img {:src url :alt (or caption "")}])])]
     [:div.media-caption
      (when url
        ^{:key url}
        [:div.media-item
         (when (seq caption) [:p caption])
         (when (seq overlay)
           [:div.media-readings (map-indexed (fn [i r] ^{:key i} [reading r]) overlay)])])]
     ;; The row keeps its height when empty, so the card never changes shape.
     [:div.media-controls
      (when (> total 1)
        [:<>
         [:button.media-prev {:aria-label "Previous"
                              :on-click   #(re-frame/dispatch [:kiosk.media/select (mod (dec index) total)])} "‹"]
         ;; Dots while they fit on one row; a count beyond that.
         (if (<= total max-media-dots)
           [:div.media-dots
            (for [i (range total)]
              ^{:key i}
              [:button {:class      (when (= i index) "on")
                        :aria-label (str "Item " (inc i))
                        :on-click   #(re-frame/dispatch [:kiosk.media/select i])}])]
           [:div.media-count (inc index) " / " total])
         [:button.media-next {:aria-label "Next"
                              :on-click   #(re-frame/dispatch [:kiosk.media/select (mod (inc index) total)])} "›"]])]]))

;;; Map chrome

(defn- loading-notice []
  (when @(re-frame/subscribe [:kiosk/layers-loading?])
    [:div.loading-notice [:div.spinner] "Bringing the next view into focus…"]))

;;; Attract screen: shown at start-up and whenever the kiosk goes idle. A
;;; placeholder until the looping attract video arrives.

(defn- attract-screen []
  (when @(re-frame/subscribe [:kiosk/attract?])
    [:div.kiosk-attract
     {:style {:position        "fixed"
              :inset           0
              ;; Above everything: Blueprint portals (drawers, dialogs) are
              ;; 2000 and the loading splash 9000.
              :z-index         10000
              :display         "flex"
              :align-items     "center"
              :justify-content "center"
              :background      "#002b49"}}
     [b/button {:text     "Start exploring"
                :large    true
                :intent   b/INTENT-PRIMARY
                :on-click #(re-frame/dispatch [:kiosk.attract/exit])}]]))

(defn layout-app []
  (let [story? (some? @(re-frame/subscribe [:kiosk/story]))]
    [:div#main-wrapper.seamap.kiosk-player
     {:class (when story? "story-mode")}
     [:div#content-wrapper
      [map-component]]
     [views/loading-display]
     (if story?
       [:<>
        [story-card]
        [media-card]
        [:button.home-button {:on-click #(re-frame/dispatch [:kiosk/start-again])} "⌂ Start again"]]
       [story-list])
     [loading-notice]
     [attract-screen]]))
