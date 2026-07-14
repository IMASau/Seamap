;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2026, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.interop.dom-to-image
  (:require
   ["dom-to-image" :as dom-to-image]
   ["file-saver" :as file-saver]
   [clojure.string :as str]))

(defn element-to-png
  "Interop function with `dom-to-image` to convert the a DOM node into a PNG image that is then downloaded.
   The first DOM node matching `selectors` is converted.

   Args:
   * `selectors: string`: JavaScript query selector for the DOM node."
  [selectors file-name]
  (let [element (js/document.querySelector selectors)]
    (.then
     (dom-to-image/toPng element)
     (fn [data-url]
       (let [[_ mime-type _ data] (str/split data-url #"[:;,]")
             binary-data (js/atob data)
             bytes       (js/Uint8Array.from binary-data #(.charCodeAt % 0))
             blob (js/Blob. #js[bytes] #js{:type mime-type})]
         (file-saver/saveAs blob file-name))))))
