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
  {"Offshore Waters (10  - 20 metres)"                                                                           "#4d4dff",
   "Coralline algae"                                                                                             "#e68a00",
   "T.hemprichii/H.uninervis(wide) and green algae/Halophila ovalis (coarse sand/rubble/shell)"                  "#6f6f2a",
   "Light H. uninervis with H. ovalis"                                                                           "#dfff80",
   "11-30%"                                                                                                      "#9ecc3b",
   "Halodule uninervis (narrow form) with mixed species"                                                         "#70c299",
   "Benthic micro-algae"                                                                                         "#a82424",
   "Closed Bruguiera"                                                                                            "#009999",
   "Offshore reef"                                                                                               "#A16632",
   "Fine sediment"                                                                                               "#A68C73",
   "Sonneratia alba/Rhizophora stylosa/Avicennia marina"                                                         "#734e26",
   "Halophila decipiens"                                                                                         "#dd99ff",
   "Moderate H. uninervis (thin) with H. ovalis"                                                                 "#39ac39",
   "Sonneratia alba/Aegiceras corniculatum"                                                                      "#b69449",
   "Open Ceriops"                                                                                                "#f09475",
   "Halophila spinulosa and Halophila uninervis"                                                                 "#9f80ff",
   "Light C. rotundata"                                                                                          "#fff566",
   "Plunging cliff"                                                                                              nil,
   "Macroalgae"                                                                                                  "#14A4AC",
   "H. uninervis & H. ovalis"                                                                                    "#00cc7a",
   "valley"                                                                                                      "#000000",
   "subtidal H.ovalis/H.uninervis(wide) - dense in patches (sand)"                                               "#996600",
   "Halophila sp."                                                                                               "#47291f",
   "Thalassia hemprichii with Halophila ovalis"                                                                  "#009900",
   "Islands and rocks"                                                                                           "#018200",
   "Caulerpa & H. ovalis"                                                                                        "#adf168",
   "Boulder+Rubble"                                                                                              "#ff9900",
   "Invertebrate Community"                                                                                      "#E64C00",
   "Halophila ovalis with Halodule uninervis (narrow form)"                                                      "#e864b1",
   "Halodule uninervis (narrow form)/Halophila ovalis"                                                           "#70c299",
   "Caulerpa"                                                                                                    "#00E6A9",
   "Open Bruguiera"                                                                                              "#009999",
   "Blue hole"                                                                                                   "#000066",
   "Sand, silt and gravel with less than 50% mud"                                                                "#E8C7AE",
   "continental-rise"                                                                                            "#a13f6d",
   "Algal mat"                                                                                                   "#018200",
   "Seagrass"                                                                                                    "#04e600",
   "Vegetated area"                                                                                              "#04e600",
   "Sand and bolders"                                                                                            "#e3ba9c",
   "Mixed reef/sediment"                                                                                         "#00ff00",
   "Shallow island fringe"                                                                                       "#7ab8b8",
   "Mixed reef and gravel"                                                                                       "#cc0000",
   "Sand"                                                                                                        "#ffd480",
   "Filter feeders"                                                                                              "#bc8fbc",
   "Channel - Shallow (<5 metres)"                                                                               "#99ffeb",
   "Wave dominated estuary"                                                                                      "#000080",
   "Mangrove/Saltmarsh Communities"                                                                              "#14A4AC",
   "Halodule uninervis (narrow form)/Halophila decipiens"                                                        "#70c299",
   "shelf"                                                                                                       "#c89e4a",
   "Estuarine - salt flats and saltmarshes"                                                                      "#ff1a53",
   "dense T.hemprichii with occassional H.uninervis(wide)/H.ovalis (coarse sand/shell/rubble)"                   "#00664d",
   "Medium Profile Reef"                                                                                         "#A16632",
   "Offshore Waters (5 - 10 metres)"                                                                             "#b3b3ff",
   "Zosteraceae/Ruppia/Halophila spp"                                                                            "#944dff",
   "Halodule uninervis"                                                                                          "#00ff99",
   "Thalassia hemprichii with mixed species"                                                                     "#335214",
   "slope"                                                                                                       "#ad4d29",
   "> 1000 g/m2"                                                                                                 "#002c00",
   "Mixed seagrass species"                                                                                      "#00ff00",
   "pinnacle"                                                                                                    "#319e6b",
   "Low Intertidal, non-mobile, smooth"                                                                          "#95896a",
   "Riverine/estuarine"                                                                                          "#00E6A9",
   "Halodule uninervis (wide)"                                                                                   "#00cc7a",
   "Live and Dead Coral"                                                                                         "#e699bf",
   "Low Profile Reef"                                                                                            "#A1967A",
   "Open Avicennia/Ceriops"                                                                                      "#f09475",
   "H. spinulosa"                                                                                                "#9f80ff",
   "Hard Sand"                                                                                                   "#FFAD00",
   "Sediment"                                                                                                    nil,
   "MOBILE SAND (subtidal)"                                                                                      "#eade9c",
   "Halophila ovalis with Enhalus acoroides"                                                                     "#e864b1",
   "Offshore deep"                                                                                               "#3d3d8f",
   "Tide dominated estuary"                                                                                      "#009999",
   "sparse T.hemprichii/H.ovalis with algae (sand)"                                                              "#b3ffda",
   "Halodule uninervis (narrow form)/Halophila ovalis with mixed species"                                        "#70c299",
   "Transition"                                                                                                  "#0000cc",
   "Enhalus acoroides"                                                                                           "#82a5c9",
   "Coral"                                                                                                       "#dd3c8c",
   "Mud and sand"                                                                                                "#FFAD00",
   "High energy coastal"                                                                                         "#79393b",
   "ALGAL MAT (subtidal)"                                                                                        "#770081",
   "reef"                                                                                                        "#640000",
   "Halophila ovalis/Halophila decipiens"                                                                        "#e864b1",
   "Exposed Rock"                                                                                                "#008080",
   "Z. meulleri, H. ovalis & C. serrulata"                                                                       "#5682bb",
   "H. ovalis, Z. muelleri, C. serrulata & S. isoetifolium"                                                      "#4d2600",
   "Tidal channel (subtidal)"                                                                                    "#1a8cff",
   "Light H. uninervis (thin)"                                                                                   "#bbff99",
   "Subtidal, mobile, below PS zone, rough"                                                                      "#52217a",
   "dense T.hemprichii with H.ovalis & massive coral (sand/shell/rubble)"                                        "#339966",
   "Zostera sp./Ruppia sp."                                                                                      "#944dff",
   "seamount/guyot"                                                                                              "#bd2889",
   "Closed Avicennia/Aegiceras"                                                                                  "#ffbf00",
   "Estuarine - water"                                                                                           "#00ffff",
   "Calcareous gravel, sand and silt"                                                                            "#BBA600",
   "Unknown / inacessible mangrove species"                                                                      "#A5A5A5",
   "Subtidal, non-mobile, below PS zone, rough"                                                                  "#2b09a3",
   "Saltpan"                                                                                                     "#E64C00",
   "Patchy Seagrass"                                                                                             nil,
   "Salt flats"                                                                                                  "#E64C00",
   "H. ovalis, Z. meulleri, H. spinulosa & S. isoetifolium"                                                      "#4d4d00",
   "Saltmarsh comm./A. marina"                                                                                   "#E64C00",
   "Ceriops tagal"                                                                                               "#7ab8b8",
   "Saltmarsh/Saltflat"                                                                                          "#E64C00",
   "Saltmarsh Communities"                                                                                       "#E64C00",
   "Cultural features"                                                                                           "#660066",
   "75-100 g/m2"                                                                                                 "#00cc00",
   "Channel - Moderate (5 - 10 metres)"                                                                          "#00e6b8",
   "Moderate T. hemprichii with mixed species"                                                                   "#b3994d",
   "Saline Grassland"                                                                                            "#437043",
   "Rocky intertidal"                                                                                            nil,
   "50-75 g/m2"                                                                                                  "#4dff4d",
   "Halophila sp./Ruppia sp."                                                                                    nil,
   "Moderate H. uninervis (thin) with H. uninervis (wide)"                                                       "#00cc7a",
   "Nearshore Waters (< 5 metres)"                                                                               "#00b359",
   "0 - 20m"                                                                                                     "#ffffcc",
   "Closed Rhizophora"                                                                                           "#b0fe9a",
   "Zostera muelleri and Halophila ovalis"                                                                       "#b21234",
   "Rhodoliths"                                                                                                  "#d7191c",
   "Mangroves"                                                                                                   "#14A4AC",
   "Rhizophora spp."                                                                                             "#6beb47",
   "Subtidal, non-mobile, PS zone, smooth"                                                                       "#7ecf63",
   "Zostera/Halophila/Ruppia"                                                                                    "#944dff",
   "subtidal sparse H.capricorni (coarse sand)"                                                                  "#993366",
   "Boulder / cobble / shingle / pebble / sand"                                                                  nil,
   "Shallow Lagoon"                                                                                              "#dbadeb",
   "Nearshore reef"                                                                                              "#640000",
   "Reef Slope"                                                                                                  "#800000",
   "Biosiliceous marl and calcareous clay"                                                                       "#3C6442",
   "Ocean embayment"                                                                                             "#5151fb",
   "< 25 g/m2"                                                                                                   "#daffb3",
   "Deep reef flat"                                                                                              "#006699",
   "Light H. decipiens"                                                                                          "#dd99ff",
   "Offshore sandy"                                                                                              "#cc99ff",
   "Sandy intermediate"                                                                                          "#FFAD00",
   "terrace"                                                                                                     "#FFAD00",
   "Mixed gravel and sand"                                                                                       "#d2a679",
   "Moderately deep; moderately high acoustic hardness; low slope; moderately smooth; unconsolidated material"   "#b3b3ff",
   "Silty Sand"                                                                                                  "#E8C7AE",
   "Avicennia marina/Ceriops tagal/Aegialitis annulata"                                                          "#cc9900",
   "Closed Avicennia/Ceriops"                                                                                    "#f09475",
   "GRAVEL/RUBBLE (subtidal)"                                                                                    "#c2783d",
   "Posidonia/Ruppia"                                                                                            "#704370",
   "Moderate C. serrulata/H. uninervis (wide)"                                                                   "#e6b800",
   "Lagoon sand / algae"                                                                                         "#00b386",
   "70-100%"                                                                                                     "#006600",
   "sparse H.uninervis(wide)/T.hemprichii (dense patches) with algae (sand)"                                     "#a8a88a",
   "None modelled with certainty"                                                                                "#989898",
   "PELAGIC"                                                                                                     "#dffcfd",
   "Subtidal, mobile, PS zone, rough"                                                                            "#aa27c1",
   "Hard"                                                                                                        "#802000",
   "Coral communities"                                                                                           nil,
   "Central Basin"                                                                                               "#000080",
   "Roads"                                                                                                       "#33334d",
   "Dredged"                                                                                                     "#A5A5A5",
   "Reef Crest"                                                                                                  "#ffb3b3",
   "Mixed kelp and other algae"                                                                                  "#00ff00",
   "Rock wall"                                                                                                   nil,
   "Vegetated Unconsolidated"                                                                                    "#04e600",
   "H. ovalis, H. spinulosa & H. uninervis"                                                                      "#6e93ab",
   "Thalassadendron ciliatum with Thalassia hemprichii"                                                          "#166969",
   "Cymodocea serrulata"                                                                                         "#ff904d",
   "Shoal, Tidal"                                                                                                "#ace600",
   "FILTER FEEDERS (subtidal)"                                                                                   "#00ffff",
   "Ruppia sp./Zosteraceae"                                                                                      "#D1FF73",
   "Open Avicennia"                                                                                              "#b35900",
   "Reef, Submerged"                                                                                             "#813918",
   "Enhalus acoroides with mixed species"                                                                        "#bd3f28",
   "Flood- and Ebb-tide Delta"                                                                                   "#7ab8b8",
   "Bare Subtrate"                                                                                               "#8c8c8c",
   "Subtidal, non-mobile, PS zone, rough"                                                                        "#33892a",
   "Soft"                                                                                                        "#ff80ff",
   "Water and terrestrial"                                                                                       "#000066",
   "Unknown"                                                                                                     nil,
   "subtidal sparse Halodule uninervis (dense patches) with H.ovalis/green algae (sand)"                         "#b32d00",
   "canyon"                                                                                                      "#c630c6",
   "Posidonia/Halophila/Ruppia"                                                                                  "#7070a9",
   "escarpment"                                                                                                  "#4228c6",
   "Cymodocea serrulata and Zostera muelleri"                                                                    "#ff904d",
   "depression"                                                                                                  "#000000",
   "Closed Rhizophora/Avicennia"                                                                                 "#009900",
   "Closed Ceriops"                                                                                              "#f09475",
   "Medium Density Filter Feeders (Sponges & Soft Corals)"                                                       "#9c6230",
   "Mud and calcareous clay"                                                                                     "#FFAD00",
   "Mixed vegetation"                                                                                            "#009999",
   "Branching"                                                                                                   "#00ffaa",
   "subtidal sparse H.uninervis(wide) (dense patches) with H.ovalis & green algae (sand)"                        "#cccc00",
   "Embayment - Subtidal Zone"                                                                                   "#ebaddb",
   "Very shallow; very high acoustic hardness; very low slope; smooth; high occurrence of consolidated material" "#ffffcc",
   "Deep Areas"                                                                                                  "#16389c",
   "Halodule uninervis (narrow form)"                                                                            "#70c299",
   "Sessile invertebrates (SI)"                                                                                  "#ab7bea",
   "Z. meulleri, H. ovalis, C. serrulata & S. isoetifolium"                                                      "#558000",
   "Seawall"                                                                                                     "#000000",
   "Bare Coarse Sediment with possibility small rocky outcrops/rippled sand"                                     "#A16632",
   "Reef flat"                                                                                                   "#640000",
   "Thalassia hemprichii with Enhalus acoroides"                                                                 "#39fc03",
   "tidal-sandwave/sand-bank"                                                                                    "#399d42",
   "1-10%"                                                                                                       "#b3ffb3",
   "Wrack"                                                                                                       "#ff8000",
   "sill"                                                                                                        "#2634aa",
   "Rocky Reef"                                                                                                  "#994d33",
   "Sandy channels"                                                                                              "#FFF9A5",
   "High Density Mixed Community (Corals, Algae, Sponges & Soft Corals)"                                         "#79393b",
   "Unvegetated"                                                                                                 "#e9beaf",
   "Bare Sand - Mud (either flats/bars)"                                                                         "#A68C73",
   "SANDY BEACH (intertidal)"                                                                                    "#fafa38",
   "Pelagic clay"                                                                                                "#A5A5A5",
   "Zostera capricorni"                                                                                          "#bd3f28",
   "15 - 45%"                                                                                                    "#9ecc3b",
   "Mangrove"                                                                                                    "#14A4AC",
   "High Density Filter Feeders (Sponges & Soft Corals)"                                                         "#79393b",
   "Brackish lake"                                                                                               "#e07552",
   "C. serrulata"                                                                                                "#ff904d",
   "Sessile invertebrates"                                                                                       "#ab7bea",
   "Sediment bottom"                                                                                             nil,
   "subtidal T.hemprichii & green algae (sand/rubble)"                                                           "#4dc3ff",
   "Halophila ovalis and Halophila uninervis"                                                                    "#e864b1",
   "Bare Coarse Sediment Rippled Sand"                                                                           "#FFAD00",
   "Bare Sandy to Mixed Sediments"                                                                               "#E8C7AE",
   "Unassigned"                                                                                                  "#999999",
   "None modelled with confidence"                                                                               "#989898",
   "Z. meulleri, H. ovalis & H. uninervis"                                                                       "#5dcfba",
   "Saltmarsh"                                                                                                   "#E64C00",
   "Backreef / shallow lagoon"                                                                                   "#33adff",
   "Subtidal sand"                                                                                               "#E8C7AE",
   "Fringing coral reefs"                                                                                        nil,
   "Limestone"                                                                                                   "#E8C7AE",
   "Cymodocea rotundata"                                                                                         "#fff566",
   "subtidal sparse H.uninervis(wide) with green algae (sand)"                                                   "#bd9828",
   "Avicennia marina"                                                                                            "#b35900",
   "Rhizophora stylosa closed forest"                                                                            "#6beb47",
   "Sparse Patchy Seagrass"                                                                                      nil,
   "Mixed mangrove species"                                                                                      "#00ff00",
   "Rocky platform"                                                                                              "#994d33",
   "Low-Medium Density Mixed Community (Corals, Algae, Sponges & Soft Corals)"                                   "#95896a",
   "Enhalus acoroides with Halophila ovalis"                                                                     "#eca093",
   "Avicennia marina/Bruguiera exaristata"                                                                       "#ffb366",
   "Rubble and Sand"                                                                                             "#999966",
   "Intertidal, non-mobile substrates"                                                                           "#e6e600",
   "Zostera muelleri/Halophila uninervis"                                                                        "#b21234",
   "Samphire-dominated Saltpan"                                                                                  "#cc0066",
   "Hard substrate"                                                                                              "#802000",
   "Rhizophora stylosa open woodland"                                                                            "#6beb47",
   "Halophila ovalis with mixed species"                                                                         "#e864b1",
   "Aquatic Macrophytes"                                                                                         "#32B34E",
   "Z. meulleri, H. ovalis & H. spinulosa"                                                                       "#3fb3d0",
   "Halodule uninervis (wide form)"                                                                              "#00cc7a",
   "Dense H. uninervis (thin)/ T. hemprichii"                                                                    "#26734d",
   "Massive"                                                                                                     "#b30000",
   "Halophila ovalis, Halophila spinulosa and Zostera muelleri"                                                  "#e864b1",
   "Diverse sandy"                                                                                               "#FFAD00",
   "Zostera sp."                                                                                                 "#944dff",
   "Medium Density Mixed Community (Corals, Algae, Sponges & Soft Corals)"                                       "#9c6230",
   "Posidonia/Zostera"                                                                                           "#cc00cc",
   "Mixed seagrass and reef"                                                                                     "#009999",
   "T.hemprichii in sandy pool (sand)"                                                                           "#00cccc",
   "scarp"                                                                                                       nil,
   "Hard coral and all mixes"                                                                                    "#b33c00",
   "Rock"                                                                                                        "#994d33",
   "subtidal sparse H.ovalis with green algae (sand)"                                                            "#4d6600",
   "Halodule uninervis (narrow form) with Enhalus acoroides"                                                     "#70c299",
   "Shallow; very low acoustic hardness; very low slope; very smooth; unconsolidated material"                   "#ffce99",
   "Thalassodendron ciliatum"                                                                                    "#166969",
   "subtidal sparse T.hemprichii (sand)"                                                                         "#0077b3",
   "bank/shoals"                                                                                                 "#3cc4c3",
   "MACROALGAE (subtidal)"                                                                                       "#ca00db",
   "Soft substrate"                                                                                              "#f4d371",
   "Reef and shoal"                                                                                              "#974749",
   "Unclassified"                                                                                                nil,
   "Moderate H. uninervis (thin) with mixed species"                                                             "#73e600",
   "basin"                                                                                                       "#29bd44",
   "Light H. spinulosa/H. uninervis (wide)"                                                                      "#ccccff",
   "Macroalgae, undifferentiated"                                                                                "#14A4AC",
   "31-50%"                                                                                                      "#38a748",
   "Hard rocky reefs"                                                                                            nil,
   "T.hemprichii with H.uninervis(narrow) (sand)"                                                                "#6f2a6f",
   "plain"                                                                                                       "#FFF9A5",
   "Zostera"                                                                                                     "#944dff",
   "Intermittent estuary"                                                                                        "#e68a00",
   "T.hemprichii/H.uninervis(wide) with massive corals/clams/red algae (sand/rubble)"                            "#ff3300",
   "Island"                                                                                                      "#000000",
   "Rocky shores"                                                                                                "#661a00",
   "sparse H.uninervis (narrow) with H.ovalis and red/brown algae (sand/shell/rock)"                             "#993300",
   "Low Density Mixed Community (Corals, Algae, Sponges & Soft Corals)"                                          "#95896a",
   "Freshwater"                                                                                                  "#66d9ff",
   "Vegetation"                                                                                                  "#04e600",
   "Macrolgae (+ rock and sediment)"                                                                             "#608000",
   "Zostera muelleri and Cymodocea serrulata"                                                                    "#b21234",
   "Ruppia sp."                                                                                                  "#D1FF73",
   "Posidonia"                                                                                                   "#558000",
   "Channel - Deep (10 - 20 metres"                                                                              "#004d4d",
   "Deep Lagoon"                                                                                                 "#8529a3",
   "Ruppia"                                                                                                      "#bc8fbc",
   "Zostera muelleri and Syringodium isoetifolium"                                                               "#b21234",
   "100-1000 g/m2"                                                                                               "#007100",
   "Sand/rubble"                                                                                                 "#E8C7AE",
   "Closed Avicennia"                                                                                            "#b35900",
   "MACROALGAE (intertidal)"                                                                                     "#f6a4fe",
   "Bommies"                                                                                                     "#60401f",
   "60 -200m"                                                                                                    "#ff9c33",
   "deep/hole/valley"                                                                                            "#a26d3f",
   "BARE REEF (subtidal)"                                                                                        "#feac00",
   "Algal Beds"                                                                                                  "#018200",
   "knoll/abyssal-hills/hills/mountains/peak"                                                                    "#348ac9",
   "Bioturbated mud"                                                                                             "#974749",
   "Halophila spinulosa"                                                                                         "#9f80ff",
   "H. ovalis"                                                                                                   "#e864b1",
   "Halophila"                                                                                                   "#8cff1a",
   "Subtidal, non-mobile, below PS zone, smooth"                                                                 "#7006de3",
   "20 - 60m"                                                                                                    "#ffce99",
   "Patchy Reef"                                                                                                 nil,
   "BARE REEF (intertidal)"                                                                                      "#fed66d",
   "Live Coral and Rock, reef slope"                                                                             "#e05275",
   "Barrier/back-barrier"                                                                                        "#ff9900",
   "No Seagrass"                                                                                                 "#d9d9d9",
   "Lagoon sand / rubble"                                                                                        "#669999",
   "Subtidal, mobile, below PS zone, smooth"                                                                     "#295aa6",
   "Sandy lagoon (protected)"                                                                                    nil,
   "Rock (+ live coral), reef crest"                                                                             "#cc9900",
   "Avicennia marina/Ceriops tagal"                                                                              "#cc9900",
   "Sponge"                                                                                                      "#FCFAE2",
   "Patchy Posidonia"                                                                                            "#a5d742",
   "Shoal, Submerged"                                                                                            "#4d6600",
   "Mixed other algae and SI"                                                                                    "#ff8533",
   "Mixed vegetation and SI"                                                                                     "#f59053",
   "Light C. serrulata"                                                                                          "#ffe6b3",
   "Non-Reef"                                                                                                    "#b3b3ff",
   "Mangrove/intertidal habitat"                                                                                 "#14A4AC",
   "Zostera muelleri"                                                                                            "#b21234",
   "Bioturbated sand"                                                                                            "#A1967A",
   "Deep; low acoustic hardness; low slope; very smooth; unconsolidated material"                                "#4d4dff",
   "Macroalgae (+ rock, live and dead coral)"                                                                    "#00e673",
   "Riverine"                                                                                                    "#ff9900",
   "Patchy hard rocky reefs / exposed rock"                                                                      nil,
   "Silt"                                                                                                        "#E5D6ED",
   "H. uninervis, H. ovalis & H. spinulosa"                                                                      "#558000",
   "Low Intertidal, non-mobile, rough"                                                                           "#95896a",
   "Other algae"                                                                                                 "#d0b0a8",
   "Subtidal, mobile, PS zone, smooth"                                                                           "#e16bc3",
   "Halophila decipiens with mixed species"                                                                      "#85b102",
   "Halophila ovalis"                                                                                            "#e864b1",
   "Bruguiera exaristata"                                                                                        "#b2fc03",
   "Z. meulleri, H. uninervis & S. isoetifolium"                                                                 "#55b03a",
   "Tidal Sand Banks"                                                                                            "#FFF9A5",
   "Gravel"                                                                                                      "#994d00",
   "Lumnitzera racemosa"                                                                                         "#00cc00",
   "abyssal-plain/deep ocean floor"                                                                              "#b2b434",
   "Mixed reef and sand"                                                                                         "#ff884d",
   "25-50 g/m2"                                                                                                  "#b5ff66",
   "Moderate C. serrulata with mixed species"                                                                    "#ff904d",
   "Channel"                                                                                                     "#006666",
   "> 0m"                                                                                                        "#666666",
   "ridge"                                                                                                       nil,
   "Light H. decipiens/H. uninervis (thin)"                                                                      "#bbff99",
   "MUDFLAT"                                                                                                     "#8c8c8c",
   "sparse T.hemprichii with soft coral  (sand)"                                                                 "#00e6ac",
   "Bare Rocky Reef"                                                                                             "#640000",
   "Mangrove/Saltmarsh"                                                                                          "#14A4AC",
   "Moderately shallow; high acoustic hardness; high slope; rugose; low occurrence of consolidated material"     "#ff9c33",
   "Light C. serrulata with mixed species"                                                                       "#ffbb99",
   "mound"                                                                                                       "#14A4AC",
   "trench/trough"                                                                                               "#307867",
   "Cobble"                                                                                                      "#808080",
   "saddle"                                                                                                      "#c64d5a",
   "Dense H. uninervis (thin) with mixed species"                                                                "#558000",
   "apron/fan"                                                                                                   "#b52829",
   "dense E.acoroides circle (sand)"                                                                             "#86b300",
   "C. serrulata & S. isoetifolium"                                                                              "#ec6344",
   "Mixed kelp and SI"                                                                                           "#ff8533",
   "Eroded sandstone"                                                                                            "#a6a6a6",
   "Syringodium isoetifolium with mixed species"                                                                 "#73e600",
   "Sand (subtidal)"                                                                                             "#A68C73",
   "Rocky headland"                                                                                              "#000000",
   "Channel - Moderate - Inshore (5 - 10 metres)"                                                                "#00e6b8",
   "Sonneratia alba"                                                                                             "#dfd19f",
   "Branching Live Coral, reef slope"                                                                            "#b561d1",
   "Light H. ovalis with H. uninervis (thin)"                                                                    "#f5bcdd",
   "Bruguiera parviflora/Rhizophora stylosa"                                                                     "#85b102",
   "Inshore, algae/sponge habitat"                                                                               "#f17493",
   "Syringodium isoetifolium"                                                                                    "#3385ff",
   "Beach"                                                                                                       "#FFF9A5",
   "Dense H. uninervis (thin) with H. ovalis"                                                                    "#e864b1",
   "Pavement"                                                                                                    "#cccc00",
   "Very deep; low acoustic hardness; very high slope; very rugose; unconsolidated material"                     "#000080",
   "Dense H. ovalis"                                                                                             "#e864b1",
   "Calcareous ooze"                                                                                             "#BBA600",
   "Artificial substrate"                                                                                        nil,
   "Posidonia/Zostera/Halophila"                                                                                 "#b30000",
   "Posidonia/Halophila"                                                                                         "#609f60",
   "10-40 %"                                                                                                     "#33ff33",
   "Mud and tidal flats"                                                                                         "#FFAD00",
   "plateau"                                                                                                     "#6ba242",
   "Rhizophora stylosa/Bruguiera spp./Ceriops spp."                                                              "#6beb47",
   "Thalassia hemprichii"                                                                                        "#b3994d",
   "Halophila ovalis with Thalassia hemprichii"                                                                  "#e864b1",
   "Bedrock"                                                                                                     "#4d3319",
   "Reef Flat Outer"                                                                                             "#ff8000",
   "Rubble+Sand"                                                                                                 "#999966",
   "Halophila/Ruppia"                                                                                            nil,
   "40-70 %"                                                                                                     "#00cc00",
   "subtidal H.tricostata/H.decipiens - dense in patches (sand/mud)"                                             "#ff9999",
   "Large Rock"                                                                                                  "#4d3319",
   "Marine"                                                                                                      "#000099",
   "Other"                                                                                                       "#BBA600",
   "Coastline - Sand"                                                                                            "#ffffcc",
   "H.uninervis(wide)/T.hemprichii (dense patches) with H.ovalis & algae (sand)"                                 "#00e600",
   "subtidal H.decipiens/H.ovalis (sand/mud)"                                                                    "#ffcc00",
   "CORAL REEF (subtidal)"                                                                                       "#ff0000",
   "Halophila tricostata"                                                                                        "#d9cda6",
   "Saltmarsh/Mangrove"                                                                                          "#E64C00",
   "1-10 %"                                                                                                      "#b3ffb3",
   "Sparse Seagrass"                                                                                             "#D1FF73",
   "51-75%"                                                                                                      "#2a753a",
   "Unconsolidated Bare Substrate"                                                                               "#A5A5A5",
   "Pavement reef"                                                                                               "#640000",
   "Mixed sandy bottom"                                                                                          "#00ff00",
   "Zostera muelleri and Halophila uninervis"                                                                    "#b21234",
   "Reef, Tidal"                                                                                                 "#e6994c",
   "bank"                                                                                                        "#974749",
   "Kelp"                                                                                                        "#006600",
   "Fluvial (bay-head) Delta"                                                                                    "#1a8cff",
   "Halophila sp./Zostera sp."                                                                                   nil,
   "Light H. uninervis (wide) with mixed species"                                                                "#33ff99",
   "Halophila ovalis/Halodule uninervis (narrow form)"                                                           "#e864b1",
   "Halophila capricorni"                                                                                        "#b3664d",
   "Aegiceras corniculatum"                                                                                      "#009999",
   "Zostera/Halophila"                                                                                           "#944dff",
   "Cay"                                                                                                         "#FFAD00",
   "Closed Mixed"                                                                                                "#85b102",
   "SANDSHOAL"                                                                                                   "#fef8a4",
   "Reef flat Inner"                                                                                             "#fcf769",
   "Closed Rhizophora/Aegiceras"                                                                                 "#7af703",
   "Zostera/Ruppia"                                                                                              "#944dff",
   "High Profile Reef"                                                                                           "#974749",
   "Halodule uninervis (narrow)"                                                                                 "#70c299",
   "Salt marsh"                                                                                                  "#E64C00",
   "Sand (intertidal)"                                                                                           "#E8C7AE",
   "patches"                                                                                                     "#bfbfbf",
   "Deep Reef Structures"                                                                                        "#24478f",
   "Inshore reef"                                                                                                "#a16632",
   "76-100%"                                                                                                     "#374705",
   "Mixed reef, gravel and sand"                                                                                 "#cccc00",
   "Non-mangrove vegetation communities"                                                                         "#000000",
   "CORAL REEF (intertidal)"                                                                                     "#fea486",
   "Muddy bottom"                                                                                                nil,
   "Halophila uninveris and Cymodocea serrulata"                                                                 "#829e2e",
   "15 - 45% Pat"                                                                                                "#9ecc3b",
   "Bruguiera parviflora"                                                                                        "#e3fe92",
   "Volcanic sand and grit"                                                                                      "#000000",
   "MANGALS"                                                                                                     "#016300",
   "Unidentified"                                                                                                "#999999"})

(def *habitat-titles*
  "Maps habitat classifications to their titles, as they appear in the
  legend (often, but not always, the same as the SM_HAB_CLS value)."
  {"Offshore Waters (10  - 20 metres)"                                                         "Offshore Waters (10 - 20 metres)",
   "Coralline algae"                                                                           "Coralline algae",
   "T.hemprichii/H.uninervis(wide) and green algae/Halophila ovalis (coarse sand/rubble/shell)"
   "T.hemprichii/H.uninervis(wide) and green algae/Halophila ovalis (coarse sand/rubble/shell)",
   "Light H. uninervis with H. ovalis"                                                         "Light H. uninervis with H. ovalis",
   "11-30%"                                                                                    "11-30%",
   "Halodule uninervis (narrow form) with mixed species"                                       "Halodule uninervis (narrow)",
   "Benthic micro-algae"                                                                       "Benthic microalgae",
   "Closed Bruguiera"                                                                          "Bruguiera",
   "Offshore reef"                                                                             "Offshore reef",
   "Fine sediment"                                                                             "Fine sediment",
   "Sonneratia alba/Rhizophora stylosa/Avicennia marina"                                       "Sonneratia alba/Rhizophora stylosa/Avicennia marina",
   "Halophila decipiens"                                                                       "Halophila decipiens",
   "Moderate H. uninervis (thin) with H. ovalis"                                               "Moderate H. uninervis (narrow) with H. ovalis",
   "Sonneratia alba/Aegiceras corniculatum"                                                    "Sonneratia alba/Aegiceras corniculatum",
   "Open Ceriops"                                                                              "Ceriops",
   "Halophila spinulosa and Halophila uninervis"                                               "Halophila spinulosa",
   "Light C. rotundata"                                                                        "Light C. rotundata",
   "Plunging cliff"                                                                            "Plunging cliff",
   "Macroalgae"                                                                                "Macroalgae",
   "H. uninervis & H. ovalis"                                                                  "H. uninervis & H. ovalis",
   "valley"                                                                                    "Valley",
   "subtidal H.ovalis/H.uninervis(wide) - dense in patches (sand)"                             "subtidal H.ovalis/H.uninervis(wide) - dense in patches (sand)",
   "Halophila sp."                                                                             "Halophila sp.",
   "Thalassia hemprichii with Halophila ovalis"                                                "Thalassia hemprichii with Halophila ovalis",
   "Islands and rocks"                                                                         "Islands and rocks",
   "Caulerpa & H. ovalis"                                                                      "Caulerpa & H. ovalis",
   "Boulder+Rubble"                                                                            "Boulder and rubble",
   "Invertebrate Community"                                                                    "Invertebrate Community",
   "Halophila ovalis with Halodule uninervis (narrow form)"                                    "Halophila ovalis",
   "Halodule uninervis (narrow form)/Halophila ovalis"                                         "Halodule uninervis (narrow)",
   "Caulerpa"                                                                                  "Caulerpa",
   "Open Bruguiera"                                                                            "Bruguiera",
   "Blue hole"                                                                                 "Blue hole",
   "Sand, silt and gravel with less than 50% mud"                                              "Sand, silt and gravel with less than 50% mud",
   "continental-rise"                                                                          "continental-rise",
   "Algal mat"                                                                                 "Algal Beds",
   "Seagrass"                                                                                  "Seagrass",
   "Vegetated area"                                                                            "Vegetation",
   "Sand and bolders"                                                                          "Sand and boulders",
   "Mixed reef/sediment"                                                                       "Mixed seagrass species",
   "Shallow island fringe"                                                                     "Shallow island fringe",
   "Mixed reef and gravel"                                                                     "Mixed reef and gravel",
   "Sand"                                                                                      "Sand",
   "Filter feeders"                                                                            "Filter feeders",
   "Channel - Shallow (<5 metres)"                                                             "Channel - Shallow (< 5 metres)",
   "Wave dominated estuary"                                                                    "Wave dominated estuary",
   "Mangrove/Saltmarsh Communities"                                                            "Mangroves",
   "Halodule uninervis (narrow form)/Halophila decipiens"                                      "Halodule uninervis (narrow)",
   "shelf"                                                                                     "shelf",
   "Estuarine - salt flats and saltmarshes"                                                    "Estuarine - salt flats and saltmarshes",
   "dense T.hemprichii with occassional H.uninervis(wide)/H.ovalis (coarse sand/shell/rubble)"
   "dense T.hemprichii with occassional H.uninervis(wide)/H.ovalis (coarse sand/shell/rubble)",
   "Medium Profile Reef"                                                                       "Medium Profile Reef",
   "Offshore Waters (5 - 10 metres)"                                                           "Offshore Waters (5 - 10 metres)",
   "Zosteraceae/Ruppia/Halophila spp"                                                          "Zostera muelleri / Halophila uninervis",
   "Halodule uninervis"                                                                        "Halodule uninervis",
   "Thalassia hemprichii with mixed species"                                                   "Thalassia hemprichii with mixed species",
   "slope"                                                                                     "slope",
   "> 1000 g/m2"                                                                               "> 1000",
   "Mixed seagrass species"                                                                    "Mixed seagrass species",
   "pinnacle"                                                                                  "pinnacle",
   "Low Intertidal, non-mobile, smooth"                                                        "Low Profile Reef",
   "Riverine/estuarine"                                                                        "Riverine/estuarine",
   "Halodule uninervis (wide)"                                                                 "Halodule uninervis (wide)",
   "Live and Dead Coral"                                                                       "Coral (live/dead combined)",
   "Low Profile Reef"                                                                          "Low Profile Reef",
   "Open Avicennia/Ceriops"                                                                    "Ceriops",
   "H. spinulosa"                                                                              "Halophila spinulosa",
   "Hard Sand"                                                                                 "Hard Sand",
   "Sediment"                                                                                  "Sediment bottom",
   "MOBILE SAND (subtidal)"                                                                    "Mobile sand (subtidal)",
   "Halophila ovalis with Enhalus acoroides"                                                   "Halophila ovalis",
   "Offshore deep"                                                                             "Offshore deep",
   "Tide dominated estuary"                                                                    "Tide dominated estuary",
   "sparse T.hemprichii/H.ovalis with algae (sand)"                                            "sparse T.hemprichii/H.ovalis with algae (sand)",
   "Halodule uninervis (narrow form)/Halophila ovalis with mixed species"                      "Halodule uninervis (narrow)",
   "Transition"                                                                                "Transition",
   "Enhalus acoroides"                                                                         "Enhalus acoroides",
   "Coral"                                                                                     "Coral",
   "Mud and sand"                                                                              "Mud and tidal flats",
   "High energy coastal"                                                                       "High Profile Reef",
   "ALGAL MAT (subtidal)"                                                                      "Algal mat (subtidal)",
   "reef"                                                                                      "Reef",
   "Halophila ovalis/Halophila decipiens"                                                      "Halophila ovalis",
   "Exposed Rock"                                                                              "Exposed rock",
   "Z. meulleri, H. ovalis & C. serrulata"                                                     "Z. meulleri, H. ovalis & C. serrulata",
   "H. ovalis, Z. muelleri, C. serrulata & S. isoetifolium"                                    "H. ovalis, Z. muelleri, C. serrulata & S. isoetifolium",
   "Tidal channel (subtidal)"                                                                  "Tidal channel (subtidal)",
   "Light H. uninervis (thin)"                                                                 "Light H. uninervis (narrow)",
   "Subtidal, mobile, below PS zone, rough"                                                    "Subtidal, mobile, below PS zone, rough",
   "dense T.hemprichii with H.ovalis & massive coral (sand/shell/rubble)"                      "dense T.hemprichii with H.ovalis & massive coral (sand/shell/rubble)",
   "Zostera sp./Ruppia sp."                                                                    "Zostera muelleri / Halophila uninervis",
   "seamount/guyot"                                                                            "seamount/guyot",
   "Closed Avicennia/Aegiceras"                                                                "Avicennia / Aegiceras",
   "Estuarine - water"                                                                         "Estuarine - water",
   "Calcareous gravel, sand and silt"                                                          "Calcareous ooze",
   "Unknown / inacessible mangrove species"                                                    "Unknown / inacessible mangrove species",
   "Subtidal, non-mobile, below PS zone, rough"                                                "Subtidal, non-mobile, below PS zone, rough",
   "Saltpan"                                                                                   "Salt flats",
   "Patchy Seagrass"                                                                           "Patchy Seagrass",
   "Salt flats"                                                                                "Salt flats",
   "H. ovalis, Z. meulleri, H. spinulosa & S. isoetifolium"                                    "H. ovalis, Z. meulleri, H. spinulosa & S. isoetifolium",
   "Saltmarsh comm./A. marina"                                                                 "Salt flats",
   "Ceriops tagal"                                                                             "Ceriops tagal",
   "Saltmarsh/Saltflat"                                                                        "Salt flats",
   "Saltmarsh Communities"                                                                     "Salt flats",
   "Cultural features"                                                                         "Cultural features",
   "75-100 g/m2"                                                                               "75-100",
   "Channel - Moderate (5 - 10 metres)"                                                        "Channel - Moderate (5 - 10 metres)",
   "Moderate T. hemprichii with mixed species"                                                 "Moderate T. hemprichii with mixed species",
   "Saline Grassland"                                                                          "Saline Grassland",
   "Rocky intertidal"                                                                          "Rocky intertidal",
   "50-75 g/m2"                                                                                "50-75",
   "Halophila sp./Ruppia sp."                                                                  "Halophila sp. ± Ruppia sp., Zostera sp.",
   "Moderate H. uninervis (thin) with H. uninervis (wide)"                                     "Moderate H. uninervis (narrow) with H. uninervis (wide)",
   "Nearshore Waters (< 5 metres)"                                                             "Nearshore Waters (< 5 metres)",
   "0 - 20m"                                                                                   "0 - 20m",
   "Closed Rhizophora"                                                                         "Rhizophora",
   "Zostera muelleri and Halophila ovalis"                                                     "Zostera muelleri",
   "Rhodoliths"                                                                                "Rhodolith",
   "Mangroves"                                                                                 "Mangroves",
   "Rhizophora spp."                                                                           "Rhizophora spp.",
   "Subtidal, non-mobile, PS zone, smooth"                                                     "Subtidal, non-mobile, PS zone, smooth",
   "Zostera/Halophila/Ruppia"                                                                  "Zostera muelleri / Halophila uninervis",
   "subtidal sparse H.capricorni (coarse sand)"                                                "subtidal sparse H.capricorni (coarse sand)",
   "Boulder / cobble / shingle / pebble / sand"                                                "Boulder/cobble/shingle/pebble/sand",
   "Shallow Lagoon"                                                                            "Shallow lagoon",
   "Nearshore reef"                                                                            "Nearshore reef",
   "Reef Slope"                                                                                "Reef slope",
   "Biosiliceous marl and calcareous clay"                                                     "Biosiliceous marl and calcareous clay",
   "Ocean embayment"                                                                           "Ocean embayment",
   "< 25 g/m2"                                                                                 "< 25",
   "Deep reef flat"                                                                            "Deep reef flat",
   "Light H. decipiens"                                                                        "Light H. decipiens",
   "Offshore sandy"                                                                            "Offshore sandy",
   "Sandy intermediate"                                                                        "Sandy intermediate",
   "terrace"                                                                                   "Terrace",
   "Mixed gravel and sand"                                                                     "Mixed gravel and sand",
   "Moderately deep; moderately high acoustic hardness; low slope; moderately smooth; unconsolidated material"
   "Moderately deep, low slope, unconsolidated",
   "Silty Sand"                                                                                "Silty Sand",
   "Avicennia marina/Ceriops tagal/Aegialitis annulata"                                        "Avicennia marina/Ceriops tagal/Aegialitis annulata",
   "Closed Avicennia/Ceriops"                                                                  "Ceriops",
   "GRAVEL/RUBBLE (subtidal)"                                                                  "Gravel/rubble (subtidal)",
   "Posidonia/Ruppia"                                                                          "Posidonia/Ruppia",
   "Moderate C. serrulata/H. uninervis (wide)"                                                 "Moderate C. serrulata/H. uninervis (wide)",
   "Lagoon sand / algae"                                                                       "Lagoon sand / algae",
   "70-100%"                                                                                   "70-100%",
   "sparse H.uninervis(wide)/T.hemprichii (dense patches) with algae (sand)"                   "sparse H.uninervis(wide)/T.hemprichii (dense patches) with algae (sand)",
   "None modelled with certainty"                                                              "None modelled with certainty",
   "PELAGIC"                                                                                   "Pelagic",
   "Subtidal, mobile, PS zone, rough"                                                          "Subtidal, mobile, PS zone, rough",
   "Hard"                                                                                      "Hard",
   "Coral communities"                                                                         "Coral communities",
   "Central Basin"                                                                             "Central Basin",
   "Roads"                                                                                     "Roads",
   "Dredged"                                                                                   "Dredged",
   "Reef Crest"                                                                                "Reef crest",
   "Mixed kelp and other algae"                                                                "Mixed seagrass species",
   "Rock wall"                                                                                 "Rock wall",
   "Vegetated Unconsolidated"                                                                  "Vegetation",
   "H. ovalis, H. spinulosa & H. uninervis"                                                    "H. ovalis, H. spinulosa & H. uninervis",
   "Thalassadendron ciliatum with Thalassia hemprichii"                                        "Thalassodendron ciliatum with Thalassia hemprichii",
   "Cymodocea serrulata"                                                                       "Cymodocea serrulata",
   "Shoal, Tidal"                                                                              "Shoal, tidal",
   "FILTER FEEDERS (subtidal)"                                                                 "Filter feeders (subtidal)",
   "Ruppia sp./Zosteraceae"                                                                    "Ruppia sp. ± Zosteraceae",
   "Open Avicennia"                                                                            "Avicennia",
   "Reef, Submerged"                                                                           "Reef, submerged",
   "Enhalus acoroides with mixed species"                                                      "Enhalus acoroides with mixed species",
   "Flood- and Ebb-tide Delta"                                                                 "Flood- and Ebb-tide Delta",
   "Bare Subtrate"                                                                             "Bare Substrate",
   "Subtidal, non-mobile, PS zone, rough"                                                      "Subtidal, non-mobile, PS zone, rough",
   "Soft"                                                                                      "Soft",
   "Water and terrestrial"                                                                     "Water and terrestrial",
   "Unknown"                                                                                   "Unknown",
   "subtidal sparse Halodule uninervis (dense patches) with H.ovalis/green algae (sand)"
   "subtidal sparse Halodule uninervis (dense patches) with H.ovalis/green algae (sand)",
   "canyon"                                                                                    "canyon",
   "Posidonia/Halophila/Ruppia"                                                                "Posidonia/Halophila/Ruppia",
   "escarpment"                                                                                "escarpment",
   "Cymodocea serrulata and Zostera muelleri"                                                  "Cymodocea serrulata",
   "depression"                                                                                "Depression",
   "Closed Rhizophora/Avicennia"                                                               "Rhizophora/Avicennia",
   "Closed Ceriops"                                                                            "Ceriops",
   "Medium Density Filter Feeders (Sponges & Soft Corals)"                                     "Medium Profile Reef",
   "Mud and calcareous clay"                                                                   "Mud and tidal flats",
   "Mixed vegetation"                                                                          "Mixed vegetation",
   "Branching"                                                                                 "Branching",
   "subtidal sparse H.uninervis(wide) (dense patches) with H.ovalis & green algae (sand)"
   "subtidal sparse H.uninervis(wide) (dense patches) with H.ovalis & green algae (sand)",
   "Embayment - Subtidal Zone"                                                                 "Embayment - Subtidal Zone",
   "Very shallow; very high acoustic hardness; very low slope; smooth; high occurrence of consolidated material"
   "Very shallow, very low slope, highly consolidated",
   "Deep Areas"                                                                                "Deep areas",
   "Halodule uninervis (narrow form)"                                                          "Halodule uninervis (narrow)",
   "Sessile invertebrates (SI)"                                                                "Sessile invertebrates (SI)",
   "Z. meulleri, H. ovalis, C. serrulata & S. isoetifolium"                                    "Z. meulleri, H. ovalis, C. serrulata & S. isoetifolium",
   "Seawall"                                                                                   "Seawall",
   "Bare Coarse Sediment with possibility small rocky outcrops/rippled sand"                   "Bare Coarse Sediment with possibility small rocky outcrops/rippled sand",
   "Reef flat"                                                                                 "Reef flat",
   "Thalassia hemprichii with Enhalus acoroides"                                               "Thalassia hemprichii with Enhalus acoroides",
   "tidal-sandwave/sand-bank"                                                                  "tidal-sandwave/sand-bank",
   "1-10%"                                                                                     "1-10%",
   "Wrack"                                                                                     "Wrack",
   "sill"                                                                                      "sill",
   "Rocky Reef"                                                                                "Rocky Reef",
   "Sandy channels"                                                                            "Sandy channels",
   "High Density Mixed Community (Corals, Algae, Sponges & Soft Corals)"                       "High Profile Reef",
   "Unvegetated"                                                                               "Unvegetated",
   "Bare Sand - Mud (either flats/bars)"                                                       "Bare Sand - Mud (flats/bars)",
   "SANDY BEACH (intertidal)"                                                                  "Sandy beach (intertidal)",
   "Pelagic clay"                                                                              "Pelagic clay",
   "Zostera capricorni"                                                                        "Zostera capricorni",
   "15 - 45%"                                                                                  "15-45%",
   "Mangrove"                                                                                  "Mangroves",
   "High Density Filter Feeders (Sponges & Soft Corals)"                                       "High Profile Reef",
   "Brackish lake"                                                                             "Brackish lake",
   "C. serrulata"                                                                              "Cymodocea serrulata",
   "Sessile invertebrates"                                                                     "Sessile invertebrates (SI)",
   "Sediment bottom"                                                                           "Sediment bottom",
   "subtidal T.hemprichii & green algae (sand/rubble)"                                         "subtidal T.hemprichii & green algae (sand/rubble)",
   "Halophila ovalis and Halophila uninervis"                                                  "Halophila ovalis",
   "Bare Coarse Sediment Rippled Sand"                                                         "Bare Coarse Sediment rippled sand",
   "Bare Sandy to Mixed Sediments"                                                             "Bare Sandy - Mixed Sediments",
   "Unassigned"                                                                                "Unassigned",
   "None modelled with confidence"                                                             "None modelled with certainty",
   "Z. meulleri, H. ovalis & H. uninervis"                                                     "Z. meulleri, H. ovalis & H. uninervis",
   "Saltmarsh"                                                                                 "Salt flats",
   "Backreef / shallow lagoon"                                                                 "Backreef / shallow lagoon",
   "Subtidal sand"                                                                             "Subtidal sand",
   "Fringing coral reefs"                                                                      "Fringing coral reefs",
   "Limestone"                                                                                 "Limestone",
   "Cymodocea rotundata"                                                                       "Cymodocea rotundata",
   "subtidal sparse H.uninervis(wide) with green algae (sand)"                                 "subtidal sparse H.uninervis(wide) with green algae (sand)",
   "Avicennia marina"                                                                          "Avicennia marina",
   "Rhizophora stylosa closed forest"                                                          "Rhizophora spp.",
   "Sparse Patchy Seagrass"                                                                    "Sparse Patchy Seagrass",
   "Mixed mangrove species"                                                                    "Mixed seagrass species",
   "Rocky platform"                                                                            "Rocky Reef",
   "Low-Medium Density Mixed Community (Corals, Algae, Sponges & Soft Corals)"                 "Low Profile Reef",
   "Enhalus acoroides with Halophila ovalis"                                                   "Enhalus acoroides with Halophila ovalis",
   "Avicennia marina/Bruguiera exaristata"                                                     "Avicennia marina/Bruguiera exaristata",
   "Rubble and Sand"                                                                           "Rubble and sand",
   "Intertidal, non-mobile substrates"                                                         "Intertidal, non-mobile substrates",
   "Zostera muelleri/Halophila uninervis"                                                      "Zostera muelleri",
   "Samphire-dominated Saltpan"                                                                "Samphire-dominated Saltpan",
   "Hard substrate"                                                                            "Hard substrate",
   "Rhizophora stylosa open woodland"                                                          "Rhizophora spp.",
   "Halophila ovalis with mixed species"                                                       "Halophila ovalis",
   "Aquatic Macrophytes"                                                                       "Aquatic Macrophytes",
   "Z. meulleri, H. ovalis & H. spinulosa"                                                     "Z. meulleri, H. ovalis & H. spinulosa",
   "Halodule uninervis (wide form)"                                                            "Halodule uninervis (wide)",
   "Dense H. uninervis (thin)/ T. hemprichii"                                                  "Dense H. uninervis (narrow)/ T. hemprichii",
   "Massive"                                                                                   "Massive",
   "Halophila ovalis, Halophila spinulosa and Zostera muelleri"                                "Halophila ovalis",
   "Diverse sandy"                                                                             "Diverse sandy",
   "Zostera sp."                                                                               "Zostera muelleri / Halophila uninervis",
   "Medium Density Mixed Community (Corals, Algae, Sponges & Soft Corals)"                     "Medium Profile Reef",
   "Posidonia/Zostera"                                                                         "Posidonia/Zostera",
   "Mixed seagrass and reef"                                                                   "Mixed seagrass and reef",
   "T.hemprichii in sandy pool (sand)"                                                         "T.hemprichii in sandy pool (sand)",
   "scarp"                                                                                     "Scarp",
   "Hard coral and all mixes"                                                                  "Hard coral and all mixes",
   "Rock"                                                                                      "Rock",
   "subtidal sparse H.ovalis with green algae (sand)"                                          "subtidal sparse H.ovalis with green algae (sand)",
   "Halodule uninervis (narrow form) with Enhalus acoroides"                                   "Halodule uninervis (narrow)",
   "Shallow; very low acoustic hardness; very low slope; very smooth; unconsolidated material" "Shallow, very low slope, unconsolidated",
   "Thalassodendron ciliatum"                                                                  "Thalassodendron ciliatum",
   "subtidal sparse T.hemprichii (sand)"                                                       "subtidal sparse T.hemprichii (sand)",
   "bank/shoals"                                                                               "bank/shoals",
   "MACROALGAE (subtidal)"                                                                     "Macroalgae (subtidal)",
   "Soft substrate"                                                                            "Soft substrate",
   "Reef and shoal"                                                                            "Reef and shoal",
   "Unclassified"                                                                              "Unclassified",
   "Moderate H. uninervis (thin) with mixed species"                                           "Moderate H. uninervis (narrow) with mixed species",
   "basin"                                                                                     "basin",
   "Light H. spinulosa/H. uninervis (wide)"                                                    "Light H. spinulosa/H. uninervis (wide)",
   "Macroalgae, undifferentiated"                                                              "Macroalgae, undifferentiated",
   "31-50%"                                                                                    "31-50%",
   "Hard rocky reefs"                                                                          "Hard rocky reefs",
   "T.hemprichii with H.uninervis(narrow) (sand)"                                              "T.hemprichii with H.uninervis(narrow) (sand)",
   "plain"                                                                                     "Plain",
   "Zostera"                                                                                   "Zostera muelleri / Halophila uninervis",
   "Intermittent estuary"                                                                      "Intermittent estuary",
   "T.hemprichii/H.uninervis(wide) with massive corals/clams/red algae (sand/rubble)"
   "T.hemprichii/H.uninervis(wide) with massive corals/clams/red algae (sand/rubble)",
   "Island"                                                                                    "Island",
   "Rocky shores"                                                                              "Rocky shores",
   "sparse H.uninervis (narrow) with H.ovalis and red/brown algae (sand/shell/rock)"
   "sparse H.uninervis (narrow) with H.ovalis and red/brown algae (sand/shell/rock)",
   "Low Density Mixed Community (Corals, Algae, Sponges & Soft Corals)"                        "Low Profile Reef",
   "Freshwater"                                                                                "Freshwater",
   "Vegetation"                                                                                "Vegetation",
   "Macrolgae (+ rock and sediment)"                                                           "Macroalgae (+ minor rock and sediment)",
   "Zostera muelleri and Cymodocea serrulata"                                                  "Zostera muelleri",
   "Ruppia sp."                                                                                "Ruppia sp. ± Zosteraceae",
   "Posidonia"                                                                                 "Posidonia",
   "Channel - Deep (10 - 20 metres"                                                            "Channel - Deep (10 - 20 metres)",
   "Deep Lagoon"                                                                               "Deep lagoon",
   "Ruppia"                                                                                    "Ruppia",
   "Zostera muelleri and Syringodium isoetifolium"                                             "Zostera muelleri",
   "100-1000 g/m2"                                                                             "100-1000",
   "Sand/rubble"                                                                               "Sand/rubble",
   "Closed Avicennia"                                                                          "Avicennia",
   "MACROALGAE (intertidal)"                                                                   "Macroalgae (intertidal)",
   "Bommies"                                                                                   "Bommies",
   "60 -200m"                                                                                  "60 - 200m",
   "deep/hole/valley"                                                                          "deep/hole/valley",
   "BARE REEF (subtidal)"                                                                      "Bare reef (subtidal)",
   "Algal Beds"                                                                                "Algal Beds",
   "knoll/abyssal-hills/hills/mountains/peak"                                                  "knoll/abyssal-hills/hills/mountains/peak",
   "Bioturbated mud"                                                                           "Bioturbated mud",
   "Halophila spinulosa"                                                                       "Halophila spinulosa",
   "H. ovalis"                                                                                 "Halophila ovalis",
   "Halophila"                                                                                 "Halophila",
   "Subtidal, non-mobile, below PS zone, smooth"                                               "Subtidal, non-mobile, below PS zone, smooth",
   "20 - 60m"                                                                                  "20 - 60m",
   "Patchy Reef"                                                                               "Patchy Reef",
   "BARE REEF (intertidal)"                                                                    "Bare reef (intertidal)",
   "Live Coral and Rock, reef slope"                                                           "Live coral and rock on reef slope",
   "Barrier/back-barrier"                                                                      "Barrier/back-barrier",
   "No Seagrass"                                                                               "No seagrass",
   "Lagoon sand / rubble"                                                                      "Lagoon sand / rubble",
   "Subtidal, mobile, below PS zone, smooth"                                                   "Subtidal, mobile, below PS zone, smooth",
   "Sandy lagoon (protected)"                                                                  "Sandy lagoon (protected)",
   "Rock (+ live coral), reef crest"                                                           "Rock (+ live coral) on reef crest",
   "Avicennia marina/Ceriops tagal"                                                            "Avicennia marina/Ceriops tagal/Aegialitis annulata",
   "Sponge"                                                                                    "Sponge",
   "Patchy Posidonia"                                                                          "Patchy Posidonia",
   "Shoal, Submerged"                                                                          "Shoal, submerged",
   "Mixed other algae and SI"                                                                  "Mixed vegetation and SI",
   "Mixed vegetation and SI"                                                                   "Mixed vegetation and SI",
   "Light C. serrulata"                                                                        "Light C. serrulata",
   "Non-Reef"                                                                                  "Non-Reef",
   "Mangrove/intertidal habitat"                                                               "Mangroves",
   "Zostera muelleri"                                                                          "Zostera muelleri",
   "Bioturbated sand"                                                                          "Bioturbated sand",
   "Deep; low acoustic hardness; low slope; very smooth; unconsolidated material"              "Deep, low slope, unconsolidated",
   "Macroalgae (+ rock, live and dead coral)"                                                  "Macroalgae (+ minor rock, live coral and dead coral)",
   "Riverine"                                                                                  "Riverine",
   "Patchy hard rocky reefs / exposed rock"                                                    "Patchy hard rocky reefs / exposed rock",
   "Silt"                                                                                      "Silt",
   "H. uninervis, H. ovalis & H. spinulosa"                                                    "H. uninervis, H. ovalis & H. spinulosa",
   "Low Intertidal, non-mobile, rough"                                                         "Low Profile Reef",
   "Other algae"                                                                               "Other algae",
   "Subtidal, mobile, PS zone, smooth"                                                         "Subtidal, mobile, PS zone, smooth",
   "Halophila decipiens with mixed species"                                                    "Halophila decipiens with mixed species",
   "Halophila ovalis"                                                                          "Halophila ovalis",
   "Bruguiera exaristata"                                                                      "Bruguiera exaristata",
   "Z. meulleri, H. uninervis & S. isoetifolium"                                               "Z. meulleri, H. uninervis & S. isoetifolium",
   "Tidal Sand Banks"                                                                          "Tidal Sand Banks",
   "Gravel"                                                                                    "Gravel",
   "Lumnitzera racemosa"                                                                       "Lumnitzera racemosa",
   "abyssal-plain/deep ocean floor"                                                            "abyssal-plain/deep ocean floor",
   "Mixed reef and sand"                                                                       "Mixed reef and sand",
   "25-50 g/m2"                                                                                "25-50",
   "Moderate C. serrulata with mixed species"                                                  "Moderate C. serrulata with mixed species",
   "Channel"                                                                                   "Channel",
   "> 0m"                                                                                      "< 0m (land)",
   "ridge"                                                                                     "Ridge",
   "Light H. decipiens/H. uninervis (thin)"                                                    "Light H. uninervis (narrow)",
   "MUDFLAT"                                                                                   "Mudflat",
   "sparse T.hemprichii with soft coral  (sand)"                                               "sparse T.hemprichii with soft coral (sand)",
   "Bare Rocky Reef"                                                                           "Bare Rocky Reef",
   "Mangrove/Saltmarsh"                                                                        "Mangroves",
   "Moderately shallow; high acoustic hardness; high slope; rugose; low occurrence of consolidated material"
   "Moderately shallow, high slope, low consolidation",
   "Light C. serrulata with mixed species"                                                     "Light C. serrulata with mixed species",
   "mound"                                                                                     "Mound",
   "trench/trough"                                                                             "trench/trough",
   "Cobble"                                                                                    "Cobble",
   "saddle"                                                                                    "saddle",
   "Dense H. uninervis (thin) with mixed species"                                              "Dense H. uninervis (narrow) with mixed species",
   "apron/fan"                                                                                 "apron/fan",
   "dense E.acoroides circle (sand)"                                                           "dense E.acoroides circle (sand)",
   "C. serrulata & S. isoetifolium"                                                            "C. serrulata & S. isoetifolium",
   "Mixed kelp and SI"                                                                         "Mixed vegetation and SI",
   "Eroded sandstone"                                                                          "Eroded sandstone",
   "Syringodium isoetifolium with mixed species"                                               "Syringodium isoetifolium",
   "Sand (subtidal)"                                                                           "Sand (subtidal)",
   "Rocky headland"                                                                            "Rocky headland",
   "Channel - Moderate - Inshore (5 - 10 metres)"                                              "Channel - Moderate (5 - 10 metres)",
   "Sonneratia alba"                                                                           "Sonneratia alba",
   "Branching Live Coral, reef slope"                                                          "Live branching coral on reef slope",
   "Light H. ovalis with H. uninervis (thin)"                                                  "Light H. ovalis with H. uninervis (narrow)",
   "Bruguiera parviflora/Rhizophora stylosa"                                                   "Bruguiera parviflora/Rhizophora stylosa",
   "Inshore, algae/sponge habitat"                                                             "Inshore, algal/sponge habitat",
   "Syringodium isoetifolium"                                                                  "Syringodium isoetifolium",
   "Beach"                                                                                     "Beach",
   "Dense H. uninervis (thin) with H. ovalis"                                                  "Dense H. ovalis",
   "Pavement"                                                                                  "Flat/hard low-relief bottom (pavement)",
   "Very deep; low acoustic hardness; very high slope; very rugose; unconsolidated material"   "Very deep, very high slope, unconsolidated",
   "Dense H. ovalis"                                                                           "Dense H. ovalis",
   "Calcareous ooze"                                                                           "Calcareous ooze",
   "Artificial substrate"                                                                      "Artificial substrate",
   "Posidonia/Zostera/Halophila"                                                               "Posidonia/Zostera/Halophila",
   "Posidonia/Halophila"                                                                       "Posidonia/Halophila",
   "10-40 %"                                                                                   "10-40%",
   "Mud and tidal flats"                                                                       "Mud and tidal flats",
   "plateau"                                                                                   "plateau",
   "Rhizophora stylosa/Bruguiera spp./Ceriops spp."                                            "Rhizophora spp.",
   "Thalassia hemprichii"                                                                      "Thalassia hemprichii",
   "Halophila ovalis with Thalassia hemprichii"                                                "Halophila ovalis",
   "Bedrock"                                                                                   "Bedrock",
   "Reef Flat Outer"                                                                           "Reef flat (outer)",
   "Rubble+Sand"                                                                               "Rubble and sand",
   "Halophila/Ruppia"                                                                          "Halophila sp. ± Ruppia sp., Zostera sp.",
   "40-70 %"                                                                                   "40-70%",
   "subtidal H.tricostata/H.decipiens - dense in patches (sand/mud)"                           "subtidal H.tricostata/H.decipiens - dense in patches (sand/mud)",
   "Large Rock"                                                                                "Large rock",
   "Marine"                                                                                    "Marine",
   "Other"                                                                                     "Other",
   "Coastline - Sand"                                                                          "Coastline - Sand",
   "H.uninervis(wide)/T.hemprichii (dense patches) with H.ovalis & algae (sand)"
   "H.uninervis(wide)/T.hemprichii (dense patches) with H.ovalis & algae (sand)",
   "subtidal H.decipiens/H.ovalis (sand/mud)"                                                  "subtidal H.decipiens/H.ovalis (sand/mud)",
   "CORAL REEF (subtidal)"                                                                     "Coral reef (subtidal)",
   "Halophila tricostata"                                                                      "Halophila tricostata",
   "Saltmarsh/Mangrove"                                                                        "Salt flats",
   "1-10 %"                                                                                    "1-10%",
   "Sparse Seagrass"                                                                           "Sparse Seagrass",
   "51-75%"                                                                                    "51-75%",
   "Unconsolidated Bare Substrate"                                                             "Unconsolidated bare substrate",
   "Pavement reef"                                                                             "Pavement reef",
   "Mixed sandy bottom"                                                                        "Mixed seagrass species",
   "Zostera muelleri and Halophila uninervis"                                                  "Zostera muelleri",
   "Reef, Tidal"                                                                               "Reef, tidal",
   "bank"                                                                                      "Bank",
   "Kelp"                                                                                      "Kelp",
   "Fluvial (bay-head) Delta"                                                                  "Fluvial (bay-head) Delta",
   "Halophila sp./Zostera sp."                                                                 "Halophila sp. ± Ruppia sp., Zostera sp.",
   "Light H. uninervis (wide) with mixed species"                                              "Light H. uninervis (wide) with mixed species",
   "Halophila ovalis/Halodule uninervis (narrow form)"                                         "Halophila ovalis",
   "Halophila capricorni"                                                                      "Halophila capricorni",
   "Aegiceras corniculatum"                                                                    "Aegiceras corniculatum",
   "Zostera/Halophila"                                                                         "Zostera muelleri / Halophila uninervis",
   "Cay"                                                                                       "Cay",
   "Closed Mixed"                                                                              "Mixed",
   "SANDSHOAL"                                                                                 "Sandshoal",
   "Reef flat Inner"                                                                           "Reef flat (inner)",
   "Closed Rhizophora/Aegiceras"                                                               "Rhizophora/Aegiceras",
   "Zostera/Ruppia"                                                                            "Zostera muelleri / Halophila uninervis",
   "High Profile Reef"                                                                         "High Profile Reef",
   "Halodule uninervis (narrow)"                                                               "Halodule uninervis (narrow)",
   "Salt marsh"                                                                                "Salt flats",
   "Sand (intertidal)"                                                                         "Sand (intertidal)",
   "patches"                                                                                   "patches",
   "Deep Reef Structures"                                                                      "Deep reef structures",
   "Inshore reef"                                                                              "Inshore reef",
   "76-100%"                                                                                   "76-100%",
   "Mixed reef, gravel and sand"                                                               "Mixed gravel, reef and sand",
   "Non-mangrove vegetation communities"                                                       "Non-mangrove vegetation communities",
   "CORAL REEF (intertidal)"                                                                   "Coral reef (intertidal)",
   "Muddy bottom"                                                                              "Muddy bottom",
   "Halophila uninveris and Cymodocea serrulata"                                               "Halophila uninveris & Cymodocea serrulata",
   "15 - 45% Pat"                                                                              "15-45%",
   "Bruguiera parviflora"                                                                      "Bruguiera parviflora",
   "Volcanic sand and grit"                                                                    "Volcanic sand and grit",
   "MANGALS"                                                                                   "Mangals",
   "Unidentified"                                                                              "Unidentified seagrass"})
