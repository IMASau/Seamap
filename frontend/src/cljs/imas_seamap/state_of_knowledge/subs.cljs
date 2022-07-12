;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.state-of-knowledge.subs
  (:require [imas-seamap.utils :refer [append-query-params-from-map]]
            [imas-seamap.state-of-knowledge.utils :as utils]))

(defn habitat-statistics [db _]
  (let [{:keys [results loading?]} (get-in db [:state-of-knowledge :statistics :habitat])
        results (map #(assoc % :color (get-in db [:habitat-colours (:habitat %)])) results)]
    {:results results :loading? loading?}))

(defn habitat-statistics-download-url [db _]
  (let [habitat-statistics-url (get-in db [:config :habitat-statistics-url])
        {:keys [active-boundary amp imcra meow]} (get-in db [:state-of-knowledge :boundaries])
        {:keys [active-network active-park active-zone active-zone-iucn]} amp
        {:keys [active-provincial-bioregion active-mesoscale-bioregion]} imcra
        {:keys [active-realm active-province active-ecoregion]} meow
        active-boundary (:id active-boundary)
        [active-network active-park active-zone active-zone-iucn
         active-provincial-bioregion active-mesoscale-bioregion active-realm
         active-province active-ecoregion]
        (map
         :name
         [active-network active-park active-zone active-zone-iucn
          active-provincial-bioregion active-mesoscale-bioregion active-realm
          active-province active-ecoregion])]
    (append-query-params-from-map
     habitat-statistics-url
     {:boundary-type        active-boundary
      :network              active-network
      :park                 active-park
      :zone                 active-zone
      :zone-iucn            active-zone-iucn
      :provincial-bioregion active-provincial-bioregion
      :mesoscale-bioregion  active-mesoscale-bioregion
      :realm                active-realm
      :province             active-province
      :ecoregion            active-ecoregion
      :format               "raw"})))

(defn bathymetry-statistics [db _]
  (let [{:keys [results loading?]} (get-in db [:state-of-knowledge :statistics :bathymetry])
        results (map #(assoc % :color (get-in db [:habitat-colours (:resolution %)])) results)]
    {:results results :loading? loading?}))

(defn bathymetry-statistics-download-url [db _]
  (let [bathymetry-statistics-url (get-in db [:config :bathymetry-statistics-url])
        {:keys [active-boundary amp imcra meow]} (get-in db [:state-of-knowledge :boundaries])
        {:keys [active-network active-park active-zone active-zone-iucn]} amp
        {:keys [active-provincial-bioregion active-mesoscale-bioregion]} imcra
        {:keys [active-realm active-province active-ecoregion]} meow
        active-boundary (:id active-boundary)
        [active-network active-park active-zone active-zone-iucn
         active-provincial-bioregion active-mesoscale-bioregion active-realm
         active-province active-ecoregion]
        (map
         :name
         [active-network active-park active-zone active-zone-iucn
          active-provincial-bioregion active-mesoscale-bioregion active-realm
          active-province active-ecoregion])]
    (append-query-params-from-map
     bathymetry-statistics-url
     {:boundary-type        active-boundary
      :network              active-network
      :park                 active-park
      :zone                 active-zone
      :zone-iucn            active-zone-iucn
      :provincial-bioregion active-provincial-bioregion
      :mesoscale-bioregion  active-mesoscale-bioregion
      :realm                active-realm
      :province             active-province
      :ecoregion            active-ecoregion
      :format               "raw"})))

(defn habitat-observations [db _]
  (get-in db [:state-of-knowledge :statistics :habitat-observations]))

(defn amp-boundaries [db _]
  (get-in db [:state-of-knowledge :boundaries :amp]))

(defn imcra-boundaries [db _]
  (get-in db [:state-of-knowledge :boundaries :imcra]))

(defn meow-boundaries [db _]
  (get-in db [:state-of-knowledge :boundaries :meow]))

(defn boundaries [db _]
  utils/boundaries)

(defn active-boundary [db _]
  (get-in db [:state-of-knowledge :boundaries :active-boundary]))

(defn open? [db _]
  (get-in db [:state-of-knowledge :open?]))

(defn pill-open? [db _]
  (get-in db [:state-of-knowledge :pill-open?]))
