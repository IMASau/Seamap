(ns imas-seamap.plot.colours)


(def ^:private -habitats
  ["< 0m"
   "< 15%"
   "< 2%"
   "< 25 g/m2"
   "> 1000 g/m2"
   "> 200m"
   "> 75%"
   "0 - 20m"
   "0%"
   "100-1000 g/m2"
   "10-40 %"
   "1-10 %"
   "1-10%"
   "11-30%"
   "15 - 45%"
   "2 - 15%"
   "20 - 60m"
   "25-50 g/m2"
   "31-50%"
   "40-70 %"
   "45 - 75%"
   "50-75 g/m2"
   "51-75%"
   "60 - 200m"
   "70-100%"
   "75-100 g/m2"
   "76-100%"
   "Abyssal-plain / Deep ocean floor"
   "Aegiceras corniculatum"
   "Aegiceras corniculatum / Avicennia marina"
   "Aegiceras corniculatum / Avicennia marina / Saltmarsh"
   "Aegiceras corniculatum / Saltmarsh"
   "Algal beds"
   "Algal mat"
   "Apron/Fan"
   "Aquatic macrophytes"
   "Arid/ Semi-arid floodplain grass, sedge, herb swamps"
   "Arid/ Semi-arid floodplain lignum swamps"
   "Arid/ Semi-arid floodplain tree swamps"
   "Arid/ Semi-arid fresh floodplain lakes"
   "Arid/ Semi-arid fresh non-floodplain lakes"
   "Arid/ Semi-arid fresh non-floodplain lakes - claypans"
   "Arid/ Semi-arid non-floodplain (spring) swamps"
   "Arid/ Semi-arid non-floodplain grass, sedge, herb swamps"
   "Arid/ Semi-arid non-floodplain lignum swamps"
   "Arid/ Semi-arid non-floodplain tree swamps"
   "Arid/ Semi-arid saline lakes"
   "Arid/ Semi-arid saline swamps"
   "Artificial substrate"
   "Artificial/ highly modified wetlands (dams, ring tanks, irrigation channel"
   "Avicennia marina"
   "Avicennia marina / Bruguiera exaristata"
   "Avicennia marina / Ceriops tagal"
   "Avicennia marina / Saltmarsh"
   "Backreef / shallow lagoon"
   "Bank"
   "Bank/Shoals"
   "Bare coarse sediment with possibility of small rocky outcrops/rippled sand"
   "Bare coarse sediment with rippled sand"
   "Bare reef (intertidal)"
   "Bare reef (subtidal)"
   "Bare rocky reef"
   "Bare sand and mud (either flats/bars)"
   "Bare sandy to mixed sediments"
   "Bare substrate"
   "Barrier/back-barrier"
   "Basin"
   "Beach"
   "Bedrock"
   "Benthic micro-algae"
   "Biosiliceous marl and calcareous clay"
   "Bioturbated mud"
   "Bioturbated sand"
   "Blue hole"
   "Bommies"
   "Boulder / cobble / shingle / pebble / sand"
   "Boulder+Rubble"
   "Brackish lake"
   "Branching coral"
   "Branching live coral, reef slope"
   "Bruguiera (closed forest)"
   "Bruguiera (open woodland)"
   "Bruguiera exaristata"
   "Bruguiera parviflora"
   "Bruguiera parviflora / Rhizophora stylosa"
   "Calcareous gravel, sand and silt"
   "Calcareous ooze"
   "Canyon"
   "Caulerpa"
   "Caulerpa/Halophila ovalis"
   "Cay"
   "Central basin"
   "Ceriops tagal"
   "Channel"
   "Channel - deep (10 - 20 metres)"
   "Channel - moderate (5 - 10 metres)"
   "Channel - shallow (< 5 metres)"
   "Coastal/ Sub-coastal floodplain grass, sedge and herb swamps"
   "Coastal/ Sub-coastal floodplain lakes"
   "Coastal/ Sub-coastal floodplain tree swamps (Melaleuca and Eucalypt)"
   "Coastal/ Sub-Coastal floodplain wet heath swamps"
   "Coastal/ Sub-Coastal non-floodplain (spring) swamps"
   "Coastal/ Sub-coastal non-floodplain grass, sedge and herb swamps"
   "Coastal/ Sub-coastal non-floodplain rock lakes"
   "Coastal/ Sub-coastal non-floodplain sand lakes"
   "Coastal/ Sub-coastal non-floodplain sand lakes (Perched)"
   "Coastal/ Sub-coastal non-floodplain sand lakes (Window)"
   "Coastal/ Sub-coastal non-floodplain soil lakes"
   "Coastal/ Sub-Coastal non-floodplain tree swamps (Melaleuca and Eucalypt)"
   "Coastal/ Sub-Coastal non-floodplain wet heath swamps"
   "Coastal/ Sub-Coastal saline swamps"
   "Coastal/ Sub-Coastal tree swamps (palm)"
   "Coastline - sand"
   "Cobble"
   "Continental-rise"
   "Coral"
   "Coral communities"
   "Coral reef (intertidal)"
   "Coral reef (subtidal)"
   "Coralline algae"
   "Cymodocea rotundata"
   "Cymodocea serrulata"
   "Cymodocea serrulata with mixed species"
   "Cymodocea serrulata/Halodule uninervis (wide form)"
   "Cymodocea serrulata/Syringodium isoetifolium"
   "Cymodocea serrulata/Zostera muelleri"
   "Deep areas"
   "Deep lagoon"
   "Deep reef flat"
   "Deep reef structures"
   "Deep, low slope, unconsolidated"
   "Deep/Hole/Valley"
   "Depression"
   "Diverse sandy"
   "Dredged"
   "Dune vegetation, undifferentiated"
   "Embayment - subtidal sone"
   "Enhalus acoroides"
   "Enhalus acoroides with mixed species"
   "Enhalus acoroides/Halodule uninervis (wide form)"
   "Enhalus acoroides/Halophila ovalis"
   "Eroded sandstone"
   "Escarpment"
   "Estuarine - Mangroves and related tree communities"
   "Estuarine - salt flats and saltmarshes"
   "Estuarine - water"
   "Exposed Rock"
   "Filter feeders"
   "Fine sediment"
   "Flood- and ebb-tide delta"
   "Fluvial (bay-head) delta"
   "Freshwater"
   "Fringing coral reefs"
   "Grass, succulent"
   "Gravel"
   "Gravel/rubble"
   "Halodule uninervis"
   "Halodule uninervis (narrow and wide forms)"
   "Halodule uninervis (narrow form)"
   "Halodule uninervis (narrow form) with mixed species"
   "Halodule uninervis (narrow form)/Enhalus acoroides"
   "Halodule uninervis (narrow form)/Halophila decipiens"
   "Halodule uninervis (wide form)"
   "Halodule uninervis (wide form) with mixed species"
   "Halodule uninervis (wide form)/Halophila ovalis"
   "Halodule uninervis (wide form)/Thalassia hemprichii"
   "Halodule uninervis (wide form)/Thalassia hemprichii/Halophila ovalis"
   "Halodule uninervis/Cymodocea serrulata"
   "Halodule uninervis/Halophila ovails"
   "Halophila"
   "Halophila / Ruppia"
   "Halophila / Zostera"
   "Halophila capricorni"
   "Halophila decipiens"
   "Halophila decipiens with mixed species"
   "Halophila decipiens/Halophila ovalis"
   "Halophila ovalis"
   "Halophila ovalis with mixed species"
   "Halophila ovalis/Enhalus acoroides"
   "Halophila ovalis/Halodule uninervis"
   "Halophila ovalis/Halodule uninervis (narrow form)"
   "Halophila ovalis/Halodule uninervis (wide form)"
   "Halophila ovalis/Halodule uninervis (wide form)/Cymodocea serrulata"
   "Halophila ovalis/Halophila decipiens"
   "Halophila ovalis/Halophila spinulosa"
   "Halophila ovalis/Halophila spinulosa/Halodule uninervis"
   "Halophila ovalis/Halophila spinulosa/Zostera muelleri"
   "Halophila ovalis/Zostera muelleri/Halophila spinulosa"
   "Halophila ovalis/Zostera muelleri/Halophila spinulosa/Syringodium isoetifolium"
   "Halophila ovalis/Zostera muelleri/Syringodium isoetifolium"
   "Halophila sp."
   "Halophila spinulosa"
   "Halophila spinulosa/Halodule uninervis"
   "Halophila spinulosa/Halodule uninervis (wide form)"
   "Halophila tricostata"
   "Halophila tricostata/Halophila decipiens"
   "Hard coral and all mixes"
   "Hard rocky reefs"
   "Hard sand"
   "Hard substrate"
   "High density filter feeders (sponges & soft corals)"
   "High density mixed community (corals, algae, sponges & soft corals)"
   "High energy coastal"
   "High profile reef"
   "Hind dune forest"
   "Inshore algae/sponge habitat"
   "Inshore reef"
   "Intermittent estuary"
   "Intertidal flats"
   "Intertidal, mobile substrates"
   "Intertidal, non-mobile substrates"
   "Invertebrate community"
   "Island"
   "Islands and rocks"
   "Kelp"
   "Knoll/Abyssal-hills/Hills/Mountains/Peak"
   "Lagoon sand / algae"
   "Lagoon sand / rubble"
   "Land"
   "Large Rock"
   "Limestone"
   "Live and dead coral"
   "Live coral (+ dead coral)"
   "Live coral and rock, reef slope"
   "Low density mixed community (corals, algae, sponges & soft sorals)"
   "Low intertidal, non-mobile, rough"
   "Low Intertidal, non-mobile, smooth"
   "Low profile reef"
   "Low tidal mudflat"
   "Low-medium density mixed community (corals, algae, sponges & soft sorals)"
   "Lumnitzera racemosa"
   "Macroalgae"
   "Macroalgae (+ rock, live and dead coral)"
   "Macroalgae (intertidal)"
   "Macroalgae (subtidal)"
   "Macroalgae, undifferentiated"
   "Macrolgae (+ rock and sediment)"
   "Mainland"
   "Mangrove/intertidal habitat"
   "Mangroves"
   "Mangroves / Saltmarsh"
   "Marine"
   "Massive coral"
   "Massive/Plate/Branching coral / Sponge"
   "Massive/Soft coral"
   "Massive/Soft/Branching coral / Sponge"
   "Medium density filter feeders (sponges & soft corals)"
   "Medium density mixed community (corals, algae, sponges & soft sorals)"
   "Medium profile reef"
   "Mixed gravel and sand"
   "Mixed kelp and SI"
   "Mixed mangrove species"
   "Mixed other algae and SI"
   "Mixed reef and gravel"
   "Mixed reef and sand"
   "Mixed reef, gravel and sand"
   "Mixed sandy bottom"
   "Mixed seagrass and reef"
   "Mixed seagrass species"
   "Mixed vegetation"
   "Mixed vegetation and SI"
   "Mobile sand"
   "Moderately deep, low slope, unconsolidated"
   "Moderately shallow, high slope, low consolidation"
   "Mound"
   "Mud and calcareous clay"
   "Mud and sand"
   "Mud and tidal flats"
   "Muddy bottom"
   "Mudflat"
   "Nearshore reef"
   "Nearshore waters < 5 metres"
   "No Seagrass"
   "None"
   "None modelled with certainty"
   "Non-mangrove vegetation communities"
   "Non-Reef"
   "Ocean embayment"
   "Offshore deep"
   "Offshore reef"
   "Offshore sandy"
   "Offshore waters < 5 metres (island, shoal)"
   "Offshore waters > 20 metres"
   "Offshore waters 10 - 20 metres"
   "Offshore waters 5 - 10 metres"
   "Other"
   "Other algae"
   "patches"
   "Patchy hard rocky reefs / exposed rock"
   "Patchy Posidonia"
   "Patchy reef"
   "Patchy seagrass"
   "Pavement"
   "Pavement reef"
   "Pelagic"
   "Pelagic clay"
   "Pinnacle"
   "Plain"
   "Plateau"
   "Plunging cliff"
   "Posidonia"
   "Posidonia / Halophila"
   "Posidonia / Halophila / Ruppia"
   "Posidonia / Ruppia"
   "Posidonia / Zostera"
   "Posidonia / Zostera / Halophila"
   "Posidonia australis"
   "Posidonia australis / Halophila"
   "Posidonia australis / Zostera"
   "Reef"
   "Reef and shoal"
   "Reef crest"
   "Reef flat"
   "Reef flat inner"
   "Reef flat outer"
   "Reef slope"
   "Reef, Submerged"
   "Reef, Tidal"
   "Rhizophora"
   "Rhizophora / Aegiceras corniculatum"
   "Rhizophora / Avicennia marina"
   "Rhizophora stylosa (closed forest)"
   "Rhizophora stylosa (open woodland)"
   "Rhizophora stylosa / Bruguiera / Ceriops"
   "Rhizophora stylosa / Camptostemon schultzii"
   "Rhodoliths"
   "Ridge"
   "Riverine"
   "Riverine/estuarine"
   "Roads, cultural features"
   "Rock"
   "Rock (+ live coral), reef crest"
   "Rock wall"
   "Rocky headland"
   "Rocky intertidal"
   "Rocky platform"
   "Rocky reef"
   "Rocky shores"
   "Rubble and sand"
   "Rubble+Sand"
   "Ruppia"
   "Saddle"
   "Saline grassland"
   "Salt flats"
   "Saltmarsh"
   "Samphire-dominated salt flats"
   "Sand"
   "Sand (intertidal)"
   "Sand (subtidal)"
   "Sand and bolders"
   "Sand, silt and gravel with less than 50% mud"
   "Sandshoal"
   "Sandy channels"
   "Sandy intermediate"
   "Sandy lagoon (protected)"
   "Scarp"
   "Seagrass"
   "Seamount/Guyot"
   "Seawall"
   "Sedge, shrub land"
   "Sediment bottom"
   "Sessile Invertebrates (SI)"
   "Shallow island fringe"
   "Shallow lagoon"
   "Shallow, very low slope, unconsolidated"
   "Shelf"
   "Shipwreck"
   "Shoal, Submerged"
   "Shoal, Tidal"
   "Sill"
   "Silt"
   "Silty sand"
   "Slope"
   "Sloping rocky bottom"
   "Sloping sandy bottom"
   "Soft bedrock"
   "Soft coral"
   "Soft substrate"
   "Sonneratia alba"
   "Sonneratia alba / Aegiceras corniculatum"
   "Sparse patchy seagrass"
   "Sparse seagrass"
   "Sponge"
   "Subtidal sand"
   "Subtidal, mobile, below PS zone, rough"
   "Subtidal, mobile, below PS zone, smooth"
   "Subtidal, mobile, PS zone, rough"
   "Subtidal, mobile, PS zone, smooth"
   "Subtidal, non-mobile, below PS zone, rough"
   "Subtidal, non-mobile, below PS zone, smooth"
   "Subtidal, non-mobile, PS zone, rough"
   "Subtidal, non-mobile, PS zone, smooth"
   "Syringodium isoetifolium"
   "Syringodium isoetifolium with mixed species"
   "Terrace"
   "Thalassia hemprichii"
   "Thalassia hemprichii and green algae"
   "Thalassia hemprichii in sandy pool"
   "Thalassia hemprichii with mixed species"
   "Thalassia hemprichii/Enhalus acoroides"
   "Thalassia hemprichii/Halodule uninervis (narrow form)"
   "Thalassia hemprichii/Halodule uninervis (wide form)"
   "Thalassia hemprichii/Halodule uninervis (wide form)/Halophila ovalis"
   "Thalassia hemprichii/Halophila ovalis"
   "Thalassodendron ciliatum"
   "Thalassodendron ciliatum/Thalassia hemprichii"
   "Tidal channel (subtidal)"
   "Tidal sand banks"
   "Tidal-sandwave/Sand-bank"
   "Tide dominated estuary"
   "Transition"
   "Trench/Trough"
   "UNASSIGNED Palustrine or Lacustrine"
   "Unconsolidated bare substrate"
   "Undefined"
   "Unidentified seagrass"
   "Unknown"
   "Unknown or inacessible mangrove species"
   "Unvegetated"
   "Valley"
   "Vegetated area"
   "Vegetated unconsolidated"
   "Very deep, very high slope, unconsolidated"
   "Very shallow, very low slope, highly consolidated"
   "Volcanic sand and grit"
   "Water and terrestrial"
   "Wave dominated estuary"
   "Wrack"
   "Zostera"
   "Zostera / Ruppia"
   "Zostera / Ruppia / Halophila"
   "Zostera capricorni"
   "Zostera muelleri"
   "Zostera muelleri/Cymodocea serrulata"
   "Zostera muelleri/Halodule uninervis"
   "Zostera muelleri/Halodule uninervis/Syringodium isoetifolium"
   "Zostera muelleri/Halophila ovalis"
   "Zostera muelleri/Halophila ovalis/Cymodocea serrulata"
   "Zostera muelleri/Halophila ovalis/Cymodocea serrulata/Syringodium isoetifolium"
   "Zostera muelleri/Halophila ovalis/Halodule uninervis"
   "Zostera muelleri/Halophila spinulosa"
   "Zostera muelleri/Halophila uninervis"
   "Zostera muelleri/Syringodium isoetifolium"])

(def *habitat-colours*
  "Mapping the habitat zones, keyed by strings obtained from the
  SM_HAB_CLS column in the different habitat tables."
  (merge
   ;; This data semi-automatically extracted; see imas-seamap.munging:
   {"Coralline algae"                                                            "#e68a00",
    "High density filter feeders (sponges & soft corals)"                        "#b30086",
    "11-30%"                                                                     "#9ecc3b",
    "Channel - moderate (5 - 10 metres)"                                         "#00e6b8",
    "Benthic micro-algae"                                                        "#a82424",
    "Sloping rocky bottom"                                                       nil,
    "Offshore reef"                                                              "#A16632",
    "Fine sediment"                                                              "#f9f075",
    "Medium profile reef"                                                        "#A16632",
    "Halophila decipiens"                                                        "#b3daff",
    "Pinnacle"                                                                   "#b52829",
    "Undefined"                                                                  nil,
    "Massive/Soft/Branching coral / Sponge"                                      "#7b50fb",
    "Invertebrate community"                                                     "#E64C00",
    "Plunging cliff"                                                             nil,
    "Macroalgae"                                                                 "#2d9624",
    "Halophila sp."                                                              "#47291f",
    "Islands and rocks"                                                          "#018200",
    "Boulder+Rubble"                                                             "#ff9900",
    "Sparse patchy seagrass"                                                     nil,
    "Caulerpa"                                                                   "#00E6A9",
    "Blue hole"                                                                  "#000066",
    "Sand, silt and gravel with less than 50% mud"                               "#E8C7AE",
    "Algal mat"                                                                  "#8fb300",
    "Shallow, very low slope, unconsolidated"                                    "#ffce99",
    "Seagrass"                                                                   "#02DC00",
    "Vegetated area"                                                             "#016300",
    "Sand and bolders"                                                           "#999966",
    "Shallow island fringe"                                                      "#7ab8b8",
    "Mixed reef and gravel"                                                      "#cc0000",
    "Sand"                                                                       "#FFF9A5",
    "Filter feeders"                                                             "#bc8fbc",
    "Patchy reef"                                                                nil,
    "Wave dominated estuary"                                                     "#000080",
    "Estuarine - salt flats and saltmarshes"                                     "#ff1a53",
    "Roads, cultural features"                                                   "#4a1c4a",
    "Halodule uninervis"                                                         "#ffccff",
    "Thalassia hemprichii with mixed species"                                    "#133926",
    "Mobile sand"                                                                "#eade9c",
    "> 1000 g/m2"                                                                "#002c00",
    "Mixed seagrass species"                                                     "#009900",
    "Riverine/estuarine"                                                         "#00E6A9",
    "Basin"                                                                      "#29bd44",
    "Rocky reef"                                                                 "#640000",
    "Rhizophora"                                                                 "#3fd411",
    "Offshore deep"                                                              "#3d3d8f",
    "Tide dominated estuary"                                                     "#009999",
    "Transition"                                                                 "#0000cc",
    "Patchy seagrass"                                                            nil,
    "Enhalus acoroides"                                                          "#4dc3ff",
    "Coral"                                                                      "#dd3c8c",
    "Mud and sand"                                                               "#FFAD00",
    "Tidal sand banks"                                                           "#FFF9A5",
    "High energy coastal"                                                        "#E64C00",
    "Low profile reef"                                                           "#A1967A",
    "Exposed Rock"                                                               "#008080",
    "Tidal channel (subtidal)"                                                   "#1a8cff",
    "Subtidal, mobile, below PS zone, rough"                                     "#52217a",
    "Estuarine - water"                                                          "#5599ff",
    "Calcareous gravel, sand and silt"                                           "#974749",
    "Saline grassland"                                                           "#7ab8b8",
    "Central basin"                                                              "#000080",
    "Posidonia australis"                                                        "#ecee5d",
    "Subtidal, non-mobile, below PS zone, rough"                                 "#2c09aa",
    "Salt flats"                                                                 "#b12d38",
    "Ceriops tagal"                                                              "#f28c8c",
    "High profile reef"                                                          "#974749",
    "Branching live coral, reef slope"                                           "#ffbb33",
    "Bare rocky reef"                                                            "#640000",
    "Fluvial (bay-head) delta"                                                   "#1a8cff",
    "75-100 g/m2"                                                                "#00cc00",
    "Thalassia hemprichii in sandy pool"                                         "#00cccc",
    "Cymodocea serrulata with mixed species"                                     "#331a00",
    "Rocky intertidal"                                                           "#999966",
    "50-75 g/m2"                                                                 "#4dff4d",
    "Very shallow, very low slope, highly consolidated"                          "#ffffcc",
    "Low density mixed community (corals, algae, sponges & soft sorals)"         "#99ffeb",
    "Low-medium density mixed community (corals, algae, sponges & soft sorals)"  "#00e6b8",
    "0 - 20m"                                                                    "#ffffcc",
    "Deep reef structures"                                                       "#24478f",
    "Soft coral"                                                                 "#ff80ff",
    "Rhodoliths"                                                                 "#d7191c",
    "Mangroves"                                                                  "#26734b",
    "Subtidal, non-mobile, PS zone, smooth"                                      "#7ecf63",
    "High density mixed community (corals, algae, sponges & soft corals)"        "#004d4d",
    "Nearshore reef"                                                             "#640000",
    "Biosiliceous marl and calcareous clay"                                      "#3C6442",
    "Ocean embayment"                                                            "#5151fb",
    "< 25 g/m2"                                                                  "#daffb3",
    "Deep reef flat"                                                             "#006699",
    "Offshore sandy"                                                             "#cc99ff",
    "Sandy intermediate"                                                         "#acac53",
    "Mixed gravel and sand"                                                      "#d2a679",
    "< 0m"                                                                       "#666666",
    "Bare substrate"                                                             "#8c8c8c",
    "Very deep, very high slope, unconsolidated"                                 "#000080",
    "Escarpment"                                                                 "#a13f6d",
    "Continental-rise"                                                           "#c471da",
    "Lagoon sand / algae"                                                        "#00b386",
    "70-100%"                                                                    "#006600",
    "None modelled with certainty"                                               "#c0c1c8",
    "Subtidal, mobile, PS zone, rough"                                           "#aa27c1",
    "Coral communities"                                                          nil,
    "Dredged"                                                                    "#A5A5A5",
    "Soft bedrock"                                                               nil,
    "Rock wall"                                                                  nil,
    "Unconsolidated bare substrate"                                              "#A5A5A5",
    "Reef flat inner"                                                            "#fcf769",
    "Cymodocea serrulata"                                                        "#ff904d",
    "Shoal, Tidal"                                                               "#ace600",
    "Depression"                                                                 "#000000",
    "Slope"                                                                      "#bd4b28",
    "Reef, Submerged"                                                            "#813918",
    "Enhalus acoroides with mixed species"                                       "#1a3365",
    "Subtidal, non-mobile, PS zone, rough"                                       "#33892a",
    "Water and terrestrial"                                                      "#000080",
    "Unknown"                                                                    nil,
    "Shipwreck"                                                                  "#000000",
    "Rubble and sand"                                                            "#E8C7AE",
    "Sloping sandy bottom"                                                       nil,
    "Mud and calcareous clay"                                                    "#14A4AC",
    "Sill"                                                                       "#5a41d8",
    "Terrace"                                                                    "#FFAD00",
    "Mixed vegetation"                                                           "#009999",
    "Seawall"                                                                    "#000000",
    "Reef flat"                                                                  "#ff8000",
    "1-10%"                                                                      "#b3ffb3",
    "Wrack"                                                                      "#ff8000",
    "Silty sand"                                                                 "#E8C7AE",
    "60 - 200m"                                                                  "#ff9c33",
    "Sandy channels"                                                             "#FFF9A5",
    "Plateau"                                                                    "#aed392",
    "Unvegetated"                                                                "#e9beaf",
    "Bare sandy to mixed sediments"                                              "#FFF9A5",
    "Pelagic clay"                                                               "#A5A5A5",
    "Zostera capricorni"                                                         "#b35900",
    "Moderately shallow, high slope, low consolidation"                          "#ff9c33",
    "Thalassia hemprichii and green algae"                                       "#003300",
    "Brackish lake"                                                              "#e07552",
    "Sediment bottom"                                                            nil,
    "Channel - deep (10 - 20 metres)"                                            "#004d4d",
    "Saltmarsh"                                                                  "#E64C00",
    "Backreef / shallow lagoon"                                                  "#33adff",
    "Subtidal sand"                                                              "#E8C7AE",
    "Fringing coral reefs"                                                       nil,
    "Limestone"                                                                  "#E8C7AE",
    "Cymodocea rotundata"                                                        "#ecec13",
    "Avicennia marina"                                                           "#ffb980",
    "Mixed mangrove species"                                                     "#43705a",
    "Rocky platform"                                                             nil,
    "Intertidal, non-mobile substrates"                                          "#c5d300",
    "Bare coarse sediment with possibility of small rocky outcrops/rippled sand" "#E8C7AE",
    "Medium density mixed community (corals, algae, sponges & soft sorals)"      "#00995c",
    "Hard substrate"                                                             "#802000",
    "Massive/Soft coral"                                                         "#ffb84d",
    "Coastline - sand"                                                           "#ffffcc",
    "Halophila ovalis with mixed species"                                        "#47291f",
    "Canyon"                                                                     "#2634aa",
    "Diverse sandy"                                                              "#FFAD00",
    "Massive coral"                                                              "#b30000",
    "Mixed seagrass and reef"                                                    "#009999",
    "Hard coral and all mixes"                                                   "#b33c00",
    "Rock"                                                                       "#bf8040",
    "Reef crest"                                                                 "#ff9999",
    "Thalassodendron ciliatum"                                                   "#125454",
    "Valley"                                                                     "#000000",
    "Bare coarse sediment with rippled sand"                                     "#FFAD00",
    "Soft substrate"                                                             "#f4d371",
    "Reef and shoal"                                                             "#640000",
    "Macroalgae, undifferentiated"                                               "#2d9624",
    "31-50%"                                                                     "#38a748",
    "Hard rocky reefs"                                                           nil,
    "Sparse seagrass"                                                            "#D1FF73",
    "Mound"                                                                      "#b52829",
    "Zostera"                                                                    "#8ef28c",
    "Abyssal-plain / Deep ocean floor"                                           "#b2b434",
    "Intermittent estuary"                                                       "#e68a00",
    "Island"                                                                     "#018200",
    "Rocky shores"                                                               "#661a00",
    "Mangroves / Saltmarsh"                                                      "#458aa1",
    "Freshwater"                                                                 "#66d9ff",
    "Shallow lagoon"                                                             "#dbadeb",
    "Unidentified seagrass"                                                      "#999999",
    "Samphire-dominated salt flats"                                              "#cc0066",
    "Posidonia"                                                                  "#ecee5d",
    "Ruppia"                                                                     "#D3FFBE",
    "Intertidal, mobile substrates"                                              "#ecffb3",
    "100-1000 g/m2"                                                              "#007100",
    "Reef slope"                                                                 "#bd4b28",
    "Scarp"                                                                      "#a13f6d",
    "Bommies"                                                                    "#4d3319",
    "Bioturbated mud"                                                            "#974749",
    "Halophila spinulosa"                                                        "#b299ff",
    "Halophila"                                                                  "#d9cda6",
    "Subtidal, non-mobile, below PS zone, smooth"                                "#3385ff",
    "20 - 60m"                                                                   "#ffce99",
    "Barrier/back-barrier"                                                       "#ff9900",
    "No Seagrass"                                                                "#e6e6e6",
    "Lagoon sand / rubble"                                                       "#669999",
    "Subtidal, mobile, below PS zone, smooth"                                    "#b68cd9",
    "Rock (+ live coral), reef crest"                                            "#cc9900",
    "Sponge"                                                                     "#FCFAE2",
    "Patchy Posidonia"                                                           "#a5d742",
    "Shoal, Submerged"                                                           "#4d6600",
    "Ridge"                                                                      "#a97070",
    "Mixed other algae and SI"                                                   "#cc0066",
    "Mixed vegetation and SI"                                                    "#ff8533",
    "Non-Reef"                                                                   "#b3b3ff",
    "Deep areas"                                                                 "#16389c",
    "Mangrove/intertidal habitat"                                                "#227722",
    "Bank"                                                                       "#44a1b5",
    "Hard sand"                                                                  "#FFAD00",
    "Zostera muelleri"                                                           "#cc2900",
    "Bioturbated sand"                                                           "#A1967A",
    "Riverine"                                                                   "#00ffff",
    "Silt"                                                                       "#E5D6ED",
    "Other algae"                                                                "#84e1e1",
    "Aquatic macrophytes"                                                        "#32B34E",
    "Subtidal, mobile, PS zone, smooth"                                          "#e16bc3",
    "Halophila decipiens with mixed species"                                     "#476b6b",
    "Offshore waters 10 - 20 metres"                                             "#4d4dff",
    "Halophila ovalis"                                                           "#bfff80",
    "Bruguiera exaristata"                                                       "#b2fc03",
    "Gravel"                                                                     "#82837E",
    "Lumnitzera racemosa"                                                        "#7ab8b8",
    "Shelf"                                                                      "#c89e4a",
    "Mixed reef and sand"                                                        "#ff884d",
    "25-50 g/m2"                                                                 "#b5ff66",
    "Vegetated unconsolidated"                                                   "#018200",
    "Channel"                                                                    "#006666",
    "Low intertidal, non-mobile, rough"                                          "#f6620a",
    "Deep, low slope, unconsolidated"                                            "#4d4dff",
    "Cobble"                                                                     "#00a9e6",
    "Pelagic"                                                                    "#0066cc",
    "Mixed kelp and SI"                                                          "#77b300",
    "Eroded sandstone"                                                           "#a6a6a6",
    "Syringodium isoetifolium with mixed species"                                "#3385ff",
    "Sand (subtidal)"                                                            "#A68C73",
    "Rocky headland"                                                             "#000000",
    "Deep lagoon"                                                                "#6600cc",
    "Sonneratia alba"                                                            "#dfd19f",
    "Flood- and ebb-tide delta"                                                  "#7ab8b8",
    "Syringodium isoetifolium"                                                   "#3385ff",
    "Beach"                                                                      "#FFF9A5",
    "Pavement"                                                                   "#cccc00",
    "Mudflat"                                                                    "#734e26",
    "Unknown or inacessible mangrove species"                                    "#A5A5A5",
    "Calcareous ooze"                                                            "#BBA600",
    "Artificial substrate"                                                       nil,
    "Massive/Plate/Branching coral / Sponge"                                     "#00b3b3",
    "10-40 %"                                                                    "#33ff33",
    "Mud and tidal flats"                                                        "#FFAD00",
    "Thalassia hemprichii"                                                       "#00e6ac",
    "Offshore waters 5 - 10 metres"                                              "#b3b3ff",
    "Bedrock"                                                                    "#4d3319",
    "Rubble+Sand"                                                                "#E8C7AE",
    "40-70 %"                                                                    "#00cc00",
    "Large Rock"                                                                 "#4d3319",
    "Marine"                                                                     "#000099",
    "Moderately deep, low slope, unconsolidated"                                 "#b3b3ff",
    "Reef flat outer"                                                            "#ff8000",
    "Other"                                                                      "#000000",
    "Halophila tricostata"                                                       "#d9cda6",
    "1-10 %"                                                                     "#b3ffb3",
    "Algal beds"                                                                 "#018200",
    "51-75%"                                                                     "#2a753a",
    "Pavement reef"                                                              "#640000",
    "Mixed sandy bottom"                                                         nil,
    "Live and dead coral"                                                        "#601f60",
    "Reef, Tidal"                                                                "#e6994c",
    "Kelp"                                                                       "#006600",
    "Halophila capricorni"                                                       "#993366",
    "Aegiceras corniculatum"                                                     "#ffde4d",
    "> 200m"                                                                     "#666666",
    "Cay"                                                                        "#FFAD00",
    "Channel - shallow (< 5 metres)"                                             "#99ffeb",
    "Saddle"                                                                     "#e6e600",
    "Sand (intertidal)"                                                          "#E8C7AE",
    "Branching coral"                                                            "#00ffaa",
    "patches"                                                                    "#bfbfbf",
    "Inshore reef"                                                               "#a16632",
    "76-100%"                                                                    "#374705",
    "Plain"                                                                      "#FFF9A5",
    "Mixed reef, gravel and sand"                                                "#cccc00",
    "Non-mangrove vegetation communities"                                        "#c2cc00",
    "Muddy bottom"                                                               nil,
    "Bruguiera parviflora"                                                       "#ecfd68",
    "Medium density filter feeders (sponges & soft corals)"                      "#ff99e6",
    "Volcanic sand and grit"                                                     "#000000",
    "Reef"                                                                       "#640000",
    "Low tidal mudflat"                                                          "#734e26"}
   ;; Manual overrides for Tasmanian layer (the style for which uses background images)
   {"Cobble"                 "#00a9e6"
    "Patchy reef"            "#640000"
    "Patchy seagrass"        "#02dc00"
    "Sparse patchy seagrass" "#d1ff73"
    "Unknown"                "#999999"}))

(def *habitat-titles*
  "Maps habitat classifications to their titles, as they appear in the
  legend (often, but not always, the same as the SM_HAB_CLS value)."
  {"Coralline algae"                                                       "Coralline algae",
   "High density filter feeders (sponges & soft corals)"                   "Filter feeders (sponges & soft corals) - high density",
   "11-30%"                                                                "11-30%",
   "Channel - moderate (5 - 10 metres)"                                    "Channel - moderate (5 - 10 metres)",
   "Benthic micro-algae"                                                   "Benthic microalgae",
   "Sloping rocky bottom"                                                  "Sloping rocky bottom",
   "Offshore reef"                                                         "Offshore reef",
   "Fine sediment"                                                         "Fine sediment",
   "Medium profile reef"                                                   "Medium profile reef",
   "Halophila decipiens"                                                   "Halophila decipiens",
   "Pinnacle"                                                              "Pinnacle",
   "Undefined"                                                             "Undefined",
   "Massive/Soft/Branching coral / Sponge"                                 "Massive/Soft/Branching coral + Sponge",
   "Invertebrate community"                                                "Invertebrate community",
   "Plunging cliff"                                                        "Plunging cliff",
   "Macroalgae"                                                            "Macroalgae",
   "Halophila sp."                                                         "Halophila sp.",
   "Islands and rocks"                                                     "Islands and rocks",
   "Boulder+Rubble"                                                        "Boulder and rubble",
   "Sparse patchy seagrass"                                                "Sparse patchy seagrass",
   "Caulerpa"                                                              "Caulerpa",
   "Blue hole"                                                             "Blue hole",
   "Sand, silt and gravel with less than 50% mud"                          "Sand, silt and gravel with less than 50% mud",
   "Algal mat"                                                             "Algal mat",
   "Shallow, very low slope, unconsolidated"                               "Shallow, very low slope, unconsolidated",
   "Seagrass"                                                              "Seagrass",
   "Vegetated area"                                                        "Vegetated area",
   "Sand and bolders"                                                      "Sand and boulders",
   "Shallow island fringe"                                                 "Shallow island fringe",
   "Mixed reef and gravel"                                                 "Mixed reef and gravel",
   "Sand"                                                                  "Sand",
   "Filter feeders"                                                        "Filter feeders",
   "Patchy reef"                                                           "Patchy reef",
   "Wave dominated estuary"                                                "Wave dominated estuary",
   "Estuarine - salt flats and saltmarshes"                                "Estuarine - salt flats and saltmarshes",
   "Roads, cultural features"                                              "Roads, cultural features",
   "Halodule uninervis"                                                    "Halodule uninervis",
   "Thalassia hemprichii with mixed species"                               "Thalassia hemprichii with mixed species",
   "Mobile sand"                                                           "Mobile sand",
   "> 1000 g/m2"                                                           "> 1000",
   "Mixed seagrass species"                                                "Mixed seagrass species",
   "Riverine/estuarine"                                                    "Riverine/estuarine",
   "Basin"                                                                 "Basin",
   "Rocky reef"                                                            "Rocky reef",
   "Rhizophora"                                                            "Rhizophora",
   "Offshore deep"                                                         "Offshore deep",
   "Tide dominated estuary"                                                "Tide dominated estuary",
   "Transition"                                                            "Transition",
   "Patchy seagrass"                                                       "Patchy seagrass",
   "Enhalus acoroides"                                                     "Enhalus acoroides",
   "Coral"                                                                 "Coral",
   "Mud and sand"                                                          "Mud and sand",
   "Tidal sand banks"                                                      "Tidal sand banks",
   "High energy coastal"                                                   "High energy coastal",
   "Low profile reef"                                                      "Low profile reef",
   "Exposed Rock"                                                          "Exposed rock",
   "Tidal channel (subtidal)"                                              "Tidal channel",
   "Subtidal, mobile, below PS zone, rough"                                "Subtidal - mobile substrate (rough), below PS zone",
   "Estuarine - water"                                                     "Estuarine - water",
   "Calcareous gravel, sand and silt"                                      "Calcareous gravel, sand and silt",
   "Saline grassland"                                                      "Saline grassland",
   "Central basin"                                                         "Central basin",
   "Posidonia australis"                                                   "Posidonia australis",
   "Subtidal, non-mobile, below PS zone, rough"                            "Subtidal - non-mobile substrate (rough), below PS zone",
   "Salt flats"                                                            "Salt flats",
   "Ceriops tagal"                                                         "Ceriops tagal",
   "High profile reef"                                                     "High profile reef",
   "Branching live coral, reef slope"                                      "Live branching coral on reef slope",
   "Bare rocky reef"                                                       "Bare rocky reef",
   "Fluvial (bay-head) delta"                                              "Fluvial (bay-head) delta",
   "75-100 g/m2"                                                           "75-100",
   "Thalassia hemprichii in sandy pool"                                    "Thalassia hemprichii in sandy pool",
   "Cymodocea serrulata with mixed species"                                "Cymodocea serrulata with mixed species",
   "Rocky intertidal"                                                      "Rocky intertidal",
   "50-75 g/m2"                                                            "50-75",
   "Very shallow, very low slope, highly consolidated"                     "Very shallow, very low slope, highly consolidated",
   "Low density mixed community (corals, algae, sponges & soft sorals)"    "Mixed community (corals, algae, sponges & soft sorals) - low density",
   "Low-medium density mixed community (corals, algae, sponges & soft sorals)"
   "Mixed community (corals, algae, sponges & soft sorals) - low-medium density",
   "0 - 20m"                                                               "0 - 20m",
   "Deep reef structures"                                                  "Deep reef structures",
   "Soft coral"                                                            "Soft coral",
   "Rhodoliths"                                                            "Rhodoliths",
   "Mangroves"                                                             "Mangroves",
   "Subtidal, non-mobile, PS zone, smooth"                                 "Subtidal - non-mobile substrate (smooth), PS zone",
   "High density mixed community (corals, algae, sponges & soft corals)"   "Mixed community (corals, algae, sponges & soft sorals) - high density",
   "Nearshore reef"                                                        "Nearshore reef",
   "Biosiliceous marl and calcareous clay"                                 "Biosiliceous marl and calcareous clay",
   "Ocean embayment"                                                       "Ocean embayment",
   "< 25 g/m2"                                                             "< 25",
   "Deep reef flat"                                                        "Deep reef flat",
   "Offshore sandy"                                                        "Offshore sandy",
   "Sandy intermediate"                                                    "Sandy intermediate",
   "Mixed gravel and sand"                                                 "Mixed gravel and sand",
   "< 0m"                                                                  "< 0m (land)",
   "Bare substrate"                                                        "Bare substrate",
   "Very deep, very high slope, unconsolidated"                            "Very deep, very high slope, unconsolidated",
   "Escarpment"                                                            "Escarpment",
   "Continental-rise"                                                      "Continental-rise",
   "Lagoon sand / algae"                                                   "Lagoon sand / algae",
   "70-100%"                                                               "70-100%",
   "None modelled with certainty"                                          "None modelled with certainty",
   "Subtidal, mobile, PS zone, rough"                                      "Subtidal - mobile substrate (rough), PS zone",
   "Coral communities"                                                     "Coral communities",
   "Dredged"                                                               "Dredged",
   "Soft bedrock"                                                          "Soft bedrock",
   "Rock wall"                                                             "Rock wall",
   "Unconsolidated bare substrate"                                         "Unconsolidated bare substrate",
   "Reef flat inner"                                                       "Reef flat (inner)",
   "Cymodocea serrulata"                                                   "Cymodocea serrulata",
   "Shoal, Tidal"                                                          "Shoal, tidal",
   "Depression"                                                            "Depression",
   "Slope"                                                                 "Slope",
   "Reef, Submerged"                                                       "Reef, submerged",
   "Enhalus acoroides with mixed species"                                  "Enhalus acoroides with mixed species",
   "Subtidal, non-mobile, PS zone, rough"                                  "Subtidal - non-mobile substrate (rough), PS zone",
   "Water and terrestrial"                                                 "Water and terrestrial",
   "Unknown"                                                               "Unknown",
   "Shipwreck"                                                             "Shipwreck",
   "Rubble and sand"                                                       "Rubble and sand",
   "Sloping sandy bottom"                                                  "Sloping sandy bottom",
   "Mud and calcareous clay"                                               "Mud and calcareous clay",
   "Sill"                                                                  "Sill",
   "Terrace"                                                               "Terrace",
   "Mixed vegetation"                                                      "Mixed vegetation",
   "Seawall"                                                               "Seawall",
   "Reef flat"                                                             "Reef flat",
   "1-10%"                                                                 "1-10%",
   "Wrack"                                                                 "Wrack",
   "Silty sand"                                                            "Silty sand",
   "60 - 200m"                                                             "60 - 200m",
   "Sandy channels"                                                        "Sandy channels",
   "Plateau"                                                               "Plateau",
   "Unvegetated"                                                           "Unvegetated",
   "Bare sandy to mixed sediments"                                         "Bare sandy to mixed sediments",
   "Pelagic clay"                                                          "Pelagic clay",
   "Zostera capricorni"                                                    "Zostera capricorni",
   "Moderately shallow, high slope, low consolidation"                     "Moderately shallow, high slope, low consolidation",
   "Thalassia hemprichii and green algae"                                  "Thalassia hemprichii and green algae",
   "Brackish lake"                                                         "Brackish lake",
   "Sediment bottom"                                                       "Sediment bottom",
   "Channel - deep (10 - 20 metres)"                                       "Channel - deep (10 - 20 metres)",
   "Saltmarsh"                                                             "Saltmarsh",
   "Backreef / shallow lagoon"                                             "Backreef / shallow lagoon",
   "Subtidal sand"                                                         "Subtidal sand",
   "Fringing coral reefs"                                                  "Fringing coral reefs",
   "Limestone"                                                             "Limestone",
   "Cymodocea rotundata"                                                   "Cymodocea rotundata",
   "Avicennia marina"                                                      "Avicennia marina",
   "Mixed mangrove species"                                                "Mixed mangrove species",
   "Rocky platform"                                                        "Rocky platform",
   "Intertidal, non-mobile substrates"                                     "Intertidal - non-mobile substrates",
   "Bare coarse sediment with possibility of small rocky outcrops/rippled sand"
   "Bare coarse sediment with possibility of small rocky outcrops/rippled sand",
   "Medium density mixed community (corals, algae, sponges & soft sorals)" "Mixed community (corals, algae, sponges & soft sorals) - medium density",
   "Hard substrate"                                                        "Hard substrate",
   "Massive/Soft coral"                                                    "Massive/Soft coral",
   "Coastline - sand"                                                      "Coastline - sand",
   "Halophila ovalis with mixed species"                                   "Halophila ovalis with mixed species",
   "Canyon"                                                                "Canyon",
   "Diverse sandy"                                                         "Diverse sandy",
   "Massive coral"                                                         "Massive coral",
   "Mixed seagrass and reef"                                               "Mixed seagrass and reef",
   "Hard coral and all mixes"                                              "Hard coral and all mixes",
   "Rock"                                                                  "Rock",
   "Reef crest"                                                            "Reef crest",
   "Thalassodendron ciliatum"                                              "Thalassodendron ciliatum",
   "Valley"                                                                "Valley",
   "Bare coarse sediment with rippled sand"                                "Bare coarse sediment with rippled sand",
   "Soft substrate"                                                        "Soft substrate",
   "Reef and shoal"                                                        "Reef and shoal",
   "Macroalgae, undifferentiated"                                          "Macroalgae, undifferentiated",
   "31-50%"                                                                "31-50%",
   "Hard rocky reefs"                                                      "Hard rocky reefs",
   "Sparse seagrass"                                                       "Sparse seagrass",
   "Mound"                                                                 "Mound",
   "Zostera"                                                               "Zostera",
   "Abyssal-plain / Deep ocean floor"                                      "Abyssal-plain / Deep ocean floor",
   "Intermittent estuary"                                                  "Intermittent estuary",
   "Island"                                                                "Island",
   "Rocky shores"                                                          "Rocky shores",
   "Mangroves / Saltmarsh"                                                 "Mangroves / Saltmarsh",
   "Freshwater"                                                            "Freshwater",
   "Shallow lagoon"                                                        "Lagoon (shallow)",
   "Unidentified seagrass"                                                 "Unidentified seagrass",
   "Samphire-dominated salt flats"                                         "Samphire-dominated salt flats",
   "Posidonia"                                                             "Posidonia",
   "Ruppia"                                                                "Ruppia",
   "Intertidal, mobile substrates"                                         "Intertidal - mobile substrates",
   "100-1000 g/m2"                                                         "100-1000",
   "Reef slope"                                                            "Reef slope",
   "Scarp"                                                                 "Scarp",
   "Bommies"                                                               "Bommies",
   "Bioturbated mud"                                                       "Bioturbated mud",
   "Halophila spinulosa"                                                   "Halophila spinulosa",
   "Halophila"                                                             "Halophila",
   "Subtidal, non-mobile, below PS zone, smooth"                           "Subtidal - non-mobile substrate (smooth), below PS zone",
   "20 - 60m"                                                              "20 - 60m",
   "Barrier/back-barrier"                                                  "Barrier/back-barrier",
   "No Seagrass"                                                           "No seagrass",
   "Lagoon sand / rubble"                                                  "Lagoon sand / rubble",
   "Subtidal, mobile, below PS zone, smooth"                               "Subtidal - mobile substrate (smooth), below PS zone",
   "Rock (+ live coral), reef crest"                                       "Rock (+ live coral) on reef crest",
   "Sponge"                                                                "Sponge",
   "Patchy Posidonia"                                                      "Patchy Posidonia",
   "Shoal, Submerged"                                                      "Shoal, submerged",
   "Ridge"                                                                 "Ridge",
   "Mixed other algae and SI"                                              "Mixed other algae and SI",
   "Mixed vegetation and SI"                                               "Mixed vegetation and SI",
   "Non-Reef"                                                              "Non-Reef",
   "Deep areas"                                                            "Other deep areas",
   "Mangrove/intertidal habitat"                                           "Mangrove/intertidal habitat",
   "Bank"                                                                  "Bank",
   "Hard sand"                                                             "Hard sand",
   "Zostera muelleri"                                                      "Zostera muelleri",
   "Bioturbated sand"                                                      "Bioturbated sand",
   "Riverine"                                                              "Riverine",
   "Silt"                                                                  "Silt",
   "Other algae"                                                           "Other algae",
   "Aquatic macrophytes"                                                   "Aquatic macrophytes",
   "Subtidal, mobile, PS zone, smooth"                                     "Subtidal - mobile substrate (smooth), PS zone",
   "Halophila decipiens with mixed species"                                "Halophila decipiens with mixed species",
   "Offshore waters 10 - 20 metres"                                        "Offshore waters 10 - 20 metres",
   "Halophila ovalis"                                                      "Halophila ovalis",
   "Bruguiera exaristata"                                                  "Bruguiera exaristata",
   "Gravel"                                                                "Gravel",
   "Lumnitzera racemosa"                                                   "Lumnitzera racemosa",
   "Shelf"                                                                 "Shelf",
   "Mixed reef and sand"                                                   "Mixed reef and sand",
   "25-50 g/m2"                                                            "25-50",
   "Vegetated unconsolidated"                                              "Vegetated unconsolidated",
   "Channel"                                                               "Channel",
   "Low intertidal, non-mobile, rough"                                     "Low intertidal - non-mobile substrate (rough)",
   "Deep, low slope, unconsolidated"                                       "Deep, low slope, unconsolidated",
   "Cobble"                                                                "Cobble",
   "Pelagic"                                                               "Pelagic",
   "Mixed kelp and SI"                                                     "Mixed kelp and SI",
   "Eroded sandstone"                                                      "Eroded sandstone",
   "Syringodium isoetifolium with mixed species"                           "Syringodium isoetifolium with mixed species",
   "Sand (subtidal)"                                                       "Sand (subtidal)",
   "Rocky headland"                                                        "Rocky headland",
   "Deep lagoon"                                                           "Deep lagoon",
   "Sonneratia alba"                                                       "Sonneratia alba",
   "Flood- and ebb-tide delta"                                             "Flood- and Ebb-tide Delta",
   "Syringodium isoetifolium"                                              "Syringodium isoetifolium",
   "Beach"                                                                 "Beach",
   "Pavement"                                                              "Flat/hard low-relief bottom (pavement)",
   "Mudflat"                                                               "Mudflat",
   "Unknown or inacessible mangrove species"                               "Unknown / inacessible mangrove species",
   "Calcareous ooze"                                                       "Calcareous ooze",
   "Artificial substrate"                                                  "Artificial substrate",
   "Massive/Plate/Branching coral / Sponge"                                "Massive/Plate/Branching coral + Sponge",
   "10-40 %"                                                               "10-40%",
   "Mud and tidal flats"                                                   "Mud and tidal flats",
   "Thalassia hemprichii"                                                  "Thalassia hemprichii",
   "Offshore waters 5 - 10 metres"                                         "Offshore waters 5 - 10 metres",
   "Bedrock"                                                               "Bedrock",
   "Rubble+Sand"                                                           "Rubble and sand",
   "40-70 %"                                                               "40-70%",
   "Large Rock"                                                            "Large rock",
   "Marine"                                                                "Marine",
   "Moderately deep, low slope, unconsolidated"                            "Moderately deep, low slope, unconsolidated",
   "Reef flat outer"                                                       "Reef flat (outer)",
   "Other"                                                                 "Other",
   "Halophila tricostata"                                                  "Halophila tricostata",
   "1-10 %"                                                                "1-10%",
   "Algal beds"                                                            "Algal beds",
   "51-75%"                                                                "51-75%",
   "Pavement reef"                                                         "Pavement reef",
   "Mixed sandy bottom"                                                    "Mixed sandy bottom",
   "Live and dead coral"                                                   "Live/dead coral combined",
   "Reef, Tidal"                                                           "Reef, tidal",
   "Kelp"                                                                  "Kelp",
   "Halophila capricorni"                                                  "Halophila capricorni",
   "Aegiceras corniculatum"                                                "Aegiceras corniculatum",
   "> 200m"                                                                "< 0m (land)",
   "Cay"                                                                   "Cay",
   "Channel - shallow (< 5 metres)"                                        "Channel - shallow (< 5 metres)",
   "Saddle"                                                                "Saddle",
   "Sand (intertidal)"                                                     "Sand (intertidal)",
   "Branching coral"                                                       "Branching coral",
   "patches"                                                               "Patches",
   "Inshore reef"                                                          "Inshore reef",
   "76-100%"                                                               "76-100%",
   "Plain"                                                                 "Plain",
   "Mixed reef, gravel and sand"                                           "Mixed reef, gravel and sand",
   "Non-mangrove vegetation communities"                                   "Non-mangrove vegetation communities",
   "Muddy bottom"                                                          "Muddy bottom",
   "Bruguiera parviflora"                                                  "Bruguiera parviflora",
   "Medium density filter feeders (sponges & soft corals)"                 "Filter feeders (sponges & soft corals) - medium density",
   "Volcanic sand and grit"                                                "Volcanic sand and grit",
   "Reef"                                                                  "Reef",
   "Low tidal mudflat"                                                     "Low tidal mudflat"})
