(ns codesmith.javagen
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defmacro c-name [arg]
  `(name ~arg))

(defmacro str-and [& args]
  `(let [v# (and ~@args)]
     (if v# v# "")))

(def push-scope conj)
(def top-scope last)

(defmulti emit (fn [ctx construct]
                 (::type construct)))

(defn static-str [static?]
  (str-and static? "static"))

(defn final-str [final?]
  (str-and final? "final"))

(defn println-one-line-stmt [& parts]
  (apply println (concat parts [";"])))

(defn parameters [& params]
  {::type      :parameters
   :parameters params})

(defmethod emit nil [_ _]
  )

(defmethod emit :fragment [ctx {:keys [fragment println?] :or {println? true}}]
  ((if println? println print) fragment))

(defmethod emit :comment [ctx {:keys [comment comment-type] :or {comment-type :block}}]
  (case comment-type
    :block (do (println "/*") (println comment) (println "*/"))
    :line (doseq [line (str/split comment #"\n")]
            (println "//" line))))

(defmethod emit :file [ctx {:keys [package-name imports declarations]}]
  (println-one-line-stmt "package" package-name)
  (println)
  (emit ctx imports)
  (println)
  (doseq [declaration declarations]
    (emit ctx declaration)
    (println)))

(defmethod emit :imports [ctx {:keys [imports]}]
  (doseq [import imports]
    (println-one-line-stmt "import" import)))

(defmethod emit :class [ctx {:keys [name access-modifier static? final? declarations extends implements] :or {access-modifier :public}}]
  (let [ctx (push-scope ctx {:scope :class :name name})]
    (println
      (c-name access-modifier)
      (static-str static?)
      (final-str final?)
      "class" (c-name name)
      (str-and extends (str "extends " extends))
      (str-and implements (str "implements " (str/join ", " implements)))
      "{")
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
                                     body
                                     throws]
                              :or   {access-modifier :public
                                     return-type     :void}}]
  (let [ctx (push-scope ctx {:scope :method
                             :name  name})]
    (print (c-name access-modifier) (static-str static?) (c-name return-type) name "(")
    (emit ctx parameters)
    (println ")" (str-and throws (str "throws " throws)) "{")
    (emit ctx body)
    (println "}")))

(defmethod emit :parameters [_ {:keys [parameters]}]
  (print (str/join ", " (map (fn [{:keys [type name]}]
                               (str (c-name type) " " name))
                             parameters))))

(defmethod emit :field [_ {:keys [type name static? final? access-modifier] :or {access-modifier :public}}]
  (println-one-line-stmt (c-name access-modifier) (static-str static?) (final-str final?) (c-name type) (c-name name)))

(defn emit-to-out [java-structure]
  (emit [] java-structure))

(defn emit-to-string [java-structure]
  (with-out-str
    (emit-to-out java-structure)))

(defn emit-to-file [f java-structure]
  (with-open [ow (io/writer f)]
    (binding [*out* ow]
      (emit-to-out java-structure))))
