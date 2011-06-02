(ns run
  (:use find))

(def pages-by-user (map-from-csv "data/log.csv" assoc-page-with-user))

(def usergroup-by-pages (users-grouped-by-pages-used pages-by-user))

(def report-usergroup-by-pages (report-on-page-groups usergroup-by-pages))

(def page-and-its-users (map-from-csv "data/log.csv" assoc-user-with-page))

(def report-top-pages (report-on-users-per-page page-and-its-users))
