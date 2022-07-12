;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.state-of-knowledge.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [goog.string :as gstring]
            [imas-seamap.blueprint :as b]
            [imas-seamap.components :as components]))

(defn boundary-selection []
  (let [selected-tab (reagent/atom "amp")]
    (fn []
      (let [{:keys [networks
                    parks
                    zones
                    zones-iucn
                    active-network
                    active-park
                    active-zone
                    active-zone-iucn]}           @(re-frame/subscribe [:sok/amp-boundaries])
            {:keys [provincial-bioregions
                    mesoscale-bioregions
                    active-provincial-bioregion
                    active-mesoscale-bioregion]} @(re-frame/subscribe [:sok/imcra-boundaries])
            {:keys [realms
                    provinces
                    ecoregions
                    active-realm
                    active-province
                    active-ecoregion]}           @(re-frame/subscribe [:sok/meow-boundaries])]
        [:div.boundaries-selection
         [components/drawer-group
          {:heading "Boundaries"
           :icon    "heatmap"}
          [b/tabs
           {:id              "boundary-selection-tabs"
            :selected-tab-id @selected-tab
            :on-change       #(reset! selected-tab %)}

           [b/tab
            {:id "amp"
             :title "AMP Boundaries"
             :panel
             (reagent/as-element
              [:div
               [:h2.bp3-heading "Australian Marine Parks"]
               [components/form-group
                {:label "Network"}
                [components/select
                 {:value    active-network
                  :options  networks
                  :onChange #(re-frame/dispatch [:map/update-active-network %])
                  :keyfns
                  {:id   :name
                   :text :name}}]]
               [components/form-group
                {:label "Park"}
                [components/select
                 {:value    active-park
                  :options  parks
                  :onChange #(re-frame/dispatch [:map/update-active-park %])
                  :keyfns
                  {:id          :name
                   :text        :name
                   :breadcrumbs (comp vector :network)}}]]
               [components/form-group
                {:label "Zone Category"}
                [components/select
                 {:value    active-zone
                  :options  zones
                  :onChange #(re-frame/dispatch [:map/update-active-zone %])
                  :keyfns
                  {:id   :name
                   :text :name}}]]
               [components/form-group
                {:label "IUCN Category (Zone)"}
                [components/select
                 {:value    active-zone-iucn
                  :options  zones-iucn
                  :onChange #(re-frame/dispatch [:map/update-active-zone-iucn %])
                  :keyfns
                  {:id   :name
                   :text :name}}]]])}]

           [b/tab
            {:id "imcra"
             :title "IMCRA Boundaries"
             :panel
             (reagent/as-element
              [:div
               [:h2.bp3-heading "Integrated Marine and Coastal Regionalisation of Australia"]
               [components/form-group
                {:label "Provincial Bioregion"}
                [components/select
                 {:value    active-provincial-bioregion
                  :options  provincial-bioregions
                  :onChange #(re-frame/dispatch [:map/update-active-provincial-bioregion %])
                  :keyfns
                  {:id   :name
                   :text :name}}]]
               [components/form-group
                {:label "Mesoscale Bioregion"}
                [components/select
                 {:value    active-mesoscale-bioregion
                  :options  mesoscale-bioregions
                  :onChange #(re-frame/dispatch [:map/update-active-mesoscale-bioregion %])
                  :keyfns
                  {:id          :name
                   :text        :name
                   :breadcrumbs (comp vector :provincial-bioregion)}}]]])}]

           [b/tab
            {:id "meow"
             :title "MEOW Boundaries"
             :panel
             (reagent/as-element
              [:div
               [:h2.bp3-heading "Marine Ecoregions of the World"]
               [components/form-group
                {:label "Realms"}
                [components/select
                 {:value    active-realm
                  :options  realms
                  :onChange #(re-frame/dispatch [:map/update-active-realm %])
                  :keyfns
                  {:id   :name
                   :text :name}}]]
               [components/form-group
                {:label "Provinces"}
                [components/select
                 {:value    active-province
                  :options  provinces
                  :onChange #(re-frame/dispatch [:map/update-active-province %])
                  :keyfns
                  {:id          :name
                   :text        :name
                   :breadcrumbs (comp vector :realm)}}]]
               [components/form-group
                {:label "Ecoregions"}
                [components/select
                 {:value    active-ecoregion
                  :options  ecoregions
                  :onChange #(re-frame/dispatch [:map/update-active-ecoregion %])
                  :keyfns
                  {:id          :name
                   :text        :name
                   :breadcrumbs (fn [{:keys [realm province]}] [realm province])}}]]])}]]

          [:a.data-coverage-report-link
           {:href   "https://blueprintjs.com/" ; Placeholder URL
            :target "_blank"}
           "View data coverage report"]]]))))

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
    (fn []
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
    (fn []
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
  [:h2
   {:class (str "bp3-heading" (when (pos? sample_id) " bp3-icon-caret-right"))}
   (if (pos? sample_id)
     (str sample_id " sediment samples (" analysed " analysed) from " survey " surveys")
     "No sediment data")]
  (let [collapsed? (reagent/atom true)]
    (fn []
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
    :onClose     #(re-frame/dispatch [:state-of-knowledge/close])
    :hasBackdrop false
    :className   "state-of-knowledge-drawer"}
   [boundary-selection]
   [habitat-statistics]
   [bathymetry-statistics]
   [habitat-observations]])

(defn floating-state-of-knowledge-pill []
  [components/floating-pill-control-menu
   {:text           "State of Knowledge"
    :icon           "add-column-right"
    :expanded?      @(re-frame/subscribe [:sok/pill-open?])
    :on-open-click  #(re-frame/dispatch [:state-of-knowledge/open])
    :on-close-click #(re-frame/dispatch [:state-of-knowledge/close-pill])}
   [:div ; TODO: replace content
    {:style {:width "300px"}}
    [:h4 "Description"]
    [:p "Welcome to the magic box; it can be any width or height desired."]
    [:p "(Magic box is only here as an example to what can be done with this space)"]]])
