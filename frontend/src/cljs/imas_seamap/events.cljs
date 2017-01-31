(ns imas-seamap.events
    (:require [re-frame.core :as re-frame]
              [imas-seamap.db :as db]))

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))
