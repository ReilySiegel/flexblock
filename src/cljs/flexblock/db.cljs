(ns flexblock.db
  (:require [cljs.reader :as reader]))

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

(defn default-db []
  (merge {:page              :rooms
          :token             ""
          :user              {}
          :rooms             []
          :rooms/time-filter #{}
          :users/role-filter #{:student :teacher :admin}
          :users             []
          :search            ""
          :date              nil}
         (get-localstorage)))
