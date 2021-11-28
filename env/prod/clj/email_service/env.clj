(ns email-service.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[email-service started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[email-service has shut down successfully]=-"))
   :middleware identity})
