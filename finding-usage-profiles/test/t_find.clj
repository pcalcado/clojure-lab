(ns t-find
  (:use find)
  (:use midje.sweet))

(def path-to-fixture-file "test/fixture.csv") 

(facts "building a usage map from csv"
       (fact "only transformation performed with data is the usage-map-building-fn"
             (let [usage-map-building-fn (fn [current new-entry] (assoc current (keyword (nth new-entry 0)) (nth new-entry 1)) )]
               (usage-map-from-csv path-to-fixture-file usage-map-building-fn)) => {:a "b" :c "d" :e "f"}))

(facts "about usage map building"
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


(facts "about finding profiles"
       (let [profile-map {{:pages-visited ["HOME_TAB"]} ["T942486"]}]            
         (fact "finds an existing profile in a map"
               (profile profile-map "HOME_TAB") => [{:pages-visited ["HOME_TAB"]} ["T942486"]])
         
         (fact "returns an empty profile if cannot find an existing profile in a map"
               (profile profile-map "NEW_TAB") => [{:pages-visited ["NEW_TAB"]} []])))

(facts "about report generation"
       (let [profile-map {{:pages-visited ["A_TAB" "B_TAB"]} ["T1" "T3" "T2"]
                          {:pages-visited ["B_TAB"]} ["T6" "T7" "T3" "T2"]
                          {:pages-visited ["C_TAB"]} ["T1"]
                          {:pages-visited ["D_TAB"]} ["T5" "T3" "T2"]
                          {:pages-visited ["E_TAB" "X_TAB"]} ["T1"]}]

         (fact "it has the profiles sorted by number of users, desc"
               (report-on profile-map) =>
               [{:profile {:pages-visited ["B_TAB"]} :count 4}
                {:profile {:pages-visited ["D_TAB"]} :count 3}
                {:profile {:pages-visited ["A_TAB" "B_TAB"]} :count 3}
                {:profile {:pages-visited ["E_TAB" "X_TAB"]} :count 1}
                {:profile {:pages-visited ["C_TAB"]} :count 1}])))
