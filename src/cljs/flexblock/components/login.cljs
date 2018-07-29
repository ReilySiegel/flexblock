(ns flexblock.components.login
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [flexblock.utils :as u]
            [ajax.core :refer [POST]]
            [flexblock.components.input :as input]
            [flexblock.components.modal :as modal]))

(defn modal []
  [modal/standard "login-modal"
   [:div.modal-content
    [:h4.center.purple-text.text-lighten-3 "Login"]
    [:div.row
     [:div.col.l6.m12
      [input/text
       {:placeholder   "Username"
        :id            "login-username-input"
        :type          :email
        :class-name    "login-form"
        :dispatch-key  :login/set-username
        :subscribe-key :login/username}]]
     [:div.col.l6.m12
      [input/text
       {:placeholder   "Password"
        :class-name    "login-form"
        :type          :password
        :dispatch-key  :login/set-password
        :subscribe-key :login/password}]]]]
   [:div.modal-footer
    [:button.btn-flat.amber-text.darken-1.waves-effect.waves-purple
     {:on-click #(rf/dispatch [:login
                               @(rf/subscribe [:login/username])
                               @(rf/subscribe [:login/password])])}
     "Login"]]])
