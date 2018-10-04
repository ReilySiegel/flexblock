(ns flexblock.routes.rooms
  (:require [flexblock.db :as db]
            [flexblock.rooms :as rooms]
            [clojure.spec.alpha :as s]
            [ring.util.http-response :as response]
            [flexblock.users :as users]))

(def routes
  [[""
    {:get  {:summary   "Get all rooms."
            :responses {200 {:body (s/coll-of ::rooms/room-hydrated)}}
            :handler   (fn [_]
                         (response/ok (vec (db/get-rooms))))}
     :post {:summary "Add a room."
            :handler (fn [{room   :body-params
                           master :identity}]
                       (db/insert-room! room (:id master))
                       (response/ok))}}]

   ["/attendance"
    {:get {:summary     "Get attendance for all rooms."
           :description "Get a map mapping [user-id room-id] -> attendance."
           :responses   {200 {:body (s/map-of
                                     ::rooms/attendance
                                     ::rooms/attendance)}}
           :handler     (fn [request]
                          (response/ok (into {} (db/get-attendance))))}}]
   ["/:id"
    {:parameters {:path {:id ::rooms/id}}
     :delete     {:summary "Delete a room."
                  :handler (fn [{{{:keys [id]} :path} :parameters
                                 master               :identity}]
                             (db/delete-room! id (:id master))
                             (response/ok))}}]
   ;; Define a second "/:id", so that the delete handler is not cloned
   ;; to children.
   ["/:id"
    {:parameters {:path {:id ::rooms/id}}}
    ["/join"
     {:post {:summary "Join a room."
             :handler (fn [{{{:keys [id]} :path} :parameters
                            master               :identity}]
                        (db/join-room! id (:id master))
                        (response/ok))}}]
    ["/leave"
     {:post {:summary "Leave a room."
             :handler (fn [{{{:keys [id]} :path} :parameters
                            master               :identity}]
                        (db/leave-room! id (:id master))
                        (response/ok))}}]
    ["/attendance"
     {:parameters {:body {:id ::users/id}}
      :patch      {:summary "Set the attendance of a user."
                   :handler (fn [{{{:keys [id]}  :path
                                   {:keys   [attendance]
                                    user-id :id} :body} :parameters
                                  master                :identity}]
                              (db/set-attendance! id user-id
                                                  (:id master) attendance)
                              (response/ok))}}]]])
