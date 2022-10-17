
from collections import namedtuple
from django.core.management.base import BaseCommand
from django.core.files.storage import default_storage
from django.core.files.base import ContentFile
from django.db import connections
import logging
import json
from catalogue.models import RegionReport
from catalogue.serializers import RegionReportSerializer

def save_region_report_data(region_report):
    network = region_report.network
    park = region_report.park

    filepath = f'region_report_data/{network}/{park}.json' if park else f'region_report_data/{network}.json'
    logging.info(f'Saving habitat observations for {network}' + (f' - {park}' if park else ''))
    default_storage.delete(filepath)
    default_storage.save(filepath, ContentFile(json.dumps(RegionReportSerializer(region_report).data)))


class Command(BaseCommand):
    def handle(self, *args, **options):
        for region_report in RegionReport.objects.all():
            save_region_report_data(region_report)
            
