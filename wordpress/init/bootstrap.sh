#!/bin/sh
# Idempotent WordPress bootstrap — entrypoint of the `wp-init` one-shot service.
#
# - First run: wp core install.
# - Every run: permalinks, custom-fields plugin, Seamap plugins, child theme
#   (when its parent is installed), region-report URL options, rewrite flush.
#
# Runs as uid 33 (www-data), matching Apache in the wordpress image, so files
# written to the shared wp-data volume keep consistent ownership.
set -eu

cd /var/www/html

echo "[wp-init] waiting for WordPress core files…"
i=0
until [ -f wp-settings.php ] && [ -f wp-config.php ]; do
  i=$((i+1))
  if [ "$i" -gt 60 ]; then
    echo "[wp-init] ERROR: WordPress files never appeared in /var/www/html" >&2
    exit 1
  fi
  sleep 1
done

echo "[wp-init] waiting for database…"
i=0
until wp db check --quiet >/dev/null 2>&1; do
  i=$((i+1))
  if [ "$i" -gt 60 ]; then
    echo "[wp-init] ERROR: database never became reachable" >&2
    wp db check || true
    exit 1
  fi
  sleep 2
done

if wp core is-installed 2>/dev/null; then
  echo "[wp-init] WordPress already installed — skipping core install."
else
  echo "[wp-init] running wp core install…"
  wp core install \
    --url="$WP_URL" \
    --title="$WP_TITLE" \
    --admin_user="$WP_ADMIN_USER" \
    --admin_password="$WP_ADMIN_PASSWORD" \
    --admin_email="$WP_ADMIN_EMAIL" \
    --skip-email
fi

# Pretty permalinks are required for /wp-json/... and the /region-reports/
# and /story-maps/ CPT slugs the frontend links to.
wp rewrite structure '/%postname%/' --quiet

# story-map registers its fields with acf_add_local_field_group(), including a
# repeater, so it needs ACF Pro (what production runs) or Secure Custom Fields
# (the free wordpress.org fork, which includes repeaters).
if [ -f wp-content/plugins/advanced-custom-fields-pro/acf.php ]; then
  echo "[wp-init] activating ACF Pro…"
  if wp plugin is-active secure-custom-fields 2>/dev/null; then
    wp plugin deactivate secure-custom-fields --quiet
  fi
  wp plugin activate advanced-custom-fields-pro --quiet
else
  echo "[wp-init] ACF Pro not present — using Secure Custom Fields instead."
  if ! wp plugin is-installed secure-custom-fields 2>/dev/null; then
    wp plugin install secure-custom-fields --quiet \
      || echo "[wp-init] WARN: could not install secure-custom-fields; story map fields will be missing."
  fi
  wp plugin activate secure-custom-fields --quiet 2>/dev/null || true
fi

echo "[wp-init] activating Seamap plugins…"
wp plugin activate story-map region-report --quiet

# The child theme needs the commercial H-Code parent theme, which isn't in the
# repo. Upload it via wp-admin (it persists in the wp-data volume) and re-run.
if wp theme is-installed h-code 2>/dev/null; then
  wp theme activate h-code-child --quiet
else
  echo "[wp-init] H-Code parent theme not installed — leaving the default theme active."
fi

# Point region reports at the local backend/frontend. Only fills options that
# are unset, so values edited in wp-admin (Settings → Region Reports) survive.
set_default_option() {
  if [ -z "$(wp option get "$1" 2>/dev/null || true)" ]; then
    wp option update "$1" "$2" --quiet
  fi
}
set_default_option region_report_habitat_statistics_url_base    "${SEAMAP_API_URL}habitat/habitatstatistics"
set_default_option region_report_bathymetry_statistics_url_base "${SEAMAP_API_URL}habitat/bathymetrystatistics"
set_default_option region_report_habitat_observations_url_base  "${SEAMAP_API_URL}habitat/habitatobservations"
set_default_option region_report_region_report_data_url_base    "${SEAMAP_API_URL}habitat/regionreportdata"
set_default_option region_report_squidle_annotations_data_url   "${SEAMAP_API_URL}squidleannotationsdata"
set_default_option region_report_research_effort_url_base       "${SEAMAP_MEDIA_URL}research_effort"
set_default_option region_report_pressure_preview_url_base      "${SEAMAP_MEDIA_URL}pressure_previews"
set_default_option region_report_map_url_base                   "$SEAMAP_MAP_URL"

wp rewrite flush --hard --quiet

echo "[wp-init] done. Admin: $WP_URL/wp-admin/ (user: $WP_ADMIN_USER)"
