(ns flexblock.utils
  (:require [re-frame.core :as rf]
            [ajax.core :refer [GET POST PATCH DELETE]]
            [flexblock.components.input :as input]))

(defn open-modal [query-selector]
  (let [e (.querySelector js/document query-selector)
        i (.getInstance js/M.Modal e)]
    (.open i)))

(defn close-modal [query-selector]
  (if-let [e (.querySelector js/document query-selector)]
    (.close (.getInstance js/M.Modal e))))

(defn handle-401 []
  (close-modal ".modal.open")
  (rf/dispatch [:set-token ""])
  (.toast js/M (clj->js {:html             "You need to log in to do that."
                         :completeCallback #(open-modal "#login-modal")
                         :displayLength    2500})))

(defn handle-403 []
  (.reload js/location true))

(defn default-error-handler [response]
  (rf/dispatch-sync [:dec-loading])
  (case (:status response)
    403 #(handle-403)
    401 #(handle-401)
    (.toast js/M (clj->js
                  {:html (or (get-in response [:response :message])
                             "An unknown error occured.")}))))

(defn get-users []
  (let [token @(rf/subscribe [:token])]
    (when-not (empty? token)
      (rf/dispatch-sync [:inc-loading])
      (GET "/user"
           {:headers       {"Authorization" (str "Token " token)}
            :handler       (fn [x]
                             (rf/dispatch-sync [:dec-loading])
                             (rf/dispatch [:set-users x]))
            :error-handler default-error-handler}))))

(defn get-rooms []
  (let [token @(rf/subscribe [:token])]
    (when-not (empty? token)
      (rf/dispatch-sync [:inc-loading])
      (GET "/room"
           {:headers       {"Authorization" (str "Token " token)}
            :handler       (fn [x]
                             (rf/dispatch-sync [:dec-loading])
                             (rf/dispatch [:set-rooms x]))
            :error-handler default-error-handler}))))

(defn post-room []
  (POST "room"
        {:params        {:title        @(rf/subscribe [:room/title])
                         :description  @(rf/subscribe [:room/description])
                         :date         @(rf/subscribe [:room/date])
                         :time         @(rf/subscribe [:room/time])
                         :room-number  @(rf/subscribe [:room/number])
                         :max-capacity @(rf/subscribe [:room/max-capacity])}
         :headers       {"Authorization" (str "Token " @(rf/subscribe [:token]))}
         :handler       (fn [_]
                          (get-rooms)
                          (rf/dispatch-sync [:add-room/reset-room])
                          (input/clear-selector ".room-form")
                          (close-modal "#add-room-modal"))
         :error-handler default-error-handler}))

(defn join-room [room-id]
  (POST "room/join"
        {:params        {:room-id room-id}
         :headers       {"Authorization" (str "Token " @(rf/subscribe [:token]))}
         :handler       (fn [_]
                          (.toast js/M (clj->js {:html "Room joined!"}))
                          (get-rooms))
         :error-handler default-error-handler}))

(defn leave-room [room-id]
  (POST "room/leave"
        {:params        {:room-id room-id}
         :headers       {"Authorization" (str "Token " @(rf/subscribe [:token]))}
         :handler       (fn [_]
                          (.toast js/M (clj->js {:html "You successfully left."}))
                          (get-rooms))
         :error-handler default-error-handler}))


(defn delete-room [room-id]
  (DELETE "room"
          {:params        {:room-id room-id}
           :headers       {"Authorization" (str "Token " @(rf/subscribe [:token]))}
           :handler       (fn [_]
                            (.toast js/M (clj->js {:html "Your room has been deleted."}))
                            (get-rooms))
           :error-handler default-error-handler}))

(defn set-password [user-id]
  (PATCH "user/password"
         {:params        {:user-id  user-id
                          :password @(rf/subscribe [:reset-password])}
          :headers       {"Authorization" (str "Token " @(rf/subscribe [:token]))}
          :handler       (fn [_]
                           (.toast js/M (clj->js {:html "Password Updated."}))
                           (rf/dispatch-sync [:set-reset-password ""])
                           (close-modal (str "#passwordmodal" user-id)))
          :error-handler default-error-handler}))
