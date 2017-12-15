;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.utils)

;;; Adapted from https://github.com/reagent-project/reagent/wiki/Beware-Event-Handlers-Returning-False
(defmacro handler-fn
  ([& body]
   `(fn [~'event]
      ~@body
      (.preventDefault ~'event)
      (.stopPropagation ~'event)
      nil)))                            ; always return nil

;;; and a version optimised for our most common case
(defmacro handler-dispatch
  ([dispatch-v]
   `(fn [~'event]
      (re-frame.core/dispatch ~dispatch-v)
      (.preventDefault ~'event)
      (.stopPropagation ~'event)
      nil)))
