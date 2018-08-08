(ns flexblock.components.login
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [ajax.core :refer [POST]]
            [flexblock.components.input :as input]
            [flexblock.components.modal :as modal]))

(defn modal []
  (let [username (r/atom "")
        password (r/atom "")]
    [modal/standard
     {:id       "login-modal"
      :on-close (fn []
                  (reset! username "")
                  (reset! password ""))}
     [:div.modal-content
      [:h4.center.purple-text.text-lighten-3 "Login"]
      [:div.row
       [:div.col.l6.m12
        [input/text
         {:placeholder "Username"
          :id          "login-username-input"
          :type        :email
          :class-name  "login-form"
          :atom        username}]]
       [:div.col.l6.m12
        [input/text
         {:placeholder "Password"
          :class-name  "login-form"
          :type        :password
          :atom        password}]]]]
     [:div.modal-footer
      [:button.btn-flat.amber-text.darken-1.waves-effect.waves-purple
       {:on-click #(rf/dispatch [:login @username @password])}
       "Login"]]]))
