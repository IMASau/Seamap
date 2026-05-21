"""
Management command to generate a fake hazard layer and NetCDF files to test with NHAT
"""
import catalogue.models
import nhat.models as models

from django.core.management.base import BaseCommand
import numpy as np
import re
import xarray as xr


# pylint: disable=line-too-long
# pylint: disable=missing-function-docstring
# pylint: disable=missing-class-docstring
# pylint: disable=redefined-outer-name

class Command(BaseCommand):
    def add_arguments(self, parser):
        parser.add_argument(
            '--layer_name'
        )

    def handle(self, *args, **options):
        layer_name = options['layer_name']
        cur_date_range = xr.date_range(start="1970-01-01",end="2009-01-01", freq="5YS-JAN",use_cftime=True,calendar='noleap')
        lat = np.arange(-44,-39.19,0.05)
        lon = np.arange(143,150.01,0.05)

        netcdf_file_basename = re.sub(r'[^a-z0-9]+', '_', layer_name.lower()).strip('_')
        # Generate NetCDF files
        for scientific_model in models.ScientificModel.objects.all():
            for scenario in models.Scenario.objects.all():
                for season in models.Season.objects.all():
                    netcdf_file_name = f"{netcdf_file_basename}_{scientific_model.name}_{scenario.name}_{season.name}"
                    cur_ds_out = xr.Dataset(data_vars=dict(
                        value = (["lat","lon","time"],np.random.rand(97,141,8))),
                        coords={'lat':lat,'lon':lon,'time':cur_date_range})
                    cur_ds_out.to_netcdf(f"{netcdf_file_name}.nc")
        layer = catalogue.models.Layer.objects.create(
            name = layer_name,
            server_url = f"https://thredds-nhat-dev.its.utas.edu.au/thredds/wms/data/{netcdf_file_basename}.nc",
            layer_name = "value",
            category = catalogue.models.Category.objects.get(name="testing"),
            minx = 142.975,
            miny = -44.025,
            maxx = 150.025,
            maxy = -39.175,
            server_type = catalogue.models.ServerType.objects.get(name="thredds"),
            info_format_type = 1,
            layer_type = "wms-timeseries",
            crs = "EPSG:4326"
        )
        models.HazardLayer.objects.create(
            layer = layer,
            color_scale_range_min = 0,
            color_scale_range_max = 1,
            color_palette = 'seq-GreysRev'
        )
