(ns imas-seamap.imgview.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b]
            [debux.cs.core :refer [dbg]]))

(def gallery (reagent/adapt-react-class js/Gallery))

(defn viewer-app []
  (let [images @(re-frame/subscribe [:imgview/images :rls])
        images (map (fn [{:keys [:imgview/display
                                 :imgview/name
                                 :imgview/thumbnail
                                 :imgview/thumbnail-width
                                 :imgview/thumbnail-height]}]
                      {:src              display
                       :caption          name
                       :thumbnail        thumbnail
                       :thumbnail-width  thumbnail-width
                       :thumbnail-height thumbnail-height})
                    images)]
    (if (zero? (count images))
      [:div.loading {:style {:width  "100%"
                             :height "100vh"}}
       [b/non-ideal-state {:visual (reagent/as-element
                                    [b/spinner {:intent "success"}])
                           :title  "Retrieving images..."}]]
      [gallery {:images                   images
                :is-open                  true
                :enable-image-selection   false
                :show-lightbox-thumbnails true}])))

