class RegionReport {
    postId = null;
    squidleAnnotationsDataUrl = null;
    pressurePreviewUrlBase = null;
    mapUrlBase = null;
    squidleCaption = null;

    networkResearchEffort = null;
    parkResearchEffort = null;

    // overview map
    overviewMap = null;
    allLayers = L.layerGroup();
    allLayersBoundary = null;
    publicLayers = L.layerGroup();
    publicLayersBoundary = null;
    overviewMapHyperlink = null;
    minimapState = 'all';
    appBoundaryLayer = null;
    bounds = null;
    network = null;
    park = null;
    minimapLayers = {};
    boundary = null;

    // imagery map
    imageryMap = null;
    imageryMarkers = [];
    imageryDepths = [];
    imageryFilterDepth = null;
    imageryFilterHighlights = false;
    imageryMinimapLayers = {};

    // url templates
    squidlePoseUrlTemplate = null;
    squidlePoseFilterMinDepthTemplate = null;
    squidlePoseFilterMaxDepthTemplate = null;
    squidlePoseFilterHighlightsTemplate = null;
    squidleMediaUrlTemplate = null;
    annotationsLinkUrlTemplate = null;

    constructor({
        postId: postId,
        habitatStatisticsUrlBase: habitatStatisticsUrlBase,
        bathymetryStatisticsUrlBase: bathymetryStatisticsUrlBase,
        habitatObservationsUrlBase: habitatObservationsUrlBase,
        researchEffortUrlBase: researchEffortUrlBase,
        regionReportDataUrlBase: regionReportDataUrlBase,
        squidleAnnotationsDataUrl: squidleAnnotationsDataUrl,
        pressurePreviewUrlBase: pressurePreviewUrlBase,
        mapUrlBase: mapUrlBase,
        networkName: networkName,
        parkName: parkName,
        squidleCaption: squidleCaption,
        squidlePoseUrlTemplate: squidlePoseUrlTemplate,
        squidlePoseFilterMinDepthTemplate: squidlePoseFilterMinDepthTemplate,
        squidlePoseFilterMaxDepthTemplate: squidlePoseFilterMaxDepthTemplate,
        squidlePoseFilterHighlightsTemplate: squidlePoseFilterHighlightsTemplate,
        squidleMediaUrlTemplate: squidleMediaUrlTemplate,
        annotationsLinkUrlTemplate: annotationsLinkUrlTemplate
    }) {
        L.Control.SingleLayers = L.Control.Layers.extend({
            onAdd: function (map) {
                this._map = map;
                map.on('overlayadd', this._update, this);
                map.on('overlayremove', this._update, this);
                return L.Control.Layers.prototype.onAdd.call(this, map);
            },
            onRemove: function (map) {
                map.on('overlayadd', this._update, this);
                map.on('overlayremove', this._update, this);
                L.Control.Layers.prototype.onRemove.call(this, map);
            },
            _addItem: function (obj) {
                var item = L.Control.Layers.prototype._addItem.call(this, obj);

                // Check if another overlay is active
                let otherActive = false;
                this._layers.forEach(
                    objOther => {
                        // If another overlay is active
                        if (objOther != obj && objOther.overlay && this._map.hasLayer(objOther.layer)) {
                            otherActive = true;
                        }
                    }
                );

                if (otherActive) {
                    item.children[0].innerHTML = `<input type="checkbox" class="leaflet-control-layers-selector" disabled><span> ${obj.name}</span>`
                }

                return item;
            }
        });

        L.control.singleLayers = function (baseLayers, overlays, options) {
            return new L.Control.SingleLayers(baseLayers, overlays, options);
        }

        L.Control.Legend = L.Control.extend({
            setLegend: function (layer) {
                const url = layer.metadata?.layer?.legend_url ?? `${layer._url}?REQUEST=GetLegendGraphic&LAYER=${layer.options.layers}&TRANSPARENT=${layer.options.transparent}&SERVICE=WMS&VERSION=1.1.1&FORMAT=image/png`
                this._container.innerHTML = `<img src="${url}">`;
                this._container.style.display = 'block';
            },
            clearLegend: function () {
                this._container.innerHTML = '';
                this._container.style.display = 'none';
            },
            onAdd: function (map) {
                const control = L.DomUtil.create('div', 'leaflet-minimap-legend');
                control.style.display = 'none';
                map.on('overlayadd', e => this.setLegend(e.layer), this);
                map.on('overlayremove', this.clearLegend, this);
                return control;
            },
            onRemove: function (map) { }
        });

        L.control.legend = function (options) {
            return new L.Control.Legend(options);
        }

        this.postId = postId;
        this.squidleAnnotationsDataUrl = squidleAnnotationsDataUrl;
        this.pressurePreviewUrlBase = pressurePreviewUrlBase;
        this.mapUrlBase = mapUrlBase;
        this.squidleCaption = squidleCaption;

        // url templates
        this.squidlePoseUrlTemplate = squidlePoseUrlTemplate;
        this.squidlePoseFilterMinDepthTemplate = squidlePoseFilterMinDepthTemplate;
        this.squidlePoseFilterMaxDepthTemplate = squidlePoseFilterMaxDepthTemplate;
        this.squidlePoseFilterHighlightsTemplate = squidlePoseFilterHighlightsTemplate;
        this.squidleMediaUrlTemplate = squidleMediaUrlTemplate;
        this.annotationsLinkUrlTemplate = annotationsLinkUrlTemplate;

        this.setupOverviewMap();
        this.setupImageryMap();
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
                this.appBoundaryLayer = response.app_boundary_layer;
                this.bounds = response.bounding_box;
                this.network = response.network.network;
                this.park = response.park;
                this.boundary = response.boundary;
                this.imageryDepths = response.depths;
                response.minimap_layers.forEach(
                    minimapLayer => {
                        this.minimapLayers[minimapLayer.label] = L.tileLayer.wms(
                            minimapLayer.layer.server_url,
                            {
                                layers: minimapLayer.layer.layer_name,
                                transparent: true,
                                tiled: true,
                                format: "image/png",
                                styles: minimapLayer.layer.style ?? "",
                                pane: 'control'
                            }
                        );
                        this.minimapLayers[minimapLayer.label].metadata = { "layer": minimapLayer.layer };

                        this.imageryMinimapLayers[minimapLayer.label] = L.tileLayer.wms(
                            minimapLayer.layer.server_url,
                            {
                                layers: minimapLayer.layer.layer_name,
                                transparent: true,
                                tiled: true,
                                format: "image/png",
                                styles: minimapLayer.layer.style ?? "",
                                pane: 'control'
                            }
                        );
                        this.imageryMinimapLayers[minimapLayer.label].metadata = { "layer": minimapLayer.layer };
                    }
                );

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

    templateStringFill(text, data) {
        return text.replace(
            /%(\w+)%/g,
            function (match, key) {
                return data[key] ?? match;
            }
        );
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
                            <div class="tooltip">Finalised public curated image annotations</div>
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
             </div>
             <div class="research-effort-legend">
                <h4>Legend</h4>
                <div class="research-effort-legend-entry">
                    <div style="background:#3C67BC"></div>
                    <div>Imagery (campaigns)</div>
                </div>
                <div class="research-effort-legend-entry">
                    <div style="background:#EA722B"></div>
                    <div>Video (campaigns)</div>
                </div>
                <div class="research-effort-legend-entry">
                    <div style="background:#9B9B9B"></div>
                    <div>Sediment (surveys)</div>
                </div>
                <div class="research-effort-legend-entry">
                    <div style="background:#FFB800"></div>
                    <div>Bathymetry (surveys)</div>
                </div>
             </div>`;
    }

    vegaMultiHistogram(values, tickStep, title) {
        let start = Math.min(...values.map(e => e.year));
        if (start % tickStep != 0)
            start -= start % tickStep;
        let end = new Date().getFullYear();
        if (end % tickStep != 0)
            end += tickStep - end % tickStep;

        const filledValues = Array.from(
            { length: end - start },
            (_, i) => {
                const year = start + i;
                let data = values.filter(e => e.year == year);
                if (data.length > 0)
                    return data[0];

                return {
                    year: year,
                    imagery_count: 0,
                    video_count: 0,
                    sediment_count: 0,
                    bathymetry_count: 0
                }
            }
        );

        return {
            width: "container",
            title: title,
            background: "transparent",
            data: { values: filledValues },
            mark: { type: "bar" },
            encoding: {
                x: {
                    field: "year",
                    type: "quantitative",
                    title: "Year",
                    axis: {
                        values: Array.from(
                            { length: (end - start) / tickStep },
                            (_, i) => i * tickStep + start
                        ),
                        format: "r",
                        labelAngle: 0,
                        labelFlush: false,
                        grid: false
                    },
                    scale: {
                        domain: [
                            start - 0.5,
                            new Date().getFullYear() + 0.5
                        ]
                    }
                },
                y: {
                    title: "Survey Effort",
                    axis: { tickMinStep: 1 }
                }
            },
            layer: [
                {
                    mark: {
                        type: "bar",
                        color: "#3C67BC"
                    },
                    transform: [
                        {
                            calculate: "datum.year - 0.5",
                            as: "start"
                        },
                        {
                            calculate: "datum.year - 0.25",
                            as: "end"
                        }
                    ],
                    encoding: {
                        y: {
                            field: "imagery_count",
                            type: "quantitative"
                        },
                        x: { field: "start" },
                        x2: { field: "end" }
                    }
                },
                {
                    mark: {
                        type: "bar",
                        color: "#EA722B"
                    },
                    transform: [
                        {
                            calculate: "datum.year - 0.25",
                            as: "start"
                        },
                        {
                            calculate: "datum.year",
                            as: "end"
                        }
                    ],
                    encoding: {
                        y: {
                            field: "video_count",
                            type: "quantitative"
                        },
                        x: { field: "start" },
                        x2: { field: "end" }
                    }
                },
                {
                    mark: {
                        type: "bar",
                        color: "#9B9B9B"
                    },
                    transform: [
                        {
                            calculate: "datum.year",
                            as: "start"
                        },
                        {
                            calculate: "datum.year + 0.25",
                            as: "end"
                        }
                    ],
                    encoding: {
                        y: {
                            field: "sediment_count",
                            type: "quantitative"
                        },
                        x: { field: "start" },
                        x2: { field: "end" }
                    }
                },
                {
                    mark: {
                        type: "bar",
                        color: "#FFB800"
                    },
                    transform: [
                        {
                            calculate: "datum.year + 0.25",
                            as: "start"
                        },
                        {
                            calculate: "datum.year + 0.5",
                            as: "end"
                        }
                    ],
                    encoding: {
                        y: {
                            field: "bathymetry_count",
                            type: "quantitative"
                        },
                        x: { field: "start" },
                        x2: { field: "end" }
                    }
                },
                {
                    mark: {
                        type: "rule",
                        stroke: "gray",
                        strokeWidth: 0.3,
                    },
                    transform: [
                        {
                            filter: `datum.year % ${tickStep} == 0`
                        },
                        {
                            calculate: `datum.year + ${tickStep / 2}`,
                            as: "rule"
                        }
                    ],
                    encoding: {
                        x: { field: "rule" },
                    }
                }
            ]
        }
    }

    toggleResearchEffort(showNetwork) {
        // build values
        const values = (showNetwork ? this.networkResearchEffort : this.parkResearchEffort);
        const filteredValues = values.filter(e => e.year >= 2000);

        vegaEmbed(
            `#region-report-research-effort-1-${this.postId}`,
            this.vegaMultiHistogram(values, 5, "Full Timeseries"),
            { actions: false }
        );
        vegaEmbed(
            `#region-report-research-effort-2-${this.postId}`,
            this.vegaMultiHistogram(filteredValues, 2, "2000 to Present"),
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
        this.overviewMap.createPane('main');
        this.overviewMap.createPane('control');
        this.overviewMap.getPane('main').style.zIndex = 800;
        this.overviewMap.getPane('control').style.zIndex = 400;
        this.overviewMapHyperlink = document.getElementById(`region-report-overview-map-hyperlink-${this.postId}`);
    }

    overviewMapAppState() {
        let layers = [];

        Object.values(this.minimapLayers).forEach(
            layer => {
                if (this.overviewMap.hasLayer(layer)) {
                    layers.push(layer.metadata.layer);
                }
            }
        );

        layers.push(this.appBoundaryLayer);

        switch (this.minimapState) {
            case 'all':
                layers = layers.concat(this.allLayers.metadata?.layers);
                break;
            case 'public':
                layers = layers.concat(this.publicLayers.metadata?.layers);
                break;
        }

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
                        `~:${this.appBoundaryLayer.server_type.toLowerCase()}`,
                        "~:category",
                        `~:${this.appBoundaryLayer.category.toLowerCase()}`,
                        "~:detail_layer",
                        this.appBoundaryLayer.detail_layer,
                        "~:organisation",
                        this.appBoundaryLayer.organisation,
                        "~:layer_name",
                        this.appBoundaryLayer.layer_name,
                        "~:server_url",
                        this.appBoundaryLayer.server_url,
                        "~:name",
                        this.appBoundaryLayer.name,
                        "~:info_format_type",
                        this.appBoundaryLayer.info_format_type,
                        "~:keywords",
                        this.appBoundaryLayer.keywords,
                        "~:style",
                        this.appBoundaryLayer.style,
                        "~:metadata_url",
                        this.appBoundaryLayer.metadata_url,
                        "~:id",
                        this.appBoundaryLayer.id,
                        "~:bounding_box",
                        [
                            "^ ",
                            "~:west",
                            this.appBoundaryLayer.bounding_box.west,
                            "~:south",
                            this.appBoundaryLayer.bounding_box.south,
                            "~:east",
                            this.appBoundaryLayer.bounding_box.east,
                            "~:north",
                            this.appBoundaryLayer.bounding_box.north
                        ],
                        "~:table_name",
                        this.appBoundaryLayer.table_name,
                        "~:data_classification",
                        this.appBoundaryLayer.data_classification,
                        "~:tooltip",
                        this.appBoundaryLayer.tooltip,
                        "~:legend_url",
                        this.appBoundaryLayer.legend_url,
                        "~:layer_type",
                        `~:${this.appBoundaryLayer.layer_type.toLowerCase()}`
                    ],
                    "~:amp",
                    [
                        "^ ",
                        "~:active-network",
                        [
                            "^ ",
                            "~:network",
                            this.network
                        ],
                        "~:active-park",
                        (
                            this.park ? [
                                "^ ",
                                "~:network",
                                this.network,
                                "~:park",
                                this.park
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
                    this.bounds.north,
                    "~:south",
                    this.bounds.south,
                    "~:east",
                    this.bounds.east,
                    "~:west",
                    this.bounds.west
                ],
                "~:active",
                [
                    "~#list",
                    layers.map(e => e.id)
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
                        styles: layer.style ?? "",
                        cql_filter: `Network='${network.network}'` + (park ? ` AND Park='${park}'` : ""),
                        pane: 'main'
                    }
                ).addTo(this.allLayers);
            }
        );
        this.allLayers.metadata = { "layers": allLayers };

        // all layers boundary
        this.allLayersBoundary = L.tileLayer.wms(
            allLayersBoundary.server_url,
            {
                layers: allLayersBoundary.layer_name,
                transparent: true,
                tiled: true,
                format: "image/png",
                styles: allLayersBoundary.style ?? "",
                cql_filter: park ? `RESNAME='${park}'` : `NETNAME='${network.network}'`,
                pane: 'main'
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
                        styles: layer.style ?? "",
                        cql_filter: `Network='${network.network}'` + (park ? ` AND Park='${park}'` : ""),
                        pane: 'main'
                    }
                ).addTo(this.publicLayers);
            }
        );
        this.publicLayers.metadata = { "layers": publicLayers };

        // public layers boundary
        this.publicLayersBoundary = L.tileLayer.wms(
            publicLayersBoundary.server_url,
            {
                layers: publicLayersBoundary.layer_name,
                transparent: true,
                tiled: true,
                format: "image/png",
                styles: publicLayersBoundary.style ?? "",
                cql_filter: park ? `RESNAME='${park}'` : `NETNAME='${network.network}'`,
                pane: 'main'
            }
        );

        // map controls
        L.control.singleLayers(null, this.minimapLayers).addTo(this.overviewMap);
        L.control.legend({ position: 'bottomleft' }).addTo(this.overviewMap);

        // set up map
        this.overviewMap._handlers.forEach(function (handler) {
            handler.disable();
        });

        switch (this.minimapState) {
            case 'all':
                this.overviewMap.addLayer(this.allLayersBoundary);
                this.overviewMap.addLayer(this.allLayers);
                break;
            case 'public':
                this.overviewMap.addLayer(this.publicLayersBoundary);
                this.overviewMap.addLayer(this.publicLayers);
                break;
        }

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

        // set up hyperlink
        const appState = this.overviewMapAppState();
        this.overviewMapHyperlink.href = `${this.mapUrlBase}/#${btoa(JSON.stringify(appState))}`;

        this.overviewMap.on(
            'overlayadd',
            () => {
                const appState = this.overviewMapAppState();
                this.overviewMapHyperlink.href = `${this.mapUrlBase}/#${btoa(JSON.stringify(appState))}`;
            },
            this
        );
        this.overviewMap.on(
            'overlayremove',
            () => {
                const appState = this.overviewMapAppState();
                this.overviewMapHyperlink.href = `${this.mapUrlBase}/#${btoa(JSON.stringify(appState))}`;
            },
            this
        );
    }

    toggleMinimap(minimapState) {
        this.minimapState = minimapState;
        this.overviewMap.removeLayer(this.allLayers);
        this.overviewMap.removeLayer(this.allLayersBoundary);
        this.overviewMap.removeLayer(this.publicLayers);
        this.overviewMap.removeLayer(this.publicLayersBoundary);

        switch (this.minimapState) {
            case 'all':
                this.overviewMap.addLayer(this.allLayersBoundary);
                this.overviewMap.addLayer(this.allLayers);
                break;
            case 'public':
                this.overviewMap.addLayer(this.publicLayersBoundary);
                this.overviewMap.addLayer(this.publicLayers);
                break;
        }

        const appState = this.overviewMapAppState();
        this.overviewMapHyperlink.href = `${this.mapUrlBase}/#${btoa(JSON.stringify(appState))}`;
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
                    <h2>Marine Parks</h2>
                    <div class="caption">View reports for Marine Parks within this Network</div>
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
        if (this.imageryMap == null) return;
        if (this.boundary == null) return;

        // initiate loading
        const imageryElement = document.getElementById(`region-report-imagery-annotations-${this.postId}`);
        imageryElement.innerHTML = "Loading imagery deployment data...";
        this.imageryMarkers.forEach(marker => this.imageryMap.removeLayer(marker));
        this.imageryMarkers = [];

        const minDepth = this.imageryDepths.find(e => e.zonename == this.imageryFilterDepth)?.min;
        const maxDepth = this.imageryDepths.find(e => e.zonename == this.imageryFilterDepth)?.max;

        const imageryUrl = this.templateStringFill(
            this.squidlePoseUrlTemplate,
            {
                'boundary': JSON.stringify(this.boundary),
                'min_depth': minDepth ? this.templateStringFill(this.squidlePoseFilterMinDepthTemplate, { 'min_depth': minDepth }) : '',
                'max_depth': maxDepth ? this.templateStringFill(this.squidlePoseFilterMaxDepthTemplate, { 'max_depth': maxDepth }) : '',
                'highlights': this.imageryFilterHighlights ? this.squidlePoseFilterHighlightsTemplate : ''
            }
        );


        // retrieve data
        $.ajax(imageryUrl, {
            dataType: "json",
            success: pose => {
                if (pose.objects.length > 0) {
                    // populate imagery grid
                    imageryElement.innerHTML = `
                        <div class="images">
                            <div class="image-grid" id="region-report-imagery-grid-${this.postId}"></div>
                            <div class="caption">${this.squidleCaption}</div>
                            <a href="#!" onclick="regionReport.refreshImagery()">Refresh imagery</a>
                        </div>
                        <div id="region-report-annotations-${this.postId}"></div>`;

                    const params = {
                        network: this.network,
                        depth_zone: this.imageryFilterDepth,
                        highlights: this.imageryFilterHighlights
                    };
                    if (this.park) params.park = this.park;

                    $.ajax(this.squidleAnnotationsDataUrl, {
                        dataType: "json",
                        data: params,
                        success: annotations => {
                            const annotationsElement = document.getElementById(`region-report-annotations-${this.postId}`);
                            const annotationsData = annotations[0]?.annotations_data?.replace(`<i class="fa fa-info-circle"/>`, `<i class="fa fa-info-circle"></i>`);
                            annotationsElement.innerHTML = annotationsData
                                ? `
                                    <a
                                        href="${this.templateStringFill(this.annotationsLinkUrlTemplate, {
                                            'network': this.network ?? '',
                                            'park': this.park ?? '',
                                            'depth_zone': this.imageryFilterDepth ?? '',
                                            'highlights': this.imageryFilterHighlights ?? '',
                                            'min_depth': minDepth ?? '',
                                            'max_depth': maxDepth ?? ''
                                        })}"
                                        target="_blank"
                                        class="annotations-link"
                                    >
                                        <h4>
                                            Dominant Annotations for the Region <span class="tooltip-parent">
                                                <i class="fa fa-info-circle"></i>
                                                <div class="tooltip">Publicly available, finalised annotations from imagery scored in this region</div>
                                            </span>
                                        </h4>
                                        ${annotationsData}
                                    </a>`
                                : `
                                    <div class="bp3-non-ideal-state">
                                        <div class="bp3-non-ideal-state-visual">
                                            <span class="bp3-icon bp3-icon-info-sign"></span>
                                        </div>
                                        <h4 class="bp3-heading">No Data</h4>
                                        <div>No public image annotations were found for this region.</div>
                                    </div>`;
                        }
                    })

                    const imageryGrid = document.getElementById(`region-report-imagery-grid-${this.postId}`);
                    pose.objects.forEach((poseObject, index) => {
                        // grid items
                        imageryGrid.innerHTML += `
                            <a
                                href="${this.templateStringFill(this.squidleMediaUrlTemplate, { 'media_id': poseObject.media.id })}"
                                target="_blank"
                                onmouseenter="regionReport.focusMarker(${index})"
                                onmouseleave="regionReport.focusMarker()"
                            >
                                <img src="${poseObject.media.path_best_thm}">
                                <div class="grid-number">${index + 1}</div>
                            </a>`;

                        // marker
                        this.imageryMarkers.push(
                            new L.Marker(
                                [poseObject.lat, poseObject.lon],
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
                                    }),
                                    pane: 'main'
                                }
                            )
                        );
                        this.imageryMarkers[index].addTo(this.imageryMap);
                    });
                } else {
                    imageryElement.innerHTML = `
                        <div class="bp3-non-ideal-state">
                            <div class="bp3-non-ideal-state-visual">
                                <span class="bp3-icon bp3-icon-info-sign"></span>
                            </div>
                            <h4 class="bp3-heading">No Data</h4>
                            <div>No public imagery was found for this region.</div>
                        </div>`;
                }
            }
        });
    }

    setImageryFilterDepth(depth) {
        this.imageryFilterDepth = depth;
        this.refreshImagery();
    }

    setImageryFilterHighlights(highlights) {
        this.imageryFilterHighlights = highlights;
        this.refreshImagery();
    }

    setupImageryMap() {
        // set-up map
        this.imageryMap = L.map(`region-report-imagery-map-${this.postId}`, { maxZoom: 19, zoomControl: false });
        L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png").addTo(this.imageryMap);
        this.imageryMap.createPane('main');
        this.imageryMap.createPane('control');
        this.imageryMap.getPane('main').style.zIndex = 800;
        this.imageryMap.getPane('control').style.zIndex = 400;
        this.imageryMap._handlers.forEach(e => e.disable());
    }

    populateImageryMap({ all_layers_boundary: allLayersBoundary, network: network, park: park, bounding_box: bounds }) {
        // map boundary layer
        L.tileLayer.wms(
            allLayersBoundary.server_url,
            {
                layers: allLayersBoundary.layer_name,
                transparent: true,
                tiled: true,
                format: "image/png",
                styles: allLayersBoundary.style ?? "",
                cql_filter: park ? `RESNAME='${park}'` : `NETNAME='${network.network}'`,
                pane: 'main'
            }
        ).addTo(this.imageryMap);

        const imageryDepthElement = document.getElementById(`region-report-imagery-depth-${this.postId}`);
        this.imageryDepths.forEach(
            depth => {
                imageryDepthElement.innerHTML += `<option value="${depth.zonename}">${depth.zonename}</option>`;
            }
        );

        // map controls
        L.control.singleLayers(null, this.imageryMinimapLayers).addTo(this.imageryMap);
        L.control.legend({ position: 'bottomleft' }).addTo(this.imageryMap);

        // zoom to map extent
        this.imageryMap.fitBounds([[bounds.north, bounds.east], [bounds.south, bounds.west]]);
        window.addEventListener("resize", this.imageryMap.invalidateSize);
        if (navigator.userAgent.match(/chrome|chromium|crios/i)) {
            window.matchMedia('print').addEventListener(
                "change",
                e => {
                    if (e.matches) this.imageryMap.invalidateSize();
                }
            );
        }

        this.refreshImagery();
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
                        `~:${boundaryLayer.server_type.toLowerCase()}`,
                        "~:category",
                        `~:${boundaryLayer.category.toLowerCase()}`,
                        "~:detail_layer",
                        boundaryLayer.detail_layer,
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
                        `~:${boundaryLayer.layer_type.toLowerCase()}`
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
                        pressureLayer,
                        boundaryLayer.id
                    ]
                ],
                "~:active-base",
                1
            ]
        ]
    }

    pressurePreview(pressure, appBoundaryLayer, bounds, network, park) {
        const appState = this.pressureAppState(pressure.layer, appBoundaryLayer, bounds, network.network, park);

        return `
            <a
                href="${this.mapUrlBase}/#${btoa(JSON.stringify(appState))}"
                target="_blank"
            >
                <img src="${this.pressurePreviewUrlBase}/${pressure.id}.png">
                <div class="pressure-label">${pressure.label}</div>
            </a>`;
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
        pressures.forEach(pressure => tabPane.innerHTML += this.pressurePreview(pressure, appBoundaryLayer, bounds, network, park));
        pressuresTabContent.appendChild(tabPane);

        // Pressure category tabs
        Object.entries(groupedPressures)
            .sort((a, b) => {
                if (/^cumulative.*$/i.test(a[0])) return 1;
                if (/^cumulative.*$/i.test(b[0])) return -1;
                return a[0] > b[0] ? 1 : -1;
            })
            .forEach(([category, pressures]) => {
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
                pressures.forEach(pressure => tabPane.innerHTML += this.pressurePreview(pressure, appBoundaryLayer, bounds, network, park));
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
