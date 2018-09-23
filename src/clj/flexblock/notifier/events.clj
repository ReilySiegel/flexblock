(ns flexblock.notifier.events
  (:require [flexblock.rooms :as rooms]))

(defmulti notification :event)

(defmethod notification :room/delete [event]
  (let [room      (:room event)
        recipient (:recipient event)
        teacher   (rooms/get-teacher room)]
    {:subject "A Session you joined has been deleted."
     :message (str (format "Hello %s,\n" (:name recipient))
                   (format "The room you are in, %s, has been deleted. "
                           (:title room))
                   "You have been automatically removed from the session. "
                   (format "If you have any questions, please contact %s at %s."
                           (:name teacher)
                           (:email teacher)))}))


(defmethod notification :room/join [event]
  (let [teacher     (rooms/get-teacher (:room event))
        user-name   (get-in event [:user :name])
        to-teacher? (= (:email teacher)
                       (:email (:recipient event)))]
    {:subject (if to-teacher?
                (format "%s has joined your Session." user-name)
                (format "You have joined %s's Session" (:name teacher)))
     :message (if to-teacher?
                (format "%s Has joined %s."
                        (:name (:user event))
                        (:title (:room event)))
                (format "You have joined %s's Session."
                        (:name teacher)))}))

(defmethod notification :room/leave [event]
  (let [teacher     (rooms/get-teacher (:room event))
        to-teacher? (= (:email teacher)
                       (:email (:recipient event)))]
    {:subject (if to-teacher?
                (format "%s has left your Session." (:name (:user event)))
                (format "You have left %s's Session" (:name teacher)))
     :message (if to-teacher?
                (format "%s Has left %s."
                        (:name (:user event))
                        (:title (:room event)))
                (format "You have left %s's Session." (:name teacher)))}))

(defmethod notification :user/set-password [event]
  {:subject "Your Password Has Been Reset"
   :message (str (format "Hello %s,\n"
                         (get-in event [:recipient :name]))
                 (format "Your password for FlexBlock has been reset by %s."
                         (get-in event [:user :name])))})

(defmethod notification :user/unenrolled [event]
  {:subject (format "You have not enrolled in a Flexblock session on %s."
                    (get event :date))
   :message (str (format "Hello %s,\n"
                         (get-in event [:recipient :name]))
                 (format "You are currently not enrolled for flexblock on %s. "
                         (get event :date))
                 "Enrollment in a Flexblock session is mandatory.")})

(defmethod notification :user/create [event]
  {:subject "Welcome to FlexBlock!"
   :message
   (str (format "Hello %s,\n"
                (get-in event [:recipient :name]))
        "You are now registered to use FlexBlock!\n\n"
        (format "You have been assigned a random password: %s\n\n"
                (get event :password))
        "We recommend that you change this password immediately "
        "for security reasons. You can change this password by "
        "logging in and pressing the 'Reset Password' button in "
        "the top right.")})
