(ns imas-seamap.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.map.views :refer [map-component]]
            [goog.object :as gobj]
            [goog.dom :as dom]))

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

(defn layer-card [{:keys [name] :as layer-spec}]
  [:div.layer-wrapper
   [:div.pt-card.pt-elevation-1
    {:on-click #(re-frame/dispatch [:map/toggle-layer layer-spec])}
    [:span name]]])

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


(def random-zone-colours
  {:zone1 (randColour)
   :zone2 (randColour)
   :zone3 (randColour)
   :zone4 (randColour)
   :zone5 (randColour)
   :zone6 (randColour)})


(def habitat-zone-colours
  {:reef        "pink"
   :vegetated   "darkgreen"
   :unvegetated "yellow"
   :seagrass    "lightgreen"
   :unknown     "grey"})


(defn generate-habitat [zone-color-mapping]
  (let [num-zones (+ 3 (rand-int 7))
        a (repeatedly num-zones #(+ 10 (rand-int 90)))
        b (list* 0 100 a)
        c (sort b)
        d (distinct c)
        habitat (for [i (take (- (count d) 1) (range))]
                  (list (nth d i) (nth d (+ 1 i)) (name (nth (keys zone-color-mapping) (rand-int (count zone-color-mapping))))))]
    habitat))


(defn min-depth [bathymetry]
  (apply min (map (fn [[_ depth]] depth) bathymetry)))


(defn max-depth [bathymetry]
  (apply max (map (fn [[_ depth]] depth) bathymetry)))


(defn depth-to-y-pos [{:keys [depth graph-range offset min-depth spread margin] :as props}]
  (+ (* graph-range (- 1 offset) (/ (- depth min-depth) spread)) (margin 1)))


(defn percentage-to-x-pos [{:keys [percentage origin graph-domain margin]}]
  (let [[mx my] margin
        [ox oy] origin]
    (+ mx ox (* (/ percentage 100) graph-domain))))


(defn habitat-at-percentage [{:keys [habitat percentage]}]
  (peek (filterv (fn [[p]] (<= p percentage)) habitat)))

(defn formatted-graph-line [{:keys [bathymetry origin margin graph-domain graph-range offset min-depth max-depth spread] :as props}]
  (let [[[first-percentage first-depth] & remaining] bathymetry
        start-string (str "M " (percentage-to-x-pos (merge props {:percentage first-percentage})) " " (depth-to-y-pos (merge props {:depth first-depth})) " ")
        middle-string (clojure.string/join (for [[percentage depth] remaining]
                                             (str "L " (percentage-to-x-pos (merge props {:percentage percentage})) " " (depth-to-y-pos (merge props {:depth depth})) " ")))]
    (str start-string middle-string)))


(defn axes [{:keys [width height origin margin]}]
  (let [[mx my] margin
        [ox oy] origin]
    [:g {:fill         "none"
         :stroke       "black"
         :stroke-width "3"}
     [:polyline {:points (str (+ mx ox) "," my " "
                              (+ mx ox) "," (- height (+ my oy)) " "
                              (- width mx) "," (- height (+ my oy)))}]]))


(defn axis-labels [{:keys [x-axis-offset y-axis-offset line-height font-size offset
                           min-depth spread graph-domain graph-range origin margin]}]
  (let [origin-depth (+ min-depth (/ spread (- 1 offset)))
        delta (- origin-depth min-depth)
        x-steps (int (/ graph-domain 50))
        y-steps (int (/ graph-range 30))
        [mx my] margin
        [ox oy] origin]
    [:g {:style {:line-height line-height
                 :font-size   font-size}}
     ;x-axis labels
     [:g {:style {:text-anchor "middle"}}
      (for [i (take (+ 1 x-steps) (range))]
        [:text {:key (hash (str "percentageLabel" i))
                :x   (+ (* (/ i x-steps) graph-domain) ox mx)
                :y   (+ my font-size x-axis-offset graph-range)}
         (str (int (* (/ i x-steps) 100)))])
      [:text {:x     (+ mx ox (/ graph-domain 2))
              :y     (+ line-height font-size x-axis-offset my graph-range)
              :style {:font-weight "bold"}}
       "Percentage Along Transect (%)"]]
     ;y-axis labels
     [:g {:style {:text-anchor "end"}}
      (for [i (take (+ 1 y-steps) (range))]
        [:text {:key (hash (str "depthLabel" i))
                :x   (- (+ mx ox) y-axis-offset)
                :y   (+ my (/ font-size 2) (* (/ i y-steps) graph-range))}
         (str (int (+ min-depth (* (/ i y-steps) delta))))])
      [:text {:x         (- (+ mx ox) (+ line-height font-size y-axis-offset))
              :y         (+ my (/ graph-range 2))
              :transform (str "rotate(-90, " (- (+ my ox) (+ line-height font-size y-axis-offset)) ", " (+ my (/ graph-range 2)) ")")
              :style     {:text-anchor "middle"
                          :font-weight "bold"}}
       "Depth (m)"]]]))


(defn tooltip [{:keys [tooltip-content tooltip-width line-height font-size]}]
  [:g#tooltip (merge {:style {:visibility "hidden"}} (:tooltip @tooltip-content))
   [:line (merge {:x1    0
                  :y1    0
                  :x2    0
                  :y2    0
                  :style {:stroke           "gray"
                          :stroke-width     2
                          :stroke-dasharray "5,5"}}
                 (:line @tooltip-content))]
   [:circle (merge {:cx    0
                    :cy    0
                    :r     5
                    :style {:fill         "white"
                            :stroke       "black"
                            :stroke-width 3}}
                   (:datapoint @tooltip-content))]
   [:g#textbox (merge {:transform "translate(0, 0)"} (:textbox @tooltip-content))
    [:rect {:x      0
            :y      0
            :rx     5
            :ry     5
            :width  tooltip-width
            :height (* 2.5 line-height)
            :style  {:opacity      0.9
                     :fill         "white"
                     :stroke       "black"
                     :stroke-width 3}}]
    [:text {:style {:font-size font-size}}
     (doall (for [s (:text @tooltip-content)]
              [:tspan {:key s
                       :x   10
                       :y   (* line-height (+ 1 (.indexOf (:text @tooltip-content) s)))}
               s]))]]])


(defn mouse-pos-to-percentage [{:keys [pagex origin graph-domain margin]}]
  (let [indent-from-client (gobj/get (.getBoundingClientRect (dom/getRequiredElement "transect-plot")) "left")
        horizontal-scroll (gobj/get (dom/getDocumentScroll) "x")
        indent-from-page (+ indent-from-client horizontal-scroll)
        [mx my] margin
        [ox oy] origin
        dist-from-y-axis (- pagex (+ indent-from-page ox mx))]
    (/ dist-from-y-axis graph-domain)))


(defn mouse-move-graph [{:keys [bathymetry event tooltip-content tooltip-width origin graph-domain graph-range margin offset on-mousemove] :as props}]
  (let [pagex (gobj/get event "pageX")
        percentage (min (max (* 100 (mouse-pos-to-percentage (merge props {:pagex pagex}))) 0) 100)
        [before after] (split-with #(< (nth % 0) percentage) bathymetry)
        previous (if (seq before) (last before) (nth after 0))
        next (if (seq before) (nth after 0) (nth after 1))
        next-is-closest (< (- (nth next 0) percentage) (- percentage (nth previous 0)))
        [closest-percentage closest-depth] (if next-is-closest next previous)
        pointx (percentage-to-x-pos (merge props {:percentage closest-percentage}))
        pointy (depth-to-y-pos (merge props {:depth closest-depth}))
        zone (nth (habitat-at-percentage (merge props {:percentage closest-percentage})) 2)
        [mx my] margin
        [ox oy] origin]
    (swap! tooltip-content merge {:tooltip   {:style {:visibility "visible"}}
                                  :textbox   {:transform (str "translate("
                                                              (+ mx ox (* (/ closest-percentage 100) (- graph-domain tooltip-width)))
                                                              ", " (+ 10 (+ my (* graph-range (- 1 offset)))) ")")}
                                  :line      {:x1 pointx
                                              :y1 my
                                              :x2 pointx
                                              :y2 (+ my graph-range)}
                                  :text      [(str "Depth: " (.toFixed closest-depth 4)) (str "Habitat: " zone)]
                                  :datapoint {:cx pointx
                                              :cy pointy}})
    (if on-mousemove (on-mousemove {:percentage closest-percentage
                                    :habitat    zone
                                    :depth      closest-depth}))))


(defn mouse-leave-graph [{:keys [tooltip-content] :as props}]
  (swap! tooltip-content merge {:tooltip {:style {:visibility "hidden"}}}))


(defn draw-graph [{:keys [bathymetry habitat width height zone-color-mapping margin font-size-tooltip font-size-axes]
                   :as   props
                   :or   {font-size-tooltip 16
                          font-size-axes    16}}]
  (let [tooltip-content (reagent/atom {:tooltip   {:style {:visibility "hidden"}}
                                       :datapoint {:cx 0 :cy 0 :r 5}
                                       :line      {:x1 0 :y1 0 :x2 20 :y2 20}
                                       :textbox   {:transform "translate(0, 0)"}
                                       :text      ["Depth: " "Habitat: "]})
        line-height-tooltip (* 1.6 font-size-tooltip)
        line-height-axes (* 1.6 font-size-axes)
        tooltip-width 200
        origin [(* 3 line-height-axes) (* 3 line-height-axes)]
        [ox oy] origin
        [mx my] margin
        graph-range (- height (+ (* 2 my) oy))
        graph-domain (- width (+ (* 2 mx) ox))
        max-depth (max-depth bathymetry)
        min-depth (min-depth bathymetry)
        spread (- max-depth min-depth)
        graph-line-offset 0.4
        graph-line-string (formatted-graph-line (merge props {:graph-domain graph-domain
                                                              :graph-range  graph-range
                                                              :min-depth    min-depth
                                                              :max-depth    max-depth
                                                              :spread       spread
                                                              :origin       origin
                                                              :offset       graph-line-offset}))
        clip-path-string (str graph-line-string "L " (- width mx) " " (+ graph-range my) "L " (+ mx ox) " " (+ my graph-range) " Z")]
    (fn []
      [:div#transect-plot
       [:svg {:width  width
              :height height}

        [:defs
         [:clipPath {:id "clipPath"}
          [:path {:d clip-path-string}]]]

        [:rect#background {:x      0
                           :y      0
                           :width  width
                           :height height
                           :style  {:fill "lightblue" :opacity 0.2}}]

        ;draw habitat zones
        [:g#habitat-zones {:style {:opacity 0.5}}
         (for [zone habitat]
           (let [[start-percentage end-percentage zone-name] zone]
             [:rect {:key    zone
                     :x      (percentage-to-x-pos (merge props {:percentage   start-percentage
                                                                :graph-domain graph-domain
                                                                :origin       origin}))
                     :y      my
                     :width  (* (/ (- end-percentage start-percentage) 100) graph-domain)
                     :height graph-range
                     :style  {:fill      ((keyword zone-name) zone-color-mapping)
                              :clip-path "url(#clipPath)"
                              }}]))]

        ;draw bathymetry line
        [:path {:d            graph-line-string
                :fill         "none"
                :stroke       "black"
                :stroke-width 3}]

        ;draw axes
        [axes (merge props {:origin origin})]

        ;label axes
        [axis-labels (merge props {:line-height   line-height-axes
                                   :font-size     font-size-axes
                                   :x-axis-offset 10
                                   :y-axis-offset 10
                                   :x-steps       20
                                   :y-steps       6
                                   :max-depth     max-depth
                                   :min-depth     min-depth
                                   :graph-domain  graph-domain
                                   :graph-range   graph-range
                                   :spread        spread
                                   :origin        origin
                                   :offset        graph-line-offset})]

        [tooltip (merge props {:tooltip-content tooltip-content
                               :line-height     line-height-tooltip
                               :tooltip-width   tooltip-width
                               :font-size       font-size-tooltip})]

        (let [buffer 20]
          [:rect#mouse-move-area {:x              (- (+ mx ox) buffer)
                                  :y              (- mx buffer)
                                  :width          (+ (* 2 buffer) graph-domain)
                                  :height         (+ (* 2 buffer) graph-range)
                                  :style          {:opacity 0}
                                  :on-mouse-move  #(mouse-move-graph (merge props {:tooltip-content tooltip-content
                                                                                   :tooltip-width   tooltip-width
                                                                                   :event           %
                                                                                   :max-depth       max-depth
                                                                                   :min-depth       min-depth
                                                                                   :graph-domain    graph-domain
                                                                                   :graph-range     graph-range
                                                                                   :spread          spread
                                                                                   :origin          origin
                                                                                   :offset          graph-line-offset}))
                                  :on-mouse-leave #(mouse-leave-graph {:tooltip-content tooltip-content})}])]])))


(defn transect-plot []
  (let [zone-colour-mapping habitat-zone-colours
        left (rand-int 500)]
    [:div {:style {:position "absolute" :bottom 0 :left left}}
     [draw-graph {:zone-color-mapping zone-colour-mapping
                  :bathymetry         (generate-bathymetry)
                  :habitat            (generate-habitat zone-colour-mapping)
                  :width              (- (gobj/get (dom/getViewportSize) "width") left)
                  :height             350
                  :margin             [20 20]}]]))
