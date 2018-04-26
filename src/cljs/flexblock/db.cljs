(ns flexblock.db)

(def room-db
  {:title       ""
   :max-cap     0
   :description ""
   :date        nil
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
   :date           nil
   :reset-password ""
   :add-room       room-db})
