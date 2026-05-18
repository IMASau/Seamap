import catalogue.serializers
from . import models

from rest_framework import serializers


class NhatLayerSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.NhatLayer
        fields = '__all__'


class LayerSerializer(catalogue.serializers.LayerSerializer):
    nhatlayer = NhatLayerSerializer(read_only=True)
