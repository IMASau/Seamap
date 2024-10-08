;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.state-of-knowledge.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [goog.string :as gstring]
            [imas-seamap.blueprint :as b]
            [imas-seamap.components :as components]
            [imas-seamap.utils :refer [format-number format-date-month]]))

(defn habitat-statistics-table
  [{:keys [habitat-statistics]}]
  [:table
   [:thead
    [:tr
     [:th "Habitat"]
     [:th "Area (km²)"]
     [:th [b/tooltip {:content "% of coverage (relative to surveyed area)"} "Mapped (%)"]]
     [:th [b/tooltip {:content "% of coverage (relative to total region area)"} "Total (%)"]]]]
   [:tbody
    (for [{:keys [habitat area mapped_percentage total_percentage]} habitat-statistics]
      [:tr
       {:key (or habitat "Total Mapped")}
       [:td (or habitat "Total Mapped")]
       [:td (format-number area)]
       [:td (or (format-number mapped_percentage) "N/A")]
       [:td (format-number total_percentage)]])]])

(defn data-sources-tooltip
  "This component is a tooltip that displays the data providers for the habitat
   statistics, bathymetry statistics, and the different habitat observations.
   
   Arguments:
   * `data-providers`: A rich-text string containing information on the data
     providers being thanked."
  [{:keys [data-providers]}]
  [:div {:className "data-sources"}
   [b/tooltip
    {:interactionKind    "hover"
     ::hover-close-delay 500
     :content
     (reagent/as-element
      [:div
       {:className "data-sources-tooltip"
        :ref
        #(when %
           (set! (.-innerHTML %) data-providers)
           (let [hyperlinks (js/Array.prototype.slice.call (.getElementsByTagName % "a"))]
             (doseq [hyperlink hyperlinks]
               (when-not (.getAttribute hyperlink "target")
                 (.setAttribute hyperlink "target" "_blank")))))}
       data-providers])}
    [:<>
     "Data Sources"
     [b/icon {:icon "info-sign" :size 12}]]]])

(defn habitat-statistics
  []
  (let [selected-tab (reagent/atom "breakdown")
        collapsed?   (reagent/atom false)]
    (fn []
      (let [{:keys [loading? results show-layers?]} @(re-frame/subscribe [:sok/habitat-statistics])
            download-url @(re-frame/subscribe [:sok/habitat-statistics-download-url])
            without-unmapped   (filter :habitat results)
            data-providers (:habitat-statistics-data-provider @(re-frame/subscribe [:site-configuration/data-providers]))]
        [components/drawer-group
         {:heading         "Habitat Statistics"
          :collapsed?      @collapsed?
          :toggle-collapse #(swap! collapsed? not)
          :class           "habitat-statistics"}
         [:<>
          [b/tabs
           {:id              "habitat-statistics-tabs"
            :selected-tab-id @selected-tab
            :on-change       #(reset! selected-tab %)}

           [b/tab
            {:id    "breakdown"
             :title "Breakdown"
             :panel
             (reagent/as-element
              (cond
                loading?
                [b/spinner]

                (empty? without-unmapped)
                [b/non-ideal-state
                 {:title       "No Data"
                  :description "No habitat data is available for this region."
                  :icon        "info-sign"}]

                :else
                [habitat-statistics-table
                 {:habitat-statistics results}]))}]

           [b/tab
            {:id    "chart"
             :title "Chart"
             :panel
             (when (= "chart" @selected-tab) ; Hack(?) to only render the donut chart when the tab is selected, so that vega updates chart correctly
               (reagent/as-element
                (cond
                  loading?
                  [b/spinner]

                  (empty? without-unmapped)
                  [b/non-ideal-state
                   {:title       "No Data"
                    :description "No habitat data is available for this region."
                    :icon        "info-sign"}]

                  :else
                  [components/donut-chart
                   {:id              "habitat-statistics-chart"
                    :values          without-unmapped
                    :independent-var :habitat
                    :dependent-var   :area
                    :color           :color
                    :legend-title    "Habitat"}])))}]

           [b/tab
            {:id    "download"
             :title "Download"
             :panel
             (reagent/as-element
              (cond
                loading?
                [b/spinner]

                (empty? without-unmapped)
                [b/non-ideal-state
                 {:title       "No Data"
                  :description "No habitat data is available for this region."
                  :icon        "info-sign"}]

                :else
                [:a.download
                 {:href download-url}
                 "Download as Shapefile"]))}]

           [b/switch
            {:checked   show-layers?
             :on-change #(re-frame/dispatch [:sok/habitat-toggle-show-layers])
             :label     "Layers"}]]
          [data-sources-tooltip {:data-providers data-providers}]]]))))

(defn bathymetry-statistics-table
  [{:keys [bathymetry-statistics]}]
  [:table
   [:thead
    [:tr
     [:th "Resolution"]
     [:th "Area (km²)"]
     [:th [b/tooltip {:content "% of coverage (relative to surveyed area)"} "Mapped (%)"]]
     [:th [b/tooltip {:content "% of coverage (relative to total region area)"} "Total (%)"]]]]
   [:tbody
    (for [{:keys [resolution area mapped_percentage total_percentage]} bathymetry-statistics]
      [:tr
       {:key (or resolution "Total Mapped")}
       [:td (or resolution "Total Mapped")]
       [:td (format-number area)]
       [:td (or (format-number mapped_percentage) "N/A")]
       [:td (format-number total_percentage)]])]])

(defn bathymetry-statistics
  []
  (let [selected-tab (reagent/atom "breakdown")
        collapsed?   (reagent/atom false)]
    (fn []
      (let [{:keys [loading? results show-layers?]} @(re-frame/subscribe [:sok/bathymetry-statistics])
            download-url @(re-frame/subscribe [:sok/bathymetry-statistics-download-url])
            without-unmapped      (filter :resolution results)
            data-providers (:bathymetry-statistics-data-provider @(re-frame/subscribe [:site-configuration/data-providers]))]
        [components/drawer-group
         {:heading         "Bathymetry Statistics"
          :collapsed?      @collapsed?
          :toggle-collapse #(swap! collapsed? not)
          :class           "bathymetry-statistics"}
         [:<>
          [b/tabs
           {:id              "bathymetry-statistics-tabs"
            :selected-tab-id @selected-tab
            :on-change       #(reset! selected-tab %)}

           [b/tab
            {:id    "breakdown"
             :title "Breakdown"
             :panel (reagent/as-element
                     (cond
                       loading?
                       [b/spinner]

                       (empty? without-unmapped)
                       [b/non-ideal-state
                        {:title       "No Data"
                         :description "No bathymetry data is available for this region."
                         :icon        "info-sign"}]

                       :else
                       [bathymetry-statistics-table
                        {:bathymetry-statistics results}]))}]

           [b/tab
            {:id    "chart"
             :title "Chart"
             :panel
             (when (= "chart" @selected-tab) ; Hack(?) to only render the donut chart when the tab is selected, so that vega updates chart correctly
               (reagent/as-element
                (cond
                  loading?
                  [b/spinner]

                  (empty? without-unmapped)
                  [b/non-ideal-state
                   {:title       "No Data"
                    :description "No bathymetry data is available for this region."
                    :icon        "info-sign"}]

                  :else
                  [components/donut-chart
                   {:id              "bathymetry-statistics-chart"
                    :values          without-unmapped
                    :independent-var :resolution
                    :dependent-var   :area
                    :color           :color
                    :legend-title    "Resolution"
                    :sort-key        :rank}])))}]

           [b/tab
            {:id    "download"
             :title "Download"
             :panel
             (reagent/as-element
              (cond
                loading?
                [b/spinner]

                (empty? without-unmapped)
                [b/non-ideal-state
                 {:title       "No Data"
                  :description "No bathymetry data is available for this region."
                  :icon        "info-sign"}]

                :else
                [:a.download
                 {:href download-url}
                 "Download as Shapefile"]))}]

           [b/switch
            {:checked   show-layers?
             :on-change #(re-frame/dispatch [:sok/bathymetry-toggle-show-layers])
             :label     "Layers"}]]
          [data-sources-tooltip {:data-providers data-providers}]]]))))

(defn- habitat-observations-group-stat
  [{:keys [label text] :as _stat}]
  [:div.habitat-observations-group-stat
   [:b (str label ": ")]
   (gstring/unescapeEntities text)])

(defn- habitat-observations-group
  [_props]
  (let [expanded? (reagent/atom false)]
    (fn [{:keys [title disabled? stats data-provider]}]
      (let [data-providers (data-provider @(re-frame/subscribe [:site-configuration/data-providers]))]
        [:div.habitat-observations-group
         {:class
          (str
           (when (and @expanded? (not disabled?)) " expanded")
           (when disabled? " disabled"))}
         [:div.habitat-observations-group-heading
          {:on-click #(when-not disabled? (swap! expanded? not))}
          [b/icon
           {:icon (if (and @expanded? (not disabled?)) "double-chevron-up" "double-chevron-down")
            :icon-size 20}]
          title]
         [b/collapse
          {:is-open               (and @expanded? (not disabled?))
           :keep-children-mounted true}
          [:<>
           [:div.habitat-observations-group-stats
            (for [{:keys [label] :as stat} stats]
              ^{:key label}
              [habitat-observations-group-stat stat])]
           [data-sources-tooltip {:data-providers data-providers}]]]]))))

(defn- squidle-stats
  [{:keys [deployments campaigns start_date end_date method images total_annotations public_annotations]}]
  (let [disabled?          (not (pos? (or deployments 0)))
        deployments        (format-number (or deployments 0) 0)
        campaigns          (format-number (or campaigns 0) 0)
        start_date         (format-date-month start_date)
        end_date           (format-date-month end_date)
        method             (or method "N/A")
        images             (format-number (or images 0) 0)
        total_annotations  (format-number (or total_annotations 0) 0)
        public_annotations (format-number (or public_annotations 0) 0)]
    [habitat-observations-group
     {:title     (str deployments " Imagery Deployments (" campaigns " Campaigns)")
      :disabled? disabled?
      :stats
      [{:label "Date Range" :text (if start_date (str start_date " to " end_date) "Unknown")}
       {:label "Methods of Collection" :text method}
       {:label "Images Collected" :text images}
       {:label "Image Annotations" :text [:<> total_annotations " (" public_annotations " " [b/tooltip {:content "Finalised public curated image annotations"} [:b [:u "public"]]] ")"]}]
      :data-provider :habitat-observations-imagery-data-provider}]))

(defn- global-archive-stats
  [{:keys [deployments campaigns start_date end_date method video_time video_annotations]}]
  (let [disabled?         (not (pos? (or deployments 0)))
        deployments       (format-number (or deployments 0) 0)
        campaigns         (format-number (or campaigns 0) 0)
        start_date        (format-date-month start_date)
        end_date          (format-date-month end_date)
        method            (or method "N/A")
        video_time        (format-number (or video_time 0) 0)
        video_annotations (format-number (or video_annotations 0) 0)]
    [habitat-observations-group
     {:title     (str deployments " Video Deployments (" campaigns " Campaigns)")
      :disabled? disabled?
      :stats
      [{:label "Date Range" :text (if start_date (str start_date " to " end_date) "Unknown")}
       {:label "Methods of Collection" :text method}
       {:label "Hours of Video" :text video_time}
       {:label "Video Annotations" :text [:<> video_annotations " " [b/tooltip {:content "Publicly available video annotations"} [:b [:u "public"]]]]}]
      :data-provider :habitat-observations-video-data-provider}]))

(defn- sediment-stats
  [{:keys [samples analysed survey start_date end_date method]}]
  (let [disabled?  (not (pos? (or samples 0)))
        samples    (format-number (or samples 0) 0)
        analysed   (format-number (or analysed 0) 0)
        survey     (format-number (or survey 0) 0)
        start_date (format-date-month start_date)
        end_date   (format-date-month end_date)
        method     (or method "N/A")]
    [habitat-observations-group
     {:title
      [:<>
       (str samples " Sediment Samples (" survey " Surveys)")]
      :disabled? disabled?
      :stats
      [{:label "Date Range" :text (if start_date (str start_date " to " end_date) "Unknown")}
       {:label "Methods of Collection" :text method}
       {:label "Samples Analysed" :text analysed}]
      :data-provider :habitat-observations-sediment-data-provider}]))

(defn- habitat-observations []
  (let [collapsed?   (reagent/atom false)]
    (fn []
      (let [{:keys [squidle global-archive sediment loading? show-layers?]} @(re-frame/subscribe [:sok/habitat-observations])]
        [components/drawer-group
         {:heading         "Habitat Observations"
          :collapsed?      @collapsed?
          :toggle-collapse #(swap! collapsed? not)
          :class           "habitat-observations"}
         [b/switch
          {:checked   show-layers?
           :on-change #(re-frame/dispatch [:sok/habitat-observations-toggle-show-layers])
           :label     "Layers"}]
         (if loading?
           [b/spinner]
           [:div.habitat-observations-groups
            [squidle-stats squidle]
            [global-archive-stats global-archive]
            [sediment-stats sediment]])]))))

(defn selected-boundaries []
  (let [active-boundary @(re-frame/subscribe [:sok/active-boundary])
        {:keys [active-network active-park active-zone active-zone-iucn active-zone-id
                active-provincial-bioregion active-mesoscale-bioregion
                active-realm active-province active-ecoregion]} @(re-frame/subscribe [:sok/valid-boundaries])
        breadcrumb-data (case (:id active-boundary)
                          "amp"   (cond-> []
                                    active-network
                                    (conj
                                     {:name   (:network active-network)
                                      :action #(re-frame/dispatch [:sok/update-active-network active-network true])})

                                    active-park
                                    (conj
                                     {:name   (:park active-park)
                                      :action #(re-frame/dispatch [:sok/update-active-park active-park true])})

                                    active-zone
                                    (conj
                                     {:name   (:zone active-zone)
                                      :action #(re-frame/dispatch [:sok/update-active-zone active-zone true])})
                                    
                                    active-zone-iucn
                                    (conj
                                     {:name   (:zone-iucn active-zone-iucn)
                                      :action #(re-frame/dispatch [:sok/update-active-zone-iucn active-zone-iucn true])})
                                    
                                    active-zone-id
                                    (conj
                                     {:name   (:zone-id active-zone-id)
                                      :action #(re-frame/dispatch [:sok/update-active-zone-id active-zone-id true])}))
                          "imcra" (cond-> []
                                    active-provincial-bioregion
                                    (conj
                                     {:name   (conj (:provincial-bioregion active-provincial-bioregion))
                                      :action #(re-frame/dispatch [:sok/update-active-provincial-bioregion active-provincial-bioregion true])})

                                    active-mesoscale-bioregion
                                    (conj
                                     {:name   (:mesoscale-bioregion active-mesoscale-bioregion)
                                      :action #(re-frame/dispatch [:sok/update-active-mesoscale-bioregion active-mesoscale-bioregion true])}))
                          "meow"  (cond-> []
                                    active-realm
                                    (conj
                                     {:name   (:realm active-realm)
                                      :action #(re-frame/dispatch [:sok/update-active-realm active-realm true])})

                                    active-province
                                    (conj
                                     {:name   (:province active-province)
                                      :action #(re-frame/dispatch [:sok/update-active-province active-province true])})

                                    active-ecoregion
                                    (conj
                                     {:name   (:ecoregion active-ecoregion)
                                      :action #(re-frame/dispatch [:sok/update-active-ecoregion active-ecoregion true])}))
                          nil)

        breadcrumbs (conj
                     (mapv
                      (fn [{:keys [name action]}]
                        [:a
                         {:href     "#!"
                          :on-click action}
                         name])
                      (butlast breadcrumb-data))
                     (:name (last breadcrumb-data)))
        region-report-url @(re-frame/subscribe [:sok/region-report-url])]
    [:div.selected-boundaries
     [:div [:h2 (:name active-boundary)]
      (when (seq breadcrumbs)
        [components/breadcrumbs
         {:content breadcrumbs}])]
     (when region-report-url
       [b/tooltip
        {:content "View full data report for region"}
        [:a {:href   region-report-url
             :target "_blank"
             :class  "region-report-button"}
         [b/icon
          {:icon "document-open"
           :size 24}]]])]))

(defn state-of-knowledge []
  [components/drawer
   {:title       "State of Knowledge"
    :position    "right"
    :size        "368px"
    :isOpen      true
    :onClose     #(re-frame/dispatch [:sok/close])
    :hasBackdrop false
    :className   "state-of-knowledge-drawer"}
   [selected-boundaries]
   [habitat-statistics]
   [bathymetry-statistics]
   [habitat-observations]])

(defn floating-state-of-knowledge-pill
  [{:keys [expanded? boundaries active-boundary]}]
  [components/floating-pill-control-menu
   (merge
    {:text           (or (:name active-boundary) "State of Knowledge")
     :id             "state-of-knowledge-pill"
     :icon           "add-column-right"
     :expanded?      expanded?
     :active?        (boolean active-boundary)
     :tooltip        (when-not active-boundary "State of research knowledge for management region")
     :on-open-click  #(re-frame/dispatch [:ui/open-pill "state-of-knowledge"])
     :on-close-click #(re-frame/dispatch [:ui/open-pill nil])}
    (when active-boundary {:reset-click #(re-frame/dispatch [:sok/update-active-boundary nil])}))
   [:div.state-of-knowledge-pill-content
    
    [components/form-group
     {:label "Management Region"}
     [components/select
      {:value        active-boundary
       :options      boundaries
       :onChange     #(re-frame/dispatch [:sok/update-active-boundary %])
       :isSearchable true
       :isClearable  true
       :keyfns
       {:id   :id
        :text :name}}]]]])

(defn floating-boundaries-pill
  [{:keys
    [expanded? networks parks provincial-bioregions mesoscale-bioregions realms
     provinces ecoregions active-network active-park active-provincial-bioregion
     active-mesoscale-bioregion active-realm active-province active-ecoregion
     active-boundary]}]
  (let [amp?   (= (:id active-boundary) "amp")
        imcra? (= (:id active-boundary) "imcra")
        meow?  (= (:id active-boundary) "meow")
        active-boundaries? @(re-frame/subscribe [:sok/active-boundaries?])
        text (case (:id active-boundary)
               "amp" (str
                      (or (:network active-network) "All networks")
                      " / "
                      (or (:park active-park) "All marine parks"))
               "imcra" (str
                        (or (:provincial-bioregion active-provincial-bioregion) "All provincial bioregions")
                        " / "
                        (or (:mesoscale-bioregion active-mesoscale-bioregion) "All mesoscale bioregions"))
               "meow" (str
                       (or (:realm active-realm) "All realms")
                       " / "
                       (or (:province active-province) "All provinces")
                       " / "
                       (or (:ecoregion active-ecoregion) "All ecoregions")))]
    [components/floating-pill-control-menu
     (merge
      {:text           text
       :icon           "heatmap"
       :expanded?      expanded?
       :active?        active-boundaries?
       :on-open-click  #(re-frame/dispatch [:ui/open-pill "boundaries"])
       :on-close-click #(re-frame/dispatch [:ui/open-pill nil])}
      (when active-boundaries? {:reset-click #(re-frame/dispatch [:sok/reset-active-boundaries])}))
     [:div.state-of-knowledge-pill-content

      (when amp?
        [components/form-group
         {:label "Network"}
         [components/select
          {:value        active-network
           :options      networks
           :onChange     #(re-frame/dispatch [:sok/update-active-network %])
           :isSearchable true
           :isClearable  true
           :keyfns
           {:id   :network
            :text :network}}]])

      (when amp?
        [components/form-group
         {:label "Marine Park"}
         [components/select
          {:value        active-park
           :options      parks
           :onChange     #(re-frame/dispatch [:sok/update-active-park %])
           :isSearchable true
           :isClearable  true
           :keyfns
           {:id          :park
            :text        :park
            :breadcrumbs (comp vector :network)}}]])

      (when imcra?
        [components/form-group
         {:label "Provincial Bioregion"}
         [components/select
          {:value        active-provincial-bioregion
           :options      provincial-bioregions
           :onChange     #(re-frame/dispatch [:sok/update-active-provincial-bioregion %])
           :isSearchable true
           :isClearable  true
           :keyfns
           {:id   :provincial-bioregion
            :text :provincial-bioregion}}]])

      (when imcra?
        [components/form-group
         {:label "Mesoscale Bioregion"}
         [components/select
          {:value        active-mesoscale-bioregion
           :options      mesoscale-bioregions
           :onChange     #(re-frame/dispatch [:sok/update-active-mesoscale-bioregion %])
           :isSearchable true
           :isClearable  true
           :keyfns
           {:id          :mesoscale-bioregion
            :text        :mesoscale-bioregion
            :breadcrumbs (comp vector :provincial-bioregion)}}]])

      (when meow?
        [components/form-group
         {:label "Realms"}
         [components/select
          {:value        active-realm
           :options      realms
           :onChange     #(re-frame/dispatch [:sok/update-active-realm %])
           :isSearchable true
           :isClearable  true
           :keyfns
           {:id   :realm
            :text :realm}}]])

      (when meow?
        [components/form-group
         {:label "Provinces"}
         [components/select
          {:value        active-province
           :options      provinces
           :onChange     #(re-frame/dispatch [:sok/update-active-province %])
           :isSearchable true
           :isClearable  true
           :keyfns
           {:id          :province
            :text        :province
            :breadcrumbs (comp vector :realm)}}]])

      (when meow?
        [components/form-group
         {:label "Ecoregions"}
         [components/select
          {:value        active-ecoregion
           :options      ecoregions
           :onChange     #(re-frame/dispatch [:sok/update-active-ecoregion %])
           :isSearchable true
           :isClearable  true
           :keyfns
           {:id          :ecoregion
            :text        :ecoregion
            :breadcrumbs (fn [{:keys [realm province]}] [realm province])}}]])]]))

(defn floating-zones-pill
  [{:keys [expanded? zones zones-iucn zone-ids active-zone active-zone-iucn active-zone-id]}]
  (let [active-zones? @(re-frame/subscribe [:sok/active-zones?])
        text (or (:zone-id active-zone-id) (:zone active-zone) (:zone-iucn active-zone-iucn) "All zones")]
    [components/floating-pill-control-menu
     (merge
      {:text           text
       :icon           "polygon-filter"
       :expanded?      expanded?
       :active?        active-zones?
       :on-open-click  #(re-frame/dispatch [:ui/open-pill "zones"])
       :on-close-click #(re-frame/dispatch [:ui/open-pill nil])}
      (when active-zones? {:reset-click #(re-frame/dispatch [:sok/reset-active-zones])}))
     [:div.state-of-knowledge-pill-content

      [components/form-group
       {:label "Zone Category"}
       [components/select
        {:value        active-zone
         :options      zones
         :onChange     #(re-frame/dispatch [:sok/update-active-zone %])
         :isSearchable true
         :isClearable  true
         :isDisabled   (boolean (or active-zone-iucn active-zone-id))
         :keyfns
         {:id   :zone
          :text :zone}}]]

      [components/form-group
       {:label "IUCN Category"}
       [components/select
        {:value        active-zone-iucn
         :options      zones-iucn
         :onChange     #(re-frame/dispatch [:sok/update-active-zone-iucn %])
         :isSearchable true
         :isClearable  true
         :isDisabled   (boolean (or active-zone active-zone-id))
         :keyfns
         {:id   :zone-iucn
          :text :zone-iucn}}]]
      
      [components/form-group
       {:label "Zone ID"}
       [components/select
        {:value        active-zone-id
         :options      zone-ids
         :onChange     #(re-frame/dispatch [:sok/update-active-zone-id %])
         :isSearchable true
         :isClearable  true
         :isDisabled   false
         :keyfns
         {:id   :zone-id
          :text :zone-id
          :breadcrumbs (fn [{:keys [network park]}] [network park])}}]]]]))
