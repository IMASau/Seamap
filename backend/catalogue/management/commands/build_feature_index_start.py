import logging
from django.core.management.base import BaseCommand
from django.db import connections

from catalogue.models import Layer

# Clears the temp layer feature table
SQL_TRUNCATE_LAYER_FEATURE_TEMP = "TRUNCATE TABLE layer_feature_temp;"


class Command(BaseCommand):
    def add_arguments(self, parser):
        parser.add_argument(
            '--layer_id',
            help="Optional parameter: outputs this layer for use in build_feature_index_layer, instead of all layers."
        )

    def handle(self, *args, **options):
        try:
            with connections['transects'].cursor() as cursor:
                cursor.execute(SQL_TRUNCATE_LAYER_FEATURE_TEMP)
        except Exception as e:
            logging.error('Error at %s', 'division', exc_info=e)
        else:
            print(
                options['layer_id']
                if options['layer_id']
                else ' '.join(str(layer.id) for layer in Layer.objects.all())
            )
