(ns jaskell.sql
  (:require [clojure.string :as str])
  (:import (jaskell.script Directive)
           (jaskell.sql Query Statement JDBCParameter)
           (clojure.lang Obj)))

(def returning :returning)

(def from :from)

(def set :set)

(def where :where)

(def as :as)

(def case :case)

(def when :when)

(def where :where)

(def join :join)

(def left :left)

(def right :right)

(def full :full)

(def cross :cross)

(def inner :inner)

(def on :on)

(def end :end)

(def do :do)

(def conflict :conflict)

(def nothing :nothing)

(def is :is)

(def null :null)

(def not :not)

(def recursive :recursive)

(def union :union)

(def all :all)

(defn partition-helper
  []
  (let [state (atom 0)
        last (atom "")]
    (fn [token]
      (let [l @last]
        (reset! last token)
        (if (or (= token as) (= l as))
          @state
          (swap! state inc))))))

(defn parse-statement
  [tokens]
  (.script (apply (first tokens) (rest tokens))))

(defn parse
  [token]
  (cond
    (keyword? token) (name token)
    (instance? Directive token) (.script token)
    (vector? token) (->> token
                         (partition-by (partition-helper))
                         (map #(->> %
                                    (map parse)
                                    (str/join " ")))
                         (str/join ", "))
    (instance? String token) token
    :else (str token)))

(defn extract
  [token]
  (cond
    (instance? Directive token) (-> token (.parameters) vec)
    :else []))

(defn select
  [& tokens]
  (proxy [Query] []
    (script []
      (str "select " (->> tokens (map parse) (str/join " "))))
    (parameters []
      (->> tokens (map extract) flatten vec))))

(defn write-helper
  [word tokens]
  (if (= (-> tokens butlast last) :returning)
    (proxy [Query] []
      (script []
        (str word " " (->> tokens (map parse) (str/join " "))))
      (parameters []
        (->> tokens (map extract) flatten vec)))
    (proxy [Statement] []
      (script []
        (str word " " (->> tokens (map parse) (str/join " "))))
      (parameters []
        (->> tokens (map extract) flatten vec)))))

(defn insert
  [& tokens]
  (write-helper "insert" tokens))

(defn delete
  [& tokens]
  (write-helper "delete" tokens))

(defn update
  [& tokens]
  (write-helper "update" tokens))

(defn with-query?
  [tokens]
  (let [select? (if (= (first tokens) :recursive)
                  (nth tokens 2)
                  (nth tokens 1))
        returning? (last (butlast tokens))]
    (if (or (= select select?) (= returning returning?))
      true
      false)))

(defn parse-cte
  [cte]
  (->> cte
       (partition-by (partition-helper))
       (map #(str/join " " [(parse (first %)) (parse (second %)) (str "(" (parse (last %)) ")")]))
       (str/join ", ")))

(defn parse-with
  [tokens]
  (let [recursive? (= recursive (first tokens))
        head (if recursive? "with recursive" "with")
        cte (if recursive? (second tokens) (first tokens))
        main (if recursive? (drop 2 tokens) (rest tokens))]
    (str head " " (parse-cte cte) " " (parse-statement main))))

(defn with
  [& tokens]
  (if (with-query? tokens)
    (proxy [Query] []
      (script []
        (parse-with tokens))
      (parameters []
        (->> tokens (map extract) flatten vec)))
    (proxy [Statement] []
      (script []
        (parse-with tokens))
      (parameters []
        (->> tokens (map extract) flatten vec)))))

(defn br
  [& tokens]
  (proxy [Directive] []
    (script []
      (str "(" (->> tokens (map parse) (str/join " ")) ")"))
    (parameters []
      (->> tokens (map extract) flatten vec))))

(defn t
  [^String token]
  (str "'" token "'"))

(defn q
  [^String token]
  (str "\"" token "\""))

(defn p
  ([key]
   (JDBCParameter. key))
  ([key cls]
   (JDBCParameter. key cls)))

(defn f
  ([name]
   (proxy [Directive] []
     (script []
       (str (parse name) "()"))
     (parameters []
       [])))
  ([name & p]
   (proxy [Directive] []
     (script []
       (str (parse name) "(" (->> p (map parse) (str/join ", ")) ")"))
     (parameters []
       (->> (concat name p) (map extract) flatten vec)))))

(defn paramterize
  [^Directive script]
  (concat (.script script) (.parameters script)))