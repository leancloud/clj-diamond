(ns clj-diamond.core
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :as pp])
  (:import (com.taobao.diamond.manager.impl DefaultDiamondManager)
           (com.taobao.diamond.manager ManagerListener)
           (com.taobao.diamond.client DiamondConfigure)))

(def status (atom {}))

(def gconf :conf)
(def gmger :manager)

(defn update-status [group dataid conf]
  (swap! status
         assoc-in (map keyword [group dataid]) conf))

(defn register-manager
  "register manager"
  [group dataid]
  (let [manager (DefaultDiamondManager. group dataid
                  (reify
                    ManagerListener
                    (getExecutor [this] nil)
                    (receiveConfigInfo [this configinfo]
                      (update-status group dataid configinfo))))]
    (update-status group dataid
                   {gmger manager
                    gconf (.getAvailableConfigureInfomation manager 1000)})))

(defn get* [group dataid key]
  (let [conf (get-in @status (map keyword [group dataid key]))]
    conf))

(defn get-conf [group dataid]
  (get* group dataid gconf))

(defn get-manager [group dataid]
  (get* group dataid gmger))

(defn all*
  "get key"
  [key]
  (reduce-kv
   (fn [m k v]
     (assoc m k
            (reduce-kv
             (fn [mm kk vv]
               (assoc mm kk
                      (reduce-kv
                       (fn [mmm kkk vvv]
                         (if (= kkk key)
                           (assoc mmm kkk vvv)
                           mmm))
                       {} vv)))
             {} v)))
   {} @status))

(defn all-conf
  "return all conf map"
  []
  (all* gconf))

(defn all-mger
  "return all manager map"
  []
  (all* gmger))

(defn print-all-conf
  "print all conf"
  []
  (pp/pprint (all-conf)))
