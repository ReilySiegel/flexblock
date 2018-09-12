(ns flexblock.routes.helpers
  (:require [ring.util.http-response :as response]))

(defmacro api-try
  "Wraps `body` in a try-catch form.
  If an exception of type ExceptionInfo is thrown, and (ex-data e#)
  returns a map containing a :message key, a
  `response/unprocessable-entity` response will be returned. If any
  other exception is thrown, a `response/internal-server-error` will
  be returned. Errors will not be caught.

  Example:

  (api-try (/ 0 0)

  (try
  (/ 0 0)
  (catch
    java.lang.Exception
    e#
    (if (contains? (ex-data e#) :message)
      (response/unprocessable-entity (ex-data e#))
      (response/internal-server-error
        {:message \"An unknown error has occurred.\"}))))"
  [& body]
  `(try ~@body
        (catch Exception e#
          (if (contains? (ex-data e#) :message)
            (response/unprocessable-entity (ex-data e#))
            (response/internal-server-error
             {:message "An unknown error has occurred."})))))
