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
 * @package           Story_Map
 *
 * @wordpress-plugin
 * Plugin Name:       Story Maps
 * Description:       Adds the Seamap story map post type.
 * Version:           1.3.0
 * Author:            Condense Pty Ltd.
 * Author URI:        https://condense.com.au/
 * License:           Affero General Public Licence (AGPL) v3
 * License URI:       https://www.gnu.org/licenses/agpl-3.0.en.html
 * Text Domain:       story-map
 */


/**
* Register story map custom post type.
*/
function story_map_setup_post_type() {
    register_post_type(
        'story_map',
        [
            'labels'      => [
                'name'          => __('Story Maps', 'textdomain'),
                'singular_name' => __('Story Map', 'textdomain'),
            ],
            'public'       => true,
            'has_archive'  => true,
            'show_in_rest' => true,
            'rewrite'      => [
                'slug' => 'story-maps'
            ],
        ]
    );
}
add_action('init', 'story_map_setup_post_type');

/**
 * Add repeating fields from ACF (runs at init, at which point ACF should have been loaded):
 */
function story_map_add_acf_fields() {
    if (class_exists('acf')) {
        add_action('acf/init', function() {
            acf_add_local_field_group([
                'key'          => 'group_story_map',
                'title'        => 'Story Map',
                'position'     => 'normal',
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
                        'key'      => 'field_story_map_description',
                        'label'    => 'Description',
                        'name'     => 'description',
                        'type'     => 'wysiwyg',
                        'required' => true,
                    ],
                    [
                        'key'          => 'field_story_map_read_time',
                        'label'        => 'Read Time',
                        'name'         => 'read_time',
                        'type'         => 'number',
                        'instructions' => 'Estimated minutes to get through the story.',
                        'min'          => 0,
                        'step'         => 1,
                        'append'       => 'minutes',
                    ],
                    [
                        'key'          => 'field_story_map_marker_lat',
                        'label'        => 'Marker Latitude',
                        'name'         => 'marker_lat',
                        'type'         => 'number',
                        'instructions' => 'Where the story\'s marker goes on the kiosk\'s home map (no marker without both latitude and longitude).',
                        'min'          => -90,
                        'max'          => 90,
                        'step'         => 'any',
                        'wrapper'      => ['width' => '50'],
                    ],
                    [
                        'key'      => 'field_story_map_marker_lon',
                        'label'    => 'Marker Longitude',
                        'name'     => 'marker_lon',
                        'type'     => 'number',
                        'min'      => -180,
                        'max'      => 180,
                        'step'     => 'any',
                        'wrapper'  => ['width' => '50'],
                    ],
                    [
                        'key'          => 'field_story_map_marker_icon',
                        'label'        => 'Marker Icon',
                        'name'         => 'marker_icon',
                        'type'         => 'url',
                        'instructions' => 'URL of the marker\'s image, centred on its location (a plain dot without one).',
                    ],
                    [
                        'key'        => 'field_story_map_map_link',
                        'label'      => 'Map Links',
                        'name'       => 'map_links',
                        'type'       => 'repeater',
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
                                'type'     => 'wysiwyg',
                                'required' => true,
                            ],
                            [
                                'key'      => 'field_story_map_map_link_shortcode',
                                'label'    => 'Shortcode',
                                'name'     => 'shortcode',
                                'type'     => 'text',
                                'required' => true,
                            ],
                            [
                                'key'          => 'field_story_map_map_link_media',
                                'label'        => 'Media',
                                'name'         => 'media',
                                'type'         => 'repeater',
                                'layout'       => 'block',
                                'button_label' => 'Add Media Item',
                                'sub_fields'   => [
                                    [
                                        'key'      => 'field_story_map_map_link_media_url',
                                        'label'    => 'URL',
                                        'name'     => 'url',
                                        'type'     => 'url',
                                        'required' => true,
                                    ],
                                    [
                                        'key'      => 'field_story_map_map_link_media_lat',
                                        'label'    => 'Latitude',
                                        'name'     => 'lat',
                                        'type'     => 'number',
                                        'required' => true,
                                        'min'      => -90,
                                        'max'      => 90,
                                        'step'     => 'any',
                                        'wrapper'  => ['width' => '50'],
                                    ],
                                    [
                                        'key'      => 'field_story_map_map_link_media_lon',
                                        'label'    => 'Longitude',
                                        'name'     => 'lon',
                                        'type'     => 'number',
                                        'required' => true,
                                        'min'      => -180,
                                        'max'      => 180,
                                        'step'     => 'any',
                                        'wrapper'  => ['width' => '50'],
                                    ],
                                    [
                                        'key'   => 'field_story_map_map_link_media_caption',
                                        'label' => 'Caption',
                                        'name'  => 'caption',
                                        'type'  => 'text',
                                    ],
                                    [
                                        'key'          => 'field_story_map_map_link_media_overlay',
                                        'label'        => 'Overlay',
                                        'name'         => 'overlay',
                                        'type'         => 'textarea',
                                        'rows'         => 3,
                                        'new_lines'    => '',
                                        'instructions' => 'JSON object of values to overlay on the media, e.g. {"temperature": 3.2604, "depth": 1245.5383}',
                                    ],
                                ],
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
}
add_action('plugins_loaded', 'story_map_add_acf_fields', 10);

/**
 * Media overlay is entered as JSON text; reject anything that isn't a JSON
 * object, and return it decoded so the REST API serves a real object.
 */
add_filter('acf/validate_value/key=field_story_map_map_link_media_overlay', function($valid, $value) {
    if ($valid !== true || trim((string) $value) === '') {
        return $valid;
    }
    $decoded = json_decode(wp_unslash($value));
    return is_object($decoded) ? true : 'Overlay must be a JSON object, e.g. {"temperature": 3.2604}';
}, 10, 2);

add_filter('acf/format_value/key=field_story_map_map_link_media_overlay', function($value) {
    if (trim((string) $value) === '') {
        return null;
    }
    $decoded = json_decode($value);
    return is_object($decoded) ? $decoded : null;
});

// An empty repeater formats as false; serve an empty list instead.
add_filter('acf/format_value/key=field_story_map_map_link_media', function($value) {
    return $value ?: [];
});

/**
 * Number fields are stored as strings; serve them as numbers (or null).
 */
foreach (['field_story_map_read_time', 'field_story_map_marker_lat', 'field_story_map_marker_lon',
          'field_story_map_map_link_media_lat', 'field_story_map_map_link_media_lon'] as $story_map_number_field) {
    add_filter("acf/format_value/key=$story_map_number_field", function($value) {
        return is_numeric($value) ? $value + 0 : null;
    });
}

/**
 * Activate the plugin.
 */
function story_map_activate() {
    story_map_setup_post_type(); 
    flush_rewrite_rules(); 
}
register_activation_hook(__FILE__, 'story_map_activate');


/**
 * Deactivation hook.
 */
function story_map_deactivate() {
    unregister_post_type('story_map');
    flush_rewrite_rules();
}
register_deactivation_hook(__FILE__, 'story_map_deactivate');
