<?php get_header(); ?>

<?php
    $habitat_statistics_url_base = get_post_meta(get_the_ID(), 'habitat_statistics_url_base', true);
    $bathymetry_statistics_url_base = get_post_meta(get_the_ID(), 'bathymetry_statistics_url_base', true);
    $habitat_observations_url_base = get_post_meta(get_the_ID(), 'habitat_observations_url_base', true);
    $research_effort_url_base = get_post_meta(get_the_ID(), 'research_effort_url_base', true);
    $region_report_data_url_base = get_post_meta(get_the_ID(), 'region_report_data_url_base', true);

    $network_name = get_post_meta(get_the_ID(), 'network_name', true);
    $park_name = get_post_meta(get_the_ID(), 'park_name', true);
    $region_name = empty($park_name) ? "$network_name network" : "$park_name park";
?>

<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">
<link rel="stylesheet" href="https://unpkg.com/leaflet@1.8.0/dist/leaflet.css" />

<script src="https://unpkg.com/vega@5.22.1/build/vega.js"></script>
<script src="https://unpkg.com/vega-lite@5.2.0/build/vega-lite.js"></script>
<script src="https://www.unpkg.com/vega-embed@6.21.0/build/vega-embed.js"></script>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="https://unpkg.com/leaflet@1.8.0/dist/leaflet.js"></script>

<script>
    function starRating(element, value, total, text) {
        element.classList.add("region-report-star-rating")

        // Stars
        const stars = document.createElement("div");

        const fullStars = Math.floor(value / 2);
        const halfStars = value % 2;
        const emptyStars = Math.floor((total - value) / 2);

        for (let i = 0; i < fullStars; i++) {
            const star = document.createElement("i");
            star.className = "fa fa-star";
            stars.appendChild(star);
        }

        for (let i = 0; i < halfStars; i++) {
            const star = document.createElement("i");
            star.className = "fa fa-star-half-o";
            stars.appendChild(star);
        }

        for (let i = 0; i < emptyStars; i++) {
            const star = document.createElement("i");
            star.className = "fa fa-star-o";
            stars.appendChild(star);
        }

        element.appendChild(stars);

        // Label
        const label = document.createElement("div");
        label.innerText = text;
        element.appendChild(label);
    }
</script>

<article id="post-<?php the_ID(); ?>" <?php post_class(); ?>>
    <script>
        let postId = "<?php the_ID(); ?>";
        let postElement = document.getElementById(`post-${postId}`);

        let habitatStatisticsUrlBase = "<?php echo $habitat_statistics_url_base; ?>";
        let bathymetryStatisticsUrlBase = "<?php echo $bathymetry_statistics_url_base; ?>";
        let habitatObservationsUrlBase = "<?php echo $habitat_observations_url_base; ?>";
        let researchEffortUrlBase = "<?php echo $research_effort_url_base; ?>";
        let regionReportDataUrlBase = "<?php echo $region_report_data_url_base; ?>";

        let networkName = "<?php echo $network_name; ?>";
        let parkName = <?php echo empty($park_name) ? 'null' : "\"$park_name\""; ?>;

        let habitatStatisticsUrl = `${habitatStatisticsUrlBase}?boundary-type=amp&network=${networkName}&park=${parkName ?? ""}`;
        let bathymetryStatisticsUrl = `${bathymetryStatisticsUrlBase}?boundary-type=amp&network=${networkName}&park=${parkName ?? ""}`;
        let habitatObservationsUrl = `${habitatObservationsUrlBase}?boundary-type=amp&network=${networkName}&park=${parkName ?? ""}`;
        let networkResearchEffortUrl = `${researchEffortUrlBase}/${networkName}.json`;
        let parkResearchEffortUrl = parkName ? `${researchEffortUrlBase}/${networkName}/${parkName}.json` : null;
        let regionReportDataUrl = `${regionReportDataUrlBase}?boundary-type=amp&network=${networkName}&park=${parkName ?? ""}`;

        let pageLink = "<?php echo get_page_link(); ?>";
    </script>

    <!-- <header class="entry-header">
        <?php the_title( '<h1 class="entry-title">', '</h1>' ); ?>
    </header> -->

    <div class="entry-content">
        <section>
            <h3 class="region-report-region-heading" id="region-report-region-heading-<?php the_ID(); ?>"></h3>
            <script>
                postElement.addEventListener(
                    "regionReportData",
                    e => {
                        const regionHeading = document.getElementById(`region-report-region-heading-${postId}`);

                        const networkHyperlink = document.createElement("a");
                        networkHyperlink.innerText = e.detail.network.network;
                        networkHyperlink.setAttribute("href", `${pageLink.split('/').slice(0, -2).join('/')}/${e.detail.network.slug}/`);
                        regionHeading.appendChild(networkHyperlink);

                        if (e.detail.park) {
                            const caret = document.createElement("i");
                            caret.className = "fa fa-caret-right";

                            const parkHyperlink = document.createElement("a");
                            parkHyperlink.innerText = e.detail.park;
                            parkHyperlink.setAttribute("href", `${pageLink.split('/').slice(0, -2).join('/')}/${e.detail.slug}/`);

                            regionHeading.appendChild(caret);
                            regionHeading.appendChild(parkHyperlink);
                        }
                    }
                );
            </script>

            <div class="region-report-outline">
                <div>
                    <?php the_content(); ?>
                    <div class="region-report-overview-map">
                        <div class="region-report-labeled-toggle">
                            <div>All data</div>
                            <label class="region-report-switch">
                                <input type="checkbox" onclick="toggleMinimap(this.checked)">
                                <span class="region-report-slider"></span>
                            </label>
                            <div>Public/analysed data</div>
                        </div>
                        <div class="region-report-overview-map-map" id="region-report-overview-map-map-<?php the_ID(); ?>"></div>
                        <script>
                            const map = L.map(`region-report-overview-map-map-${postId}`, { maxZoom: 19, zoomControl: false });

                            const allLayers = L.layerGroup();
                            let allLayersBoundary;
                            const publicLayers = L.layerGroup();
                            let publicLayersBoundary;

                            let overviewMapBounds;

                            L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png").addTo(map);

                            postElement.addEventListener(
                                "regionReportData",
                                e => {
                                    // all layers
                                    e.detail.all_layers.forEach(
                                        layer => {
                                            L.tileLayer.wms(
                                                layer.server_url,
                                                {
                                                    layers: layer.layer_name,
                                                    transparent: true,
                                                    tiled: true,
                                                    format: "image/png",
                                                    styles: layer.style,
                                                    cql_filter: `Network='${e.detail.network.network}'` + (e.detail.park ? ` AND Park='${e.detail.park}'` : "")
                                                }
                                            ).addTo(allLayers);
                                        }
                                    );

                                    // all layers boundary
                                    allLayersBoundary = L.tileLayer.wms(
                                        e.detail.all_layers_boundary.server_url,
                                        {
                                            layers: e.detail.all_layers_boundary.layer_name,
                                            transparent: true,
                                            tiled: true,
                                            format: "image/png",
                                            styles: e.detail.all_layers_boundary.style,
                                            cql_filter: e.detail.park ? `RESNAME='${e.detail.park}'` : `NETNAME='${e.detail.network.network}'`
                                        }
                                    );

                                    // public layers
                                    e.detail.public_layers.forEach(
                                        layer => {
                                            L.tileLayer.wms(
                                                layer.server_url,
                                                {
                                                    layers: layer.layer_name,
                                                    transparent: true,
                                                    tiled: true,
                                                    format: "image/png",
                                                    styles: layer.style,
                                                    cql_filter: `Network='${e.detail.network.network}'` + (e.detail.park ? ` AND Park='${e.detail.park}'` : "")
                                                }
                                            ).addTo(publicLayers);
                                        }
                                    );

                                    // public layers boundary
                                    publicLayersBoundary = L.tileLayer.wms(
                                        e.detail.public_layers_boundary.server_url,
                                        {
                                            layers: e.detail.public_layers_boundary.layer_name,
                                            transparent: true,
                                            tiled: true,
                                            format: "image/png",
                                            styles: e.detail.public_layers_boundary.style,
                                            cql_filter: e.detail.park ? `RESNAME='${e.detail.park}'` : `NETNAME='${e.detail.network.network}'`
                                        }
                                    );

                                    // set up map
                                    map.addLayer(allLayersBoundary);
                                    map.addLayer(allLayers);
                                    map._handlers.forEach(function (handler) {
                                        handler.disable();
                                    });
                                    $.ajax(e.detail.all_layers_boundary.server_url, {
                                        dataType: "json",
                                        data: {
                                            request: "GetFeature",
                                            service: "WFS",
                                            version: "2.0.0",
                                            outputFormat: "application/json",
                                            typeNames: e.detail.all_layers_boundary.layer_name,
                                            cql_filter: e.detail.park ? `RESNAME='${e.detail.park}'` : `NETNAME='${e.detail.network.network}'`
                                        },
                                        success: response => {
                                            overviewMapBounds = L.geoJson(response).getBounds();
                                            map.fitBounds(overviewMapBounds);

                                            window.addEventListener(
                                                "resize",
                                                e => { map.fitBounds(overviewMapBounds); }
                                            );
                                        }
                                    });
                                }
                            );

                            function toggleMinimap(publicOnly) {
                                map.removeLayer(allLayers);
                                map.addLayer(allLayersBoundary);
                                map.removeLayer(publicLayers);
                                map.addLayer(publicLayersBoundary);

                                map.addLayer(publicOnly ? publicLayersBoundary : allLayersBoundary);
                                map.addLayer(publicOnly ? publicLayers : allLayers);
                            }
                        </script>
                    </div>
                </div>

                <div id="region-report-parks-<?php the_ID(); ?>"></div>
            </div>
            <script>
                    postElement.addEventListener(
                        "regionReportData",
                        e => {
                            const parks = document.getElementById(`region-report-parks-${postId}`);
                            
                            if (e.detail.parks) {
                                const parkList = document.createElement("ul");
                                e.detail.parks.forEach(
                                    e => {
                                        const listItem = document.createElement("li");
                                        const hyperlink = document.createElement("a");
                                        hyperlink.innerText = e.park;
                                        hyperlink.setAttribute("href", `${pageLink.split('/').slice(0, -2).join('/')}/${e.slug}/`);
                                        listItem.appendChild(hyperlink);
                                        parkList.appendChild(listItem);
                                    }
                                );
                                parks.appendChild(parkList);
                            } else {
                                parks.remove();
                            }
                        }
                    );
                </script>
        </section>

        <section class="region-report-known">
            <h2>What's known about the <?php echo $region_name; ?>?</h2>

            <section>
                <h3>Habitat</h3>
                <div class="region-report-chart-table">
                    <div>
                        <div class="region-report-chart" id="region-report-habitat-chart-<?php the_ID(); ?>"></div>
                        <script>
                            postElement.addEventListener(
                                "habitatStatistics",
                                e => {
                                    const values = e.detail.filter(e => e.habitat);
                                    vegaEmbed(
                                        `#region-report-habitat-chart-${postId}`,
                                        {
                                            background: "transparent",
                                            data: { values: values },
                                            mark: { type: "arc" },
                                            encoding: {
                                                theta: {
                                                    field: "area",
                                                    type: "quantitative"
                                                },
                                                color: {
                                                    field: "habitat",
                                                    type: "nominal",
                                                    legend: { title: "Habitat" },
                                                    sort: values.map(e => e.habitat),
                                                    scale: { range: values.map(e => e.color) }
                                                }
                                            }
                                        },
                                        { actions: false }
                                    );
                                }
                            );
                        </script>
                    </div>
                    <div>
                        <table>
                            <thead>
                                <tr>
                                    <th>Habitat</th>
                                    <th>Area (km²)</th>
                                    <th>Mapped (%)</th>
                                    <th>Total (%)</th>
                                </tr>
                            </thead>

                            <tbody id="region-report-habitat-table-<?php the_ID(); ?>"></tbody>
                        </table>
                        <script>
                            postElement.addEventListener(
                                "habitatStatistics",
                                e => {
                                    const values = e.detail;
                                    const table = document.getElementById(`region-report-habitat-table-${postId}`);

                                    values.forEach( habitat => {
                                        const row = table.insertRow();

                                        row.insertCell().innerHTML = habitat.habitat ?? "Total Mapped";
                                        row.insertCell().innerHTML = habitat.area.toFixed(1);
                                        row.insertCell().innerHTML = habitat.mapped_percentage?.toFixed(1) ?? "N/A";
                                        row.insertCell().innerHTML = habitat.total_percentage.toFixed(1);
                                    });
                                }
                            );
                        </script>
                    </div>
                </div>
            </section>

            <section>
                <h3>Bathymetry</h3>
                <div class="region-report-chart-table">
                    <div>
                        <div class="region-report-chart" id="region-report-bathymetry-chart-<?php the_ID(); ?>"></div>
                        <script>
                            postElement.addEventListener(
                                "bathymetryStatistics",
                                e => {
                                    const values = e.detail.filter(e => e.resolution);
                                    vegaEmbed(
                                        `#region-report-bathymetry-chart-${postId}`,
                                        {
                                            background: "transparent",
                                            data: { values: values },
                                            mark: { type: "arc" },
                                            encoding: {
                                                theta: {
                                                    field: "area",
                                                    type: "quantitative"
                                                },
                                                color: {
                                                    field: "resolution",
                                                    type: "nominal",
                                                    legend: { title: "Resolution" },
                                                    sort: values.map(e => e.resolution),
                                                    scale: { range: values.map(e => e.color) }
                                                }
                                            }
                                        },
                                        { actions: false }
                                    );
                                }
                            );
                        </script>
                    </div>
                    <div>
                        <table>
                            <thead>
                                <tr>
                                    <th>Resolution</th>
                                    <th>Area (km²)</th>
                                    <th>Mapped (%)</th>
                                    <th>Total (%)</th>
                                </tr>
                            </thead>

                            <tbody id="region-report-bathymetry-table-<?php the_ID(); ?>"></tbody>
                        </table>
                        <script>
                            postElement.addEventListener(
                                "bathymetryStatistics",
                                e => {
                                    const values = e.detail;
                                    const table = document.getElementById(`region-report-bathymetry-table-${postId}`);

                                    values.forEach( bathymetry => {
                                        const row = table.insertRow();

                                        row.insertCell().innerHTML = bathymetry.resolution ?? "Total Mapped";
                                        row.insertCell().innerHTML = bathymetry.area.toFixed(1);
                                        row.insertCell().innerHTML = bathymetry.mapped_percentage?.toFixed(1) ?? "N/A";
                                        row.insertCell().innerHTML = bathymetry.total_percentage.toFixed(1);
                                    });
                                }
                            );
                        </script>
                    </div>
                </div>
            </section>

            <section>
                <h3>Habitat Observations</h3>
                <ul id="region-report-habitat-observations-<?php the_ID(); ?>">
                    <li>
                        <span>0 imagery deployments (0 campaigns)</span>
                    </li>

                    <li>
                        <span>0 video deployments (0 campaigns)</span>
                    </li>

                    <li>
                        <span>0 sediment samples (0 analysed) from 0 surveys</span>
                    </li>
                </ul>
                <script>
                    postElement.addEventListener(
                        "habitatObservations",
                        e => {
                            function addToList(list, text) {
                                const item = document.createElement("li");
                                item.innerText = text;
                                list.appendChild(item);
                            }

                            const squidle = e.detail.squidle;
                            const globalArchive = e.detail.global_archive;
                            const sediment = e.detail.sediment;

                            const habitatObservationsList = document.getElementById(`region-report-habitat-observations-${postId}`);
                            
                            // squidle item
                            const squidleItem = document.createElement("li");

                            const squidleHead = document.createElement("span");
                            squidleHead.innerText = `${squidle.deployments ?? 0} imagery deployments (${squidle.campaigns ?? 0} campaigns)`;
                            squidleItem.appendChild(squidleHead);

                            if ((squidle.deployments ?? 0) > 0) {
                                const squidleList = document.createElement("ul");
                                addToList(squidleList, `Date range: ${squidle.start_date ?? "unknown"} to ${squidle.end_date ?? "unknown"}`);
                                addToList(squidleList, `Methods of collection:  ${squidle.method ?? "N/A"}`);
                                addToList(squidleList, `${squidle.images ?? 0} images collected`);
                                addToList(squidleList, `${squidle.total_annotations ?? 0} images annotations (${squidle.public_annotations ?? 0} public)`);
                                squidleItem.appendChild(squidleList);
                            }

                            // global archive item
                            const globalArchiveItem = document.createElement("li");

                            const globalArchiveHead = document.createElement("span");
                            globalArchiveHead.innerText = `${globalArchive.deployments ?? 0} video deployments (${globalArchive.campaigns ?? 0} campaigns)`;
                            globalArchiveItem.appendChild(globalArchiveHead);

                            if ((globalArchive.deployments ?? 0) > 0) {
                                const globalArchiveList = document.createElement("ul");
                                addToList(globalArchiveList, `Date range: ${globalArchive.start_date ?? "unknown"} to ${globalArchive.end_date ?? "unknown"}`);
                                addToList(globalArchiveList, `Methods of collection: ${globalArchive.method ?? "N/A"}`);
                                addToList(globalArchiveList, `${globalArchive.video_time ?? 0} hours of video`);
                                globalArchiveItem.appendChild(globalArchiveList);
                            }

                            // sediment item
                            const sedimentItem = document.createElement("li");

                            const sedimentHead = document.createElement("span");
                            sedimentHead.innerText = `${sediment.samples ?? 0} sediment samples (${sediment.analysed ?? 0} analysed) from ${sediment.survey ?? 0} surveys`;
                            sedimentItem.appendChild(sedimentHead);
                            
                            if ((sediment.samples ?? 0) > 0) {
                                const sedimentList = document.createElement("ul");
                                addToList(sedimentList, `Date range: ${sediment.start_date ?? "unknown"} to ${sediment.end_date ?? "unknown"}`);
                                addToList(sedimentList, `Methods of collection:  ${sediment.method ?? "N/A"}`);
                                sedimentItem.appendChild(sedimentList);
                            }

                            // populate habitat observations list
                            habitatObservationsList.innerHTML = "";
                            habitatObservationsList.appendChild(squidleItem);
                            habitatObservationsList.appendChild(globalArchiveItem);
                            habitatObservationsList.appendChild(sedimentItem);
                        }
                    );
                </script>
            </section>

            <section>
                <h3>Research Effort</h3>
                
                <div id="region-report-research-effort-<?php the_ID(); ?>"></div>
                <script>
                    // declarations
                    let networkResearchEffort;
                    let parkResearchEffort;

                    function toggleResearchEffort(showNetwork) {
                        // build values
                        const values = [];
                        (showNetwork ? networkResearchEffort : parkResearchEffort).forEach(e => {
                            values.push({
                                year: e.year,
                                end: e.year + 0.25,
                                count: e.imagery_count,
                                group: "Imagery (campaigns)",
                                color: "#3C67BC"
                            });
                            values.push({
                                year: e.year + 0.25,
                                end: e.year + 0.5,
                                count: e.video_count,
                                group: "Video (campaigns)",
                                color: "#EA722B"
                            });
                            values.push({
                                year: e.year + 0.5,
                                end: e.year + 0.75,
                                count: e.sediment_count,
                                group: "Sediment (surveys)",
                                color: "#9B9B9B"
                            });
                            values.push({
                                year: e.year + 0.75,
                                end: e.year + 1,
                                count: e.bathymetry_count,
                                group: "Bathymetry (surveys)",
                                color: "#FFB800"
                            });
                        });
                        const filteredValues = values.filter(e => e.year >= 2000);

                        // generate graphs
                        vegaEmbed(
                            `#region-report-research-effort-1-${postId}`,
                            {
                                title: "Full Timeseries",
                                background: "transparent",
                                data: { values: values },
                                width: "container",
                                mark: "bar",
                                encoding: {
                                    x: {
                                        field: "year",
                                        type: "quantitative",
                                        axis: {
                                            tickMinStep: 1,
                                            format: "r"
                                        },
                                        scale: {
                                            domain: [
                                                Math.floor(Math.min(...values.map(e => e.year))),
                                                new Date().getFullYear() + 1
                                            ]
                                        },
                                        title: "Year"
                                    },
                                    x2: { field: "end" },
                                    y: {
                                        field: "count",
                                        type: "quantitative",
                                        title: "Survey Effort",
                                        axis: { tickMinStep: 1 }
                                    },
                                    color: {
                                        field: "group",
                                        type: "nominal",
                                        legend: { title: null },
                                        sort: values.map(e => e.group),
                                        scale: { range: values.map(e => e.color) }
                                    }
                                }
                            },
                            { actions: false }
                        );
                        
                        vegaEmbed(
                            `#region-report-research-effort-2-${postId}`,
                            {
                                title: "2000 to Present",
                                background: "transparent",
                                data: { values: filteredValues },
                                width: "container",
                                mark: "bar",
                                encoding: {
                                    x: {
                                        field: "year",
                                        type: "quantitative",
                                        axis: {
                                            tickMinStep: 1,
                                            format: "r"
                                        },
                                        scale: {
                                            domain: [
                                                2000,
                                                new Date().getFullYear() + 1
                                            ]
                                        },
                                        title: "Year"
                                    },
                                    x2: { field: "end" },
                                    y: {
                                        field: "count",
                                        type: "quantitative",
                                        title: "Survey Effort",
                                        axis: { tickMinStep: 1 }
                                    },
                                    color: {
                                        field: "group",
                                        type: "nominal",
                                        legend: { title: null },
                                        sort: filteredValues.map(e => e.group),
                                        scale: { range: filteredValues.map(e => e.color) }
                                    }
                                }
                            },
                            { actions: false }
                        );
                    }

                    // setup
                    const researchEffortElement = document.getElementById(`region-report-research-effort-${postId}`);

                    if (parkName) {
                        researchEffortElement.innerHTML = `
                            <div class="region-report-labeled-toggle">
                                <div>${parkName}</div>
                                <label class="region-report-switch">
                                    <input type="checkbox" onclick="toggleResearchEffort(this.checked)">
                                    <span class="region-report-slider"></span>
                                </label>
                                <div>${networkName}</div>
                            </div>`;
                    }
                    researchEffortElement.innerHTML += `
                        <div class="region-report-research-effort-graphs" id="region-report-research-effort-graphs-${postId}">
                            <div id="region-report-research-effort-1-${postId}"></div>
                            <div id="region-report-research-effort-2-${postId}"></div>
                        </div>`;

                    postElement.addEventListener(
                        "networkResearchEffort",
                        e => {
                            networkResearchEffort = e.detail;
                            if (!parkName) toggleResearchEffort(true);
                        }
                    );

                    postElement.addEventListener(
                        "parkResearchEffort",
                        e => {
                            parkResearchEffort = e.detail;
                            toggleResearchEffort(false);
                        }
                    );
                </script>

                <div class="region-report-research-rating">
                    <div id="region-report-star-ratings-<?php the_ID(); ?>">
                        <div><!-- State of bathymetry mapping --></div>
                        <div><!-- State of habitat observations --></div>
                        <div><!-- State of habitat maps --></div>
                    </div>
                    <script>
                        postElement.addEventListener(
                            "regionReportData",
                            e => {
                                let starRatings = document.getElementById(`region-report-star-ratings-${postId}`)
                                starRating(
                                    starRatings.children[0],
                                    Math.round(e.detail.bathymetry_state * 2),
                                    10,
                                    "State of bathymetry mapping"
                                );
                                starRating(
                                    starRatings.children[1],
                                    Math.round(e.detail.habitat_observations_state * 2),
                                    10,
                                    "State of habitat observations"
                                );
                                starRating(
                                    starRatings.children[2],
                                    Math.round(e.detail.habitat_state * 2),
                                    10,
                                    "State of habitat maps"
                                );
                            }
                        );
                    </script>

                    <div class="region-report-research-rating-quote" id="region-report-research-rating-quote-<?php the_ID(); ?>"></div>
                    <script>
                        postElement.addEventListener(
                            "regionReportData",
                            e => {
                                let quote = document.getElementById(`region-report-research-rating-quote-${postId}`)
                                quote.innerText = e.detail.state_summary;
                            }
                        );
                    </script>
                </div>
            </section>
        </section>

        <section class="region-report-contains">
            <h2>What's in the <?php echo $region_name; ?>?</h2>
            
            <section class="region-report-mapped-habitat">
                <h3>Mapped Habitat</h3>
            </section>

            <section class="region-report-reserves">
                <h3>Reserves</h3>
            </section>
        </section>

        <section>
            <h2>Imagery</h2>
            <div class="region-report-imagery" id="region-report-imagery-<?php the_ID(); ?>">Loading imagery deployment data...</div>
            <script>
                // declarations
                let imageryMap;
                let imageryMapBounds;
                let imageryMarkers = [];
                let squidleUrl;
                let imageryGrid;

                function focusMarker(focusIndex) {
                    imageryMarkers.forEach((marker, index) => {
                        imageryMap.removeLayer(marker);
                        if (index == focusIndex || focusIndex == null) imageryMap.addLayer(marker);
                    });
                }

                function refreshImagery() {
                    if (squidleUrl == null) return;
                    if (imageryMap == null) return;

                    $.ajax(squidleUrl, {
                        dataType: "json",
                        success: media => {

                            $.ajax("https://squidle.org/api/pose", {
                                dataType: "json",
                                data: {
                                    q: JSON.stringify({
                                        filters: [{
                                            name: "media_id",
                                            op: "in",
                                            val: media.objects.map(e => e.id)
                                        }]
                                    })
                                },
                                success: pose => {
                                    // clear
                                    imageryMarkers.forEach(marker => imageryMap.removeLayer(marker));
                                    imageryMarkers = [];
                                    imageryGrid.innerHTML = "";

                                    // populate
                                    media.objects.forEach((image, index) => {
                                        Object.assign(image, pose.objects.filter(e => e.media.id == image.id)[0]);

                                        // grid items
                                        imageryGrid.innerHTML += `
                                            <a
                                                href="https://squidle.org/api/media/${image.media.id}?template=models/media/preview_single.html"
                                                target="_blank"
                                                onmouseenter="focusMarker(${index})"
                                                onmouseleave="focusMarker()"
                                            >
                                                <img src="${image.path_best_thm}">
                                                <div class="region-report-imagery-grid-number">${index + 1}</div>
                                            </a>`;

                                        // marker
                                        imageryMarkers.push(
                                            new L.Marker(
                                                [image.lat, image.lon],
                                                {
                                                    icon: L.divIcon({
                                                        html: `
                                                            <svg width=25 height=41>
                                                                <polygon
                                                                    points="0,0 25,0, 25,28 20,28 12.5,41 5,28 0,28"
                                                                    fill="rgb(0, 147, 36)"
                                                                    stroke="white"
                                                                    stroke-width=1.5
                                                                />
                                                                <text
                                                                    fill="white"
                                                                    x="50%"
                                                                    y=14
                                                                    dominant-baseline="middle"
                                                                    text-anchor="middle"
                                                                    font-family="sans-serif"
                                                                    font-weight="bold"
                                                                >${index + 1}</text>
                                                            </svg>`,
                                                        iconSize: [25, 41],
                                                        iconAnchor: [12.5, 41]
                                                    })
                                                }
                                            )
                                        );
                                        imageryMarkers[imageryMarkers.length - 1].addTo(imageryMap);
                                    });
                                }
                            });
                        }
                    });
                }

                postElement.addEventListener(
                    "regionReportData",
                    e => {
                        squidleUrl = e.detail.squidle_url;
                        const imageryElement = document.getElementById(`region-report-imagery-${postId}`);

                        if (squidleUrl) {
                            imageryElement.innerHTML = `
                                <div class="region-report-imagery-map" id="region-report-imagery-map-${postId}"></div>
                                <div class="region-report-imagery-images">
                                    <div class="region-report-imagery-grid" id="region-report-imagery-grid-${postId}"></div>
                                    <a href="#!" onclick="refreshImagery()">Refresh images</a>
                                </div>`;


                            imageryGrid = document.getElementById(`region-report-imagery-grid-${postId}`);

                            // set-up map
                            imageryMap = L.map(`region-report-imagery-map-${postId}`, { maxZoom: 19, zoomControl: false });
                            L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png").addTo(imageryMap);
                            imageryMap._handlers.forEach(e => e.disable());

                            refreshImagery();

                            // map boundary layer
                            L.tileLayer.wms(
                                e.detail.all_layers_boundary.server_url,
                                {
                                    layers: e.detail.all_layers_boundary.layer_name,
                                    transparent: true,
                                    tiled: true,
                                    format: "image/png",
                                    styles: e.detail.all_layers_boundary.style,
                                    cql_filter: e.detail.park ? `RESNAME='${e.detail.park}'` : `NETNAME='${e.detail.network.network}'`
                                }
                            ).addTo(imageryMap);

                            // zoom to map extent
                            $.ajax(e.detail.all_layers_boundary.server_url, {
                                dataType: "json",
                                data: {
                                    request: "GetFeature",
                                    service: "WFS",
                                    version: "2.0.0",
                                    outputFormat: "application/json",
                                    typeNames: e.detail.all_layers_boundary.layer_name,
                                    cql_filter: e.detail.park ? `RESNAME='${e.detail.park}'` : `NETNAME='${e.detail.network.network}'`
                                },
                                success: response => {
                                    imageryMapBounds = L.geoJson(response).getBounds();
                                    imageryMap.fitBounds(imageryMapBounds);

                                    window.addEventListener(
                                        "resize",
                                        e => { imageryMap.fitBounds(imageryMapBounds); }
                                    );
                                }
                            });
                        } else {
                            imageryElement.innerText = "No imagery deployments found in this region";
                        }
                    }
                );
            </script>
        </section>

        <section class="region-report-pressures">
            <h2>What's happening in the <?php echo $region_name; ?>?</h2>
        </section>
    </div>

    <script>
        $.ajax(habitatStatisticsUrl, {
            dataType : "json",
            success: response => {
                postElement.dispatchEvent(
                    new CustomEvent(
                        "habitatStatistics",
                        { detail: response }
                    )
                );
            }
        });

        $.ajax(bathymetryStatisticsUrl, {
            dataType : "json",
            success: response => {
                postElement.dispatchEvent(
                    new CustomEvent(
                        "bathymetryStatistics",
                        { detail: response }
                    )
                );
            }
        });

        $.ajax(habitatObservationsUrl, {
            dataType : "json",
            success: response => {
                postElement.dispatchEvent(
                    new CustomEvent(
                        "habitatObservations",
                        { detail: response }
                    )
                );
            }
        });

        $.ajax(networkResearchEffortUrl, {
            dataType: "json",
            success: response => {
                postElement.dispatchEvent(
                    new CustomEvent(
                        "networkResearchEffort",
                        { detail: response }
                    )
                );
            }
        });

        if (parkResearchEffortUrl) {
            $.ajax(parkResearchEffortUrl, {
                dataType: "json",
                success: response => {
                    postElement.dispatchEvent(
                        new CustomEvent(
                            "parkResearchEffort",
                            { detail: response }
                        )
                    );
                }
            });
        }

        $.ajax(regionReportDataUrl, {
            dataType : "json",
            success: response => {
                postElement.dispatchEvent(
                    new CustomEvent(
                        "regionReportData",
                        { detail: response }
                    )
                );
            }
        });
    </script>
</article>

<?php get_footer(); ?>
