"""
Management command to load hazard layers from a THREDDS server.
"""

import io
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

    def load_netcdf(self, server_url: str, hazard_layer_name: str, netcdf_name: str) -> None:
        netcdf_url = f"{server_url}fileServer/data/{hazard_layer_name}/{netcdf_name}"
        self.stdout.write(f"Loading NetCDF from {netcdf_url}...")
        response = requests.get(netcdf_url, timeout=30)
        response.raise_for_status()
        ds = xr.open_dataset(io.BytesIO(response.content))

        hazard_layer = self.get_or_create_hazard_layer(hazard_layer_name, server_url, ds.attrs)

        for var_name in ds.data_vars:
            var = ds[var_name]
            # TODO: Use regex to pull CMIP, scenario, and season, then create or update corresponding model objects
            # TODO: Create HazardLayerDataset

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
