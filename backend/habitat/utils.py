# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.

"""
Utility functions for the habitat app.
"""
import re
import xml.etree.ElementTree as ET
from datetime import datetime

import requests
from catalogue.models import Layer

# pylint: disable=line-too-long


def _get_geoserver_layer_timestamps(layer: Layer) -> list[str]:
    """
    Retrieves available timestamps for a temporal WMS layer from GeoServer.

    Args:
        layer (Layer): A Layer model for a WMS-compliant GeoServer endpoint.

    Returns:
        A list of timestamp strings as defined in the layer's WMS Dimension element.
        The timestamp strings will be in ISO-8601.
        Returns an empty list if the layer has no time dimension or if the layer is not
        found in the GetCapabilities document.

    Raises:
        requests.RequestException: If the HTTP request to the GeoServer fails.
        xml.etree.ElementTree.ParseError: If the GetCapabilities response is not
            valid XML.

    Example:
        >>> layer = Layer.objects.get(layer_name='s2_ls_combined')
        >>> get_geoserver_layer_timestamps(layer)
        ['1986-08-16', '1986-08-17', '1986-08-18', ...]
    """
    params = {
        'request': 'GetCapabilities',
        'service': 'WMS',
    }
    r = requests.get(url=layer.server_url, params=params, verify=False, timeout=30)

    # Parse XML
    root = ET.fromstring(r.text)
    ns = {'wms': 'http://www.opengis.net/wms'}
    for server_layer in root.findall('.//wms:Layer', ns):
        name_elem = server_layer.find('wms:Name', ns)
        if name_elem is not None and name_elem.text == layer.layer_name:
            dimension = server_layer.find('wms:Dimension[@name="time"]', ns)
            if dimension is not None:
                return dimension.text.split(',')
    return []


def _get_esri_layer_timestamps(layer: Layer) -> list[str]:
    """
    Retrieves available timestamps for a temporal ESRI layer.

    Args:
        layer (Layer): A Layer model for an ESRI MapServer or FeatureServer endpoint
            with temporal data.

    Returns:
        A list of timestamp strings in ISO-8601 format (YYYY-MM-DD).
        Returns an empty list if the layer has no features with time data.

    Raises:
        requests.RequestException: If the HTTP request to the ESRI server fails.
        KeyError: If the layer's server info does not contain timeInfo or
            startTimeField metadata.

    Example:
        >>> layer = Layer.objects.get(server_url='https://services9.arcgis.com/RHVPKKiFTONKtxq3/arcgis/rest/services/seaice_extent_S_v1/FeatureServer/0')
        >>> _get_esri_layer_timestamps(layer)
        ['1978-11-01', '1978-12-15', '1979-01-15', ...]
    """
    start_time_field = layer.server_info().get("timeInfo").get("startTimeField") # This will tell us what the temporal field we're querying for is
    params = {
        'where': '1=1',
        'f': 'geojson',
        'returnGeometry': False,
        'returnDistinctValues': True,
        'orderByFields': f"{start_time_field} ASC",
    }
    r = requests.get(f"{layer.server_url}/query", params=params, verify=False, timeout=30)
    data = r.json()

    # Extract timestamp values from feature properties and convert to ISO-8601
    timestamps = []
    for feature in data.get('features', []):
        epoch = feature['properties'][start_time_field]
        dt = datetime.fromtimestamp(epoch / 1000)
        timestamps.append(dt.strftime('%Y-%m-%d'))
    return timestamps

def get_layer_timestamps(layer: Layer) -> list[str]:
    """
    Retrieves available timestamps for a temporal layer.

    Automatically detects the layer type (ESRI or GeoServer WMS) based on the
    server_url pattern and delegates to the appropriate implementation.
    This is the primary function to use when retrieving timestamps for any temporal
    layer.

    Args:
        layer (Layer): A Layer model instance with temporal data. The layer can be
            from either an ESRI MapServer/FeatureServer or a WMS-compliant GeoServer.

    Returns:
        A list of timestamp strings in ISO-8601 format.
        For GeoServer layers, the format matches the WMS Dimension element.
        For ESRI layers, timestamps are converted from UNIX epoch to YYYY-MM-DD format.
        Returns an empty list if the layer has no time dimension.

    Raises:
        requests.RequestException: If the HTTP request to the server fails.
        xml.etree.ElementTree.ParseError: If the WMS GetCapabilities response is not
            valid XML (GeoServer layers only).
        KeyError: If the layer's server info is missing required metadata (ESRI layers only).

    Example:
        >>> # Works with both GeoServer and ESRI layers
        >>> layer = Layer.objects.get(layer_name='s2_ls_combined')
        >>> geoserver_layer = Layer.objects.get(layer_name='s2_ls_combined')
        >>> esri_layer = Layer.objects.get(server_url='https://services9.arcgis.com/RHVPKKiFTONKtxq3/arcgis/rest/services/seaice_extent_S_v1/FeatureServer/0')
        >>> get_layer_timestamps(geoserver_layer)
        ['1986-08-16', '1986-08-17', '1986-08-18', ...]
        >>> get_layer_timestamps(esri_layer)
        ['1978-11-01', '1978-12-15', '1979-01-15', ...]
    """
    if re.search(r'^(.+?)/services/(.+?)/(?:MapServer|FeatureServer)/(?![Ww][Mm][Ss][Ss]erver).+$', layer.server_url):
        return _get_esri_layer_timestamps(layer)
    else:
        return _get_geoserver_layer_timestamps(layer)
