"""
Management command to load hazard layers from a THREDDS server.
"""

import io
import re
import requests
import xarray as xr
import xml.etree.ElementTree as ET
from django.core.management.base import BaseCommand, CommandParser
from typing import Any

import catalogue.models
import nhat.models as models

# mypy: disable-error-code="misc"
# pylint: disable=line-too-long
# pylint: disable=missing-function-docstring
# pylint: disable=missing-class-docstring
# pylint: disable=redefined-outer-name


class Command(BaseCommand):
    def get_netcdf_names(self, server_url: str, hazard_layer_name: str) -> list[str]:
        catalog_url = f"{server_url}catalog/data/{hazard_layer_name}/catalog.xml"
        response = requests.get(catalog_url, timeout=30)
        response.raise_for_status()
        root = ET.fromstring(response.content)

        ns = {"t": "http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"}

        return [
            ds.attrib["name"]
            for ds in root.findall(".//t:dataset", ns)
            if ds.attrib["name"].endswith(".nc")
        ]

    def get_or_create_hazard_layer(self, hazard_layer_name: str, server_url: str, ds_attrs: dict[str, Any]) -> models.HazardLayer:
        try:
            hazard_layer = models.HazardLayer.objects.get(name=hazard_layer_name)
            hazard_layer.color_palette = ds_attrs["colour_palette"]
            hazard_layer.human_readable_units = ds_attrs["human_readable_units"]
            hazard_layer.save()
            assert isinstance(hazard_layer, models.HazardLayer) # assert silences mypy strict type checking
            return hazard_layer
        except models.HazardLayer.DoesNotExist:
            category, _ = catalogue.models.Category.objects.get_or_create(name=ds_attrs["category"])
            data_classification, _ = catalogue.models.DataClassification.objects.get_or_create(name=ds_attrs["data_classification"])
            thredds_server_type, _ = catalogue.models.ServerType.objects.get_or_create(name="thredds")
            layer = catalogue.models.Layer.objects.create(
                name = ds_attrs["display_name"],
                server_url = f"{server_url}wms/data/",
                category = category,
                data_classification = data_classification,
                minx = ds_attrs["minx"],
                miny = ds_attrs["miny"],
                maxx = ds_attrs["maxx"],
                maxy = ds_attrs["maxy"],
                server_type = thredds_server_type,
                info_format_type = 1,
                layer_type = "wms-timeseries",
                tooltip = ds_attrs.get("tooltip", None),
                crs = "EPSG:4326",
                download_format = "thredds-wcs",
            )
            hazard_layer = models.HazardLayer.objects.create(
                layer = layer,
                name = hazard_layer_name,
                color_palette = ds_attrs["colour_palette"], # "color" vs "colour" noted
                human_readable_units = ds_attrs["human_readable_units"],
            )
            assert isinstance(hazard_layer, models.HazardLayer) # assert silences mypy strict type checking
            return hazard_layer

    def get_netcdf_cmip(self, netcdf_name: str) -> str:
        """
        Extract the CMIP from the NetCDF name using regex.

        Using regex over the file name is unreliable; suggest to Climate Futures
        including CMIP in NetCDF global attributes in the future.
        """
        pattern = re.compile(
            r'^(?P<cmip>[^_]+)_[^_]+_(?P<scenario>[^_]+)_(?P<season>[^_]+)\.nc$'
        )
        match = pattern.search(netcdf_name)
        if match:
            return match.group('cmip')
        else:
            raise ValueError(f"Could not find CMIP in NetCDF name: {netcdf_name}")

    def get_netcdf_scenario(self, netcdf_name: str) -> str:
        """
        Extract the scenario from the NetCDF name using regex.

        Using regex over the file name is unreliable; suggest to Climate Futures
        including scenario in NetCDF global attributes in the future.
        """
        pattern = re.compile(
            r'^(?P<cmip>[^_]+)_[^_]+_(?P<scenario>[^_]+)_(?P<season>[^_]+)\.nc$'
        )
        match = pattern.search(netcdf_name)
        if match:
            return match.group('scenario')
        else:
            raise ValueError(f"Could not find scenario in NetCDF name: {netcdf_name}")

    def get_netcdf_season(self, netcdf_name: str) -> str:
        """
        Extract the season from the NetCDF name using regex.

        Using regex over the file name is unreliable; suggest to Climate Futures
        including season in NetCDF global attributes in the future.
        """
        pattern = re.compile(
            r'^(?P<cmip>[^_]+)_[^_]+_(?P<scenario>[^_]+)_(?P<season>[^_]+)\.nc$'
        )
        match = pattern.search(netcdf_name)
        if match:
            return match.group('season')
        else:
            raise ValueError(f"Could not find season in NetCDF name: {netcdf_name}")

    def load_netcdf(self, server_url: str, hazard_layer_name: str, netcdf_name: str) -> None:
        netcdf_url = f"{server_url}fileServer/data/{hazard_layer_name}/{netcdf_name}"
        self.stdout.write(f"Loading NetCDF from {netcdf_url}...")
        response = requests.get(netcdf_url, timeout=30)
        response.raise_for_status()
        ds = xr.open_dataset(io.BytesIO(response.content))

        hazard_layer = self.get_or_create_hazard_layer(hazard_layer_name, server_url, ds.attrs)
        cmip_name = self.get_netcdf_cmip(netcdf_name)
        scenario_name = self.get_netcdf_scenario(netcdf_name)
        season_name = self.get_netcdf_season(netcdf_name)
        is_historical = scenario_name == "historical"
        cmip, _ = models.CmipPhase.objects.get_or_create(name=cmip_name, defaults={"display_name": cmip_name})
        scenario = None
        if not is_historical:
            scenario, _ = models.Scenario.objects.get_or_create(name=scenario_name, defaults={"display_name": scenario_name})
        season, _ = models.Season.objects.get_or_create(name=season_name, defaults={"display_name": season_name})

        for var_name in ds.data_vars:
            var = ds[var_name]
            model, _ = models.ScientificModel.objects.get_or_create(
                name=var_name,
                defaults={
                    "display_name": var.attrs["name"],
                    "data_category": var.attrs["data_category"],
                },
            )

            try:
                hazard_layer_dataset = models.HazardLayerDataset.objects.get(
                    hazard_layer=hazard_layer,
                    scientific_model=model,
                    cmip_phase=cmip,
                    scenario=scenario if not is_historical else None,
                    season=season,
                    is_historical=is_historical,
                )
                self.stdout.write(f"Updating existing Hazard Layer Dataset: {hazard_layer_dataset}")
                hazard_layer_dataset.color_scale_range_min = var.attrs["colour_scale_range_min"] # "color" vs "colour" noted
                hazard_layer_dataset.color_scale_range_max = var.attrs["colour_scale_range_max"]
                hazard_layer_dataset.save() # Probably some efficiency to be gained by bulk updating, but this is fine for now
            except models.HazardLayerDataset.DoesNotExist:
                self.stdout.write(f"Creating new Hazard Layer Dataset for {hazard_layer.name}, {model.name}, {cmip.name}, {scenario.name if scenario else 'historical'}, {season.name}...")
                hazard_layer_dataset = models.HazardLayerDataset.objects.create(
                    hazard_layer=hazard_layer,
                    scientific_model=model,
                    cmip_phase=cmip,
                    scenario=scenario if not is_historical else None,
                    season=season,
                    is_historical=is_historical,
                    color_scale_range_min=var.attrs["colour_scale_range_min"], # "color" vs "colour" noted
                    color_scale_range_max=var.attrs["colour_scale_range_max"],
                )

    def load_hazard_layers(self, server_url: str, hazard_layer_name: str) -> None:
        """
        Load hazard layers from a THREDDS server.
        """
        # TODO: hardcoded for one hazard layer for now
        # Get the list of hazard layers from the THREDDS server
        netcdf_names = self.get_netcdf_names(server_url, hazard_layer_name) # TODO: hardcoded for one hazard layer for now
        for netcdf_name in netcdf_names:
            self.load_netcdf(server_url, hazard_layer_name, netcdf_name)

    def add_arguments(self, parser: CommandParser) -> None:
        parser.add_argument(
            '--server_url',
            type=str,
            required=True,
            help='The base URL of the THREDDS server to load hazard layers from.',
        )
        parser.add_argument(
            '--name',
            type=str,
            required=True,
            help='The name of the hazard layer to load. TODO: Remove when we load all hazard layers from the server.',
        )

    def handle(self, *_args: Any, **options: str) -> None:
        server_url = options['server_url']
        name = options['name']
        self.stdout.write(f"Loading hazard layers from {server_url}...")
        self.load_hazard_layers(server_url, name) # TODO: Remove name argument when we load all hazard layers from the server
        self.stdout.write(self.style.SUCCESS("Successfully loaded hazard layers."))
