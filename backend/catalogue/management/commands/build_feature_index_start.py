import logging
from django.core.management.base import BaseCommand
from django.db import connections

from catalogue.models import Layer

# Clears the temp layer feature table
SQL_TRUNCATE_LAYER_FEATURE_TEMP = "TRUNCATE TABLE layer_feature_temp;"

class Command(BaseCommand):
    def handle(self, *args, **options):
        try:
            with connections['transects'].cursor() as cursor:
                cursor.execute(SQL_TRUNCATE_LAYER_FEATURE_TEMP)
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
        else:
            print(' '.join(str(layer.id) for layer in Layer.objects.all()))
