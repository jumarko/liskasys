(ns liskasys.endpoint.example
  (:require [compojure.core :refer :all]
            [clojure.java.io :as io]))

(defn example-endpoint [{{db :spec} :db}]
  (context "/example" []
    (GET "/" []
      (io/resource "liskasys/endpoint/example/example.html"))))
