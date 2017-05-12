(ns imas-seamap.munging
  "We need to automatically extract colours mapped against different
  SM_HAB_CLS values for the plot.  We do this by parsing the geoserver
  SLD (styled layer descriptor) files.  Designed to be used
  interactively from the REPL; not part of the client-application
  itself."
  (:require [clojure.data.xml :as xml]
            [clojure.data.zip.xml :as zx]
            [clojure.java.io :as io]
            [clojure.zip :as zip]
            [globber.glob :refer [glob]]))


(xml/alias-uri 'sld "http://www.opengis.net/sld")
(xml/alias-uri 'ogc "http://www.opengis.net/ogc")

(defn- rule->mapping [rule]
  [[(zx/xml1-> rule ::sld/Title zx/text)
    (zx/xml1-> rule ::sld/PolygonSymbolizer ::sld/Fill ::sld/CssParameter (zx/attr= :name "fill") zx/text)]])

(defn- sld->colour-map [zipped-sld]
  (let [rules (zx/xml-> zipped-sld
                        ::sld/StyledLayerDescriptor
                        ::sld/NamedLayer
                        ::sld/UserStyle
                        ::sld/FeatureTypeStyle
                        ::sld/Rule
                        rule->mapping)]
    (into {} rules)))

(defn colour-scheme-from-sld
  "We need a colour scheme, mapping habitat classifications to
  colours. This also needs to match the same scheme used in the
  geoserver layers.  We achieve this by this utility, which parses a
  geoserver .sld file and extracting the colour mapping from it."
  [filename]
  (-> filename
      io/resource
      slurp
      xml/parse-str
      zip/xml-zip
      sld->colour-map))

;;; TODO:
;;; * Extract all of sld:Title, ogc:Literal, and the fill colour (keyed by literal?)
;;; * Warn about nodes that have no fill (it's probably an image), or a complex-expression (ogc:Or, ogc:And; 16 items)
;;; * expand out globs against the habitats list
;;; * Another lookup, SM_HAB_CLS -> Title (matches legend)
