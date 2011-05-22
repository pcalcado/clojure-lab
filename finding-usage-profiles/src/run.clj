(ns run
  (:use find))

(def pages-by-user (map-from-csv "data/log.csv" assoc-page-with-user))

(def usergroup-by-pages (users-grouped-by-pages pages-by-user))

(def report (report-on-page-groups usergroup-by-pages))

