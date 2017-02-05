(ns imas-seamap.subs
    (:require-macros [reagent.ratom :refer [reaction]])
    (:require [re-frame.core :as re-frame]))

(defn map-props [db] (:map db))
