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


def _netcdf_dataset(time, lat, lon, value):
    """Generates an xarray dataset for a CF Conventions compliant NetCDF file."""
    return xr.Dataset(
        data_vars=dict(
            value=(
                ["time", "lat", "lon"],
                value,
                {
                    "standard_name": "air_temperature",
                    "long_name": "Random Test Variable",
                    "units": "degC",
                    "_FillValue": np.float32(np.nan),
                    "coordinates": "time lat lon",
                },
            )
        ),
        coords={
            "lat": (
                ["lat"],
                lat,
                {
                    "units": "degrees_north",
                    "standard_name": "latitude",
                    "long_name": "latitude",
                    "axis": "Y",
                },
            ),
            "lon": (
                ["lon"],
                lon,
                {
                    "units": "degrees_east",
                    "standard_name": "longitude",
                    "long_name": "longitude",
                    "axis": "X",
                },
            ),
            "time": (
                ["time"],
                time,
                {
                    "standard_name": "time",
                    "long_name": "time",
                    "axis": "T",
                },
            ),
        },
        attrs={
            "Conventions": "CF-1.8",
            "title": "WCS Test Dataset",
        },
    )

class Command(BaseCommand):
    def add_arguments(self, parser):
        parser.add_argument(
            '--layer_name'
        )

    def handle(self, *args, **options):
        layer_name = options['layer_name']
        time = xr.date_range(
            start="1970-01-01",
            end="2009-01-01",
            freq="5YS-JAN",
            use_cftime=True,
            calendar="noleap",
        )
        lat = np.arange(-44,-39.19,0.05)
        lon = np.arange(143,150.01,0.05)

        netcdf_file_basename = re.sub(r'[^a-z0-9]+', '_', layer_name.lower()).strip('_')
        # Generate NetCDF files
        for scientific_model in models.ScientificModel.objects.all():
            scientific_model_offset = np.random.rand()
            for scenario in models.Scenario.objects.all():
                scenario_offset = np.random.rand() + scientific_model_offset
                for season in models.Season.objects.all():
                    season_offset = np.random.rand() + scenario_offset
                    netcdf_file_name = (
                        f"{netcdf_file_basename}_{scientific_model.name}_{scenario.name}" +
                        (f"_{season.name}" if season.name != "All" else "")
                    )
                    cur_ds_out = _netcdf_dataset(time, lat, lon, (np.random.rand(len(time), len(lat), len(lon))+season_offset).astype(np.float32))
                    cur_ds_out.to_netcdf(f"{netcdf_file_name}.nc")
        cur_ds_out = _netcdf_dataset(time, lat, lon, (np.random.rand(len(time), len(lat), len(lon))*4).astype(np.float32))
        cur_ds_out.to_netcdf(f"{netcdf_file_basename}.nc")

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
            color_scale_range_max = 4,
            color_palette = 'seq-GreysRev'
        )
