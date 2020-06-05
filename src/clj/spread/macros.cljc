(ns spread.macros
  (:require #?(:clj [cljs.core]
               :default [taoensso.timbre]))
  #?(:cljs (:require-macros [shared.macros])))

(defn compiletime-info
  [_ and-form ns]
  (let [meta-info (meta and-form)]
    {:ns (str (ns-name ns))
     :line (:line meta-info)
     :file (:file meta-info)}))

;; (defmacro slurpit [path]
;;   (clojure.core/slurp path))

(defmacro get-version []
  (str (clojure.core/slurp "./version")))

#?(:clj
   (defmacro get-env-variable
     [var-name & [required?]]
     (let [var-name (System/getenv var-name)]
       (if (and (not var-name)
                required?)
         (throw (Exception. (str "MISSING ENV VARIABLE: " var-name " not defined in environment")))
         var-name))))
