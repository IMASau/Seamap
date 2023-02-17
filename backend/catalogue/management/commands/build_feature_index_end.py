import logging
from django.core.management.base import BaseCommand
from django.db import connections

# Ends insertion of features by updating the geoms for each row, since they are
# inserted without a SRID due to fast_executemany limitations
# (https://github.com/mkleehammer/pyodbc/issues/490#issuecomment-445918375).
# Reenables spatial index.
SQL_REENABLE_SPATIAL_INDEX = """
UPDATE layer_feature SET geom.STSrid = 4326;
UPDATE layer_feature SET geom = geom.MakeValid();
ALTER INDEX layer_geom ON layer_feature REBUILD;
"""

class Command(BaseCommand):
    def handle(self, *args, **options):
        try:
            with connections['transects'].cursor() as cursor:
                cursor.execute(SQL_REENABLE_SPATIAL_INDEX)
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
