"""
Management command to generate a fake hazard layer and NetCDF files to test with NHAT
"""
import re
from typing import Any

import catalogue.models
import nhat.models as models
import numpy as np
import xarray as xr
from django.core.management.base import BaseCommand, CommandParser
from django.db.models import F
from numpy.typing import NDArray
from xarray.coding.cftimeindex import CFTimeIndex


# mypy: disable-error-code="misc"
# pylint: disable=line-too-long
# pylint: disable=missing-function-docstring
# pylint: disable=missing-class-docstring
# pylint: disable=redefined-outer-name


def _netcdf_dataset(time: CFTimeIndex, lat: NDArray[np.floating[Any]], lon: NDArray[np.floating[Any]], value: NDArray[np.floating[Any]]) -> xr.Dataset:
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
    def add_arguments(self, parser: CommandParser) -> None:
        parser.add_argument(
            '--layer_name'
        )

    def handle(self, *_args: Any, **options: str) -> None:
        layer_name = options['layer_name']
        time = xr.date_range(
            start="1970-01-01",
            end="2099-01-01",
            freq="5YS-JAN",
            use_cftime=True,
            calendar="noleap",
        )
        lat = np.arange(-44,-39.19,0.05)
        lon = np.arange(143,150.01,0.05)

        netcdf_file_basename = re.sub(r'[^a-z0-9]+', '_', layer_name.lower()).strip('_')

        # Generate NetCDF files for each CMIP phase, scientific model, scenario, and season
        cmip_phases = models.CmipPhase.objects.order_by(F('sort_key').asc(nulls_last=True)) # [models.CmipPhase(name="CMIP6"), models.CmipPhase(name="Other CMIP (For Testing)")]
        scientific_models = models.ScientificModel.objects.order_by(F('sort_key').asc(nulls_last=True)) # [models.ScientificModel(name="Multi-model median"), models.ScientificModel(name="Other Model (For Testing)")]
        scenarios = models.Scenario.objects.order_by(F('sort_key').asc(nulls_last=True)) # [models.Scenario(name="SSP1-2.6"), models.Scenario(name="SSP3-7.0")]
        seasons = models.Season.objects.order_by(F('sort_key').asc(nulls_last=True))
        cmip_phases_count = len(cmip_phases)
        scientific_models_count = len(scientific_models)
        scenarios_count = len(scenarios)
        seasons_count = len(seasons)

        for index, cmip_phase in enumerate(cmip_phases):
            cmip_phase_offset = index / (cmip_phases_count - 1) if cmip_phases_count > 1 else 0.5
            for index, scientific_model in enumerate(scientific_models):
                scientific_model_offset = index / (scientific_models_count - 1) if scientific_models_count > 1 else 0.5
                for index, scenario in enumerate(scenarios):
                    scenario_offset = index / (scenarios_count - 1) if scenarios_count > 1 else 0.5
                    for index, season in enumerate(seasons):
                        season_offset = index / (seasons_count - 1) if seasons_count > 1 else 0.5
                        netcdf_file_name = (
                            f"{netcdf_file_basename}_{cmip_phase.name}_{scientific_model.name}_{scenario.name}" +
                            (f"_{season.name}" if season.name != "All" else "")
                        )
                        offset = cmip_phase_offset + scientific_model_offset + scenario_offset + season_offset
                        value = (np.random.rand(len(time), len(lat), len(lon)) + offset).astype(np.float32)
                        cur_ds_out = _netcdf_dataset(time, lat, lon, value)
                        cur_ds_out.to_netcdf(f"{netcdf_file_name}.nc")
        cur_ds_out = _netcdf_dataset(time, lat, lon, (np.random.rand(len(time), len(lat), len(lon)) * 5).astype(np.float32))
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
            crs = "EPSG:4326",
            download_format = "thredds-wcs",
        )
        models.HazardLayer.objects.create(
            layer = layer,
            color_scale_range_min = 0,
            color_scale_range_max = 5,
            color_palette = 'seq-GreysRev'
        )
