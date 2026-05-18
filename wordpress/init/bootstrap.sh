#!/bin/sh
# Idempotent WP bootstrap. Runs once per `docker compose up wp-init`.
#
# - Waits for WordPress (Apache) and MariaDB to be ready.
# - On first run: wp core install, sets permalinks, creates admin user.
# - On every run: activates bespoke plugins + ACF Pro, reasserts permalinks.
#
# Safe to re-run; wp-cli's `wp core install` exits non-zero if WP is already
# installed, which we treat as "fine, move on". Plugin activation is naturally
# idempotent (--quiet hides "already active" notices).
set -eu

# wp-cli inside the official wordpress:cli image runs as user 33 (www-data).
# That matches the apache user in the wordpress:apache image so file
# ownership stays consistent across the two containers' shared volume.

cd /var/www/html

echo "[wp-init] waiting for WordPress core files…"
i=0
while [ ! -f /var/www/html/wp-settings.php ]; do
  i=$((i+1))
  if [ "$i" -gt 60 ]; then
    echo "[wp-init] ERROR: wp core files never appeared at /var/www/html/" >&2
    exit 1
  fi
  sleep 1
done

echo "[wp-init] waiting for database…"
i=0
until wp db check --quiet 2>/dev/null; do
  i=$((i+1))
  if [ "$i" -gt 60 ]; then
    echo "[wp-init] ERROR: database never became reachable" >&2
    wp db check || true
    exit 1
  fi
  sleep 2
done

if wp core is-installed --quiet 2>/dev/null; then
  echo "[wp-init] WP is already installed — skipping core install."
else
  echo "[wp-init] running wp core install…"
  wp core install \
    --url="${WP_URL:-http://localhost:8888}" \
    --title="${WP_TITLE:-Seamap Local}" \
    --admin_user="${WP_ADMIN_USER:-admin}" \
    --admin_password="${WP_ADMIN_PASSWORD:-admin}" \
    --admin_email="${WP_ADMIN_EMAIL:-admin@example.com}" \
    --skip-email
fi

echo "[wp-init] setting permalink structure (CPT rewrites need this)…"
wp rewrite structure '/%postname%/' --hard

echo "[wp-init] activating plugins…"
# ACF Pro must be active before story-map's acf_add_local_field_group() will
# register — the plugin guards its ACF code with class_exists('acf').
for plugin in advanced-custom-fields-pro story-map region-report; do
  if [ -d "/var/www/html/wp-content/plugins/$plugin" ]; then
    wp plugin activate "$plugin" || echo "[wp-init] WARN: could not activate $plugin"
  else
    echo "[wp-init] WARN: plugin '$plugin' missing from wp-content/plugins/"
  fi
done

echo "[wp-init] flushing rewrites so /wp-json/wp/v2/story_map resolves…"
wp rewrite flush --hard

echo "[wp-init] done. Admin: ${WP_URL:-http://localhost:8888}/wp-admin/  (user: ${WP_ADMIN_USER:-admin})"
