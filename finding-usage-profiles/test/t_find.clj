(ns t-find
  (:use find)
  (:use midje.sweet))


(def path-to-fixture-file "test/fixture.csv") 

(facts "building a map from csv"
       (fact "only transformation performed with data is the map-building-fn"
             (let [map-building-fn (fn [current new-entry] (assoc current (keyword (nth new-entry 0)) (nth new-entry 1)) )]
               (map-from-csv path-to-fixture-file map-building-fn)) => {:a "b" :c "d" :e "f"}))

(facts "about usage map per page building"
       (fact "associates user with page in empty map"
             (let [new-entry ["T942486" "36272" "424001662227" "HOME_TAB" "Home" "" "01/01/2010 06:44:20"] 
                   current-map {}]
               (assoc-user-with-page current-map new-entry) => {{:pages-visited ["HOME_TAB"]} #{"T942486"}}))
       
       (fact "associates user with page in non-empty map"
             (let [new-entry ["T666" "36272" "424001662227" "OTHER_TAB" "Other" "" "01/01/2010 06:44:20"]
                   current-map {{:pages-visited ["HOME_TAB"]} #{"T942486"}}]
               (assoc-user-with-page current-map new-entry) =>
               {{:pages-visited ["HOME_TAB"]} #{"T942486"}
                {:pages-visited ["OTHER_TAB"]} #{"T666"}}))
       
       (fact "associates user with already known page"
             (let [new-entry ["T666" "36272" "424001662227" "HOME_TAB" "Other" "" "01/01/2010 06:44:20"]
                   current-map {{:pages-visited ["HOME_TAB"]} #{"T942486"}}]
               (assoc-user-with-page current-map new-entry) =>
               {{:pages-visited ["HOME_TAB"]} #{"T942486" "T666"}}))

       (fact "does not associate user with page more than once"
             (let [new-entry ["T666" "36272" "424001662227" "HOME_TAB" "Other" "" "01/01/2010 06:44:20"]
                   current-map {{:pages-visited ["HOME_TAB"]} #{"T666"}}]
               (assoc-user-with-page current-map new-entry) =>
               {{:pages-visited ["HOME_TAB"]} #{"T666"}})))

(facts "about report-on-users-per-page generation"
       (let [profile-map {{:pages-visited ["A_TAB" "B_TAB"]} ["T1" "T3" "T2"]
                          {:pages-visited ["B_TAB"]} ["T6" "T7" "T3" "T2"]
                          {:pages-visited ["C_TAB"]} ["T1"]
                          {:pages-visited ["D_TAB"]} ["T5" "T3" "T2"]
                          {:pages-visited ["E_TAB" "X_TAB"]} ["T1"]}]

         (fact "it has the profiles sorted by number of users, desc"
               (report-on-users-per-page profile-map) =>
               [{:profile {:pages-visited ["B_TAB"]} :count 4}
                {:profile {:pages-visited ["D_TAB"]} :count 3}
                {:profile {:pages-visited ["A_TAB" "B_TAB"]} :count 3}
                {:profile {:pages-visited ["E_TAB" "X_TAB"]} :count 1}
                {:profile {:pages-visited ["C_TAB"]} :count 1}])))

(facts "about user to pages map bulding"
       (fact "adds pages to user in empty map"
             (let [new-entry ["T666" "36272" "424001662227" "HOME_TAB" "Other" "" "01/01/2010 06:44:20"]
                   current-map {}]
               (assoc-page-with-user current-map new-entry) => {"T666" {:pages-visited #{"HOME_TAB"}}}))       
       
       (fact "adds pages to new user"
             (let [new-entry ["T666" "36272" "424001662227" "HOME_TAB" "Other" "" "01/01/2010 06:44:20"]
                   current-map {"T111" {:pages-visited #{"OTHER_TAB"}}}]
               (assoc-page-with-user current-map new-entry) => {"T111" {:pages-visited #{"OTHER_TAB"}}
                                                                "T666" {:pages-visited #{"HOME_TAB"}}}))
       
       (fact "adds new pages to existing user"
             (let [new-entry ["T666" "36272" "424001662227" "HOME_TAB" "Other" "" "01/01/2010 06:44:20"]
                   current-map {"T111" {:pages-visited #{"OTHER_TAB"}}
                                "T666" {:pages-visited #{"OTHER_TAB"}}}]
               (assoc-page-with-user current-map new-entry) => {"T111" {:pages-visited #{"OTHER_TAB"}}
                                                                "T666" {:pages-visited #{"OTHER_TAB" "HOME_TAB"}}}))
       
       (fact "does not add duplicated pages to user"
             (let [new-entry ["T666" "36272" "424001662227" "SOME_TAB" "Other" "" "01/01/2010 06:44:20"]
                   current-map {"T111" {:pages-visited #{"OTHER_TAB"}}
                                "T666" {:pages-visited #{"SOME_TAB"}}}]
               (assoc-page-with-user current-map new-entry) => {"T111" {:pages-visited #{"OTHER_TAB"}}
                                                                "T666" {:pages-visited #{"SOME_TAB"}}})))

(facts "about grouping users by the page they visited"
       (fact "maps from pages-for-user to profiles-for-user"
             (let [pages-per-user-map {"T1" {:pages-visited #{"A" "B" "C"}}
                                       "T2" {:pages-visited #{"A" "C"}}}]
               (map to-user-profile-map pages-per-user-map) => (list
                                                                {:user "T1" :profiles (list '("A") '("B") '("C") '("A" "B") '("A" "C") '("B" "C") '("A" "B" "C"))}
                                                                {:user "T2" :profiles (list '("A") '("C") '("A" "C"))})))
       (fact "users are aggregated by their profiles"
             (add-user-to-consolidated-map {} {:user "T1" :profiles (list '("A") '("B") '("A" "B"))}) => {'("A" "B") '("T1") '("B") '("T1") '("A") '("T1")}
             (add-user-to-consolidated-map {'("A" "B") '("T1") '("B") '("T1") '("A") '("T1")} {:user "T2" :profiles (list '("A"))}) => {'("A" "B") '("T1") '("B") '("T1") '("A") '("T2" "T1")})
       
       
       (fact ""
             (let [user-map {"T1" {:pages-visited #{"A" "B" "C"}}
                             "T2" {:pages-visited #{"A" "C"}}
                             "T3" {:pages-visited #{"A" "B" "C"}}
                             "T4" {:pages-visited #{"A" "B" "Z"}}
                             "T5" {:pages-visited #{"X" "Y" "Z"}}}
                   expected-profile-map {'("A" "B" "C") '("T1" "T3")
                                         '("A" "C")     '("T1" "T2" "T3")
                                         '("A" "B")     '("T1" "T3" "T4")
                                         '("X" "Y" "Z") '("T5")}]
               
               (consolidate-in-profiles user-map) => expected-profile-map))
       (fact "profiles must have more than one member"
             (let [user-map {"T1" {:pages-visited #{"A" "B"}}
                             "T2" {:pages-visited #{"A" "C"}}
                             "T3" {:pages-visited #{"D" "E"}}
                             "T4" {:pages-visited #{"F" "G" "H"}}}
                   expected-profile-map { #{"A"} #{"T1" "T2"}}]
               
               (consolidate-in-profiles user-map) => expected-profile-map)))


