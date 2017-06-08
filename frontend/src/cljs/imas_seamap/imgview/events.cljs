(ns imas-seamap.imgview.events
  (:require [ajax.core :as ajax]
            [debux.cs.core :refer [dbg]]))

(defn load-survey [_ [_ survey-id]]
  {:http-xhrio {:method :get
                ;; Hackish; we could get the root endpoint and find
                ;; the URL for the id, but this will do for now:
                ;; :uri (str "http://rls.tpac.org.au/pq/" survey-id)
                :uri (str "/pq/" survey-id) ; proxy, until CORS is enabled
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [:imgview/on-load-survey]
                :on-failure [:ajax/default-err-handler]}})

(defn- find-by-kv
  "The structure of the response is a bit skewed; we can't access
  entries directly by key, we have to iterate over a list looking for
  a particular key-value combination"
  [k v lst]
  (some #(and (= (k %) v) %) lst))

(defn find-url
  "The other thing we need to do is even more general; find an entry
  that even has a particular key.  Could generalise to find any key,
  but we'll stick with the one known case here."
  [lst]
  (some :url lst))

(defn- format-img-response [{:keys [name width height urls scaled]}]
  (let [scale       800
        ratio       (/ width height)
        display-img (->> scaled
                         (find-by-kv :scale 800)
                         :urls
                         find-url)]
    {:imgview/full-size        (find-url urls)
     :imgview/name             name
     :imgview/display          display-img
     :imgview/thumbnail        display-img ; Identical, for now
     ;; Hack this for now; in the response (= width height) all the
     ;; time, and furthermore the scaled orientation is always rotated
     ;; to landscape even if the original image is portrait:
     :imgview/thumbnail-width  800
     :imgview/thumbnail-height 533}))

(defn on-load-survey [db [_ response]]
  ;; Results in pagination format, but pagination not implemented yet
  (assoc-in db [:survey :rls]
            (->> response
                 :results
                 (mapv format-img-response))))
