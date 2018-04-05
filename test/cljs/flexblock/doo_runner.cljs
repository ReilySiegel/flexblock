(ns flexblock.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [flexblock.core-test]))

(doo-tests 'flexblock.core-test)

