;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(defproject imas-seamap "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript "1.9.671"]
                 [cljs-ajax "0.5.5"]
                 [org.clojure/data.xml "0.2.0-alpha2"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/test.check "0.9.0"]
                 [figwheel-sidecar "0.5.18" :exclusions [ring/ring-core]]
                 [cider/piggieback "0.4.0"]
                 [com.cemerick/url "0.1.1"]
                 [binaryage/oops "0.5.5"]
                 [reagent "0.6.0"]
                 [re-frame "0.9.1"]
                 [day8.re-frame/async-flow-fx "0.0.7"]
                 [day8.re-frame/http-fx "0.1.3" :exclusions [cljs-ajax]]
                 [com.smxemail/re-frame-cookie-fx "0.0.2"]
                 [philoskim/debux "0.3.1"]]

  ;; Managed using create-react-app:
  :exclusions [cljsjs/react
               cljsjs/react-dom
               cljsjs/react-dom-server]

  :plugins [[lein-cljsbuild "1.1.4"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:http-server-root "public"
             :server-port      3451
             :nrepl-port       7892
             :ring-handler     imas-seamap.dev-handlers/app
             :css-dirs         ["resources/public/css"]}
  :repl-options {:nrepl-middleware [cider.piggieback/wrap-cljs-repl]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.4"]
                   [cljsjs/d3 "4.3.0-5"]
                   [day8.re-frame/trace "0.1.10"]
                   [tailrecursion/ring-proxy "2.0.0-SNAPSHOT" :exclusions [commons-codec commons-io]]
                   [ring "1.5.1"]
                   [potemkin "0.4.3"]   ; Was getting a weird error with the ring-proxy dependency version
                   [ring/ring-defaults "0.2.1"]
                   [compojure "1.5.0" :exclusions [ring/ring-codec commons-codec]]
                   [spootnik/globber "0.4.1"]
                   [org.clojure/data.json "0.2.6"]
                   [org.osgeo/proj4j "0.1.0"]]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs" "src/clj"]
     :figwheel     {:on-jsload "imas-seamap.core/figwheel-reload"}
     :compiler     {:main                 imas-seamap.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :closure-defines      {goog.DEBUG true
                                           "re_frame.trace.trace_enabled_QMARK_" true}
                    :preloads             [devtools.preload
                                           day8.re-frame.trace.preload
                                           imas-seamap.specs.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}

    {:id           "min"
     :source-paths ["src/cljs" "src/clj"]
     :compiler     {:main            imas-seamap.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :output-dir      "resources/public/js/compiled/app"
                    :source-map      "resources/public/js/compiled/app.js.map"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG                   false
                                      imas-seamap.db/api-url-base "https://data.imas.utas.edu.au/seamap/api/"
                                      imas-seamap.db/img-url-base "/app/img/"}
                    :pseudo-names    false
                    :pretty-print    false}}]})
