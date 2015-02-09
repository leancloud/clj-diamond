(ns clj-diamond.core
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :as pp]
            [clojure.data.json :as json])
  (:import (java.util Properties)
           (java.io FileInputStream File)
           (cn.leancloud.diamond.manager.impl DefaultDiamondManager)
           (cn.leancloud.diamond.manager ManagerListener)
           (cn.leancloud.diamond.client DiamondConfigure)))

(def status (atom {}))

(def gconf :conf)

(def gmger :manager)


(defn get-property
  [^Properties prop ^String key]
  (.getProperty prop key))
(defn get-property-bool
  [^Properties prop ^String key]
  (when-let [p (get-property prop key)]
    (Boolean/valueOf p)))

(defn get-property-num
  [^Properties prop ^String key]
  (when-let [p (get-property prop key)]
    (Integer/valueOf p)))

(defn update-conf*
  []
  (let [prop (Properties.)
        fis (try (FileInputStream. "./conf.properties")
                 (catch Exception e
                   (log/info "can't find conf.properties")))
        default-conf {:polling-interval-time 15
                      #_:domain-name-list
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

(defn update-status [group dataid conf]
  (swap! status
         assoc-in (map keyword [group dataid]) conf))

(defn add-manager*
  "register manager"
  [[group dataid callback]]
  (let [manager (DefaultDiamondManager. group dataid
                  (reify
                    ManagerListener
                    (getExecutor [this] nil)
                    (receiveConfigInfo [this configinfo]
                      (update-status group dataid {gconf configinfo})
                      (callback configinfo))))]
    (update-conf! manager)
    (let [c (.getAvailableConfigureInfomation manager 1000)]
      (update-status group dataid
                    {gmger manager
                     gconf c}))))

(defn add-manager
  [& meta]
  (if (and (= 0 (mod (count meta) 3)) (not= (count meta) 0))
    (let [tmp (partition 3 meta)]
      (doseq [t tmp]
        (add-manager* t)))
    (ex-info "Group and Id count is not even" {:count (count meta)})))

(defn get* [group dataid gkey]
  (let [conf (get-in @status (map keyword [group dataid gkey]))]
    conf))

(defn get-conf
  ([group dataid]
   (get* group dataid gconf))
  ([group dataid conf-type]
   (let [c (get* group dataid gconf)]
     (case conf-type
       :clojure-map (read-string c)
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
