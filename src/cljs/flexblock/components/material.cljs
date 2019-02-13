(ns flexblock.components.material
  (:refer-clojure :exclude [List])
  (:require [camel-snake-kebab.core :as case]
            [clojure.set :as set]
            ["@material-ui/core" :as material]
            ["@material-ui/core/colors" :as material-colors]
            ["@material-ui/core/styles" :as material-styles]
            ["@material-ui/lab" :as material-lab]
            [reagent.core :as r]
            [reagent.impl.template :as rtpl]))

(defn color [color]
  (aget material-colors (name color)))

(defn createMuiTheme [theme]
  (material-styles/createMuiTheme (clj->js theme)))


(def MuiThemeProvider (-> material-styles/MuiThemeProvider
                          (reagent.core/adapt-react-class)))

(def ^:private input-component
  (r/reactify-component
   (fn [props]
     [:input (-> props
                 (assoc :ref (:inputRef props))
                 (dissoc :inputRef))])))

(def ^:private textarea-component
  (r/reactify-component
   (fn [props]
     [:textarea (-> props
                    (assoc :ref (:inputRef props))
                    (dissoc :inputRef))])))

(defn TextField [props & children]
  (let [props
        (-> props
            (assoc-in [:InputProps :inputComponent]
                      (cond
                        (and (:multiline props)
                             (:rows props)
                             (not (:maxRows props)))
                        textarea-component

                        ;; FIXME: Autosize multiline field is broken.
                        (:multiline props)
                        nil

                        ;; Select doesn't require cursor fix so
                        ;; default can be used.
                        (:select props)
                        nil

                        :else
                        input-component))
            rtpl/convert-prop-value)]
    (apply r/create-element
           material/TextField
           props
           (map r/as-element children))))


(defn material-component [key]
  (r/adapt-react-class (aget material (name key))))

(defn material-lab-component [key]
  (r/adapt-react-class (aget material-lab (name key))))

(def AppBar (material-component :AppBar))
(def Avatar (material-component :Avatar))
(def Button (material-component :Button))
(def Card (material-component :Card))
(def CardActions (material-component :CardActions))
(def CardContent (material-component :CardContent))
(def CardHeader (material-component :CardHeader))
(def Checkbox (material-component :Checkbox))
(def Chip (material-component :Chip))
(def Collapse (material-component :Collapse))
(def CssBaseline (material-component :CssBaseline))
(def Dialog (material-component :Dialog))
(def DialogActions (material-component :DialogActions))
(def DialogContent (material-component :DialogContent))
(def DialogContentText (material-component :DialogContentText))
(def DialogTitle (material-component :DialogTitle))
(def Drawer (material-component :Drawer))
(def Fab (material-component :Fab))
(def FormControl (material-component :FormControl))
(def FormControlLabel (material-component :FormControlLabel))
(def Grid (material-component :Grid))
(def Grow (material-component :Grow))
(def Icon (material-component :Icon))
(def IconButton (material-component :IconButton))
(def InputLabel (material-component :InputLabel))
(def LinearProgress (material-component :LinearProgress))
(def List (material-component :List))
(def ListItem (material-component :ListItem))
(def ListItemAvatar (material-component :ListItemAvatar))
(def ListItemSecondaryAction (material-component :ListItemSecondaryAction))
(def ListItemText (material-component :ListItemText))
(def Menu (material-component :Menu))
(def MenuItem (material-component :MenuItem))
(def Portal (material-component :Portal))
(def Select (material-component :Select))
(def Snackbar (material-component :Snackbar))
(def Slide (material-component :Slide))
(def SpeedDial (material-lab-component :SpeedDial))
(def SpeedDialAction (material-lab-component :SpeedDialAction))
(def Tab (material-component :Tab))
(def Tabs (material-component :Tabs))
(def Toolbar (material-component :Toolbar))
(def Tooltip (material-component :Tooltip))
(def Typography (material-component :Typography))
(def Zoom (material-component :Zoom))
