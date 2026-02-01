;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.config.content
  "Content registry for deployment-specific text and rich content.

   This namespace stores rich content (like welcome dialogues) that would
   otherwise be hardcoded in components. Content is stored as Reagent hiccup
   and referenced by keyword from deployment configurations.")

(def welcome-content
  "Welcome dialogue content for each deployment"

  {:welcome/seamap-australia
   [:div.overview
    "Seamap Australia is a nationally synthesised product of
     seafloor habitat data collected from various stakeholders around
     Australia. Source datasets were reclassified according to a
     newly-developed national marine benthic habitat classification
     scheme, and synthesised to produce a single standardised GIS
     data layer of Australian benthic marine habitats."]

   :welcome/tas-marine-atlas
   [:div.overview
    [:p
     "The waters and coastlines represented in the Tasmania's Marine Atlas are of
      significance to Tasmanian Aboriginal people. We recognise the deep history and
      culture of the islands represented in this Atlas and acknowledge the traditional
      owners and custodians of Country. We pay respect to Tasmanian Aboriginal people,
      who have survived invasion and dispossession, and continue to maintain their
      identity, culture and Indigenous rights and honour their Elders, past and
      present."]
    [:p
     "Any absence of information about Tasmanian Aboriginal people's connection to Sea
      Country in the Tasmania's Marine Atlas does not indicate an absence of
      connection or significance. We respect the right of Tasmanian Aboriginal people
      to self-determination and affirm their right to decide what information is
      included in the Atlas."]
    [:p
     "Learn more about Sea Country and Tasmanian Aboriginal People "
     [:a
      {:href "https://storymaps.arcgis.com/stories/22672ae2d60f424f8d1e024232cfa885"
       :target "_blank"}
      "here"]
     "."]]

   :welcome/seamap-antarctica
   [:div.overview
    "Seamap Antarctica provides access to seafloor habitat data for the Antarctic region.
     Explore benthic marine habitats and environmental data for Antarctic waters."]

   :welcome/futures-of-seafood
   [:div.overview
    "Welcome to the Futures of Seafood interactive map. Explore seafood-related
     data and information for Australian waters."]})

(def citation-content
  "Citation information for each deployment"

  {:citation/seamap-australia
   [:div.citation-section
    [:p
     "Please cite as "
     [:span {:id "citation"}
      "Lucieer V, Walsh P, Flukes E, Butler C, Proctor R, Johnson C (2017). "
      [:i "Seamap Australia - a national seafloor habitat classification scheme."]
      " Institute for Marine and Antarctic Studies (IMAS), University of Tasmania (UTAS)."]]
    [:p
     "Data contributors should be acknowledged, and datasets cited where appropriate,
      which may include adding citations to lists of references and/or citing the data
      in figure captions. Dataset citations are available from each dataset's metadata
      (Available from layer info controls). Contributors of source datasets can be
      viewed in the layer catalogue under the Classification tab of the left sidebar."]]})

(defn get-content
  "Retrieve content by keyword.
   Returns nil if content key not found."
  [content-key]
  (get welcome-content content-key))

(defn get-citation
  "Retrieve citation content by keyword.
   Returns nil if citation key not found."
  [citation-key]
  (get citation-content citation-key))
