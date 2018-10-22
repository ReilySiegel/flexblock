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
  (merge {:page                :rooms
          :login/token         ""
          :login/user          {}
          :login/open          false
          :navbar/options-open false
          :navbar/page-zoom    true
          :rooms               []
          :rooms/modal-open    false
          :rooms/time-filter   #{}
          :users/modal-open    false
          :users/password      ""
          :users/role-filter   #{:student :teacher :admin}
          :users               []
          :search              ""
          :date                ""}
         (get-localstorage)))
