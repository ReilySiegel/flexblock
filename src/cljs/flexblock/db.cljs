(ns flexblock.db)

(def room-db
  {:title       ""
   :max-cap     ""
   :description ""
   :date        nil
   :time        ""
   :room-number ""})

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
