(ns imas-seamap.events
    (:require [re-frame.core :as re-frame]
              [imas-seamap.db :as db]))

(defn -initialise-db [_ _] db/default-db)

(re-frame/reg-event-db :initialise-db -initialise-db)
