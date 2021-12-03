(ns email-service.routes.services
  (:require
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [email-service.middleware.formats :as formats]
    [ring.util.http-response :refer :all]
    [clojure.java.io :as io]
    [postal.core :refer [send-message]]
    [clj-pdf.core :refer [pdf]]
    [failjure.core :as f]
    [miner.ftp :as ftp]
    [clj-http.client :as client]
    ;;[clojure.zip :as zip]
    ;[clojure.xml :as xml]
    [clojure.data.xml :as xml]
    [clojure.data.zip.xml :as zip-xml]
    [clojure.zip :as zip]
    [hiccup.core :refer [html]]))

(defn send-email
  [to]
  (send-message {:host "smtp.gmail.com"
                 :user "democera@gmail.com"
                 :pass "Password@123"
                 :port 587
                 :tls true}
                {:from "democera@gmail.com"
                 :to to
                 :subject "Hi!"
                 :body "Test."}))

(defn generate-pdf
  [_]
  (try
    (pdf
      [{}
       [:list {:roman true}
        [:chunk {:style :bold} "a bold item"]
        "another item"
        "yet another item"]
       [:phrase "some text"]
       [:phrase "some more text"]
       [:paragraph "yet more text"]
       [:chart
        {:type "bar-chart"
         :title "Bar Chart"
         :background [10 100 40]
         :x-label "Items"
         :y-label "Quality"}
        [2 "Foo"] [4 "Bar"] [10 "Baz"]]]
      "resources/temp/doc.pdf")
    {:status 200
     :headers {"Content-Type" "application/pdf"}
     :body (clojure.java.io/file "resources/temp/doc.pdf")}
    (catch Exception e
      (internal-server-error {:error "Error"}))
    (finally
      (println "In Finally")
      ;(clojure.java.io/delete-file "resources/temp/doc.pdf")
      ))
  )

(defn upload-file
  [{{{:keys [file]} :multipart} :parameters}]
  (pdf
    [{}
     [:list {:roman true}
      [:chunk {:style :bold} "a bold item"]
      "another item"
      "yet another item"]
     [:phrase "some text"]
     [:phrase "some more text"]
     [:paragraph "yet more text"]
     [:chart
      {:type "bar-chart"
       :title "Bar Chart"
       :background [10 100 40]
       :x-label "Items"
       :y-label "Quality"}
      [2 "Foo"] [4 "Bar"] [10 "Baz"]]]
    "resources/temp/doc.pdf")
  ;(ftp/with-ftp [client "ftp://user_xryg02ui:demo1234@push-12.cdn77.com/www/resources"]
  ;              (ftp/client-put client "resources/temp/doc.pdf"))

  ;(ftp/with-ftp [client "ftp://user_xryg02ui:demo1234@push-12.cdn77.com/www/resources"]
  ;              (ftp/client-put client (:tempfile file)))


  ;(clojure.pprint/pprint (ftp/list-files "ftp://user_xryg02ui:Demo@123@push-12.cdn77.com"))
  ;(clojure.pprint/pprint (ftp/list-directories "ftp://user_xryg02ui:demo1234@push-12.cdn77.com"))

  ;(ftp/with-ftp [client "ftp://user_xryg02ui:demo1234@push-12.cdn77.com/www/resources"]
  ;              (ftp/client-get client "doc.pdf" "doc-get.pdf"))
  ;(clojure.pprint/pprint (format "/resources/temp/%s" (:filename file)))
  ;(createNewFile. (io/file (:filename file)))
  ;(io/copy (:tempfile file) (io/file (:filename file)))
  {:status 200
   :body {:name "Ok"
          :size 156}})

(defn generate-education
  []
  [:Education
   [:ContactEducation
    [:Degree "Bachelors"]]])

(defn generate-xml
  []
  (html
    [:SaveContactRestRequest {:xmlns "http://schemas.datacontract.org/2004/07/SmashFly.WebServices.ContactManagerService.v2"}
     [:Contact
      [:Address1 "1 Main Street"]
      [:Address2 "Unit 2"]
      [:City "Boston"]
      [:Company "3COM"]
      (generate-education)]]))

(defn send-xml-response
  [_]
  {:status 200
   :headers {"Content-Type" "application/xml"}
   :body (generate-xml)})

(defn zip-str
  [xmlString]
  (zip/xml-zip (xml/parse (java.io.StringReader. xmlString)))
  ;(zip/xml-zip
  ;  (xml/parse (java.io.ByteArrayInputStream. (.getBytes xmlString))))
  )

(defn process-xml
  [_]
  (let [response (-> "http://localhost:3001/api/xmlresponse"
                     (client/get)
                     (:body)
                     (zip-str))
        data (-> (zip-xml/xml-> response
                                :SaveContactRestRequest
                                :Contact
                                :Address1)
                 (first)
                 (first)
                 (:content)
                 (first))]
   (clojure.pprint/pprint data))
  {:status 200
   :message {:total 1}})

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["/ping"
    {:get (constantly (ok {:message "pong"}))}]
   
   ["/email"
    {:post {:summary "Send Email"
            :parameters {:body {:to string?}}
            :responses {200 {:body {:message pos-int?}}}
            :handler (fn [{{{:keys [to]} :body} :parameters}]
                       (send-email to)
                       {:status 200
                        :body {:message 1}})}}]

   ["/pdf"
    {:get {:summary "Get Email"
           :handler generate-pdf}}]

   ["/xmlresponse"
    {:get {:summary "Send XML Response"
           :handler send-xml-response}}]

   ["/processXML"
    {:get {:summary "Process XML"
           :responses {200 {:body {:total pos-int?}}}
           :handler process-xml}}]
   

   ["/math"
    {:swagger {:tags ["math"]}}

    ["/plus"
     {:get {:summary "plus with spec query parameters"
            :parameters {:query {:x int?, :y int?}}
            :responses {200 {:body {:total pos-int?}}}
            :handler (fn [{{{:keys [x y]} :query} :parameters}]
                       {:status 200
                        :body {:total (+ x y)}})}
      :post {:summary "plus with spec body parameters"
             :parameters {:body {:x int?, :y int?}}
             :responses {200 {:body {:total pos-int?}}}
             :handler (fn [{{{:keys [x y]} :body} :parameters}]
                        {:status 200
                         :body {:total (+ x y)}})}}]]

   ["/files"
    {:swagger {:tags ["files"]}}

    ["/upload"
     {:post {:summary "upload a file"
             :parameters {:multipart {:file multipart/temp-file-part}}
             :responses {200 {:body {:name string?, :size int?}}}
             :handler upload-file}}]

    ["/download"
     {:get {:summary "downloads a file"
            :swagger {:produces ["image/png"]}
            :handler (fn [_]
                       {:status 200
                        :headers {"Content-Type" "image/png"}
                        :body (-> "public/img/warning_clojure.png"
                                  (io/resource)
                                  (io/input-stream))})}}]]])
