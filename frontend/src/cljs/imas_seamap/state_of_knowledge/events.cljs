;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.state-of-knowledge.events
  (:require [clojure.set :refer [rename-keys]]
            [ajax.core :as ajax]
            [imas-seamap.utils :refer [first-where]]))

(defn update-amp-boundaries [db [_ {:keys [networks parks zones zones_iucn]}]]
  (-> db
      (assoc-in [:state-of-knowledge :boundaries :amp :networks] networks)
      (assoc-in [:state-of-knowledge :boundaries :amp :parks] parks)
      (assoc-in [:state-of-knowledge :boundaries :amp :zones] zones)
      (assoc-in [:state-of-knowledge :boundaries :amp :zones-iucn] zones_iucn)))

(defn update-imcra-boundaries [db [_ {:keys [provincial_bioregions mesoscale_bioregions]}]]
  (let [mesoscale_bioregions (mapv #(rename-keys % {:provincial_bioregion :provincial-bioregion}) mesoscale_bioregions)]
    (-> db
        (assoc-in [:state-of-knowledge :boundaries :imcra :provincial-bioregions] provincial_bioregions)
        (assoc-in [:state-of-knowledge :boundaries :imcra :mesoscale-bioregions] mesoscale_bioregions))))

(defn update-meow-boundaries [db [_ {:keys [realms provinces ecoregions]}]]
  (-> db
      (assoc-in [:state-of-knowledge :boundaries :meow :realms] realms)
      (assoc-in [:state-of-knowledge :boundaries :meow :provinces] provinces)
      (assoc-in [:state-of-knowledge :boundaries :meow :ecoregions] ecoregions)))

(defn update-active-boundary [{:keys [db]} [_ active-boundary]]
  (let [db (-> db
               (assoc-in [:state-of-knowledge :boundaries :active-boundary] active-boundary)
               (cond->
                (not= (:id active-boundary) "amp")
                 (->
                  (assoc-in [:state-of-knowledge :boundaries :amp :active-network] nil)
                  (assoc-in [:state-of-knowledge :boundaries :amp :active-park] nil)
                  (assoc-in [:state-of-knowledge :boundaries :amp :active-zone] nil)
                  (assoc-in [:state-of-knowledge :boundaries :amp :active-zone-iucn] nil))

                 (not= (:id active-boundary) "imcra")
                 (->
                  (assoc-in [:state-of-knowledge :boundaries :imcra :active-mesoscale-bioregion] nil)
                  (assoc-in [:state-of-knowledge :boundaries :imcra :active-provincial-bioregion] nil))

                 (not= (:id active-boundary) "meow")
                 (->
                  (assoc-in [:state-of-knowledge :boundaries :meow :active-realm] nil)
                  (assoc-in [:state-of-knowledge :boundaries :meow :active-province] nil)
                  (assoc-in [:state-of-knowledge :boundaries :meow :active-ecoregion] nil))))
        layers (get-in db [:map :layers])
        layer  (first-where #(= (:id %) (:layer active-boundary)) layers)]
    (merge
     {:db db}
     (when layer {:dispatch [:map/pan-to-layer layer]}))))

(defn update-active-network [{:keys [db]} [_ network]]
  (let [db (cond-> db
             (not= network (get-in db [:state-of-knowledge :boundaries :amp :active-network]))
             (->
              (assoc-in [:state-of-knowledge :boundaries :amp :active-park] nil)
              (assoc-in [:state-of-knowledge :boundaries :amp :active-network] network)))]
    {:db db
     :dispatch-n [[:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}))

(defn update-active-park [{:keys [db]} [_ {:keys [network] :as park}]]
  (let [networks (get-in db [:state-of-knowledge :boundaries :amp :networks])
        network (first-where #(= (:name %) network) networks)
        db (-> db
               (assoc-in [:state-of-knowledge :boundaries :amp :active-network] network)
               (assoc-in [:state-of-knowledge :boundaries :amp :active-park] park))]
    {:db db
     :dispatch-n [[:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}))

(defn update-active-zone [{:keys [db]} [_ zone]]
  (let [db (-> db
               (assoc-in [:state-of-knowledge :boundaries :amp :active-zone] zone)
               (assoc-in [:state-of-knowledge :boundaries :amp :active-zone-iucn] nil))]
    {:db db
     :dispatch-n [[:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}))

(defn update-active-zone-iucn [{:keys [db]} [_ zone-iucn]]
  (let [db (-> db
               (assoc-in [:state-of-knowledge :boundaries :amp :active-zone-iucn] zone-iucn)
               (assoc-in [:state-of-knowledge :boundaries :amp :active-zone] nil))]
    {:db db
     :dispatch-n [[:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}))

(defn update-active-provincial-bioregion [{:keys [db]} [_ provincial-bioregion]]
  (let [db (cond-> db
             (not= provincial-bioregion (get-in db [:state-of-knowledge :boundaries :imcra :active-provincial-bioregion]))
             (->
              (assoc-in [:state-of-knowledge :boundaries :imcra :active-mesoscale-bioregion] nil)
              (assoc-in [:state-of-knowledge :boundaries :imcra :active-provincial-bioregion] provincial-bioregion)))]
    {:db db
     :dispatch-n [[:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}))

(defn update-active-mesoscale-bioregion [{:keys [db]} [_ {:keys [provincial-bioregion] :as mesoscale-bioregion}]]
  (let [provincial-bioregions (get-in db [:state-of-knowledge :boundaries :imcra :provincial-bioregions])
        provincial-bioregion2 (first-where #(= (:name %) provincial-bioregion) provincial-bioregions)
        db (-> db
               (assoc-in [:state-of-knowledge :boundaries :imcra :active-provincial-bioregion] provincial-bioregion2)
               (assoc-in [:state-of-knowledge :boundaries :imcra :active-mesoscale-bioregion] mesoscale-bioregion))]
    {:db db
     :dispatch-n [[:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}))

(defn update-active-realm [{:keys [db]} [_ realm]]
  (let [db (cond-> db
             (not= realm (get-in db [:state-of-knowledge :boundaries :meow :active-realm]))
             (->
              (assoc-in [:state-of-knowledge :boundaries :meow :active-realm] realm)
              (assoc-in [:state-of-knowledge :boundaries :meow :active-province] nil)
              (assoc-in [:state-of-knowledge :boundaries :meow :active-ecoregion] nil)))]
    {:db db
     :dispatch-n [[:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}))

(defn update-active-province [{:keys [db]} [_ {:keys [realm] :as province}]]
  (let [realms (get-in db [:state-of-knowledge :boundaries :meow :realms])
        realm (first-where #(= (:name %) realm) realms)
        db (cond-> db
             (not= province (get-in db [:state-of-knowledge :boundaries :meow :active-province]))
             (->
              (assoc-in [:state-of-knowledge :boundaries :meow :active-realm] realm)
              (assoc-in [:state-of-knowledge :boundaries :meow :active-province] province)
              (assoc-in [:state-of-knowledge :boundaries :meow :active-ecoregion] nil)))]
    {:db db
     :dispatch-n [[:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}))

(defn update-active-ecoregion [{:keys [db]} [_ {:keys [realm province] :as ecoregion}]]
  (let [{:keys [realms provinces]} (get-in db [:state-of-knowledge :boundaries :meow])
        realm (first-where #(= (:name %) realm) realms)
        province (first-where #(= (:name %) province) provinces)
        db (-> db
               (assoc-in [:state-of-knowledge :boundaries :meow :active-realm] realm)
               (assoc-in [:state-of-knowledge :boundaries :meow :active-province] province)
               (assoc-in [:state-of-knowledge :boundaries :meow :active-ecoregion] ecoregion))]
    {:db db
     :dispatch-n [[:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}))

(defn get-habitat-statistics [{:keys [db]}]
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
    {:db (assoc-in db [:state-of-knowledge :statistics :habitat :loading?] true)
     :http-xhrio {:method          :get
                  :uri             habitat-statistics-url
                  :params          {:boundary-type        active-boundary
                                    :network              active-network
                                    :park                 active-park
                                    :zone                 active-zone
                                    :zone-iucn            active-zone-iucn
                                    :provincial-bioregion active-provincial-bioregion
                                    :mesoscale-bioregion  active-mesoscale-bioregion
                                    :realm                active-realm
                                    :province             active-province
                                    :ecoregion            active-ecoregion}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:sok/got-habitat-statistics]
                  :on-failure      [:ajax/default-err-handler]}}))

(defn got-habitat-statistics [db [_ habitat-statistics]]
  (-> db
      (assoc-in [:state-of-knowledge :statistics :habitat :results] habitat-statistics)
      (assoc-in [:state-of-knowledge :statistics :habitat :loading?] false)))

(defn get-bathymetry-statistics [{:keys [db]}]
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
    {:db (assoc-in db [:state-of-knowledge :statistics :bathymetry :loading?] true)
     :http-xhrio {:method          :get
                  :uri             bathymetry-statistics-url
                  :params          {:boundary-type        active-boundary
                                    :network              active-network
                                    :park                 active-park
                                    :zone                 active-zone
                                    :zone-iucn            active-zone-iucn
                                    :provincial-bioregion active-provincial-bioregion
                                    :mesoscale-bioregion  active-mesoscale-bioregion
                                    :realm                active-realm
                                    :province             active-province
                                    :ecoregion            active-ecoregion}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:sok/got-bathymetry-statistics]
                  :on-failure      [:ajax/default-err-handler]}}))

(defn got-bathymetry-statistics [db [_ bathymetry-statistics]]
  (-> db
      (assoc-in [:state-of-knowledge :statistics :bathymetry :results] bathymetry-statistics)
      (assoc-in [:state-of-knowledge :statistics :bathymetry :loading?] false)))

(defn get-habitat-observations [{:keys [db]}]
  (let [habitat-observations-url (get-in db [:config :habitat-observations-url])
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
    {:db (assoc-in db [:state-of-knowledge :statistics :habitat-observations :loading?] true)
     :http-xhrio {:method          :get
                  :uri             habitat-observations-url
                  :params          {:boundary-type        active-boundary
                                    :network              active-network
                                    :park                 active-park
                                    :zone                 active-zone
                                    :zone-iucn            active-zone-iucn
                                    :provincial-bioregion active-provincial-bioregion
                                    :mesoscale-bioregion  active-mesoscale-bioregion
                                    :realm                active-realm
                                    :province             active-province
                                    :ecoregion            active-ecoregion}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [:sok/got-habitat-observations]
                  :on-failure      [:ajax/default-err-handler]}}))

(defn got-habitat-observations [db [_ {:keys [global_archive sediment squidle]}]]
  (-> db
      (assoc-in [:state-of-knowledge :statistics :habitat-observations :global-archive] global_archive)
      (assoc-in [:state-of-knowledge :statistics :habitat-observations :sediment] sediment)
      (assoc-in [:state-of-knowledge :statistics :habitat-observations :squidle] squidle)
      (assoc-in [:state-of-knowledge :statistics :habitat-observations :loading?] false)))

(defn open [db _]
  (-> db
      (assoc-in [:state-of-knowledge :open?] true)
      (assoc-in [:state-of-knowledge :open-pill] "state-of-knowledge")))

(defn close [{:keys [db]} _]
  (let [db (-> db
               (assoc-in [:state-of-knowledge :open?] false)
               (assoc-in [:state-of-knowledge :open-pill] nil))]
    {:db db
     :dispatch-n [[:sok/update-active-boundary nil]
                  [:sok/got-habitat-statistics nil]
                  [:sok/got-bathymetry-statistics nil]
                  [:sok/got-habitat-observations nil]]}))

(defn toggle [{:keys [db]} _]
  {:dispatch (if (get-in db [:state-of-knowledge :open?])
               [:sok/close]
               [:sok/open])})

(defn open-pill [db [_ pill-id]]
  (assoc-in db [:state-of-knowledge :open-pill] pill-id))
