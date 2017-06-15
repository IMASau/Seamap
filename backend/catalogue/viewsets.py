from catalogue.models import Layer, LayerGroup, LayerGroupPriority
from catalogue.serializers import LayerSerializer, GroupSerializer, GroupPrioritySerializer
from rest_framework import viewsets


class LayerViewset(viewsets.ReadOnlyModelViewSet):
    queryset = Layer.objects.all()
    serializer_class = LayerSerializer


class GroupViewset(viewsets.ReadOnlyModelViewSet):
    queryset = LayerGroup.objects.all()
    serializer_class = GroupSerializer


class GroupPriorityViewset(viewsets.ReadOnlyModelViewSet):
    queryset = LayerGroupPriority.objects.all()
    serializer_class = GroupPrioritySerializer
