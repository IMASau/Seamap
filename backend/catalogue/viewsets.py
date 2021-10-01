# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from django.db.models import Value
from django.db.models.functions import Coalesce
from catalogue.models import Organisation, HabitatDescriptor, BaseLayer, Layer, LayerGroup, LayerGroupPriority, DataClassification
from catalogue.serializers import ClassificationSerializer, OrganisationSerializer, HabitatSerializer, BaseLayerSerializer, LayerSerializer, GroupSerializer, GroupPrioritySerializer
from rest_framework import viewsets


class OrganisationViewset(viewsets.ReadOnlyModelViewSet):
    queryset = Organisation.objects.all()
    serializer_class = OrganisationSerializer


class ClassificationViewset(viewsets.ReadOnlyModelViewSet):
    queryset = DataClassification.objects.all()
    serializer_class = ClassificationSerializer


class DescriptorViewset(viewsets.ReadOnlyModelViewSet):
    queryset = HabitatDescriptor.objects.all()
    serializer_class = HabitatSerializer


class LayerViewset(viewsets.ReadOnlyModelViewSet):
    queryset = Layer.objects.all().prefetch_related('category',
                                                    'data_classification',
                                                    'organisation',
                                                    'server_type') \
                                  .annotate(sort_key_null=Coalesce('sort_key', Value('zzzzzzzz'))) \
                                  .order_by('sort_key_null', 'name')
    serializer_class = LayerSerializer


class BaseLayerViewset(viewsets.ReadOnlyModelViewSet):
    queryset = BaseLayer.objects.all()
    serializer_class = BaseLayerSerializer


class GroupViewset(viewsets.ReadOnlyModelViewSet):
    queryset = LayerGroup.objects.all()
    serializer_class = GroupSerializer


class GroupPriorityViewset(viewsets.ReadOnlyModelViewSet):
    queryset = LayerGroupPriority.objects.all()
    serializer_class = GroupPrioritySerializer
