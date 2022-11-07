;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.state-of-knowledge.utils
  (:require #_[debux.cs.core :refer [dbg] :include-macros true]))

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
    {:network              (:name active-network)
     :park                 (:name active-park)
     :zone                 (:name active-zone)
     :zone-iucn            (:name active-zone-iucn)
     :zone-id              (:name active-zone-id)
     :provincial-bioregion (:name active-provincial-bioregion)
     :mesoscale-bioregion  (:name active-mesoscale-bioregion)
     :realm                (:name active-realm)
     :province             (:name active-province)
     :ecoregion            (:name active-ecoregion)}))

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
