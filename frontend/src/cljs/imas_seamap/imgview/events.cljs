(ns imas-seamap.imgview.events
  (:require [ajax.core :as ajax]))

(defn load-survey [_ [_ survey-id]]
  {:http-xhrio {:method :get
                ;; Hackish; we could get the root endpoint and find
                ;; the URL for the id, but this will do for now:
                :uri (str "http://rls.tpac.org.au/pq/" survey-id)
                :response-format (ajax/json-response-format {:keywords? true})
                :on-success [:imgview/on-load-survey]
                :on-failure [:ajax/default-err-handler]}})

(defn on-load-survey [db [_ response]]
  (assoc-in db [:survey :rls] response))
