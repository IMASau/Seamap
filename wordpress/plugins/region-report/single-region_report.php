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
