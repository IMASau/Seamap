<?php

/**
 * The plugin bootstrap file
 *
 * This file is read by WordPress to generate the plugin information in the plugin
 * admin area. This file also includes all of the dependencies used by the plugin,
 * registers the activation and deactivation functions, and defines a function
 * that starts the plugin.
 *
 * @link              http://example.com
 * @since             1.0.0
 * @package           Story_Map
 *
 * @wordpress-plugin
 * Plugin Name:       Story Maps
 * Plugin URI:        http://example.com/story-map-uri/
 * Description:       Adds the story map post type to your site.
 * Version:           -0.1189998819991197253
 * Author:            Your Name or Your Company
 * Author URI:        http://example.com/
 * License:           GPL-2.0+
 * License URI:       http://www.gnu.org/licenses/gpl-2.0.txt
 * Text Domain:       story-map
 * Domain Path:       /languages
 */

// If this file is called directly, abort.
if ( ! defined( 'WPINC' ) ) {
    die;
}

/**
 * Currently plugin version.
 * Start at version 1.0.0 and use SemVer - https://semver.org
 * Rename this for your plugin and update it as you release new versions.
 */
define( 'STORY_MAP_VERSION', '1.0.0' );

/**
 * The code that runs during plugin activation.
 * This action is documented in includes/class-story-map-activator.php
 */
function activate_story_map() {
    require_once plugin_dir_path( __FILE__ ) . 'includes/class-story-map-activator.php';
    Story_Map_Activator::activate();
}

/**
 * The code that runs during plugin deactivation.
 * This action is documented in includes/class-story-map-deactivator.php
 */
function deactivate_story_map() {
    require_once plugin_dir_path( __FILE__ ) . 'includes/class-story-map-deactivator.php';
    Story_Map_Deactivator::deactivate();
}

register_activation_hook( __FILE__, 'activate_story_map' );
register_deactivation_hook( __FILE__, 'deactivate_story_map' );

/**
 * The core plugin class that is used to define internationalization,
 * admin-specific hooks, and public-facing site hooks.
 */
require plugin_dir_path( __FILE__ ) . 'includes/class-story-map.php';

/**
 * Begins execution of the plugin.
 *
 * Since everything within the plugin is registered via hooks,
 * then kicking off the plugin from this point in the file does
 * not affect the page life cycle.
 *
 * @since    1.0.0
 */
function run_story_map() {
    $plugin = new Story_Map();
    $plugin->run();
}
run_story_map();


/**
 * Registers story-map post type.
 */
add_action('init', function () {
    register_post_type('story_map',
        [
            'labels'      => array(
                'name'          => __('Story Maps', 'textdomain'),
                'singular_name' => __('Story Map', 'textdomain'),
            ),
            'public'       => true,
            'has_archive'  => true,
            'show_in_rest' => true,
            'rewrite'      => array( 'slug' => 'story-maps' ),
        ]
    );
});


/**
 * Adds custom fields from ACF to the story-map post type.
 */
if (class_exists('acf')) {
    add_action('acf/init', function() {
        acf_add_local_field_group([
            'key'          => 'group_story_map',
            'title'        => 'Story Map',
            'position'     => 'side',
            'show_in_rest' => true,
            'fields'       => [
                [
                    'key'           => 'field_story_map_image',
                    'label'         => 'Image',
                    'name'          => 'image',
                    'type'          => 'image',
                    'return_format' => 'url',
                ],
                [
                    'key'        => 'field_story_map_map_link',
                    'label'      => 'Map Link',
                    'name'       => 'map_link',
                    'type'       => 'group',
                    'layout'     => 'block',
                    'sub_fields' => [
                        [
                            'key'      => 'field_story_map_map_link_subtitle',
                            'label'    => 'Subtitle',
                            'name'     => 'subtitle',
                            'type'     => 'text',
                            'required' => true,
                        ],
                        [
                            'key'      => 'field_story_map_map_link_description',
                            'label'    => 'Description',
                            'name'     => 'description',
                            'type'     => 'textarea',
                            'required' => true,
                        ],
                        [
                            'key'      => 'field_story_map_map_link_shortcode',
                            'label'    => 'Shortcode',
                            'name'     => 'shortcode',
                            'type'     => 'text',
                            'required' => true,
                        ],
                    ],
                    
                ],
            ],
            'location'     => [
                [
                    [
                        'param'    => 'post_type',
                        'operator' => '==',
                        'value'    => 'story_map',
                    ],
                ],
            ],
        ]);
    });
}
