(ns flexblock.primitives
  "Spec Primitives."
  (:require [clojure.spec.alpha :as s]
            [spec-tools.spec :as spec]))

(s/def ::int? #?(:clj spec/int?
                 :default int?))
(s/def ::pos-int? #?(:clj spec/pos-int?
                     :default pos-int?))
(s/def ::string? #?(:clj spec/string?
                    :default string?))
(s/def ::boolean? #?(:clj spec/boolean?
                     :default boolean?))
(s/def ::nil? #?(:clj spec/nil?
                 :default nil?))
