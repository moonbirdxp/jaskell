(defproject liu.mars/jaskell "0.1.1"
  :name "jaskell"
  :description "Rock your Java!"
  :url "https://github.com/MarchLiu/jaskell"
  :license {:name "THE MIT LICENSE"
            :url "https://opensource.org/licenses/MIT"}
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles {:dev {:dependencies [[junit/junit "4.11"]
                                  [org.clojure/java.jdbc "0.7.7"]
                                  [org.xerial/sqlite-jdbc "3.23.1"]]
                   :source-paths ["src/main/clojure" "src/test/clojure"]
                   :java-source-paths ["src/main/java" "src/test/java"]
                   :junit ["src/test/java"]}}
  :plugins [[lein-junit "1.1.8"]]
  :java-source-paths ["src/main/java"]
  :source-paths ["src/main/clojure" "src/main/java"]
  :test-paths ["src/test/clojure" "src/test/java"]
  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:unchecked" "-Xlint:varargs"]
  :junit ["src/test/java"])
