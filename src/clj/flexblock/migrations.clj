(ns flexblock.migrations
  (:require [clojure.java.jdbc :as jdbc]))

(def table-specs
  "Table specifications for the database."
  {:users       [[:id :serial :primary :key]
                 [:name "varchar(50)" :not :null]
                 [:passwordhash "character(98)" :not :null]
                 [:email "varchar(50)" :unique :not :null]
                 [:teacher :boolean :not :null]
                 [:admin :boolean :not :null]
                 [:advisor_id :integer
                  :references "users(id)" :on :delete :set :null]
                 [:class :integer]]
   :rooms       [[:id :bigserial :primary :key]
                 [:description "varchar(250)" :not :null]
                 [:title "varchar(50)" :not :null]
                 [:date :date :not :null]
                 [:room_number :integer :not :null]
                 [:max_capacity :integer :not :null]
                 [:time "varchar(6)" :not :null]]
   :users_rooms [[:id :bigserial :primary :key]
                 [:users_id :bigint :not :null :references "users(id)"]
                 [:rooms_id :bigint :not :null :references "rooms(id)"]]})

(defn init-tables! [connection]
  (doseq [[table spec] table-specs]
    (try
      (jdbc/db-do-commands
       connection
       [(jdbc/create-table-ddl table spec)])
      (catch Throwable t
        (println
         (:cause (Throwable->map t)))))))

(defn destroy-tables! [connection]
  (doseq [[table] table-specs]
    (try
      (jdbc/db-do-commands
       connection
       [(jdbc/drop-table-ddl table)])
      (catch Throwable t))))
