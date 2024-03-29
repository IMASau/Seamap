;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.plot.views
  (:require [reagent.core :as reagent]
            [clojure.string :as string]
            [imas-seamap.blueprint :refer [button non-ideal-state spinner]]
            [imas-seamap.utils :refer [handler-dispatch] :include-macros true]
            [goog.dom :as dom]
            [goog.object :as gobj]
            [goog.math :as gmaths]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn randColour []
  (str "rgb(" (rand-int 255) "," (rand-int 255) "," (rand-int 255) ")"))


(defn generate-bathymetry []
  (let [percentages (iterate inc 0)
        depths (repeatedly 101 #(+ (rand 50) (rand 50)))
        bathymetry (mapv vector percentages depths)
        bath-with-nil (mapv (fn [[p d]](vector p (if (or (< p 20) (> p 80)) nil d))) bathymetry)]
    bath-with-nil))


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


(defn generate-habitat [zone-colour-mapping]
  (let [num-zones (+ 3 (rand-int 7))
        a (repeatedly num-zones #(+ 10 (rand-int 90)))
        b (list* 0 100 a)
        c (sort b)
        d (distinct c)
        habitat (for [[x1 x2] (map vector d (rest d))]
                  [x1 x2 (-> zone-colour-mapping keys rand-nth name)])
        habitat-with-nil (map (fn [[start end zone]] (vector start end (if (< (rand) 0.33) nil zone))) habitat)]
    habitat-with-nil))


(defn min-depth [bathymetry]
  (let [min-val (apply min (filterv #(not (nil? %)) (mapv second bathymetry)))]
    (if (zero? min-val) -0.1 min-val)))


(defn max-depth [bathymetry]
  (let [max-val (apply max (map second bathymetry))]
    (if (zero? max-val) 0.1 max-val)))


(defn depth-to-y-pos [{:keys [depth graph-range offset min-depth spread margin]}]
  (let [[_m-left _m-right m-top _m-bottom] margin]
    (+ (* graph-range (- 1 offset) (/ (- depth min-depth) spread)) m-top)))


(defn percentage-to-x-pos [{:keys [percentage origin graph-domain margin]}]
  (let [[m-left _m-right _m-top _m-bottom] margin
        [ox _oy] origin]
    (+ m-left ox (* (/ percentage 100) graph-domain))))


(defn habitat-at-percentage [{:keys [:transect.results/habitat percentage]}]
  (peek (filterv #(<= (:start_percentage %) percentage) habitat)))

(defn depth-at-percentage [{:keys [:transect.results/bathymetry percentage]}]
  (->> bathymetry
       (filterv #(<= (first %) percentage))
       peek
       second))

(defn graph-line-string [{:keys [:transect.results/bathymetry margin graph-range] :as props}]
  (let [[_m-left _m-right m-top _m-bottom] margin
        [[p1 d1] & remaining] bathymetry
        [_p2 d2] remaining
        first-x-pos (percentage-to-x-pos (merge props {:percentage p1}))
        graph-start (string/join (vector (str "M " first-x-pos " " (if (nil? d1) (+ m-top graph-range) (depth-to-y-pos (merge props {:depth d1}))) " L ")
                                                 (if (and (not (nil? d1)) (nil? d2))
                                                   (str first-x-pos " " (+ m-top graph-range) " ")
                                                   "")))
        graph-middle (string/join (for [[[_ prev-d] [p d] [_ next-d]] (map vector bathymetry remaining (rest remaining))]
                                            (let [x-pos (percentage-to-x-pos (merge props {:percentage p}))]
                                              (if (nil? d)
                                                (str "" x-pos " " (+ m-top graph-range) " ")
                                                (string/join (vector (when-not prev-d (str x-pos " " (+ m-top graph-range) " "))
                                                                             (str x-pos " " (depth-to-y-pos (merge props {:depth d})) " ")
                                                                             (when-not next-d (str x-pos " " (+ m-top graph-range) " "))))))))
        [last-p last-d] (last bathymetry)
        graph-end (str (percentage-to-x-pos (merge props {:percentage last-p})) " " (if (nil? last-d) (+ m-top graph-range) (depth-to-y-pos (merge props {:depth last-d}))) " ")]
    (str graph-start graph-middle graph-end)))


(defn axes [{:keys [width height origin margin]}]
  (let [[m-left m-right m-top m-bottom] margin
        [ox oy] origin]
    [:g {:fill         "none"
         :stroke       "black"
         :stroke-width "3"}
     [:polyline {:points (str (+ m-left ox) "," m-top " "
                              (+ m-left ox) "," (- height (+ m-bottom oy)) " "
                              (- width m-right) "," (- height (+ m-bottom oy)))}]]))


;;; Axis-tick calculation via http://www.realtimerendering.com/resources/GraphicsGems/gems/Label.c
(defn nice-num
  "find a 'nice' number approximately equal to x. Round the number if
  round=1, take ceiling if round=0"
  [x round]
  (let [expv (Math/floor (Math/log10 x)) ; exponent of x
        f    (/ x (Math/pow 10 expv))    ; fractional part of x
        nf   (if round                     ; nice rounded fraction
               (cond (< f 1.5) 1
                     (< f 3)   2
                     (< f 7)   5
                     :default  10)
               (cond (< f 1)   1
                     (< f 2)   2
                     (< f 5)   5
                     :default  10))]
    (* nf (Math/pow 10 expv))))

(def ^:private NUM-TICKS 10)
(defn loose-label-ticks
  "Loose-labelling method for data from min to max.  Expects min<max."
  [min max]
  (let [rng      (nice-num (- max min) false)
        d        (nice-num (/ rng (- NUM-TICKS 1)) true)
        graphmin (* d (Math/floor (/ min d)))
        graphmax (* d (Math/ceil  (/ max d)))]
    (range graphmin (+ graphmax (/ d 2)) d)))

(defn axis-labels [{:keys [x-axis-offset y-axis-offset line-height font-size offset
                           min-depth max-x spread graph-domain graph-range origin margin
                           :transect.results/habitat]}]
  (let [origin-depth (+ min-depth (/ spread (- 1 offset)))
        delta (- origin-depth min-depth)
        y-steps (int (/ graph-range 30))
        x-ticks   (loose-label-ticks 0 max-x)
        x-ticks   (if (< (last x-ticks) max-x) (butlast x-ticks) x-ticks)
        [m-left _m-right m-top _m-bottom] margin
        [ox _oy] origin]
    (when (pos? y-steps)
      [:g {:style {:line-height line-height
                   :font-size   font-size}}
       ;; x-axis labels
       [:g {:style {:text-anchor "middle"}}
        (for [tick x-ticks]
          [:text {:key tick
                  :x   (+ (* (/ tick max-x) graph-domain) ox m-left)
                  :y   (+ m-top font-size x-axis-offset graph-range)}
           (str tick)])
        [:text {:x     (+ m-left ox (/ graph-domain 2))
                :y     (+ line-height font-size x-axis-offset m-top graph-range)
                :style {:font-weight "bold"}}
         (if (seq habitat) "Distance Along Transect (m)" "Percentage Along Transect")]]
       ;; y-axis labels
       [:g {:style {:text-anchor "end"}}
        (for [i (range (+ 1 y-steps))]
          [:text {:key (hash (str "depthLabel" i))
                  :x   (- (+ m-left ox) y-axis-offset)
                  :y   (+ m-top (/ font-size 2) (* (/ i y-steps) graph-range))
                  }
           (str (int (+ min-depth (* (/ i y-steps) delta))))])
        [:text {:x         (- (+ m-left ox) (+ line-height font-size y-axis-offset))
                :y         (+ m-top (/ graph-range 2))
                :transform (str "rotate(-90, " (- (+ m-left ox) (+ line-height font-size y-axis-offset)) ", " (+ m-top (/ graph-range 2)) ")")
                :style     {:text-anchor "middle"
                            :font-weight "bold"}}
         "Depth (m)"]]])))


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
                            :stroke-width 2}}
                   (:datapoint @tooltip-content))]
   [:g#textbox (merge {:transform "translate(0, 0)"} (:textbox @tooltip-content))
    [:defs
     [:clipPath {:id "tooltip-clip"}
      [:rect {:x 0 :y 0 :width tooltip-width :height (* 4.5 line-height)}]]]
    [:rect {:x      0
            :y      0
            :rx     5
            :ry     5
            :width  tooltip-width
            :height (* 4.5 line-height)
            :style  {:opacity      0.9
                     :fill         "white"
                     :stroke       "black"
                     :stroke-width 2}}]
    [:text {:style {:font-size font-size} :clip-path "url(#tooltip-clip)"}
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
        [ox _oy] origin
        dist-from-y-axis (- pagex (+ indent-from-page ox m-left))]
    (/ dist-from-y-axis graph-domain)))


(defn mouse-move-graph [{:keys [:transect.results/habitat
                                :transect.results/zone-legend
                                graph-domain graph-range
                                tooltip-content tooltip-width
                                origin margin offset
                                max-x
                                event on-mousemove] :as props}]
  (let [pagex                              (gobj/get event "pageX")
        [m-left _m-right m-top _m-bottom]  margin
        [ox _oy]                           origin
        percentage                         (min (max (* 100 (mouse-pos-to-percentage (merge props {:pagex pagex}))) 0) 100)
        depth                              (depth-at-percentage (merge props {:percentage percentage}))
        pointx                             (percentage-to-x-pos (merge props {:percentage percentage}))
        pointy                             (if depth (depth-to-y-pos (merge props {:depth depth})) (+ graph-range m-top))
        {:keys [layer_name name]}          (habitat-at-percentage (merge props {:percentage percentage}))
        depth-label                        (if depth (str (.toFixed depth) "m") "No data")
        zone-label                         (or (get zone-legend name name) "No data")
        layer-label                        (or layer_name "No data")
        distance                           (int (/ (* percentage max-x) 100))
        distance-unit                      (if (seq habitat) "m" "%")]
    (swap! tooltip-content merge {:tooltip   {:style {:visibility "visible"}}
                                  :textbox   {:transform (str "translate("
                                                              (+ m-left ox (* (/ percentage 100) (- graph-domain tooltip-width)))
                                                              ", " (- (+ m-top (* graph-range (- 1 offset))) 10) ")")}
                                  :line      {:x1 pointx
                                              :y1 m-top
                                              :x2 pointx
                                              :y2 (+ m-top graph-range)}
                                  :text      [(str "Depth: " depth-label)
                                              (str "Habitat: " zone-label)
                                              (str "Layer: " layer-label)
                                              (str "Distance: " distance distance-unit)]
                                  :datapoint {:cx pointx
                                              :cy pointy}})
    (when on-mousemove (on-mousemove {:percentage percentage
                                    :habitat    zone-label
                                    :depth      depth}))))


(defn mouse-leave-graph [{:keys [tooltip-content on-mouseout]}]
  (swap! tooltip-content merge {:tooltip {:style {:visibility "hidden"}}})
  (when on-mouseout (on-mouseout)))


(defn transect-graph [_props]
  (let [tooltip-content (reagent/atom {:tooltip   {:style {:visibility "hidden"}}
                                       :datapoint {:cx 0 :cy 0 :r 5}
                                       :line      {:x1 0 :y1 0 :x2 20 :y2 20}
                                       :textbox   {:transform "translate(0, 0)"}
                                       :text      ["Depth: " "Habitat: " "Distance: "]})]
    (fn [{:keys [:transect.results/bathymetry
                 :transect.results/habitat
                 :transect.results/zone-colours
                 width height margin
                 font-size-tooltip font-size-axes]
          :as   props
          :or   {font-size-tooltip 12
                 font-size-axes    16
                 margin            [5 15 15 5]}}]
      (let [line-height-tooltip             (* 1.5 font-size-tooltip)
            line-height-axes                (* 1.6 font-size-axes)
            tooltip-width                   420
            origin                          [(* 3 line-height-axes) (* 3 line-height-axes)]
            [ox oy]                         origin
            [m-left m-right m-top m-bottom] margin
            graph-range                     (- height (+ m-top m-bottom oy))
            graph-domain                    (- width (+ m-left m-right ox))
            max-depth                       (* 1.01 (max-depth bathymetry))
            min-depth                       (* 0.99 (min-depth bathymetry))
            max-x                           (:end_distance (last habitat) 100)
            spread                          (- max-depth min-depth)
            graph-line-offset               0.4
            graph-line-string               (graph-line-string (merge props {:graph-domain graph-domain
                                                                             :graph-range  graph-range
                                                                             :min-depth    min-depth
                                                                             :max-depth    max-depth
                                                                             :spread       spread
                                                                             :origin       origin
                                                                             :offset       graph-line-offset
                                                                             :margin       margin}))
            clip-path-string                (str graph-line-string " "
                                                 (+ graph-domain ox m-left) " " (+ graph-range m-top) " "
                                                 (+ ox m-left) " " (+ graph-range m-top) " "
                                                 "Z")]
        (when (and (pos? graph-range)  (gmaths/isFiniteNumber graph-range))
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

            ;; draw habitat zones
            [:g#habitat-zones
             (for [{:keys [start_percentage end_percentage name] :as _zone} habitat
                   :let [zone-colour (get zone-colours name)]]
               (when (and name zone-colour)
                 (let [x-pos (percentage-to-x-pos (merge props {:percentage   start_percentage
                                                                :graph-domain graph-domain
                                                                :origin       origin
                                                                :margin       margin}))
                       width (* (/ (- end_percentage start_percentage) 100) graph-domain)]
                   [:g {:key start_percentage}
                    [:rect {:x      x-pos
                            :y      m-top
                            :width  width
                            :height graph-range
                            :style  {:opacity 0.25
                                     :fill    zone-colour}}]
                    [:rect {:x      x-pos
                            :y      m-top
                            :width  width
                            :height graph-range
                            :style  {:opacity   0.75
                                     :fill      zone-colour
                                     :clip-path "url(#clipPath)"}}]])))]

            ;; draw bathymetry line
            [:path {:d            graph-line-string
                    :fill         "none"
                    :stroke       "black"
                    :stroke-width 2}]

            ;; draw axes
            [axes (merge props {:origin origin
                                :margin margin})]

            ;; label axes
            [axis-labels (merge props {:line-height   line-height-axes
                                       :font-size     font-size-axes
                                       :x-axis-offset 10
                                       :y-axis-offset 10
                                       ;; default of 100 means it looks like pctg if we don't have habitat data
                                       :max-x         max-x
                                       :y-steps       6
                                       :max-depth     max-depth
                                       :min-depth     min-depth
                                       :graph-domain  graph-domain
                                       :graph-range   graph-range
                                       :spread        spread
                                       :origin        origin
                                       :margin        margin
                                       :offset        graph-line-offset})]

            [tooltip (merge props {:tooltip-content tooltip-content
                                   :line-height     line-height-tooltip
                                   :tooltip-width   tooltip-width
                                   :font-size       font-size-tooltip
                                   :margin          margin})]

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
                                                                                       :max-x           max-x
                                                                                       :graph-domain    graph-domain
                                                                                       :graph-range     graph-range
                                                                                       :spread          spread
                                                                                       :origin          origin
                                                                                       :margin          margin
                                                                                       :offset          graph-line-offset}))
                                      :on-mouse-leave #(mouse-leave-graph (assoc props
                                                                                 :tooltip-content tooltip-content))}])]])))))

(defn- transect-no-data []
  [non-ideal-state
   {:title       "No Data to Display"
    :description "Try the \"Draw Transect\" button above!"}])

(defn- transect-loading []
  [non-ideal-state
   {:title       "Loading..."
    :description (reagent/as-element
                  [button {:text     "Cancel"
                           :on-click (handler-dispatch [:transect.query/cancel])}])
    :icon        (reagent/as-element [spinner {:intent "success"}])}])

(defn- transect-error []
  [non-ideal-state
   {:title       "Error"
    :description "There was an error querying the data"
    :icon        "bp3-icon-error"}])

(def test-data
  {:transect.results/bathymetry (generate-bathymetry)
   :transect.results/habitat    (generate-habitat habitat-zone-colours)
   :zone-colour-mapping         habitat-zone-colours})

(defn transect-display-component [{:keys [:transect.results/status size] :as results}]
  [:div {:style {:position "relative" :height "100%"}}
   [:div {:style {:position "absolute" :width "100%" :height (str (:height size) "px")}}
    (case status
      :transect.results.status/empty   [transect-no-data]
      :transect.results.status/loading [transect-loading]
      :transect.results.status/error   [transect-error]
      :transect.results.status/partial
      [:div.transect-overlay
       [non-ideal-state {:icon        (reagent/as-element [spinner {:intent "success"}])
                         :description (reagent/as-element
                                       [button {:text     "Cancel"
                                                :on-click (handler-dispatch [:transect.query/cancel])}])}]]
      ;; Default (merge the "size" map, from the SizeMe component, so
      ;; width + height are available as assumed by transect-graph):
      [transect-graph (merge results size)])]])

