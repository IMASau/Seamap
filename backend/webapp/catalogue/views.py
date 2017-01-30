from catalogue.models import Layer
from catalogue.serializers import LayerSerializer
from rest_framework import generics

class LayerList(generics.ListCreateAPIView):
	queryset = Layer.objects.all()
	serializer_class = LayerSerializer

class LayerDetail(generics.RetrieveUpdateDestroyAPIView):
	queryset = Layer.objects.all()
	serializer_class = LayerSerializer