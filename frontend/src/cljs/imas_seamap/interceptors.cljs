(ns imas-seamap.interceptors
  (:require [re-frame.interceptor :refer [->interceptor get-effect get-coeffect]]
            [re-frame.loggers :refer [console]]
            [clojure.data :as data]))

(defn debug-excluding
  "An interceptor which logs data about the handling of an event.  A
  straight clone of the re-frame.std-interceptors.debug, with the
  addition that you can specify a number of event keys to be filtered
  from the output.

  Includes a `clojure.data/diff` of the db, before vs after, showing
  the changes caused by the event handler.

  You'd typically want this interceptor after (to the right of) any
  path interceptor.

  Warning:  calling clojure.data/diff on large, complex data structures
  can be slow. So, you won't want this interceptor present in production
  code. See the todomvc example to see how to exclude interceptors from
  production code."
  [& excluded-events]
  (->interceptor
   :id     :debug-excluding
   :before (fn debug-before
             [context]
             (let [event (first (get-coeffect context :event))]
               (when-not (some #{event} excluded-events)
                 (console :log "Handling re-frame event:" event)))
             context)
   :after  (fn debug-after
             [context]
             (let [event   (get-coeffect context :event)
                   orig-db (get-coeffect context :db)
                   new-db  (get-effect   context :db ::not-found)]
               (when-not (some #{(first event)} excluded-events)
                 (if (= new-db ::not-found)
                   (console :log "No :db changes caused by:" event)
                   (let [[only-before only-after] (data/diff orig-db new-db)
                         db-changed?    (or (some? only-before) (some? only-after))]
                     (if db-changed?
                       (do (console :group "db clojure.data/diff for:" event)
                           (console :log "only before:" only-before)
                           (console :log "only after :" only-after)
                           (console :groupEnd))
                       (console :log "no app-db changes caused by:" event)))))
               context))))

(defn analytics-for [events-to-log]
  (let [loggable-events (set events-to-log)]
    (->interceptor
     :id    :generate-analytics
     :after (fn analytics-for-after
              [context]
              (let [event (get-coeffect context :event)]
                (when (loggable-events (first event))
                  ;; FIXME: interact with the javascript ga object
                  ;; here.  Need to determine how we convert re-frame
                  ;; events to those understood by GA, and how to
                  ;; handle args (eg, what layers are enabled, etc)
                  (console :warn "Generating analytics event for" (name (first event))))
                context)))))
