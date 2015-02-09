(ns clj-diamond.core
  (:require [clojure.tools.logging :as log]
            [clojure.pprint :as pp]
            [clojure.data.json :as json]
            [clj-yaml.core :as yaml])
  (:import (java.util Properties)
           (java.io FileInputStream File)
           (java.io StringReader)
           (cn.leancloud.diamond.manager.impl DefaultDiamondManager)
           (cn.leancloud.diamond.manager ManagerListener)
           (cn.leancloud.diamond.client DiamondConfigure)))

(def managers (atom {}))

(def ^:dynamic *current* (atom nil))


(def gconf :conf)

(def gmger :manager)

(def gtype :type)

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

(defn- update-conf*
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



(defn- update-conf!
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

(defn- update-managers [group data-id conf]
  (swap! managers
         assoc-in (map keyword [group data-id]) conf))

(defn- add-manager*
  "register manager"
  [[group data-id type callback & {:keys [sync-timeout sync-cb]}]]
  (log/infof "Adding new diamond manager %s/%s" group data-id)
  (let [manager (DefaultDiamondManager. group data-id
                  (reify
                    ManagerListener
                    (getExecutor [this] nil)
                    (receiveConfigInfo [this configinfo]
                      (log/infof "Receiving new config info for %s/%s: %s"
                                 group data-id configinfo)
                      (when callback
                        (callback configinfo))
                      (update-managers group data-id {gconf configinfo}))))]
    (update-conf! manager)
    (let [c (.getAvailableConfigureInfomation manager (or sync-timeout 1000))]
      (when (and (or sync-cb true) callback)
        (callback c))
      (update-managers group data-id
                       {gtype (or type :string)
                        gmger manager
                        gconf c}))))

(defn single-manager
  [[group data-id & _ :as meta]]
  (reset! *current* [group data-id])
  (add-manager* meta))

(defn add-manager
  [& groups]
  (doseq [c groups]
    (add-manager* c)))

(defn get-map*
  [group data-id]
  (get-in @managers (map keyword [group data-id])))

(defn- get* [group data-id gkey]
  (get gkey (get-map* group data-id)))

(defn pro->map
  [^String s]
  (let [prop (Properties.)
        sr (StringReader. s)]
    (.load prop sr)
    (into {} (for [[k v] prop] [(keyword k) (read-string v)]))))

(defn yml->map
  [^String s]
  (yaml/parse-string s))

(defn get-conf
  [group data-id]
  (let [mmap (get-map* group data-id)
        conf-type (get mmap gtype)
        c (get mmap gconf)]
    (case conf-type
      :clojure (read-string c)
      :prop (pro->map c)
      :yml (yml->map c)
      :json (json/read-str c :key-fn keyword)
      :string c
      (ex-info "Un support type" {:type conf-type}))))

(defn env
  ([k default]
   (if-let [e (apply get-conf @*current*)]
     (if (vector? k)
       (if-let [result (get-in e k)]
         result
         default)
       (if-let [result (get e k)]
         result
         default))
     default))
  ([k]
   (env k nil)))

(defmacro with-current [group data-id & body]
  `(binding [*current* (atom ~[group data-id])]
     ~@body))

(defn get-manager
  ([]
   (apply get-manager @*current*))
  ([group data-id]
   (get* group data-id gmger)))

(defn- all*
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
