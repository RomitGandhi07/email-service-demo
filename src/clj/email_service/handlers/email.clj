(ns email-service.handlers.email
  (:require
   [postal.core :refer [send-message]]
   [clojure.data.json :as json]))


(defn send-email
  [message]
  (println message "Message")
  (let [data (json/read-str (nth message 2))]
    (println data "Here" (get data "to") (get data "name"))
    (cond
      (get data "registration?")
      (println (send-message {:host "smtp.gmail.com"
                              :user "democera@gmail.com"
                              :pass "Password@123"
                              :port 587
                              :tls true}
                             {:from "democera@gmail.com"
                              :to (get data "to")
                              :subject "Hi!"
                              :body (str "Test." (get data "name"))})))))