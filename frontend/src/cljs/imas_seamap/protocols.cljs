(ns imas-seamap.protocols
  (:require [goog.object :as gobj]))

;;; https://gist.github.com/dwest/8f3365e3117bb12e0ddd
(defn- jobj->dict [o options]
  (let [result (atom {})]
    (gobj/forEach o (fn [val key obj]
                      (when-not (re-matches #"^cljs.*" key)
                        (swap! result assoc (keyword key) val))))
    (deref result)))

;;; DOMStringMap is returned by the HTMLDivElement.dataset property
(extend-type js/DOMStringMap
  IEncodeClojure
  (-js->clj [o options] (jobj->dict o options)))

;;; Types returned by the HTMLDivElement.getBoundingClientRect() method;
;;; Chrome, Edge:
(when (exists? js/ClientRect)
  (extend-type js/ClientRect
    IEncodeClojure
    (-js->clj [o options] (jobj->dict o options))))
;;; Firefox:
(when (exists? js/DOMRect)
  (extend-type js/DOMRect
    IEncodeClojure
    (-js->clj [o options] (jobj->dict o options))))
