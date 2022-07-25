;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.state-of-knowledge.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [goog.string :as gstring]
            [imas-seamap.blueprint :as b]
            [imas-seamap.components :as components]))

(defn habitat-statistics-table
  [{:keys [habitat-statistics]}]
  [:table
   [:thead
    [:tr
     [:th "Habitat"]
     [:th "Area (km²)"]
     [:th "Mapped (%)"]
     [:th "Total (%)"]]]
   [:tbody
    (if (seq habitat-statistics)
      (for [{:keys [habitat area mapped_percentage total_percentage]} habitat-statistics]
        [:tr
         {:key (or habitat "Total Mapped")}
         [:td (or habitat "Total Mapped")]
         [:td (gstring/format "%.1f" area)]
         [:td (if mapped_percentage (gstring/format "%.1f" mapped_percentage) "N/A")]
         [:td (gstring/format "%.1f" total_percentage)]])
      [:tr
       [:td
        {:colSpan 4}
        "No habitat information"]])]])

(defn habitat-statistics
  []
  (let [selected-tab (reagent/atom "breakdown")
        collapsed?   (reagent/atom false)]
    (fn []
      (let [{:keys [loading? results]} @(re-frame/subscribe [:sok/habitat-statistics])
            download-url @(re-frame/subscribe [:sok/habitat-statistics-download-url])
            without-unmapped   (filter :habitat results)]
        [components/drawer-group
         {:heading     "Habitat Statistics"
          :icon        "home"
          :collapsed?  @collapsed?
          :toggle-collapse #(swap! collapsed? not)}
         (if loading?
           [b/spinner]
           [b/tabs
            {:id              "habitat-statistics-tabs"
             :selected-tab-id @selected-tab
             :on-change       #(reset! selected-tab %)}

            [b/tab
             {:id    "breakdown"
              :title "Breakdown"
              :panel
              (reagent/as-element
               [habitat-statistics-table
                {:habitat-statistics results}])}]

            [b/tab
             {:id    "chart"
              :title "Chart"
              :panel
              (when (= "chart" @selected-tab) ; Hack(?) to only render the donut chart when the tab is selected, so that vega updates chart correctly
                (reagent/as-element
                 (if (seq without-unmapped)
                   [components/donut-chart
                    {:id              "habitat-statistics-chart"
                     :values          without-unmapped
                     :independent-var :habitat
                     :dependent-var   :area
                     :color           :color
                     :legend-title    "Habitat"}]
                   [:div "No habitat information"])))}]

            [b/tab
             {:id    "download"
              :title "Download"
              :panel
              (reagent/as-element
               (if (seq without-unmapped)
                 [:a.download
                  {:href download-url}
                  "Download as Shapefile"]
                 [:div "No habitat information"]))}]])]))))

(defn bathymetry-statistics-table
  [{:keys [bathymetry-statistics]}]
  [:table
   [:thead
    [:tr
     [:th "Resolution"]
     [:th "Area (km²)"]
     [:th "Mapped (%)"]
     [:th "Total (%)"]]]
   [:tbody
    (if (seq bathymetry-statistics)
      (for [{:keys [resolution area mapped_percentage total_percentage]} bathymetry-statistics]
        [:tr
         {:key (or resolution "Total Mapped")}
         [:td (or resolution "Total Mapped")]
         [:td (gstring/format "%.1f" area)]
         [:td (if mapped_percentage (gstring/format "%.1f" mapped_percentage) "N/A")]
         [:td (gstring/format "%.1f" total_percentage)]])
      [:tr
       [:td
        {:colSpan 4}
        "No bathymetry information"]])]])

(defn bathymetry-statistics
  []
  (let [selected-tab (reagent/atom "breakdown")
        collapsed?   (reagent/atom false)]
    (fn []
      (let [{:keys [loading? results]} @(re-frame/subscribe [:sok/bathymetry-statistics])
            download-url @(re-frame/subscribe [:sok/bathymetry-statistics-download-url])
            without-unmapped      (filter :resolution results)]
        [components/drawer-group
         {:heading         "Bathymetry Statistics"
          :icon            "timeline-area-chart"
          :collapsed?      @collapsed?
          :toggle-collapse #(swap! collapsed? not)}
         (if loading?
           [b/spinner]
           [b/tabs
            {:id              "bathymetry-statistics-tabs"
             :selected-tab-id @selected-tab
             :on-change       #(reset! selected-tab %)}

            [b/tab
             {:id    "breakdown"
              :title "Breakdown"
              :panel (reagent/as-element
                      [bathymetry-statistics-table
                       {:bathymetry-statistics results}])}]

            [b/tab
             {:id    "chart"
              :title "Chart"
              :panel
              (when (= "chart" @selected-tab) ; Hack(?) to only render the donut chart when the tab is selected, so that vega updates chart correctly
                (reagent/as-element
                 (if (seq without-unmapped)
                   [components/donut-chart
                    {:id              "bathymetry-statistics-chart"
                     :values          without-unmapped
                     :independent-var :resolution
                     :dependent-var   :area
                     :color           :color
                     :legend-title    "Resolution"
                     :sort-key        :rank}]
                   [:div "No bathymetry information"])))}]

            [b/tab
             {:id    "download"
              :title "Download"
              :panel
              (reagent/as-element
               (if (seq without-unmapped)
                 [:a.download
                  {:href download-url}
                  "Download as Shapefile"]
                 [:div "No bathymetry information"]))}]])]))))

(defn squidle-stats
  [{:keys [deployment_id campaign_name start_date end_date method images total_annotations public_annotations]}]
  (let [collapsed? (reagent/atom true)]
    (fn [{:keys [deployment_id campaign_name start_date end_date method images total_annotations public_annotations]}]
      (let [deployment_id      (or deployment_id 0)
            campaign_name      (or campaign_name 0)
            start_date         (or start_date "unknown")
            end_date           (or end_date "unknown")
            method             (or method "N/A")
            images             (or images 0)
            total_annotations  (or total_annotations 0)
            public_annotations (or public_annotations 0)]
        [:div
         {:class (str "habitat-observation-stats" (when @collapsed? " collapsed") (when-not (pos? deployment_id) " disabled"))}
         [:h2
          {:class (str "bp3-heading" (if (or @collapsed? (not (pos? deployment_id))) " bp3-icon-caret-right" " bp3-icon-caret-down"))
           :on-click #(swap! collapsed? not)}
          (str deployment_id " imagery deployments (" campaign_name " campaigns)")]
         [:ul
          [:li (str "Date range: " start_date " to " end_date)]
          [:li (str "Methods of collection: " method)]
          [:li (str images " images collected")]
          [:li (str total_annotations " image annotations (" public_annotations " public)")]]]))))

(defn global-archive-stats
  [{:keys [deployment_id campaign_name start_date end_date method video_time]}]
  (let [collapsed? (reagent/atom true)]
    (fn [{:keys [deployment_id campaign_name start_date end_date method video_time]}]
      (let [deployment_id (or deployment_id 0)
            campaign_name (or campaign_name 0)
            start_date    (or start_date "unknown")
            end_date      (or end_date "unknown")
            method        (or method "N/A")
            video_time    (or video_time 0)]
        [:div
         {:class (str "habitat-observation-stats" (when @collapsed? " collapsed") (when-not (pos? deployment_id) " disabled"))}
         [:h2
          {:class (str "bp3-heading" (if (or @collapsed? (not (pos? deployment_id))) " bp3-icon-caret-right" " bp3-icon-caret-down"))
           :on-click #(swap! collapsed? not)}
          (str deployment_id " video deployments (" campaign_name " campaigns)")]
         [:ul
          [:li (str "Date range: " start_date " to " end_date)]
          [:li (str "Methods of collection: " method)]
          [:li (str video_time " hours of video")]]]))))

(defn sediment-stats
  [{:keys [sample_id analysed survey start_date end_date method]}]
  (let [collapsed? (reagent/atom true)]
    (fn [{:keys [sample_id analysed survey start_date end_date method]}]
      (let [sample_id  (or sample_id 0)
            analysed   (or analysed 0)
            survey     (or survey 0)
            start_date (or start_date "unknown")
            end_date   (or end_date "unknown")
            method     (or method "N/A")]
        [:div
         {:class (str "habitat-observation-stats" (when @collapsed? " collapsed") (when-not (pos? sample_id) " disabled"))}
         [:h2
          {:class (str "bp3-heading" (if (or @collapsed? (not (pos? sample_id))) " bp3-icon-caret-right" " bp3-icon-caret-down"))
           :on-click #(swap! collapsed? not)}
          (str sample_id " sediment samples (" analysed " analysed) from " survey " surveys")]
         [:ul
          [:li (str "Date range: " start_date " to " end_date)]
          [:li (str "Methods of collection: " method)]]]))))

(defn habitat-observations []
  (let [collapsed?   (reagent/atom false)]
    (fn []
      (let [{:keys [squidle global-archive sediment loading?]} @(re-frame/subscribe [:sok/habitat-observations])]
        [components/drawer-group
         {:heading         "Habitat Observations"
          :icon            "media"
          :collapsed?      @collapsed?
          :toggle-collapse #(swap! collapsed? not)}
         (if loading?
           [b/spinner]
           [:div
            [squidle-stats squidle]
            [global-archive-stats global-archive]
            [sediment-stats sediment]])]))))

(defn state-of-knowledge []
  [components/drawer
   {:title       "State of Knowledge"
    :position    "right"
    :size        "460px"
    :isOpen      @(re-frame/subscribe [:sok/open?])
    :onClose     #(re-frame/dispatch [:sok/close])
    :hasBackdrop false
    :className   "state-of-knowledge-drawer"}
   [habitat-statistics]
   [bathymetry-statistics]
   [habitat-observations]])

(defn floating-state-of-knowledge-pill
  [{:keys [expanded? boundaries active-boundary]}]
  [components/floating-pill-control-menu
   (merge
    {:text           (or (:name active-boundary) "State of Knowledge")
     :icon           "add-column-right"
     :expanded?      expanded?
     :on-open-click  #(re-frame/dispatch [:sok/open-pill "state-of-knowledge"])
     :on-close-click #(re-frame/dispatch [:sok/open-pill nil])}
    (when active-boundary {:reset-click #(re-frame/dispatch [:sok/update-active-boundary nil])}))
   [:div.state-of-knowledge-pill-content
    
    [components/form-group
     {:label "Management Region"}
     [components/select
      {:value    active-boundary
       :options  boundaries
       :onChange #(re-frame/dispatch [:sok/update-active-boundary %])
       :keyfns
       {:id   :id
        :text :name}}]]
    
    [:a.data-coverage-report-link
     {:href   "https://blueprintjs.com/" ; Placeholder URL
      :target "_blank"}
     "View data coverage report"]]])

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
                      (or (:name active-network) "All networks")
                      " / "
                      (or (:name active-park) "All parks"))
               "imcra" (str
                        (or (:name active-provincial-bioregion) "All provincial bioregions")
                        " / "
                        (or (:name active-mesoscale-bioregion) "All mesoscale bioregions"))
               "meow" (str
                       (or (:name active-realm) "All realms")
                       " / "
                       (or (:name active-province) "All provinces")
                       " / "
                       (or (:name active-ecoregion) "All ecoregions")))]
    [components/floating-pill-control-menu
     (merge
      {:text           text
       :icon           "heatmap"
       :expanded?      expanded?
       :on-open-click  #(re-frame/dispatch [:sok/open-pill "boundaries"])
       :on-close-click #(re-frame/dispatch [:sok/open-pill nil])}
      (when active-boundaries? {:reset-click #(re-frame/dispatch [:sok/reset-active-boundaries])}))
     [:div.state-of-knowledge-pill-content

      (when amp?
        [components/form-group
         {:label "Network"}
         [components/select
          {:value    active-network
           :options  networks
           :onChange #(re-frame/dispatch [:sok/update-active-network %])
           :keyfns
           {:id   :name
            :text :name}}]])

      (when amp?
        [components/form-group
         {:label "Park"}
         [components/select
          {:value    active-park
           :options  parks
           :onChange #(re-frame/dispatch [:sok/update-active-park %])
           :keyfns
           {:id          :name
            :text        :name
            :breadcrumbs (comp vector :network)}}]])

      (when imcra?
        [components/form-group
         {:label "Provincial Bioregion"}
         [components/select
          {:value    active-provincial-bioregion
           :options  provincial-bioregions
           :onChange #(re-frame/dispatch [:sok/update-active-provincial-bioregion %])
           :keyfns
           {:id   :name
            :text :name}}]])

      (when imcra?
        [components/form-group
         {:label "Mesoscale Bioregion"}
         [components/select
          {:value    active-mesoscale-bioregion
           :options  mesoscale-bioregions
           :onChange #(re-frame/dispatch [:sok/update-active-mesoscale-bioregion %])
           :keyfns
           {:id          :name
            :text        :name
            :breadcrumbs (comp vector :provincial-bioregion)}}]])

      (when meow?
        [components/form-group
         {:label "Realms"}
         [components/select
          {:value    active-realm
           :options  realms
           :onChange #(re-frame/dispatch [:sok/update-active-realm %])
           :keyfns
           {:id   :name
            :text :name}}]])

      (when meow?
        [components/form-group
         {:label "Provinces"}
         [components/select
          {:value    active-province
           :options  provinces
           :onChange #(re-frame/dispatch [:sok/update-active-province %])
           :keyfns
           {:id          :name
            :text        :name
            :breadcrumbs (comp vector :realm)}}]])

      (when meow?
        [components/form-group
         {:label "Ecoregions"}
         [components/select
          {:value    active-ecoregion
           :options  ecoregions
           :onChange #(re-frame/dispatch [:sok/update-active-ecoregion %])
           :keyfns
           {:id          :name
            :text        :name
            :breadcrumbs (fn [{:keys [realm province]}] [realm province])}}]])]]))

(defn floating-zones-pill
  [{:keys [expanded? zones zones-iucn active-zone active-zone-iucn]}]
  (let [active-zones? @(re-frame/subscribe [:sok/active-zones?])
        text (or (:name active-zone) (:name active-zone-iucn) "All zones")]
    [components/floating-pill-control-menu
     (merge
      {:text           text
       :icon           "polygon-filter"
       :expanded?      expanded?
       :on-open-click  #(re-frame/dispatch [:sok/open-pill "zones"])
       :on-close-click #(re-frame/dispatch [:sok/open-pill nil])}
      (when active-zones? {:reset-click #(re-frame/dispatch [:sok/reset-active-zones])}))
     [:div.state-of-knowledge-pill-content

      [components/form-group
       {:label "Zone Category"}
       [components/select
        {:value    active-zone
         :options  zones
         :onChange #(re-frame/dispatch [:sok/update-active-zone %])
         :keyfns
         {:id   :name
          :text :name}}]]

      [components/form-group
       {:label "IUCN Category (Zone)"}
       [components/select
        {:value    active-zone-iucn
         :options  zones-iucn
         :onChange #(re-frame/dispatch [:sok/update-active-zone-iucn %])
         :keyfns
         {:id   :name
          :text :name}}]]]]))
