(ns imas-seamap.plot.colours)

;;; Confession: the easiest way to generate a bunch of interpolated
;;; colour values was
;;; http://plottablejs.org/components/legends/interpolated-color/,
;;; opening up a console and calling range() and domain() on
;;; colorScale, then iterating over an array to generate the values:
;;;
;;; colorScale.domain([0,512])
;;; colorScale.range(["#1F4B99","#9E2B0E"])
;;; copy( [...Array(512).keys()].map((i) => colorScale.scale(i)) )

(def ^:private -colours
  ["#1f4b99",
   "#204b99",
   "#214b98",
   "#224b98",
   "#234b98",
   "#244b98",
   "#254b97",
   "#264b97",
   "#274b97",
   "#284b97",
   "#294a96",
   "#2a4a96",
   "#2b4a96",
   "#2c4a95",
   "#2d4a95",
   "#2d4a95",
   "#2e4a95",
   "#2f4a94",
   "#304a94",
   "#304a94",
   "#314a94",
   "#324a93",
   "#334a93",
   "#334a93",
   "#344a92",
   "#354a92",
   "#354a92",
   "#364a92",
   "#374a91",
   "#374991",
   "#384991",
   "#394991",
   "#394990",
   "#3a4990",
   "#3a4990",
   "#3b498f",
   "#3b498f",
   "#3c498f",
   "#3d498f",
   "#3d498e",
   "#3e498e",
   "#3e498e",
   "#3f498e",
   "#3f498d",
   "#40498d",
   "#40498d",
   "#41498d",
   "#41498c",
   "#42498c",
   "#42488c",
   "#43488b",
   "#43488b",
   "#44488b",
   "#44488b",
   "#45488a",
   "#45488a",
   "#46488a",
   "#46488a",
   "#474889",
   "#474889",
   "#484889",
   "#484888",
   "#484888",
   "#494888",
   "#494888",
   "#4a4887",
   "#4a4887",
   "#4b4887",
   "#4b4787",
   "#4b4786",
   "#4c4786",
   "#4c4786",
   "#4d4786",
   "#4d4785",
   "#4d4785",
   "#4e4785",
   "#4e4784",
   "#4f4784",
   "#4f4784",
   "#4f4784",
   "#504783",
   "#504783",
   "#514783",
   "#514783",
   "#514782",
   "#524782",
   "#524782",
   "#524682",
   "#534681",
   "#534681",
   "#544681",
   "#544680",
   "#544680",
   "#554680",
   "#554680",
   "#55467f",
   "#56467f",
   "#56467f",
   "#56467f",
   "#57467e",
   "#57467e",
   "#57467e",
   "#58467e",
   "#58467d",
   "#58467d",
   "#59467d",
   "#59457c",
   "#59457c",
   "#5a457c",
   "#5a457c",
   "#5a457b",
   "#5b457b",
   "#5b457b",
   "#5b457b",
   "#5b457a",
   "#5c457a",
   "#5c457a",
   "#5c457a",
   "#5d4579",
   "#5d4579",
   "#5d4579",
   "#5e4578",
   "#5e4578",
   "#5e4578",
   "#5f4478",
   "#5f4477",
   "#5f4477",
   "#5f4477",
   "#604477",
   "#604476",
   "#604476",
   "#614476",
   "#614476",
   "#614475",
   "#614475",
   "#624475",
   "#624475",
   "#624474",
   "#624474",
   "#634474",
   "#634473",
   "#634473",
   "#644473",
   "#644373",
   "#644372",
   "#644372",
   "#654372",
   "#654372",
   "#654371",
   "#654371",
   "#664371",
   "#664371",
   "#664370",
   "#664370",
   "#674370",
   "#674370",
   "#67436f",
   "#67436f",
   "#68436f",
   "#68436e",
   "#68436e",
   "#68426e",
   "#69426e",
   "#69426d",
   "#69426d",
   "#69426d",
   "#6a426d",
   "#6a426c",
   "#6a426c",
   "#6a426c",
   "#6b426c",
   "#6b426b",
   "#6b426b",
   "#6b426b",
   "#6c426b",
   "#6c426a",
   "#6c426a",
   "#6c426a",
   "#6d4269",
   "#6d4169",
   "#6d4169",
   "#6d4169",
   "#6d4168",
   "#6e4168",
   "#6e4168",
   "#6e4168",
   "#6e4167",
   "#6f4167",
   "#6f4167",
   "#6f4167",
   "#6f4166",
   "#6f4166",
   "#704166",
   "#704166",
   "#704165",
   "#704165",
   "#714165",
   "#714065",
   "#714064",
   "#714064",
   "#714064",
   "#724063",
   "#724063",
   "#724063",
   "#724063",
   "#724062",
   "#734062",
   "#734062",
   "#734062",
   "#734061",
   "#734061",
   "#744061",
   "#744061",
   "#744060",
   "#744060",
   "#743f60",
   "#753f60",
   "#753f5f",
   "#753f5f",
   "#753f5f",
   "#753f5f",
   "#763f5e",
   "#763f5e",
   "#763f5e",
   "#763f5e",
   "#763f5d",
   "#773f5d",
   "#773f5d",
   "#773f5c",
   "#773f5c",
   "#773f5c",
   "#783f5c",
   "#783e5b",
   "#783e5b",
   "#783e5b",
   "#783e5b",
   "#793e5a",
   "#793e5a",
   "#793e5a",
   "#793e5a",
   "#793e59",
   "#793e59",
   "#7a3e59",
   "#7a3e59",
   "#7a3e58",
   "#7a3e58",
   "#7a3e58",
   "#7b3e58",
   "#7b3e57",
   "#7b3e57",
   "#7b3d57",
   "#7b3d57",
   "#7b3d56",
   "#7c3d56",
   "#7c3d56",
   "#7c3d56",
   "#7c3d55",
   "#7c3d55",
   "#7d3d55",
   "#7d3d54",
   "#7d3d54",
   "#7d3d54",
   "#7d3d54",
   "#7d3d53",
   "#7e3d53",
   "#7e3d53",
   "#7e3c53",
   "#7e3c52",
   "#7e3c52",
   "#7e3c52",
   "#7f3c52",
   "#7f3c51",
   "#7f3c51",
   "#7f3c51",
   "#7f3c51",
   "#7f3c50",
   "#803c50",
   "#803c50",
   "#803c50",
   "#803c4f",
   "#803c4f",
   "#803c4f",
   "#813c4f",
   "#813b4e",
   "#813b4e",
   "#813b4e",
   "#813b4e",
   "#813b4d",
   "#823b4d",
   "#823b4d",
   "#823b4c",
   "#823b4c",
   "#823b4c",
   "#823b4c",
   "#833b4b",
   "#833b4b",
   "#833b4b",
   "#833b4b",
   "#833b4a",
   "#833a4a",
   "#833a4a",
   "#843a4a",
   "#843a49",
   "#843a49",
   "#843a49",
   "#843a49",
   "#843a48",
   "#853a48",
   "#853a48",
   "#853a48",
   "#853a47",
   "#853a47",
   "#853a47",
   "#853a47",
   "#863a46",
   "#863a46",
   "#863946",
   "#863946",
   "#863945",
   "#863945",
   "#873945",
   "#873945",
   "#873944",
   "#873944",
   "#873944",
   "#873943",
   "#873943",
   "#883943",
   "#883943",
   "#883942",
   "#883942",
   "#883842",
   "#883842",
   "#883841",
   "#893841",
   "#893841",
   "#893841",
   "#893840",
   "#893840",
   "#893840",
   "#893840",
   "#8a383f",
   "#8a383f",
   "#8a383f",
   "#8a383f",
   "#8a383e",
   "#8a383e",
   "#8a373e",
   "#8b373e",
   "#8b373d",
   "#8b373d",
   "#8b373d",
   "#8b373d",
   "#8b373c",
   "#8b373c",
   "#8b373c",
   "#8c373b",
   "#8c373b",
   "#8c373b",
   "#8c373b",
   "#8c373a",
   "#8c373a",
   "#8c363a",
   "#8d363a",
   "#8d3639",
   "#8d3639",
   "#8d3639",
   "#8d3639",
   "#8d3638",
   "#8d3638",
   "#8e3638",
   "#8e3638",
   "#8e3637",
   "#8e3637",
   "#8e3637",
   "#8e3637",
   "#8e3636",
   "#8e3536",
   "#8f3536",
   "#8f3535",
   "#8f3535",
   "#8f3535",
   "#8f3535",
   "#8f3534",
   "#8f3534",
   "#8f3534",
   "#903534",
   "#903533",
   "#903533",
   "#903533",
   "#903533",
   "#903432",
   "#903432",
   "#903432",
   "#913432",
   "#913431",
   "#913431",
   "#913431",
   "#913430",
   "#913430",
   "#913430",
   "#913430",
   "#92342f",
   "#92342f",
   "#92342f",
   "#92332f",
   "#92332e",
   "#92332e",
   "#92332e",
   "#92332e",
   "#93332d",
   "#93332d",
   "#93332d",
   "#93332c",
   "#93332c",
   "#93332c",
   "#93332c",
   "#93332b",
   "#94332b",
   "#94322b",
   "#94322b",
   "#94322a",
   "#94322a",
   "#94322a",
   "#943229",
   "#943229",
   "#953229",
   "#953229",
   "#953228",
   "#953228",
   "#953228",
   "#953228",
   "#953227",
   "#953127",
   "#953127",
   "#963126",
   "#963126",
   "#963126",
   "#963126",
   "#963125",
   "#963125",
   "#963125",
   "#963125",
   "#963124",
   "#973124",
   "#973124",
   "#973023",
   "#973023",
   "#973023",
   "#973023",
   "#973022",
   "#973022",
   "#983022",
   "#983021",
   "#983021",
   "#983021",
   "#983021",
   "#983020",
   "#983020",
   "#982f20",
   "#982f1f",
   "#992f1f",
   "#992f1f",
   "#992f1e",
   "#992f1e",
   "#992f1e",
   "#992f1e",
   "#992f1d",
   "#992f1d",
   "#992f1d",
   "#9a2f1c",
   "#9a2e1c",
   "#9a2e1c",
   "#9a2e1b",
   "#9a2e1b",
   "#9a2e1b",
   "#9a2e1b",
   "#9a2e1a",
   "#9a2e1a",
   "#9b2e1a",
   "#9b2e19",
   "#9b2e19",
   "#9b2e19",
   "#9b2d18",
   "#9b2d18",
   "#9b2d18",
   "#9b2d17",
   "#9b2d17",
   "#9b2d17",
   "#9c2d16",
   "#9c2d16",
   "#9c2d16",
   "#9c2d15",
   "#9c2d15",
   "#9c2d15",
   "#9c2c14",
   "#9c2c14",
   "#9c2c14",
   "#9d2c13",
   "#9d2c13",
   "#9d2c13",
   "#9d2c12",
   "#9d2c12",
   "#9d2c11",
   "#9d2c11",
   "#9d2c11",
   "#9d2c10",
   "#9d2b10",
   "#9e2b10",
   "#9e2b0f",
   "#9e2b0f",
   "#9e2b0e"])

(def ^:private -habitats
  ["< 15%"
   "< 2%"
   "< 25 g/m2"
   "> 0m"
   "> 1000 g/m2"
   "> 200m"
   "> 75%"
   "> 75% Patchy"
   "0"
   "0 - 20m"
   "0%"
   "0-10%"
   "100-1000 g/m2"
   "10-40 %"
   "1-10 %"
   "1-10%"
   "11-30%"
   "15 - 45%"
   "15 - 45% Pat"
   "2 - 15%"
   "20 - 60m"
   "25-50 g/m2"
   "31-50%"
   "40-70 %"
   "45 - 75%"
   "50-75 g/m2"
   "51-75%"
   "60 -200m"
   "70-100%"
   "75-100 g/m2"
   "76-100%"
   "A. corniculatum/A. marina"
   "A.corniculatum/A.marina/Saltmars"
   "A.corniculatum/Saltmarsh Comm."
   "abyssal-plain/deep ocean floor"
   "Aegiceras corniculatum"
   "Algal Beds"
   "Algal mat"
   "ALGAL MAT (subtidal)"
   "apron/fan"
   "Aquatic Macrophytes"
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
   "Avicennia marina/Bruguiera exaristata"
   "Avicennia marina/Ceriops tagal"
   "Avicennia marina/Ceriops tagal/Aegialitis annulata"
   "Backreef / shallow lagoon"
   "bank"
   "bank/shoals"
   "Bare Coarse Sediment Rippled Sand"
   "Bare Coarse Sediment with possibility small rocky outcrops/rippled sand"
   "BARE REEF (intertidal)"
   "BARE REEF (subtidal)"
   "Bare Rocky Reef"
   "Bare Sand - Mud (either flats/bars)"
   "Bare Sandy to Mixed Sediments"
   "Bare Subtrate"
   "Barrier/back-barrier"
   "basin"
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
   "Branching"
   "Branching Live Coral, reef slope"
   "Bruguiera exaristata"
   "Bruguiera parviflora"
   "Bruguiera parviflora/Rhizophora stylosa"
   "C. serrulata"
   "C. serrulata & S. isoetifolium"
   "Calcareous gravel, sand and silt"
   "Calcareous ooze"
   "canyon"
   "Caulerpa"
   "Caulerpa & H. ovalis"
   "Cay"
   "Central Basin"
   "Ceriops tagal"
   "Channel"
   "Channel - Deep (10 - 20 metres"
   "Channel - Moderate - Inshore (5 - 10 metres)"
   "Channel - Moderate (5 - 10 metres)"
   "Channel - Shallow (<5 metres)"
   "Closed Aegiceras"
   "Closed Avicennia"
   "Closed Avicennia/Aegiceras"
   "Closed Avicennia/Ceriops"
   "Closed Bruguiera"
   "Closed Ceriops"
   "Closed Mixed"
   "Closed Rhizophora"
   "Closed Rhizophora/Aegiceras"
   "Closed Rhizophora/Avicennia"
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
   "Coastline - Sand"
   "Cobble"
   "continental-rise"
   "Coral"
   "Coral communities"
   "CORAL REEF (intertidal)"
   "CORAL REEF (subtidal)"
   "Coralline algae"
   "Cultural features"
   "Cymodocea rotundata"
   "Cymodocea serrulata"
   "Cymodocea serrulata and Zostera muelleri"
   "Deep Areas"
   "Deep Lagoon"
   "Deep reef flat"
   "Deep Reef Structures"
   "deep/hole/valley"
   "Deep; low acoustic hardness; low slope; very smooth; unconsolidated material"
   "dense E.acoroides circle (sand)"
   "Dense H. ovalis"
   "Dense H. uninervis (thin) with H. ovalis"
   "Dense H. uninervis (thin) with mixed species"
   "Dense H. uninervis (thin)/ T. hemprichii"
   "dense T.hemprichii with H.ovalis & massive coral (sand/shell/rubble)"
   "dense T.hemprichii with occassional H.uninervis(wide)/H.ovalis (coarse sand/shell/rubble)"
   "depression"
   "Diverse sandy"
   "Dredged"
   "Dune vegetation, undifferentiated"
   "Embayment - Subtidal Zone"
   "Enhalus acoroides"
   "Enhalus acoroides with Halophila ovalis"
   "Enhalus acoroides with mixed species"
   "Enhalus acoroides/Halodule uninervis (wide form)"
   "Eroded sandstone"
   "escarpment"
   "Estuarine - Mangroves and related tree communities"
   "Estuarine - salt flats and saltmarshes"
   "Estuarine - water"
   "Exposed Rock"
   "Filter feeders"
   "FILTER FEEDERS (subtidal)"
   "Fine sediment"
   "Flood- and Ebb-tide Delta"
   "Fluvial (bay-head) Delta"
   "Freshwater"
   "Fringing coral reefs"
   "Grass, succulent"
   "Gravel"
   "GRAVEL/RUBBLE (subtidal)"
   "H. ovalis"
   "H. ovalis & H. spinulosa"
   "H. ovalis & Z. muelleri"
   "H. ovalis, H. spinulosa & H. uninervis"
   "H. ovalis, Z. meulleri & H. spinulosa"
   "H. ovalis, Z. meulleri & S. isoetifolium"
   "H. ovalis, Z. meulleri, H. spinulosa & S. isoetifolium"
   "H. ovalis, Z. muelleri, C. serrulata & S. isoetifolium"
   "H. spinulosa"
   "H. uninervis & H. ovalis"
   "H. uninervis, H. ovalis & H. spinulosa"
   "H.uninervis(wide)/T.hemprichii (dense patches) with H.ovalis & algae (sand)"
   "Halodule uninervis"
   "Halodule uninervis (narrow form)"
   "Halodule uninervis (narrow form) with Enhalus acoroides"
   "Halodule uninervis (narrow form) with mixed species"
   "Halodule uninervis (narrow form)/Halophila decipiens"
   "Halodule uninervis (narrow form)/Halophila ovalis"
   "Halodule uninervis (narrow form)/Halophila ovalis with mixed species"
   "Halodule uninervis (narrow)"
   "Halodule uninervis (wide form)"
   "Halodule uninervis (wide)"
   "Halophila"
   "Halophila capricorni"
   "Halophila decipiens"
   "Halophila decipiens with mixed species"
   "Halophila ovalis"
   "Halophila ovalis and Halophila uninervis"
   "Halophila ovalis with Enhalus acoroides"
   "Halophila ovalis with Halodule uninervis (narrow form)"
   "Halophila ovalis with mixed species"
   "Halophila ovalis with Thalassia hemprichii"
   "Halophila ovalis, Halophila spinulosa and Zostera muelleri"
   "Halophila ovalis/Halodule uninervis (narrow form)"
   "Halophila ovalis/Halophila decipiens"
   "Halophila sp."
   "Halophila sp./Ruppia sp."
   "Halophila sp./Zostera sp."
   "Halophila spinulosa"
   "Halophila spinulosa and Halophila uninervis"
   "Halophila tricostata"
   "Halophila uninveris and Cymodocea serrulata"
   "Halophila/Ruppia"
   "Hard"
   "Hard coral and all mixes"
   "Hard rocky reefs"
   "Hard Sand"
   "Hard substrate"
   "High Density Filter Feeders (Sponges & Soft Corals)"
   "High Density Mixed Community (Corals, Algae, Sponges & Soft Corals)"
   "High energy coastal"
   "High Profile Reef"
   "Hind dune forest"
   "Inshore reef"
   "Inshore, algae/sponge habitat"
   "Intermittent estuary"
   "Intertidal Flats"
   "Intertidal mobile substrates"
   "Intertidal, non-mobile substrates"
   "Invertebrate Community"
   "Island"
   "Islands and rocks"
   "Kelp"
   "knoll/abyssal-hills/hills/mountains/peak"
   "Lagoon sand / algae"
   "Lagoon sand / rubble"
   "Land"
   "Land mass"
   "Large Rock"
   "Light C. rotundata"
   "Light C. serrulata"
   "Light C. serrulata with mixed species"
   "Light H. decipiens"
   "Light H. decipiens/H. uninervis (thin)"
   "Light H. ovalis with H. uninervis (thin)"
   "Light H. spinulosa/H. uninervis (wide)"
   "Light H. uninervis (thin)"
   "Light H. uninervis (wide) with mixed species"
   "Light H. uninervis with H. ovalis"
   "Limestone"
   "Live and Dead Coral"
   "Live Coral (+ dead coral)"
   "Live Coral and Rock, reef slope"
   "Low Density Mixed Community (Corals, Algae, Sponges & Soft Corals)"
   "Low Intertidal, non-mobile, rough"
   "Low Intertidal, non-mobile, smooth"
   "Low Profile Reef"
   "Low-Medium Density Mixed Community (Corals, Algae, Sponges & Soft Corals)"
   "Lumnitzera racemosa"
   "Macroalgae"
   "Macroalgae (+ rock, live and dead coral)"
   "MACROALGAE (intertidal)"
   "MACROALGAE (subtidal)"
   "Macroalgae, undifferentiated"
   "Macrolgae (+ rock and sediment)"
   "Mainland"
   "MANGALS"
   "Mangrove"
   "Mangrove/intertidal habitat"
   "Mangrove/Saltmarsh"
   "Mangrove/Saltmarsh Communities"
   "Mangroves"
   "Marine"
   "Massive"
   "Massive/Soft"
   "Massive/Soft/Sponge/Branching"
   "Massive/Sponge/Plate/Branching"
   "Medium Density Filter Feeders (Sponges & Soft Corals)"
   "Medium Density Mixed Community (Corals, Algae, Sponges & Soft Corals)"
   "Medium Profile Reef"
   "Mixed gravel and sand"
   "Mixed kelp and other algae"
   "Mixed kelp and SI"
   "Mixed mangrove species"
   "Mixed other algae and SI"
   "Mixed reef and gravel"
   "Mixed reef and sand"
   "Mixed reef, gravel and sand"
   "Mixed reef/sediment"
   "Mixed sandy bottom"
   "Mixed seagrass and reef"
   "Mixed seagrass species"
   "Mixed vegetation"
   "Mixed vegetation and SI"
   "MOBILE SAND (subtidal)"
   "Moderate C. serrulata with mixed species"
   "Moderate C. serrulata/H. uninervis (wide)"
   "Moderate H. uninervis (thin)"
   "Moderate H. uninervis (thin) with H. ovalis"
   "Moderate H. uninervis (thin) with H. uninervis (wide)"
   "Moderate H. uninervis (thin) with mixed species"
   "Moderate T. hemprichii with mixed species"
   "Moderately deep; moderately high acoustic hardness; low slope; moderately smooth; unconsolidated material"
   "Moderately shallow; high acoustic hardness; high slope; rugose; low occurrence of consolidated material"
   "mound"
   "Mud and calcareous clay"
   "Mud and sand"
   "Mud and tidal flats"
   "Muddy bottom"
   "MUDFLAT"
   "n/a"
   "Nearshore reef"
   "Nearshore Waters (< 5 metres)"
   "No Seagrass"
   "None"
   "None modelled with certainty"
   "None modelled with confidence"
   "Non-mangrove vegetation communities"
   "Non-Reef"
   "Not Recorded"
   "Null"
   "Ocean embayment"
   "Offshore deep"
   "Offshore reef"
   "Offshore sandy"
   "Offshore Waters (> 20 metres)"
   "Offshore Waters (10  - 20 metres)"
   "Offshore Waters (5 - 10 metres)"
   "Offshore waters < 5 metres (island, shoal)"
   "Offshore waters 5-10 metres (island, shoal)"
   "Open Avicennia"
   "Open Avicennia/Ceriops"
   "Open Bruguiera"
   "Open Ceriops"
   "Other"
   "Other algae"
   "P. australis/Halophila sp."
   "P. australis/Zostera sp."
   "patches"
   "Patchy hard rocky reefs / exposed rock"
   "Patchy Posidonia"
   "Patchy Reef"
   "Patchy Seagrass"
   "Pavement"
   "Pavement reef"
   "PELAGIC"
   "Pelagic clay"
   "pinnacle"
   "plain"
   "plateau"
   "Plunging cliff"
   "Posidona australis"
   "Posidonia"
   "Posidonia/Halophila"
   "Posidonia/Halophila/Ruppia"
   "Posidonia/Ruppia"
   "Posidonia/Zostera"
   "Posidonia/Zostera/Halophila"
   "reef"
   "Reef and shoal"
   "Reef Crest"
   "Reef flat"
   "Reef flat Inner"
   "Reef Flat Outer"
   "Reef Slope"
   "Reef, Submerged"
   "Reef, Tidal"
   "Rhizophora spp."
   "Rhizophora stylosa closed forest"
   "Rhizophora stylosa open woodland"
   "Rhizophora stylosa/Bruguiera spp./Ceriops spp."
   "Rhodoliths"
   "ridge"
   "Riverine"
   "Riverine/estuarine"
   "Roads"
   "Rock"
   "Rock (+ live coral), reef crest"
   "Rock wall"
   "Rocky headland"
   "Rocky intertidal"
   "Rocky platform"
   "Rocky Reef"
   "Rocky shores"
   "Rubble and Sand"
   "Rubble+Sand"
   "Ruppia"
   "Ruppia sp."
   "Ruppia sp./Zosteraceae"
   "saddle"
   "Saline Grassland"
   "Salt flats"
   "Salt marsh"
   "Saltmarsh"
   "Saltmarsh comm./A. marina"
   "Saltmarsh Communities"
   "Saltmarsh/Mangrove"
   "Saltmarsh/Saltflat"
   "Saltpan"
   "Samphire-dominated Saltpan"
   "Sand"
   "Sand (intertidal)"
   "Sand (subtidal)"
   "Sand and bolders"
   "Sand, silt and gravel with less than 50% mud"
   "Sand/rubble"
   "SANDSHOAL"
   "SANDY BEACH (intertidal)"
   "Sandy channels"
   "Sandy intermediate"
   "Sandy lagoon (protected)"
   "scarp"
   "Seagrass"
   "seamount/guyot"
   "Seawall"
   "Sedge, shrub land"
   "Sediment"
   "Sediment bottom"
   "Sessile invertebrates"
   "Sessile invertebrates (SI)"
   "Shallow island fringe"
   "Shallow Lagoon"
   "Shallow; very low acoustic hardness; very low slope; very smooth; unconsolidated material"
   "shelf"
   "Shipwreck"
   "Shoal, Submerged"
   "Shoal, Tidal"
   "sill"
   "Silt"
   "Silty Sand"
   "slope"
   "Sloping rocky bottom"
   "Sloping sandy bottom"
   "Soft"
   "Soft bedrock"
   "Soft substrate"
   "Sonneratia alba"
   "Sonneratia alba/Aegiceras corniculatum"
   "Sonneratia alba/Rhizophora stylosa/Avicennia marina"
   "sparse H.uninervis (narrow) with H.ovalis and red/brown algae (sand/shell/rock)"
   "sparse H.uninervis(wide)/T.hemprichii (dense patches) with algae (sand)"
   "Sparse Patchy Seagrass"
   "Sparse Seagrass"
   "sparse T.hemprichii with soft coral  (sand)"
   "sparse T.hemprichii/H.ovalis with algae (sand)"
   "Sponge"
   "subtidal H.decipiens/H.ovalis (sand/mud)"
   "subtidal H.ovalis/H.uninervis(wide) - dense in patches (sand)"
   "subtidal H.ovalis/H.uninervis(wide)/C.serrulata (sand)"
   "subtidal H.tricostata/H.decipiens - dense in patches (sand/mud)"
   "Subtidal sand"
   "subtidal sparse H.capricorni (coarse sand)"
   "subtidal sparse H.ovalis with green algae (sand)"
   "subtidal sparse H.uninervis(wide) (dense patches) with H.ovalis & green algae (sand)"
   "subtidal sparse H.uninervis(wide) with green algae (sand)"
   "subtidal sparse Halodule uninervis (dense patches) with H.ovalis/green algae (sand)"
   "subtidal sparse T.hemprichii (sand)"
   "subtidal T.hemprichii & green algae (sand/rubble)"
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
   "T.hemprichii in sandy pool (sand)"
   "T.hemprichii with H.uninervis(narrow) (sand)"
   "T.hemprichii/H.uninervis(wide) and green algae/Halophila ovalis (coarse sand/rubble/shell)"
   "T.hemprichii/H.uninervis(wide) with massive corals/clams/red algae (sand/rubble)"
   "terrace"
   "Thalassadendron ciliatum with Thalassia hemprichii"
   "Thalassia hemprichii"
   "Thalassia hemprichii with Enhalus acoroides"
   "Thalassia hemprichii with Halophila ovalis"
   "Thalassia hemprichii with mixed species"
   "Thalassodendron ciliatum"
   "Tidal channel (subtidal)"
   "Tidal Sand Banks"
   "tidal-sandwave/sand-bank"
   "Tide dominated estuary"
   "Transition"
   "trench/trough"
   "Unassigned"
   "UNASSIGNED Palustrine or Lacustrine"
   "Unclassified"
   "Unconsolidated Bare Substrate"
   "Unidentified"
   "Unknown"
   "Unknown / inacessible mangrove species"
   "unmapped"
   "Unvegetated"
   "valley"
   "Vegetated area"
   "Vegetated Unconsolidated"
   "Vegetation"
   "Very deep; low acoustic hardness; very high slope; very rugose; unconsolidated material"
   "Very shallow; very high acoustic hardness; very low slope; smooth; high occurrence of consolidated material"
   "Volcanic sand and grit"
   "Water and terrestrial"
   "Wave dominated estuary"
   "Wrack"
   "Z. meulleri"
   "Z. meulleri & C. serrulata"
   "Z. meulleri & H. ovalis"
   "Z. meulleri & H. spinulosa"
   "Z. meulleri & H. uninervis"
   "Z. meulleri, H. ovalis & C. serrulata"
   "Z. meulleri, H. ovalis & H. spinulosa"
   "Z. meulleri, H. ovalis & H. uninervis"
   "Z. meulleri, H. ovalis & S. isoetifolium"
   "Z. meulleri, H. ovalis, C. serrulata & S. isoetifolium"
   "Z. meulleri, H. uninervis & S. isoetifolium"
   "Zostera"
   "Zostera capricorni"
   "Zostera muelleri"
   "Zostera muelleri and Cymodocea serrulata"
   "Zostera muelleri and Halophila ovalis"
   "Zostera muelleri and Halophila uninervis"
   "Zostera muelleri and Syringodium isoetifolium"
   "Zostera muelleri/Halophila uninervis"
   "Zostera sp."
   "Zostera sp./Ruppia sp."
   "Zostera/Halophila"
   "Zostera/Halophila/Ruppia"
   "Zostera/Ruppia"
   "Zosteraceae/Ruppia/Halophila spp"])

(def *habitat-colours*
  "Mapping the habitat zones, keyed by strings obtained from the
  SM_HAB_CLS column in the different habitat tables."
  (into {} (map vector -habitats -colours)))

(def *habitat-titles*
  "Maps habitat classifications to their titles, as they appear in the
  legend (often, but not always, the same as the SM_HAB_CLS value)."
  {})
