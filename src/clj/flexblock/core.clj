(ns flexblock.core
  "The core namespace for Flexblock.
  This is the first namespace loaded by the Clojure compiler. After
  this namespace, along with any `:require`d by it are loaded, the
  function `-main` is called.

  This namespace is responsible for starting and stopping the various
  components of the Flexblock system. In addition, the `http-server`
  and `repl-server` components are located here."
  (:require [flexblock.handler :as handler]
            [luminus.repl-server :as repl]
            [luminus.http-server :as http]
            [flexblock.config :refer [env]]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [mount.core :as mount])
  (:gen-class))

(def cli-options
  "Describes the command line options that Flexblock can take."
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate ^{:on-reload :noop} http-server
  :start
  (http/start
   (-> env
       (assoc  :handler #'handler/app)
       (update :io-threads #(or % (* 2 (.availableProcessors
                                        (Runtime/getRuntime)))))
       (update :port #(or (-> env :options :port) %))))
  :stop
  (http/stop http-server))

(mount/defstate ^{:on-reload :noop} repl-server
  :start
  (when-let [nrepl-port (env :nrepl-port)]
    (repl/start {:port nrepl-port :handler cider-nrepl-handler}))
  :stop
  (when repl-server
    (repl/stop repl-server)))


(defn stop-app
  "Shut down all of the components.
  Some components need to be explicitly shut down and cleaned up."
  []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app
  "Start all the components."
  [args]
  (doseq [component
          ;; Passes arguments to each component, then starts it.
          (-> args
              (parse-opts cli-options)
              mount/start-with-args
              :started)]
    ;; For each started component, log that the component has started.
    (log/info component "started"))
  ;; Run `stop-app` before Flexblock exits.
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main
  "Starts Flexblock."
  [& args]
  (start-app args))
