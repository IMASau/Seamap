<?php get_header(); ?>

<?php
    while (have_posts()) :
        the_post();
        $title = get_the_title();
        $content = get_the_content();
        $image = get_field('image');
        $map_link = get_field('map_link');
?>
    <article class="story-map">
        <section>
            <h2><?php echo $title; ?></h2>
            <?php echo $content; ?>

            <?php if (!empty($image)): ?>
                <img src="<?php echo $image; ?>" />
            <?php endif; ?>
        </section>

        <section>
            <!-- Iterate over map link repeaters -->
            <div class="map-link">
                <div class="subtitle"><?php echo $map_link['subtitle'] ?></div>
                <div class="description"><?php echo $map_link['description'] ?></div>
                <a
                    class="shortcode"
                    href="https://seamapaustralia-dev.imas.utas.edu.au/map/#<?php echo $map_link['shortcode'] ?>"
                >
                    Show me
                </a>
            </div>
        </section>
    </article>
<?php endwhile; ?>

<?php get_footer(); ?>
