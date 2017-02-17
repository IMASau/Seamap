(ns imas-seamap.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.map.views :refer [map-component]]
            [goog.object :as gobj]))

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
             :class-name "pt-fill draw-transect"
             :on-click #(re-frame/dispatch [dispatch-key])
             :text label}]))

(defn layer-card [idx]
  [:div.layer-wrapper
   [:div.pt-card.pt-elevation-1
    "Roar" idx]])

(defn layer-group [{:keys [title expanded] :or {expanded false}} layers]
  (let [expanded-state (reagent/atom expanded)]
    (fn [props layers]
      [:div.layer-group
       [:h1 {:class (if @expanded-state "pt-icon-chevron-down" "pt-icon-chevron-right")
             :on-click #(swap! expanded-state not)}
        (str title " (" (count layers) ")")]
       [Collapse {:is-open @expanded-state}
        (for [layer layers]
          ^{:key (:layer_name layer)}
          [layer-card layer])]])))

(defn app-controls []
  (let [{:keys [habitat bathymetry imagery third-party] :as groups} @(re-frame/subscribe [:map/layers])]
    [:div#sidebar
     [transect-toggle]
     [layer-group {:title "Habitat"    :expanded true } habitat]
     [layer-group {:title "Bathymetry" :expanded true } bathymetry]
     [layer-group {:title "Imagery"    :expanded false} imagery]
     [layer-group {:title "Other"      :expanded false} third-party]]))

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
        habitat (for [i (take (- (count d) 1) (range))]
                  (list (nth d i) (nth d (+ 1 i)) (str "zone" (+ 1 (rand-int 6)))))]
    habitat))


(def zone-color-mapping
  {:zone1 (randColour)
   :zone2 (randColour)
   :zone3 (randColour)
   :zone4 (randColour)
   :zone5 (randColour)
   :zone6 (randColour)})


(defn min-depth [bathymetry]
  (apply min (map #(nth % 1) bathymetry)))


(defn max-depth [bathymetry]
  (apply max (map #(nth % 1) bathymetry)))


(defn depth-to-y-pos [{:keys [depth graph-range offset min-depth spread buffer] :as props}]
  (+ (* (* graph-range (- 1 offset)) (/ (- depth min-depth) spread)) (buffer 1)))


(defn percentage-to-x-pos [{:keys [percentage origin graph-domain]}]
  (+ (origin 0) (* (/ percentage 100) graph-domain)))


(defn formatted-graph-line [{:keys [bathymetry origin buffer graph-domain graph-range offset min-depth max-depth spread] :as props}]
  (let [[first-depth & remaining] bathymetry
        start-string (str "M " (percentage-to-x-pos (merge props {:percentage (nth first-depth 0)})) " " (depth-to-y-pos (merge props {:depth (nth first-depth 1)})) " ")
        middle-string (clojure.string/join (for [depth remaining]
                                             (str "L " (percentage-to-x-pos (merge props {:percentage (nth depth 0)})) " " (depth-to-y-pos (merge props {:depth (nth depth 1)})) " ")))]
    (str start-string middle-string)))


(defn draw-axes [{:keys [width height origin buffer]}]
  [:g {:fill         "none"
       :stroke       "black"
       :stroke-width "3"}
   [:polyline {:points (str (origin 0) "," (buffer 1) " " (origin 0) "," (- height (origin 1)) " " (- width (buffer 0)) "," (- height (origin 1)))}]])


(defn label-axes [{:keys [x-steps y-steps x-axis-offset y-axis-offset line-height offset
                          max-depth min-depth spread graph-domain graph-range origin buffer]}]
  (let [origin-depth (+ min-depth (/ spread (- 1 offset)))
        delta (- origin-depth min-depth)]
    [:g {:style {:line-height line-height}}

     ;x-axis
     [:g {:style {:text-anchor "middle"}}
      (for [i (take (+ 1 x-steps) (range))]
        [:text {:key (hash (str "percentageLabel" i))
                :x   (+ (* (/ i x-steps) graph-domain) (origin 0))
                :y   (+ (buffer 1) (+ (+ line-height x-axis-offset) graph-range))}
         (str (int (* (/ i x-steps) 100)))])
      [:text {:x (+ (origin 0) (/ graph-domain 2))
              :y (+ (+ line-height x-axis-offset) (+ (+ (buffer 1) graph-range) (* 2 line-height)))}
       "Percentage Along Transect (%)"]]

     ;y-axis
     [:g {:style {:text-anchor "end"}}
      (for [i (take (+ 1 y-steps) (range))]
        [:text {:key (hash (str "depthLabel" i))
                :x   (- (origin 0) y-axis-offset)
                :y   (+ (buffer 1) (+ (/ line-height 2) (* (/ i y-steps) graph-range)))}
         (str (int (+ min-depth (* (/ i y-steps) delta))))])
      [:text {:x         (- (origin 0) (+ 30 y-axis-offset))
              :y         (+ (buffer 1) (/ graph-range 2))
              :transform (str "rotate(-90, " (- (origin 0) (+ 30 y-axis-offset)) ", " (+ (buffer 1) (/ graph-range 2)) ")")
              :style     {:text-anchor "middle"}}
       "Depth (m)"]]]))


(defn mouse-pos-to-percentage [{:keys [pagex origin graph-domain]}]
  (let [dist-from-y-axis (- pagex (nth origin 0))]
    (/ dist-from-y-axis graph-domain)))


(defn mouse-move-graph [{:keys [bathymetry event mouse-loc tool-tip origin graph-domain] :as props}]
  (let [pagex (gobj/get event "pageX")
        pagey (gobj/get event "pageY")
        percentage (* 100 (mouse-pos-to-percentage (merge props {:pagex pagex})))
        previous (peek (filterv #(<= (nth % 0) percentage) bathymetry))
        next ((filterv #(>= (nth % 0) percentage) bathymetry) 0)
        next-is-closest (< (- (nth next 0) percentage) (- percentage (nth previous 0)))
        closest (if next-is-closest next previous)
        pointx (percentage-to-x-pos (merge props {:percentage (nth closest 0)}))
        pointy (depth-to-y-pos (merge props {:depth (nth closest 1)}))
        tt-offset-x 10
        tt-offset-y 10]
    (swap! mouse-loc merge {:cx pointx
                            :cy    pointy
                            :style {:visibility "visible"}})
    (swap! tool-tip merge {:x (+ pointx tt-offset-x)
                           :style {:visibility "visible"
                                   :opacity 0.7}})))


(defn mouse-leave-graph [{:keys [mouse-loc tool-tip] :as props}]
  (swap! mouse-loc merge {:style {:visibility "hidden"}})
  (swap! tool-tip merge {:style {:visibility "hidden"}}))


(defn draw-graph [{:keys [bathymetry habitat width height zone-color-mapping origin buffer offset]
                   :or {width 500}
                   :as props}]
  (let [graph-range (- height (+ (buffer 1) (origin 1)))
        graph-domain (- width (+ (buffer 0) (origin 0)))
        max-depth (max-depth bathymetry)
        min-depth (min-depth bathymetry)
        spread (- max-depth min-depth)
        graph-line-string (formatted-graph-line (merge props {:graph-domain graph-domain
                                                              :graph-range  graph-range
                                                              :min-depth    min-depth
                                                              :max-depth    max-depth
                                                              :spread       spread}))
        clip-path-string (str graph-line-string "L " (- width (buffer 0)) " " (+ graph-range (buffer 1)) "L " (origin 0) " " (+ (buffer 1) graph-range) " Z")
        mouse-loc (reagent/atom {:cx 0 :cy 0 :style {:visibility "hidden"}})
        tool-tip (reagent/atom {})]
    (fn []
      [:div#transect-plot
       [:svg {:width  width
              :height height}

        [:defs
         [:clipPath {:id "clipPath"}
          [:path {:d clip-path-string}]]]

        [:rect#graph-area {:x      0
                           :y      0
                           :width  width
                           :height height
                           :style  {:opacity 0.1}}]

        ;draw habitat zones
        [:g
         (for [zone habitat]
           [:rect {:key    zone
                   :x      (+ (origin 0) (* (/ (nth zone 0) 100) graph-domain))
                   :y      (buffer 1)
                   :width  (* (/ (- (nth zone 1) (nth zone 0)) 100) graph-domain)
                   :height graph-range
                   :style  {:fill      ((keyword (nth zone 2)) zone-color-mapping)
                            :clip-path "url(#clipPath)"
                            }}
            [:title (nth zone 2)]])]

        ;draw bathymetry line
        [:path {:d            graph-line-string
                :fill         "none"
                :stroke       "black"
                :stroke-width 3}]

        ;draw axes
        [draw-axes props]

        ;label axes
        [label-axes (merge props {:line-height   10
                                  :x-axis-offset 10
                                  :y-axis-offset 10
                                  :x-steps       10
                                  :y-steps       6
                                  :max-depth     max-depth
                                  :min-depth     min-depth
                                  :graph-domain  graph-domain
                                  :graph-range   graph-range
                                  :spread        spread})]

        [:circle#data-point (merge {:cx    0
                                    :cy    0
                                    :r     5
                                    :fill  "red"
                                    :style {:visibility "hidden"}}
                                   @mouse-loc)]

        [:rect#tool-tip (merge {:x      0
                                :y      (+ (nth buffer 1) (/ graph-range 2))
                                :width  100
                                :height 50
                                :fill   "white"
                                :style  {:visibility "hidden"}}
                               @tool-tip)]

        [:rect {:x              (origin 0)
                :y              (buffer 1)
                :width          graph-domain
                :height         graph-range
                :style          {:opacity 0}
                :on-mouse-move  #(mouse-move-graph (merge props {:mouse-loc    mouse-loc
                                                                 :tool-tip     tool-tip
                                                                 :event        %
                                                                 :max-depth    max-depth
                                                                 :min-depth    min-depth
                                                                 :graph-domain graph-domain
                                                                 :graph-range  graph-range
                                                                 :spread       spread}))
                :on-mouse-leave #(mouse-leave-graph {:mouse-loc mouse-loc
                                                     :tool-tip tool-tip})}]
        ]]))
  )

(defn transect-plot []
  [:div
   [draw-graph {:bathymetry         (generate-bathymetry)
                :habitat            (generate-habitat)
                :width              900
                :height             300
                :zone-color-mapping zone-color-mapping
                :origin             [100 100]
                :buffer             [20 20]
                :offset             0.3}]])

