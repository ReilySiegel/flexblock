(defproject flexblock "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[buddy "2.0.0"]
                 [cider/cider-nrepl "0.15.1"]
                 [clj-time "0.14.2"]
                 [cljs-ajax "0.7.3"]
                 [compojure "1.6.0"]
                 [cprop "0.1.11"]
                 [funcool/struct "1.2.0"]
                 [luminus-immutant "0.2.4"]
                 [luminus-nrepl "0.1.4"]
                 [luminus/ring-ttl-session "0.3.2"]
                 [markdown-clj "1.0.2"]
                 [metosin/ring-http-response "0.9.0"]
                 [mount "0.1.11"]
                 [org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]
                 [org.clojure/tools.cli "0.3.5"]
                 [org.clojure/tools.logging "0.4.0"]
                 [org.clojure/tools.reader "1.2.1"]
                 [org.webjars.bower/tether "1.4.3"]
                 [org.webjars/bootstrap "4.0.0"]
                 [org.webjars/font-awesome "5.0.6"]
                 [re-frame "0.10.4"]
                 [reagent "0.7.0"]
                 [reagent-utils "0.2.1"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.6.3"]
                 [ring/ring-defaults "0.3.1"]
                 [secretary "1.2.3"]
                 [selmer "1.11.6"]
                 [korma "0.4.3"]
                 [org.clojure/java.jdbc "0.7.6"]
                 [org.postgresql/postgresql "42.1.3.jre7"]
                 [com.h2database/h2 "1.4.197"]
                 [org.clojure/core.async "0.4.474"]
                 [log4j "1.2.15" :exclusions [javax.mail/mail
                                              javax.jms/jms
                                              com.sun.jdmk/jmxtools
                                              com.sun.jmx/jmxri]]
                 [clj-fuzzy "0.4.1"]
                 [criterium "0.4.4"]
                 [com.draines/postal "2.0.2"]
                 [day8.re-frame/http-fx "0.1.6"]
                 [phrase "0.3-SNAPSHOT"]
                 [java-jdbc/dsl "0.1.3"]
                 [honeysql "0.9.2"]
                 [clj-time "0.14.3"]
                 [metosin/spec-tools "0.5.1"]
                 [metosin/compojure-api "2.0.0-SNAPSHOT"]
                 [metosin/ring-swagger-ui "3.9.0"]
                 [metosin/ring-swagger "0.26.0"]]

  :min-lein-version "2.0.0"

  :source-paths ["src/clj" "src/cljc"]
  :test-paths ["test/clj" "test/cljc"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot flexblock.core

  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-immutant "2.1.0"]
            [lein-cloverage "1.0.10"]
            [jonase/eastwood "0.2.6"]
            [lein-kibit "0.1.6"]
            [nightlight/lein-nightlight "RELEASE"]]
  :clean-targets ^{:protect false}
  [:target-path [:cljsbuild :builds :app :compiler :output-dir] [:cljsbuild :builds :app :compiler :output-to]]
  :figwheel
  {:http-server-root "public"
   :nrepl-port       7002
   :css-dirs         ["resources/public/css"]
   :nrepl-middleware
   [cemerick.piggieback/wrap-cljs-repl cider.nrepl/cider-middleware]}


  :profiles
  {:uberjar {:omit-source true
             :prep-tasks  ["compile" ["cljsbuild" "once" "min"]]
             :cljsbuild
             {:builds
              {:min
               {:source-paths ["src/cljc" "src/cljs" "env/prod/cljs"]
                :compiler
                {:output-dir    "target/cljsbuild/public/js"
                 :output-to     "target/cljsbuild/public/js/app.js"
                 :source-map    "target/cljsbuild/public/js/app.js.map"
                 :optimizations :whitespace
                 :pretty-print  false
                 :closure-warnings
                 {:externs-validation :off :non-standard-jsdoc :off}
                 :externs       ["react/externs/react.js"]}}}}


             :aot            :all
             :uberjar-name   "flexblock.jar"
             :source-paths   ["env/prod/clj"]
             :resource-paths ["env/prod/resources"]}

   :dev  [:project/dev :profiles/dev]
   :test [:project/dev :project/test :profiles/test]

   :ci {:local-repo ".m2"}

   :project/dev {:jvm-opts     ["-server" "-Dconf=dev-config.edn"]
                 :dependencies [[binaryage/devtools "0.9.9"]
                                [com.cemerick/piggieback "0.2.2"]
                                [doo "0.1.8"]
                                [figwheel-sidecar "0.5.14"]
                                [pjstadig/humane-test-output "0.8.3"]
                                [prone "1.5.0"]
                                [re-frisk "0.5.3"]
                                [ring/ring-devel "1.6.3"]
                                [ring/ring-mock "0.3.2"]]
                 :plugins      [[com.jakemccrary/lein-test-refresh "0.19.0"]
                                [lein-doo "0.1.8"]
                                [lein-figwheel "0.5.14"]
                                [org.clojure/clojurescript "1.9.946"]]
                 :cljsbuild
                 {:builds
                  {:app
                   {:source-paths ["src/cljs" "src/cljc" "env/dev/cljs"]
                    :figwheel     {:on-jsload "flexblock.core/mount-components"}
                    :compiler
                    {:main          "flexblock.app"
                     :asset-path    "/js/out"
                     :output-to     "target/cljsbuild/public/js/app.js"
                     :output-dir    "target/cljsbuild/public/js/out"
                     :source-map    true
                     :optimizations :none
                     :pretty-print  true
                     :preloads      [re-frisk.preload]}}}}



                 :doo            {:build "test"}
                 :source-paths   ["env/dev/clj"]
                 :resource-paths ["env/dev/resources"]
                 :repl-options   {:init-ns user}
                 :injections     [(require 'pjstadig.humane-test-output)
                                  (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts       ["-server" "-Dconf=test-config.edn"]
                  :resource-paths ["env/test/resources"]
                  :cljsbuild
                  {:builds
                   {:test
                    {:source-paths ["src/cljc"
                                    "src/cljs"
                                    ;; Dont test cljc in cljs.
                                    ;; "test/cljc"
                                    "test/cljs"]
                     :compiler
                     {:output-to     "target/test.js"
                      :main          "flexblock.doo-runner"
                      :optimizations :whitespace
                      :pretty-print  true}}}}

                  }
   :profiles/dev  {}
   :profiles/test {}})
