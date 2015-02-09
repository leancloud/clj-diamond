(ns clj-diamond.core
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :as pp]
            [clojure.data.json :as json])
  (:import (java.util Properties)
           (java.io FileInputStream File)
           (cn.leancloud.diamond.manager.impl DefaultDiamondManager)
           (cn.leancloud.diamond.manager ManagerListener)
           (cn.leancloud.diamond.client DiamondConfigure)))

(def managers (atom {}))

(def gconf :conf)

(def gmger :manager)

(defn get-property
  ([prop key]
     (get-property nil prop key))
  ([cast-fn ^Properties prop ^String key]
     (when-let [v (.getProperty prop key)]
       (if cast-fn
         (cast-fn v)
         v))))

(def get-property-bool
  (partial get-property #(Boolean/valueOf %)))

(def get-property-num
  (partial get-property #(Integer/valueOf %)))

(defn update-conf*
  []
  (let [prop (Properties.)
        fis (try (FileInputStream. "./conf.properties")
                 (catch Exception e
                   (log/info "can't find conf.properties")))
        default-conf {:polling-interval-time 15
                      :user-flow-control true
                      :once-timeout 2000
                      :recv-wait-time 10000
                      :scheduled-threadpool 1
                      :config-server-address "127.0.0.1"
                      :config-server-port 8080
                      :retrieve-data-retry-times 2}
        pconf (fn [map key fun strkey]
                (if-let [c (fun prop strkey)]
                  (assoc map key c)
                  map))]
    (if-not fis
      default-conf
      (let [_ (.load prop fis)
            updated-conf
            (-> default-conf
                (pconf :polling-interval-time get-property-num "pollingintervaltime")
                (pconf :user-flow-control get-property-bool "userflowcontrol")
                (pconf :once-timeout get-property-num "oncetimeout")
                (pconf :recv-wait-time get-property-num "recvwaittime")
                (pconf :scheduled-threadpool get-property-num "scheduledthreadpool")
                (pconf :config-server-address get-property "configserveraddress")
                (pconf :config-server-port get-property-num "configserverport")
                (pconf :retrieve-data-retry-times get-property-num "retrievedataretrytimes"))]
        (.close fis)
        updated-conf))))

(defn update-conf!
  "Configure diamond manager."
  [^DiamondConfigure manager]
  (let [conf (.getDiamondConfigure manager)
        mapconf (update-conf*)]
    (doto conf
      (.setPollingIntervalTime (:polling-interval-time mapconf))
      (.setUseFlowControl (:user-flow-control mapconf))
      (.setOnceTimeout (:once-timeout mapconf))
      (.setReceiveWaitTime (:recv-wait-time mapconf))
      (.setScheduledThreadPoolSize (:scheduled-threadpool mapconf))
      (.setConfigServerAddress (:config-server-address mapconf))
      (.setConfigServerPort (:config-server-port mapconf))
      (.setRetrieveDataRetryTimes (:retrieve-data-retry-times mapconf)))))

(defn update-managers [group dataid conf]
  (swap! managers
         assoc-in (map keyword [group dataid]) conf))

(defn add-manager*
  "register manager"
  [[group dataid callback & {:keys [sync-timeout sync-cb]}]]
  (log/infof "Adding new diamond manager %s/%s" group dataid)
  (let [manager (DefaultDiamondManager. group dataid
                  (reify
                    ManagerListener
                    (getExecutor [this] nil)
                    (receiveConfigInfo [this configinfo]
                      (log/infof "Receiving new config info for %s/%s: %s"
                                 group dataid configinfo)
                      (when callback
                        (callback configinfo))
                      (update-managers group dataid {gconf configinfo}))))]
    (update-conf! manager)
    (let [c (.getAvailableConfigureInfomation manager (or sync-timeout 1000))]
      (when (and sync-cb callback)
        (callback c))
      (update-managers group dataid
                       {gmger manager
                        gconf c}))))

(defn add-manager
  [& groups]
  (doseq [c groups]
    (add-manager* c)))

(defn get* [group dataid gkey]
  (let [conf (get-in @managers (map keyword [group dataid gkey]))]
    conf))

(defn get-conf
  ([group dataid]
     (get* group dataid gconf))
  ([group dataid conf-type]
     (let [c (get* group dataid gconf)]
       (case conf-type
         :json (json/read-str c :key-fn keyword)
         :num  (Long/valueOf c)
         (ex-info "Un support type" {:type conf-type})))))

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
   {} @managers))

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
