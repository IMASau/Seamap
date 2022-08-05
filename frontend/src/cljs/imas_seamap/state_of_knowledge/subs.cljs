;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.state-of-knowledge.subs
  (:require [imas-seamap.utils :refer [append-query-params-from-map]]
            [imas-seamap.state-of-knowledge.utils :as utils :refer [boundary-filter-names cql-filter]]))

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

(defn valid-amp-boundaries [db _]
  (let [{:keys [active-network]
         :as   boundaries}      (get-in db [:state-of-knowledge :boundaries :amp])
        active-network          (:name active-network)]
    (cond-> boundaries
      (not (nil? active-network))
      (update :parks #(filter (fn [{:keys [network]}] (= network active-network)) %))))) ; only show parks within the selected network (if the selected network is not nil)

(defn valid-imcra-boundaries [db _]
  (let [{:keys [active-provincial-bioregion]
         :as   boundaries}                   (get-in db [:state-of-knowledge :boundaries :imcra])
        active-provincial-bioregion          (:name active-provincial-bioregion)]
    (cond-> boundaries
      (not (nil? active-provincial-bioregion))
      (update :mesoscale-bioregions #(filter (fn [{:keys [provincial-bioregion]}] (= provincial-bioregion active-provincial-bioregion)) %)))))  ; only show mesoscale bioregions within the selected provincial bioregions (if the selected provincial bioregion is not nil)

(defn valid-meow-boundaries [db _]
  (let [{:keys [active-realm active-province]
         :as   boundaries}                    (get-in db [:state-of-knowledge :boundaries :meow])
        active-realm                          (:name active-realm)
        active-province                       (:name active-province)]
    (cond-> boundaries
      (not (nil? active-realm))
      (->
       (update :provinces #(filter (fn [{:keys [realm]}] (= realm active-realm)) %))   ; only show provinces within the selected realm (if the selected realm is not nil)
       (update :ecoregions #(filter (fn [{:keys [realm]}] (= realm active-realm)) %))) ; only show ecoregions within the selected realm (if the selected realm is not nil)

      (not (nil? active-province))
      (update :ecoregions #(filter (fn [{:keys [province]}] (= province active-province)) %))))) ; only show ecoregions within the selected province (if the selected province is not nil)

(defn boundaries [db _]
  utils/boundaries)

(defn active-boundary [db _]
  (get-in db [:state-of-knowledge :boundaries :active-boundary]))

(defn active-boundaries? [db _]
  (let [{:keys [amp imcra meow]}                                         (get-in db [:state-of-knowledge :boundaries])
        {:keys [active-network active-park]}                             amp
        {:keys [active-provincial-bioregion active-mesoscale-bioregion]} imcra
        {:keys [active-realm active-province active-ecoregion]}          meow]
    (boolean ; coerce to boolean to hide implementation
     (or
      active-network active-park active-provincial-bioregion
      active-mesoscale-bioregion active-realm active-province active-ecoregion))))

(defn active-zones? [db _]
  (let [amp                                    (get-in db [:state-of-knowledge :boundaries :amp])
        {:keys [active-zone active-zone-iucn]} amp]
    (boolean (or active-zone active-zone-iucn)))) ; coerce to boolean to hide implementation

(defn open? [db _]
  (boolean (get-in db [:state-of-knowledge :boundaries :active-boundary])))

(defn open-pill [db _]
  (get-in db [:state-of-knowledge :open-pill]))

(defn boundary-layer-filter-fn [db _]
  (let [{:keys [active-boundary-layer] :as boundaries} (get-in db [:state-of-knowledge :boundaries])
        cql-filter (cql-filter boundaries)]
    (fn [layer]
      (when (and (= layer active-boundary-layer) cql-filter)
        {:cql_filter cql-filter}))))
