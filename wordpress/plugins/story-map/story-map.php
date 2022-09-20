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
 * Version:           1.0.0
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
                    'key'      => 'field_story_map_description',
                    'label'    => 'Description',
                    'name'     => 'description',
                    'type'     => 'textarea',
                    'required' => true,
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
