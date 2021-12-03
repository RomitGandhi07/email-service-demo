(ns email-service.core
  (:require
   [email-service.handler :as handler]
   [email-service.nrepl :as nrepl]
   [luminus.http-server :as http]
   [email-service.config :refer [env]]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.tools.logging :as log]
   [mount.core :as mount]
   [taoensso.carmine :as car :refer [wcar]]
   [email-service.handlers.email :refer [send-email]]
   [chime.core :as chime])
  (:import (java.time Instant Duration))
  (:gen-class))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread ex]
      (log/error {:what :uncaught-exception
                  :exception ex
                  :where (str "Uncaught exception on" (.getName thread))}))))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate ^{:on-reload :noop} http-server
  :start
  (http/start
    (-> env
        (update :io-threads #(or % (* 2 (.availableProcessors (Runtime/getRuntime))))) 
        (assoc  :handler (handler/app))
        (update :port #(or (-> env :options :port) %))
        (select-keys [:handler :host :port])))
  :stop
  (http/stop http-server))

(mount/defstate ^{:on-reload :noop} repl-server
  :start
  (when (env :nrepl-port)
    (nrepl/start {:bind (env :nrepl-bind)
                  :port (env :nrepl-port)}))
  :stop
  (when repl-server
    (nrepl/stop repl-server)))


(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(def server1-conn {:pool {} :spec {:uri "redis://redistogo:4f77e548d5905b50fc71f55cb2c2a6e5@sole.redistogo.com:9441/"}})
;; (defmacro wcar* [& body] `(car/wcar server1-conn ~@body))

(def listener
  (car/with-new-pubsub-listener (:spec server1-conn)
    {"email" (fn f1 [msg] (send-email msg))
     "foo*"   (fn f2 [msg] (println "Pattern match: " msg))}
    (car/subscribe  "email" "foobaz")))

(defn cron-job
  []
  ;(let [now (Instant/now)]
  ;  (chime/chime-at [(.plusSeconds now 2)
  ;                   (.plusSeconds now 4)]
  ;                  (fn [time]
  ;                    (println "Chiming at" time))))
  (-> (chime/periodic-seq (Instant/now) (Duration/ofSeconds 10))
      (chime/chime-at (fn [time]
                        (println "Checking at 1 " time))))

  (-> (chime/periodic-seq (Instant/now) (Duration/ofSeconds 15))
      (chime/chime-at (fn [time]
                        (println "Checking at 2 " time)))))

(defn -main [& args]
  (start-app args)
  ;;(cron-job)
  )
