;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.tmag-kiosk.media-cache
  "Large media (the attract video) held by the app, so it's downloaded once
  rather than whenever it's shown: a video's src is otherwise streamed again
  each time its element is made, and the browser's HTTP cache can't be relied
  on (the kiosk runs InPrivate, and the attract video is sent without caching
  headers).

  `keep!` fetches each URL (with CORS) into a Blob, and `src` gives a blob: URL
  for it once it's in, or the URL itself until then (or if it can't be
  fetched: a server without CORS headers), so nothing waits on it. Each is
  also put in Cache Storage, so a reload of the page doesn't download it
  again; InPrivate keeps that in memory only, until the browser closes, with
  a smaller quota, so a failure to store is ignored. A stored copy is checked
  against the server's Last-Modified and Content-Length before it's used,
  and fetched again if they've changed; URLs no longer kept are dropped."
  (:require [reagent.core :as r]))

(def ^:private cache-name "kiosk-media")

;; URL -> blob: URL
(defonce ^:private object-urls (r/atom {}))
;; URLs fetched or being fetched
(defonce ^:private requested (atom #{}))

(defn src
  "What a media element should load for `url`: its Blob, once kept (see
  `keep!`), or else the URL."
  [url]
  (get @object-urls url url))

(defn- same-file?
  "Whether a stored response is still the server's file: the same
  Last-Modified and Content-Length (both readable cross-origin), or true if
  the server can't be asked."
  [url ^js stored]
  (-> (js/fetch url #js {:method "HEAD" :mode "cors" :credentials "omit" :cache "no-store"})
      (.then (fn [^js response]
               (or (not (.-ok response))
                   (every? #(= (.. stored -headers (get %)) (.. response -headers (get %)))
                           ["last-modified" "content-length"]))))
      (.catch (constantly true))))

(defn- fetched [^js cache url]
  (-> (js/fetch url #js {:mode "cors" :credentials "omit"})
      (.then (fn [^js response]
               (when-not (.-ok response)
                 (throw (js/Error. (str "HTTP " (.-status response)))))
               (when cache
                 (-> (.put cache url (.clone response))
                     (.catch #(js/console.warn "Not stored for reloads:" url (.-message %)))))
               (.blob response)))))

(defn- keep-one! [^js cache url]
  (swap! requested conj url)
  (-> (if cache (.match cache url) (js/Promise.resolve nil))
      (.then (fn [^js stored]
               (if stored
                 (.then (same-file? url stored)
                        #(if % (.blob stored) (fetched cache url)))
                 (fetched cache url))))
      (.then #(swap! object-urls assoc url (js/URL.createObjectURL %)))
      (.catch (fn [e]
                ;; Played from the URL instead; tried again on a reload.
                (swap! requested disj url)
                (js/console.warn "Media not cached, streaming instead:" url (.-message e))))))

(defn keep!
  "Fetches the media at `urls` and keeps it (see the namespace doc), dropping
  any stored before that isn't among them."
  [urls]
  (let [urls (set urls)]
    (-> (if (exists? js/caches)
          (.catch (.open js/caches cache-name) (constantly nil))
          (js/Promise.resolve nil))
        (.then (fn [^js cache]
                 (doseq [url urls :when (not (@requested url))]
                   (keep-one! cache url))
                 (when cache
                   (.then (.keys cache)
                          (fn [requests]
                            (doseq [^js request requests :when (not (urls (.-url request)))]
                              (.delete cache request))))))))))
