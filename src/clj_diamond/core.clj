(ns clj-diamond.core
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :as pp])
  (:import (com.taobao.diamond.manager.impl DefaultDiamondManager)
           (com.taobao.diamond.manager ManagerListener)
           (com.taobao.diamond.client DiamondConfigure)))

(def status (atom {}))

(defn update-status [group dataid conf]
  (swap! status
         assoc-in (map keyword [group dataid]) conf))

(defn register-manager
  "register manager"
  ([group dataid]
   (register-manager group dataid nil))
  ([group dataid callback]
   (let [manager (DefaultDiamondManager. group dataid
                   (reify
                     ManagerListener
                     (getExecutor [this] nil)
                     (receiveConfigInfo [this configinfo]
                       (update-status group dataid configinfo)
                       (callback))))]
     (update-status group dataid
                    (.getAvailableConfigureInfomation manager 1000))
     manager)))

(defn get-conf [group dataid]
  (let [conf (get-in @status (map keyword [group dataid]))]
    (if (nil? conf)
      (log/warnf "this conf maybe not exists group:%s dataid:%s" group dataid)
      conf)))

(defn all-conf
  "return all conf map"
  []
  @status)

(defn print-all-conf
  "print all conf"
  []
  (pp/pprint (all-conf)))

(def manager (register-manager "DEFAULT_GROUP" "test"))

(def manager1 (register-manager "DEFAULT_GROUP" "test1"))

(def manager2 (register-manager "DEFAULT_GROUP" "ttt"))


(def default-conf (partial get-conf "DEFAULT_GROUP"))



(defn -main []
  (loop []
    (Thread/sleep 5000)
    (println "this is test conf " (default-conf "test"))
    (println "this is test1 conf " (default-conf "test1"))
    (println "this is ttt conf " (default-conf "ttt"))
    (println "this is all conf " (all-conf))
    (recur)))
