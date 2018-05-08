(ns flexblock.db)

(def room-db
  {:title       ""
   :max-cap     ""
   :description ""
   :date        nil
   :time        ""
   :room-number ""})

(def user-db
  {:email ""
   :name  ""
   :class ""})

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
   :add-room       room-db
   :add-user       user-db})
