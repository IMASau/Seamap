;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.state-of-knowledge.events
  (:require [clojure.set :refer [rename-keys]]
            [ajax.core :as ajax]
            [imas-seamap.utils :refer [first-where encode-state]]
            [imas-seamap.state-of-knowledge.utils :refer [boundary-filter-names cql-filter]]
            [imas-seamap.interop.leaflet :as leaflet]))

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

(defn- select-boundary-layer
  "Based on the currently active boundary and boundary filters, select an
   appropriate boundary layer."
  [keyed-layers {boundary-id :id} {:keys [amp imcra meow] :as _boundaries}]
  (let [{:keys [active-park active-zone active-zone-iucn]} amp
        {:keys [active-mesoscale-bioregion]} imcra
        {:keys [active-province active-ecoregion]} meow

        layer-key (case boundary-id

                    "amp"   (cond
                              active-zone-iucn :amp-zone-iucn
                              active-zone      :amp-zone
                              active-park      :amp-park
                              :else            :amp-network)

                    "imcra" (cond
                              active-mesoscale-bioregion :imcra-mesoscale-bioregion
                              :else                      :imcra-provincial-bioregion)

                    "meow"  (cond
                              active-ecoregion :meow-ecoregion
                              active-province  :meow-province
                              :else            :meow-realm)

                    nil)]
    (when layer-key (layer-key keyed-layers))))

(defn update-active-boundary-layer [{:keys [db]} _]
  (let [keyed-layers (get-in db [:map :keyed-layers])
        active-boundary (get-in db [:state-of-knowledge :boundaries :active-boundary])

        previous-boundary-layer (get-in db [:state-of-knowledge :boundaries :active-boundary-layer])
        current-boundary-layer  (select-boundary-layer keyed-layers active-boundary (get-in db [:state-of-knowledge :boundaries]))

        db                      (assoc-in db [:state-of-knowledge :boundaries :active-boundary-layer] current-boundary-layer)]
    {:db db
     :dispatch-n [(when previous-boundary-layer [:map/remove-layer previous-boundary-layer])
                  (when current-boundary-layer [:map/add-layer current-boundary-layer])
                  (when current-boundary-layer [:sok/get-filtered-bounds])]}))

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
                  (assoc-in [:state-of-knowledge :boundaries :imcra :active-provincial-bioregion] nil)
                  (assoc-in [:state-of-knowledge :boundaries :imcra :active-mesoscale-bioregion] nil))

                 (not= (:id active-boundary) "meow")
                 (->
                  (assoc-in [:state-of-knowledge :boundaries :meow :active-realm] nil)
                  (assoc-in [:state-of-knowledge :boundaries :meow :active-province] nil)
                  (assoc-in [:state-of-knowledge :boundaries :meow :active-ecoregion] nil))))]
    {:db db
     :dispatch-n [[:sok/update-active-boundary-layer]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]
                  (when active-boundary [:sok/open-pill nil])]}))

(defn update-active-network [{:keys [db]} [_ network]]
  (let [db (cond-> db
             (not= network (get-in db [:state-of-knowledge :boundaries :amp :active-network]))
             (->
              (assoc-in [:state-of-knowledge :boundaries :amp :active-park] nil)
              (assoc-in [:state-of-knowledge :boundaries :amp :active-network] network)))
        park (get-in db [:state-of-knowledge :boundaries :amp :active-park])]
    {:db db
     :dispatch-n [[:sok/update-active-boundary-layer]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]
                  (when (and network park) [:sok/open-pill nil])]}))

(defn update-active-park [{:keys [db]} [_ {:keys [network] :as park}]]
  (let [old-network (get-in db [:state-of-knowledge :boundaries :amp :active-network])
        networks (get-in db [:state-of-knowledge :boundaries :amp :networks])
        network (if park
                  (first-where #(= (:name %) network) networks)
                  old-network)
        db (-> db
               (assoc-in [:state-of-knowledge :boundaries :amp :active-network] network)
               (assoc-in [:state-of-knowledge :boundaries :amp :active-park] park))]
    {:db db
     :dispatch-n [[:sok/update-active-boundary-layer]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]
                  (when (and park old-network) [:sok/open-pill nil])]}))

(defn update-active-zone [{:keys [db]} [_ zone]]
  (let [db (-> db
               (assoc-in [:state-of-knowledge :boundaries :amp :active-zone] zone)
               (assoc-in [:state-of-knowledge :boundaries :amp :active-zone-iucn] nil))]
    {:db db
     :dispatch-n [[:sok/update-active-boundary-layer]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]
                  (when zone [:sok/open-pill nil])]}))

(defn update-active-zone-iucn [{:keys [db]} [_ zone-iucn]]
  (let [db (-> db
               (assoc-in [:state-of-knowledge :boundaries :amp :active-zone-iucn] zone-iucn)
               (assoc-in [:state-of-knowledge :boundaries :amp :active-zone] nil))]
    {:db db
     :dispatch-n [[:sok/update-active-boundary-layer]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]
                  (when zone-iucn [:sok/open-pill nil])]}))

(defn update-active-provincial-bioregion [{:keys [db]} [_ provincial-bioregion]]
  (let [db (cond-> db
             (not= provincial-bioregion (get-in db [:state-of-knowledge :boundaries :imcra :active-provincial-bioregion]))
             (->
              (assoc-in [:state-of-knowledge :boundaries :imcra :active-mesoscale-bioregion] nil)
              (assoc-in [:state-of-knowledge :boundaries :imcra :active-provincial-bioregion] provincial-bioregion)))
        mesoscale-bioregion (get-in db [:state-of-knowledge :boundaries :imcra :active-mesoscale-bioregion])]
    {:db db
     :dispatch-n [[:sok/update-active-boundary-layer]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]
                  (when (and provincial-bioregion mesoscale-bioregion) [:sok/open-pill nil])]}))

(defn update-active-mesoscale-bioregion [{:keys [db]} [_ {:keys [provincial-bioregion] :as mesoscale-bioregion}]]
  (let [old-provincial-bioregion (get-in db [:state-of-knowledge :boundaries :imcra :active-provincial-bioregion])
        provincial-bioregions (get-in db [:state-of-knowledge :boundaries :imcra :provincial-bioregions])
        provincial-bioregion (if mesoscale-bioregion
                               (first-where #(= (:name %) provincial-bioregion) provincial-bioregions)
                               old-provincial-bioregion)
        db (-> db
               (assoc-in [:state-of-knowledge :boundaries :imcra :active-provincial-bioregion] provincial-bioregion)
               (assoc-in [:state-of-knowledge :boundaries :imcra :active-mesoscale-bioregion] mesoscale-bioregion))]
    {:db db
     :dispatch-n [[:sok/update-active-boundary-layer]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]
                  (when (and mesoscale-bioregion old-provincial-bioregion) [:sok/open-pill nil])]}))

(defn update-active-realm [{:keys [db]} [_ realm]]
  (let [db (cond-> db
             (not= realm (get-in db [:state-of-knowledge :boundaries :meow :active-realm]))
             (->
              (assoc-in [:state-of-knowledge :boundaries :meow :active-realm] realm)
              (assoc-in [:state-of-knowledge :boundaries :meow :active-province] nil)
              (assoc-in [:state-of-knowledge :boundaries :meow :active-ecoregion] nil)))
        ecoregion (get-in db [:state-of-knowledge :boundaries :meow :active-ecoregion])]
    {:db db
     :dispatch-n [[:sok/update-active-boundary-layer]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]
                  (when (and realm ecoregion) [:sok/open-pill nil])]}))

(defn update-active-province [{:keys [db]} [_ {:keys [realm] :as province}]]
  (let [old-realm (get-in db [:state-of-knowledge :boundaries :meow :active-realm])
        realms (get-in db [:state-of-knowledge :boundaries :meow :realms])
        realm (if province
                (first-where #(= (:name %) realm) realms)
                old-realm)
        db (cond-> db
             (not= province (get-in db [:state-of-knowledge :boundaries :meow :active-province]))
             (->
              (assoc-in [:state-of-knowledge :boundaries :meow :active-realm] realm)
              (assoc-in [:state-of-knowledge :boundaries :meow :active-province] province)
              (assoc-in [:state-of-knowledge :boundaries :meow :active-ecoregion] nil)))
        ecoregion (get-in db [:state-of-knowledge :boundaries :meow :active-ecoregion])]
    {:db db
     :dispatch-n [[:sok/update-active-boundary-layer]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]
                  (when (and province ecoregion) [:sok/open-pill nil])]}))

(defn update-active-ecoregion [{:keys [db]} [_ {:keys [realm province] :as ecoregion}]]
  (let [{old-realm    :active-realm
         old-province :active-province} (get-in db [:state-of-knowledge :boundaries :meow])
        {:keys [realms provinces]} (get-in db [:state-of-knowledge :boundaries :meow])
        realm (if ecoregion
                (first-where #(= (:name %) realm) realms)
                old-realm)
        province (if ecoregion
                   (first-where #(= (:name %) province) provinces)
                   old-province)
        db (-> db
               (assoc-in [:state-of-knowledge :boundaries :meow :active-realm] realm)
               (assoc-in [:state-of-knowledge :boundaries :meow :active-province] province)
               (assoc-in [:state-of-knowledge :boundaries :meow :active-ecoregion] ecoregion))]
    {:db db
     :dispatch-n [[:sok/update-active-boundary-layer]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]
                  (when (and ecoregion old-province) [:sok/open-pill nil])]}))

(defn reset-active-boundaries [{:keys [db]} _]
  (let [db (-> db
               (assoc-in [:state-of-knowledge :boundaries :amp :active-network] nil)
               (assoc-in [:state-of-knowledge :boundaries :amp :active-park] nil)
               (assoc-in [:state-of-knowledge :boundaries :imcra :active-provincial-bioregion] nil)
               (assoc-in [:state-of-knowledge :boundaries :imcra :active-mesoscale-bioregion] nil)
               (assoc-in [:state-of-knowledge :boundaries :meow :active-realm] nil)
               (assoc-in [:state-of-knowledge :boundaries :meow :active-province] nil)
               (assoc-in [:state-of-knowledge :boundaries :meow :active-ecoregion] nil))]
    {:db db
     :dispatch-n [[:sok/update-active-boundary-layer]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}))

(defn reset-active-zones [{:keys [db]} _]
  (let [db (-> db
               (assoc-in [:state-of-knowledge :boundaries :amp :active-zone] nil)
               (assoc-in [:state-of-knowledge :boundaries :amp :active-zone-iucn] nil))]
    {:db db
     :dispatch-n [[:sok/update-active-boundary-layer]
                  [:sok/get-habitat-statistics]
                  [:sok/get-bathymetry-statistics]
                  [:sok/get-habitat-observations]]}))

(defn get-habitat-statistics [{:keys [db]}]
  (let [habitat-statistics-url (get-in db [:config :habitat-statistics-url])
        {:keys [active-boundary] :as boundaries} (get-in db [:state-of-knowledge :boundaries])
        active-boundary (:id active-boundary)]
    (if
     active-boundary
      {:db (assoc-in db [:state-of-knowledge :statistics :habitat :loading?] true)
       :http-xhrio {:method          :get
                    :uri             habitat-statistics-url
                    :params          (merge
                                      {:boundary-type        active-boundary}
                                      (boundary-filter-names boundaries))
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:sok/got-habitat-statistics]
                    :on-failure      [:ajax/default-err-handler]}}
      {:db (assoc-in db [:state-of-knowledge :statistics :habitat :results] [])})))

(defn got-habitat-statistics [db [_ habitat-statistics]]
  (-> db
      (assoc-in [:state-of-knowledge :statistics :habitat :results] habitat-statistics)
      (assoc-in [:state-of-knowledge :statistics :habitat :loading?] false)))

(defn get-bathymetry-statistics [{:keys [db]}]
  (let [bathymetry-statistics-url (get-in db [:config :bathymetry-statistics-url])
        {:keys [active-boundary] :as boundaries} (get-in db [:state-of-knowledge :boundaries])
        active-boundary (:id active-boundary)]
    (if
     active-boundary
      {:db (assoc-in db [:state-of-knowledge :statistics :bathymetry :loading?] true)
       :http-xhrio {:method          :get
                    :uri             bathymetry-statistics-url
                    :params          (merge
                                      {:boundary-type        active-boundary}
                                      (boundary-filter-names boundaries))
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:sok/got-bathymetry-statistics]
                    :on-failure      [:ajax/default-err-handler]}}
      {:db (assoc-in db [:state-of-knowledge :statistics :bathymetry :results] [])})))

(defn got-bathymetry-statistics [db [_ bathymetry-statistics]]
  (-> db
      (assoc-in [:state-of-knowledge :statistics :bathymetry :results] bathymetry-statistics)
      (assoc-in [:state-of-knowledge :statistics :bathymetry :loading?] false)))

(defn get-habitat-observations [{:keys [db]}]
  (let [habitat-observations-url (get-in db [:config :habitat-observations-url])
        {:keys [active-boundary] :as boundaries} (get-in db [:state-of-knowledge :boundaries])
        active-boundary (:id active-boundary)]
    (if
     active-boundary
      {:db (assoc-in db [:state-of-knowledge :statistics :habitat-observations :loading?] true)
       :http-xhrio {:method          :get
                    :uri             habitat-observations-url
                    :params          (merge
                                      {:boundary-type        active-boundary}
                                      (boundary-filter-names boundaries))
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:sok/got-habitat-observations]
                    :on-failure      [:ajax/default-err-handler]}}
      {:db (-> db
               (assoc-in [:state-of-knowledge :statistics :habitat-observations :global-archive] nil)
               (assoc-in [:state-of-knowledge :statistics :habitat-observations :sediment] nil)
               (assoc-in [:state-of-knowledge :statistics :habitat-observations :squidle] nil))})))

(defn got-habitat-observations [db [_ {:keys [global_archive sediment squidle]}]]
  (-> db
      (assoc-in [:state-of-knowledge :statistics :habitat-observations :global-archive] global_archive)
      (assoc-in [:state-of-knowledge :statistics :habitat-observations :sediment] sediment)
      (assoc-in [:state-of-knowledge :statistics :habitat-observations :squidle] squidle)
      (assoc-in [:state-of-knowledge :statistics :habitat-observations :loading?] false)))

(defn close [{:keys [db]} _]
  {:db db
   :dispatch-n [[:sok/open-pill nil]
                [:sok/update-active-boundary nil]
                [:sok/got-habitat-statistics nil]
                [:sok/got-bathymetry-statistics nil]
                [:sok/got-habitat-observations nil]]})

(defn open-pill [db [_ pill-id]]
  (assoc-in db [:state-of-knowledge :open-pill] pill-id))

(defn get-filtered-bounds [{:keys [db]} _]
  (let [{{:keys [layer_name server_url]} :active-boundary-layer :as boundaries}
        (get-in db [:state-of-knowledge :boundaries])
        cql-filter (cql-filter boundaries)]
    {:http-xhrio {:method          :get
                  :uri             server_url
                  :params          (merge
                                    {:request      "GetFeature"
                                     :service      "WFS"
                                     :version      "2.0.0"
                                     :typeNames    layer_name
                                     :outputFormat "application/json"}
                                    (when cql-filter {:cql_filter cql-filter}))
                  :response-format (ajax/json-response-format)
                  :on-success      [:sok/got-filtered-bounds]
                  :on-failure      [:ajax/default-err-handler]}}))

(defn got-filtered-bounds [{:keys [db]} [_ geojson]]
  (let [{{south :lat west :lng} :_southWest
         {north :lat east :lng} :_northEast}
        (js->clj (.getBounds (leaflet/geojson-feature (clj->js geojson))) :keywordize-keys true)
        bounds {:north north
                :south south
                :east  east
                :west  west}
        db     (assoc-in db [:map :bounds] bounds)]
    {:db       db
     :put-hash (encode-state db)}))

(defn habitat-toggle-show-layers [{:keys [db]} _]
  (let [db            (update-in db [:state-of-knowledge :statistics :habitat :show-layers?] not)
        show-layers?  (get-in db [:state-of-knowledge :statistics :habitat :show-layers?])
        habitat-layer (get-in db [:map :keyed-layers :habitat])]
    {:db db
     :dispatch
     (if show-layers?
       [:map/add-layer habitat-layer]
       [:map/remove-layer habitat-layer])}))

(defn bathymetry-toggle-show-layers [{:keys [db]} _]
  (let [db               (update-in db [:state-of-knowledge :statistics :bathymetry :show-layers?] not)
        show-layers?     (get-in db [:state-of-knowledge :statistics :bathymetry :show-layers?])
        bathymetry-layer (get-in db [:map :keyed-layers :bathymetry])]
    {:db db
     :dispatch
     (if show-layers?
       [:map/add-layer bathymetry-layer]
       [:map/remove-layer bathymetry-layer])}))

(defn habitat-observations-toggle-show-layers [{:keys [db]} _]
  (let [db           (update-in db [:state-of-knowledge :statistics :habitat-observations :show-layers?] not)
        show-layers? (get-in db [:state-of-knowledge :statistics :habitat-observations :show-layers?])
        habitat-obs-imagery-layer  (get-in db [:map :keyed-layers :habitat-obs-imagery])
        habitat-obs-sediment-layer (get-in db [:map :keyed-layers :habitat-obs-sediment])
        habitat-obs-video-layer    (get-in db [:map :keyed-layers :habitat-obs-video])]
    {:db db
     :dispatch-n
     (if show-layers?
       [[:map/add-layer habitat-obs-imagery-layer]
        [:map/add-layer habitat-obs-sediment-layer]
        [:map/add-layer habitat-obs-video-layer]]
       [[:map/remove-layer habitat-obs-imagery-layer]
        [:map/remove-layer habitat-obs-sediment-layer]
        [:map/remove-layer habitat-obs-video-layer]])}))
