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

(defn all-boundaries [{:keys [amp imcra meow] :as _boundaries}]
  (let [amp-boundaries   (:boundaries amp)
        imcra-boundaries (:boundaries imcra)
        meow-boundaries  (:boundaries meow)]
    {:amp   {:networks   (vec (distinct (map #(select-keys % [:network]) amp-boundaries)))
             :parks      (vec (distinct (map #(select-keys % [:network :park]) amp-boundaries)))
             :zones      (vec (distinct (map #(select-keys % [:zone]) amp-boundaries)))
             :zones-iucn (vec (distinct (map #(select-keys % [:zone-iucn]) amp-boundaries)))
             :zone-ids   (vec (distinct (map #(select-keys % [:network :park :zone-id]) amp-boundaries)))}
     :imcra {:provincial-bioregions (vec (distinct (map #(select-keys % [:provincial-bioregion]) imcra-boundaries)))
             :mesoscale-bioregions  (vec (distinct (map #(select-keys % [:provincial-bioregion :mesoscale-bioregion]) imcra-boundaries)))}
     :meow  {:realms     (vec (distinct (map #(select-keys % [:realm]) meow-boundaries)))
             :provinces  (vec (distinct (map #(select-keys % [:realm :province]) meow-boundaries)))
             :ecoregions (vec (distinct (map #(select-keys % [:realm :province :ecoregion]) meow-boundaries)))}}))

(defn valid-boundaries [{:keys [amp imcra meow] :as boundaries}]
  (let [boundary-filter-names  (boundary-filter-names boundaries)
        amp-boundaries         (:boundaries amp)
        imcra-boundaries       (:boundaries imcra)
        meow-boundaries        (:boundaries meow)
        valid-amp-boundaries   (filterv
                                (fn [{:keys [network park zone zone-iucn zone-id]}]
                                  (and
                                   (or (nil? (:network boundary-filter-names)) (= network (:network boundary-filter-names)))
                                   (or (nil? (:park boundary-filter-names)) (= park (:park boundary-filter-names)))
                                   (or (nil? (:zone boundary-filter-names)) (= zone (:zone boundary-filter-names)))
                                   (or (nil? (:zone-iucn boundary-filter-names)) (= zone-iucn (:zone-iucn boundary-filter-names)))
                                   (or (nil? (:zone-id boundary-filter-names)) (= zone-id (:zone-id boundary-filter-names)))))
                                amp-boundaries)
        valid-imcra-boundaries (filterv
                                (fn [{:keys [provincial-bioregion mesoscale-bioregion]}]
                                  (and
                                   (or (nil? (:provincial-bioregion boundary-filter-names)) (= provincial-bioregion (:provincial-bioregion boundary-filter-names)))
                                   (or (nil? (:mesoscale-bioregion boundary-filter-names)) (= mesoscale-bioregion (:mesoscale-bioregion boundary-filter-names)))))
                                imcra-boundaries)
        valid-meow-boundaries  (filterv
                                (fn [{:keys [realm province ecoregion]}]
                                  (and
                                   (or (nil? (:realm boundary-filter-names)) (= realm (:realm boundary-filter-names)))
                                   (or (nil? (:province boundary-filter-names)) (= province (:province boundary-filter-names)))
                                   (or (nil? (:ecoregion boundary-filter-names)) (= ecoregion (:ecoregion boundary-filter-names)))))
                                meow-boundaries)]
    {:amp   {:networks   (vec (distinct (map #(select-keys % [:network]) valid-amp-boundaries)))
             :parks      (vec (distinct (map #(select-keys % [:network :park]) valid-amp-boundaries)))
             :zones      (vec (distinct (map #(select-keys % [:zone]) valid-amp-boundaries)))
             :zones-iucn (vec (distinct (map #(select-keys % [:zone-iucn]) valid-amp-boundaries)))
             :zone-ids   (vec (distinct (map #(select-keys % [:network :park :zone-id]) valid-amp-boundaries)))}
     :imcra {:provincial-bioregions (vec (distinct (map #(select-keys % [:provincial-bioregion]) valid-imcra-boundaries)))
             :mesoscale-bioregions  (vec (distinct (map #(select-keys % [:provincial-bioregion :mesoscale-bioregion]) valid-imcra-boundaries)))}
     :meow  {:realms     (vec (distinct (map #(select-keys % [:realm]) valid-meow-boundaries)))
             :provinces  (vec (distinct (map #(select-keys % [:realm :province]) valid-meow-boundaries)))
             :ecoregions (vec (distinct (map #(select-keys % [:realm :province :ecoregion]) valid-meow-boundaries)))}}))
