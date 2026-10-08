(ns imas-seamap.natural-hazards-atlas.views-test
  (:require [cljs.test :refer [deftest is testing]]
            [reagent.dom.server :refer [render-to-static-markup]]
            [imas-seamap.natural-hazards-atlas.views :as views]))

(defn- render->dom [hiccup]
  (let [el (js/document.createElement "div")]
    (set! (.-innerHTML el) (render-to-static-markup hiccup))
    el))

(deftest helper-overlay-steps-test
  (let [steps (views/helper-overlay-steps true)]
    (testing "help text has no leftover Seamap (habitat) wording"
      (is (empty? (filter #(re-find #"(?i)habitat" (str (:helperText %))) steps))))
    (testing "no step for the transect control, which NHAT removed"
      (is (not-any? #(= "transect-control" (:id %)) steps)))))

(deftest dont-show-again-checkbox-test
  (let [el    (render->dom [views/dont-show-again-checkbox false identity])
        input (.querySelector el "input[type=checkbox]")
        label (.querySelector el "label")]
    (testing "the label is linked to its checkbox, so clicking the text toggles it"
      (is (some? input))
      (is (seq (.-htmlFor label)))
      (is (= (.-htmlFor label) (.-id input))))))
