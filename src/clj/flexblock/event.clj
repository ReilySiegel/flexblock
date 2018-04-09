(ns flexblock.event
  (:require [flexblock.rooms :as rooms]
            [flexblock.mailer :as mailer]))

(defmulti message :event)

(defmethod message :default [_]
  "Default Message")

(defmethod message :room/delete [event]
  (let [room      (:room event)
        recipient (:recipient event)
        teacher   (rooms/get-teacher room)]
    (str (format "Hello %s,\n" (:name recipient))
         (format "The room you are in, %s, has been deleted. "
                 (:title room))
         "You have been automatically removed from the session. "
         (format "If you have any questions, please contact %s at %s."
                 (:name teacher)
                 (:email teacher)))))

(defmethod message :room/join [event]
  (let [teacher (rooms/get-teacher (:room event))]
    (if (= (:email teacher)
           (:email (:recipient event)))
      (format "%s Has joined %s." 
              (:name (:user event))
              (:title (:room event)))
      (format "You have joined %s's Session."
              (:name teacher)))))

(defmethod message :room/leave [event]
  (let [teacher (rooms/get-teacher (:room event))]
    (if (= (:email teacher)
           (:email (:recipient event)))
      (format "%s Has left %s." 
              (:name (:user event))
              (:title (:room event)))
      (format "You have left %s's Session."
              (:name teacher)))))

(defmethod message :user/set-password [event]
  (str (format "Hello %s,\n"
               (get-in event [:recipient :name]))
       (format "Your password for FlexBlock has been reset by %s."
               (get-in event [:user :name]))))

(defmulti subject :event)

(defmethod subject :default [_]
  "Default Subject")

(defmethod subject :user/set-password [event]
  "Your Password Has Been Reset")

(defmethod subject :room/delete [event]
  "A Session you had joined has been deleted.")

(defmethod subject :room/join [event]
  (let [teacher (rooms/get-teacher (:room event))]
    (if (= (:email teacher)
           (:email (:recipient event)))
      (format "%s has joined your Session."
              (:name (:user event)))
      (format "You have joined %s's Session"
              (:name teacher)))))

(defmethod subject :room/leave [event]
  (let [teacher (rooms/get-teacher (:room event))]
    (if (= (:email teacher)
           (:email (:recipient event)))
      (format "%s has left your Session."
              (:name (:user event)))
      (format "You have left %s's Session"
              (:name teacher)))))


(defn create-mail [event]
  {:from    (:from mailer/settings)
   :to      (or (:send-to mailer/settings)
                (get-in event [:recipient :email]))
   :subject (subject event)
   :body    (message event)})
