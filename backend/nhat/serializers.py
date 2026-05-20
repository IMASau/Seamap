import catalogue.serializers
from . import models

from rest_framework import serializers


class HazardLayerSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.HazardLayer
        fields = '__all__'


class LayerSerializer(catalogue.serializers.LayerSerializer):
    hazardlayer = HazardLayerSerializer(read_only=True)
