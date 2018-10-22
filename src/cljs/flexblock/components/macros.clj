(ns flexblock.components.macros)

(def components
  '[AppBar
    Avatar
    Button
    Card
    CardActions
    CardContent
    CardHeader
    Checkbox
    Collapse
    CssBaseline
    Dialog
    DialogActions
    DialogContent
    DialogTitle
    Drawer
    FormControl
    FormControlLabel
    Grid
    Grow
    IconButton
    InputLabel
    LinearProgress
    List
    ListItem
    ListItemAvatar
    ListItemSecondaryAction
    ListItemText
    Menu
    MenuItem
    Portal
    Select
    Snackbar
    Slide
    Tab
    Tabs
    TextField
    Toolbar
    Tooltip
    Typography
    Zoom])

(defn material-ui-react-import [component]
  `(def ~component
     (reagent.core/adapt-react-class (aget js/MaterialUI
                                           ~(name component)))))

(defmacro export-material-ui-react-classes []
  `(do
     ~@(map material-ui-react-import components)))
