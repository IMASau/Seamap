"""REST Framework model serializers for the NHAT app."""
import catalogue.serializers
from . import models

from rest_framework import serializers


# pylint: disable=missing-class-docstring

class CmipPhaseSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.CmipPhase
        fields = '__all__'


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


class HazardLayerDatasetSerializer(serializers.ModelSerializer):
    cmip_phase = serializers.SlugRelatedField(
        read_only=True,
        slug_field='name'
    )
    scientific_model = serializers.SlugRelatedField(
        read_only=True,
        slug_field='name'
    )
    scenario = serializers.SlugRelatedField(
        read_only=True,
        slug_field='name'
    )
    season = serializers.SlugRelatedField(
        read_only=True,
        slug_field='name'
    )
    server_url = serializers.ReadOnlyField()
    layer_name = serializers.ReadOnlyField()

    class Meta:
        model = models.HazardLayerDataset
        exclude = ('hazard_layer',) # Don't need, since this is a nested serializer in HazardLayerSerializer


class HazardLayerSerializer(serializers.ModelSerializer):
    datasets = HazardLayerDatasetSerializer(many=True, read_only=True)
    class Meta:
        model = models.HazardLayer
        exclude = ('layer',) # Don't need, since this is a nested serializer in LayerSerializer


class LayerSerializer(catalogue.serializers.LayerSerializer):
    hazardlayer = HazardLayerSerializer(read_only=True)
