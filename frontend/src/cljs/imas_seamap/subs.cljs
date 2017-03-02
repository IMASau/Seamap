(ns imas-seamap.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(defn transect-info [{:keys [map transect] :as db} _]
  {:drawing? (boolean (get-in map [:controls :transect]))
   :query transect})

(defn help-layer-open? [db _]
  (get-in db [:display :help-overlay]))
