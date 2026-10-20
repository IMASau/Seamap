;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.events
  (:require [ajax.core :as ajax]
            [imas-seamap.db :as db]
            [imas-seamap.map.events :as mevents]
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

;; The home view, where the kiosk starts and returns to: the waters around
;; Tasmania, from Bass Strait to the south of the island. Seamap's own (the
;; start-up layer's bounds) is all of Australia.
(def home-bounds {:west 142.5 :south -44.5 :east 149.5 :north -39.0})

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
  (cond-> (dissoc kiosk :story-id :step-index :pending-stories :keep-view? :media-index :landed-step)
    pending-stories (assoc :stories pending-stories)))

(declare prepare-flights)

(defn re-boot [{:keys [db]} _]
  (let [kiosk (kiosk-reset (:kiosk db))
        db (merge-in db/default-db (ajax-loaded-info db))
        startup-layers (get-in db [:map :keyed-layers :startup] [])
        db (-> db
               (assoc-in [:map :active-layers] startup-layers)
               (assoc-in [:map :active-base-layer] (first (get-in db [:map :grouped-base-layers])))
               (assoc :initialised true)
               (assoc :kiosk kiosk))]
    (cond-> {:db         db
             :dispatch-n [[:map/update-map-view {:bounds home-bounds :instant? true}]
                          ;; Anything new since the last warm-up (eg refreshed stories)
                          [:kiosk/warm-up]]
             :local-storage/remove
             {:name :seamap-app-state}}
      (prepare-flights db) (assoc :kiosk/prepare-flights (prepare-flights db)))))

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
  {:bounds  home-bounds
   :basemap (first (get-in db [:map :grouped-base-layers]))})

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
  "The flights to keep ready (see `imas-seamap.tmag-kiosk.flight/prepare!`):
  in a story, those from its current step; otherwise, those from home into
  each story. Those into each story are kept ready throughout, as a visitor
  may return home and choose any of them."
  [db]
  (let [leaflet-map (get-in db [:map :leaflet-map])
        home        (home-target db)
        firsts      (keep #(first-step-target db %) (get-in db [:kiosk :stories]))
        step        (some->> (current-shortcode db) (step-target db))]
    (when (and leaflet-map (:basemap home))
      (if (get-in db [:kiosk :story-id])
        (when step
          {:leaflet-map leaflet-map
           :from        step
           :flights     (step-flights db)
           :also        (mapv #(hash-map :from home :to %) firsts)})
        {:leaflet-map leaflet-map
         :from        home
         :flights     (vec firsts)}))))

(defn load-stories [{:keys [db]} _]
  {:http-xhrio {:method          :get
                :uri             (stories-url db)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success      [:kiosk.stories/loaded]
                :on-failure      [:kiosk.stories/load-failed]}})

(defn stories-loaded [{:keys [db]} [_ response]]
  (let [stories (->> response (map response->story) (filter (comp seq :steps)) vec)]
    {:db             (if (get-in db [:kiosk :story-id])
                       (assoc-in db [:kiosk :pending-stories] stories)
                       (assoc-in db [:kiosk :stories] stories))
     :dispatch       [:kiosk/warm-up]
     :dispatch-later {:ms stories-refresh-ms :dispatch [:kiosk.stories/load]}}))

;;; Warming up. Every story must be ready before a visitor arrives: a tap on a
;;; story, or on a step, flies on the next frame, and lands on layers that are
;;; already cached. So, as soon as the stories and the catalogue are in:
;;;
;;; - every step's save state is fetched;
;;; - every flight of every story has its tiles fetched into the tile cache
;;;   (`:kiosk/prefetch-flights`), and the flights from home into each story
;;;   are decoded (`prepare-flights`);
;;; - behind the attract screen, the map visits every step, so its layers'
;;;   tiles (and its photos) are cached too (`tour-next`).
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

(defn open-story [{:keys [db]} [_ story-id]]
  (let [db         (update db :kiosk assoc :story-id story-id :step-index 0 :media-index 0)
        uncached   (->> (current-story db) :steps (map :shortcode) distinct
                        (remove #(get-in db [:kiosk :save-states %])))]
    {:db         db
     :dispatch-n (conj (mapv #(vector :kiosk.save-state/fetch %) uncached)
                       [:kiosk.step/apply-view])}))

(defn go-to-step [{:keys [db]} [_ index]]
  (when (< -1 index (count (:steps (current-story db))))
    {:db       (update db :kiosk assoc :step-index index :media-index 0)
     :dispatch [:kiosk.step/apply-view]}))

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
  "Flies home, then resets the kiosk (`re-boot`)."
  [{:keys [db]} _]
  (if-let [leaflet-map (get-in db [:map :leaflet-map])]
    {:kiosk/fly {:leaflet-map  leaflet-map
                 :to           (home-target db)
                 :from-basemap (get-in db [:map :active-base-layer])
                 :on-land      [:re-boot]}}
    {:dispatch [:re-boot]}))

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
