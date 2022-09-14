;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.story-maps.subs)

(defn featured-maps [db _]
  (get-in db [:story-maps :featured-maps]))

(defn featured-map [db _]
  (get-in db [:story-maps :featured-map]))

(defn featured-map-open?
  "There's a reason that we separately track the drawer open/closed state; if we
   just told the drawer to be open when the featured map was not nil, then when we
   erase the featured map the drawer would have errors as it's closing saying it
   has no title/content to display while in the closing state."
  [db _]
  (get-in db [:story-maps :open?]))
