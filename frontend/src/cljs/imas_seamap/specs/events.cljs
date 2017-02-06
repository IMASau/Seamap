(ns imas-seamap.specs.events
  (:require [cljs.spec :as s]
            [imas-seamap.specs.app-state]
            [imas-seamap.events :as events]))


(s/fdef events/initialise-db
  :args (s/cat :db map?       ; Presumably uninitialised at this point
               :event-v (s/coll-of keyword? :count 1))
  :ret (constantly false))
