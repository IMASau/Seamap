# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

from django.core.validators import MinValueValidator, RegexValidator
from django.db import models
from six import python_2_unicode_compatible
from uuid import uuid4
from datetime import datetime
import requests
import xml.etree.ElementTree as ET 


@python_2_unicode_compatible
class Category(models.Model):
    "Category for semantic grouping in the UI; eg bathymetry or habitat"
    name = models.CharField(max_length = 200)
    display_name = models.CharField(max_length = 200, null=True)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class DataClassification(models.Model):
    """Category for data-type grouping; eg SST, chlorophyll, etc (relevant
    for third-party layers)"""
    name = models.CharField(max_length = 200)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class ServerType(models.Model):
    name = models.CharField(max_length = 200)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class Organisation(models.Model):
    name = models.CharField(max_length = 200)
    logo = models.CharField(max_length = 50, blank=True, null=True)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.name


# Habitat descriptors; ie, how we map from a habitat key to a colour
# and (optionally) a title:

colour_validator = RegexValidator(regex=r'^#[0-9a-fA-F]{6,6}$',
                                  message="Colour must be in hexadecimal format")

@python_2_unicode_compatible
class HabitatDescriptor(models.Model):
    name = models.CharField(max_length=200, unique=True)
    colour = models.CharField(max_length=7, validators=[colour_validator])
    title = models.CharField(max_length=200, blank=True, null=True)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class Layer(models.Model):
    name = models.CharField(max_length = 200)
    server_url = models.URLField(max_length = 200)
    legend_url = models.URLField(max_length = 250, null=True, blank=True)
    layer_name = models.CharField(max_length = 200)
    detail_layer = models.CharField(max_length = 200, blank=True, null=True)
    table_name = models.CharField(max_length = 200, blank=True, null=True)
    category = models.ForeignKey(Category, on_delete=models.PROTECT)
    data_classification = models.ForeignKey(DataClassification, blank=True, null=True, on_delete=models.PROTECT)
    organisation = models.ForeignKey(Organisation, blank=True, null=True, on_delete=models.PROTECT)
    # Bounding box; store as four separate fields
    minx = models.DecimalField(max_digits=20, decimal_places=17)
    miny = models.DecimalField(max_digits=20, decimal_places=17)
    maxx = models.DecimalField(max_digits=20, decimal_places=17)
    maxy = models.DecimalField(max_digits=20, decimal_places=17)
    metadata_url = models.URLField(max_length = 250)
    server_type = models.ForeignKey(ServerType, on_delete=models.PROTECT)
    sort_key = models.CharField(max_length=10, null=True, blank=True)
    info_format_type = models.IntegerField()
    keywords = models.CharField(max_length = 400, null=True, blank=True)
    style = models.CharField(max_length=200, null=True, blank=True)
    layer_type = models.CharField(max_length=10)
    tooltip = models.TextField(null=True, blank=True)
    metadata_summary = models.TextField(null=True, blank=True)

    def __str__(self):
        return self.name
    
    def get_geoserver_wfs_capabilities(self) -> ET.ElementTree:
        """Retrieves the WFS GetCapabilities document from the GeoServer layer.

        This function sends a GetCapabilities request to the GeoServer specified by the
        instance's `server_url` and parses the returned XML document into an ElementTree.
        It ensures the server type is 'geoserver' before making the request.

        Returns:
            xml.etree.ElementTree.ElementTree: The parsed GetCapabilities document.

        Raises:
            Exception: If the server type is not 'geoserver'.
            Exception: If the capabilities document cannot be retrieved or parsed.
        """
        if self.server_type.name != 'geoserver':
            raise Exception(f"Cannot retrieve WFS capabilities from non-GeoServer server type '{self.server_type.name}'")

        params = {
            'request': 'GetCapabilities',
            'service': 'WFS'
        }
        r = requests.get(url=self.server_url, params=params, verify=False)
        if r.status_code != 200:
            raise Exception(f"Cannot retrieve WFS capabilities from geoserver ({self.server_url})")

        return ET.ElementTree(ET.fromstring(r.text))

    def get_feature_is_supported(self) -> bool:
        """Determines if the layer supports the WFS GetFeature request.

        This function checks the layer's WFS capabilities document to see if the
        GetFeature operation is supported.

        Returns:
            bool: True if the GetFeature operation is supported, False otherwise.

        Raises:
            Exception: If the server type is not 'geoserver'.
            Exception: If the capabilities document cannot be retrieved or parsed.
        """
        root = self.get_geoserver_wfs_capabilities().getroot()
        namespaces = {
            'wfs': 'http://www.opengis.net/wfs/2.0',
            'ows': 'http://www.opengis.net/ows/1.1'
        }

        # Check if GetFeature operation is supported
        operations_metadata = root.find('ows:OperationsMetadata', namespaces)
        feature_type_list = root.find('wfs:FeatureTypeList', namespaces)
        if operations_metadata is not None:
            for operation in operations_metadata.findall('ows:Operation', namespaces):
                if operation.attrib.get('name') == 'GetFeature':
                    break
            else:
                return False
        else:
            return False

        # Check if the specific feature type is listed
        feature_type_list = root.find('wfs:FeatureTypeList', namespaces)
        if feature_type_list is not None:
            for feature_type in feature_type_list.findall('wfs:FeatureType', namespaces):
                name_element = feature_type.find('wfs:Name', namespaces)
                if name_element is not None and name_element.text == self.layer_name:
                    return True
        return False
    
    def geojson(self, out_fields: str=None):
        url = f"{self.server_url}/query"
        params = {
            'where':     '1=1',
            'outFields': out_fields,
            'f':         'geojson'
        }

        r = requests.get(url=url, params=params, verify=False)

        return r.json()

    def server_info(self):
        url = self.server_url
        params = { 'f': 'json' }

        r = requests.get(url=url, params=params, verify=False)

        return r.json()
    
    def bounds(self):
        return {
            'north': float(self.maxy),
            'south': float(self.miny),
            'east':  float(self.maxx),
            'west':  float(self.minx)
        }

@python_2_unicode_compatible
class BaseLayerGroup(models.Model):
    name = models.CharField(max_length = 200)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class BaseLayer(models.Model):
    name = models.CharField(max_length = 200, unique=True)
    server_url = models.URLField(max_length = 200)
    attribution = models.CharField(max_length = 200)
    sort_key = models.CharField(max_length=10, null=True, blank=True)
    layer_group = models.ForeignKey(BaseLayerGroup, blank=True, null=True, on_delete=models.PROTECT, db_column='layer_group')
    layer_type = models.CharField(max_length=10)

    def __str__(self):
        return self.name


@python_2_unicode_compatible
class SaveState(models.Model):
    id = models.UUIDField(primary_key=True, default=uuid4, editable=False)
    hashstate = models.CharField(max_length = 8000)
    description = models.TextField(blank=True, null=True)
    time_created = models.DateTimeField(default=datetime.now())

    def __str__(self):
        return self.description or '{id} ({time_created})'.format(
            id=self.id,
            time_created=self.time_created.strftime('%Y/%m/%d, %H:%M:%S')
        )

@python_2_unicode_compatible
class KeyedLayer(models.Model):
    keyword = models.CharField(max_length = 200)
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    description = models.TextField(null=True, blank=True)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

    def __str__(self):
        return self.keyword

@python_2_unicode_compatible
class RichLayer(models.Model):
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    tab_label = models.CharField(max_length=255)
    slider_label = models.CharField(max_length=255)
    icon = models.CharField(max_length=255)
    tooltip = models.CharField(max_length=255)

    def __str__(self):
        return str(self.layer)

@python_2_unicode_compatible
class RichLayerAlternateView(models.Model):
    richlayer = models.ForeignKey(RichLayer, on_delete=models.PROTECT)
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

@python_2_unicode_compatible
class RichLayerTimeline(models.Model):
    richlayer = models.ForeignKey(RichLayer, on_delete=models.PROTECT)
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    value = models.FloatField(null=False)
    label = models.CharField(max_length=255)

class EmptyStringToNoneField(models.CharField):
    def get_prep_value(self, value):
        if value == '':
            return None  
        return value

@python_2_unicode_compatible
class RegionReport(models.Model):
    network = models.CharField(max_length=254, null=False, blank=False)
    park = EmptyStringToNoneField(max_length=254, null=True, blank=True)
    habitat_state = models.FloatField(default=0)
    bathymetry_state = models.FloatField(default=0)
    habitat_observations_state = models.FloatField(default=0)
    state_summary = models.TextField()
    slug = models.CharField(max_length=255, null=False, blank=False)
    minx = models.FloatField("Max X", null=False)
    miny = models.FloatField("Max Y", null=False)
    maxx = models.FloatField("Min X", null=False)
    maxy = models.FloatField("Min Y", null=False)

    def __str__(self):
        return self.network + (f' > {self.park}' if self.park else '')


@python_2_unicode_compatible
class Pressure(models.Model):
    region_report = models.ForeignKey(RegionReport, on_delete=models.CASCADE)
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    category = models.CharField(max_length=200, null=False, blank=False)

    def __str__(self):
        return f'{self.region_report}: {self.layer}'
