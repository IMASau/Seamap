(ns imas-seamap.natural-hazards-atlas.views-test
  (:require [cljs.test :refer [deftest is testing]]
            [reagent.dom.server :refer [render-to-static-markup]]
            [imas-seamap.natural-hazards-atlas.views :as views]))

(defn- render->dom [hiccup]
  (let [el (js/document.createElement "div")]
    (set! (.-innerHTML el) (render-to-static-markup hiccup))
    el))

(deftest dont-show-again-checkbox-test
  (let [el    (render->dom [views/dont-show-again-checkbox false identity])
        input (.querySelector el "input[type=checkbox]")
        label (.querySelector el "label")]
    (testing "the label is linked to its checkbox, so clicking the text toggles it"
      (is (some? input))
      (is (seq (.-htmlFor label)))
      (is (= (.-htmlFor label) (.-id input))))))
