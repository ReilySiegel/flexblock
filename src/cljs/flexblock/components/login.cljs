(ns flexblock.components.login
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [flexblock.utils :as u]
            [ajax.core :refer [POST]]
            [flexblock.components.input :as input :refer [input-rf-dispatch]]
            [flexblock.components.modal :as modal]))

(defn- submit-login []
  (POST "login"
        {:params        {:username @(rf/subscribe [:login/username])
                         :password @(rf/subscribe [:login/password])}
         :handler       (fn [t]
                          (let [e (.querySelector js/document "#login-modal")
                                i (.getInstance js/M.Modal e)]
                            (.close i)
                            (rf/dispatch-sync [:set-token (:token t)])
                            (rf/dispatch [:set-user (get t :user)])
                            (input/clear-selector ".login-form")
                            (u/get-rooms)
                            (u/get-users)))
         :error-handler u/default-error-handler}))

(defn modal [] 
  [modal/standard "login-modal"
   [:div.modal-content
    [:h4.center.purple-text.text-lighten-3 "Login"]
    [:div.row
     [:div.col.l6.m12
      [input-rf-dispatch
       {:placeholder "Username"
        :type        :email
        :class-name  "validate login-form"}
       "Username" :login/set-username :login/username]]
     [:div.col.l6.m12
      [input-rf-dispatch
       {:placeholder "Password"
        :class-name  "login-form"
        :type        :password}
       "Password" :login/set-password :login/password]]]] 
   [:div.modal-footer 
    [:a.btn-flat.amber-text.darken-1.waves-effect.waves-purple
     {:on-click submit-login}
     "Submit"]]])

