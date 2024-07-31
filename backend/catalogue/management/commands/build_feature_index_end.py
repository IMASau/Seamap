import logging
from django.core.management.base import BaseCommand
from django.db import connections

from catalogue.emails import email_build_feature_index_summary, LayerFeatureIndexError

# Ends insertion of features by updating the geoms for each row of
# layer_feature_temp, since they are inserted without a SRID due to
# fast_executemany limitations
# (https://github.com/mkleehammer/pyodbc/issues/490#issuecomment-445918375).
# Copies layer_feature_temp into layer_feature, overriding the layers present in
# layer_feature_temp.
# Rebuilds spatial index.
SQL_REENABLE_SPATIAL_INDEX = """
UPDATE layer_feature_temp SET geom.STSrid = 4326;
UPDATE layer_feature_temp SET geom = geom.MakeValid();
ALTER INDEX layer_geom ON layer_feature DISABLE;

DELETE FROM layer_feature
WHERE layer_id IN (
  SELECT DISTINCT layer_id
  FROM layer_feature_temp
);

INSERT INTO layer_feature (layer_id, geom)
SELECT layer_id, geom FROM layer_feature_temp;

ALTER INDEX layer_geom ON layer_feature REBUILD;
"""

# Pulls the latest error for each layer from layer_feature_log.
SQL_GET_FEATURE_INDEX_LOG_LATEST = """
SELECT
  layer_feature_log.layer_id,
  layer_feature_log.error,
  catalogue_layer.name
FROM layer_feature_log
INNER JOIN catalogue_layer
  ON layer_feature_log.layer_id = catalogue_layer.id
WHERE
  layer_feature_log.id IN (
    SELECT MAX(id)
    FROM layer_feature_log
    GROUP BY layer_id
  ) AND
  layer_feature_log.error IS NOT NULL;
"""

class Command(BaseCommand):
    def handle(self, *args, **options):
        try:
            with connections['transects'].cursor() as cursor:
                cursor.execute(SQL_REENABLE_SPATIAL_INDEX)
                cursor.execute(SQL_GET_FEATURE_INDEX_LOG_LATEST)
                errors = [LayerFeatureIndexError(*row) for row in cursor.fetchall()]
                email_build_feature_index_summary(errors)
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
