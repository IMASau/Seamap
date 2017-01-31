from catalogue.models import Layer
from catalogue.serializers import LayerSerializer
from rest_framework import viewsets

class LayerViewset(viewsets.ReadOnlyModelViewSet):
    queryset = Layer.objects.all()
    serializer_class = LayerSerializer