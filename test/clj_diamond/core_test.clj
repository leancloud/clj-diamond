(ns clj-diamond.core-test
  (:require [clojure.test :refer :all]
            [clj-diamond.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(register-manager "DEFAULT_GROUP" "test")

(register-manager "DEFAULT_GROUP" "test1")

(register-manager "DEFAULT_GROUP" "ttt")


(def default-conf (partial get-conf "DEFAULT_GROUP"))


(deftest get
  (testing "test get"
    (are [x y] (= x (default-conf y))
         "test" "test"
         "test1" "test")))

#_(defn -main []
  (loop []
    (Thread/sleep 5000)
    (println "this is test conf " (default-conf "test"))
    (println "this is test1 conf " (default-conf "test1"))
    (println "this is ttt conf " (default-conf "ttt"))
    (println "this is all conf " (all-conf))
    #_(print-all-conf)
    (recur)))
