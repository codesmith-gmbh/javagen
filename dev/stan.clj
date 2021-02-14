(ns stan
  (:require [codesmith.javagen :as jg]))

(comment

  (jg/emit [] {::jg/type     :file
               :package-name "ch.codesmith.test"
               :imports      {::jg/type :imports
                              :imports  ["java.util.List"]}
               :declarations [{::jg/type        :class
                               :access-modifier :public
                               :name            "Test"
                               :declarations    [{::jg/type :field
                                                  :name     "a"
                                                  :type     :String}
                                                 {::jg/type :constructor}
                                                 {::jg/type   :constructor
                                                  :parameters {::jg/type :fragment
                                                               :fragment "int a"}
                                                  :body {::jg/type :fragment
                                                         :fragment "int b = a + 1;\n"}}
                                                 {::jg/type   :method
                                                  :name       "test"
                                                  :parameters {::jg/type   :parameters
                                                               :parameters [{:name "first"
                                                                             :type "String"}
                                                                            {:name "second"
                                                                             :type "int"}]}}]}]})

  )