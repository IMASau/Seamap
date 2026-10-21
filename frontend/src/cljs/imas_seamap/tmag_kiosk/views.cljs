;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.views
  "The kiosk's layout, after the seamap-kiosk prototype (kiosk_vertical_slice):
  a full-bleed map with the kiosk's content in fixed cards over it. The
  right-hand column holds the story list, or the open story's card in the same
  place and size; with a story open, the step's media card is in the left. The
  map keeps Leaflet's default controls and interaction; only the layout comes
  from the prototype. Styles: src/ui/css/tmag-kiosk.scss."
  (:require [clojure.string :as string]
            [reagent.core :as r]
            [re-frame.core :as re-frame]
            [imas-seamap.blueprint :as b]
            [imas-seamap.views :as views]
            [imas-seamap.tmag-kiosk.map.views :refer [map-component]]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- html [content]
  [:div {:dangerouslySetInnerHTML {:__html content}}])

(defn- html->text
  "Plain text of a WordPress HTML fragment, for card summaries. DOMParser
  doesn't run scripts or fetch anything."
  [content]
  (when (seq content)
    (.. (js/DOMParser.) (parseFromString content "text/html") -body -textContent)))

;;; Browse: no story open. A card per story: a band of its picture with the read
;;; time on it, then its title and summary.

(defn- story-list []
  (let [stories @(re-frame/subscribe [:kiosk/stories])]
    [:section.browse-card
     [:div.eyebrow "Island to Ice"]
     [:h1 "Choose a story"]
     [:div.browse-story-list
      (if (seq stories)
        (for [{:keys [id title summary image read-time]} stories]
          ^{:key id}
          [:button.browse-story
           {:on-click #(re-frame/dispatch [:kiosk.story/open id])}
           [:span.browse-story-image
            (if image
              [:img {:src image :alt ""}]
              [:span.story-thumb-fallback])]
           [:span.browse-story-copy
            [:b title]
            [:span.summary (html->text summary)]
            (when read-time [:small.read-time read-time " min"])]])
        [:p.browse-loading [:span.spinner] "Loading stories…"])]]))

;;; Story: one open. It starts on an intro card, in the story card's place.

(defn- home-button []
  [:button.home-button {:aria-label "Start again"
                        :on-click   #(re-frame/dispatch [:kiosk/start-again])}
   [b/icon {:icon "home"}]])

(defn- with-view-transition
  "Runs `f`, a synchronous change of app-db, as a view transition: the story's
  picture and title (`view-transition-name`s in tmag-kiosk.scss) move and morph
  from where they were to where they are after it. Only they animate; the rest
  of the page, the map included, stays live."
  [f]
  (if (.-startViewTransition js/document)
    (.startViewTransition js/document (fn [] (f) (r/flush)))
    (f)))

(defn- begin!
  "Begin: the flight sets off at once, and the intro card turns into the first
  step's as it goes."
  []
  (re-frame/dispatch-sync [:kiosk.story/begin-flight])
  (with-view-transition #(re-frame/dispatch-sync [:kiosk.story/begin])))

(defn- intro-card
  "`arriving?`: from the story list, so it fades in over `panel-morph`."
  [{:keys [arriving?]}]
  (let [{:keys [title summary image read-time]} @(re-frame/subscribe [:kiosk/story])]
    [:section.story-card.story-intro {:class (when arriving? "arriving")}
     (if image
       [:img.intro-image {:src image :alt ""}]
       [:div.intro-image.story-thumb-fallback])
     [:div.intro-copy
      [:h1 title]
      [:div.intro-summary [html summary]]
      (when read-time
        [:p.intro-read-time "Estimated read time: " read-time (if (= 1 read-time) " minute" " minutes")])]
     [:div.story-actions
      [home-button]
      [:button.next-button {:on-click begin!} "Begin"]]]))

(defn- copy [{step-title :title :keys [content]} class]
  [:div.story-copy {:class class}
   (when (seq step-title) [:h1 step-title])
   [:div.narrative [html content]]])

(defn- step-copy
  "The step's copy. On to another step, the last one's copy fades out over it
  and then the new one fades in (opacity only: the flight is under way). The
  first copy shown, arriving from the intro card, waits for the title's morph
  to pass over it (`with-view-transition`)."
  [_index _step]
  (let [shown   (atom nil)
        leaving (atom nil)]
    (fn [index step]
      (let [first? (nil? @shown)]
        (when (and @shown (not= (:index @shown) index))
          (reset! leaving @shown))
        (when-not (= (:index @shown) index)
          (reset! shown {:index index :step step :first? first?}))
        [:div.story-copy-stack
         (when-let [{i :index s :step} @leaving]
           ^{:key (str "leaving-" i)} [copy s "leaving"])
         ^{:key index} [copy step (when (:first? @shown) "after-morph")]]))))

(defn- story-card []
  (let [{:keys [title image steps]} @(re-frame/subscribe [:kiosk/story])
        index      @(re-frame/subscribe [:kiosk/step-index])
        last-step? (= index (dec (count steps)))]
    [:section.story-card
     ;; The story's picture behind its title, if it has one.
     (if image
       [:div.eyebrow.with-image
        [:img.eyebrow-image {:src image :alt ""}]
        [:span.eyebrow-title title]]
       [:div.eyebrow title])
     [step-copy index (get steps index)]
     ;; One continuous bar, not a segment per step, so it doesn't read as tabs.
     [:div.story-progress
      {:role "progressbar" :aria-valuemin 1 :aria-valuemax (count steps) :aria-valuenow (inc index)}
      [:div.progress-track
       [:div.progress-fill {:style {:width (str (* 100 (/ (inc index) (count steps))) "%")}}]]
      [:div.step-number (inc index) " / " (count steps)]]
     ;; Start again leftmost, with Back between it and Next, the most tapped.
     [:div.story-actions
      [home-button]
      ;; Back from the first step is back to the intro card.
      [:button.back-button
       {:on-click #(if (zero? index)
                     (with-view-transition (fn [] (re-frame/dispatch-sync [:kiosk.story/intro])))
                     (re-frame/dispatch [:kiosk.step/go (dec index)]))}
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

(defn- clock [seconds]
  (let [s (js/Math.floor seconds)]
    (str (quot s 60) ":" (.padStart (str (mod s 60)) 2 "0"))))

(defn- media-video
  "A video with the kiosk's own controls, sized for touch, and none of the
  browser's extras (download, playback speed, picture-in-picture). Tapping the
  video plays or pauses it."
  [_url]
  (let [video   (atom nil)
        state   (r/atom {:playing? false :muted? false :time 0 :duration 0})
        sync!   #(when-let [^js v @video]
                   (reset! state {:playing? (not (.-paused v))
                                  :muted?   (.-muted v)
                                  :time     (.-currentTime v)
                                  :duration (if (js/isFinite (.-duration v)) (.-duration v) 0)}))
        toggle! #(when-let [^js v @video]
                   (if (.-paused v) (.catch (.play v) (fn [_])) (.pause v)))
        mute!   #(when-let [^js v @video] (set! (.-muted v) (not (.-muted v))))
        seek!   #(when-let [^js v @video] (set! (.-currentTime v) (js/parseFloat (.. % -target -value))))]
    (fn [url]
      (let [{:keys [playing? muted? time duration]} @state]
        [:div.media-video
         [:video {:src                     url
                  :preload                 "metadata"
                  :playsInline             true
                  :disablePictureInPicture true
                  :ref                     #(reset! video %)
                  :on-click                toggle!
                  :on-play                 sync!
                  :on-pause                sync!
                  :on-ended                sync!
                  :on-time-update          sync!
                  :on-loaded-metadata      sync!
                  :on-volume-change        sync!}]
         (when-not playing?
           [:button.video-play-big {:aria-label "Play" :on-click toggle!} [b/icon {:icon "play"}]])
         [:div.video-controls
          [:button {:aria-label (if playing? "Pause" "Play") :on-click toggle!}
           [b/icon {:icon (if playing? "pause" "play")}]]
          [:input.video-seek {:type "range" :min 0 :max duration :step "any" :value time
                              :aria-label "Position" :on-change seek!}]
          [:span.video-time (clock time) " / " (clock duration)]
          [:button {:aria-label (if muted? "Unmute" "Mute") :on-click mute!}
           [b/icon {:icon (if muted? "volume-off" "volume-up")}]]]]))))

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
           [media-video url]
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
     [b/button {:text      "Start exploring"
                :className "attract-start"
                :large     true
                :intent    b/INTENT-PRIMARY
                :on-click  #(re-frame/dispatch [:kiosk.attract/exit])}]]))

(defn- panel-morph
  "Between the wide story list and a story's narrow card, a blank panel that
  narrows or widens from one to the other, under the arriving card as it fades
  in. It's empty, so animating its width is cheap enough to run during a flight
  (going home). Keyed by transition, so each restarts its animation; once done
  it's invisible."
  [_mode]
  (let [state (atom {:mode nil :n 0})]
    (fn [mode]
      (let [{prev :mode} @state]
        (when (and prev (not= prev mode))
          (swap! state #(-> % (update :n inc) (assoc :to (if (= mode :browse) "to-wide" "to-narrow")))))
        (swap! state assoc :mode mode)
        (let [{:keys [n to]} @state]
          (when (pos? n)
            ^{:key n} [:div.panel-morph {:class to}]))))))

(defn layout-app []
  ;; The intro card fades in only when it arrives from the story list; back
  ;; from the first step, it morphs in instead (see `with-view-transition`).
  (let [screen    (atom nil)
        arriving? (atom false)]
    (fn []
      (let [story? (some? @(re-frame/subscribe [:kiosk/story]))
            intro? @(re-frame/subscribe [:kiosk/intro?])
            now    (cond (not story?) :list intro? :intro :else :step)]
        (when (not= now @screen)
          (reset! arriving? (and (= now :intro) (= @screen :list)))
          (reset! screen now))
        [:div#main-wrapper.seamap.kiosk-player
         {:class (when story? "story-mode")}
         [:div#content-wrapper
          [map-component]]
         [views/loading-display]
         [panel-morph (if story? :story :browse)]
         (case now
           :list  [story-list]
           :intro [intro-card {:arriving? @arriving?}]
           ;; The media card belongs to the steps.
           :step  [:<>
                   [story-card]
                   [media-card]])
         [loading-notice]
         [attract-screen]]))))
