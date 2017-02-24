(ns imas-seamap.plot.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [goog.object :as gobj]
            [goog.dom :as dom]))

(defn randColour []
  (str "rgb(" (rand-int 255) "," (rand-int 255) "," (rand-int 255) ")"))


(defn generate-bathymetry []
  (let [a (iterate inc 0)
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
        habitat (for [[x1 x2] (map vector d (rest d))]
                  [x1 x2 (-> habitat-zone-colours keys rand-nth name)])]
    habitat))


(defn min-depth [bathymetry]
  (apply min (map second bathymetry)))


(defn max-depth [bathymetry]
  (apply max (map second bathymetry)))


(defn depth-to-y-pos [{:keys [depth graph-range offset min-depth spread margin] :as props}]
  (let [[m-left m-right m-top m-bottom] margin]
    (+ (* graph-range (- 1 offset) (/ (- depth min-depth) spread)) m-top)))


(defn percentage-to-x-pos [{:keys [percentage origin graph-domain margin]}]
  (let [[m-left m-right m-top m-bottom] margin
        [ox oy] origin]
    (+ m-left ox (* (/ percentage 100) graph-domain))))


(defn habitat-at-percentage [{:keys [habitat percentage]}]
  (peek (filterv (fn [[p]] (<= p percentage)) habitat)))

(defn formatted-graph-line [{:keys [bathymetry origin graph-domain graph-range offset min-depth max-depth spread] :as props}]
  (let [[[first-percentage first-depth] & remaining] bathymetry
        start-string (str "M " (percentage-to-x-pos (merge props {:percentage first-percentage})) " " (depth-to-y-pos (merge props {:depth first-depth})) " ")
        middle-string (clojure.string/join (for [[percentage depth] remaining]
                                             (str "L " (percentage-to-x-pos (merge props {:percentage percentage})) " " (depth-to-y-pos (merge props {:depth depth})) " ")))]
    (str start-string middle-string)))


(defn axes [{:keys [width height origin margin]}]
  (let [[m-left m-right m-top m-bottom] margin
        [ox oy] origin]
    [:g {:fill         "none"
         :stroke       "black"
         :stroke-width "3"}
     [:polyline {:points (str (+ m-left ox) "," m-top " "
                              (+ m-left ox) "," (- height (+ m-bottom oy)) " "
                              (- width m-right) "," (- height (+ m-bottom oy)))}]]))


(defn axis-labels [{:keys [x-axis-offset y-axis-offset line-height font-size offset
                           min-depth spread graph-domain graph-range origin margin]}]
  (let [origin-depth (+ min-depth (/ spread (- 1 offset)))
        delta (- origin-depth min-depth)
        x-steps (int (/ graph-domain 50))
        y-steps (int (/ graph-range 30))
        [m-left m-right m-top m-bottom] margin
        [ox oy] origin]
    [:g {:style {:line-height line-height
                 :font-size   font-size}}
     ;x-axis labels
     [:g {:style {:text-anchor "middle"}}
      (for [i (take (+ 1 x-steps) (range))]
        [:text {:key (hash (str "percentageLabel" i))
                :x   (+ (* (/ i x-steps) graph-domain) ox m-left)
                :y   (+ m-top font-size x-axis-offset graph-range)}
         (str (int (* (/ i x-steps) 100)))])
      [:text {:x     (+ m-left ox (/ graph-domain 2))
              :y     (+ line-height font-size x-axis-offset m-top graph-range)
              :style {:font-weight "bold"}}
       "Percentage Along Transect (%)"]]
     ;y-axis labels
     [:g {:style {:text-anchor "end"}}
      (for [i (take (+ 1 y-steps) (range))]
        [:text {:key (hash (str "depthLabel" i))
                :x   (- (+ m-left ox) y-axis-offset)
                :y   (+ m-top (/ font-size 2) (* (/ i y-steps) graph-range))}
         (str (int (+ min-depth (* (/ i y-steps) delta))))])
      [:text {:x         (- (+ m-left ox) (+ line-height font-size y-axis-offset))
              :y         (+ m-top (/ graph-range 2))
              :transform (str "rotate(-90, " (- (+ m-left ox) (+ line-height font-size y-axis-offset)) ", " (+ m-top (/ graph-range 2)) ")")
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
        [m-left _ _ _] margin
        [ox oy] origin
        dist-from-y-axis (- pagex (+ indent-from-page ox m-left))]
    (/ dist-from-y-axis graph-domain)))


(defn mouse-move-graph [{:keys [bathymetry event tooltip-content tooltip-width origin graph-domain graph-range margin offset on-mousemove] :as props}]
  (let [pagex (gobj/get event "pageX")
        percentage (min (max (* 100 (mouse-pos-to-percentage (merge props {:pagex pagex}))) 0) 100)
        [before after] (split-with #(< (first %) percentage) bathymetry)
        previous (if (seq before) (last before) (first after))
        next (if (seq after) (first after) (last before))
        next-is-closest (< (- (first next) percentage) (- percentage (first previous)))
        [closest-percentage closest-depth] (if next-is-closest next previous)
        pointx (percentage-to-x-pos (merge props {:percentage closest-percentage}))
        pointy (depth-to-y-pos (merge props {:depth closest-depth}))
        [_ _ zone] (habitat-at-percentage (merge props {:percentage closest-percentage}))
        [m-left m-right m-top m-bottom] margin
        [ox oy] origin]
    (swap! tooltip-content merge {:tooltip   {:style {:visibility "visible"}}
                                  :textbox   {:transform (str "translate("
                                                              (+ m-left ox (* (/ closest-percentage 100) (- graph-domain tooltip-width)))
                                                              ", " (+ 10 (+ m-top (* graph-range (- 1 offset)))) ")")}
                                  :line      {:x1 pointx
                                              :y1 m-top
                                              :x2 pointx
                                              :y2 (+ m-top graph-range)}
                                  :text      [(str "Depth: " (.toFixed closest-depth 4)) (str "Habitat: " zone)]
                                  :datapoint {:cx pointx
                                              :cy pointy}})
    (if on-mousemove (on-mousemove {:percentage closest-percentage
                                    :habitat    zone
                                    :depth      closest-depth}))))


(defn mouse-leave-graph [{:keys [tooltip-content] :as props}]
  (swap! tooltip-content merge {:tooltip {:style {:visibility "hidden"}}}))


(defn graph [props]
  (let [tooltip-content (reagent/atom {:tooltip   {:style {:visibility "hidden"}}
                                       :datapoint {:cx 0 :cy 0 :r 5}
                                       :line      {:x1 0 :y1 0 :x2 20 :y2 20}
                                       :textbox   {:transform "translate(0, 0)"}
                                       :text      ["Depth: " "Habitat: "]})]
    (fn [{:keys [bathymetry habitat width height zone-color-mapping margin font-size-tooltip font-size-axes]
          :as   props
          :or   {font-size-tooltip 16
                 font-size-axes    16
                 margin [5 15 15 5]}}]
      (let [line-height-tooltip (* 1.6 font-size-tooltip)
            line-height-axes (* 1.6 font-size-axes)
            tooltip-width 200
            origin [(* 3 line-height-axes) (* 3 line-height-axes)]
            [ox oy] origin
            [m-left m-right m-top m-bottom] margin
            graph-range (- height (+ m-top m-bottom oy))
            graph-domain (- width (+ m-left m-right ox))
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
                                                                  :offset       graph-line-offset
                                                                  :margin margin}))
            clip-path-string (str graph-line-string "L " (- width m-right) " " (+ graph-range m-top) "L " (+ m-left ox) " " (+ graph-range m-top) " Z")]
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
                             :style  {:opacity 0.2}}]

                                        ;draw habitat zones
          [:g#habitat-zones {:style {:opacity 0.5}}
           (for [zone habitat]
             (let [[start-percentage end-percentage zone-name] zone]
               [:rect {:key    zone
                       :x      (percentage-to-x-pos (merge props {:percentage   start-percentage
                                                                  :graph-domain graph-domain
                                                                  :origin       origin
                                                                  :margin margin}))
                       :y      m-top
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
          [axes (merge props {:origin origin
                              :margin margin})]

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
                                     :margin margin
                                     :offset        graph-line-offset})]

          [tooltip (merge props {:tooltip-content tooltip-content
                                 :line-height     line-height-tooltip
                                 :tooltip-width   tooltip-width
                                 :font-size       font-size-tooltip
                                 :margin margin})]

          (let [buffer (min 20 (min m-top m-right))]
            [:rect#mouse-move-area {:x              (- (+ m-left ox) buffer)
                                    :y              (- m-top buffer)
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
                                                                                     :margin margin
                                                                                     :offset          graph-line-offset}))
                                    :on-mouse-leave #(mouse-leave-graph {:tooltip-content tooltip-content})}])]]))))

(defn testGraph []
  (graph {:bathymetry (generate-bathymetry)
          :habitat (generate-habitat habitat-zone-colours)
          :width (gobj/get (dom/getViewportSize) "width")
          :height 300
          :zone-color-mapping habitat-zone-colours
          :font-size-tooltip 16
          :font-size-axes 16}))
