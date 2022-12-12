class RegionReport {
    postId = null;
    pressurePreviewUrlBase = null;
    mapUrlBase = null;
    imageryCaption = null;

    networkResearchEffort = null;
    parkResearchEffort = null;

    // overview map
    overviewMap = null;
    allLayers = L.layerGroup();
    allLayersBoundary = null;
    allLayersHyperlink = null;
    publicLayers = L.layerGroup();
    publicLayersBoundary = null;
    publicLayersHyperlink = null;
    overviewMapHyperlink = null;

    // imagery map
    imageryMap = null;
    imageryMarkers = [];
    squidleUrl = null;
    imageryGrid = null;

    constructor({
        postId: postId,
        habitatStatisticsUrlBase: habitatStatisticsUrlBase,
        bathymetryStatisticsUrlBase: bathymetryStatisticsUrlBase,
        habitatObservationsUrlBase: habitatObservationsUrlBase,
        researchEffortUrlBase: researchEffortUrlBase,
        regionReportDataUrlBase: regionReportDataUrlBase,
        pressurePreviewUrlBase: pressurePreviewUrlBase,
        mapUrlBase: mapUrlBase,
        networkName: networkName,
        parkName: parkName,
        imageryCaption: imageryCaption
    }) {
        this.postId = postId;
        this.pressurePreviewUrlBase = pressurePreviewUrlBase;
        this.mapUrlBase = mapUrlBase;
        this.imageryCaption = imageryCaption;

        this.setupOverviewMap();
        this.setupResearchEffort(networkName, parkName);

        // Do AJAX
        let habitatStatisticsUrl = `${habitatStatisticsUrlBase}?boundary-type=amp&network=${networkName}&park=${parkName ?? ""}`;
        $.ajax(habitatStatisticsUrl, {
            dataType: "json",
            success: response => {
                this.populateHabitatChart(response);
                this.populateHabitatTable(response);
            }
        });

        let bathymetryStatisticsUrl = `${bathymetryStatisticsUrlBase}?boundary-type=amp&network=${networkName}&park=${parkName ?? ""}`;
        $.ajax(bathymetryStatisticsUrl, {
            dataType: "json",
            success: response => {
                this.populateBathymetryChart(response);
                this.populateBathymetryTable(response);
            }
        });

        let habitatObservationsUrl = `${habitatObservationsUrlBase}?boundary-type=amp&network=${networkName}&park=${parkName ?? ""}`;
        $.ajax(habitatObservationsUrl, {
            dataType: "json",
            success: response => {
                this.populateHabitatObservations(response);
            }
        });

        let networkResearchEffortUrl = `${researchEffortUrlBase}/${networkName}.json`;
        $.ajax(networkResearchEffortUrl, {
            dataType: "json",
            success: response => {
                this.networkResearchEffort = response;
                if (!parkName) this.toggleResearchEffort(true);
            }
        });

        let parkResearchEffortUrl = parkName ? `${researchEffortUrlBase}/${networkName}/${parkName}.json` : null;
        if (parkResearchEffortUrl)
            $.ajax(parkResearchEffortUrl, {
                dataType: "json",
                success: response => {
                    this.parkResearchEffort = response;
                    this.toggleResearchEffort(false);
                }
            });

        let regionReportDataUrl = `${regionReportDataUrlBase}?boundary-type=amp&network=${networkName}&park=${parkName ?? ""}`;
        $.ajax(regionReportDataUrl, {
            dataType: "json",
            success: response => {
                this.populateRegionHeading(response);
                this.populateOverviewMap(response);
                this.populateParksList(response);
                this.populateResearchRatings(response);
                this.populateResearchRatingQuote(response);
                this.populateImageryMap(response);
                this.populatePressures(response);
            }
        });
    }

    starRating(element, value, total, text) {
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

    toggleTab(tab) {
        const tabs = Array.prototype.slice.call(tab.parentElement.children);
        const tabContent = document.getElementById(tab.parentElement.dataset.tabContent);
        const tabPanes = Array.prototype.slice.call(tabContent.children);
        const tabPane = tabPanes.filter(tabPane => tabPane.dataset.tab == tab.dataset.tab)[0];

        tabs.forEach(tab => tab.classList.remove("selected"));
        tab.classList.add("selected");

        tabPanes.forEach(tabPane => tabPane.classList.remove("selected"));
        tabPane.classList.add("selected");
    }

    populateHabitatChart(habitatStatistics) {
        const values = habitatStatistics.filter(e => e.habitat);

        if (values.length > 0) {
            vegaEmbed(
                `#region-report-habitat-chart-${this.postId}`,
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
        } else {
            document.getElementById(`region-report-habitat-chart-${this.postId}`).innerHTML = `
                <div class="bp3-non-ideal-state">
                    <div class="bp3-non-ideal-state-visual">
                        <span class="bp3-icon bp3-icon-info-sign"></span>
                    </div>
                    <h4 class="bp3-heading">No Data</h4>
                    <div>No habitat data is available for this region.</div>
                </div>`;
        }
    }

    populateHabitatTable(habitatStatistics) {
        const table = document.getElementById(`region-report-habitat-table-${this.postId}`);
        const withoutUnmapped = habitatStatistics.filter(e => e.habitat);
        if (withoutUnmapped.length > 0) {
            habitatStatistics.forEach(habitat => {
                const row = table.insertRow();
    
                row.insertCell().innerText = habitat.habitat ?? "Total Mapped";
                row.insertCell().innerText = habitat.area.toLocaleString("en-US", { maximumFractionDigits: 1, minimumFractionDigits: 1 });
                row.insertCell().innerText = habitat.mapped_percentage?.toLocaleString("en-US", { maximumFractionDigits: 1, minimumFractionDigits: 1 }) ?? "N/A";
                row.insertCell().innerText = habitat.total_percentage.toLocaleString("en-US", { maximumFractionDigits: 1, minimumFractionDigits: 1 });
            });
        } else {
            table.parentElement.outerHTML = '';
        }
    }

    populateBathymetryChart(bathymetryStatistics) {
        const values = bathymetryStatistics.filter(e => e.resolution);

        if (values.length > 0) {
            vegaEmbed(
                `#region-report-bathymetry-chart-${this.postId}`,
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
        } else {
            document.getElementById(`region-report-bathymetry-chart-${this.postId}`).innerHTML = `
                <div class="bp3-non-ideal-state">
                    <div class="bp3-non-ideal-state-visual">
                        <span class="bp3-icon bp3-icon-info-sign"></span>
                    </div>
                    <h4 class="bp3-heading">No Data</h4>
                    <div>No bathymetry data is available for this region.</div>
                </div>`;
        }
    }

    populateBathymetryTable(bathymetryStatistics) {
        const table = document.getElementById(`region-report-bathymetry-table-${this.postId}`);
        const withoutUnmapped = bathymetryStatistics.filter(e => e.resolution);

        if (withoutUnmapped.length > 0) {
            bathymetryStatistics.forEach(bathymetry => {
                const row = table.insertRow();

                row.insertCell().innerText = bathymetry.resolution ?? "Total Mapped";
                row.insertCell().innerText = bathymetry.area.toLocaleString("en-US", { maximumFractionDigits: 1, minimumFractionDigits: 1 });
                row.insertCell().innerText = bathymetry.mapped_percentage?.toLocaleString("en-US", { maximumFractionDigits: 1, minimumFractionDigits: 1 }) ?? "N/A";
                row.insertCell().innerText = bathymetry.total_percentage.toLocaleString("en-US", { maximumFractionDigits: 1, minimumFractionDigits: 1 });
            });
        } else {
            table.parentElement.outerHTML = '';
        }
    }

    populateHabitatObservations({ squidle: squidle, global_archive: globalArchive, sediment: sediment }) {
        const habitatObservationsList = document.getElementById(`region-report-habitat-observations-${this.postId}`);

        // squidle item
        let squidleDeployments = squidle.deployments?.toLocaleString("en-US") ?? 0;
        let squidleCampaigns = squidle.campaigns?.toLocaleString("en-US") ?? 0;
        let squidleStartDate = squidle.start_date ? (new Date(squidle.start_date)).toLocaleString("en-AU", { month: "short", year: "numeric" }) : null;
        let squidleEndDate = squidle.end_date ? (new Date(squidle.end_date)).toLocaleString("en-AU", { month: "short", year: "numeric" }) : null;
        let squidleDateRange = squidle.start_date ? `${squidleStartDate} to ${squidleEndDate}` : "Unknown";
        let squidleMethod = squidle.method ?? "N/A";
        let squidleImages = squidle.images?.toLocaleString("en-US") ?? 0;
        let squidleTotalAnnotations = squidle.total_annotations?.toLocaleString("en-US") ?? 0;
        let squidlePublicAnnotations = squidle.public_annotations?.toLocaleString("en-US") ?? 0;

        const squidleItem = document.createElement("li");
        squidleItem.innerHTML = `<span>${squidleDeployments} Imagery Deployments <wbr>(${squidleCampaigns} Campaigns)</span>`;

        if (squidle.deployments > 0)
            squidleItem.innerHTML += `
                <ul>
                    <li><b>Date Range: </b>${squidleDateRange}</li>
                    <li><b>Methods of Collection: </b>${squidleMethod}</li>
                    <li><b>Images Collected: </b>${squidleImages}</li>
                    <li>
                        <b>Images Annotations: </b>
                        ${squidleTotalAnnotations} (${squidlePublicAnnotations}
                        <span class="tooltip-parent">
                            <b><u>public</u></b>
                            <div class="tooltip">Finalised curated image annotations</div>
                        </span>
                        )
                    </li>
                </ul>`;

        // global archive item
        let globalArchiveDeployments = globalArchive.deployments?.toLocaleString("en-US") ?? 0;
        let globalArchiveCampaigns = globalArchive.campaigns?.toLocaleString("en-US") ?? 0;
        let globalArchiveStartDate = globalArchive.start_date ? (new Date(globalArchive.start_date)).toLocaleString("en-AU", { month: "short", year: "numeric" }) : null;
        let globalArchiveEndDate = globalArchive.end_date ? (new Date(globalArchive.end_date)).toLocaleString("en-AU", { month: "short", year: "numeric" }) : null;
        let globalArchiveDateRange = globalArchive.start_date ? `${globalArchiveStartDate} to ${globalArchiveEndDate}` : "Unknown";
        let globalArchiveMethod = globalArchive.method ?? "N/A";
        let globalArchiveVideoTime = globalArchive.video_time?.toLocaleString("en-US") ?? 0;
        let globalArchiveVideoAnnotations = globalArchive.video_annotations?.toLocaleString("en-US") ?? 0;

        const globalArchiveItem = document.createElement("li");
        globalArchiveItem.innerHTML = `<span>${globalArchiveDeployments} Video Deployments <wbr>(${globalArchiveCampaigns} Campaigns)</span>`;
        if (globalArchive.deployments > 0)
            globalArchiveItem.innerHTML += `
                <ul>
                    <li><b>Date Range: </b>${globalArchiveDateRange}</li>
                    <li><b>Methods of Collection: </b>${globalArchiveMethod}</li>
                    <li><b>Hours of Video: </b>${globalArchiveVideoTime}</li>
                    <li>
                        <b>Video Annotations: </b>
                        ${globalArchiveVideoAnnotations}
                        <span class="tooltip-parent">
                            <b><u>public</u></b>
                            <div class="tooltip">Publicly available video annotations</div>
                        </span>
                    </li>
                </ul>`;

        // sediment item
        let sedimentSamples = sediment.samples?.toLocaleString("en-US") ?? 0;
        let sedimentAnalysed = sediment.analysed?.toLocaleString("en-US") ?? 0;
        let sedimentSurvey = sediment.survey?.toLocaleString("en-US") ?? 0;
        let sedimentStartDate = sediment.start_date ? (new Date(sediment.start_date)).toLocaleString("en-AU", { month: "short", year: "numeric" }) : null;
        let sedimentEndDate = sediment.end_date ? (new Date(sediment.end_date)).toLocaleString("en-AU", { month: "short", year: "numeric" }) : null;
        let sedimentDateRange = sediment.start_date ? `${sedimentStartDate} to ${sedimentEndDate}` : "Unknown";
        let sedimentMethod = sediment.method ?? "N/A";

        const sedimentItem = document.createElement("li");
        sedimentItem.innerHTML = `<span>${sedimentSamples} Sediment Samples <wbr>(${sedimentSurvey} Surveys)</span>`;

        if (sediment.samples > 0)
            sedimentItem.innerHTML += `
                <ul>
                    <li><b>Date Range: </b>${sedimentDateRange}</li>
                    <li><b>Methods of Collection: </b>${sedimentMethod}</li>
                    <li><b>Samples Analysed: </b>${sedimentAnalysed}</li>
                </ul>`;

        // populate habitat observations list
        habitatObservationsList.innerHTML = "";
        habitatObservationsList.appendChild(squidleItem);
        habitatObservationsList.appendChild(globalArchiveItem);
        habitatObservationsList.appendChild(sedimentItem);
    }

    setupResearchEffort(network, park) {
        // setup
        const researchEffortElement = document.getElementById(`region-report-research-effort-${this.postId}`);

        if (park)
            researchEffortElement.innerHTML = `
                    <div class="labeled-toggle">
                        <div>${park}</div>
                        <label class="switch">
                            <input type="checkbox" onclick="regionReport.toggleResearchEffort(this.checked)">
                            <span class="switch-slider"></span>
                        </label>
                        <div>${network}</div>
                    </div>`;
        researchEffortElement.innerHTML += `
             <div class="research-effort-graphs" id="research-effort-graphs-${this.postId}">
                 <div id="region-report-research-effort-1-${this.postId}"></div>
                 <div id="region-report-research-effort-2-${this.postId}"></div>
             </div>`;
    }

    toggleResearchEffort(showNetwork) {
        // build values
        const values = [];
        (showNetwork ? this.networkResearchEffort : this.parkResearchEffort).forEach(e => {
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
            `#region-report-research-effort-1-${this.postId}`,
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
                        legend: {
                            title: "Legend",
                            orient: "bottom",
                            direction: "vertical"
                        },
                        sort: values.map(e => e.group),
                        scale: { range: values.map(e => e.color) }
                    }
                }
            },
            { actions: false }
        );

        vegaEmbed(
            `#region-report-research-effort-2-${this.postId}`,
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
                        legend: {
                            title: "Legend",
                            orient: "bottom",
                            direction: "vertical"
                        },
                        sort: filteredValues.map(e => e.group),
                        scale: { range: filteredValues.map(e => e.color) }
                    }
                }
            },
            { actions: false }
        );
    }

    populateRegionHeading(regionReportData) {
        const regionHeading = document.getElementById(`region-report-region-heading-${this.postId}`);

        regionHeading.innerHTML = `
            <a href="${(location.origin + location.pathname).split('/').slice(0, -2).join('/')}/${regionReportData.network.slug}/">
                ${regionReportData.network.network}
            </a>`;
        if (regionReportData.park)
            regionHeading.innerHTML += `
                <i class="fa fa-caret-right"></i>
                <a href="${(location.origin + location.pathname).split('/').slice(0, -2).join('/')}/${regionReportData.slug}/">
                    ${regionReportData.park}
                </a>
            `;
    }

    setupOverviewMap() {
        this.overviewMap = L.map(`region-report-overview-map-map-${this.postId}`, { maxZoom: 19, zoomControl: false });
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png").addTo(this.overviewMap);
        this.overviewMapHyperlink = document.getElementById(`region-report-overview-map-hyperlink-${this.postId}`);
    }

    overviewMapAppState(bounds, layers) {
        return [
            "^ ",
            "~:autosave?",
            true,
            "~:filters",
            [
                "^ ",
                "~:layers",
                ""
            ],
            "~:story-maps",
            [
                "^ ",
                "~:featured-map",
                null,
                "~:open?",
                false
            ],
            "~:display",
            [
                "^ ",
                "~:left-drawer",
                true,
                "~:left-drawer-tab",
                "active-layers"
            ],
            "~:map",
            [
                "^ ",
                "~:bounds",
                [
                    "^ ",
                    "~:north",
                    bounds.north,
                    "~:south",
                    bounds.south,
                    "~:east",
                    bounds.east,
                    "~:west",
                    bounds.west
                ],
                "~:active",
                [
                    "~#list",
                    layers
                ],
                "~:active-base",
                1
            ]
        ]
    }

    populateOverviewMap({ all_layers: allLayers, all_layers_boundary: allLayersBoundary, public_layers: publicLayers, public_layers_boundary: publicLayersBoundary, network: network, park: park, bounding_box: bounds }) {
        // all layers
        allLayers.forEach(
            layer => {
                L.tileLayer.wms(
                    layer.server_url,
                    {
                        layers: layer.layer_name,
                        transparent: true,
                        tiled: true,
                        format: "image/png",
                        styles: layer.style,
                        cql_filter: `Network='${network.network}'` + (park ? ` AND Park='${park}'` : "")
                    }
                ).addTo(this.allLayers);
            }
        );

        // all layers boundary
        this.allLayersBoundary = L.tileLayer.wms(
            allLayersBoundary.server_url,
            {
                layers: allLayersBoundary.layer_name,
                transparent: true,
                tiled: true,
                format: "image/png",
                styles: allLayersBoundary.style,
                cql_filter: park ? `RESNAME='${park}'` : `NETNAME='${network.network}'`
            }
        );

        // public layers
        publicLayers.forEach(
            layer => {
                L.tileLayer.wms(
                    layer.server_url,
                    {
                        layers: layer.layer_name,
                        transparent: true,
                        tiled: true,
                        format: "image/png",
                        styles: layer.style,
                        cql_filter: `Network='${network.network}'` + (park ? ` AND Park='${park}'` : "")
                    }
                ).addTo(this.publicLayers);
            }
        );

        // public layers boundary
        this.publicLayersBoundary = L.tileLayer.wms(
            publicLayersBoundary.server_url,
            {
                layers: publicLayersBoundary.layer_name,
                transparent: true,
                tiled: true,
                format: "image/png",
                styles: publicLayersBoundary.style,
                cql_filter: park ? `RESNAME='${park}'` : `NETNAME='${network.network}'`
            }
        );

        // set up map
        this.overviewMap.addLayer(this.allLayersBoundary);
        this.overviewMap.addLayer(this.allLayers);
        this.overviewMap._handlers.forEach(function (handler) {
            handler.disable();
        });

        // zoom to map extent
        this.overviewMap.fitBounds([[bounds.north, bounds.east], [bounds.south, bounds.west]]);
        window.addEventListener("resize", this.overviewMap.invalidateSize);
        if (navigator.userAgent.match(/chrome|chromium|crios/i))
            window.matchMedia('print').addEventListener(
                "change",
                e => {
                    if (e.matches) this.overviewMap.invalidateSize();
                }
            );

        const allLayersAppState = this.overviewMapAppState(bounds, [allLayersBoundary.id, ...(allLayers.map(e=>e.id))]);
        this.allLayersHyperlink = `${this.mapUrlBase}/#${btoa(JSON.stringify(allLayersAppState))}`;
        const publicLayersAppState = this.overviewMapAppState(bounds, [publicLayersBoundary.id, ...(publicLayers.map(e=>e.id))]);
        this.publicLayersHyperlink = `${this.mapUrlBase}/#${btoa(JSON.stringify(publicLayersAppState))}`;

        this.overviewMapHyperlink.href = this.allLayersHyperlink;
    }

    toggleMinimap(publicOnly) {
        this.overviewMap.removeLayer(this.allLayers);
        this.overviewMap.removeLayer(this.allLayersBoundary);
        this.overviewMap.removeLayer(this.publicLayers);
        this.overviewMap.removeLayer(this.publicLayersBoundary);

        this.overviewMap.addLayer(publicOnly ? this.publicLayersBoundary : this.allLayersBoundary);
        this.overviewMap.addLayer(publicOnly ? this.publicLayers : this.allLayers);
        this.overviewMapHyperlink.href = publicOnly ? this.publicLayersHyperlink : this.allLayersHyperlink;
    }

    populateParksList({ parks: parks }) {
        const outline = document.getElementById(`overview-${this.postId}`);

        if (parks) {
            const parkList = document.createElement("ul");
            parks.forEach(
                e => {
                    parkList.innerHTML += `
                        <li>
                            <a href="${(location.origin + location.pathname).split('/').slice(0, -2).join('/')}/${e.slug}/">${e.park}</a>
                        </li>`;
                }
            );
            const parksParent = document.createElement("div");
            parksParent.innerHTML = `
                <div class="heading">
                    <h2>Parks</h2>
                    <div class="caption">View reports for Parks within this Network</div>
                </div>`;
            parksParent.appendChild(parkList);
            parksParent.className = "parks";
            outline.appendChild(parksParent);
            this.overviewMap.invalidateSize();
        }
    }

    populateResearchRatings({ bathymetry_state: bathymetry, habitat_observations_state: observations, habitat_state: habitat }) {
        let starRatings = document.getElementById(`region-report-star-ratings-${this.postId}`)
        this.starRating(
            starRatings.children[0],
            Math.round(bathymetry * 2),
            10,
            "State of bathymetry mapping"
        );
        this.starRating(
            starRatings.children[1],
            Math.round(observations * 2),
            10,
            "State of habitat observations"
        );
        this.starRating(
            starRatings.children[2],
            Math.round(habitat * 2),
            10,
            "State of habitat maps"
        );
    }

    populateResearchRatingQuote({ state_summary: quote }) {
        document.getElementById(`region-report-research-rating-quote-${this.postId}`).innerText = quote;
    }

    focusMarker(focusIndex) {
        this.imageryMarkers.forEach((marker, index) => {
            this.imageryMap.removeLayer(marker);
            if (index == focusIndex || focusIndex == null)
                this.imageryMap.addLayer(marker);
        });
    }

    refreshImagery() {
        if (this.squidleUrl == null) return;
        if (this.imageryMap == null) return;

        $.ajax(this.squidleUrl, {
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
                        this.imageryMarkers.forEach(marker => this.imageryMap.removeLayer(marker));
                        this.imageryMarkers = [];
                        this.imageryGrid.innerHTML = "";

                        // populate
                        media.objects.forEach((image, index) => {
                            Object.assign(image, pose.objects.filter(e => e.media.id == image.id)[0]);

                            // grid items
                            this.imageryGrid.innerHTML += `
                                <a
                                    href="https://squidle.org/api/media/${image.media.id}?template=models/media/preview_single.html"
                                    target="_blank"
                                    onmouseenter="regionReport.focusMarker(${index})"
                                    onmouseleave="regionReport.focusMarker()"
                                >
                                    <img src="${image.path_best_thm}">
                                    <div class="grid-number">${index + 1}</div>
                                </a>`;

                            // marker
                            this.imageryMarkers.push(
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
                            this.imageryMarkers[this.imageryMarkers.length - 1].addTo(this.imageryMap);
                        });
                    }
                });
            }
        });
    }

    populateImageryMap({ squidle_url: squidleUrl, all_layers_boundary: allLayersBoundary, network: network, park: park, bounding_box: bounds }) {
        this.squidleUrl = squidleUrl;
        const imageryElement = document.getElementById(`region-report-imagery-${this.postId}`);

        if (this.squidleUrl) {
            imageryElement.innerHTML = `
                <div class="map" id="region-report-imagery-map-${this.postId}"></div>
                <div class="images">
                    <div class="image-grid" id="region-report-imagery-grid-${this.postId}"></div>
                    <div class="caption">${this.imageryCaption}</div>
                    <a href="#!" onclick="regionReport.refreshImagery()">Refresh images</a>
                </div>`;


            this.imageryGrid = document.getElementById(`region-report-imagery-grid-${this.postId}`);

            // set-up map
            this.imageryMap = L.map(`region-report-imagery-map-${this.postId}`, { maxZoom: 19, zoomControl: false });
            L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png").addTo(this.imageryMap);
            this.imageryMap._handlers.forEach(e => e.disable());

            this.refreshImagery();

            // map boundary layer
            L.tileLayer.wms(
                allLayersBoundary.server_url,
                {
                    layers: allLayersBoundary.layer_name,
                    transparent: true,
                    tiled: true,
                    format: "image/png",
                    styles: allLayersBoundary.style,
                    cql_filter: park ? `RESNAME='${park}'` : `NETNAME='${network.network}'`
                }
            ).addTo(this.imageryMap);

            // zoom to map extent
            this.imageryMap.fitBounds([[bounds.north, bounds.east], [bounds.south, bounds.west]]);
            window.addEventListener("resize", this.imageryMap.invalidateSize);
            if (navigator.userAgent.match(/chrome|chromium|crios/i))
                window.matchMedia('print').addEventListener(
                    "change",
                    e => {
                        if (e.matches) this.imageryMap.invalidateSize();
                    }
                );
        } else {
            imageryElement.innerText = "No imagery deployments found in this region";
        }
    }

    pressureAppState(pressureLayer, boundaryLayer, bounds, network, park) {
        return [
            "^ ",
            "~:autosave?",
            true,
            "~:filters",
            [
                "^ ",
                "~:layers",
                ""
            ],
            "~:state-of-knowledge",
            [
                "^ ",
                "~:boundaries",
                [
                    "^ ",
                    "~:active-boundary",
                    [
                        "^ ",
                        "~:id",
                        "amp",
                        "~:name",
                        "Australian Marine Parks"
                    ],
                    "~:active-boundary-layer",
                    [
                        "^ ",
                        "~:server_type",
                        boundaryLayer.server_type,
                        "~:category",
                        boundaryLayer.category,
                        "~:detail_layer",
                        null,
                        "~:organisation",
                        boundaryLayer.organisation,
                        "~:layer_name",
                        boundaryLayer.layer_name,
                        "~:server_url",
                        boundaryLayer.server_url,
                        "~:name",
                        boundaryLayer.name,
                        "~:info_format_type",
                        boundaryLayer.info_format_type,
                        "~:keywords",
                        boundaryLayer.keywords,
                        "~:style",
                        boundaryLayer.style,
                        "~:metadata_url",
                        boundaryLayer.metadata_url,
                        "~:id",
                        boundaryLayer.id,
                        "~:bounding_box",
                        [
                            "^ ",
                            "~:west",
                            boundaryLayer.bounding_box.west,
                            "~:south",
                            boundaryLayer.bounding_box.south,
                            "~:east",
                            boundaryLayer.bounding_box.east,
                            "~:north",
                            boundaryLayer.bounding_box.north
                        ],
                        "~:table_name",
                        boundaryLayer.table_name,
                        "~:data_classification",
                        boundaryLayer.data_classification,
                        "~:tooltip",
                        boundaryLayer.tooltip,
                        "~:legend_url",
                        boundaryLayer.legend_url,
                        "~:layer_type",
                        boundaryLayer.layer_type
                    ],
                    "~:amp",
                    [
                        "^ ",
                        "~:active-network",
                        [
                            "^ ",
                            "~:network",
                            network
                        ],
                        "~:active-park",
                        (
                            park ? [
                                "^ ",
                                "~:network",
                                network,
                                "~:park",
                                park
                            ] : null
                        ),
                    ]
                ]
            ],
            "~:story-maps",
            [
                "^ ",
                "~:featured-map",
                null,
                "~:open?",
                false
            ],
            "~:display",
            [
                "^ ",
                "~:left-drawer",
                true,
                "~:left-drawer-tab",
                "active-layers"
            ],
            "~:map",
            [
                "^ ",
                "~:bounds",
                [
                    "^ ",
                    "~:north",
                    bounds.north,
                    "~:south",
                    bounds.south,
                    "~:east",
                    bounds.east,
                    "~:west",
                    bounds.west
                ],
                "~:active",
                [
                    "~#list",
                    [
                        boundaryLayer.id,
                        pressureLayer
                    ]
                ],
                "~:active-base",
                1
            ]
        ]        
    }

    populatePressures({ pressures: pressures, app_boundary_layer: appBoundaryLayer, bounding_box: bounds, network: network, park: park }) {
        const pressuresTabs = document.getElementById(`region-report-pressures-categories-${this.postId}`);
        const pressuresTabContent = document.getElementById(pressuresTabs.dataset.tabContent);

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
                class="region-report-tab selected"
                data-tab="All"
                onclick="regionReport.toggleTab(this)"
            >
                All (${pressures.length})
            </div>`;

        // Create tab pane
        const tabPane = document.createElement("div");
        tabPane.className = "region-report-tab-pane pressures-grid selected";
        tabPane.dataset.tab = "All";
        pressures.forEach(pressure => {
            const appState = this.pressureAppState(pressure.layer, appBoundaryLayer, bounds, network.network, park);         

            tabPane.innerHTML += `
                <a
                    href="${this.mapUrlBase}/#${btoa(JSON.stringify(appState))}"
                    target="_blank"
                >
                    <img src="${this.pressurePreviewUrlBase}/${pressure.id}.png">
                </a>`;
        });
        pressuresTabContent.appendChild(tabPane);

        // Pressure category tabs
        Object.entries(groupedPressures).forEach(([category, pressures]) => {
            pressuresTabs.innerHTML += `
                <div
                    class="region-report-tab"
                    data-tab="${category}"
                    onclick="regionReport.toggleTab(this)"
                >
                    ${category} (${pressures.length})
                </div>`;

            // Create tab pane
            const tabPane = document.createElement("div");
            tabPane.className = "region-report-tab-pane pressures-grid";
            tabPane.dataset.tab = category;
            pressures.forEach(pressure => {
                const appState = this.pressureAppState(pressure.layer, appBoundaryLayer, bounds, network.network, park);         

                tabPane.innerHTML += `
                    <a
                        href="${this.mapUrlBase}/#${btoa(JSON.stringify(appState))}"
                        target="_blank"
                    >
                        <img src="${this.pressurePreviewUrlBase}/${pressure.id}.png">
                    </a>`;
            });
            pressuresTabContent.appendChild(tabPane);
        });
    }

    disablePrintCss(stylesheetId) {
        let href = document.getElementById(stylesheetId)?.href;
        if (href == null) return;

        let targetStylesheet = null;
    
        for (let i in document.styleSheets) {
            if (document.styleSheets[i].href == href) {
                targetStylesheet = document.styleSheets[i];
                break;
            }
        }
        
        for (let i in targetStylesheet.cssRules) {
            if (targetStylesheet.cssRules[i] instanceof CSSMediaRule && targetStylesheet.cssRules[i].conditionText == 'print')
                targetStylesheet.deleteRule(i);
        }
    }
}
