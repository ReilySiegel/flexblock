(ns flexblock.db
  (:require [cljs.reader :as reader]))

(def room-db
  {:title       ""
   :max-cap     ""
   :description ""
   :date        nil
   :time        ""
   :room-number ""})

(def user-db
  {:email   ""
   :name    ""
   :class   nil
   :teacher false
   :admin   false})

(defn set-localstorage!
  "Sets the LocalStorage key 'flexblock' with the value of (prn-str x)."
  [x]
  (-> js/window
      .-localStorage
      (.setItem "flexblock" (prn-str x))))

(defn get-localstorage
  "Gets the key 'flexblock' from LocalStorage, and reads it as EDN."
  []
  (-> js/window
      .-localStorage
      (.getItem "flexblock")
      (reader/read-string)))

(def default-db
  (merge {:page           :rooms
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
          :add-user       user-db}
         (get-localstorage)))
