(ns imas-seamap.utils)

;;; Adapted from https://github.com/reagent-project/reagent/wiki/Beware-Event-Handlers-Returning-False
(defmacro handler-fn
  ([& body]
   `(fn [~'event]
      ~@body
      (.preventDefault ~'event)
      (.stopPropagation ~'event)
      nil)))                            ; always return nil
