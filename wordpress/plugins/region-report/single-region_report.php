<?php get_header(); ?>

<?php
    $network_name = "South-east";
    $park_name = NULL;
    $region_name = is_null($park_name) ? $network_name . " network" : $park_name . " park";
    
    $habitat_statistics_url = get_post_meta(get_the_ID(), 'habitat_statistics_url', true);
    $bathymetry_statistics_url = get_post_meta(get_the_ID(), 'bathymetry_statistics_url', true);
    $habitat_observations_url = get_post_meta(get_the_ID(), 'habitat_observations_url', true);
?>

<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">

<script src="https://unpkg.com/vega@5.22.1/build/vega.js"></script>
<script src="https://unpkg.com/vega-lite@5.2.0/build/vega-lite.js"></script>
<script src="https://www.unpkg.com/vega-embed@6.21.0/build/vega-embed.js"></script>
<script src="https://ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js"></script>

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
    </script>

    <header class="entry-header">
        <?php the_title( '<h1 class="entry-title">', '</h1>' ); ?>
    </header>

    <div class="entry-content">
        <section class="region-report-outline">
            <h3><?php echo $network_name . (is_null($park_name) ? "" : " > " . $park_name); ?></h3>
            <?php the_content(); ?>
            <div class="region-report-outline-maps">
                <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/3/3f/Placeholder_view_vector.svg/681px-Placeholder_view_vector.svg.png">
            </div>
        </section>

        <section class="region-report-known">
            <h2>What's known about the <?php echo $region_name; ?>?</h2>
            <section>
                <h3>Habitat</h3>
                <div class="region-report-chart-table">
                    <div>
                        <div class="region-report-chart" id="region-report-habitat-chart-<?php echo the_ID(); ?>"></div>
                        <script>
                            postElement.addEventListener(
                                "habitatStatistics",
                                e => {
                                    const values = e.detail;
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

                            <tbody id="region-report-habitat-table-<?php echo the_ID(); ?>"></tbody>
                        </table>
                        <script>
                            postElement.addEventListener(
                                "habitatStatistics",
                                e => {
                                    const values = e.detail;
                                    const table = document.getElementById(`region-report-habitat-table-${postId}`);

                                    values.forEach( habitat => {
                                        const row = table.insertRow();

                                        row.insertCell().innerHTML = habitat.habitat;
                                        row.insertCell().innerHTML = habitat.area.toFixed(1);
                                        row.insertCell().innerHTML = habitat.mapped_percentage.toFixed(1);
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
                        <div class="region-report-chart" id="region-report-bathymetry-chart-<?php echo the_ID(); ?>"></div>
                        <script>
                            postElement.addEventListener(
                                "bathymetryStatistics",
                                e => {
                                    const values = e.detail;
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

                            <tbody id="region-report-bathymetry-table-<?php echo the_ID(); ?>"></tbody>
                        </table>
                        <script>
                            postElement.addEventListener(
                                "bathymetryStatistics",
                                e => {
                                    const values = e.detail;
                                    const table = document.getElementById(`region-report-bathymetry-table-${postId}`);

                                    values.forEach( bathymetry => {
                                        const row = table.insertRow();

                                        row.insertCell().innerHTML = bathymetry.resolution;
                                        row.insertCell().innerHTML = bathymetry.area.toFixed(1);
                                        row.insertCell().innerHTML = bathymetry.mapped_percentage.toFixed(1);
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
                <!-- TODO: Use habitat observations data -->
                <!-- TODO: Confirm with Emma the exact content of these dropdowns -->
                <div class="region-report-habitat-observations-breakdown">
                    <ul>
                        <li class="tree-caret">
                            <span>1690 imagery deployments (73 campaigns)</span>
                            <ul>
                                <li>Date range: 2009-02-01 to 2022-04-17</li>
                                <li>Methods of collection: ACFR AUV Holt, CSIRO O&A MRITC Towed Stereo Camera, GlobalArchive Stereo-BRUVs, IMOS AUV Nimbus, IMOS AUV Sirius, NESP Towed Camera, RLS DIVER Photos, SOI ROV Subastian</li>
                                <li>1907422 images collected</li>
                                <li>766824 image annotations (508833 public)</li>
                            </ul>
                        </li>

                        <li class="tree-caret">
                            <span>2549 video deployments (48 campaigns)</span>
                            <ul>
                                <li>Date range: 1976-07-29 to 2021-03-11</li>
                                <li>Methods of collection: BRUVs, stereo-BOSS, stereo-BRUVs</li>
                                <li>982 hours of video</li>
                            </ul>
                        </li>

                        <li class="tree-caret">
                            <span>9084 sediment samples (3725 analysed) from 157 surveys</span>
                            <ul>
                                <li>Date range: 1905-05-21 to 2020-12-03,</li>
                                <li>Methods of collection: core, seabed sample</li>
                            </ul>
                        </li>
                    </ul>
                </div>
            </section>

            <section>
                <h3>Research Effort</h3>
                <div class="region-report-known-classified">
                    <div>
                        <!-- TODO: Determine how this is calculated from habitat statistics -->
                        <div>20%</div>
                        <div>Shelf habitat classified</div>
                    </div>
                    
                    <!-- TODO: Star ratings -->
                    <div id="region-report-star-ratings-<?php echo the_ID(); ?>">
                        <div><!-- State of bathymetry mapping --></div>
                        <div><!-- State of habitat observations --></div>
                        <div><!-- State of habitat maps --></div>
                    </div>
                    <script>
                        let starRatings = document.getElementById(`region-report-star-ratings-${postId}`)
                        starRating(starRatings.children[0], 6, 6, "State of bathymetry mapping");
                        starRating(starRatings.children[1], 3, 6, "State of habitat observations");
                        starRating(starRatings.children[2], 2, 6, "State of habitat maps");
                    </script>

                    <div>
                        "You have good imagery and bathymetry coverage. Invest in modelling"
                        <br>– SA Team
                    </div>
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

        <section class="region-report-imagery">
            <h2>Imagery</h3>
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
    </script>
</article>

<script>
    let toggler = document.getElementsByClassName("tree-caret");

    for (let i = 0; i < toggler.length; i++) {
        toggler[i].children[1].classList.add("tree-nested");

        toggler[i].children[0].addEventListener("click", function () {
            this.parentElement.querySelector(".tree-nested").classList.toggle("tree-active");
            this.classList.toggle("tree-caret-down");
        });
    }
</script>

<?php get_footer(); ?>
