# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
import catalogue.models as models
import catalogue.serializers as serializers

from django.db.models import Prefetch, Value
from django.db.models.functions import Coalesce
from rest_framework import viewsets


class OrganisationViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.Organisation.objects.all()
    serializer_class = serializers.OrganisationSerializer


class ClassificationViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.DataClassification.objects.all()
    serializer_class = serializers.ClassificationSerializer


class DescriptorViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.HabitatDescriptor.objects.all()
    serializer_class = serializers.HabitatSerializer


class LayerViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.Layer.objects.all().prefetch_related('category',
                                                    'data_classification',
                                                    'organisation',
                                                    'server_type') \
                                  .annotate(sort_key_null=Coalesce('sort_key', Value('zzzzzzzz'))) \
                                  .order_by('sort_key_null', 'name')
    serializer_class = serializers.LayerSerializer


class BaseLayerViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.BaseLayer.objects.all()
    serializer_class = serializers.BaseLayerSerializer


class BaseLayerGroupViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.BaseLayerGroup.objects.all()
    serializer_class = serializers.BaseLayerGroupSerializer


class CategoryViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.Category.objects.all()
    serializer_class = serializers.CategorySerializer


class KeyedLayerViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.KeyedLayer.objects.all()
    serializer_class = serializers.KeyedLayerSerializer

class RichLayerViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.RichLayer.objects.prefetch_related(
        Prefetch(
            'alternate_views',
            queryset=models.RichLayerAlternateView.objects \
                .annotate(sort_key_null=Coalesce('sort_key', Value('zzzzzzzz'))) \
                .order_by('sort_key_null')
        ),
        Prefetch('timeline', queryset=models.RichLayerTimeline.objects.order_by('value')),
        Prefetch(
            'side_by_side_views',
            queryset=models.RichLayerSideBySideView.objects \
                .annotate(sort_key_null=Coalesce('sort_key', Value('zzzzzzzz'))) \
                .order_by('sort_key_null')
        ),
        'controls'
    ).all()
    serializer_class = serializers.RichLayerSerializer

class RegionReportViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.RegionReport.objects.all()
    serializer_class = serializers.RegionReportSerializer

class DynamicPillViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.DynamicPill.objects.prefetch_related('layers').all()
    serializer_class = serializers.DynamicPillSerializer

# SQL Views

class AmpDepthZonesViewset(viewsets.ReadOnlyModelViewSet):
    queryset = models.AmpDepthZones.objects.all()
    serializer_class = serializers.AmpDepthZonesSerializer
