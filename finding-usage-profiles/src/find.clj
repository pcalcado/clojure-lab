(ns find
  (:use clojure-csv.core)
  (:use clojure.contrib.combinatorics))

(defn- add-to-map-list [current-map key new-item]
     (assoc current-map key (conj (get current-map key) new-item)))

(defn- user-in [csv-entry] (nth csv-entry 0))

(defn- tab-in [csv-entry] (nth csv-entry 3))

(defn- profile-by-tabs [profile-map tab-visited]
  (let [desired-profile {:pages-visited [tab-visited]}]
    (cond
     (contains? profile-map desired-profile) [desired-profile (get  profile-map desired-profile)]
     :else [desired-profile #{}])))

(defn- pages-for-user [profile-map user]
  (or (:pages-visited (get profile-map user)) #{}))

(defn map-from-csv [path map-building-fn]
  (with-open [rdr (java.io.BufferedReader. 
                   (java.io.FileReader. path))]
    (let [all-lines (line-seq rdr)
          all-entries (map (comp first parse-csv) all-lines)
          data-entries (next all-entries)]
      (reduce map-building-fn {} data-entries))))

(defn assoc-user-with-page [profile-map new-entry]
  (let [p (profile-by-tabs profile-map (tab-in new-entry))
        visits (first p)
        users (second p)]
    (assoc profile-map visits (conj users (user-in new-entry)))))

(defn assoc-page-with-user [profile-map new-entry]
  (let [current-pages (pages-for-user profile-map (user-in new-entry))]
    (assoc profile-map (user-in new-entry) {:pages-visited (conj current-pages (tab-in new-entry))})))

(defn users-grouped-by-pages-used [user-map]
  (reduce (fn [acc cur]
            (let [user (first cur)
                  pages(:pages-visited (second cur))]
              (add-to-map-list acc pages user))) {} user-map))

(defn report-on-users-per-page [profile-map]
  (let [profiles-and-users-count
        (map (fn [p u] {:profile p :count (count u)}) (keys profile-map) (vals profile-map))]
    (reverse (sort-by last profiles-and-users-count))))

(defn report-on-page-groups [users-grouped-by-pages]
  (let [users-in-group-count (map (fn[c] [(first c) (count (second c))]) users-grouped-by-pages)]
   (reverse (sort-by last users-in-group-count))))


                                        ;anonymise source to publish dataset
(import (java.security MessageDigest)) 
(defn hash-fn [input]
(let [md (MessageDigest/getInstance "SHA1")]
(. md update (.getBytes input))
(let [digest (.digest md)]
(apply str (map #(format "%02x" (bit-and % 0xff)) digest)))))


(defn anonymise-data [path new-file]
  (with-open [rdr (java.io.BufferedReader. 
                   (java.io.FileReader. path))
              wtr (java.io.PrintWriter. (java.io.BufferedWriter. (java.io.FileWriter. new-file)))]
    (let [all-lines (line-seq rdr)
          all-entries (map (comp first parse-csv) all-lines)
          data-entries (next all-entries)]
      (. wtr write (write-csv (map (fn [e] [(hash-fn (nth e 0))
                                          (hash-fn (nth e 1))
                                          (nth e 2)
                                          (. (. (nth e 3) replaceAll "PWTT" "WEB") replaceAll "POWERTEL" "BIZ")
                                          (hash-fn (nth e 4))
                                          (hash-fn (nth e 5))
                                          (nth e 6)]) data-entries))))))



