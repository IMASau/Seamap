;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.specs.components
  (:require
   [cljs.spec.alpha :as s]
   [imas-seamap.components :as components]))

;; TODO: Some of these are s/nilable, but they shouldn't need to be. Optional props
;; should be excluded from the hash map (no key) if they don't have a value, but
;; currently some of Seamap has taken shortcuts and passed nils.
(s/def :floating-pill-control-menu.props/text string?)
(s/def :floating-pill-control-menu.props/icon string?)
(s/def :floating-pill-control-menu.props/disabled? (s/nilable boolean?))
(s/def :floating-pill-control-menu.props/expanded? (s/nilable boolean?))
(s/def :floating-pill-control-menu.props/on-open-click (s/nilable fn?))
(s/def :floating-pill-control-menu.props/on-close-click (s/nilable fn?))
(s/def :floating-pill-control-menu.props/reset-click (s/nilable fn?))
(s/def :floating-pill-control-menu.props/active? (s/nilable boolean?))
(s/def :floating-pill-control-menu.props/tooltip (s/nilable string?))
(s/def :floating-pill-control-menu.props/id (s/nilable string?))
(s/def :floating-pill-control-menu/props
  (s/keys
   :req-un
   [:floating-pill-control-menu.props/text
    :floating-pill-control-menu.props/icon]
   :opt-un
   [:floating-pill-control-menu.props/disabled?
    :floating-pill-control-menu.props/expanded?
    :floating-pill-control-menu.props/on-open-click
    :floating-pill-control-menu.props/on-close-click
    :floating-pill-control-menu.props/reset-click
    :floating-pill-control-menu.props/active?
    :floating-pill-control-menu.props/tooltip
    :floating-pill-control-menu.props/id]))
(s/def :floating-pill-control-menu/children (s/* vector?)) ; hiccup

(s/fdef components/floating-pill-control-menu
  :args
  (s/cat
   :props    :floating-pill-control-menu/props
   :children :floating-pill-control-menu/children))
