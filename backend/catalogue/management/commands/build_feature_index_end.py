import logging
from django.core.management.base import BaseCommand
from django.db import connections

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

DELETE layer_feature
WHERE layer_id IN (
  SELECT DISTINCT layer_id
  layer_feature_temp
);

INSERT INTO layer_feature
SELECT * FROM layer_feature_temp;

ALTER INDEX layer_geom ON layer_feature REBUILD;
"""

class Command(BaseCommand):
    def handle(self, *args, **options):
        try:
            with connections['transects'].cursor() as cursor:
                cursor.execute(SQL_REENABLE_SPATIAL_INDEX)
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
