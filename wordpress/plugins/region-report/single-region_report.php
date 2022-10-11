<?php get_header(); ?>

<?php
    $network_name = "South-east";
    $park_name = NULL;
    $region_name = is_null($park_name) ? $network_name . " network" : $park_name . " park";
?>

<article id="post-<?php the_ID(); ?>" <?php post_class(); ?>>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/css/bootstrap.min.css">

    <script src="https://unpkg.com/vega@5.22.1/build/vega.js"></script>
    <script src="https://unpkg.com/vega-lite@5.2.0/build/vega-lite.js"></script>
    <script src="https://www.unpkg.com/vega-embed@6.21.0/build/vega-embed.js"></script>
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.4.1/js/bootstrap.min.js"></script>

    <script>
        let postId = "<?php the_ID(); ?>";
        let postElement = document.getElementById(`post-${postId}`);
    </script>

    <header class="entry-header">
        <?php the_title( '<h1 class="entry-title">', '</h1>' ); ?>
    </header>

    <div class="entry-content">
        <section class="region-report-outline">
            <div>
                <h3><?php echo $network_name . (is_null($park_name) ? "" : " > " . $park_name); ?></h3>
                <?php the_content(); ?>
                <div class="region-report-outline-maps"></div>
            </div>

            <div class="region-report-breakdowns">
                <div class="region-report-classified">
                    <!-- TODO: Determine how this is calculated from habitat statistics -->
                    <div>20%</div>
                    <div>Shelf habitat classified</div>
                    <div>Top 10 coverage in Aus networks</div>
                </div>
                
                <div class="region-report-habitat-breakdown">
                    <ul class="nav nav-tabs">
                        <li class="active"><a data-toggle="tab" href="#region-report-habitat-breakdown-<?php echo the_ID(); ?>-1">Chart</a></li>
                        <li><a data-toggle="tab" href="#region-report-habitat-breakdown-<?php echo the_ID(); ?>-2">Breakdown</a></li>
                    </ul>

                    <div class="tab-content">
                        <div id="region-report-habitat-breakdown-<?php echo the_ID(); ?>-1" class="tab-pane fade in active">
                            <div class="region-report-habitat-breakdown-chart" id="region-report-habitat-breakdown-chart-<?php echo the_ID(); ?>"></div>
                            <script>
                                postElement.addEventListener(
                                    "habitatStatistics",
                                    e => {
                                        const values = e.detail;
                                        vegaEmbed(
                                            `#region-report-habitat-breakdown-chart-${postId}`,
                                            {
                                                background: "transparent",
                                                description: "A simple donut chart with embedded data.",
                                                width: "container",
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
                        
                        <div id="region-report-habitat-breakdown-<?php echo the_ID(); ?>-2" class="tab-pane fade">
                            <table class="region-report-habitat-breakdown-table">
                                <thead>
                                    <tr>
                                        <th>Habitat</th>
                                        <th>Area (km²)</th>
                                        <th>Mapped (%)</th>
                                        <th>Total (%)</th>
                                    </tr>
                                </thead>

                                <tbody id="region-report-habitat-breakdown-table-<?php echo the_ID(); ?>"></tbody>
                            </table>
                            <script>
                                postElement.addEventListener(
                                    "habitatStatistics",
                                    e => {
                                        const values = e.detail;
                                        const table = document.getElementById(`region-report-habitat-breakdown-table-${postId}`);

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
                </div>

                <div class="region-report-other-breakdowns"></div>
            </div>
        </section>

        <section class="region-report-known">
            <h2>What's known about the <?php echo $region_name; ?>?</h2>
        </section>

        <section class="region-report-contains">
            <h2>What's in the <?php echo $region_name; ?>?</h2>
            
            <section class="region-report-mapped-habitat">
                <h3>Mapped Habitat</h3>
            </section>

            <section class="region-report-reserves">
                <h3>Reserves</h3>
            </section>

            <section class="region-report-imagery">
                <h3>Imagery</h3>
            </section>
        </section>

        <section class="region-report-pressures">
            <h2>What's happening in the <?php echo $region_name; ?>?</h2>
        </section>
    </div>

    <script>
        // TODO: Retrieve habitat statistics using AJAX
        const habitatStatistics = [
            {
                habitat: "Consolidated Hard Substrata",
                area: 1235.8150275754394,
                mapped_percentage: 18.38023961629475,
                total_percentage: 0.034265818335294004,
                color: "#640000"
            },
            {
                habitat: "Coral Biota",
                area: 88.92393920043945,
                mapped_percentage: 1.3225630645838133,
                total_percentage: 0.0024656210503274383,
                color: "#f166ff"
            },
            {
                habitat: "Hard Substrata",
                area: 10.152846682128907,
                mapped_percentage: 0.15100298235663012,
                total_percentage: 0.000281511061310253,
                color: "#8a5c5c"
            },
            {
                habitat: "Invertebrates",
                area: 106.06767944702149,
                mapped_percentage: 1.5775413959849975,
                total_percentage: 0.002940970739211998,
                color: "#ff0e48"
            },
            {
                habitat: "Macroalgae",
                area: 181.3878893095703,
                mapped_percentage: 2.697776604598161,
                total_percentage: 0.005029397057501583,
                color: "#2d9624"
            },
            {
                habitat: "Macrophytes",
                area: 85.10082499267578,
                mapped_percentage: 1.2657020023283883,
                total_percentage: 0.002359616402386455,
                color: "#00e6b4"
            },
            {
                habitat: "Mixed Biota",
                area: 91.86948494140626,
                mapped_percentage: 1.3663720775118597,
                total_percentage: 0.0025472930910504636,
                color: "#0099ff"
            },
            {
                habitat: "Mixed Filter Feeder Community",
                area: 108.54156908508301,
                mapped_percentage: 1.6143354819260376,
                total_percentage: 0.0030095650280237253,
                color: "#ff99e6"
            },
            {
                habitat: "Mixed Hard/Soft Substrata",
                area: 9.745303597167968,
                mapped_percentage: 0.14494160635099748,
                total_percentage: 0.00027021099050558375,
                color: "#cc6600"
            },
            {
                habitat: "Mixed Invertebrate Community",
                area: 12.257090540039062,
                mapped_percentage: 0.18229933776297688,
                total_percentage: 0.00033985606938947066,
                color: "#7300e6"
            },
            {
                habitat: "Pavement",
                area: 201.4696749716797,
                mapped_percentage: 2.996452396813441,
                total_percentage: 0.0055862108233093845,
                color: "#cccc00"
            },
            {
                habitat: "Sand",
                area: 474.52206928344725,
                mapped_percentage: 7.0575524184725635,
                total_percentage: 0.01315721743087626,
                color: "#FFF9A5"
            },
            {
                habitat: "Seagrass",
                area: 3996.9879088410034,
                mapped_percentage: 59.447080565174524,
                total_percentage: 0.11082569682083905,
                color: "#02DC00"
            },
            {
                habitat: "Soft Substrata",
                area: 118.92278854333496,
                mapped_percentage: 1.7687350456911453,
                total_percentage: 0.003297408250607912,
                color: "#ffd480"
            },
            {
                habitat: "Sponges",
                area: 0.2259760517578125,
                mapped_percentage: 0.0033609349993110288,
                total_percentage: 0.0000062657065700615275,
                color: "#FCFAE2"
            },
            {
                habitat: "Unknown",
                area: 1.6166555456542968,
                mapped_percentage: 0.024044469150399463,
                total_percentage: 0.000044825498963884484,
                color: null
            }
        ];
        postElement.dispatchEvent(
            new CustomEvent(
                "habitatStatistics",
                { detail: habitatStatistics }
            )
        );
    </script>
</article>

<?php get_footer(); ?>
