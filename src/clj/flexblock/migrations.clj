(ns flexblock.migrations
  (:require [clojure.java.jdbc :as jdbc]
            [flexblock.db :as db]))

(defn reset!
  "Sets up database tables.
  WILL DELETE ALL DATA IN THE DATABASE!"
  []
  (jdbc/db-do-commands
   db/db
   [(jdbc/drop-table-ddl :users_rooms)
    (jdbc/drop-table-ddl :users)
    (jdbc/drop-table-ddl :rooms)
    (jdbc/create-table-ddl
     :users
     [[:id :integer :primary :key :not :null]
      [:name "varchar(50)" :not :null]
      [:passwordhash "character(98)" :not :null]
      [:email "varchar(50)" :unique :not :null]
      [:teacher :boolean :not :null]
      [:advisor "varchar(50)"]])
    (jdbc/create-table-ddl
     :rooms
     [[:id :bigint :primary :key :not :null]
      [:description "varchar(250)" :not :null]
      [:title "varchar(50)" :not :null]
      [:date "varchar(10)" :not :null]
      [:room_number :integer :not :null]
      [:max_capacity :integer :not :null]
      [:time "varchar(6)" :not :null]])
    (jdbc/create-table-ddl
     :users_rooms
     [[:id :bigint :primary :key :not :null]
      [:users_id :bigint :references "users(id)" :not :null]
      [:rooms_id :bigint :references "rooms(id)" :not :null]])]))
