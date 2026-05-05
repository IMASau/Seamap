;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.natural-hazards-atlas.subs
  (:require [imas-seamap.utils :refer [first-where]]))

(defn current-view-models
  "List of scientific models available to analyze the hazard data"
  [db _]
  (get-in db [:current-view :models]))

(defn current-view-scenarios
  "List of scenarios available to analyze the hazard data"
  [db _]
  (get-in db [:current-view :scenarios]))

(defn current-view-seasonal-datas
  "List of seasonal data available to analyze the hazard data"
  [db _]
  (get-in db [:current-view :seasonal-datas]))

(defn current-view-selected-model
  "Scientific model to analyze the hazard data"
  [db _]
  (let [models            (get-in db [:current-view :models])
        selected-model-id (get-in db [:current-view :selected-model-id])
        selected-model    (first-where #(= (:id %) selected-model-id) models)]
    (when selected-model-id
      (assert selected-model (str "Selected model id " selected-model-id " not found in models list")))
    selected-model))

(defn current-view-selected-scenario
  "Scenario to analyze the hazard data"
  [db _]
  (let [scenarios               (get-in db [:current-view :scenarios])
        selected-scenario-id    (get-in db [:current-view :selected-scenario-id])
        selected-scenario       (first-where #(= (:id %) selected-scenario-id) scenarios)]
    (when selected-scenario-id
      (assert selected-scenario (str "Selected scenario id " selected-scenario-id " not found in scenarios list")))
    selected-scenario))

(defn current-view-selected-seasonal-data
  "Seasonal data to analyze the hazard data"
  [db _]
  (let [seasonal-datas               (get-in db [:current-view :seasonal-datas])
        selected-seasonal-data-id    (get-in db [:current-view :selected-seasonal-data-id])
        selected-seasonal-data       (first-where #(= (:id %) selected-seasonal-data-id) seasonal-datas)]
    (when selected-seasonal-data-id
      (assert selected-seasonal-data (str "Selected seasonal data id " selected-seasonal-data-id " not found in seasonal datas list")))
    selected-seasonal-data))
