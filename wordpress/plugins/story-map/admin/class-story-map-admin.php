<?php

/**
 * The admin-specific functionality of the plugin.
 *
 * @link       http://example.com
 * @since      1.0.0
 *
 * @package    Story_Map
 * @subpackage Story_Map/admin
 */

/**
 * The admin-specific functionality of the plugin.
 *
 * Defines the plugin name, version, and two examples hooks for how to
 * enqueue the admin-specific stylesheet and JavaScript.
 *
 * @package    Story_Map
 * @subpackage Story_Map/admin
 * @author     Your Name <email@example.com>
 */
class Story_Map_Admin {

	/**
	 * The ID of this plugin.
	 *
	 * @since    1.0.0
	 * @access   private
	 * @var      string    $story_map    The ID of this plugin.
	 */
	private $story_map;

	/**
	 * The version of this plugin.
	 *
	 * @since    1.0.0
	 * @access   private
	 * @var      string    $version    The current version of this plugin.
	 */
	private $version;

	/**
	 * Initialize the class and set its properties.
	 *
	 * @since    1.0.0
	 * @param      string    $story_map       The name of this plugin.
	 * @param      string    $version    The version of this plugin.
	 */
	public function __construct( $story_map, $version ) {

		$this->story_map = $story_map;
		$this->version = $version;

	}

	/**
	 * Register the stylesheets for the admin area.
	 *
	 * @since    1.0.0
	 */
	public function enqueue_styles() {

		/**
		 * This function is provided for demonstration purposes only.
		 *
		 * An instance of this class should be passed to the run() function
		 * defined in Story_Map_Loader as all of the hooks are defined
		 * in that particular class.
		 *
		 * The Story_Map_Loader will then create the relationship
		 * between the defined hooks and the functions defined in this
		 * class.
		 */

		wp_enqueue_style( $this->story_map, plugin_dir_url( __FILE__ ) . 'css/story-map-admin.css', array(), $this->version, 'all' );

	}

	/**
	 * Register the JavaScript for the admin area.
	 *
	 * @since    1.0.0
	 */
	public function enqueue_scripts() {

		/**
		 * This function is provided for demonstration purposes only.
		 *
		 * An instance of this class should be passed to the run() function
		 * defined in Story_Map_Loader as all of the hooks are defined
		 * in that particular class.
		 *
		 * The Story_Map_Loader will then create the relationship
		 * between the defined hooks and the functions defined in this
		 * class.
		 */

		wp_enqueue_script( $this->story_map, plugin_dir_url( __FILE__ ) . 'js/story-map-admin.js', array( 'jquery' ), $this->version, false );

	}

}
