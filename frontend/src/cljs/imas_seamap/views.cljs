(ns imas-seamap.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.map.views :refer [map-component]]))

(def css-transition-group
  (reagent/adapt-react-class js/React.addons.CSSTransitionGroup))

(def Button (reagent/adapt-react-class js/Blueprint.Button))
(def Collapse (reagent/adapt-react-class js/Blueprint.Collapse))

(defn transect-toggle []
  (let [{:keys [drawing?]} @(re-frame/subscribe [:transect/info])
        [dispatch-key label] (if drawing?
                               [:transect.draw/disable "Cancel Transect"]
                               [:transect.draw/enable  "Draw Transect"])]
    [Button {:icon-name "edit"
             :class-name "pt-fill"
             :on-click #(re-frame/dispatch [dispatch-key])
             :text label}]))

(defn layer-card [idx]
  [:div.layer-wrapper
   [:div.pt-card.pt-elevation-1
    "Roar" idx]])

(defn layer-group [title & children]
  (let [expanded (reagent/atom false)]
    (fn [title & children]
      [:div.layer-group
       [:span {:class (if @expanded "pt-icon-chevron-down" "pt-icon-chevron-right")
               :on-click #(swap! expanded not)}
        title]
       [Collapse {:is-open @expanded}
        (map-indexed #(with-meta %2 {:key %1}) children)]])))

(defn app-controls []
  [:div#sidebar
   [transect-toggle]
   [layer-group "Habitat"
    [layer-card "one"]
    [layer-card "four"]]])

(defn plot-component-animatable [{:keys [on-add on-remove]
                                  :or   {on-add identity on-remove identity}
                                  :as   props}]
  (reagent/create-class
   {:display-name           "plot-component-animatable"
    :component-will-unmount on-remove
    :component-did-mount    on-add
    :reagent-render         (fn [props] [:div.plot-container])}))

(defn plot-component []
  (let [show-plot (reagent/atom true)
        force-resize #(js/window.dispatchEvent (js/Event. "resize"))]
    (fn []
      [:footer {:on-click #(swap! show-plot not)}
       [css-transition-group {:transition-name "plot-height"
                              :transition-enter-timeout 300
                              :transition-leave-timeout 300}
        (if @show-plot
          [plot-component-animatable {:on-add force-resize :on-remove force-resize}])]])))

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
            b (repeatedly 101 #(+ (rand 50) (rand 50)))
            c (map list a b)
            bathymetry (into [] c)]
           bathymetry))

(defn generate-habitat []
      (let [num-zones (+ 3 (rand-int 7))
            a (repeatedly num-zones #(+ 10 (rand-int 90)))
            b (list* 0 100 a)
            c (sort b)
            d (distinct c)
            habitat (for [i (take (+ 1 num-zones) (clojure.core/range))]
                         (list (nth d i) (nth d (+ 1 i)) (str "zone" (rand-int 7))))
            ]
           (js/console.log "habitat: " habitat)
           habitat))

(defn get-min-depth [bathymetry]
      (apply min (map #(nth % 1) bathymetry)))

(defn get-max-depth [bathymetry]
      (apply max (map #(nth % 1) bathymetry)))

(defn get-formatted-graph-line [{:keys [bathymetry origin buffer domain range offset minDepth maxDepth] :as props}]
      (let [spread (- maxDepth minDepth)
            [firstDepth & remaining] bathymetry
            startString (str "M " (+ (origin 0) (* (/ (nth firstDepth 0) 100) domain)) " " (+ (* (* range (- 1 offset)) (/ (- (nth firstDepth 1) minDepth) spread)) (buffer 1)) " ")
            middleString (clojure.string/join (for [depth remaining]
                                                   (str "L " (+ (origin 0) (* (/ (nth depth 0) 100) domain)) " " (+ (* (* range (- 1 offset)) (/ (- (nth depth 1) minDepth) spread)) (buffer 1)) " ")))
           ]
           (str startString middleString)
           ))

(def zone-color-mapping
  {:zone1 (randColour)
   :zone2 (randColour)
   :zone3 (randColour)
   :zone4 (randColour)
   :zone5 (randColour)
   :zone6 (randColour)})

(defn draw-graph [{:keys [bathymetry habitat width height] :or {width 500} :as props}]
      (let [origin [100 100]
            buffer [20 20]
            range (- height (+ (buffer 1) (origin 1)))
            domain (- width (+ (buffer 0) (origin 0)))
            max-depth (get-max-depth bathymetry)
            min-depth (get-min-depth bathymetry)
            offset 0.3
            line (get-formatted-graph-line {:bathymetry bathymetry
                                            :origin     origin
                                            :domain     domain
                                            :buffer     buffer
                                            :range      range
                                            :offset     offset
                                            :minDepth   min-depth
                                            :maxDepth   max-depth})
            clipPathString (str line "L " (- width (buffer 0)) " " (+ range (buffer 1)) "L " (origin 0) " " (+ (buffer 1) range) " Z")
            ]
           [:div#transect-plot
            [:svg {:width  width
                   :height height}

             [:defs
              [:clipPath {:id "clipPath"}
               [:path {:d clipPathString}]]]

             ;draw habitat zones
             [:g
              (for [zone habitat]
                   [:rect {:x          (+ (origin 0) (* (/ (nth zone 0) 100) domain))
                           :y          (buffer 1)
                           :width      (* (/ (- (nth zone 1) (nth zone 0)) 100) domain)
                           :height     range
                           :title (nth zone 2)
                           :style      {:fill      (zone-color-mapping (keyword (nth zone 2)))
                                        :clip-path "url(#clipPath)"
                                        }}])]

             ;draw bathymetry line
             [:path {:d line
                     :fill "none"
                     :stroke "black"
                     :stroke-width 3}]

             ;draw axes
             [:g {:fill         "none"
                  :stroke       "black"
                  :stroke-width "3"}
              [:line {:x1 (origin 0)
                      :y1 (- height (origin 1))
                      :x2 width
                      :y2 (- height (origin 1))}]
              [:line {:x1 (origin 0)
                      :y1 (- height (origin 1))
                      :x2 (origin 0)
                      :y2 "0"}]]


             ;label axes
             (let [line-height 10
                   offset-x-axis (+ 10 line-height)
                   offset-y-axis 10
                   spread (- max-depth min-depth)
                   origin-depth (+ min-depth (/ spread (- 1 offset)))
                   delta (- origin-depth min-depth)
                   x-steps 10
                   y-steps 6]
                  [:g {:style {:line-height line-height}}

                   ;x-axis
                   [:g {:style {:text-anchor "middle"}}
                    (for [i (take (+ 1 x-steps) (clojure.core/range))]
                         [:text {:x (+ (* (/ i x-steps) domain) (origin 0))
                                 :y (+ (buffer 1) (+ offset-x-axis range))}
                          (str (int (* (/ i x-steps) 100)))])
                    [:text {:x (+ (origin 0) (/ domain 2))
                            :y (+ offset-x-axis (+ (+ (buffer 1) range) (* 2 line-height)))}
                     "Percentage (%)"]]

                   ;y-axis
                   [:g {:style {:text-anchor "end"}}
                    (for [i (take (+ 1 y-steps) (clojure.core/range))]
                         [:text {:x (- (origin 0) offset-y-axis)
                                 :y (+ (buffer 1) (+ (/ line-height 2) (* (/ i y-steps) range)))}
                          (str (int (+ min-depth (* (/ i y-steps) delta))))])
                    [:text {:x         (- (origin 0) (+ 30 offset-y-axis))
                            :y         (+ (buffer 1) (/ range 2))
                            :transform (str "rotate(-90, " (- (origin 0) (+ 30 offset-y-axis)) ", " (+ (buffer 1) (/ range 2)) ")")
                            :style     {:text-anchor "middle"}}
                     "Depth (m)"]]

                   ])
             ]])
      )

(defn transect-plot []
      [:div {:style {:position "absolute" :top "50px"}}
       (draw-graph {:bathymetry (generate-bathymetry)
                    :habitat    (generate-habitat)
                    :width      900
                    :height     300})])

