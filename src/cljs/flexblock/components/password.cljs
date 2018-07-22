(ns flexblock.components.password
  (:require [re-frame.core :as rf]
            [flexblock.utils :as u]
            [flexblock.components.input :as input]
            [flexblock.components.modal :as modal]))

(defn modal
  "Shows a modal that allows a teacher to change the password of `user`."
  [user opts]
  (let [{:keys [id]
         :or   {id (str "passwordmodal" (:id user))}}
        opts]
    ^{:key (:id user)}
    [modal/standard id
     [:div.modal-content
      [:h4.center.purple-text.text-lighten-3
       (str "Reset Password for " (:name user))]
      [:div.row [:div.col.l6.offset-l3.m12
                 [input/text
                  {:type          :password
                   :placeholder   "New Password"
                   :dispatch-key  :set-reset-password
                   :subscribe-key :reset-password}]]]]
     [:div.modal-footer
      [:button.btn-flat.amber-text.waves-effect.waves-purple
       {:on-click #(u/set-password (:id user))}
       "Reset Password"]]]))
