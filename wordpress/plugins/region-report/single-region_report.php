<?php get_header(); ?>

<?php
    $habitat_statistics_url_base = get_option('region_report_habitat_statistics_url_base');
    $bathymetry_statistics_url_base = get_option('region_report_bathymetry_statistics_url_base');
    $habitat_observations_url_base = get_option('region_report_habitat_observations_url_base');
    $research_effort_url_base = get_option('region_report_research_effort_url_base');
    $region_report_data_url_base = get_option('region_report_region_report_data_url_base');
    $pressure_preview_url_base = get_option('region_report_pressure_preview_url_base');
    $map_url_base = get_option('region_report_map_url_base');

    $overview_map_caption = get_option('region_report_overview_map_caption');
    $known_caption = get_option('region_report_known_caption');
    $imagery_caption = get_option('region_report_imagery_caption');
    $pressures_caption = get_option('region_report_pressures_caption');

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
    const habitatStatisticsUrlBase = <?php echo json_encode($habitat_statistics_url_base); ?>;
    const bathymetryStatisticsUrlBase = <?php echo json_encode($bathymetry_statistics_url_base); ?>;
    const habitatObservationsUrlBase = <?php echo json_encode($habitat_observations_url_base); ?>;
    const researchEffortUrlBase = <?php echo json_encode($research_effort_url_base); ?>;
    const regionReportDataUrlBase = <?php echo json_encode($region_report_data_url_base); ?>;
    const pressurePreviewUrlBase = <?php echo json_encode($pressure_preview_url_base); ?>;
    const mapUrlBase = <?php echo json_encode($map_url_base); ?>;

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

    function regionReportToggleTab(tab) {
        const tabs = Array.prototype.slice.call(tab.parentElement.children);
        const tabContent = document.getElementById(tab.parentElement.dataset.tabContent);
        const tabPanes = Array.prototype.slice.call(tabContent.children);
        const tabPane = tabPanes.filter(tabPane => tabPane.dataset.tab == tab.dataset.tab)[0];

        tabs.forEach(tab => tab.classList.remove("region-report-selected"));
        tab.classList.add("region-report-selected");

        tabPanes.forEach(tabPane => tabPane.classList.remove("region-report-selected"));
        tabPane.classList.add("region-report-selected");
    }
</script>

<article id="post-<?php the_ID(); ?>" <?php post_class(); ?>>
    <script>
        let postId = "<?php the_ID(); ?>";
        let postElement = document.getElementById(`post-${postId}`);

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
                        <div class="region-report-caption"><?php echo $overview_map_caption; ?></div>
                        <script>
                            const map = L.map(`region-report-overview-map-map-${postId}`, { maxZoom: 19, zoomControl: false });

                            const allLayers = L.layerGroup();
                            let allLayersBoundary;
                            const publicLayers = L.layerGroup();
                            let publicLayersBoundary;

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

                                    // zoom to map extent
                                    let bounds = [[e.detail.bounding_box.north, e.detail.bounding_box.east], [e.detail.bounding_box.south, e.detail.bounding_box.west]];
                                    map.fitBounds(bounds);
                                    window.addEventListener(
                                        "resize",
                                        e => { map.fitBounds(bounds); }
                                    );
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
            <div class="region-report-caption"><?php echo $known_caption; ?></div>

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

        <section>
            <h2>What's in the <?php echo $region_name; ?>?</h2>
            <div class="region-report-caption"><?php echo $pressures_caption; ?></div>
        
            <section>
                <h3>Imagery</h3>
                <div class="region-report-imagery" id="region-report-imagery-<?php the_ID(); ?>">Loading imagery deployment data...</div>
                <script>
                    // declarations
                    let imageryMap;
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
                                        <div class="region-report-caption"><?php echo $imagery_caption; ?></div>
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
                                let bounds = [[e.detail.bounding_box.north, e.detail.bounding_box.east], [e.detail.bounding_box.south, e.detail.bounding_box.west]];
                                imageryMap.fitBounds(bounds);
                                window.addEventListener(
                                    "resize",
                                    e => { imageryMap.fitBounds(bounds); }
                                );
                            } else {
                                imageryElement.innerText = "No imagery deployments found in this region";
                            }
                        }
                    );
                </script>
            </section>

            <section>
                <h3>Pressures</h3>
                <div class="region-report-pressures">
                    <div class="region-report-tabs" id="region-report-pressures-categories-<?php the_ID(); ?>" data-tab-content="region-report-pressures-tab-content-<?php the_ID(); ?>"></div>
                    <div class="region-report-tab-content" id="region-report-pressures-tab-content-<?php the_ID(); ?>"></div>
                </div>
                <script>
                    const pressuresTabs = document.getElementById(`region-report-pressures-categories-${postId}`);
                    const pressuresTabContent = document.getElementById(pressuresTabs.dataset.tabContent);

                    postElement.addEventListener(
                        "regionReportData",
                        e => {
                            const pressures = e.detail.pressures;
                            const groupedPressures = pressures.reduce(
                                (acc, curr) => {
                                    if (!acc[curr.category]) acc[curr.category] = [];
                                    acc[curr.category].push(curr);
                                    return acc;
                                }, {}
                            );

                            // All pressures tab
                            pressuresTabs.innerHTML += `
                                <div
                                    class="region-report-tab region-report-selected"
                                    data-tab="All"
                                    onclick="regionReportToggleTab(this)"
                                >
                                    All (${pressures.length})
                                </div>`;

                            // Create tab pane
                            const tabPane = document.createElement("div");
                            tabPane.className = "region-report-tab-pane region-report-pressures-grid region-report-selected";
                            tabPane.dataset.tab = "All";
                            pressures.forEach(pressure => {
                                tabPane.innerHTML += `
                                    <a
                                        href="${mapUrlBase}#${pressure.save_state}"
                                        target="_blank"
                                    >
                                        <img src="${pressurePreviewUrlBase}/${pressure.id}.png">
                                    </a>`;
                            });
                            pressuresTabContent.appendChild(tabPane);

                            // Pressure category tabs
                            Object.entries(groupedPressures).forEach(([category, pressures]) => {
                                pressuresTabs.innerHTML += `
                                    <div
                                        class="region-report-tab"
                                        data-tab="${category}"
                                        onclick="regionReportToggleTab(this)"
                                    >
                                        ${category} (${pressures.length})
                                    </div>`;

                                // Create tab pane
                                const tabPane = document.createElement("div");
                                tabPane.className = "region-report-tab-pane region-report-pressures-grid";
                                tabPane.dataset.tab = category;
                                pressures.forEach(pressure => {
                                    tabPane.innerHTML += `
                                        <a
                                            href="${mapUrlBase}/#${pressure.save_state}"
                                            target="_blank"
                                        >
                                            <img src="${pressurePreviewUrlBase}/${pressure.id}.png">
                                        </a>`;
                                });
                                pressuresTabContent.appendChild(tabPane);
                            })
                        }
                    );
                </script>
            </section>
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
