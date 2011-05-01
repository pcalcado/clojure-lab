(ns find
  (:require clojure.contrib.duck-streams)
  (:use clojure-csv.core))

(defn usage-map-from-csv [path usage-map-building-fn]
  (with-open [rdr (java.io.BufferedReader. 
                   (java.io.FileReader. path))]
    (let [all-lines (line-seq rdr)
          all-entries (map (comp first parse-csv) all-lines)
          data-entries (next all-entries)]
      (reduce usage-map-building-fn {} data-entries))))


(defn profile [profile-map tab-visited]
  (let [desired-profile {:pages-visited [tab-visited]}]
    (cond
     (contains? profile-map desired-profile) [desired-profile (get  profile-map desired-profile)]
     :else [desired-profile #{}])))

(defn assoc-user-with-page [profile-map new-entry]
  (let [tab (nth new-entry 3)
        user (nth new-entry 0)
        p (profile profile-map tab)
        visits (first p)
        users (second p)]
    (assoc profile-map visits (conj users user))))

(defn report-on [profile-map]
  (let [profiles-and-users-count
        (map (fn [p u] {:profile p :count (count u)}) (keys profile-map) (vals profile-map))]
    (reverse (sort-by last profiles-and-users-count))))
