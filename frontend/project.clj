(defproject imas-seamap "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha17"]
                 [org.clojure/clojurescript "1.9.562"]
                 [cljs-ajax "0.5.5"]
                 [org.clojure/data.xml "0.2.0-alpha2"]
                 [org.clojure/data.zip "0.1.2"]
                 [org.clojure/test.check "0.9.0"]
                 [figwheel-sidecar "0.5.10" :exclusions [ring/ring-core]]
                 [com.cemerick/piggieback "0.2.1"]
                 [binaryage/oops "0.5.5"]
                 [reagent "0.6.0"]
                 [re-frame "0.9.1"]
                 [day8.re-frame/http-fx "0.1.3" :exclusions [cljs-ajax]]
                 [philoskim/debux "0.2.1"]]

  ;; Managed using create-react-app:
  ;; TODO: snaffle the externs from these (and Leaflet) and include
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
  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.4"]
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
                    :closure-defines      {goog.DEBUG true}
                    :preloads             [devtools.preload
                                           imas-seamap.specs.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}

    {:id           "min"
     :source-paths ["src/cljs" "resources/ext"]
     :compiler     {:main            imas-seamap.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :output-dir      "resources/public/js/compiled/app"
                    :source-map      "resources/public/js/compiled/app.js.map"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pseudo-names    false
                    :pretty-print    false}}]})
