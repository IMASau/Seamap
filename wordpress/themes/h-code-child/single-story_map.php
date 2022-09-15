<?php get_header(); ?>

<?php
    while (have_posts()) :
        the_post();
        $title = get_the_title();
        $content = get_the_content();
        $image = get_field('image');
        $description = get_field('description');
        $map_links = get_field('map_links');
?>
    <article class="story-map">
        <section>
            <h2><?php echo $title; ?></h2>
            <p><?php echo $description; ?></p>
            <?php echo $content; ?>

            <?php if (!empty($image)): ?>
                <img src="<?php echo $image; ?>" />
            <?php endif; ?>

            <?php
                // Iterate over map link repeaters
                if ($map_links): foreach ($map_links as $map_link):
            ?>
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
            <?php endforeach; endif; ?>
        </section>
    </article>
<?php endwhile; ?>

<?php get_footer(); ?>
