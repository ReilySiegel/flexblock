(ns flexblock.components.password
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [flexblock.components.input :as input]
            [flexblock.components.modal :as modal]))

(defn modal
  "Shows a modal that allows a teacher to change the password of `user`."
  [user opts]
  (let [{:keys [id]
         :or   {id (str "passwordmodal" (:id user))}} opts
        password                                      (r/atom "")]
    ^{:key (:id user)}
    [modal/standard
     {:id       id
      :on-close #(reset! password "")}
     [:div.modal-content
      [:h4.center.purple-text.text-lighten-3
       (str "Reset Password for " (:name user))]
      [:div.row [:div.col.l6.offset-l3.m12
                 [input/text
                  {:type        :password
                   :placeholder "New Password"
                   :atom        password}]]]]
     [:div.modal-footer
      [:button.btn-flat.amber-text.waves-effect.waves-purple
       {:on-click #(rf/dispatch [:user/reset-password (:id user) @password])}
       "Reset Password"]]]))
