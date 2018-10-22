(ns flexblock.subs
  "Global re-frame subscriptions.
  This namespace MUST require all other subscription namespaces."
  (:require [flexblock.login.subs]
            [flexblock.navbar.subs]
            [flexblock.reminder.subs]
            [flexblock.rooms.subs]
            [flexblock.search.subs]
            [flexblock.snackbar.subs]
            [flexblock.users.subs]
            [re-frame.core :as rf]))
