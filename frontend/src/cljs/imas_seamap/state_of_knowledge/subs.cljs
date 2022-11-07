;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.state-of-knowledge.subs
  (:require [imas-seamap.utils :refer [append-query-params]]
            [imas-seamap.state-of-knowledge.utils :as utils :refer [boundary-filter-names cql-filter valid-boundaries]]))

(defn habitat-statistics [db _]
  (let [{:keys [results loading? show-layers?]} (get-in db [:state-of-knowledge :statistics :habitat])
        results (map #(assoc % :color (get-in db [:habitat-colours (:habitat %)])) results)]
    {:results results :loading? loading? :show-layers? show-layers?}))

(defn habitat-statistics-download-url [db _]
  (let [habitat-statistics-url (get-in db [:config :urls :habitat-statistics-url])
        {:keys [active-boundary] :as boundaries} (get-in db [:state-of-knowledge :boundaries])]
    (append-query-params
     habitat-statistics-url
     (merge
      (boundary-filter-names boundaries)
      {:boundary-type        (:id active-boundary)
       :format               "raw"}))))

(defn bathymetry-statistics [db _]
  (let [{:keys [results loading? show-layers?]} (get-in db [:state-of-knowledge :statistics :bathymetry])
        results (map #(assoc % :color (get-in db [:habitat-colours (:resolution %)])) results)]
    {:results results :loading? loading? :show-layers? show-layers?}))

(defn bathymetry-statistics-download-url [db _]
  (let [bathymetry-statistics-url (get-in db [:config :urls :bathymetry-statistics-url])
        {:keys [active-boundary] :as boundaries} (get-in db [:state-of-knowledge :boundaries])]
    (append-query-params
     bathymetry-statistics-url
     (merge
      (boundary-filter-names boundaries)
      {:boundary-type        (:id active-boundary)
       :format               "raw"}))))

(defn habitat-observations [db _]
  (get-in db [:state-of-knowledge :statistics :habitat-observations]))

(defn amp-boundaries [db _]
  (get-in db [:state-of-knowledge :boundaries :amp]))

(defn imcra-boundaries [db _]
  (get-in db [:state-of-knowledge :boundaries :imcra]))

(defn meow-boundaries [db _]
  (get-in db [:state-of-knowledge :boundaries :meow]))

(defn valid-amp-boundaries [db _]
  (let [{:keys [active-network active-park active-zone active-zone-iucn active-zone-id]} (get-in db [:state-of-knowledge :boundaries :amp])]
    #_(cond-> boundaries
      (not (nil? active-network))
      (update :parks #(filter (fn [{:keys [network]}] (= network active-network)) %))) ; only show parks within the selected network (if the selected network is not nil)
    (merge
     (:amp (valid-boundaries (get-in db [:state-of-knowledge :boundaries])))
     {:active-network   active-network
      :active-park      active-park
      :active-zone      active-zone
      :active-zone-iucn active-zone-iucn
      :active-zone-id   active-zone-id})))

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
  (let [amp (get-in db [:state-of-knowledge :boundaries :amp])
        {:keys [active-zone active-zone-iucn active-zone-id]} amp]
    (boolean (or active-zone active-zone-iucn active-zone-id)))) ; coerce to boolean to hide implementation

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
