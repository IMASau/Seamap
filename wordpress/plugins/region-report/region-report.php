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
 * Version:           1.0.4
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
    wp_register_script('region_report', plugins_url('region-report.js', __FILE__ ));
    wp_enqueue_script('region_report');
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

// Add config page
add_action( 'admin_init', function () {
    // Register base URL settings
    register_setting(
        'region_report',
        'region_report_habitat_statistics_url_base',
        [
            'type'              => 'string',
            'description'       => 'Habitat statistics URL base',
            'sanitize_callback' => 'sanitize_text_field',
            'show_in_rest'      => true,
            'default'           => null
        ]
    );
    register_setting(
        'region_report',
        'region_report_bathymetry_statistics_url_base',
        [
            'type'              => 'string',
            'description'       => 'Bathymetry statistics URL base',
            'sanitize_callback' => 'sanitize_text_field',
            'show_in_rest'      => true,
            'default'           => null
        ]
    );
    register_setting(
        'region_report',
        'region_report_habitat_observations_url_base',
        [
            'type'              => 'string',
            'description'       => 'Habitat observations URL base',
            'sanitize_callback' => 'sanitize_text_field',
            'show_in_rest'      => true,
            'default'           => null
        ]
    );
    register_setting(
        'region_report',
        'region_report_research_effort_url_base',
        [
            'type'              => 'string',
            'description'       => 'Research effort URL base',
            'sanitize_callback' => 'sanitize_text_field',
            'show_in_rest'      => true,
            'default'           => null
        ]
    );
    register_setting(
        'region_report',
        'region_report_region_report_data_url_base',
        [
            'type'              => 'string',
            'description'       => 'Region report data URL base',
            'sanitize_callback' => 'sanitize_text_field',
            'show_in_rest'      => true,
            'default'           => null
        ]
    );
    register_setting(
        'region_report',
        'region_report_pressure_preview_url_base',
        [
            'type'              => 'string',
            'description'       => 'Pressure previews URL base',
            'sanitize_callback' => 'sanitize_text_field',
            'show_in_rest'      => true,
            'default'           => null
        ]
    );
    register_setting(
        'region_report',
        'region_report_map_url_base',
        [
            'type'              => 'string',
            'description'       => 'Map URL base',
            'sanitize_callback' => 'sanitize_text_field',
            'show_in_rest'      => true,
            'default'           => null
        ]
    );

    add_settings_section(
        'region_report_url_bases',
        'Region Report URL bases',
        null,
        'region_report'
    );

    // Register base URL settings fields
    add_settings_field(
        'region_report_habitat_statistics_url_base_field',
        'Habitat statistics URL base',
        function () {
            $setting = get_option('region_report_habitat_statistics_url_base');
            ?>
            <input
                type="text"
                name="region_report_habitat_statistics_url_base"
                value="<?php echo isset( $setting ) ? esc_attr( $setting ) : ''; ?>"
            >
            <?php
        },
        'region_report',
        'region_report_url_bases'
    );
    add_settings_field(
        'region_report_bathymetry_statistics_url_base_field',
        'Bathymetry statistics URL base',
        function () {
            $setting = get_option('region_report_bathymetry_statistics_url_base');
            ?>
            <input
                type="text"
                name="region_report_bathymetry_statistics_url_base"
                value="<?php echo isset( $setting ) ? esc_attr( $setting ) : ''; ?>"
            >
            <?php
        },
        'region_report',
        'region_report_url_bases'
    );
    add_settings_field(
        'region_report_habitat_observations_url_base_field',
        'Habitat observations URL base',
        function () {
            $setting = get_option('region_report_habitat_observations_url_base');
            ?>
            <input
                type="text"
                name="region_report_habitat_observations_url_base"
                value="<?php echo isset( $setting ) ? esc_attr( $setting ) : ''; ?>"
            >
            <?php
        },
        'region_report',
        'region_report_url_bases'
    );
    add_settings_field(
        'region_report_research_effort_url_base_field',
        'Research effort URL base',
        function () {
            $setting = get_option('region_report_research_effort_url_base');
            ?>
            <input
                type="text"
                name="region_report_research_effort_url_base"
                value="<?php echo isset( $setting ) ? esc_attr( $setting ) : ''; ?>"
            >
            <?php
        },
        'region_report',
        'region_report_url_bases'
    );
    add_settings_field(
        'region_report_region_report_data_url_base_field',
        'Region report data URL base',
        function () {
            $setting = get_option('region_report_region_report_data_url_base');
            ?>
            <input
                type="text"
                name="region_report_region_report_data_url_base"
                value="<?php echo isset( $setting ) ? esc_attr( $setting ) : ''; ?>"
            >
            <?php
        },
        'region_report',
        'region_report_url_bases'
    );
    add_settings_field(
        'region_report_pressure_preview_url_base_field',
        'Pressure previews URL base',
        function () {
            $setting = get_option('region_report_pressure_preview_url_base');
            ?>
            <input
                type="text"
                name="region_report_pressure_preview_url_base"
                value="<?php echo isset( $setting ) ? esc_attr( $setting ) : ''; ?>"
            >
            <?php
        },
        'region_report',
        'region_report_url_bases'
    );
    add_settings_field(
        'region_report_map_url_base_field',
        'Map URL base',
        function () {
            $setting = get_option('region_report_map_url_base');
            ?>
            <input
                type="text"
                name="region_report_map_url_base"
                value="<?php echo isset( $setting ) ? esc_attr( $setting ) : ''; ?>"
            >
            <?php
        },
        'region_report',
        'region_report_url_bases'
    );

    // Register configurable text settings
    register_setting(
        'region_report',
        'region_report_overview_map_caption',
        [
            'type'              => 'string',
            'description'       => 'Overview map caption',
            'sanitize_callback' => null,
            'show_in_rest'      => true,
            'default'           => null
        ]
    );
    register_setting(
        'region_report',
        'region_report_known_caption',
        [
            'type'              => 'string',
            'description'       => 'Known caption',
            'sanitize_callback' => null,
            'show_in_rest'      => true,
            'default'           => null
        ]
    );
    register_setting(
        'region_report',
        'region_report_squidle_caption',
        [
            'type'              => 'string',
            'description'       => 'Squidle caption',
            'sanitize_callback' => null,
            'show_in_rest'      => true,
            'default'           => null
        ]
    );
    register_setting(
        'region_report',
        'region_report_imagery_caption',
        [
            'type'              => 'string',
            'description'       => 'Imagery caption',
            'sanitize_callback' => null,
            'show_in_rest'      => true,
            'default'           => null
        ]
    );
    register_setting(
        'region_report',
        'region_report_pressures_caption',
        [
            'type'              => 'string',
            'description'       => 'Pressures caption',
            'sanitize_callback' => null,
            'show_in_rest'      => true,
            'default'           => null
        ]
    );

    add_settings_section(
        'region_report_configurable_text',
        'Region Report Configurable Text',
        null,
        'region_report'
    );

    // Register configurable text settings fields
    add_settings_field(
        'region_report_overview_map_caption_field',
        'Overview map caption',
        function () {
            $setting = get_option('region_report_overview_map_caption');
            ?>
            <textarea
                name="region_report_overview_map_caption"
                rows="6"
                cols="80"
            ><?php echo isset( $setting ) ? esc_attr( $setting ) : ''; ?></textarea>
            <?php
        },
        'region_report',
        'region_report_configurable_text'
    );
    add_settings_field(
        'region_report_known_caption_field',
        'Known caption',
        function () {
            $setting = get_option('region_report_known_caption');
            ?>
            <textarea
                name="region_report_known_caption"
                rows="6"
                cols="80"
            ><?php echo isset( $setting ) ? esc_attr( $setting ) : ''; ?></textarea>
            <?php
        },
        'region_report',
        'region_report_configurable_text'
    );
    add_settings_field(
        'region_report_squidle_caption_field',
        'Squidle caption',
        function () {
            $setting = get_option('region_report_squidle_caption');
            ?>
            <textarea
                name="region_report_squidle_caption"
                rows="6"
                cols="80"
            ><?php echo isset( $setting ) ? esc_attr( $setting ) : ''; ?></textarea>
            <?php
        },
        'region_report',
        'region_report_configurable_text'
    );
    add_settings_field(
        'region_report_imagery_caption_field',
        'Imagery caption',
        function () {
            $setting = get_option('region_report_imagery_caption');
            ?>
            <textarea
                name="region_report_imagery_caption"
                rows="6"
                cols="80"
            ><?php echo isset( $setting ) ? esc_attr( $setting ) : ''; ?></textarea>
            <?php
        },
        'region_report',
        'region_report_configurable_text'
    );
} );

add_action( 'admin_menu', function () {
    add_menu_page(
        'Region Report Config',
        'Region Report Config',
        'manage_options',
        'region_report',
        function () {
            if ( ! current_user_can( 'manage_options' ) ) {
                return;
            }

            if ( isset( $_GET['settings-updated'] ) ) {
                add_settings_error(
                    'region_report_messages',
                    'region_report_message',
                    __( 'Settings Saved', 'region_report' ),
                    'updated'
                );
            }

            settings_errors( 'region_report_messages' );

            ?>
            <div class="wrap">
                <h1><?php echo esc_html( get_admin_page_title() ); ?></h1>
                <form action="options.php" method="post">
                    <?php
                    settings_fields( 'region_report' );
                    do_settings_sections( 'region_report' );
                    submit_button( 'Save Settings' );
                    ?>
                </form>
            </div>
            <?php
        }
    );
} );
