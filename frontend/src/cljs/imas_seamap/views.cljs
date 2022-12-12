;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.views
  (:require [clojure.string :as string]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [imas-seamap.blueprint :as b :refer [use-hotkeys]]
            [imas-seamap.interop.react :refer [css-transition-group css-transition container-dimensions use-memo]]
            [imas-seamap.map.views :refer [map-component]]
            [imas-seamap.map.layer-views :refer [layer-card main-national-layer-card layer-catalogue-node main-national-layer-catalogue-node]]
            [imas-seamap.state-of-knowledge.views :refer [state-of-knowledge floating-state-of-knowledge-pill floating-boundaries-pill floating-zones-pill]]
            [imas-seamap.story-maps.views :refer [featured-maps featured-map-drawer]]
            [imas-seamap.plot.views :refer [transect-display-component]]
            [imas-seamap.utils :refer [handler-fn handler-dispatch] :include-macros true]
            [imas-seamap.components :as components]
            [imas-seamap.map.utils :refer [layer-search-keywords]]
            [imas-seamap.fx :refer [show-message]]
            [goog.string.format]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

(defn- ->query-selector
  "Normalises selectors for helper-overlay into a form for use with
  document.querySelectorAll.  Arguments may be keywords, strings, or
  maps.  Maps must contain either an :id or :selector key.  Selectors
  without a # or . at the front are treated as ids."
  [selector]
  (let [normalise-selector-str #(if-not (#{"#" "."} (first %))
                                  (str "#" %)
                                  %)]
    (if (map? selector)
      (normalise-selector-str (name (or (:id selector) (:selector selector))))
      (normalise-selector-str (name selector)))))

(defn- elem-props [selector elem]
  (when elem
    (merge
     (-> elem .getBoundingClientRect js->clj)
     (-> elem .-dataset js->clj)
     (when (map? selector) selector))))

(defn- selectors+elements [selectors]
  (->> selectors
       (map
        (fn [selector]
          (map (partial elem-props selector)
               (array-seq (.querySelectorAll js/document (->query-selector selector))))))
       (apply concat)))

;; TODO: Update, replace?
(defn helper-overlay [& element-selectors]
  (let [*line-height* 17.6 *padding* 10 *text-width* 200 ;; hard-code
        *vertical-bar* 50 *horiz-bar* 100
        posn->offsets (fn [posn width height]
                        (case (name posn)
                          "top"    {:bottom (+ height *vertical-bar* *padding*)
                                    :left (- (/ width 2) (/ *text-width* 2))}
                          "bottom" {:top (+ height *vertical-bar* *padding*)
                                    :left (- (/ width 2) (/ *text-width* 2))}
                          "left"   {:right (+ width *horiz-bar* *padding*)
                                    :top (- (/ height 2) (/ *line-height* 2))}
                          "right"  {:left (+ width *horiz-bar* *padding*)
                                    :top (- (/ height 2) (/ *line-height* 2))}))
        wrapper-props (fn [posn {:keys [top left width height] :as _elem-props}]
                        (let [vertical-padding   (when (#{"top" "bottom"} posn) *padding*)
                              horizontal-padding (when (#{"left" "right"} posn) *padding*)]
                          {:width  (+ width horizontal-padding) ; allow for css padding
                           :height (+ height vertical-padding)
                           :top    (if (= posn "top")  (- top *padding*)  top)
                           :left   (if (= posn "left") (- left *padding*) left)}))
        open? @(re-frame/subscribe [:help-layer/open?])]
    [b/overlay {:is-open  open?
                :on-close #(re-frame/dispatch [:help-layer/close])}
     (when open?
       (for [{:keys [width height
                     helperText helperPosition
                     textWidth]
                    :or {helperPosition "right"}
                    :as eprops} (selectors+elements element-selectors)
             :let [posn-cls (str "helper-layer-" helperPosition)]
             :when (and eprops
                        (pos? height)
                        (not (string/blank? helperText)))]
         ^{:key (hash eprops)}
         [:div.helper-layer-wrapper {:class posn-cls
                                     :style (wrapper-props helperPosition eprops)}
          [:div.helper-layer-tooltip {:class posn-cls
                                      :style (posn->offsets helperPosition width height)}
           [:div.helper-layer-tooltiptext {:style {:width (or textWidth *text-width*)}}
            helperText]]]))]))

;; TODO: Update, replace?
(defn- help-button []
  [:div.layer-controls.help-button
   [b/tooltip {:content "Show the quick-help guide"}
    [:span.control.bp3-icon-large.bp3-icon-help.bp3-text-muted
     {:on-click #(re-frame/dispatch [:help-layer/open])}]]])

(defn- ->sort-by [sorting-info ordering-key]
  (let [name-key-mapping (get sorting-info ordering-key)]
    ;; Sort by key first, then name (ie, return vector of [key name])
    (comp #(vector (get name-key-mapping %) %) first)))

(defn- layers->nodes
  "group-ordering is the category keys to order by, eg [:organisation :data_category]"
  [layers [ordering & ordering-remainder :as group-ordering] sorting-info expanded-states id-base
   {:keys [main-national-layer] :as layer-props} open-all?]
  (for [[val layer-subset] (sort-by (->sort-by sorting-info ordering) (group-by ordering layers))
        ;; sorting-info maps category key -> label -> [sort-key,id].
        ;; We use the id for a stable node-id:
        :let [sorting-id (get-in sorting-info [ordering val 1])
              id-str (str id-base "|" sorting-id)
              display-name (get-in sorting-info [ordering val 2])
              expanded? (or (get expanded-states id-str false) open-all?)]]
    {:id id-str
     :label (reagent/as-element
             [:<>
              [b/icon
               {:icon  "caret-right"
                :size  "22px"
                :class (str "bp3-tree-node-caret" (when expanded? " bp3-tree-node-caret-open"))}]
              [b/clipped-text {:ellipsize true}
               (or display-name "Ungrouped")]])
     :isExpanded expanded?
     :hasCaret   false
     :className  (-> ordering name (string/replace "_" "-"))
     :childNodes (concat
                  (if (seq ordering-remainder)
                    (layers->nodes layer-subset (rest group-ordering) sorting-info expanded-states id-str layer-props open-all?)
                    (map-indexed
                     (fn [i layer]
                       ((if (= layer main-national-layer)
                          main-national-layer-catalogue-node
                          layer-catalogue-node)
                        {:id          (str id-str "-" i)
                         :layer       layer
                         :layer-props layer-props}))
                     layer-subset))
                  
                  ;; Dummy element appended inside of category ordering content for achieving styling
                  (when (= ordering :category)
                    [{:id (str id-str "-" (inc (count layer-subset)))
                      :className "tree-cap"}]))}))

(defn- layer-catalogue-tree [_layers _ordering _id _layer-props _open-all?]
  (let [expanded-states (re-frame/subscribe [:ui.catalogue/nodes])
        sorting-info (re-frame/subscribe [:sorting/info])
        on-open (fn [node]
                  (let [node (js->clj node :keywordize-keys true)]
                    (re-frame/dispatch [:ui.catalogue/toggle-node (:id node)])))
        on-close (fn [node]
                   (let [node (js->clj node :keywordize-keys true)]
                     (re-frame/dispatch [:ui.catalogue/toggle-node (:id node)])))
        on-click (fn [node]
                   (let [{:keys [childNodes id]} (js->clj node :keywordize-keys true)]
                     (when (seq childNodes)
                       ;; If we have children, toggle expanded state, else add to map
                       (re-frame/dispatch [:ui.catalogue/toggle-node id]))))]
    (fn [layers ordering id layer-props open-all?]
     [:div.tab-body {:id id}
      [b/tree {:contents (layers->nodes layers ordering @sorting-info @expanded-states id layer-props open-all?)
               :onNodeCollapse on-close
               :onNodeExpand on-open
               :onNodeClick on-click
               :onNodeMouseEnter #(when-let [preview-layer (get-in (js->clj % :keywordize-keys true) [:nodeData :previewLayer])]
                                    (re-frame/dispatch [:map/update-preview-layer preview-layer]))
               :onNodeMouseLeave #(re-frame/dispatch [:map/update-preview-layer nil])}]])))

(defn- layer-catalogue [layers layer-props]
  (let [selected-tab @(re-frame/subscribe [:ui.catalogue/tab])
        select-tab   #(re-frame/dispatch [:ui.catalogue/select-tab %1])
        open-all?    (>= (count @(re-frame/subscribe [:map.layers/filter])) 3)]
    [b/tabs {:selected-tab-id selected-tab
             :on-change       select-tab}
     [b/tab
      {:id    "cat"
       :title "By Category"
       :panel (reagent/as-element
               [layer-catalogue-tree layers [:category :data_classification] "cat" layer-props open-all?])}]
     [b/tab
      {:id    "org"
       :title "By Organisation"
       :panel (reagent/as-element
               [layer-catalogue-tree layers [:category :organisation :data_classification] "org" layer-props open-all?])}]]))

(defn- viewport-only-toggle []
  (let [[icon text] (if @(re-frame/subscribe [:map/viewport-only?])
                      ["globe" "All Layers"]
                      ["map" "Viewport Layers Only"])]
    [b/button
     {:icon     icon
      :class    "bp3-fill"
      :intent   b/INTENT-PRIMARY
      :on-click #(re-frame/dispatch [:map/toggle-viewport-only])
      :text     text}]))

(defn- autosave-application-state-toggle []
  (let [[icon text] (if @(re-frame/subscribe [:autosave?])
                      ["disable" "Disable Autosave Application State"]
                      ["floppy-disk" "Enable Autosave Application State"])]
    [b/button
     {:icon     icon
      :class    "bp3-fill"
      :intent   b/INTENT-PRIMARY
      :on-click #(re-frame/dispatch [:toggle-autosave])
      :text     text}]))

(defn- layer-search-filter []
  [b/text-input
   {:value         @(re-frame/subscribe [:map.layers/filter])
    :placeholder   "Search Layers..."
    :type          "search"
    :right-element (reagent/as-element
                    [b/icon
                     {:icon "search-template"
                      :size "22px"}])
    :id            "layer-search"
    :class         "layer-search"
    :on-change     #(re-frame/dispatch [:map.layers/filter (.. % -target -value)])}])

(defn- active-layer-selection-list
  [{:keys [layers visible-layers main-national-layer loading-fn error-fn expanded-fn opacity-fn]}]
  [components/items-selection-list
   {:items
    (for [{:keys [id] :as layer} layers]
      {:key (str id)
       :content
       (if (= layer main-national-layer)
         [main-national-layer-card
          {:layer layer}]
         [layer-card
          {:layer layer
           :layer-state
           {:active?   true
            :visible?  (some #{layer} visible-layers)
            :loading?  (loading-fn layer)
            :errors?   (error-fn layer)
            :expanded? (expanded-fn layer)
            :opacity   (opacity-fn layer)}}])})
    :disabled    false
    :data-path   [:map :active-layers]
    :has-handle  false
    :is-reversed true}])

(defn- plot-component-animatable [{:keys [on-add on-remove]
                                  :or   {on-add identity on-remove identity}
                                  :as   _props}
                                 _child-component
                                 _child-props]
  (reagent/create-class
   {:display-name           "plot-component-animatable"
    :component-will-unmount on-remove
    :component-did-mount    on-add
    :reagent-render
    (fn [_props child-component child-props]
      [:div.plot-container
       [container-dimensions {:monitor-height true
                              :no-placeholder true}
        #(reagent/as-element [child-component
                              (merge child-props
                                     (js->clj % :keywordize-keys true))])]])}))

(defn plot-component []
  (let [show-plot (re-frame/subscribe [:transect.plot/show?])
        force-resize #(js/window.dispatchEvent (js/Event. "resize"))
        transect-results (re-frame/subscribe [:transect/results])]
    (fn []
      [:footer#plot-footer
       {:on-click (handler-dispatch [:transect.plot/toggle-visibility])}
       [:div.drag-handle [:span.bp3-icon-large.bp3-icon-drag-handle-horizontal]]
       [css-transition-group
        (when @show-plot
          [css-transition {:class-names "plot-height"
                           :timeout    {:enter 300 :exit 300}}
           [plot-component-animatable {:on-add force-resize :on-remove force-resize}
            transect-display-component (assoc @transect-results
                                              :on-mousemove
                                              #(re-frame/dispatch [:transect.plot/mousemove %])
                                              :on-mouseout
                                              #(re-frame/dispatch [:transect.plot/mouseout]))]])]])))

(defn loading-display []
  (let [loading?  @(re-frame/subscribe [:app/loading?])
        main-msg  @(re-frame/subscribe [:app/load-normal-msg])
        error-msg @(re-frame/subscribe [:app/load-error-msg])]
    (when loading?
      [:div.loading-splash {:class (when error-msg "load-error")}
       (if error-msg
         [b/non-ideal-state
          {:title       error-msg
           :description "We were unable to load everything we need to get started.  Please try again later."
           :icon      "error"}]
         [b/non-ideal-state
          {:title  main-msg
           :icon (reagent/as-element [b/spinner {:intent "success"}])}])])))

(defn welcome-dialogue []
  (let [open? @(re-frame/subscribe [:welcome-layer/open?])]
    [b/dialogue
     {:title
      (reagent/as-element
       [:<> "Welcome to" [:br] "Seamap Australia."])
      :class    "welcome-splash"
      :is-open  open?
      :on-close #(re-frame/dispatch [:welcome-layer/close])}
     [:div.bp3-dialog-body
      [:div.overview
       "Seamap Australia is a nationally synthesised product of
        seafloor habitat data collected from various stakeholders around
        Australia. Source datasets were reclassified according to a
        newly-developed national marine benthic habitat classification
        scheme, and synthesised to produce a single standardised GIS
        data layer of Australian benthic marine habitats."]
      [b/button
       {:text       "Get Started!"
        :intent     b/INTENT-PRIMARY
        :auto-focus true
        :on-click   #(re-frame/dispatch [:welcome-layer/close])}]]
     [:div.bp3-dialog-footer
      [:h3 "Citations"]
      [:div.citation-section
       [:p
        "Please cite as "
        [:span {:id "citation"} "Lucieer V, Walsh P, Flukes E, Butler C,Proctor R, Johnson C (2017). "
         [:i "Seamap Australia - a national seafloor habitat classification scheme."]
         " Institute for Marine and Antarctic Studies (IMAS), University of Tasmania (UTAS)."]]
       [:div.citation-button
        {:on-click
         ;; this code will copy the citation as rich text (with the italics for the citation), instead of as plaintext like with 'copy-text'
         ;; https://stackoverflow.com/a/34192073
         #(do
            (.removeAllRanges (js/window.getSelection))
            (let [range (js/document.createRange ())]
              (.selectNode range (js/document.getElementById "citation"))
              (.addRange (js/window.getSelection) range)
              (js/document.execCommand "copy")
              (.removeAllRanges (js/window.getSelection))
              (show-message
               ["Citation copied to clipboard!"
                {:intent b/INTENT-SUCCESS :icon "clipboard"}])))}
        [components/custom-icon {:icon "export" :size "27px"}]
        [:div "Copy Citation"]]]
      [:h3 "Acknowledgements"]
      [:p
       "Seamap Australia would not have been possible without the
        collaboration of its stakeholders, including (but not limited
        to): Parks Australia, The University of Queensland, The
        University of Western Australia, The University of Tasmania,
        James Cook University, Griffith University, Deakin University,
        CSIRO, IMOS, Geoscience Australia, Great Barrier Reef Marine
        Park Authority (GBRMPA), the National Environmental Science
        Program (NESP), and all State Governments."]]]))
(defn settings-overlay []
  [b/dialogue
   {:title      (reagent/as-element [:div.bp3-icon-cog "Settings"])
    :class      "settings-overlay-dialogue"
    :is-open    @(re-frame/subscribe [:ui/settings-overlay])
    :on-close   #(re-frame/dispatch [:ui/settings-overlay false])}
   [:div.bp3-dialog-body
    [autosave-application-state-toggle]
    [viewport-only-toggle]
    [b/button
     {:icon     "undo"
      :class    "bp3-fill"
      :intent   b/INTENT-PRIMARY
      :text     "Reset Interface"
      :on-click   #(re-frame/dispatch [:re-boot])}]]])

(defn- metadata-record [_props]
  (let [expanded (reagent/atom false)
        {:keys [img-url-base]} @(re-frame/subscribe [:url-base])]
    (fn  [{:keys [license-name license-link license-img constraints other]
           {:keys [category organisation metadata_url server_url layer_name]} :layer
           :as _layer-info}]
      [:div.metadata-record {:class (clojure.core/name category)}
       (when-let [logo (:logo @(re-frame/subscribe [:map/organisations organisation]))]
         [:div.section
          [:img.metadata-img.org-logo
           {:class (string/replace logo #"\..+$" "")
            :src   (str img-url-base logo)}]])
       [:h3.metadata-subheader "Citation Information:"]
       [:div.section
        [:p.citation  constraints]]
       (when (seq other)
         [:div.section
          [:h3.metadata-subheader "Usage:"]
          (map-indexed (fn [i o] (when o ^{:key i} [:p.other-constraints o])) other)])
       [:h3.clickable {:on-click (handler-fn (swap! expanded not))}
        [b/icon {:icon (if @expanded "chevron-down" "chevron-right")}]
        "API Access"]
       [b/collapse {:is-open               @expanded
                    :keep-children-mounted true}
        [:p "You can access the data online at"]
        [:div.server-info.section
         [:span "WMS:"]
         [:span.server-url [:a {:href server_url} server_url]]
         [:span.server-layer layer_name]]]
       [:div.license-info.clearfix.section
        [:h3 "License Information:"]
        (when license-img [:img.license.metadata-img {:src license-img}])
        [:a {:href license-link :target "_blank"} license-name]]
       [:div.more-info.section
        [:a {:href metadata_url :target "_blank"} "Click here for the full metadata record."]]
       [:div.section
        [:p.download-instructions
         "Downloading implies acceptance of all citation and usage requirements."]]])))

(defn- download-menu [{:keys [title disabled? layer bbox]}]
  [b/popover {:position           b/BOTTOM
              :disabled           disabled?
              :popover-class-name "bp3-minimal"
              :content            (reagent/as-element
                                   [b/menu
                                    [b/menu-item {:text     "GeoTIFF"
                                                  :label    (reagent/as-element [b/icon {:icon "globe"}])
                                                  :on-click (handler-dispatch [:map.layer/download
                                                                               layer
                                                                               bbox
                                                                               :map.layer.download/geotiff])}]
                                    [b/menu-item {:text     "SHP File"
                                                  :label    (reagent/as-element [b/icon {:icon "polygon-filter"}])
                                                  :on-click (handler-dispatch [:map.layer/download
                                                                               layer
                                                                               bbox
                                                                               :map.layer.download/shp])}]
                                    [b/menu-item {:text     "CSV"
                                                  :label    (reagent/as-element [b/icon {:icon "th"}])
                                                  :on-click (handler-dispatch [:map.layer/download
                                                                               layer
                                                                               bbox
                                                                               :map.layer.download/csv])}]])}
   [b/button {:text       title
              :disabled   disabled?
              :right-icon "caret-down"}]])

(defn info-card []
  (let [layer-info       @(re-frame/subscribe [:map.layer/info])
        {:keys [metadata_url category] :as layer} (:layer layer-info)
        title            (or (get-in layer-info [:layer :name]) "Layer Information")
        {:keys [region]} @(re-frame/subscribe [:map.layer.selection/info])]
    [b/dialogue {:title    title
                 :class    "info-card"
                 :is-open  (and layer-info (not (:hidden? layer-info)))
                 :on-close #(re-frame/dispatch [:map.layer/close-info])}
     [:div.bp3-dialog-body
      (case layer-info
        :display.info/loading
        [b/non-ideal-state
         {:title "Loading Metadata..."
          :icon  (reagent/as-element [b/spinner {:intent "success"}])}]

        :display.info/error
        [b/non-ideal-state
         {:title       "Error"
          :description "Unable to load metadata record.  Please try again later."
          :icon        "error"}]

        [metadata-record layer-info])]
     [:div.bp3-dialog-footer
      [:div.bp3-dialog-footer-actions
       (when (and
              metadata_url
              (re-matches #"^https://metadata\.imas\.utas\.edu\.au/geonetwork/srv/eng/catalog\.search#/metadata/[-0-9a-zA-Z]+$" metadata_url)
              (= category :habitat))
         [:<>
          [download-menu {:title     "Download Selection..."
                          :layer     layer
                          :disabled? (nil? region)
                          :bbox      region}]
          [download-menu {:title "Download All..."
                          :layer layer}]])
       [b/button {:text       "Close"
                  :auto-focus true
                  :intent     b/INTENT-PRIMARY
                  :on-click   (handler-dispatch [:map.layer/close-info])}]]]]))

(defn pre-leaflet-controls [& controls]
  (into [:div.pre-leaflet-controls.leaflet-touch] controls))

(defn- leaflet-control-button [{:keys [on-click tooltip icon]}]
  [:div.leaflet-bar.leaflet-control
   (if tooltip
     [b/tooltip
      {:content tooltip :position b/RIGHT}
      [:a {:on-click on-click}
       [b/icon {:icon icon :size 18}]]]
     [:a {:on-click on-click}
      [b/icon {:icon icon :size 18}]])])

(defn menu-button []
  [leaflet-control-button
   {:on-click #(re-frame/dispatch [:left-drawer/toggle])
    :icon     "menu"}])

(defn settings-button []
  [leaflet-control-button
   {:on-click #(re-frame/dispatch [:ui/settings-overlay true])
    :tooltip  "Settings"
    :icon     "cog"}])

(defn- floating-pills []
  (let [collapsed                (:collapsed @(re-frame/subscribe [:ui/sidebar]))
        state-of-knowledge-open? @(re-frame/subscribe [:sok/open?])
        valid-boundaries         @(re-frame/subscribe [:sok/valid-boundaries])
        boundaries               @(re-frame/subscribe [:sok/boundaries])
        active-boundary          @(re-frame/subscribe [:sok/active-boundary])
        open-pill                @(re-frame/subscribe [:sok/open-pill])]
    [:div
     {:class (str "floating-pills" (when collapsed " collapsed"))}

     [floating-state-of-knowledge-pill
      {:expanded?       (= open-pill "state-of-knowledge")
       :boundaries      boundaries
       :active-boundary active-boundary}]
     
     (when (and state-of-knowledge-open? active-boundary)
       [floating-boundaries-pill
        (merge
         valid-boundaries
         {:expanded?       (= open-pill "boundaries")
          :active-boundary active-boundary})])
     
     (when (and state-of-knowledge-open? (= (:id active-boundary) "amp"))
       [floating-zones-pill
        (merge
         valid-boundaries
         {:expanded? (= open-pill "zones")})])]))

(defn layers-search-omnibar []
  (let [categories @(re-frame/subscribe [:map/categories-map])
        open?      @(re-frame/subscribe [:layers-search-omnibar/open?])
        {:keys [sorted-layers]} @(re-frame/subscribe [:map/layers])]
    [components/omnibar
     {:placeholder  "Search Layers..."
      :isOpen       open?
      :onClose      #(re-frame/dispatch [:layers-search-omnibar/close])
      :items        sorted-layers
      :onItemSelect #(re-frame/dispatch [:map/add-layer-from-omnibar %])
      :keyfns
      {:id          :id
       :text        :name
       :breadcrumbs (fn [{:keys [category data_classification]}]
                      (map
                       #(or % "Ungrouped")
                       [(or
                         (:display_name (category categories))
                         (:name (category categories)))
                        data_classification]))
       :keywords    #(layer-search-keywords categories %)}}]))

(defn left-drawer-catalogue []
  (let [{:keys [filtered-layers active-layers visible-layers viewport-layers loading-layers error-layers expanded-layers layer-opacities main-national-layer]} @(re-frame/subscribe [:map/layers])
        viewport-only? @(re-frame/subscribe [:map/viewport-only?])
        catalogue-layers (filterv #(or (not viewport-only?) ((set viewport-layers) %)) filtered-layers)]
    [:<>
     [layer-search-filter]
     [layer-catalogue catalogue-layers
      {:active-layers  active-layers
       :visible-layers visible-layers
       :main-national-layer main-national-layer
       :loading-fn     loading-layers
       :error-fn       error-layers
       :expanded-fn    expanded-layers
       :opacity-fn     layer-opacities}]]))

(defn left-drawer-active-layers []
  (let [{:keys [active-layers visible-layers loading-layers error-layers expanded-layers layer-opacities main-national-layer]} @(re-frame/subscribe [:map/layers])]
    [active-layer-selection-list
     {:layers         active-layers
      :visible-layers visible-layers
      :main-national-layer main-national-layer
      :loading-fn     loading-layers
      :error-fn       error-layers
      :expanded-fn    expanded-layers
      :opacity-fn     layer-opacities}]))

(defn- left-drawer []
  (let [open? @(re-frame/subscribe [:left-drawer/open?])
        tab   @(re-frame/subscribe [:left-drawer/tab])
        {:keys [active-layers]} @(re-frame/subscribe [:map/layers])]
    [components/drawer
     {:title
      [:<>
       [:div
        [:a {:href "https://seamapaustralia.org/"}
         [:img {:src "img/Seamap2_V2_RGB.png"}]]]
       [b/button
        {:icon     "double-chevron-left"
         :minimal  true
         :on-click #(re-frame/dispatch [:left-drawer/close])}]]
      :position    "left"
      :size        "368px"
      :isOpen      open?
      :onClose     #(re-frame/dispatch [:left-drawer/close])
      :className   "left-drawer"
      :isCloseButtonShown false
      :hasBackdrop false}
     [b/tabs
      {:id              "left-drawer-tabs"
       :class           "left-drawer-tabs"
       :selected-tab-id tab
       :on-change       #(re-frame/dispatch [:left-drawer/tab %1])}

      [b/tab
       {:id    "catalogue"
        :class "catalogue"
        :title "Catalogue"
        :panel (reagent/as-element [left-drawer-catalogue])}]

      [b/tab
       {:id    "active-layers"
        :title (reagent/as-element
                [:<> "Active Layers"
                 (when (seq active-layers)
                   [:div.notification-bubble (count active-layers)])])
        :panel (reagent/as-element [left-drawer-active-layers])}]

      [b/tab
       {:id    "featured-maps"
        :title "Featured Maps"
        :panel (reagent/as-element [featured-maps])}]]]))

(defn layer-preview [_preview-layer-url]
  (let [previous-url (reagent/atom nil) ; keeps track of previous url for the purposes of tracking its changes
        error? (reagent/atom false)]    ; keeps track of if previous url had an error in displaying
    (fn [preview-layer-url]
      (when (not= preview-layer-url @previous-url) ; if the preview layer has changed, then:
        (reset! error? false)                      ; - reset errors we're keeping track of (we need to check if the new image has errors in displaying)
        (reset! previous-url preview-layer-url))   ; - set the "previous url" we're keeping track of to the new url
      (when preview-layer-url
        [:div.layer-preview
         (if @error?
           [:div "No layer preview available"]
           [:img
            {:src preview-layer-url
             :onError #(reset! error? true)}])])))) ; if there's an error in displaying the image, then we keep track of it so we can instead display an error message

(def hotkeys-combos
  (let [keydown-wrapper
        (fn [m keydown-v]
          (assoc m :global    true
                 :group "Keyboard Shortcuts"
                 :onKeyDown #(re-frame/dispatch keydown-v)))]
    ;; See note on `use-hotkeys' for rationale invoking `clj->js' here:
    (clj->js
     [(keydown-wrapper
       {:label "Zoom In"                :combo "plus"}
       [:map/zoom-in])
      (keydown-wrapper
       {:label "Zoom Out"               :combo "-"}
       [:map/zoom-out])
      (keydown-wrapper
       {:label "Pan Up"                 :combo "up"}
       [:map/pan-direction :up])
      (keydown-wrapper
       {:label "Pan Down"               :combo "down"}
       [:map/pan-direction :down])
      (keydown-wrapper
       {:label "Pan Left"               :combo "left"}
       [:map/pan-direction :left])
      (keydown-wrapper
       {:label "Pan Right"              :combo "right"}
       [:map/pan-direction :right])
      (keydown-wrapper
       {:label "Toggle Transect Panel"  :combo "p"}
       [:transect.plot/toggle-visibility])
      (keydown-wrapper
       {:label "Toggle Left Drawer"     :combo "a"}
       [:left-drawer/toggle])
      (keydown-wrapper
       {:label "Start/Clear Transect"   :combo "t"}
       [:transect.draw/toggle])
      (keydown-wrapper
       {:label "Start/Clear Region Select" :combo "r"}
       [:map.layer.selection/toggle])
      (keydown-wrapper
       {:label "Cancel"                 :combo "esc"}
       [:ui.drawing/cancel])
      (keydown-wrapper
       {:label "Layer Power Search"     :combo "s"}
       [:layers-search-omnibar/toggle])
      (keydown-wrapper
       {:label "Reset"                  :combo "shift + r"}
       [:re-boot])
      (keydown-wrapper
       {:label "Create Shareable URL"   :combo "c"}
       [:create-save-state])
      (keydown-wrapper
       {:label "Show Help Overlay"      :combo "h"}
       [:help-layer/toggle])])))

(defn layout-app []
  (let [hot-keys (use-memo (fn [] hotkeys-combos))
        ;; We don't need the results of this, just need to ensure it's called!
        _ #_{:keys [handle-keydown handle-keyup]} (use-hotkeys hot-keys)
        catalogue-open?    @(re-frame/subscribe [:left-drawer/open?])
        right-drawer-open? (or @(re-frame/subscribe [:sok/open?]) @(re-frame/subscribe [:sm.featured-map/open?]))]
    [:div#main-wrapper ;{:on-key-down handle-keydown :on-key-up handle-keyup}
     {:class (str (when catalogue-open? " catalogue-open") (when right-drawer-open? " right-drawer-open"))}
     [:div#content-wrapper
      [map-component]
      [plot-component]]
     
     ;; TODO: Update helper-overlay for new Seamap version (or remove?)
     [helper-overlay
      {:selector       ".layer-card:first-child .layer-header"
       :helperPosition "bottom"
       :helperText     "Toggle layer visibility, view more info, show legend, and download data"}
      {:id "layer-search" :helperText "Search for a specific layer using its name or keywords"}
      {:id "transect-control" :helperText "Click to draw a transect"}
      {:id "select-control" :helperText "Click to select a region"}
      {:id "plot-footer"
       :helperText "This shows the habitat data along a bathymetry transect you can draw"
       :helperPosition "top"}]
     [welcome-dialogue]
     [settings-overlay]
     [info-card]
     [loading-display]
     [left-drawer]
     [state-of-knowledge]
     [featured-map-drawer]
     [layers-search-omnibar]
     [pre-leaflet-controls
      [menu-button]
      [settings-button]]
     [floating-pills]
     [layer-preview @(re-frame/subscribe [:ui/preview-layer-url])]]))

