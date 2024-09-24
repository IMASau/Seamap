;;; Seamap: view and interact with Australian coastal habitat data
;;; Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
;;; Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
(ns imas-seamap.specs.state-of-knowledge.views
  (:require
   [cljs.spec.alpha :as s]
   [imas-seamap.state-of-knowledge.views :as views]))

(s/def :data-sources-tooltip.props/data-providers string?) ; rich-text string
(s/def :data-sources-tooltip/props
  (s/keys :req-un [:data-sources-tooltip.props/data-providers]))

(s/fdef views/data-sources-tooltip
  :args (s/cat :props :data-sources-tooltip/props))
