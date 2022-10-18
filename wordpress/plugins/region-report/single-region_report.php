<?php get_header(); ?>

<?php
    $habitat_statistics_url = get_post_meta(get_the_ID(), 'habitat_statistics_url', true);
    $bathymetry_statistics_url = get_post_meta(get_the_ID(), 'bathymetry_statistics_url', true);
    $habitat_observations_url = get_post_meta(get_the_ID(), 'habitat_observations_url', true);
    $region_report_data_url = get_post_meta(get_the_ID(), 'region_report_data_url', true);
?>

<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">

<script src="https://unpkg.com/vega@5.22.1/build/vega.js"></script>
<script src="https://unpkg.com/vega-lite@5.2.0/build/vega-lite.js"></script>
<script src="https://www.unpkg.com/vega-embed@6.21.0/build/vega-embed.js"></script>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js"></script>

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

        let habitatStatisticsUrl = "<?php echo $habitat_statistics_url; ?>";
        let bathymetryStatisticsUrl = "<?php echo $bathymetry_statistics_url; ?>";
        let habitatObservationsUrl = "<?php echo $habitat_observations_url; ?>";
        let regionReportDataUrl = "<?php echo $region_report_data_url; ?>";

        let pageLink = "<?php echo get_page_link(); ?>";
    </script>

    <!-- <header class="entry-header">
        <?php the_title( '<h1 class="entry-title">', '</h1>' ); ?>
    </header> -->

    <div class="entry-content">
        <section>
            <h3 class="region-report-region-heading" id="region-report-region-heading-<?php the_ID(); ?>">
            </h3>
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
                    <div class="region-report-outline-maps">
                        <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/3/3f/Placeholder_view_vector.svg/681px-Placeholder_view_vector.svg.png">
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
            <h2 id="region-report-known-heading-<?php the_ID(); ?>">What's known about this region?</h2>
            <script>
                postElement.addEventListener(
                    "regionReportData",
                    e => { document.getElementById(`region-report-known-heading-${postId}`).innerText = `What's known about the ${e.detail.park ? e.detail.park + " park" : e.detail.network.network + " network"}?`; }
                );
            </script>
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
                <h3>Observations</h3>
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
                            const squidleHead = document.createElement("span");
                            squidleHead.innerText = `${squidle.deployments} imagery deployments (${squidle.campaigns} campaigns)`;

                            const squidleList = document.createElement("ul");
                            addToList(squidleList, `Date range: ${squidle.start_date ?? "unknown"} to ${squidle.end_date ?? "unknown"}`);
                            addToList(squidleList, `Methods of collection:  ${squidle.method ?? "N/A"}`);
                            addToList(squidleList, `${squidle.images ?? 0} images collected`);
                            addToList(squidleList, `${squidle.total_annotations ?? 0} images annotations (${squidle.public_annotations ?? 0} public)`);
                            
                            const squidleItem = document.createElement("li");
                            squidleItem.appendChild(squidleHead);
                            squidleItem.appendChild(squidleList);

                            // global archive item
                            const globalArchiveHead = document.createElement("span");
                            globalArchiveHead.innerText = `${globalArchive.deployments} video deployments (${globalArchive.campaigns})`;

                            const globalArchiveList = document.createElement("ul");
                            addToList(globalArchiveList, `Date range: ${globalArchive.start_date ?? "unknown"} to ${globalArchive.end_date ?? "unknown"}`);
                            addToList(globalArchiveList, `Methods of collection: ${globalArchive.method ?? "N/A"}`);
                            addToList(globalArchiveList, `${globalArchive.video_time ?? 0} hours of video`);

                            const globalArchiveItem = document.createElement("li");
                            globalArchiveItem.appendChild(globalArchiveHead);
                            globalArchiveItem.appendChild(globalArchiveList);

                            // sediment item
                            const sedimentHead = document.createElement("span");
                            sedimentHead.innerText = `${sediment.samples} sediment samples (${sediment.analysed} analysed) from ${sediment.survey} surveys`;

                            const sedimentList = document.createElement("ul");
                            addToList(sedimentList, `Date range: ${sediment.start_date ?? "unknown"} to ${sediment.end_date ?? "unknown"}`);
                            addToList(sedimentList, `Methods of collection:  ${sediment.method ?? "N/A"}`);
                            
                            const sedimentItem = document.createElement("li");
                            sedimentItem.appendChild(sedimentHead);
                            sedimentItem.appendChild(sedimentList);

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
                <div class="region-report-research-effort">
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
                                    6,
                                    "State of bathymetry mapping"
                                );
                                starRating(
                                    starRatings.children[1],
                                    Math.round(e.detail.habitat_observations_state * 2),
                                    6,
                                    "State of habitat observations"
                                );
                                starRating(
                                    starRatings.children[2],
                                    Math.round(e.detail.habitat_state * 2),
                                    6,
                                    "State of habitat maps"
                                );
                            }
                        );
                    </script>

                    <div class="region-report-research-effort-quote" id="region-report-research-effort-quote-<?php the_ID(); ?>"></div>
                    <script>
                        postElement.addEventListener(
                            "regionReportData",
                            e => {
                                let quote = document.getElementById(`region-report-research-effort-quote-${postId}`)
                                quote.innerText = e.detail.state_summary;
                            }
                        );
                    </script>
                </div>
            </section>
        </section>

        <section class="region-report-contains">
            <h2 id="region-report-contains-heading-<?php the_ID(); ?>">What's in this region?</h2>
            <script>
                postElement.addEventListener(
                    "regionReportData",
                    e => { document.getElementById(`region-report-contains-heading-${postId}`).innerText = `What's in the ${e.detail.park ? e.detail.park + " park" : e.detail.network.network + " network"}?`; }
                );
            </script>
            
            <section class="region-report-mapped-habitat">
                <h3>Mapped Habitat</h3>
            </section>

            <section class="region-report-reserves">
                <h3>Reserves</h3>
            </section>
        </section>

        <section class="region-report-imagery">
            <h2>Imagery</h3>
        </section>

        <section class="region-report-pressures">
            <h2 id="region-report-pressures-heading-<?php the_ID(); ?>">What's happening in this region?</h2>
            <script>
                postElement.addEventListener(
                    "regionReportData",
                    e => { document.getElementById(`region-report-pressures-heading-${postId}`).innerText = `What's happening in the ${e.detail.park ? e.detail.park + " park" : e.detail.network.network + " network"}?`; }
                );
            </script>
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
