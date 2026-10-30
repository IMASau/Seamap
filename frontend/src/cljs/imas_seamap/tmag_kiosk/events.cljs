;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.events
  (:require [ajax.core :as ajax]
            [imas-seamap.db :as db]
            [imas-seamap.map.events :as mevents]
            [imas-seamap.map.utils :as map-utils]
            [imas-seamap.utils :refer [merge-in ajax-loaded-info append-params-from-map decode-html-entities parse-state]]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- boot-flow []
  {:first-dispatch [:ui/show-loading]
   :rules
   [{:when :seen? :events :ui/show-loading :dispatch [:construct-urls]}
    {:when :seen? :events :construct-urls :dispatch [:initialise-layers]}
    {:when :seen-all-of? :events [:map/update-base-layers
                                  :map/update-base-layer-groups]
     :dispatch [:map/update-grouped-base-layers]}
    {:when :seen-all-of? :events [:map/update-layers
                                  :map/update-keyed-layers
                                  :map/update-rich-layers]
     :dispatch-n [[:map/join-keyed-layers]
                  [:map/join-rich-layers]]}
    {:when :seen-all-of? :events [:map/update-grouped-base-layers
                                  :map/update-layers
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-descriptors
                                  :map/update-categories
                                  :map/update-keyed-layers
                                  :update-dynamic-pills
                                  :map/join-keyed-layers
                                  :map/join-rich-layers]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn- boot-flow-hash-state [hash-code]
  {:first-dispatch [:ui/show-loading]
   :rules
   [{:when :seen? :events :ui/show-loading :dispatch [:construct-urls]}
    {:when :seen? :events :construct-urls :dispatch [:load-hash-state hash-code]}
    {:when :seen? :events :load-hash-state :dispatch [:initialise-layers]}
    {:when :seen-all-of? :events [:map/update-base-layers
                                  :map/update-base-layer-groups]
     :dispatch [:map/update-grouped-base-layers]}
    {:when :seen-all-of? :events [:map/update-layers
                                  :map/update-keyed-layers
                                  :map/update-rich-layers]
     :dispatch-n [[:map/join-keyed-layers]
                  [:map/join-rich-layers]]}
    {:when :seen-all-of? :events [:map/update-grouped-base-layers
                                  :map/update-layers
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-descriptors
                                  :map/update-categories
                                  :map/update-keyed-layers
                                  :update-dynamic-pills
                                  :map/join-keyed-layers
                                  :map/join-rich-layers]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn- boot-flow-save-state [shortcode]
  {:first-dispatch [:ui/show-loading]
   :rules
   [{:when :seen? :events :ui/show-loading :dispatch [:construct-urls]}
    {:when :seen? :events :construct-urls :dispatch [:get-save-state shortcode [:load-hash-state]]}
    {:when :seen? :events :load-hash-state :dispatch [:initialise-layers]}
    {:when :seen-all-of? :events [:map/update-base-layers
                                  :map/update-base-layer-groups]
     :dispatch [:map/update-grouped-base-layers]}
    {:when :seen-all-of? :events [:map/update-layers
                                  :map/update-keyed-layers
                                  :map/update-rich-layers]
     :dispatch-n [[:map/join-keyed-layers]
                  [:map/join-rich-layers]]}
    {:when :seen-all-of? :events [:map/update-grouped-base-layers
                                  :map/update-layers
                                  :map/update-organisations
                                  :map/update-classifications
                                  :map/update-descriptors
                                  :map/update-categories
                                  :map/update-keyed-layers
                                  :update-dynamic-pills
                                  :map/join-keyed-layers
                                  :map/join-rich-layers]
     :dispatch-n [[:map/initialise-display]
                  [:transect/maybe-query]]}
    {:when :seen? :events :ui/hide-loading :halt? true}
    {:when :seen-any-of? :events [:ajax/default-err-handler] :dispatch [:loading-failed] :halt? true}]})

(defn one-column?
  "Whether the story list is one column of cards, the story card's width:
  for four stories or fewer. Otherwise it's two, twice as wide."
  [stories]
  (<= (count stories) 4))

;; The home view, where the kiosk starts and returns to: Tasmania, the marine
;; parks around it and the inset of Macquarie Island's (south-east of Tasmania
;; in the home layer, TMAG:HOME_AMP_boundaries), 141.8 to 159.4 E and 39 to
;; 50.6 S, fitted with a margin into the map left of the story list and above
;; the bezel, on the table's 3840x2160 screen. The list is twice as wide for
;; five stories or more (`one-column?`), so that view's centre is further east.
(def home-views
  {:one-column {:center [-45.409 154.831] :zoom 7.25}
   :two-column {:center [-45.409 158.716] :zoom 7.25}})

(defn- home-view [db]
  (home-views (if (one-column? (get-in db [:kiosk :stories])) :one-column :two-column)))

;; Where a visitor can take the map, the same at every view (see
;; `imas-seamap.tmag-kiosk.map.limits`): the marine parks around Tasmania and
;; the inset of Macquarie Island's (to 159.4 E and 50.5 S; see `home-views`),
;; with a margin. A step at the real Macquarie Island, outside them, stretches
;; them while it's shown.
(def kiosk-limits {:bounds {:west 140.5 :south -51.5 :east 160.5 :north -37.5}})

;; Loops behind the attract screen's button. A 30 s draft; the final one will
;; be longer, and made smaller for the web.
(def attract-video-url "https://data.imas.utas.edu.au/kiosk/app_files/attract.mp4")

(defn boot [{:keys [save-code hash-code] {:keys [seamap-app-state]} :local-storage/get} [_ api-url-base media-url-base wordpress-url-base img-url-base]]
  {:db         (-> db/default-db
                   (assoc-in [:config :url-base]
                             {:api-url-base       api-url-base
                              :media-url-base     media-url-base
                              :wordpress-url-base wordpress-url-base
                              :img-url-base       img-url-base})
                   ;; The map starts at the home view (see
                   ;; `tmag-kiosk.map.views`), rather than fitting the start-up
                   ;; layer once it loads.
                   (assoc-in [:map :initial-bounds?] false)
                   (assoc-in [:kiosk :attract?] true))
   :async-flow (cond ; Choose async boot flow based on what information we have for the DB:
                 (seq save-code)    (boot-flow-save-state save-code)    ; A shortform save-code that can be used to query for a hash-code
                 (seq hash-code)    (boot-flow-hash-state hash-code)    ; A hash-code that can be decoded into th DB's initial state
                 (seq seamap-app-state) (boot-flow-hash-state seamap-app-state) ; Same as hash-code, except that we use the one stored in local storage
                 :else              (boot-flow))                       ; No information, so we start with an empty DB
   ;; Stories don't depend on the layer catalogue, so load them alongside it;
   ;; this also starts the refresh loop.
   :dispatch   [:kiosk.stories/load]})

(defn- kiosk-reset
  "Kiosk state to carry through a reset: loaded stories and save states are
  kept (applying any refresh that arrived mid-story); the open story is closed."
  [{:keys [pending-stories] :as kiosk}]
  (cond-> (dissoc kiosk :story-id :step-index :intro? :pending-stories :keep-view? :media-index :landed-step
                  :feature-media :feature-query)
    pending-stories (assoc :stories pending-stories)))

(declare prepare-flights home-target)

(defn re-boot
  "Resets the map and the kiosk. `landed-home?` (from `start-again`): the story
  was closed at the tap, so any story open now was opened on the way home, and
  stays open (its intro card doesn't move the map)."
  [{:keys [db]} [_ landed-home?]]
  (let [kiosk (if (and landed-home? (get-in db [:kiosk :story-id]))
                (:kiosk db)
                (kiosk-reset (:kiosk db)))
        db (merge-in db/default-db (ajax-loaded-info db))
        startup-layers (get-in db [:map :keyed-layers :startup] [])
        db (-> db
               (assoc-in [:map :active-layers] startup-layers)
               (assoc-in [:map :active-base-layer] (first (get-in db [:map :grouped-base-layers])))
               (assoc :initialised true)
               (assoc :kiosk (dissoc kiosk :homeward?)))]
    (cond-> {:db         db
             :dispatch-n [[:map/update-map-view (assoc (home-view db) :instant? true)]
                          ;; Anything new since the last warm-up (eg refreshed stories)
                          [:kiosk/warm-up]]
             :local-storage/remove
             {:name :seamap-app-state}}
      ;; Before it's moved home (the limits might have grown to take in a step
      ;; outside them).
      (get-in db [:map :leaflet-map])
      (assoc :kiosk.map/limits {:leaflet-map (get-in db [:map :leaflet-map]) :to (home-target db)})
      ;; Prepared once the map is home (it may be anywhere: the idle reset
      ;; and the tour leave it where it was).
      (prepare-flights db) (assoc :kiosk/prepare-flights (assoc (prepare-flights db) :moving? true)))))

;;; Idle and attract
;;;
;;; After a period without input the kiosk resets itself and returns to the
;;; attract screen. This is done in-app, never by reloading the page: Edge's
;;; kiosk idle reset (which restarts the browser) stays off, so the InPrivate
;;; session, and everything the app has cached in memory, survives for days.
;;; Activity is watched by `imas-seamap.tmag-kiosk.idle`.

(defn go-idle
  "Resets the kiosk (as `re-boot` does, without a flight) and shows the attract
  screen, where the warm-up picks up anything new. Nothing to do if it's
  already there."
  [{:keys [db] :as cofx} event]
  (when-not (get-in db [:kiosk :attract?])
    (-> (re-boot cofx event)
        (assoc-in [:db :kiosk :attract?] true)
        (assoc :kiosk/cancel-flight true))))

(defn attract-exit
  "Leaves the attract screen for the story list, ending the tour if it's under
  way (see `tour-next`)."
  [{:keys [db]} _]
  (if (get-in db [:kiosk :tour])
    {:db                    (update db :kiosk #(-> % (assoc :attract? false) (dissoc :tour)))
     :kiosk/suspend-flights false
     :dispatch              [:re-boot]}
    {:db (assoc-in db [:kiosk :attract?] false)}))

(defn show-initial-layers
  "Seamap's, then warms the kiosk up (see `warm-up`)."
  [cofx event]
  (-> (mevents/show-initial-layers cofx event)
      (update :dispatch-n #(conj (vec %) [:kiosk/warm-up]))))

(defn initialise-layers [{:keys [db]} _]
  (let [{:keys [site-configuration-url
                layer-url
                base-layer-url
                base-layer-group-url
                organisation-url
                classification-url
                descriptor-url
                category-url
                keyed-layers-url
                rich-layers-url
                dynamic-pills-url]} (get-in db [:config :urls])]
    {:db         db
     :http-xhrio [{:method          :get
                   :uri             site-configuration-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:update-site-configuration]
                   :on-failure      [:update-site-configuration/error-handler]}
                  {:method          :get
                   :uri             layer-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-layers]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             base-layer-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-base-layers]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             base-layer-group-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-base-layer-groups]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             descriptor-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-descriptors]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             classification-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-classifications]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             organisation-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-organisations]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             category-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-categories]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             keyed-layers-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-keyed-layers]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             rich-layers-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:map/update-rich-layers]
                   :on-failure      [:ajax/default-err-handler]}
                  {:method          :get
                   :uri             dynamic-pills-url
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:update-dynamic-pills]
                   :on-failure      [:ajax/default-err-handler]}]}))

;;; Stories
;;;
;;; Read live from the kiosk's WordPress (story-map plugin), so edits there
;;; reach the kiosk on the next refresh. A refresh never changes the story a
;;; visitor is in: it waits until the story is closed (see `kiosk-reset`).
;;;
;;; Each step's map view is a Seamap save state, applied with Seamap's own
;;; :merge-state. Save states are immutable, so they are cached by shortcode
;;; for the life of the page; every story's are fetched as soon as the stories
;;; arrive (see `warm-up`).

(def stories-refresh-ms (* 5 60 1000))

(defn- stories-url [db]
  (str (get-in db [:config :url-base :wordpress-url-base])
       "wp-json/wp/v2/story_map?acf_format=standard&per_page=100"))

(defn- response->story [{:keys [id title modified_gmt acf]}]
  {:id        id
   :title     (decode-html-entities (:rendered title))
   :summary   (:description acf)
   :image     (or (:image acf) nil) ; ACF returns false for no image
   :read-time (:read_time acf)
   ;; Its marker on the home map, if it has a location.
   :marker    (let [{:keys [marker_lat marker_lon marker_icon]} acf]
                (when (and (number? marker_lat) (number? marker_lon))
                  {:lat marker_lat :lon marker_lon :icon (not-empty marker_icon)}))
   :revision  modified_gmt
   :steps     (mapv (fn [{:keys [subtitle description shortcode media]}]
                      {:title     subtitle
                       :content   description
                       :shortcode shortcode
                       :media     (vec (or media []))})
                    (or (:map_links acf) []))})

(defn current-story [{{:keys [stories story-id]} :kiosk}]
  (first (filter #(= story-id (:id %)) stories)))

(defn- current-shortcode [db]
  (get-in (current-story db) [:steps (get-in db [:kiosk :step-index]) :shortcode]))

;;; Flight targets (see `imas-seamap.tmag-kiosk.flight`): a view, {:zoom
;;; :center} or {:bounds}, and the catalogue base layer shown there.

(defn- home-target [db]
  (assoc (home-view db) :basemap (first (get-in db [:map :grouped-base-layers]))))

(defn- step-target
  "The view of the step with save state `shortcode`, if it's loaded and has one."
  [db shortcode]
  (when-let [hashstate (get-in db [:kiosk :save-states shortcode])]
    (let [{:keys [zoom center active-base]} (:map (parse-state hashstate))]
      (when (and zoom (seq center))
        {:zoom    zoom
         :center  center
         :basemap (or (first (filter #(= active-base (:id %)) (get-in db [:map :grouped-base-layers])))
                      (get-in db [:map :active-base-layer]))}))))

(defn- first-step-target [db {:keys [steps]}]
  (some->> (get-in steps [0 :shortcode]) (step-target db)))

(defn- step-flights
  "The flights a visitor can take from the current step: back, next, home."
  [db]
  (let [steps (:steps (current-story db))
        index (get-in db [:kiosk :step-index])
        to    #(some->> (get-in steps [% :shortcode]) (step-target db))]
    (filterv some? [(when (pos? index) (to (dec index)))
                    (to (inc index))
                    (home-target db)])))

(defn- prepare-flights
  "The flights to keep ready (see `imas-seamap.tmag-kiosk.flight/prepare!`).
  With a step's map showing, those on from it. On a story's intro card, the map
  still home, just the flight into its first step, decoded while the visitor
  reads (choosing a story doesn't move the map). At home, none: the tiles on
  screen only, rather than every story's first flight."
  [db]
  (let [leaflet-map (get-in db [:map :leaflet-map])
        home        (home-target db)
        {:keys [story-id landed-step]} (:kiosk db)
        step        (some->> (current-shortcode db) (step-target db))]
    (when (and leaflet-map (:basemap home))
      (cond
        (and story-id landed-step)
        (when step
          {:leaflet-map leaflet-map
           :from        step
           :flights     (step-flights db)})
        story-id
        {:leaflet-map leaflet-map
         :from        home
         :flights     (if-let [first-step (first-step-target db (current-story db))] [first-step] [])}
        :else
        {:leaflet-map leaflet-map
         :from        home
         :flights     []}))))

(defn load-stories [{:keys [db]} _]
  {:http-xhrio {:method          :get
                :uri             (stories-url db)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success      [:kiosk.stories/loaded]
                :on-failure      [:kiosk.stories/load-failed]}})

(defn stories-loaded
  "Takes in loaded stories, unless one is open (then they wait for the reset).
  If they change the story list's width (`one-column?`), the map moves at once
  to the home view for it, unless the tour has it."
  [{:keys [db]} [_ response]]
  (let [stories  (->> response (map response->story) (filter (comp seq :steps)) vec)
        browsing (not (get-in db [:kiosk :story-id]))
        resized? (and browsing
                      (not (get-in db [:kiosk :tour]))
                      (not= (one-column? stories) (one-column? (get-in db [:kiosk :stories]))))
        db       (if browsing
                   (assoc-in db [:kiosk :stories] stories)
                   (assoc-in db [:kiosk :pending-stories] stories))]
    {:db             db
     :dispatch-n     [(when resized? [:map/update-map-view (assoc (home-view db) :instant? true)])
                      [:kiosk/warm-up]]
     :dispatch-later {:ms stories-refresh-ms :dispatch [:kiosk.stories/load]}}))

;;; Warming up. Every story must be ready before a visitor arrives: Begin, or a
;;; tap on a step, flies on the next frame, and lands on layers that are
;;; already cached. So, as soon as the stories and the catalogue are in:
;;;
;;; - every step's save state is fetched;
;;; - every flight of every story has its tiles fetched into the tile cache
;;;   (`:kiosk/prefetch-flights`); a story's first flight is decoded once its
;;;   intro card opens (`prepare-flights`);
;;; - behind the attract screen, the map visits every step, so its layers'
;;;   tiles (and its photos) are cached too, and its layers' rings beyond the
;;;   screen kept in the tile cache (`tour-next`, and
;;;   `imas-seamap.tmag-kiosk.map.tiers/keep-surroundings!`);
;;; - the floor (the kiosk's whole extent, at its minimum zoom) is fetched and
;;;   kept in the tile cache (`:kiosk.map/keep-floors`).
;;;
;;; Each is done once per story revision, or per step, so a refresh that brings
;;; new ones warms just those up.

(defn- ready?
  "Whether the map and catalogue are in, so steps' views can be worked out."
  [db]
  (boolean (and (:initialised db)
                (get-in db [:map :leaflet-map])
                (seq (get-in db [:map :grouped-base-layers])))))

(defn- all-shortcodes [db]
  (->> (get-in db [:kiosk :stories]) (mapcat :steps) (keep :shortcode) distinct))

(defn- story-flights
  "Every flight of a story, into it from home first: then between its steps,
  and home from each."
  [db {:keys [steps]}]
  (let [home    (home-target db)
        targets (mapv #(step-target db (:shortcode %)) steps)]
    (when (every? some? targets)
      (concat [{:from home :to (first targets)}]
              (for [[a b] (partition 2 1 targets)] {:from a :to b})
              (for [t (rest targets)] {:from t :to home})))))

(defn warm-up [{:keys [db]} _]
  (let [kiosk     (:kiosk db)
        uncached  (remove #(get-in kiosk [:save-states %]) (all-shortcodes db))
        to-fetch  (remove (set (:save-states-requested kiosk)) uncached)
        all-in?   (and (ready? db) (empty? uncached))
        revision  (juxt :id :revision)
        unfetched (when all-in? (remove #(contains? (:prefetched kiosk) (revision %)) (:stories kiosk)))
        flights   (keep #(story-flights db %) unfetched)]
    (cond-> {:db         (cond-> (update-in db [:kiosk :save-states-requested] (fnil into #{}) to-fetch)
                           (seq unfetched) (update-in [:kiosk :prefetched] (fnil into #{}) (map revision unfetched)))
             :dispatch-n (mapv #(vector :kiosk.save-state/fetch %) to-fetch)}
      ;; Into each story first, as those are needed first.
      (seq flights)
      (assoc :kiosk/prefetch-flights {:leaflet-map (get-in db [:map :leaflet-map])
                                      :flights     (concat (map first flights) (mapcat rest flights))})
      (and all-in? (not (get-in db [:kiosk :story-id])) (prepare-flights db))
      (assoc :kiosk/prepare-flights (prepare-flights db))
      all-in?
      (assoc :kiosk.map/keep-floors {:leaflet-map (get-in db [:map :leaflet-map])
                                     :basemaps    (distinct (map :basemap (cons (home-target db)
                                                                                (keep #(step-target db %) (all-shortcodes db)))))}
             ;; Steps no longer in any story
             :kiosk.map/drop-surroundings (all-shortcodes db))
      all-in?
      (update :dispatch-n conj [:kiosk.tour/start]))))

(defn- video? [url]
  (boolean (re-find #"(?i)\.(mp4|m4v|webm|mov)(\?|$)" url)))

(defn tour-start
  "Starts the tour of the steps not yet visited, if the attract screen is up
  to hide it."
  [{:keys [db]} _]
  (let [{:keys [attract? tour toured]} (:kiosk db)
        pending (remove (set toured) (all-shortcodes db))]
    (when (and attract? (not tour) (seq pending) (ready? db))
      {:db                    (assoc-in db [:kiosk :tour] (vec pending))
       :kiosk/suspend-flights true
       :dispatch              [:kiosk.tour/next]})))

(defn tour-next
  "Visits the tour's next step: applies its save state, moves the map there at
  once, and waits for its layers to load; then on to the next. At the end, back
  home. Leaving the attract screen ends it early (`attract-exit`)."
  [{:keys [db]} _]
  (when-let [[shortcode & more] (seq (get-in db [:kiosk :tour]))]
    (let [hashstate (get-in db [:kiosk :save-states shortcode])
          target    (step-target db shortcode)
          images    (->> (get-in db [:kiosk :stories]) (mapcat :steps)
                         (filter #(= shortcode (:shortcode %))) (mapcat :media) (keep :url)
                         (remove video?) distinct)]
      (if (and hashstate target)
        {:db               (update db :kiosk assoc :tour (vec more) :keep-view? true)
         :dispatch         [:merge-state hashstate]
         :kiosk/tour-visit {:leaflet-map (get-in db [:map :leaflet-map])
                            :to          target
                            :images      images
                            :keep-as     shortcode
                            :on-loaded   [:kiosk.tour/visited shortcode]}}
        {:db       (update db :kiosk assoc :tour (vec more))
         :dispatch [:kiosk.tour/visited shortcode]}))))

(defn tour-visited [{:keys [db]} [_ shortcode]]
  (when (get-in db [:kiosk :tour])
    (let [db (update-in db [:kiosk :toured] (fnil conj #{}) shortcode)]
      (if (seq (get-in db [:kiosk :tour]))
        {:db db :dispatch [:kiosk.tour/next]}
        {:db                    (update db :kiosk dissoc :tour)
         :kiosk/suspend-flights false
         :dispatch              [:re-boot]}))))

(defn stories-load-failed [_ [_ response]]
  ;; Keep showing the last stories we had, and try again next cycle.
  (js/console.error "Could not load kiosk stories" response)
  {:dispatch-later {:ms stories-refresh-ms :dispatch [:kiosk.stories/load]}})

;; A story opens on its intro card (`:intro?`), with the map left home; Begin
;; flies to its first step. Meanwhile that flight is prepared.
(defn open-story [{:keys [db]} [_ story-id]]
  (let [db         (update db :kiosk assoc :story-id story-id :step-index 0 :media-index 0 :intro? true)
        uncached   (->> (current-story db) :steps (map :shortcode) distinct
                        (remove #(get-in db [:kiosk :save-states %])))
        prepare    (prepare-flights db)]
    (cond-> {:db         db
             :dispatch-n (mapv #(vector :kiosk.save-state/fetch %) uncached)}
      prepare (assoc :kiosk/prepare-flights prepare))))

(declare apply-step-view)

(defn begin-flight
  "Begin's flight to the first step, unless its map is already showing (back on
  the intro card from it). Separate from `begin-story`, so the flight sets off
  at the tap while the cards animate (see `views/begin!`)."
  [{:keys [db] :as cofx} event]
  (when-not (= 0 (get-in db [:kiosk :landed-step]))
    (apply-step-view cofx event)))

(defn begin-story
  "From the intro card to the first step's card (`begin-flight` moves the map)."
  [db _]
  (update db :kiosk dissoc :intro?))

(defn show-intro
  "Back from the first step to the intro card; the map stays put."
  [db _]
  (update db :kiosk #(-> % (assoc :intro? true :media-index 0) (dissoc :feature-media :feature-query))))

(defn go-to-step [{:keys [db]} [_ index]]
  (when (< -1 index (count (:steps (current-story db))))
    {:db       (update db :kiosk #(-> % (assoc :step-index index :media-index 0) (dissoc :feature-media :feature-query)))
     :dispatch [:kiosk.step/apply-view]}))

;;; Feature media. Tapping a step's map asks the layers under the tap for their
;;; features (WMS GetFeatureInfo, as JSON); features carrying media fill the
;;; media card in place of the step's own, until the visitor moves on. Only
;;; once the step's flight has landed: elsewhere there's no media card.

;; How far around a tap a feature counts as tapped, in design units (see
;; `--kiosk-u` in tmag-kiosk.scss): it's a finger, not a pointer.
(def feature-tolerance 15)

(def ^:private media-re #"(?i)\.(jpe?g|png|webp|gif|mp4|m4v|webm|mov)(\?|$)")

(defn- parse-overlay [overlay]
  (if (string? overlay)
    (try (js->clj (js/JSON.parse overlay) :keywordize-keys true)
         (catch :default _ nil))
    overlay))

(defn feature->media
  "A media item, in a story step's form ({:url :lat :lon :caption :overlay}),
  from a GeoJSON feature, or nil if it carries no photo or video. Takes the
  stories' own property names, or those of the KIOSK layers so far: the media
  from `media_url`, `media_URL` or `image_orig` before `url` (the iNaturalist
  layer's `url` is its web page); the place from `lat`/`lon`, `latitude`/
  `longitude` or the feature's point; the caption from `caption`, or the
  species' common and scientific names; readings from `overlay`, or
  `temperature` and `depth`."
  [{p :properties {:keys [type coordinates]} :geometry}]
  (let [url  (some #(let [v (get p %)] (when (and (string? v) (re-find media-re v)) v))
                   [:media_url :media_URL :image_orig :url])
        [x y] (when (= "Point" type) coordinates)
        lat  (or (:lat p) (:latitude p) y)
        lon  (or (:lon p) (:longitude p) x)
        {common :common_name sci :scientific_name} p]
    (when url
      {:url     url
       :lat     lat
       :lon     lon
       :caption (or (:caption p)
                    (cond (and common sci) (str common " (" sci ")")
                          :else            (or common sci)))
       :overlay (or (parse-overlay (:overlay p))
                    (not-empty (select-keys p [:temperature :depth])))})))

(defn- feature-request
  "GetFeatureInfo for `layer`'s features within `feature-tolerance` of the tap."
  [layer {:keys [lat lng]} {:keys [size bounds]} request-id]
  (let [{:keys [north south east west]} bounds
        ;; A 101px window round the tap, at the map's own scale.
        dx     (* 50 (/ (- east west) (:x size)))
        dy     (* 50 (/ (- north south) (:y size)))
        buffer (js/Math.round (* feature-tolerance (min (/ (:x size) 1920) (/ (:y size) 1080))))
        names  (map-utils/layer-name layer)]
    {:method          :get
     :uri             (:server_url layer)
     :params          {:SERVICE       "WMS"
                       :VERSION       "1.1.1"
                       :REQUEST       "GetFeatureInfo"
                       :LAYERS        names
                       :QUERY_LAYERS  names
                       :STYLES        ""
                       :SRS           "EPSG:4326"
                       :BBOX          (str (- lng dx) "," (- lat dy) "," (+ lng dx) "," (+ lat dy))
                       :WIDTH         101
                       :HEIGHT        101
                       :X             50
                       :Y             50
                       :BUFFER        buffer
                       :FEATURE_COUNT 50
                       :INFO_FORMAT   "application/json"}
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success      [:kiosk.features/loaded request-id]
     :on-failure      [:kiosk.features/failed request-id]}))

(defn map-clicked
  "The kiosk's `:map/clicked`: on a step whose map has landed, asks the WMS
  layers under the tap for their features."
  [{:keys [db]} [_ leaflet-props point]]
  (let [{:keys [story-id intro? landed-step step-index]} (:kiosk db)
        layers     (->> (map-utils/displayed-layers-under-point (map-utils/visible-layers (:map db)) point db)
                        (filter #(and (:server_url %) (re-find #"^wms" (name (or (:layer_type %) "")))))
                        (remove #(map-utils/is-insecure? (:server_url %))))
        request-id (random-uuid)]
    (when (and story-id (not intro?) (= landed-step step-index) (seq layers))
      {:db         (assoc-in db [:kiosk :feature-query] {:id request-id :remaining (count layers) :media [] :point point})
       :http-xhrio (mapv #(feature-request % point leaflet-props request-id) layers)})))

(defn- nearest-first
  "`media` sorted by distance from `point` ({:lat :lng}), nearest first; those
  without a place last."
  [media {:keys [lat lng]}]
  (let [k (js/Math.cos (* lat (/ js/Math.PI 180)))]
    (sort-by (fn [{mlat :lat mlon :lon}]
               (if (and (number? mlat) (number? mlon))
                 (+ (js/Math.pow (- mlat lat) 2) (js/Math.pow (* k (- mlon lng)) 2))
                 js/Infinity))
             media)))

(defn- distinct-urls
  "`media` without repeats of a url (layers can share features), in order."
  [media]
  (second (reduce (fn [[seen out] {:keys [url] :as m}]
                    (if (seen url) [seen out] [(conj seen url) (conj out m)]))
                  [#{} []]
                  media)))

(defn- features-in
  "One layer's answer (`media`) to the tap `request-id`. Once all are in, any
  media found fills the media card, nearest the tap first; none leaves it as it
  was."
  [db request-id media]
  (if (not= request-id (get-in db [:kiosk :feature-query :id]))
    db ; an earlier tap, or the visitor has moved on
    (let [db                (-> db
                                (update-in [:kiosk :feature-query :remaining] dec)
                                (update-in [:kiosk :feature-query :media] into media))
          {:keys [remaining point]
           found :media}    (get-in db [:kiosk :feature-query])]
      (cond
        (pos? remaining) db
        (seq found)      (update db :kiosk #(-> %
                                                (dissoc :feature-query)
                                                (assoc :feature-media (distinct-urls (nearest-first found point))
                                                       :media-index 0)))
        :else            (update db :kiosk dissoc :feature-query)))))

(defn features-loaded [db [_ request-id response]]
  (features-in db request-id (keep feature->media (:features response))))

(defn features-failed [db [_ request-id _response]]
  (features-in db request-id []))

(defn select-media
  "Shows the current step's `index`th media item in the media card."
  [db [_ index]]
  (assoc-in db [:kiosk :media-index] index))

(defn apply-step-view
  "Flies to the current step's map view if its save state has arrived (see
  `imas-seamap.tmag-kiosk.flight`); otherwise `save-state-loaded` does it on
  arrival. The step's layers are applied once the flight lands."
  [{:keys [db]} _]
  (let [shortcode   (current-shortcode db)
        leaflet-map (get-in db [:map :leaflet-map])
        target      (step-target db shortcode)]
    (when-let [hashstate (get-in db [:kiosk :save-states shortcode])]
      (if (and leaflet-map target)
        {:kiosk/fly {:leaflet-map  leaflet-map
                     :to           target
                     :from-basemap (get-in db [:map :active-base-layer])
                     :on-land      [:kiosk.step/apply-layers shortcode]}}
        {:db       (assoc-in db [:kiosk :landed-step] (get-in db [:kiosk :step-index]))
         :dispatch [:merge-state hashstate]}))))

(defn apply-step-layers
  "Applies a step's save state, apart from its view (the flight sees to that),
  and prepares the flights on from it. Nothing to do if the visitor has moved
  on from that step."
  [{:keys [db]} [_ shortcode]]
  (when (= shortcode (current-shortcode db))
    (let [leaflet-map (get-in db [:map :leaflet-map])
          from        (step-target db shortcode)]
      (cond-> {:db       (update db :kiosk assoc
                                 :keep-view? true
                                 ;; The step whose map is showing (see `:kiosk/media-markers`)
                                 :landed-step (get-in db [:kiosk :step-index]))
               :dispatch [:merge-state (get-in db [:kiosk :save-states shortcode])]}
        (and leaflet-map from)
        (assoc :kiosk/prepare-flights (prepare-flights db))))))

(defn start-again
  "Closes the story at once, so the story list is back while the map flies
  home; then resets the map (`re-boot`). The stories' markers wait for it to
  land (`:homeward?`)."
  [{:keys [db]} _]
  (let [db (update db :kiosk #(assoc (kiosk-reset %) :homeward? true))]
    (if-let [leaflet-map (get-in db [:map :leaflet-map])]
      {:db        db
       :kiosk/fly {:leaflet-map  leaflet-map
                   :to           (home-target db)
                   :from-basemap (get-in db [:map :active-base-layer])
                   :on-land      [:re-boot true]}}
      {:db db :dispatch [:re-boot]})))

(defn update-map-view
  "Seamap's, except that the view request from applying a step's layers is
  dropped (see `apply-step-layers`)."
  [db event]
  (if (get-in db [:kiosk :keep-view?])
    (update db :kiosk dissoc :keep-view?)
    (mevents/update-map-view db event)))

(defn fetch-save-state [{:keys [db]} [_ shortcode]]
  {:http-xhrio {:method          :get
                :uri             (append-params-from-map (get-in db [:config :urls :save-state-url]) {:id shortcode})
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success      [:kiosk.save-state/loaded shortcode]
                :on-failure      [:kiosk.save-state/load-failed shortcode]}})

(defn save-state-loaded [{:keys [db]} [_ shortcode response]]
  (if-let [hashstate (:hashstate (first response))]
    (let [db (assoc-in db [:kiosk :save-states shortcode] hashstate)]
      (cond
        ;; On the intro card, prepare the flight into the story (the map stays
        ;; home until Begin).
        (and (= shortcode (current-shortcode db)) (get-in db [:kiosk :intro?]))
        (cond-> {:db db}
          (prepare-flights db) (assoc :kiosk/prepare-flights (prepare-flights db)))
        ;; Only move the map if the visitor is still on the step that asked.
        (= shortcode (current-shortcode db))
        {:db db :dispatch [:kiosk.step/apply-view]}
        ;; The last of them: on with the warm-up.
        (every? #(get-in db [:kiosk :save-states %]) (all-shortcodes db))
        {:db db :dispatch [:kiosk/warm-up]}
        :else
        {:db db}))
    (do (js/console.error "No save state for shortcode" shortcode)
        {})))

(defn save-state-load-failed [{:keys [db]} [_ shortcode response]]
  (js/console.error "Could not load save state" shortcode response)
  ;; Asked for again at the next warm-up.
  {:db (update-in db [:kiosk :save-states-requested] disj shortcode)})
