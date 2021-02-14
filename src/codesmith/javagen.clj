(ns codesmith.javagen
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defmacro c-name [arg]
  `(name ~arg))

(def push-scope conj)
(def top-scope last)

(defmulti emit (fn [ctx construct]
                 (::type construct)))

(defn static-str [static?]
  (if static? "static" ""))

(defn println-oneline-stmt [& parts]
  (apply println (concat parts [";"])))

(defmethod emit nil [_ _]
  )

(defmethod emit :fragment [ctx {:keys [fragment println?] :or {println? true}}]
  ((if println? println print) fragment))

(defmethod emit :file [ctx {:keys [package-name imports declarations]}]
  (println-oneline-stmt "package" package-name)
  (println)
  (emit ctx imports)
  (println)
  (doseq [declaration declarations]
    (emit ctx declaration)
    (println)))

(defmethod emit :imports [ctx {:keys [imports]}]
  (doseq [import imports]
    (println-oneline-stmt "import" import)))

(defmethod emit :class [ctx {:keys [name access-modifier static? declarations] :or {access-modifier :public}}]
  (let [ctx (push-scope ctx {:scope :class :name name})]
    (println (c-name access-modifier) (static-str static?) "class" (c-name name) "{")
    (println)
    (doseq [part declarations]
      (emit ctx part)
      (println))
    (println "}")))

(defmethod emit :constructor [ctx {:keys [access-modifier parameters body] :or {access-modifier :public}}]
  (let [{:keys [scope name]} (top-scope ctx)
        class-name (and (= scope :class) name)
        ctx        (push-scope ctx {:scope      :constructor
                                    :class-name class-name})]
    (print (c-name access-modifier) class-name "(")
    (emit ctx parameters)
    (println ") {")
    (emit ctx body)
    (println "}")))

(defmethod emit :method [ctx {:keys [name
                                     static?
                                     access-modifier
                                     return-type
                                     parameters
                                     body]
                              :or   {access-modifier :public
                                     return-type     :void}}]
  (let [ctx (push-scope ctx {:scope :method
                             :name  name})]
    (print (c-name access-modifier) (static-str static?) (c-name return-type) name "(")
    (emit ctx parameters)
    (println ") {")
    (emit ctx body)
    (println "}")))

(defmethod emit :parameters [_ {:keys [parameters]}]
  (print (str/join ", " (map (fn [{:keys [type name]}]
                               (str type " " name))
                             parameters))))

(defmethod emit :field [_ {:keys [type name static? access-modifier] :or {access-modifier :public}}]
  (println-oneline-stmt (c-name access-modifier) (static-str static?) (c-name type) (c-name name)))

(defn emit-to-out [java-structure]
  (emit [] java-structure))

(defn emit-to-string [java-structure]
  (with-out-str
    (emit-to-out java-structure)))

(defn emit-to-file [f java-structure]
  (with-open [ow (io/writer f)]
    (binding [*out* ow]
      (emit-to-out java-structure))))
