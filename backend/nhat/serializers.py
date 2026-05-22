"""REST Framework model serializers for the NHAT app."""
import catalogue.serializers
from . import models

from rest_framework import serializers


# pylint: disable=missing-class-docstring

class HazardLayerSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.HazardLayer
        fields = '__all__'


class LayerSerializer(catalogue.serializers.LayerSerializer):
    hazardlayer = HazardLayerSerializer(read_only=True)


class ScientificModelSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.ScientificModel
        fields = '__all__'


class ScenarioSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.Scenario
        fields = '__all__'


class SeasonSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.Season
        fields = '__all__'
