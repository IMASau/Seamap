;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.state-of-knowledge.utils
  (:require [clojure.set :refer [rename-keys]]
            #_[debux.cs.core :refer [dbg] :include-macros true]))

;; Hardcoding this seems bad, but I'm not sure what to do about it? - TODO: figure out how to NOT hardcode this
(def boundaries
  [{:id    "amp"
    :name  "Australian Marine Parks"}
   {:id    "imcra"
    :name  "Integrated Marine and Coastal Regionalisation of Australia"}
   {:id    "meow"
    :name  "Marine Ecoregions of the World"}])

(defn boundary-filter-names [{:keys [amp imcra meow] :as _boundaries}]
  (let [{:keys [active-network active-park active-zone active-zone-iucn active-zone-id]} amp
        {:keys [active-provincial-bioregion active-mesoscale-bioregion]} imcra
        {:keys [active-realm active-province active-ecoregion]} meow]
    {:network              (:network active-network)
     :park                 (:park active-park)
     :zone                 (:zone active-zone)
     :zone-iucn            (:zone-iucn active-zone-iucn)
     :zone-id              (:zone-id active-zone-id)
     :provincial-bioregion (:provincial-bioregion active-provincial-bioregion)
     :mesoscale-bioregion  (:mesoscale-bioregion active-mesoscale-bioregion)
     :realm                (:realm active-realm)
     :province             (:province active-province)
     :ecoregion            (:ecoregion active-ecoregion)}))

(defn cql-filter [boundaries]
  (let [{:keys
         [network park zone zone-iucn zone-id
          provincial-bioregion mesoscale-bioregion
          realm province ecoregion]} (boundary-filter-names boundaries)
        filters (remove
                 nil?
                 [(when network (str "NETNAME='" network "'"))
                  (when park (str "RESNAME='" park "'"))
                  (when zone (str "ZONENAME='" zone "'"))
                  (when zone-iucn (str "ZONEIUCN='" zone-iucn "'"))
                  (when zone-id   (str "POLYGONID='" zone-id "'"))
                  (when provincial-bioregion (str "PB_NAME='" provincial-bioregion "'"))
                  (when mesoscale-bioregion (str "MESO_NAME='" mesoscale-bioregion "'"))
                  (when realm (str "REALM='" realm "'"))
                  (when province (str "PROVINCE='" province "'"))
                  (when ecoregion (str "ECOREGION='" ecoregion "'"))])]
    (when (seq filters) (apply str (interpose " AND " filters)))))

(defn- valid-boundary? [boundary boundary-filters keys]
  (reduce
   (fn [acc key]
     (and
      acc
      (or
       (nil? (key boundary-filters))
       (= (key boundary) (key boundary-filters)))))
   true keys))

(defn filter-valid-boundaries [{:keys [amp imcra meow] :as boundaries}]
  (let [boundary-filter-names  (boundary-filter-names boundaries)
        amp-boundaries         (:boundaries amp)
        imcra-boundaries       (:boundaries imcra)
        meow-boundaries        (:boundaries meow)]
    {:amp   {:networks   (->> amp-boundaries
                              (filter #(and (:network %) (valid-boundary? % boundary-filter-names [:zone :zone-iucn])))
                              (map #(select-keys % [:network]))
                              distinct
                              (sort-by :network)
                              vec)
             :parks      (->> amp-boundaries
                              (filter #(and (:zone %) (valid-boundary? % boundary-filter-names [:network :zone :zone-iucn])))
                              (map #(select-keys % [:network :park]))
                              distinct
                              (sort-by (juxt :network :park))
                              vec)
             :zones      (->> amp-boundaries
                              (filter #(and (:zone %) (valid-boundary? % boundary-filter-names [:network :park])))
                              (map #(select-keys % [:zone]))
                              distinct
                              (sort-by :zone)
                              vec)
             :zones-iucn (->> amp-boundaries
                              (filter #(and (:zone-iucn %) (valid-boundary? % boundary-filter-names [:network :park])))
                              (map #(select-keys % [:zone-iucn]))
                              distinct
                              (sort-by :zone-iucn)
                              vec)
             :zone-ids   (->> amp-boundaries
                              (filter #(and (:zone-id %) (valid-boundary? % boundary-filter-names [:network :park :zone :zone-iucn])))
                              (map #(select-keys % [:network :park :zone-id]))
                              distinct
                              (sort-by (juxt :network :park :zone-id))
                              vec)}
     :imcra {:provincial-bioregions (->> imcra-boundaries
                                         (filter #(:provincial-bioregion %))
                                         (map #(select-keys % [:provincial-bioregion]))
                                         distinct
                                         (sort-by :provincial-bioregion)
                                         vec)
             :mesoscale-bioregions  (->> imcra-boundaries
                                         (filter #(and (:mesoscale-bioregion %) (valid-boundary? % boundary-filter-names [:mesoscale-bioregion])))
                                         (map #(select-keys % [:provincial-bioregion :mesoscale-bioregion]))
                                         distinct
                                         (sort-by (juxt :provincial-bioregion :mesoscale-bioregion))
                                         vec)}
     :meow  {:realms     (->> meow-boundaries
                              (filter #(:realm %))
                              (map #(select-keys % [:realm]))
                              distinct
                              (sort-by :realm)
                              vec)
             :provinces  (->> meow-boundaries
                              (filter #(and (:province %) (valid-boundary? % boundary-filter-names [:realm])))
                              (map #(select-keys % [:realm :province]))
                              distinct
                              (sort-by (juxt :realm :province))
                              vec)
             :ecoregions (->> meow-boundaries
                              (filter #(and (:ecoregion %) (valid-boundary? % boundary-filter-names [:realm :province])))
                              (map #(select-keys % [:realm :province :ecoregion]))
                              distinct
                              (sort-by (juxt :realm :province :ecoregion))
                              vec)}}))
