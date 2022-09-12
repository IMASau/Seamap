<?php
get_header();
?>

<?php
	while (have_posts()) :
		the_post();
		$title = get_the_title();
		$content = get_the_content();
		$image = get_the_post_thumbnail_url();
		$map_link = get_field('map_link');
?>
	<article class="story-map">
		<section>
			<h2><?php echo $title ?></h2>
			<p><?php echo $content ?></p>
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
<?php
	endwhile;
?>
<!-- 
/* Start the Loop */
while ( have_posts() ) :
	the_post();

	get_template_part( 'template-parts/content/content-single' );

	if ( is_attachment() ) {
		// Parent post navigation.
		the_post_navigation(
			array(
				/* translators: %s: Parent post link. */
				'prev_text' => sprintf( __( '<span class="meta-nav">Published in</span><span class="post-title">%s</span>', 'twentytwentyone' ), '%title' ),
			)
		);
	}

	// If comments are open or there is at least one comment, load up the comment template.
	if ( comments_open() || get_comments_number() ) {
		comments_template();
	}

	// Previous/next post navigation.
	$twentytwentyone_next = is_rtl() ? twenty_twenty_one_get_icon_svg( 'ui', 'arrow_left' ) : twenty_twenty_one_get_icon_svg( 'ui', 'arrow_right' );
	$twentytwentyone_prev = is_rtl() ? twenty_twenty_one_get_icon_svg( 'ui', 'arrow_right' ) : twenty_twenty_one_get_icon_svg( 'ui', 'arrow_left' );

	$twentytwentyone_next_label     = esc_html__( 'Next post', 'twentytwentyone' );
	$twentytwentyone_previous_label = esc_html__( 'Previous post', 'twentytwentyone' );

	the_post_navigation(
		array(
			'next_text' => '<p class="meta-nav">' . $twentytwentyone_next_label . $twentytwentyone_next . '</p><p class="post-title">%title</p>',
			'prev_text' => '<p class="meta-nav">' . $twentytwentyone_prev . $twentytwentyone_previous_label . '</p><p class="post-title">%title</p>',
		)
	);
endwhile; // End of the loop.

get_footer(); -->
<?php
	get_footer();
