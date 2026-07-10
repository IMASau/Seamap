"""
Viewsets for Natural Hazards Atlas API endpoints.
"""
import catalogue.models
from . import models, serializers

import re
import requests
from django.core.cache import cache
from django.db.models import F, Prefetch, Value
from django.db.models.functions import Coalesce
from django.views.decorators.cache import cache_page
from rest_framework import viewsets
from rest_framework.decorators import action, api_view
from rest_framework.response import Response
from rest_framework.request import Request


# pylint: disable=line-too-long
# pylint: disable=missing-class-docstring

class LayerViewset(viewsets.ReadOnlyModelViewSet):
    """
    Viewset for Natural Hazards Atlas layers.
    Based on the LayerViewset in catalogue.viewsets, but modified to include the
    related HazardLayer model.
    """
    queryset = catalogue.models.Layer.objects.all() \
        .prefetch_related(
            'category',
            'data_classification',
            'organisation',
            'server_type',
            Prefetch(
                'hazardlayer__datasets',
                queryset=models.HazardLayerDataset.objects.select_related(
                    'cmip_phase',
                    'scientific_model',
                    'scenario',
                    'season',
                ),
            ),
        ) \
        .select_related('hazardlayer') \
        .annotate(sort_key_null=Coalesce('sort_key', Value('zzzzzzzz'))) \
        .order_by('sort_key_null', 'name')
    serializer_class = serializers.LayerSerializer

    def list(self, request, *args, **kwargs):
        """
        Override default list method to manually cache the response.
        By manually caching the response, we can give it an infinite timeout, so the
        cache may only be cleared by an explicit call to cache.clear()
        """
        cache_key = "nhatlayer_list"

        data = cache.get(cache_key)
        if data is not None:
            return Response(data)

        response = super().list(request, *args, **kwargs)

        cache.set(cache_key, response.data, timeout=None)
        return response


class CmipPhaseViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.CmipPhase.objects.all() \
        .order_by(F('sort_key').asc(nulls_last=True))
    serializer_class = serializers.CmipPhaseSerializer


class ScientificModelViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.ScientificModel.objects.all() \
        .order_by(F('sort_key').asc(nulls_last=True))
    serializer_class = serializers.ScientificModelSerializer


class ScenarioViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.Scenario.objects.all() \
        .order_by(F('sort_key').asc(nulls_last=True))
    serializer_class = serializers.ScenarioSerializer


class SeasonViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.Season.objects.all() \
        .order_by(F('sort_key').asc(nulls_last=True))
    serializer_class = serializers.SeasonSerializer


def _get_nhat_thredds_legend(layer: catalogue.models.Layer) -> str:
    """
    Constructs the URL for the legend graphic image for a Thredds Natural Hazards
    Atlas layer.

    Returns:
        str: The URL of the legend graphic image in PNG format.

    Example:
        >>> _get_nhat_thredds_legend(layer)
        "https://thredds...?service=WMS&version=1.1.1&request=GetLegendGraphic&layer=...&format=image/png&transparent=True"

    Note:
    - The method assumes that `layer.server_url` points to a valid Thredds server that
        returns data in the expected format.
    """
    server_url = layer.server_url
    params = {
        'service': 'WMS',
        'version': '1.1.1',
        'request': 'GetLegendGraphic',
        'layer': layer.layer_name,
        'format': 'image/png',
        'transparent': True,
        'style': 'default-scalar/psu-viridis',
    }
    if layer.style:
        params['style'] = layer.style

    if hasattr(layer, "hazardlayer"):
        dataset = layer.hazardlayer.datasets.all().first()
        server_url = dataset.server_url
        params.update({
            'layer': dataset.layer_name,
            'styles': f"default-scalar/{layer.hazardlayer.color_palette}",
            'colorscalerange': f"{dataset.color_scale_range_min},{dataset.color_scale_range_max}",
            'abovemaxcolor': layer.hazardlayer.above_max_color,
            'belowmincolor': layer.hazardlayer.below_min_color,
        })
    legend_url = requests.get(url=server_url, params=params, timeout=30).url
    assert isinstance(legend_url, str) # assert silences mypy strict type checking
    return legend_url


@action(methods=['GET'], detail=False)
@cache_page(60 * 15)
@api_view()
def layer_legend(_request: Request, layer_id: int) -> Response:
    """
    Get the legend for a layer in Natural Hazards Atlas.
    Based on the base layer_legend API view in habitat.viewsets, but modified to...
    """
    try:
        layer = catalogue.models.Layer.objects.get(id=layer_id)
    except catalogue.models.Layer.DoesNotExist:
        return Response("Layer not found", status=400)

    try:
        if layer.server_type.name == 'thredds':
            legend = _get_nhat_thredds_legend(layer)
        else:
            legend = layer.get_legend()
        return Response(legend)
    except ValueError:
        return Response("No legend available for this layer", status=400)
