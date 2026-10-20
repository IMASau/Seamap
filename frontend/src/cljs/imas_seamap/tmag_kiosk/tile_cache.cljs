;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.tile-cache
  "An in-memory cache of map tiles, owned by the app rather than the browser.

  Revisiting a story step should be instant, but the browser can't help: IMAS
  GeoServer sends `Cache-Control: no-store` on its WMS tiles, and the kiosk runs
  InPrivate, so the HTTP cache starts empty anyway. Instead, tiles are fetched
  (with CORS) into Blobs, kept in memory and shown from blob: URLs.

  `install!` replaces `L.TileLayer.prototype.createTile`, so it covers Leaflet
  tile layers and WMS layers (which inherit it); layer types with their own
  `createTile`, such as esri-leaflet's tiled map layers, load as normal. It
  patches the prototype for the whole page, so only the kiosk installs it.

  - Hits are shown straight from memory, with no request.
  - A tile already being fetched isn't fetched again, fetches are queued with
    a limit per server, and tiles abandoned while queued aren't fetched.
  - Only image responses are kept, so a server's error document is never
    cached.
  - A server that refuses CORS can't be cached: once it has failed repeatedly
    without ever succeeding, its tiles load as normal, outside the cache, for
    the rest of the session.
  - Least-recently-used tiles are evicted once `budget-bytes` is exceeded, but
    never one on screen.
  - `prefetch!` fetches tiles in the background, behind anything on screen.

  The cache lasts as long as the page: a reload or restart starts it empty."
  (:require ["leaflet" :as L]
            [clojure.string :as string]
            [goog.object :as gobject]))

;; A hypothesis to benchmark on the kiosk hardware, not an established safe
;; figure. Map tiles are mostly 5-50 kB, so this is tens of thousands of tiles.
(def budget-bytes (* 1024 1024 1024))

;; url -> #js {:objectUrl :size :blob}, oldest first (a JS Map keeps insertion order,
;; and a hit is re-inserted at the end).
(defonce ^:private entries (js/Map.))
;; url -> {:tiles #js [img …] :prefetch? (volatile! bool) :promise Promise of an
;; object URL, or of nil}
(defonce ^:private in-flight (js/Map.))
(defonce ^:private no-cors-origins (atom #{}))
;; URLs a server answered with something other than an image (eg a 404 beyond a
;; basemap's deepest zoom level), not fetched again by `blob`.
(defonce ^:private failed (js/Set.))
(defonce ^:private counts (atom {:bytes 0 :hits 0 :misses 0 :uncached 0}))

(defn- origin [url]
  (.-origin (js/URL. url js/location.href)))

(defn- cacheable? [url]
  (and (or (string/starts-with? url "https:") (string/starts-with? url "http:"))
       (not (@no-cors-origins (origin url)))))

(defn- on-screen? [object-url]
  (some? (.querySelector js/document (str "img[src=\"" object-url "\"]"))))

(defn- evict! []
  (let [urls (.keys entries)]
    (loop []
      (when (> (:bytes @counts) budget-bytes)
        (let [next-url (.next urls)]
          (when-not (.-done next-url)
            (let [url   (.-value next-url)
                  entry (.get entries url)]
              (when-not (on-screen? (.-objectUrl entry))
                (.delete entries url)
                (js/URL.revokeObjectURL (.-objectUrl entry))
                (swap! counts update :bytes - (.-size entry))))
            (recur)))))))

(defn- store! [url blob]
  (let [object-url (js/URL.createObjectURL blob)]
    (.set entries url #js {:objectUrl object-url :size (.-size blob) :blob blob})
    (swap! counts update :bytes + (.-size blob))
    (evict!)
    object-url))

(defn- image? [response]
  (string/starts-with? (or (.get (.-headers response) "content-type") "") "image/"))

;;; Fetching. A map flight asks for hundreds of tiles across its intermediate
;;; zooms, most abandoned before they arrive, so fetches are queued per server
;;; and limited to `max-fetches-per-origin` at a time. The newest go first (they
;;; are for the view being landed on), and a tile Leaflet has abandoned by its
;;; turn is skipped rather than fetched (unless it's also wanted by `blob`).
;;; Prefetches wait in a queue of their own, first in first out, and only take
;;; a server's spare fetches.

(def ^:private max-fetches-per-origin 6)

;; A failed fetch can't say whether CORS was refused or the network failed, so
;; a server is only given up on after this many failures and no successes.
(def ^:private failures-before-no-cors 5)

(defonce ^:private origins (atom {})) ; origin -> {:active n :ok? bool :failures n}
(defonce ^:private queues (js/Map.))  ; origin -> array of jobs, newest last
(defonce ^:private background (js/Map.)) ; origin -> array of urls to prefetch, next first

(defn- wanted?
  "Leaflet gives a tile it abandons a placeholder source, or removes it."
  [^js tile]
  (and (.-isConnected tile) (not (.getAttribute tile "src"))))

(declare pump!)

(defn- run-job! [from {:keys [url resolve]}]
  (swap! origins update-in [from :active] (fnil inc 0))
  ;; Bypass the HTTP cache: this cache is the one that counts, and a copy the
  ;; browser kept from an ordinary <img> load can fail a CORS fetch.
  (-> (js/fetch url #js {:mode "cors" :credentials "omit" :cache "no-store"})
      (.then (fn [response]
               (swap! origins assoc-in [from :ok?] true)
               (if (and (.-ok response) (image? response))
                 (.blob response)
                 (do (.add failed url) nil))))
      (.then (fn [blob] (resolve (when blob (store! url blob)))))
      (.catch (fn [_]
                (let [{:keys [ok? failures]} (get (swap! origins update-in [from :failures] (fnil inc 0)) from)]
                  (when (and (not ok?) (>= failures failures-before-no-cors))
                    (swap! no-cors-origins conj from)))
                (resolve nil)))
      (.finally (fn []
                  (.delete in-flight url)
                  (swap! origins update-in [from :active] (fnil dec 1))
                  (pump! from)))))

(declare new-job!)

(defn- spare? [from]
  (< (get-in @origins [from :active] 0) max-fetches-per-origin))

(defn- pump! [from]
  (let [queue (or (.get queues from) #js [])
        later (or (.get background from) #js [])]
    (loop []
      (when (and (pos? (.-length queue)) (spare? from))
        (let [{:keys [url tiles prefetch? resolve] :as job} (.pop queue)]
          (if (or @prefetch? (some wanted? tiles))
            (run-job! from job)
            (do (.delete in-flight url)
                (resolve nil))))
        (recur)))
    (loop []
      (when (and (pos? (.-length later)) (spare? from))
        (let [url (.shift later)]
          (when-not (or (.has entries url) (.has in-flight url) (.has failed url))
            (run-job! from (new-job! url nil))))
        (recur)))))

(defn- new-job!
  "Registers a fetch of `url` as in flight, for the image `tile` (or nil for a
  prefetch), and returns its job."
  [url tile]
  (let [tiles     (if tile #js [tile] #js [])
        prefetch? (volatile! (nil? tile))
        resolve   (atom nil)
        promise   (js/Promise. (fn [res _] (reset! resolve res)))]
    (.set in-flight url {:tiles tiles :prefetch? prefetch? :promise promise})
    {:url url :tiles tiles :prefetch? prefetch? :resolve @resolve}))

(defn- fetch-tile
  "A Promise of the tile's object URL, or of nil if it couldn't be cached (load
  it from its own URL instead). `tile` is the image that wants it, or nil for a
  prefetch, which is fetched whether or not an image wants it."
  [url tile]
  (if-let [{:keys [tiles prefetch? promise]} (.get in-flight url)]
    (do (if tile (.push tiles tile) (vreset! prefetch? true))
        promise)
    (let [from (origin url)
          job  (new-job! url tile)]
      (when-not (.has queues from) (.set queues from #js []))
      (.push (.get queues from) job)
      ;; Let Leaflet put the tile in the page first, so it counts as wanted.
      (js/queueMicrotask #(pump! from))
      (:promise (.get in-flight url)))))

(defn- create-tile
  "Leaflet 1.9's `TileLayer.createTile`, with the image's source taken from the
  cache."
  [^js layer coords done]
  (let [tile    (js/document.createElement "img")
        url     (.getTileUrl layer coords)
        options (.-options layer)
        dom-event (.-DomEvent L/default)]
    (.on dom-event tile "load" (.bind (.-_tileOnLoad layer) layer done tile))
    (.on dom-event tile "error" (.bind (.-_tileOnError layer) layer done tile))
    (when (or (.-crossOrigin options) (= "" (.-crossOrigin options)))
      (set! (.-crossOrigin tile) (if (true? (.-crossOrigin options)) "" (.-crossOrigin options))))
    (when (string? (.-referrerPolicy options))
      (set! (.-referrerPolicy tile) (.-referrerPolicy options)))
    (set! (.-alt tile) "")
    ;; The tile's own URL, as its source may be an object URL.
    (set! (.-kioskUrl tile) url)
    (if-let [entry (.get entries url)]
      (do (.delete entries url)       ; most recently used
          (.set entries url entry)
          (swap! counts update :hits inc)
          (set! (.-src tile) (.-objectUrl entry)))
      (if (cacheable? url)
        (do (swap! counts update :misses inc)
            (.then (fetch-tile url tile)
                   (fn [object-url]
                     ;; Leave abandoned tiles alone.
                     (when (wanted? tile)
                       (set! (.-src tile) (or object-url url))))))
        (do (swap! counts update :uncached inc)
            (set! (.-src tile) url))))
    tile))

(defn blob
  "A Promise of the tile at `url` as a Blob, from the cache or fetched into it,
  or of nil if it can't be had (eg its server refuses CORS)."
  [url]
  (if-let [entry (.get entries url)]
    (js/Promise.resolve (.-blob entry))
    (if (and (cacheable? url) (not (.has failed url)))
      (.then (fetch-tile url nil)
             (fn [_] (some-> (.get entries url) .-blob)))
      (js/Promise.resolve nil))))

(defn prefetch!
  "Fetches the tiles at `urls` into the cache in the background, in order,
  behind any tile on screen."
  [urls]
  (let [origins (js/Set.)]
    (doseq [url urls
            :when (and (cacheable? url) (not (.has entries url)) (not (.has failed url))
                       (not (.has in-flight url)))]
      (let [from (origin url)]
        (when-not (.has background from) (.set background from #js []))
        (.push (.get background from) url)
        (.add origins from)))
    (doseq [from (es6-iterator-seq (.values origins))]
      (pump! from))))

(defonce ^:private installed? (atom false))

(defn install! []
  (when (compare-and-set! installed? false true)
    (gobject/set (.. L/default -TileLayer -prototype) "createTile"
                 (fn [coords done]
                   (this-as layer (create-tile layer coords done))))))

(defn ^:export stats
  "For checking the cache from the console:
  `imas_seamap.tmag_kiosk.tile_cache.stats()`."
  []
  (clj->js (assoc @counts
                  :entries        (.-size entries)
                  :inFlight       (.-size in-flight)
                  :failed         (.-size failed)
                  :prefetching    (reduce + (map #(.-length %) (es6-iterator-seq (.values background))))
                  :noCorsOrigins  (vec @no-cors-origins)
                  :budgetBytes    budget-bytes)))
