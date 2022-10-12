<?php

/**
 * The plugin bootstrap file
 *
 * This file is read by WordPress to generate the plugin information in the plugin
 * admin area. This file also includes all of the dependencies used by the plugin,
 * registers the activation and deactivation functions, and defines a function
 * that starts the plugin.
 *
 * @since             1.0.0
 * @package           Region_Report
 *
 * @wordpress-plugin
 * Plugin Name:       Region Reports
 * Description:       Adds the Seamap region report post type.
 * Version:           1.0.0
 * Author:            Condense Pty Ltd.
 * Author URI:        https://condense.com.au/
 * License:           Affero General Public Licence (AGPL) v3
 * License URI:       https://www.gnu.org/licenses/agpl-3.0.en.html
 * Text Domain:       region-report
 */


/**
* Register region report custom post type.
*/
function region_report_setup_post_type() {
    wp_register_style('region_report', plugins_url('style.css', __FILE__ ));
    wp_enqueue_style('region_report');
    register_post_type(
        'region_report',
        [
            'labels'      => [
                'name'          => __('Region Reports', 'textdomain'),
                'singular_name' => __('Region Report', 'textdomain'),
            ],
            'public'       => true,
            'has_archive'  => true,
            'show_in_rest' => true,
            'rewrite'      => [
                'slug' => 'region-reports'
            ],
            'supports'     => [
                'title',
                'editor',
                'excerpt',
                'thumbnail',
                'custom-fields',
                'revisions',
            ],
        ]
    );
}
add_action('init', 'region_report_setup_post_type');


/**
 * Activate the plugin.
 */
function region_report_activate() {
    region_report_setup_post_type();
    flush_rewrite_rules();
}
register_activation_hook(__FILE__, 'region_report_activate');


/**
 * Deactivation hook.
 */
function region_report_deactivate() {
    unregister_post_type('region_report');
    flush_rewrite_rules();
}
register_deactivation_hook(__FILE__, 'region_report_deactivate');

function region_report_template($single) {
    global $post;

    /* Checks for single template by post type */
    if ($post->post_type == 'region_report') {
        if (file_exists(dirname(__FILE__) . '/single-region_report.php')) {
            return dirname(__FILE__) . '/single-region_report.php';
        }
    }

    return $single;
}
add_filter('single_template', 'region_report_template');

add_filter('acf/settings/remove_wp_meta_box', '__return_false');
