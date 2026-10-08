(ns imas-seamap.natural-hazards-atlas.state-test
  (:require [cljs.test :refer [deftest is testing]]
            [goog.crypt.base64 :as b64]
            [cognitect.transit :as t]
            [imas-seamap.utils :as utils]
            [imas-seamap.natural-hazards-atlas.utils :as nhatutils]))

(defn- hash-of [state] (b64/encodeString (t/write (t/writer :json) state)))

(deftest parse-state-does-not-shadow-map-2-with-nils
  ;; A save/share hash from a session where compare mode was never used has no
  ;; map-2 state. Map 2 should then fall back to map 1, not become nil.
  (let [db     {:current-view {:is-historic? true :selected-time-period-id "all"}}
        hash   (hash-of {:current-view {:is-historic? false :selected-time-period-id "long"}})
        merged (utils/merge-in db (nhatutils/parse-state hash))]
    (testing "map 1 gets the hashed value"
      (is (= false (nhatutils/current-view-is-historic? merged))))
    (testing "map 2 (never independently set) falls back to map 1"
      (is (= false (nhatutils/current-view-is-historic? merged :map-2))))
    (testing "no nil-valued independent state is introduced"
      (is (nil? (get-in merged [:independent-map-state :map-2]))))))

(deftest parse-state-keeps-real-map-2-values
  (let [hash   (hash-of {:current-view          {:is-historic? false}
                         :independent-map-state {:map-2 {:current-view {:is-historic? true}}}})
        merged (utils/merge-in {} (nhatutils/parse-state hash))]
    (is (= true (nhatutils/current-view-is-historic? merged :map-2)))
    (is (= false (nhatutils/current-view-is-historic? merged)))))
