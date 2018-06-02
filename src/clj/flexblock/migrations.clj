(ns flexblock.migrations
  (:require [clojure.java.jdbc :as jdbc]))

(def table-specs
  "Table specifications that can be used with any database.
  Specs for users_rooms are in a separate var, as postgres and h2
  handle foreign key relationships differently."
  [[:users
    [[:id :serial :primary :key]
     [:name "varchar(50)" :not :null]
     [:passwordhash "character(98)" :not :null]
     [:email "varchar(50)" :unique :not :null]
     [:teacher :boolean :not :null]
     [:admin :boolean :not :null]
     [:advisor_id :integer :references "users(id)"]
     [:class :integer]]]
   [:rooms
    [[:id :bigserial :primary :key]
     [:description "varchar(250)" :not :null]
     [:title "varchar(50)" :not :null]
     [:date :date :not :null]
     [:room_number :integer :not :null]
     [:max_capacity :integer :not :null]
     [:time "varchar(6)" :not :null]]]])

(def postgres-table-specs
  "Tables that only work with postgres."
  [:users_rooms
   [[:id :bigserial :primary :key]
    [:users_id :bigint :references "users(id)" :not :null]
    [:rooms_id :bigint :references "rooms(id)" :not :null]]])

(def h2-table-specs
  "Tables that only work with h2."
  [:users_rooms
   [[:id :bigserial :primary :key]
    [:users_id :bigint :not :null]
    [:rooms_id :bigint :not :null]
    [:foreign :key "(users_id)" :references "users(id)"]
    [:foreign :key "(rooms_id)" :references "rooms(id)"]]])

(defn init-tables! [connection]
  (doseq [[table spec] (conj table-specs
                             (if (= "h2:mem" (:dbtype connection))
                               h2-table-specs
                               postgres-table-specs))]
    (try
      (jdbc/db-do-commands
       connection
       [(jdbc/create-table-ddl table spec)])
      (catch Throwable t
        (println
         (:cause (Throwable->map t)))))))

(defn destroy-tables! [connection]
  (doseq [[table] (reverse (conj table-specs
                                 (if (= "h2:mem" (:dbtype connection))
                                   h2-table-specs
                                   postgres-table-specs)))]
    (try
      (jdbc/db-do-commands
       connection
       [(jdbc/drop-table-ddl table)])
      (catch Throwable t))))
