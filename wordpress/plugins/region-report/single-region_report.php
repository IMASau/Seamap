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

<article id="post-<?php the_ID(); ?>" <?php post_class(); ?>>
    <div class="entry-content">
        <section class="overview" id="overview-<?php the_ID(); ?>">

            <div class="region-summary">
                <div class="heading">
                    <h3>Report</h3>
                    <h2 id="region-report-region-heading-<?php the_ID(); ?>"></h2>
                </div>
                <div class="content"><?php the_content(); ?></div>
            </div>
        </section>

        <section class="overview-map">
            <div class="labeled-toggle">
                <div>All data</div>
                <label class="switch">
                    <input type="checkbox" onclick="regionReport.toggleMinimap(this.checked)">
                    <span class="switch-slider"></span>
                </label>
                <div>Public/analysed data</div>
            </div>
            <div class="map" id="region-report-overview-map-map-<?php the_ID(); ?>"></div>
            <div><div class="caption"><?php echo $overview_map_caption; ?></div></div>
        </section>

        <section class="known">
            <h2>What's known about the <?php echo $region_name; ?>?</h2>
            <div class="caption"><?php echo $known_caption; ?></div>

            <div class="statistics">
                <div>
                    <h3>Habitat</h3>
                    <div class="chart-table">
                        <div class="chart-container">
                            <div id="region-report-habitat-chart-<?php the_ID(); ?>"></div>
                        </div>
                        <div class="table-container">
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
                        </div>
                    </div>
                </div>

                <div>
                    <h3>Bathymetry</h3>
                    <div class="chart-table">
                        <div class="chart-container">
                            <div id="region-report-bathymetry-chart-<?php the_ID(); ?>"></div>
                        </div>
                        <div class="table-container">
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
                        </div>
                    </div>
                </div>
            </div>

            <div class="observations-and-research">
                <div>
                    <h3>Habitat Observations</h3>
                    <ul class="habitat-observations" id="region-report-habitat-observations-<?php the_ID(); ?>">
                        <li>
                            <span>0 imagery deployments <wbr>(0 campaigns)</span>
                        </li>

                        <li>
                            <span>0 video deployments <wbr>(0 campaigns)</span>
                        </li>

                        <li>
                            <span>0 sediment samples <wbr>(0 analysed) from 0 surveys</span>
                        </li>
                    </ul>
                </div>

                <div>
                    <h3>Research Effort</h3>
                    
                    <div id="region-report-research-effort-<?php the_ID(); ?>"></div>
                </div>
            </div>

            <div class="research-rating">
                <h2>Data Quality</h2>
                <div>
                    <div class="star-ratings" id="region-report-star-ratings-<?php the_ID(); ?>">
                        <div><!-- State of bathymetry mapping --></div>
                        <div><!-- State of habitat observations --></div>
                        <div><!-- State of habitat maps --></div>
                    </div>
                    <div class="feedback">
                        <h2>"</h2>
                        <h3>Feedback</h3>
                        <div class="quote" id="region-report-research-rating-quote-<?php the_ID(); ?>"></div>
                    </div>
                </div>
            </div>
        </section>

        <section class="imagery-and-pressures">
            <h2>What's in the <?php echo $region_name; ?>?</h2>
            <div class="caption"><?php echo $pressures_caption; ?></div>
        
            <section class="imagery">
                <h3>Imagery</h3>
                <div id="region-report-imagery-<?php the_ID(); ?>">Loading imagery deployment data...</div>
            </section>

            <section class="pressures">
                <h3>Pressures & Activities</h3>
                <div>
                    <div class="region-report-tabs" id="region-report-pressures-categories-<?php the_ID(); ?>" data-tab-content="region-report-pressures-tab-content-<?php the_ID(); ?>"></div>
                    <div class="tab-content" id="region-report-pressures-tab-content-<?php the_ID(); ?>"></div>
                </div>
            </section>
        </section>
    </div>
</article>
<script>
    const regionReport = new RegionReport({
        postId: "<?php the_ID(); ?>",
        habitatStatisticsUrlBase: <?php echo json_encode($habitat_statistics_url_base); ?>,
        bathymetryStatisticsUrlBase: <?php echo json_encode($bathymetry_statistics_url_base); ?>,
        habitatObservationsUrlBase: <?php echo json_encode($habitat_observations_url_base); ?>,
        researchEffortUrlBase: <?php echo json_encode($research_effort_url_base); ?>,
        regionReportDataUrlBase: <?php echo json_encode($region_report_data_url_base); ?>,
        pressurePreviewUrlBase: <?php echo json_encode($pressure_preview_url_base); ?>,
        mapUrlBase: <?php echo json_encode($map_url_base); ?>,
        networkName: "<?php echo $network_name; ?>",
        parkName: <?php echo empty($park_name) ? 'null' : "\"$park_name\""; ?>,
        imageryCaption: <?php echo json_encode($imagery_caption); ?>
    });
    regionReport.disablePrintCss("hcode-bootstrap-css");
    regionReport.disablePrintCss("hello-elementor-css");
</script>

<?php get_footer(); ?>
