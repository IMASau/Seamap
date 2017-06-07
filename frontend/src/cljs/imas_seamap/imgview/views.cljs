(ns imas-seamap.imgview.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [debux.cs.core :refer [dbg]]))

(def gallery (reagent/adapt-react-class js/Gallery))

;;; Main flow:
;;; * we get the survey id as a prop
;;; * 

(defn viewer-app [survey-id]
  (js/console.warn "survey-id:" survey-id)
  [:div
   [:div "Hello world"]
   [gallery {:images [{:src "http://rls.tpac.org.au/pq/912343963/scale/800/LHI24_6m210214PhillipN_RSS (8).JPG/"
                       :thumbnail "http://rls.tpac.org.au/pq/912343963/scale/800/LHI24_6m210214PhillipN_RSS (8).JPG/"}
                      {:src "http://rls.tpac.org.au/pq/912343963/scale/800/LHI24_6m210214PhillipN_RSS (15).JPG/"
                       :thumbnail "http://rls.tpac.org.au/pq/912343963/scale/800/LHI24_6m210214PhillipN_RSS (15).JPG/"}]
             :is-open true
             :enable-image-selection false
             :showThumbnails true}]])

