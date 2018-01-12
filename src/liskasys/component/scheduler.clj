(ns liskasys.component.scheduler
  (:require [com.stuartsierra.component :as component]
            [liskasys.service :as service]
            [taoensso.timbre :as timbre]
            [twarc.core :as twarc])
  (:import java.util.TimeZone))

(twarc/defjob process-lunch-order-and-substitutions-job [scheduler conn]
  (try
    (service/process-lunch-order-and-substitutions conn)
    (catch Exception e
      (timbre/error "process-lunch-order-and-substitutions-job error" e))))

(defrecord Scheduler [datomic twarc-scheduler quartz-props]
  component/Lifecycle
  (start [component]
    (let [sched (-> (twarc/make-scheduler quartz-props)
                    twarc/start)]
      (doseq [[db-key conn] (:conns datomic)]
        (timbre/info db-key "Scheduling periodic tasks")
        ;; cron expression: sec min hour day-of-mon mon day-of-week ?year
        (process-lunch-order-and-substitutions-job sched
                                                   [conn]
                                                   :trigger {:cron {:expression "0 0 8 * * ?" ;; at 8:00:00 AM
                                                                    :misfire-handling :fire-and-process
                                                                    :time-zone (TimeZone/getTimeZone "Europe/Prague")}}))
      (assoc component :twarc-scheduler sched)))
  (stop [component]
    (when twarc-scheduler
      (timbre/info "Stopping scheduler")
      (twarc/stop twarc-scheduler))
    (assoc component :twarc-scheduler nil)))

(defn scheduler []
  (map->Scheduler {:quartz-props
                   {:threadPool.class "org.quartz.simpl.SimpleThreadPool"
                    :threadPool.threadCount 1
                    :plugin.triggHistory.class "org.quartz.plugins.history.LoggingTriggerHistoryPlugin"
                    :plugin.jobHistory.class "org.quartz.plugins.history.LoggingJobHistoryPlugin"}}))
