(ns email-service.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [email-service.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[email-service started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[email-service has shut down successfully]=-"))
   :middleware wrap-dev})
