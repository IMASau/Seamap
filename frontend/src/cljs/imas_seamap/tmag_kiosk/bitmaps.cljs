;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.bitmaps
  "Decoded map tiles (ImageBitmaps) for map flights, which draw them to a canvas
  (see `imas-seamap.tmag-kiosk.flight`).

  A flight can't afford to fetch or decode anything while it moves, so every
  tile of each flight the visitor might take next is decoded beforehand, and
  kept while that flight is still possible. Tiles are held in named sets (one
  per flight path, and one for the tiles on screen): `retain!` adds a set,
  `release!` and `retain-only!` drop them, and a tile is closed (its memory
  freed) once no set holds it.

  A decoded 256px tile is 256 kB, so sets are large: a few hundred MB for each
  flight. Decoding runs up to `max-decodes` at a time (more janks the page),
  fewer while a flight animates (`limit!`)."
  (:require [imas-seamap.tmag-kiosk.tile-cache :as tile-cache]))

(def max-decodes 16)

;; url -> #js {:bitmap ImageBitmap or nil :done? :promise :sets #{set-id}}
(defonce ^:private tiles (js/Map.))
;; set-id -> array of urls
(defonce ^:private sets (js/Map.))
;; url -> the <img> showing it, decoded from if the tile cache can't supply it
;; (a server that refuses CORS)
(defonce ^:private images (js/Map.))
(defonce ^:private queue #js [])  ; urls waiting to decode, next last
(defonce ^:private state (atom {:active 0 :limit max-decodes}))

(defn bitmap
  "The decoded tile at `url`, or nil."
  [url]
  (some-> (.get tiles url) .-bitmap))

(defn- drop-tile! [url ^js tile]
  (.delete tiles url)
  (.delete images url)
  (some-> (.-bitmap tile) .close)
  ((.-resolve tile))) ; anything still waiting on it

(declare pump!)

(defn- source [url]
  (.then (tile-cache/blob url)
         (fn [blob]
           (or blob
               (when-let [^js img (.get images url)]
                 (when (and (.-complete img) (pos? (.-naturalWidth img))) img))))))

(defn- decode! [url ^js tile]
  (swap! state update :active inc)
  (set! (.-decoding tile) true)
  (-> (source url)
      (.then #(when % (js/createImageBitmap %)))
      (.then (fn [bitmap]
               (if (identical? tile (.get tiles url))
                 (set! (.-bitmap tile) bitmap)
                 (some-> bitmap .close)))) ; released while decoding
      (.catch (fn [_] nil))
      (.finally (fn []
                  (set! (.-done tile) true)
                  (swap! state update :active dec)
                  ((.-resolve tile))
                  (pump!)))))

(defn- pump! []
  (loop []
    (let [{:keys [active limit]} @state]
      (when (and (< active limit) (pos? (.-length queue)))
        (let [url  (.pop queue)
              tile (.get tiles url)]
          (when (and tile (not (.-decoding tile)))
            (decode! url tile))
          (recur))))))

(defn- tile-promise
  "Holds `url` for `set-id`, queueing it to decode if it's new (or moving it to
  the front if `urgent?`). A Promise that resolves once it's decoded (or has
  failed)."
  [url set-id urgent?]
  (if-let [tile (.get tiles url)]
    (do (.add (.-sets tile) set-id)
        (when (and urgent? (not (.-decoding tile)))
          (.push queue url))
        (.-promise tile))
    (let [tile #js {:bitmap nil :sets (js/Set. #js [set-id])}]
      (set! (.-promise tile) (js/Promise. (fn [resolve _] (set! (.-resolve tile) resolve))))
      (.set tiles url tile)
      (if urgent? (.push queue url) (.unshift queue url))
      (.-promise tile))))

(defn- unhold! [url set-id]
  (when-let [tile (.get tiles url)]
    (.delete (.-sets tile) set-id)
    (when (zero? (.-size (.-sets tile)))
      (drop-tile! url tile))))

(defn retain!
  "Holds the tiles at `urls` as the set `set-id`, decoding any that aren't
  already, in order. `urgent?` puts them ahead of other sets waiting to decode.
  Returns a Promise that resolves once they're all decoded (or failed)."
  [set-id urls urgent?]
  (let [old (.get sets set-id)]
    (.set sets set-id urls)
    (let [promises (mapv #(tile-promise % set-id urgent?) (if urgent? (reverse urls) urls))]
      ;; Drop the set's old tiles that it no longer has.
      (when old
        (let [now (js/Set. urls)]
          (doseq [url old :when (not (.has now url))]
            (unhold! url set-id))))
      (pump!)
      (js/Promise.all (clj->js promises)))))

(defn release!
  "Drops the set `set-id`, freeing tiles no other set holds."
  [set-id]
  (when-let [urls (.get sets set-id)]
    (.delete sets set-id)
    (doseq [url urls] (unhold! url set-id))))

(defn retain-only!
  "Drops every set but those in `set-ids`."
  [set-ids]
  (let [keep (set set-ids)]
    (doseq [set-id (vec (es6-iterator-seq (.keys sets))) :when (not (keep set-id))]
      (release! set-id))))

(defn image!
  "Notes the <img> showing `url`, to decode the tile from if the tile cache
  can't supply it."
  [url img]
  (.set images url img))

(defn missing
  "How many of `urls` are held but not yet decoded (nor failed)."
  [urls]
  (count (filter #(when-let [^js tile (.get tiles %)] (not (.-done tile))) urls)))

(defn limit!
  "Decodes at most `n` tiles at a time (0 stops decoding)."
  [n]
  (swap! state assoc :limit n)
  (pump!))

(defn ^:export stats
  "For checking from the console: `imas_seamap.tmag_kiosk.bitmaps.stats()`."
  []
  (let [decoded (count (filter #(.-bitmap %) (es6-iterator-seq (.values tiles))))]
    (clj->js (assoc @state
                    :tiles   (.-size tiles)
                    :decoded decoded
                    :mb      (js/Math.round (/ (* decoded 256 256 4) 1024 1024))
                    :queued  (.-length queue)
                    :sets    (into {} (map (fn [[k v]] [(str k) (count v)])) (es6-iterator-seq (.entries sets)))))))
