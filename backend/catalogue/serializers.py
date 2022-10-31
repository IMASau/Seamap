# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
import catalogue.models as models

from django.db.models import Min, Max
from rest_framework import serializers


class OrganisationSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.Organisation
        fields = ('id', 'name', 'logo', 'sort_key')


class ClassificationSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.DataClassification
        fields = ('id', 'name', 'sort_key')


class HabitatSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.HabitatDescriptor
        fields = ('name', 'title', 'colour')


class BaseLayerSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.BaseLayer
        fields = '__all__'


class LayerSerializer(serializers.ModelSerializer):
    category = serializers.SerializerMethodField()
    server_type = serializers.SerializerMethodField()
    data_classification = serializers.SerializerMethodField()
    organisation = serializers.SerializerMethodField()
    bounding_box = serializers.SerializerMethodField()

    def get_category(self, obj):
        return obj.category.name

    def get_server_type(self, obj):
        return obj.server_type.name

    def get_data_classification(self, obj):
        return getattr(obj.data_classification, 'name', None)

    def get_organisation(self, obj):
        return getattr(obj.organisation, 'name', None)

    def get_bounding_box(self, obj):
        return {'west': float(obj.minx),
                'south': float(obj.miny),
                'east': float(obj.maxx),
                'north': float(obj.maxy)}

    class Meta:
        model = models.Layer
        exclude = ('minx', 'miny', 'maxx', 'maxy', 'sort_key',)


class BaseLayerGroupSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.BaseLayerGroup
        fields = '__all__'


class SaveStateSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.SaveState
        fields = '__all__'

class CategorySerializer(serializers.ModelSerializer):
    class Meta:
        model = models.Category
        fields = '__all__'

class KeyedLayerSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.KeyedLayer
        fields = ('keyword', 'layer', 'sort_key')

class NationalLayerTimelineSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.NationalLayerTimeline
        fields = '__all__'

class RegionReportSerializer(serializers.ModelSerializer):
    parks = serializers.SerializerMethodField()
    network = serializers.SerializerMethodField()
    all_layers = serializers.SerializerMethodField()
    all_layers_boundary = serializers.SerializerMethodField()
    public_layers = serializers.SerializerMethodField()
    public_layers_boundary = serializers.SerializerMethodField()
    pressures = serializers.SerializerMethodField()

    def get_parks(self, obj):
        return [{'park': v.park, 'slug': v.slug} for v in models.RegionReport.objects.filter(network=obj.network) if v.park] if obj.park == None else None

    def get_network(self, obj):
        v = models.RegionReport.objects.get(network=obj.network, park=None)
        return {'network': v.network, 'slug': v.slug}

    def get_all_layers(self, obj):
        return [LayerSerializer(v.layer).data for v in models.KeyedLayer.objects.filter(keyword='data-report-minimap-panel1').order_by('-sort_key')]

    def get_all_layers_boundary(self, obj):
        return LayerSerializer(models.KeyedLayer.objects.get(keyword='data-report-minimap-panel1-boundary').layer).data

    def get_public_layers(self, obj):
        return [LayerSerializer(v.layer).data for v in models.KeyedLayer.objects.filter(keyword='data-report-minimap-panel2').order_by('-sort_key')]

    def get_public_layers_boundary(self, obj):
        return LayerSerializer(models.KeyedLayer.objects.get(keyword='data-report-minimap-panel2-boundary').layer).data

    def get_pressures(self, obj):
        return [PressureSerializer(v).data for v in models.Pressure.objects.filter(region_report=obj.id)]

    class Meta:
        model = models.RegionReport
        exclude = ('id',)

class PressureSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.Pressure
        exclude = ('region_report',)
