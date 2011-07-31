(ns barulho.t-core
  (:use [barulho core])
  (:use [clojure.data json])
  (:use [midje.sweet]))

(def search-result "{\"completed_in\":0.08,\"max_id\":97286930944892929,\"max_id_str\":\"97286930944892929\",\"next_page\":\"?page=2&max_id=97286930944892929&q=soundcloud\",\"page\":1,\"query\":\"soundcloud\",\"refresh_url\":\"?since_id=97286930944892929&q=soundcloud\",
 \"results\":[
   {\"created_at\":\"Sat, 30 Jul 2011 12:45:48 +0000\",\"from_user\":\"IamPriscillaJ\",\"from_user_id\":368892088,\"from_user_id_str\":\"368892088\",\"geo\":{\"coordinates\":[40.807798,-73.945538],\"type\":\"Point\"},\"id\":97286772580560898,\"id_str\":\"97286772580560898\",\"iso_language_code\":\"en\",\"metadata\":{\"result_type\":\"recent\"},\"profile_image_url\":\"http://a2.twimg.com/profile_images/1424787789/image_normal.jpg\",\"source\":\"&lt;a href=&quot;http://twitter.com/#!/download/iphone&quot; rel=&quot;nofollow&quot;&gt;Twitter for iPhone&lt;/a&gt;\",\"text\":\"@TheHarlemHotBoy #SUPPORTME PLEASE #ImFromHARLEM LISTEN N GIVE ME FEEDBACK ON MY TRACK &quot;I NEED A BOSS&quot; http://t.co/HLR1IDt\",\"to_user\":\"TheHarlemHotBoy\",\"to_user_id\":147875248,\"to_user_id_str\":\"147875248\"},
   {\"created_at\":\"Sat, 30 Jul 2011 12:45:47 +0000\",\"from_user\":\"CarrieCare76\",\"from_user_id\":166213192,\"from_user_id_str\":\"166213192\",\"geo\":{\"coordinates\":[8.816667,48.833333],\"type\":\"Point\"},\"id\":97286766683373568,\"id_str\":\"97286766683373568\",\"iso_language_code\":\"en\",\"metadata\":{\"result_type\":\"recent\"},\"profile_image_url\":\"http://a0.twimg.com/profile_images/1423336863/LAilaa71048-L_normal.jpg\",\"source\":\"&lt;a href=&quot;http://twitter.com/&quot;&gt;web&lt;/a&gt;\",\"text\":\"@megan_mclanahan No, I found this link- http://t.co/3dXWZKM\",\"to_user\":\"megan_mclanahan\",\"to_user_id\":149382788,\"to_user_id_str\":\"149382788\"},
   {\"created_at\":\"Sat, 30 Jul 2011 12:45:43 +0000\",\"from_user\":\"NoGravityShow\",\"from_user_id\":189711070,\"from_user_id_str\":\"189711070\",\"geo\":null,\"id\":97286748882747392,\"id_str\":\"97286748882747392\",\"iso_language_code\":\"en\",\"metadata\":{\"result_type\":\"recent\"},\"profile_image_url\":\"http://a3.twimg.com/profile_images/1241519182/10-en-aee254254d8d0dd32c2177ca74556805_normal.jpg\",\"source\":\"&lt;a href=&quot;http://www.facebook.com/twitter&quot; rel=&quot;nofollow&quot;&gt;Facebook&lt;/a&gt;\",\"text\":\"Here, Have This. -VJ http://fb.me/FYNvtDFp\",\"to_user_id\":null,\"to_user_id_str\":null}]
 ,\"results_per_page\":15,\"since_id\":0,\"since_id_str\":\"0\"}")


(facts "about twitter search consuming"
       (fact "should search for soundloud-related posts on twitter"
             (search-twitter!) => (read-json search-result)
             (provided
              (slurp anything) => search-result)))

(facts "about json parsing"
       (fact "should collect all links in search"
             (links-in-stream (read-json search-result)) => ["http://t.co/HLR1IDt" "http://t.co/3dXWZKM" "http://fb.me/FYNvtDFp"]))



