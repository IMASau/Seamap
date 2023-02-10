import logging
from django.core.management.base import BaseCommand
from django.db import connections

from catalogue.models import Layer

# Clears table, and prepares it for insertion of features by disabling spatial
# index.
SQL_RESET_LAYER_FEATURES = """
ALTER INDEX layer_geom ON layer_feature DISABLE;
TRUNCATE TABLE layer_feature;
"""

class Command(BaseCommand):
    def handle(self, *args, **options):
        try:
            with connections['transects'].cursor() as cursor:
                cursor.execute(SQL_RESET_LAYER_FEATURES)
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
        else:
            print(' '.join(str(layer.id) for layer in Layer.objects.all()[0:4]))
