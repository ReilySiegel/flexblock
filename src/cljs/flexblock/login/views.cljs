(ns flexblock.login.views
  (:require [flexblock.components.material :as material]
            [re-frame.core :as rf]
            [reagent.core :as r]))

(defn modal []
  (let [username (r/atom "")
        password (r/atom "")]
    (fn [] [material/Dialog
            {:open    @(rf/subscribe [:login/open])
             :scroll  :body
             :onClose (fn []
                        (reset! username "")
                        (reset! password "")
                        (rf/dispatch [:login/set-open false]))}
            [material/DialogTitle "Login"]
            [material/DialogContent
             [material/Grid
              {:container true
               :spacing   16}
              [material/Grid
               {:item true
                :xs   12
                :md   6}
               [material/TextField
                {:autoFocus true
                 :label     "Email"
                 :fullWidth true
                 :type      :email
                 :value     @username
                 :onChange  #(reset! username (-> % .-target .-value))}]]
              [material/Grid
               {:item true
                :xs   12
                :md   6}
               [material/TextField
                {:label     "Password"
                 :fullWidth true
                 :type      :password
                 :value     @password
                 :onChange  #(reset! password (-> % .-target .-value))}]]]]
            [material/DialogActions
             [material/Button
              {:color   :secondary
               :onClick (fn [] (rf/dispatch [:login @username @password])
                          (reset! username "")
                          (reset! password ""))}
              "Log In"]]
            #_
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
            #_[:div.modal-footer
               [:button.btn-flat.amber-text.darken-1.waves-effect.waves-purple
                {:on-click #(rf/dispatch [:login @username @password])}
                "Login"]]])))
