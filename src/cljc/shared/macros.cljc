(ns shared.macros
  (:require #?(:clj [cljs.core]
               :default [taoensso.timbre]))
  #?(:cljs (:require-macros [shared.macros])))

(defn compiletime-info
  [_ and-form ns]
  (let [meta-info (meta and-form)]
    {:ns   (str (ns-name ns))
     :line (:line meta-info)
     :file (:file meta-info)}))

(defmacro promise->
  "Takes `thenable` functions as arguments (i.e. functions returning a JS/Promise) and chains them,
   taking care of error handling
   Example:
   (promise-> (thenable-1)
              (thenable-2))"
  [promise & body]
  `(.catch
     (-> ~promise
         ~@(map (fn [expr] (list '.then expr)) body))
     (fn [error#]
       (taoensso.timbre/error "Promise rejected" (merge {:error error#}
                                                        (ex-data error#)
                                                        ~(compiletime-info &env &form *ns*))))))

(defmacro slurpit [path]
  (clojure.core/slurp path))

(defmacro try-catch [& body]
  `(try
     ~@body
     (catch js/Object e#
       (taoensso.timbre/error "Unexpected exception" (merge {:error e#}
                                                            (ex-data e#)
                                                            ~(compiletime-info &env &form *ns*))))))

(defmacro try-catch-throw [& body]
  `(try
     ~@body
     (catch js/Object e#
       (taoensso.timbre/error "Unexpected exception" (merge {:error e#}
                                                            (ex-data e#)
                                                            ~(compiletime-info &env &form *ns*)))
       (throw (js/Error. e#)))))

#?(:clj
   (defmacro get-env-variable
     [var-name & [required?]]
     (let [var-name (System/getenv var-name)]
       (if (and (empty? var-name)
                required?)
         (throw (Exception. (str "MISSING ENV VARIABLE: " var-name " not defined in environment")))
         var-name))))
