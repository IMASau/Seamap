(ns imas-seamap.events
  (:require [ajax.core :as ajax]
            [imas-seamap.db :as db]
            [re-frame.core :as re-frame]
            [debux.cs.core :refer-macros [dbg]]))

;;; TODO: maybe pull in extra config inject as page config, but that
;;; may not be feasible with a wordpress host
(defn initialise-db [_ _] db/default-db)

(defn initialise-layers [db _]
  (let [layer-url ""]
    (re-frame/dispatch [:ajax layer-url
                        {:handler :map/update-layers}])
    db))


(defn ajax [db [_ url {:keys [handler err-handler override-opts]
                       :or   {handler     :ajax/default-success-handler
                              err-handler :ajax/default-err-handler}
                       :as opts}]]
  (ajax/GET url
            (merge
             {:handler #(re-frame/dispatch [handler %])
              :error-handler #(re-frame/dispatch [err-handler %])
              :response-format :json :keywords? true}
             override-opts))
  db)

(defn default-success-handler [db [_ response]]
  (js/console.info "Received AJAX response" response)
  db)

(defn default-err-handler [db [_ response]]
  (js/console.info "AJAX error response" response)
  db)
