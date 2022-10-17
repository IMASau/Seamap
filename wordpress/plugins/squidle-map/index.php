<?php
/**
 * Plugin Name: Gutenberg examples 01
 */
function gutenberg_examples_01_register_block() {
    register_block_type( __DIR__ );
}
add_action( 'init', 'gutenberg_examples_01_register_block' );
