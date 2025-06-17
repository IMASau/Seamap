# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

import re
from typing import Union
from django.core.validators import MinValueValidator, RegexValidator
import django.utils.timezone
from django.db import models
from six import python_2_unicode_compatible
from uuid import uuid4
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


INFO_FORMAT_TYPE_CHOICES = [
    (1, '1 (text/html)'),
    (2, '2 (application/json)'),
    (3, '3 (none)'),
    (4, '4 (feature)'),
    (5, '5 (xml)'),
    (6, '6 (map-server)'),
]


LAYER_TYPE_CHOICES = [
    ('wms', 'wms'),
    ('tile', 'tile'),
    ('feature', 'feature'),
    ('map-server', 'map-server'),
    ('wms-non-tiled', 'wms-non-tiled'),
]


CRS_CHOICES = [
    ('EPSG:4326', 'EPSG:4326'),
    ('EPSG:3857', 'EPSG:3857'),
    ('EPSG:3112', 'EPSG:3112'),
    ('EPSG:3031', 'EPSG:3031'),
]


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
    info_format_type = models.IntegerField(
        choices=INFO_FORMAT_TYPE_CHOICES,
        help_text="""
            <p>Specifies format to retrieve <code>GetFeatureInfo</code> request (click-for-popup). Most IMAS-hosted layers will have a HTML styled popup configured (content.ftl file), but many external layers do not have this configured, so GFI requests <code>text/html</code> and unformatted text is returned. This column enables a flag to request <code>GetFeatureInfo</code> in 6 possible formats (allowed values: 1-6):</p>
            <ol>
                <li><code>GetFeatureInfo</code> as <code>INFO_FORMAT</code>=<code>text/html</code> (for “styled” popups)</li>
                <li><code>GetFeatureInfo</code> as <code>INFO_FORMAT</code>=<code>application/json</code> (for unstyled popups that can be represented in tabular format – defaults to a grey/white striped table using raw JSON)</li>
                <li>No GetFeatureInfo (displays message “no info available” – for unstyled data which can’t be represented in json)</li>
                <li>Feature info specifically for ESRI FeatureServer layers: will have <code>layer_type</code> = <code>feature</code></li>
                <li>Feature info specifically for List (+ maybe others) that return results in XML - will display similar to 2 in roughly styled table</li>
                <li>Feature info for ESRI MapServer layers (though not <code>tile</code> or <code>wms</code> MapServer layers). Will have <code>layer_type</code> = <code>map-server</code></li>
            </ol>
        """
    )
    keywords = models.CharField(max_length = 400, null=True, blank=True)
    style = models.CharField(max_length=200, null=True, blank=True)
    layer_type = models.CharField(
        max_length=200,
        choices=LAYER_TYPE_CHOICES,
        help_text="""
            <p>Seamap accommodates 5 different types of map layer:&nbsp;</p>
            <ol>
                <li><code>wms</code>: standard OGC/MapServer wms or WMSServer</li>
                <li><code>feature</code>: vector-styled layer from ESRI ArcGIS server</li>
                <li><code>map-server</code>: not <code>tile</code> or <code>wms</code> layer from ESRI ArcGIS MapServer</li>
                <li><code>tile</code>: tile server</li>
                <li><code>wms-non-tiled</code>: special case where we want a WMS request to be made of a single image of the layer, rather than a series of tiles. This is less efficient but is used sometimes for layers that use a global render (e.g. heatmaps)</li>
            </ol>
            <p>Extra info:</p>
            <ul>
                <li><code>wms</code> and <code>wms-non-tiled</code> (/wms and /WMSServer) layers require a <code>server_url</code> (e.g. <a target="_blank" rel="noopener noreferrer" href="https://geoserver.imas.utas.edu.au/geoserver/wms">https://geoserver.imas.utas.edu.au/geoserver/wms</a>) and <code>layer_name</code> (e.g. seamap:SeamapAus_TAS_AbHab_substrata)</li>
                <li><code>feature</code> layers (/FeatureServer) <i>only&nbsp;</i>require a <code>server_url</code>. The layer is identified as a trailing /x numeral: e.g. <a target="_blank" rel="noopener noreferrer" href="https://services3.arcgis.com/nbGeuo4JHMRYW4oj/arcgis/rest/services/Biologically_Important_Areas/FeatureServer/2">https://services3.arcgis.com/nbGeuo4JHMRYW4oj/arcgis/rest/services/Biologically_Important_Areas/FeatureServer/2</a></li>
                <li><code>map-server</code> layers (from /MapServer) are configured the same as <code>feature</code></li>
                <li><code>tile</code> layers&nbsp;<i>only</i> require a <code>server_url</code>, and must identify the x/y/z tile ordering in the <code>server_url</code>: e.g. <a target="_blank" rel="noopener noreferrer" href="https://services1.arcgis.com/wfNKYeHsOyaFyPw3/ArcGIS/rest/services/AIS_2020_Vessel_Tracks/MapServer/tile/{z}/{y}/{x}">https://services1.arcgis.com/wfNKYeHsOyaFyPw3/ArcGIS/rest/services/AIS_2020_Vessel_Tracks/MapServer/tile/{z}/{y}/{x}</a></li>
            </ul>
        """
    )
    tooltip = models.TextField(null=True, blank=True)
    metadata_summary = models.TextField(null=True, blank=True)
    crs = models.CharField(max_length=10, choices=CRS_CHOICES, default='EPSG:3112')
    regenerate_preview = models.BooleanField(
        default=True,
        help_text="Dictates if a layer should generate a new layer preview each week, even if a preview already exists. If no preview image exists for the layer, this property will be ignored."
    )

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

        result_record_count = 50
        result_offset = 0
        
        params = {
            'where':             '1=1',
            'outFields':         out_fields,
            'f':                 'geojson',
            'resultRecordCount': result_record_count,
            'recordOffset':      result_offset
        }

        r = requests.get(url=url, params=params, verify=False)
        geojson = r.json()

        while r.json()['features']:
            result_offset += result_record_count
            params = {
                'where':             '1=1',
                'outFields':         out_fields,
                'f':                 'geojson',
                'resultRecordCount': result_record_count,
                'resultOffset':      result_offset
            }
            r = requests.get(url=url, params=params, verify=False)
            print(r.url)
            geojson['features'] += r.json()['features']

        return geojson

    def server_info(self):
        url = self.server_url
        params = { 'f': 'json' }

        r = requests.get(url=url, params=params, verify=False)

        return r.json()

    def cql_property_values(self, cql_properties: list) -> dict:
        params = {
            'service': 'WFS',
            'version': '2.0.0',
            'request': 'GetFeature',
            'typeNames': self.layer_name,
            'outputFormat': 'application/json',
            'propertyName': f"({','.join(cql_properties)})",
        }
        r = requests.get(url=self.server_url, params=params, verify=False)
        data = r.json()

        features_cql_properties = [
            {
                cql_property: feature["properties"][cql_property]
                for cql_property in cql_properties
            }
            for feature in data["features"]
        ]

        value_combinations = [
            dict(y) for y in set(
                tuple(x.items())
                for x in features_cql_properties
            )
        ]

        values = [
            {
                'cql_property': cql_property,
                'values': sorted(
                    set(
                        value_combination[cql_property]
                        for value_combination in value_combinations
                    ),
                    key=lambda x: (x is None, x)
                )
            }
            for cql_property in cql_properties
        ]
        
        return {
            'values': values,
            'value_combinations': value_combinations
        }

    def feature_server_renderer_value_info_to_legend_key(self, value_info: dict) -> dict:
        """
        Converts a FeatureServer renderer value info dictionary into a legend key
        dictionary.

        Args:
            value_info (dict): A `valueInfo` dictionary from a FeatureServer renderer's
                `uniqueValueInfos`.

        Returns:
            dict: A dictionary representing a legend key with a `label` and `style`.

        Example:
            >>> self.feature_server_renderer_value_info_to_legend_key(renderer['uniqueValueInfos'][0])
            {
                'label': 'Feature A',
                'style': {
                    'backgroundColor': 'rgba(255,0,0,1)',
                    'border': '2px solid rgba(0,0,0,1)',
                    'height': '100%',
                    'width': '100%'
                }
            }

        Note:
        - If 'label' is empty or `None`, the function falls back to using 'name'.
        """
        
        # Construct a legend key dictionary
        style = {
            'height': '100%',
            'width': '100%'
        }

        if value_info['symbol'].get('color'):
            style['backgroundColor'] = f"rgba({','.join(map(str, value_info['symbol']['color']))})"
        if value_info['symbol'].get('outline'):
            style['border'] = f"2px solid rgba({','.join(map(str, value_info['symbol']['outline']['color']))})"

        return {
            'label': value_info.get('label', None) or value_info.get('name'),
            'style': style
        }

    def get_feature_server_legend(self) -> list[dict]:
        """
        Retrieves and constructs a list of legend keys for a FeatureServer layer.

        This function makes an HTTP GET request to the server specified by
        `self.server_url` to fetch JSON data. It then extracts the renderer information
        and checks if it contains `uniqueValueInfos`. Based on this check, it constructs
        and returns a list of legend keys.

        If `uniqueValueInfos` is present in the renderer, it converts each value info
        into a legend key using the `feature_server_renderer_value_info_to_legend_key`
        method. If `uniqueValueInfos` is not present, it returns a single legend key for
        the renderer itself.

        Returns:
            list[dict]: A list of dictionaries where each dictionary represents a legend
                key. Each legend key contains a `label` and `style`.

        Example:
            >>> self.get_feature_server_legend()
            [
                {
                    'label': 'Feature A',
                    'style': {
                        'backgroundColor': 'rgba(255,0,0,1)',
                        'border': '2px solid rgba(0,0,0,1)',
                        'height': '100%',
                        'width': '100%'
                    }
                },
                ...
            ]

        Note:
        - The method assumes that `self.server_url` points to a valid FeatureServer
            endpoint that returns data in the expected format.
        - The resulting legend keys are derived from either `uniqueValueInfos` or directly
            from the renderer if `uniqueValueInfos` is absent.
        """
        r = requests.get(url=self.server_url, params={ 'f': 'json' }) # Request server data
        data = r.json()
        renderer = data['drawingInfo']['renderer'] # Get renderer data
        if 'uniqueValueInfos' in renderer: # If renderer has uniqueValueInfos, then...
            # ...return a list of legend keys for each value info, else...
            return [self.feature_server_renderer_value_info_to_legend_key(value_info) for value_info in renderer['uniqueValueInfos']]
        else:
            # ...return a list containing a single legend key for the renderer
            return [self.feature_server_renderer_value_info_to_legend_key(renderer)]

    def map_server_layer_data(self) -> dict:
        """
        Retrieves and returns the MapServer layer data.
        
        Returns:
            dict: A dictionary of the MapServer layer data.
        """
        r = requests.get(url=self.server_url, params={ 'f': 'json' })
        return r.json()

    def map_server_legend_item_to_legend_key(self, legend_item: dict, legend_layer: dict) -> dict:
        """
        Converts a MapServer layer legend item into a legend key dictionary.

        Args:
            legend_item (dict): A dictionary representing a legend item from a MapServer
                legend's layer `legend`.
            legend_layer (dict): Optional. A dictionary of the legend layer data from
                MapServer, containing the `legend_item`.

        Returns:
            dict: A dictionary representing a legend key with a `label` and `image`.

        Example:
            >>> self.map_server_legend_item_to_legend_key(legend_layer['legend'][0])
            {
                'label': 'Feature A',
                'image': 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNwAAAABJRU5ErkJggg=='
            }
        """
        return {
            'label': legend_item['label'] or legend_layer['layerName'],
            'image': f"data:image/png;base64,{legend_item['imageData']}"
        }

    def get_map_server_legend(self) -> Union[str, list[dict]]:
        """
        Retrieves and constructs a list of legend keys for a MapServer layer.

        This function constructs a URL to fetch the legend for a specific map server
        layer based on `self.server_url`. It makes a HTTP GET request to retrieve legend
        data in JSON format, then extracts and processes the legend.

        Returns:
            Union[str, list[dict]]: A list of dictionaries where each dictionary represents
                a legend key. Each legend key contains a `label` and `image`.  If the legend
                data cannot be retrieved, returns the URL of the legend graphic image in PNG
                format.

        Example:
            >>> self.get_map_server_legend()
            [
                {
                    'label': 'Feature A',
                    'image': 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAAAAFCAYAAACNbyblAAAAHElEQVQI12P4//8/w38GIAXDIBKE0DHxgljNwAAAABJRU5ErkJggg=='
                },
                ...
            ] # if legend data can be retrieved
            >>> self.get_map_server_legend()
            "https://mapserver.../legend?service=WMS&request=GetLegendGraphic&layer=...&format=image/png" # if legend data cannot be retrieved

        Note:
        - The method assumes that `self.server_url` points to a valid MapServer endpoint
            that returns data in the expected format.
        """
        # First, try to get legend data in JSON format
        try:
            match = re.match(r'^(?P<map_server_url>.+)/(?P<map_server_layer_id>\d+)$', self.server_url)
            legend_url = f"{match.group('map_server_url')}/legend"
            map_server_layer_id = int(match.group('map_server_layer_id'))
            r = requests.get(url=legend_url, params={ 'f': 'json' })
            data = r.json()
            layer_data = self.map_server_layer_data()

            # group-layer case
            if layer_data['type'] == 'Group Layer':
                legend = []

                # Extract legend data for each sub-layer, and merge into a single legend
                for layer in layer_data['subLayers']:
                    legend_layer = next((legend_layer for legend_layer in data['layers'] if legend_layer['layerId'] == layer['id']))
                    legend += [self.map_server_legend_item_to_legend_key(legend_item, legend_layer) for legend_item in legend_layer['legend']]
                return legend

            # single-layer case
            else:
                legend_layer = next((legend_layer for legend_layer in data['layers'] if legend_layer['layerId'] == map_server_layer_id))
                return [self.map_server_legend_item_to_legend_key(legend_item, legend_layer) for legend_item in legend_layer['legend']]

        # If the legend data cannot be retrieved, return the legend graphic image URL
        except Exception as e:
            params = {
                'service': 'WMS',
                'version': '1.1.1',
                'request': 'GetLegendGraphic',
                'layer': self.layer_name,
                'format': 'image/png',
                'transparent': True,
            }
            if self.style:
                params['style'] = self.style
            return requests.get(url=self.server_url, params=params).url

    def geoserver_legend_rule_to_legend_key(self, legend_rule: dict) -> dict:
        """
        Converts a GeoServer legend rule into a legend key dictionary.

        Args:
            legend_rule (dict): A dictionary from a GeoServer legend's rules.

        Returns:
            dict: A dictionary representing a legend key with a `label` and `style`.

        Raises:
            ValueError: If the legend rule's symbolizer type is unsupported.

        Example:
            >>> self.geoserver_legend_rule_to_legend_key(geoserver_legend['Legend'][0]['rules'][0])
            {
                'label': 'Red Polygon',
                'style': {
                    'height': '100%',
                    'width': '100%',
                    'backgroundColor': '#ff0000',
                    'border': '2px solid #000000'
                }
            }

        Note:
        - The method handles 'Polygon' and 'Point' symbolizer types. Symbolizer types not
            supported by this method will raise a `ValueError`.
        """
        symbolizer_type = next(iter(legend_rule['symbolizers'][0]))
        symbolizer = legend_rule['symbolizers'][0][symbolizer_type]
        style = {}

        # Handle Polygon symbolizer type
        if symbolizer_type == 'Polygon':
            # Construct a legend key dictionary
            style = {
                'height': '100%',
                'width': '100%',
            }
            
            # Populate optional properties in the legend key
            if 'fill' in symbolizer:
                style['backgroundColor'] = symbolizer['fill']
            # Add a border if either stroke-width or stroke is present
            if ('stroke-width' in symbolizer) or ('stroke' in symbolizer):
                style['border'] = f"{symbolizer.get('stroke-width', 2)}px solid {symbolizer.get('stroke', 'black')}"

        # Handle Point symbolizer type
        elif symbolizer_type == 'Point':
            # Construct a legend key dictionary
            style = {
                'height': f"{symbolizer['size']}px",
                'width': f"{symbolizer['size']}px",
            }

            # Populate optional properties in the legend key
            graphic = symbolizer['graphics'][0]
            if 'fill' in graphic:
                style['backgroundColor'] = graphic['fill']
            # Add a border if either stroke-width or stroke is present
            if ('stroke-width' in graphic) or ('stroke' in graphic):
                style['border'] = f"{graphic.get('stroke-width', 2)}px solid {graphic.get('stroke', 'black')}"
            if 'mark' in graphic and graphic['mark'] == 'circle':
                style['borderRadius'] = '100%'
            if 'fill-opacity' in graphic:
                style['opacity'] = graphic['fill-opacity']

        # Unhandled symbolizer type
        else:
            raise ValueError(f"Unsupported symbolizer type: {symbolizer_type}")

        return {
            'label': legend_rule.get('title', None) or legend_rule.get('name'),
            'style': style
        }

    def get_geoserver_legend(self) -> Union[str, list[dict]]:
        """
        Retrieves and constructs a list of legend keys for a GeoServer layer.

        This method attempts to obtain the legend data for a GeoServer layer in JSON
        format. If the legend data can be converted into legend keys, it returns a list
        of these legend keys. If the legend cannot be processed it instead returns the
        URL of the legend graphic image in PNG format.

        Returns:
            Union[str, list[dict]]: If the legend data can be converted, returns a list of
                dictionaries where each dictionary represents a legend key. Each legend key
                contains a `label` and `style`. If the legend data cannot be converted, returns
                the URL of the legend graphic image in PNG format.

        Example:
            >>> self.get_geoserver_legend()
            [
                {
                    'label': 'Red Polygon',
                    'style': {
                        'height': '100%',
                        'width': '100%',
                        'backgroundColor': '#ff0000',
                        'border': '2px solid #000000'
                    }
                },
                ...
            ] # for successful conversion
            >>> self.get_geoserver_legend()
            "https://geoserver...?service=WMS&version=1.1.1&request=GetLegendGraphic&layer=...&format=image/png&transparent=True" # for unsuccessful conversion
        
        Note:
        - The method assumes that `self.server_url` points to a valid GeoServer endpoint
            that returns data in the expected format.
        """
        # First, try to get legend data in JSON format
        params = {
            'service': 'WMS',
            'version': '1.1.1',
            'request': 'GetLegendGraphic',
            'layer': self.layer_name,
            'format': 'application/json',
            'legend_options': 'forceLabels:on',
        }
        if self.style:
            params['style'] = self.style
        r = requests.get(url=self.server_url, params=params)
        
        try:
            data = r.json()
            legend_rules = data['Legend'][0]['rules'] # Get legend rules
            assert self.geoserver_legend_rule_to_legend_key(legend_rules[0]) # Check if the first legend rule can be converted
            return [self.geoserver_legend_rule_to_legend_key(legend_rule) for legend_rule in legend_rules] # Convert all legend rules
        except Exception as e:
            # If the legend rules cannot be converted, return the legend graphic image URL
            params = {
                'service': 'WMS',
                'version': '1.1.1',
                'request': 'GetLegendGraphic',
                'layer': self.layer_name,
                'format': 'image/png',
                'transparent': True,
            }
            if self.style:
                params['style'] = self.style
            return requests.get(url=self.server_url, params=params).url

    def get_legend(self) -> Union[str, list[dict]]:
        """
        Retrieves the legend for the current layer based on its type and configuration.

        This method determines the appropriate method to fetch the legend based on the
        type of layer and the availability of a legend URL. If a `legend_url` is
        provided, it is returned directly.

        Returns:
            Union[str, list[dict]]: If a `legend_url` is set, returns the `legend_url` as a
                string. For feature or map-server layers with a valid FeatureServer or MapServer
                URL, returns a list of dictionaries representing the legend keys. For WMS
                layers, returns the legend in the form of a list of dictionaries or a string URL
                depending on whether the legend is successfully converted or not.

        Example:
            >>> layer.get_legend()
            [
                {
                    'label': 'Red Polygon',
                    'style': {
                        'height': '100%',
                        'width': '100%',
                        'backgroundColor': '#ff0000',
                        'border': '2px solid #000000'
                    }
                },
                ...
            ]
        """
        # If a legend URL is provided, use that
        if self.legend_url:
            return self.legend_url

        # If the layer is a feature or map-server layer...
        if self.layer_type in ['feature', 'map-server']:
            # ...and has a FeatureServer URL, request legend data and transform
            if re.match(r'^(.+?)/services/(.+?)/FeatureServer/.+$', self.server_url):
                return self.get_feature_server_legend()
            
            # ...and has a MapServer URL, request legend data and transform
            else:
                return self.get_map_server_legend()
        
        # ...otherwise, if the layer is a WMS layer
        elif self.layer_type in ['wms', 'wms-non-tiled']:
            return self.get_geoserver_legend()

        # ...otherwise, layer type is not supported
        else:
            raise ValueError("Unsupported layer type")

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
    time_created = models.DateTimeField(default=django.utils.timezone.now)

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
    alternate_view_label = models.CharField(max_length=255, default='Alternate View')
    icon = models.CharField(max_length=255)
    tooltip = models.CharField(max_length=255)

    def __str__(self):
        return str(self.layer)

@python_2_unicode_compatible
class RichLayerAlternateView(models.Model):
    richlayer = models.ForeignKey(RichLayer, on_delete=models.PROTECT, related_name='alternate_views')
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    sort_key = models.CharField(max_length=10, null=True, blank=True)

@python_2_unicode_compatible
class RichLayerTimeline(models.Model):
    richlayer = models.ForeignKey(RichLayer, on_delete=models.PROTECT, related_name='timeline')
    layer = models.ForeignKey(Layer, on_delete=models.PROTECT)
    value = models.FloatField(null=False)
    label = models.CharField(max_length=255)

DATA_TYPE_CHOICES = [
    ('string', 'string'),
    ('number', 'number'),
]

CONTROLLER_TYPE_CHOICES = [
    ('slider', 'slider'),
    ('dropdown', 'dropdown'),
    ('multi-dropdown', 'multi-dropdown'),
]

class EmptyStringToNoneField(models.CharField):
    def get_prep_value(self, value):
        if value == '':
            return None
        return value

@python_2_unicode_compatible
class RichLayerControl(models.Model):
    richlayer = models.ForeignKey(RichLayer, on_delete=models.PROTECT, related_name='controls')
    cql_property = models.CharField(max_length=255)
    label = models.CharField(max_length=255)
    data_type = models.CharField(max_length=255, choices=DATA_TYPE_CHOICES)
    controller_type = models.CharField(max_length=255, choices=CONTROLLER_TYPE_CHOICES)
    icon = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    tooltip = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    default_value = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    show_invalid = models.BooleanField(
        default=True,
        help_text="""
            <p>Show invalid values in the dropdown/multi-dropdown rich layer control:</p>
            <ol>
                <li>If true, invalid values are visible but disabled.</li>
                <li>If false, invalid values are not visible.</li>
            </ol>
        """
    )

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


@python_2_unicode_compatible
class DynamicPill(models.Model):
    text = models.CharField(max_length=255)
    icon = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    tooltip = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    url = models.URLField(max_length=255)
    layers = models.ManyToManyField(
        Layer,
        through='DynamicPillLayer',
        through_fields=('dynamic_pill', 'layer')
    )
    # consider extracting control into a separate model, and adding as a one-to-one field?
    region_control_cql_property = models.CharField(max_length=255)
    region_control_label = models.CharField(max_length=255)
    region_control_data_type = models.CharField(max_length=255, choices=DATA_TYPE_CHOICES)
    region_control_controller_type = models.CharField(max_length=255, choices=CONTROLLER_TYPE_CHOICES)
    region_control_icon = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    region_control_tooltip = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    region_control_default_value = EmptyStringToNoneField(max_length=255, null=True, blank=True)

    def __str__(self):
        return self.text


@python_2_unicode_compatible
class DynamicPillLayer(models.Model):
    layer = models.ForeignKey(Layer, on_delete=models.CASCADE, db_column='layer_id')
    dynamic_pill = models.ForeignKey(DynamicPill, on_delete=models.CASCADE, related_name='dynamicpill_layers', db_column='dynamicpill_id')
    metadata = EmptyStringToNoneField(max_length=255, null=True, blank=True)
    
    class Meta:
        db_table = 'catalogue_dynamicpill_layers'


# Not really catalogue tables - are they better put somewhere else (e.g. sql app?)

@python_2_unicode_compatible
class SquidleAnnotationsData(models.Model):
    network = models.CharField(max_length=255, db_column='NETNAME')
    park = EmptyStringToNoneField(max_length=255, null=True, blank=True, db_column='RESNAME')
    depth_zone = EmptyStringToNoneField(max_length=255, null=True, blank=True, db_column='ZONENAME')
    highlights = models.BooleanField(db_column='HIGHLIGHTS')
    annotations_data = models.TextField(db_column='ANNOTATIONS_DATA')
    error = models.TextField()
    last_modified = models.DateTimeField(auto_now=True)

    def __str__(self):
        return f'{"⚠️ " if self.error else ""}{self.network}{(" > " + self.park) if self.park else ""} > {self.depth_zone if self.depth_zone else "All Depths"} {"(Highlights)" if self.highlights else "(No Highlights)"}'

    class Meta:
        db_table = 'squidle_annotations_data'
        unique_together = (('network', 'park', 'depth_zone'),)


# SQL Views

@python_2_unicode_compatible
class AmpDepthZones(models.Model):
    netname = models.CharField(max_length=255, null=False, blank=False, db_column='NETNAME', primary_key=True) # NETNAME is not unique, but Django models require a primary key
    resname = models.CharField(max_length=255, null=False, blank=False, db_column='RESNAME')
    zonename = models.CharField(max_length=255, null=False, blank=False, db_column='ZONENAME')
    min = models.IntegerField(db_column='MIN')
    max = models.IntegerField(db_column='MAX')

    def __str__(self):
        return self.netname + (f' > {self.resname}' if self.resname else '') + f': {self.zonename}'
    
    def save(self, **kwargs):
        raise NotImplementedError()
    
    class Meta:
        db_table = 'VW_AMP_DEPTHZONES'
        managed = False
        unique_together = (('netname', 'resname', 'zonename'),)

@python_2_unicode_compatible
class SquidleAnnotationsDataView(models.Model):
    network = models.CharField(max_length=255, db_column='NETNAME')
    park = EmptyStringToNoneField(max_length=255, null=True, blank=True, db_column='RESNAME')
    depth_zone = EmptyStringToNoneField(max_length=255, null=True, blank=True, db_column='ZONENAME')
    highlights = models.BooleanField(db_column='HIGHLIGHTS')
    annotations_data = models.TextField(db_column='ANNOTATIONS_DATA')

    def __str__(self):
        return f'{self.network}{(" > " + self.park) if self.park else ""} > {self.depth_zone if self.depth_zone else "All Depths"} {"(Highlights)" if self.highlights else "(No Highlights)"}'

    def save(self, **kwargs):
        raise NotImplementedError()

    class Meta:
        db_table = 'VW_squidle_annotations_data'
        managed = False
        unique_together = (('network', 'park', 'depth_zone'),)
