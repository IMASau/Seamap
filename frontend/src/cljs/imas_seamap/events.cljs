(ns imas-seamap.events
    (:require [re-frame.core :as re-frame]
              [imas-seamap.db :as db]))

;;; TODO: maybe pull in extra config inject as page config, but that
;;; may not be feasible with a wordpress host
(defn -initialise-db [_ _] db/default-db)
