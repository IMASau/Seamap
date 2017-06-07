(ns imas-seamap.dev-handlers
  (:require [compojure.core :refer [defroutes GET]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.util.response :as response]
            [tailrecursion.ring-proxy :refer [wrap-proxy]]))

(defroutes app-routes
  ;; NOTE: this will deliver all of your assets from the public directory
  ;; of resources i.e. resources/public
  (route/resources "/" {:root "public"})
  ;; NOTE: this will deliver your index.html
  #_(GET "/" [] (-> (response/resource-response "index.html" {:root "public"})
                  (response/content-type "text/html")))
  (GET "/hello" [] "Hello World there!")
  (route/not-found "Not Found"))


(def app (-> app-routes
             (wrap-defaults site-defaults)
             (wrap-proxy "/api" "http://localhost:8000/api")
             (wrap-proxy "/pq" "http://rls.tpac.org.au/pq")
             (wrap-proxy "/static" "http://localhost:8000/static")))
