(ns flexblock.about.views
  (:require [flexblock.components.material :as material]
            [re-frame.core :as rf]))

(defn modal []
  [material/Dialog
   {:fullWidth true
    :open      @(rf/subscribe [:about/open?])
    :onClose   #(rf/dispatch [:about/open? false])}
   [material/DialogTitle "About Flexblock"]
   [material/DialogContent
    [material/DialogContentText
     "Flexblock embraces the open-source philosophy, which emphasizes community
contribution, review, and modification. As such, the source code for Flexblock
is freely available, and student contributions are welcome. Contributors with
any level of programming experience are welcome, including absolute
beginners."]]
   [material/DialogActions
    [material/Button
     {:color  :secondary
      :href   "https://gitlab.com/ReilySiegel/flexblock"
      :target :_blank}
     "Contribute"]
    [material/Button
     {:color  :secondary
      :href   "https://gitlab.com/ReilySiegel/flexblock/issues"
      :target :_blank}
     "Report a Bug"]]])
