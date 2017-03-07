(ns ul-feedback.core
  (:require [mount.core :refer [defstate start stop]]
            [instaparse.core :refer [parser failure?]]
            [instaparse.transform :refer [transform]]
            [selmer.parser :refer [render-file]])
  (:gen-class)
  (:import (com.ullink.slack.simpleslackapi.impl SlackSessionFactory)
           (java.net Proxy$Type)
           (com.ullink.slack.simpleslackapi.events SlackMessagePosted SlackEvent)
           (com.ullink.slack.simpleslackapi SlackSession)
           (com.ullink.slack.simpleslackapi.listeners SlackMessagePostedListener)))

(def whitespace (parser "whitespace = #'\\s+'"))
(def query-parser (parser (clojure.java.io/resource "query.bnf") :auto-whitespace whitespace))

(defstate configuration :start (read-string (slurp "conf/init.edn")))

(defonce db (atom {"vlad" {"what do you think about clojure hands-on?" {"nath" nil "benoit" nil "xavier" nil}
                           "what do you think about ul-conf?"          {"greg" nil "benoit" nil}}
                   "nath" {"what do you think about ce?" {"vlad" nil "xavier" nil}}}))

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
  ; TODO : provide user name
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

(defn find-user-by-id [session user]
  (->> user
       (.findUserById session)
       .getUserName))

(defn extract-template [questions]
  (render-file "list.template" {:questions questions}))

(defn reply-to-slack-request [session event details]
  (.sendMessage session (.getChannel event) (extract-template details)))

(defn notify-users-question-asked [session user question from]
  (.sendMessageToUser session user (str question " from " from) nil))

(defn ask-question [users text]
  (fn [db session from event]
    (let [interpreted-users (users session)]
      (swap! db ask-for-feedback from text interpreted-users)
      (doseq [user interpreted-users]
        (notify-users-question-asked session (.findUserByUserName session user) text from))))
  )

(def compiler {
               :text  identity
               :users (fn [& user-ids] (fn [session] (map (partial find-user-by-id session) user-ids)))
               :ask   ask-question
               :list  (fn [] (fn [db session from event] (reply-to-slack-request session event (show-questions @db from))))
               })

(defn act-on-event [event session]
  (let [compiled (->> event
                      .getMessageContent
                      query-parser
                      (transform compiler)
                      first)
        username (-> event .getSender .getUserName)]
    (println compiled)
    (println username)
    ;(if (failure? (query-parser (->> event .getMessageContent)))
    ;  (reply-to-slack-request session event (query-parser (->> event .getMessageContent)))
    ;  )
    (compiled db session username event)
    (println @db)
    ))

(defn createListener []
  (reify SlackMessagePostedListener
    (onEvent [_ event session]
      ; TODO ignore message from bot
      (act-on-event event session))))


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
