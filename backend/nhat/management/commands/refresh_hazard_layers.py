"""
Management command to load all hazard layers from a THREDDS server.
"""

from enum import Enum
import io
import re
import requests
import xarray as xr
import xml.etree.ElementTree as ET
from django.core.management.base import BaseCommand, CommandParser
from django.db import transaction
from typing import Any, NamedTuple, Optional

import catalogue.models
import nhat.models as models

# mypy: disable-error-code="misc"
# pylint: disable=line-too-long
# pylint: disable=missing-function-docstring
# pylint: disable=missing-class-docstring
# pylint: disable=redefined-outer-name

class DataCategory(Enum):
    ENSEMBLE_STATISTIC = "ensemble_statistic"
    MODEL = "model"

class NetCdfVariableAttributes(NamedTuple):
    name: str
    display_name: str
    data_category: DataCategory
    colour_scale_range_min: float
    colour_scale_range_max: float

class NetCdfAttributes(NamedTuple):
    category: str
    data_classification: str
    display_name: str
    minx: float
    miny: float
    maxx: float
    maxy: float
    tooltip: Optional[str]
    human_readable_units: str
    colour_palette: str
    variable_attributes: list[NetCdfVariableAttributes]

class Command(BaseCommand):
    NETCDF_NAME_RE = re.compile(r'^(?P<cmip>[^_]+)_.+_(?P<scenario>[^_]+)_(?P<season>[^_]+)\.nc$')
    THREDDS_XML_NS = {"t": "http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0"}
    server_url: str
    thredds_server_type: catalogue.models.ServerType
    is_failure = False
    warnings: set[str] = set()

    def retrieve_netcdf_names(self, catalog_ref_name: str) -> list[str]:
        """
        Retrieves all the NetCDF file names within a catalog ref on the THREDDs server.
        """
        catalog_url = f"{self.server_url}catalog/data/{catalog_ref_name}/catalog.xml"
        response = requests.get(catalog_url, timeout=30)
        response.raise_for_status()
        root = ET.fromstring(response.content)

        return [
            ds.attrib["name"]
            for ds in root.findall(".//t:dataset", self.THREDDS_XML_NS)
            if ds.attrib["name"].endswith(".nc")
        ]

    def update_or_create_hazard_layer(self, catalog_ref_name: str, netcdf_attributes: NetCdfAttributes) -> models.HazardLayer:
        """
        Updates a hazard layer and corresponding layer if it exists, else creates hazard
        layer and corresponding layers.

        `models.HazardLayer.objects.update_or_create` is not suitable for this purpose,
        because `layer` is a required field for HazardLayer. Supplying `layer` for
        `defaults` in `update_or_create` would create a new layer when one already
        exists, and not supplying it would error when the DB tries to create the
        `HazardLayer`, due to the required field constraint. 
        """
        category, _ = catalogue.models.Category.objects.get_or_create(name=netcdf_attributes.category)
        data_classification, _ = catalogue.models.DataClassification.objects.get_or_create(name=netcdf_attributes.data_classification)
        organisation, _ = catalogue.models.Organisation.objects.get_or_create(name="Climate Futures")
        hazard_layer: models.HazardLayer
        layer_fields = {
            "name": netcdf_attributes.display_name,
            "server_url": f"{self.server_url}wms/data/",
            "category": category,
            "data_classification": data_classification,
            "organisation": organisation,
            "minx": netcdf_attributes.minx,
            "miny": netcdf_attributes.miny,
            "maxx": netcdf_attributes.maxx,
            "maxy": netcdf_attributes.maxy,
            "server_type": self.thredds_server_type,
            "info_format_type": 5,
            "layer_type": "wms-timeseries",
            "tooltip": netcdf_attributes.tooltip,
            "crs": "EPSG:4326",
            "download_format": "thredds-wcs",
        }
        try:
            hazard_layer = models.HazardLayer.objects.get(name=catalog_ref_name)
            for field, value in layer_fields.items():
                setattr(hazard_layer.layer, field, value)
            hazard_layer.layer.save()
            hazard_layer.color_palette = netcdf_attributes.colour_palette # "color" vs "colour" discrepancy noted
            hazard_layer.human_readable_units = netcdf_attributes.human_readable_units
            hazard_layer.save()
        except models.HazardLayer.DoesNotExist:
            layer = catalogue.models.Layer.objects.create(**layer_fields)
            hazard_layer = models.HazardLayer.objects.create(
                layer=layer,
                name=catalog_ref_name,
                color_palette=netcdf_attributes.colour_palette, # "color" vs "colour" discrepancy noted
                human_readable_units=netcdf_attributes.human_readable_units,
            )
        return hazard_layer

    def parse_netcdf_name(self, netcdf_name: str) -> dict[str, str]:
        """
        Parse the CMIP, scenario, and season from the NetCDF name using regex.

        Using regex over the file name is unreliable; suggest to Climate Futures
        including these as NetCDF global attributes in the future.
        """
        match = self.NETCDF_NAME_RE.match(netcdf_name)
        if not match:
            raise ValueError(f"Could not parse NetCDF name: {netcdf_name}")
        return match.groupdict()

    def retrieve_netcdf_catalog_ref_names(self) -> list[str]:
        """
        Retrieves the "catalog refs" from the THREDDs server.

        Each of these is a directory containing a group of NetCDF files, collectively
        making up a single hazard layer.
        """
        catalog_url = f"{self.server_url}catalog/data/catalog.xml"
        response = requests.get(catalog_url, timeout=30)
        response.raise_for_status()
        root = ET.fromstring(response.content)

        return [
            ds.attrib["name"]
            for ds in root.findall(".//t:catalogRef", self.THREDDS_XML_NS)
        ]

    def retrieve_netcdf_attributes(self, netcdf_url: str) -> NetCdfAttributes:
        """Retrieve a NetCDF file, and read its global and variable attributes."""
        self.stdout.write(f"Loading NetCDF from {netcdf_url}...")
        response = requests.get(netcdf_url, timeout=30)
        response.raise_for_status()
        with xr.open_dataset(io.BytesIO(response.content)) as ds:
            netcdf_variable_attributes = [
                NetCdfVariableAttributes(
                    name=data_var_name,
                    display_name=ds[data_var_name].attrs["long_name"],
                    data_category=DataCategory(ds[data_var_name].attrs["data_category"]),
                    colour_scale_range_min=ds[data_var_name].attrs["colour_scale_range_min"],
                    colour_scale_range_max=ds[data_var_name].attrs["colour_scale_range_max"],
                )
                for data_var_name in ds.data_vars
            ]
            netcdf_attributes = NetCdfAttributes(
                category=ds.attrs["category"],
                data_classification=ds.attrs["data_classification"],
                display_name=ds.attrs["data_classification"],
                minx=ds.attrs["minx"],
                miny=ds.attrs["miny"],
                maxx=ds.attrs["maxx"],
                maxy=ds.attrs["maxy"],
                tooltip=ds.attrs.get("tooltip"), # tooltip not required
                human_readable_units=ds.attrs["human_readable_units"],
                colour_palette=ds.attrs["colour_palette"],
                variable_attributes=netcdf_variable_attributes
            )
            return netcdf_attributes

    def load_hazard_layer(self, catalog_ref_name: str) -> None:
        """Load a hazard layer from the data within the NetCDFs of the catalog ref."""
        netcdf_names = self.retrieve_netcdf_names(catalog_ref_name)
        for netcdf_name in netcdf_names:
            netcdf_url = f"{self.server_url}fileServer/data/{catalog_ref_name}/{netcdf_name}"
            netcdf_attributes = self.retrieve_netcdf_attributes(netcdf_url)
            parsed = self.parse_netcdf_name(netcdf_name)
            cmip_name, scenario_name, season_name = parsed["cmip"], parsed["scenario"], parsed["season"]
            is_historical = scenario_name == "historical"

            # Create/update hazard layer with rollback
            with transaction.atomic():
                cmip, _ = models.CmipPhase.objects.update_or_create(name=cmip_name, defaults={"display_name": cmip_name})
                scenario = None
                if not is_historical:
                    scenario, _ = models.Scenario.objects.update_or_create(name=scenario_name, defaults={"display_name": scenario_name})
                season, _ = models.Season.objects.update_or_create(name=season_name, defaults={"display_name": season_name})
                hazard_layer = self.update_or_create_hazard_layer(catalog_ref_name, netcdf_attributes)
                # Probably some efficiency to be gained by bulk updating, but this is fine for now
                for netcdf_variable_attributes in netcdf_attributes.variable_attributes:
                    model, _ = models.ScientificModel.objects.update_or_create(
                        name=netcdf_variable_attributes.name,
                        defaults={
                            "display_name": netcdf_variable_attributes.display_name,
                            "data_category": netcdf_variable_attributes.data_category,
                        },
                    )
                    if netcdf_variable_attributes.colour_scale_range_min > netcdf_variable_attributes.colour_scale_range_max:
                        self.warnings.add("Some hazard layer colour scale ranges have min values greater than their max values")
                    models.HazardLayerDataset.objects.update_or_create(
                        hazard_layer=hazard_layer,
                        scientific_model=model,
                        cmip_phase=cmip,
                        scenario=scenario,
                        season=season,
                        is_historical=is_historical,
                        defaults={
                            "color_scale_range_min": netcdf_variable_attributes.colour_scale_range_min, # "color" vs "colour" discrepancy noted
                            "color_scale_range_max": netcdf_variable_attributes.colour_scale_range_max, # "color" vs "colour" discrepancy noted
                        }
                    )

    def load_hazard_layers(self) -> None:
        """Load hazard layers from a THREDDS server."""
        catalog_ref_names = self.retrieve_netcdf_catalog_ref_names()
        for catalog_ref_name in catalog_ref_names:
            try:
                self.load_hazard_layer(catalog_ref_name)
            except Exception: # pylint: disable=broad-except
                self.is_failure = True

    def add_arguments(self, parser: CommandParser) -> None:
        parser.add_argument(
            '--server_url',
            type=str,
            required=True,
            help='The base URL of the THREDDS server to load hazard layers from.',
        )

    def handle(self, *_args: Any, **options: Any) -> None:
        self.server_url = options['server_url']
        self.thredds_server_type, _ = catalogue.models.ServerType.objects.get_or_create(name="thredds")
        self.stdout.write(f"Loading hazard layers from {self.server_url}...")
        self.load_hazard_layers()
        self.stdout.write(self.style.SUCCESS("Successfully loaded hazard layers."))
