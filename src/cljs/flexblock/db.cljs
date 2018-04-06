(ns flexblock.db)

(def room-db
  {:title       ""
   :max-cap     0
   :description ""
   :date        ""
   :time        ""
   :room-number 0})

(def default-db
  {:page           :rooms
   :token          ""
   :user           {}
   :login          {:username ""
                    :password ""}
   :rooms          []
   :users          []
   :loading        0
   :search         ""
   :date           ""
   :reset-password ""
   :add-room       room-db})
