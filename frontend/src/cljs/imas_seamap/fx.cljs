;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.fx
  (:require [clojure.string :as string]
            [imas-seamap.utils :refer [parse-state]]
            [re-frame.core :as re-frame]))

(defn set-location-anchor [anchor]
  (set! js/location -hash anchor))

(re-frame/reg-fx :put-hash set-location-anchor)


(defn cofx-hash-state [cofx _]
  (let [hash-val (. js/location -hash)]
    (merge cofx
           (when-not (string/blank? hash-val)
             {:hash-state (parse-state (subs hash-val 1))}))))

(re-frame/reg-cofx :hash-state cofx-hash-state)
