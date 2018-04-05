(ns ^:figwheel-no-load flexblock.app
  (:require [flexblock.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
