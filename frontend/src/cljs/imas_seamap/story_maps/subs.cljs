;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.story-maps.subs)

(defn featured-maps [db _]
  (get-in db [:story-maps :featured-maps]))

(defn featured-map [db _]
  (get-in db [:story-maps :featured-map]))
