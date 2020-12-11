(ns api.db
  (:require [clojure.repl :refer [demunge]]
            [hugsql.adapter.next-jdbc :as next-adapter]
            [hugsql.core :as hugsql]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

(def transform-result-keys-fn (comp keyword demunge))

(defn demunge-result-set-builder [rs opts]
  (rs/as-unqualified-modified-maps rs (assoc opts :label-fn transform-result-keys-fn)))

(defn init
  "Returns a datasource"
  [db-opts]

  (hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc {:builder-fn demunge-result-set-builder}))

  ;; TODO: Important add a pool here!!!!
  ;; When next.jdbc is given a datasource, it creates a java.sql.Connection from it,
  ;; uses it for the SQL operation (by creating and populating a java.sql.PreparedStatement from
  ;; the connection and the SQL string and parameters passed in), and then closes it.
  ;; If you're not using a connection pooling datasource (see below),
  ;; that can be quite an overhead: setting up database connections to remote servers is not cheap!

  (jdbc/get-datasource (merge {:dbtype "mysql"}
                              db-opts)))
