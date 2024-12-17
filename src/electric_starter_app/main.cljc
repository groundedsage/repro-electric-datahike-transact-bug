(ns electric-starter-app.main
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [nano-id.core :refer [nano-id]]
            #?(:clj [datahike.api :as d])
            [hyperfiddle.electric-forms0 :as cqrs :refer
             [Input! Button! Checkbox! Checkbox Form! Service try-ok effects*]]
            #_[electric-tutorial.chat-monitor :refer [ChatMonitor]]))

(e/declare db)

#?(:clj (def cfg {:store {:backend :mem
                          :id "schemaless"}
                  :schema-flexibility :read}))

#?(:clj (def schema [{:db/ident :user/id
                      :db/valueType :db.type/string
                      :db/unique :db.unique/identity
                      :db/cardinality :db.cardinality/one}
                     {:db/ident :user/contacts
                      :db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/many}]))

#?(:clj (defonce create-db (when-not (d/database-exists? cfg) (d/create-database cfg))))
#?(:clj (defonce conn (d/connect cfg)))
#?(:clj (defonce dh-schema-tx (d/transact conn {:tx-data schema})))


#?(:clj (defonce !toggle (atom false)))

;; Init user
#?(:clj (defonce user1-id (nano-id)))


#?(:clj (defn get-user-data [db user1-id]
          (d/pull db '[*] [:user/id user1-id])))

#?(:clj (defn add-contact [new-contact-id]
          (d/transact conn {:tx-data [{:db/id -1
                                       :user/id new-contact-id}
                                      {:db/id [:user/id user1-id]
                                       :user/contacts  -1}]})))

#?(:clj (defonce init-example 
          (do
            (d/transact conn {:tx-data [{:user/id user1-id
                                         :user/name "Alice"}]})
            (add-contact (nano-id)))))


(e/defn Add-contact [new-contact]
  (e/server 
    (e/Offload #(try 
                  (swap! !toggle not)
                  (println "Add contact" new-contact) 

                  ;; Why does this freeze electric app?
                  ;; (add-contact new-contact)
                  ::ok
                  (catch Exception e (doto ::fail (prn e)))))))

(e/defn Effects []
  (e/client
    {`Add-contact Add-contact}))


#?(:cljs (defn pretty-print [data]
           (with-out-str (cljs.pprint/pprint data))))

(e/defn Main [ring-request]
  (e/client
    (binding [dom/node js/document.body
              e/http-request (e/server ring-request)
              cqrs/effects* (Effects)
              db (e/server (e/watch conn))]
      ; mandatory wrapper div https://github.com/hyperfiddle/electric/issues/74
      (dom/div (dom/props {:style {:display "contents"}})
        (dom/pre (dom/text (pretty-print (e/server (e/Offload #(try
                                                                 (get-user-data db user1-id) 
                                                                 (catch Exception e (prn "Failure in: get-contacts" e))))))))
        (dom/h1 (dom/text "Server toggle: " (e/server (e/watch !toggle))))
        (Service (Button! [`Add-contact (nano-id)]
                   :label "Add to contacts"))))))

(comment 
  
  (add-contact (nano-id))

  (get-user-data @conn user1-id) 

  )