(ns imas-seamap.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.views.map :refer [map-component]]))

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(defn app-controls []
  [:div#sidebar])

(defn plot-component []
  (let [show-plot (reagent/atom true)]
    (fn []
      [:footer {:on-click #(swap! show-plot not)}
       [css-transition-group {:transition-name "plot-height"
                              :transition-enter-timeout 300
                              :transition-leave-timeout 300}
        (if @show-plot
          [:div.plot-container])]])))

(defn layout-app []
  [:div#main-wrapper
   [app-controls]
   [:div#content-wrapper
    [map-component]
    [plot-component]]])

(defn randColour []
  (str "rgb(" (rand-int 255) "," (rand-int 255) "," (rand-int 255) ")"))

(defn generate-bathymetry []
      (let [a (iterate #(+ 1 %) 0)
            b (repeatedly 101 #(rand 100))
            c (map list a b)
            bathymetry (into [] c)]
           bathymetry))

(defn generate-habitat []
      (let [habitat [(list 0 15 "zone1")
                     (list 15 20 "zone2")
                     (list 20 31 "zone3")
                     (list 31 47 "zone4")
                     (list 47 60 "zone5")
                     (list 60 100 "zone6")]]
           habitat))

(defn get-min-depth [bathymetry]
      0)

(defn get-max-depth [bathymetry]
      100)

(defn get-formatted-graph-line [{:keys [bathymetry origin domain range] :as props}]
      (let [minDepth (get-min-depth bathymetry)
            maxDepth (get-max-depth bathymetry)
            spread (- maxDepth minDepth)
            offsetPercentage 0.3
            [firstDepth & remaining] bathymetry
            startString (str "M " (+ (origin 0) (* (/ (nth firstDepth 0) 100) domain)) " " (/ (nth firstDepth 1) spread) " ")
            middleString (clojure.string/join (for [depth remaining]
                                                   (str "L " (+ (origin 0) (* (/ (nth depth 0) 100) domain)) " " (- range (nth depth 1)) " ")))
           ]
           (str startString middleString)
           ))

(def zone-color-mapping
  {:zone1 (randColour)
   :zone2 (randColour)
   :zone3 (randColour)
   :zone4 (randColour)
   :zone5 (randColour)
   :zone6 (randColour)
   })

(defn draw-graph [{:keys [bathymetry habitat width height] :as props}]
      (let [origin [100 100]
            range (- height (origin 1))
            domain (- width (origin 0))
            line (get-formatted-graph-line {:bathymetry bathymetry
                                               :origin     origin
                                               :domain     domain
                                               :range      range})
            endString (str "L " width " " height "L 0 " height " Z")
            clipPathString (str line endString)
            ]
           [:div#transect-plot
            [:svg {:width  width
                   :height height}
             [:clipPath {:id "clipPath"}
              [:path {:d clipPathString}]]
             (for [zone habitat]
                  [:rect {:x      (+ (origin 0) (* (/ (nth zone 0) 100) (- width (origin 0))))
                          :y      0
                          :width  (* (/ (- (nth zone 1) (nth zone 0)) 100) (- width (origin 0)))
                          :height (- height (origin 1))
                          :style  {:fill      (zone-color-mapping (keyword (nth zone 2)))
                                   :clip-path "url(#clipPath)"}}])
             [:path {:d line
                     :fill "none"
                     :stroke "black"
                     :stroke-width 3}]

             ;draw axes
             [:path {:d            (str "M " (origin 0) " " (- height (origin 1)) " L " width " " (- height (origin 1)))
                     :fill         "none"
                     :stroke       "black"
                     :stroke-width 5}]
             [:path {:d (str "M " (origin 0) " " (- height (origin 1)) " L " (origin 0) " " 0)
                     :fill "none"
                     :stroke "black"
                     :stroke-width 5}]
             ]])
      )

(defn transect-plot []
      (draw-graph {:bathymetry (generate-bathymetry)
                   :habitat    (generate-habitat)
                   :width      900
                   :height     300}))

