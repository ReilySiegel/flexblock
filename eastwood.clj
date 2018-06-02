(disable-warning
 {:linter                      :constant-test
  :for-macro                   'clojure.core/cond
  :if-inside-macroexpansion-of #{'clojure.core.async/go}
  :within-depth                10
  :reason
  "False positives on cond with :else key inside a go block."})
