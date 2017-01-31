(defproject imas-seamap "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-alpha10"]
                 [org.clojure/clojurescript "1.9.229"]
                 [figwheel-sidecar "0.5.4-7"]
                 [com.cemerick/piggieback "0.2.1"]
                 [reagent "0.6.0" :exclusions [cljsjs/react]]
                 [re-frame "0.9.1"]
                 [cljsjs/react-with-addons "15.4.2-1"]
                 [cljsjs/react-leaflet "0.12.3-2"]
                 [re-frisk "0.3.2"]
                 [philoskim/debux "0.2.1"]]

  :plugins [[lein-cljsbuild "1.1.4"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:http-server-root "public"
             :server-port      3451
             :nrepl-port       7892
             :css-dirs         ["resources/public/css"]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.8.2"]]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "imas-seamap.core/mount-root"}
     :compiler     {:main                 imas-seamap.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            imas-seamap.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})
