(ns ul-feedback.core
  (:require [mount.core :refer [defstate start stop]])
  (:gen-class)
  (:import (com.ullink.slack.simpleslackapi.impl SlackSessionFactory)
           (java.net Proxy$Type)
           (com.ullink.slack.simpleslackapi.events SlackMessagePosted SlackEvent)
           (com.ullink.slack.simpleslackapi SlackSession)
           (com.ullink.slack.simpleslackapi.listeners SlackMessagePostedListener)))

(defstate configuration :start (read-string (slurp "conf/init.edn")))

(defn createListener []
  (reify SlackMessagePostedListener
    (onEvent [_ event session]
      (println (.getMessageContent event) "from" (-> event .getSender .getRealName)))))

(defn startSlack []
  (let [session
        (-> (SlackSessionFactory/getSlackSessionBuilder (get configuration :slack-bot-auth-token))
            (.withProxy Proxy$Type/HTTP (get configuration :proxy-host) (get configuration :proxy-port))
            (.build))]
    (.addMessagePostedListener session (createListener))
    (.connect session)
    session))

(defstate session
          :start (startSlack)
          :stop (.disconnect session))

(def db {"vlad" {"what do you think about clojure hands-on?" {"nath" nil "benoit" nil "xavier" nil}
                 "what do you think about ul-conf?"          {"greg" nil "benoit" nil}}
         "nath" {"what do you think about ce?" {"vlad" nil "xavier" nil}}})

(defn- update-target-audience [db user question target-audience]
  (reduce #(assoc %1 %2 nil) (get-in db [user question]) target-audience))

(defn- wrap-as-array [elem]
  (if-not (coll? elem) [elem] elem))

(defn ask-for-feedback [db user question target-audience]
  (->> target-audience
       wrap-as-array
       (update-target-audience db user question)
       (assoc-in db [user question])))

(defn show-questions [db user]
  (-> db
      (get user)
      keys))

(defn- is-question-applicable [user [_ audience]]
  (and
    (contains? audience user)
    (nil? (get audience user))))

(defn- get-questions [user acc [from questions-and-audience]]
  (->> questions-and-audience
       (filter (partial is-question-applicable user))
       (map first)
       (map (fn [q] {:question q :from from}))
       (concat acc)))

(defn incoming-requests [db user]
  (reduce (partial get-questions user) () db))

(defn answer-question [db user {:keys [question from]} answer]
  (assoc-in db [from question user] answer))

(defn see-answers [db user]
  (get db user))
