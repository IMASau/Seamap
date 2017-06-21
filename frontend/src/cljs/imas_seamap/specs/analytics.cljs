(ns imas-seamap.specs.analytics
  (:require [cljs.spec.alpha :as s]
            [imas-seamap.analytics :as analytics]))


(s/def ::ga-event
  (s/and (s/map-of keyword? string?)
         (s/keys :req-un [:ga/hitType :ga/eventCategory :ga/eventAction]
                 :opt-un [:ga/eventLabel :ga/eventValue])))

(s/fdef analytics/track-event
  :args (s/cat :event ::ga-event))
