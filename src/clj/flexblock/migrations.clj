(ns flexblock.migrations
  (:require [clojure.java.jdbc :as jdbc]))

(def table-specs
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
     [:time "varchar(6)" :not :null]]]
   [:users_rooms
    [[:id :bigserial :primary :key]
     [:users_id :bigint :references "users(id)" :not :null]
     [:rooms_id :bigint :references "rooms(id)" :not :null]]]])

(defn init-tables! [connection]
  (for [[table spec] table-specs]
    (try
      (jdbc/db-do-commands
       connection
       [(jdbc/create-table-ddl table spec)])
      (catch Throwable t))))

(defn destroy-tables! [connection]
  (for [[table] (reverse table-specs)]
    (try
      (jdbc/db-do-commands
       connection
       [(jdbc/drop-table-ddl table)])
      (catch Throwable t))))
