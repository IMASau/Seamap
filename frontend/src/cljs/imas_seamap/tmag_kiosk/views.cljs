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
            [imas-seamap.tmag-kiosk.events :refer [one-column? attract-video-url bookmark-shortcode]]
            [imas-seamap.tmag-kiosk.map.views :refer [map-component]]
            [imas-seamap.tmag-kiosk.idle :as idle]
            [imas-seamap.tmag-kiosk.media-cache :as media-cache]
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

(defn- story-list
  "One or two columns of story cards (see `one-column?`), in three rows while
  a column has up to three cards (so three fill it), and four for four.
  Stories are nil until they've loaded, and may load as none."
  []
  (let [stories @(re-frame/subscribe [:kiosk/stories])
        columns (if (one-column? stories) 1 2)]
    [:section.browse-card
     {:class (when (= columns 1) "one-column")}
     [:div.eyebrow "Island to Ice"]
     [:h1 "Choose a story"]
     [:div.browse-story-list
      {:style {"--browse-rows" (-> (count stories) (/ columns) js/Math.ceil (max 3) (min 4))}}
      (cond
        (seq stories)
        (for [{:keys [id title summary image read-time marker]} stories]
          ^{:key id}
          [:button.browse-story
           {:on-click #(re-frame/dispatch [:kiosk.story/open id])}
           [:span.browse-story-image
            (if image
              [:img {:src image :alt ""}]
              [:span.story-thumb-fallback])]
           ;; As on its marker, to match them up.
           (when-let [icon (:icon marker)]
             [:span.story-icon [:img {:src icon :alt ""}]])
           [:span.browse-story-copy
            [:b title]
            [:span.summary (html->text summary)]
            (when read-time [:small.read-time read-time " min"])]])

        (some? stories)
        [:p.browse-loading "No stories yet."]

        :else
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
  "Begin: the intro card turns into the first step's, and the flight sets off
  as soon as the turn's view transition has taken its snapshots: taken during
  the flight, they cost it a frame."
  []
  (if (.-startViewTransition js/document)
    (let [^js transition (.startViewTransition js/document
                                               (fn [] (re-frame/dispatch-sync [:kiosk.story/begin]) (r/flush)))]
      (.then (.-ready transition)
             #(re-frame/dispatch-sync [:kiosk.story/begin-flight])
             #(re-frame/dispatch-sync [:kiosk.story/begin-flight])))
    (do (re-frame/dispatch-sync [:kiosk.story/begin-flight])
        (re-frame/dispatch-sync [:kiosk.story/begin]))))

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

(def ^:private safe-html
  "A tapped feature's html (from a layer's data, not the kiosk's WordPress)
  without scripts, embeds, forms or event handlers. Its links are inert, as
  in all the narrative (tmag-kiosk.scss)."
  (memoize
   (fn [content]
     (let [body (.-body (.parseFromString (js/DOMParser.) content "text/html"))]
       (doseq [^js el (array-seq (.querySelectorAll body "script, style, link, meta, iframe, frame, object, embed, form, base"))]
         (.remove el))
       (doseq [^js el (array-seq (.querySelectorAll body "*"))
               ^js attr (array-seq (.from js/Array (.-attributes el)))
               :when (or (string/starts-with? (.toLowerCase (.-name attr)) "on")
                         (re-find #"(?i)^\s*javascript:" (.-value attr)))]
         (.removeAttribute el (.-name attr)))
       (.-innerHTML body)))))

(def ^:private with-bookmark-links
  "`content` with its map bookmark links (see
  `imas-seamap.tmag-kiosk.events/bookmark-shortcode`) marked as such: they're
  the only links in the narrative that can be tapped."
  (memoize
   (fn [content]
     (let [body  (.-body (.parseFromString (js/DOMParser.) (or content "") "text/html"))
           links (filter #(bookmark-shortcode (.getAttribute ^js % "href"))
                         (array-seq (.querySelectorAll body "a[href]")))]
       (if (seq links)
         (do (doseq [^js a links] (.add (.-classList a) "bookmark-link"))
             (.-innerHTML body))
         content)))))

(defn- narrative-click
  "A tap on a bookmark link in the narrative goes to its bookmark."
  [^js e]
  (when-let [^js a (.closest (.-target e) "a.bookmark-link")]
    (.preventDefault e)
    (re-frame/dispatch [:kiosk.bookmark/go (bookmark-shortcode (.getAttribute a "href"))])))

(defn- copy [{step-title :title :keys [content]} class]
  [:div.story-copy {:class class}
   (when (seq step-title) [:h1 step-title])
   [:div.narrative {:on-click narrative-click} [html (with-bookmark-links content)]]])

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

(defn- story-card
  "The step's card. A tapped feature with html (`:kiosk/feature-html`) takes
  the copy's place."
  []
  (let [{:keys [title image steps]} @(re-frame/subscribe [:kiosk/story])
        index      @(re-frame/subscribe [:kiosk/step-index])
        feature    @(re-frame/subscribe [:kiosk/feature-html])
        last-step? (= index (dec (count steps)))]
    [:section.story-card
     ;; The story's picture behind its title, if it has one.
     (if image
       [:div.eyebrow.with-image
        [:img.eyebrow-image {:src image :alt ""}]
        [:span.eyebrow-title title]]
       [:div.eyebrow title])
     (if feature
       [step-copy (str "feature-" (:id feature)) {:content (safe-html (:html feature))}]
       [step-copy index (get steps index)])
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

;; An `overlay` reading, label and value as authored: the overlay may hold
;; anything.
(defn- reading [[k v]]
  [:div.media-reading
   [:span (name k)]
   [:b (str v)]])

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

;;; Legends: the step's layers' keys, beside the story card. Drawn here rather
;;; than with Seamap's `legend-display`, to size them in design units.

(defn- legend-key [info]
  (if (string? info)
    ;; A legend graphic (eg WMS GetLegendGraphic): drawn at a design unit per
    ;; pixel, so twice its size on the table, like the rest of the layout.
    [:img.legend-graphic
     {:src     info
      :alt     ""
      :on-load #(let [^js img (.-target %)]
                  (.setProperty (.-style img) "--natural-w" (.-naturalWidth img)))}]
    ;; Vector keys, [{:label :image}] or [{:label :style}]: a swatch and a label.
    [:ul.legend-entries
     (map-indexed
      (fn [i {:keys [label image style]}]
        ^{:key i}
        [:li
         [:span.legend-swatch (if image [:img {:src image :alt ""}] [:span {:style style}])]
         [:span.legend-label (string/replace (str label) #"\\n" "\n")]])
      info)]))

(defn- fit-legends!
  "Scales the keys down to fit the frame, only if they're larger than it, and
  sizes the card to them: the card never scrolls. The keys are laid out at
  their own size, which the transform doesn't change, so this doesn't feed
  back into the observer."
  [^js frame]
  (let [^js card (.-firstChild frame)
        ^js keys (.-firstChild card)
        style    (js/getComputedStyle card)
        chrome   #(transduce (map (fn [prop] (js/parseFloat (.getPropertyValue style prop)))) + %&)
        w        (.-offsetWidth keys)
        h        (.-offsetHeight keys)]
    (when (and (pos? w) (pos? h))
      (let [s (min 1
                   (/ (- (.-clientWidth frame) (chrome "padding-left" "padding-right" "border-left-width" "border-right-width")) w)
                   (/ (- (.-clientHeight frame) (chrome "padding-top" "padding-bottom" "border-top-width" "border-bottom-width")) h))]
        (doto (.-style card)
          (.setProperty "--legend-w" (str w "px"))
          (.setProperty "--legend-h" (str h "px"))
          (.setProperty "--legend-s" s))))))

(defn- legend-card []
  (let [observer (atom nil)
        ;; Refitted as images load, and as the keys change from step to step.
        frame!   (fn [^js frame]
                   (some-> ^js @observer .disconnect)
                   (reset! observer
                           (when frame
                             (doto (js/ResizeObserver. #(fit-legends! frame))
                               (.observe frame)
                               (.observe (.. frame -firstChild -firstChild))))))]
    (fn []
      (when-let [legends (seq @(re-frame/subscribe [:kiosk/legends]))]
        [:div.legend-frame {:ref frame!}
         [:section.legend-card
          [:div.legend-keys
           (for [{:keys [layer-id info]} legends]
             ^{:key layer-id}
             [:div.legend [legend-key info]])]]]))))

;;; Map chrome

(defn- loading-notice []
  (when @(re-frame/subscribe [:kiosk/layers-loading?])
    [:div.loading-notice [:div.spinner] "Bringing the next view into focus…"]))

;;; Attract screen: shown at start-up and whenever the kiosk goes idle. The
;;; attract video loops, muted, behind the button (on the background colour
;;; until it starts).

(defn- attract-video
  "Plays the kiosk's own copy of the video once it has one (see
  `media-cache`), or else streams it. A copy arriving while it streams takes
  over where it's got to."
  []
  (let [el        (atom nil)
        shown     (atom nil)
        resume-at (atom nil)]
    (fn []
      (let [src (media-cache/src attract-video-url)]
        ;; Rendered before the new src is set, so the old one's time is read.
        (when (and @el @shown (not= src @shown))
          (reset! resume-at (.-currentTime ^js @el)))
        (reset! shown src)
        [:video {:ref                #(reset! el %)
                 :src                src
                 :auto-play          true
                 :muted              true
                 :loop               true
                 :plays-inline       true
                 :on-loaded-metadata (fn [^js e]
                                       (when-let [t @resume-at]
                                         (set! (.. e -target -currentTime) t)
                                         (reset! resume-at nil)))
                 :style              {:position "absolute" :inset 0 :width "100%" :height "100%" :object-fit "cover"}}]))))

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
     [attract-video]
     [b/button {:text      "Start exploring"
                :className "attract-start"
                :large     true
                :intent    b/INTENT-PRIMARY
                :style     {:position "relative"} ; over the video
                :on-click  #(re-frame/dispatch [:kiosk.attract/exit])}]]))

(defn- still-there
  "Asked before the kiosk goes idle, with a bar running down to it. Any input
  answers it (see `imas-seamap.tmag-kiosk.idle`); the scrim takes the tap
  that does, so it doesn't also act on what's underneath."
  []
  (when @(re-frame/subscribe [:kiosk/still-there?])
    [:div.still-there
     [:div.still-there-card
      [:h2 "Still there?"]
      [:p "Touch the screen to keep exploring."]
      [:div.still-there-timer
       [:div {:style {:animation-duration (str idle/prompt-ms "ms")}}]]]]))

(defn- panel-morph
  "Between the wide story list and a story's narrow card, a blank panel that
  narrows or widens from one to the other, under the arriving card as it fades
  in. It's empty, so animating its width is cheap enough to run during a flight
  (going home). Between a one-column list and a story it stays the same width,
  just keeping the map from showing through as the card fades in. Keyed by
  transition, so each restarts its animation; once done it's invisible."
  [_mode _wide-list?]
  (let [state (atom {:mode nil :n 0})]
    (fn [mode wide-list?]
      (let [{prev :mode} @state]
        (when (and prev (not= prev mode))
          (swap! state #(-> % (update :n inc) (assoc :to (cond (not wide-list?) "to-same"
                                                                (= mode :browse) "to-wide"
                                                                :else            "to-narrow")))))
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
            wide?  (not (one-column? @(re-frame/subscribe [:kiosk/stories])))
            now    (cond (not story?) :list intro? :intro :else :step)]
        (when (not= now @screen)
          (reset! arriving? (and (= now :intro) (= @screen :list)))
          (reset! screen now))
        [:div#main-wrapper.seamap.kiosk-player
         {:class [(when story? "story-mode") (when-not wide? "same-width")]}
         [:div#content-wrapper
          [map-component]]
         [views/loading-display]
         [panel-morph (if story? :story :browse) wide?]
         (case now
           :list  [story-list]
           :intro [intro-card {:arriving? @arriving?}]
           ;; The media card belongs to the steps.
           :step  [:<>
                   [story-card]
                   [media-card]
                   [legend-card]])
         [loading-notice]
         [still-there]
         [attract-screen]]))))
