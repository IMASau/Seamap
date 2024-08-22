# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
import catalogue.models as models

from django.db.models import Value
from django.db.models.functions import Coalesce
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
        exclude = ('minx', 'miny', 'maxx', 'maxy', 'sort_key', 'regenerate_preview')


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

class RichLayerAlternateViewSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.RichLayerAlternateView
        exclude = ('id', 'richlayer',)

class RichLayerTimelineSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.RichLayerTimeline
        exclude = ('id', 'richlayer',)

class RichLayerControlSerializer(serializers.ModelSerializer):
    default_value = serializers.SerializerMethodField()
    
    def get_default_value(self, obj):
        try: 
            if obj.default_value and obj.data_type == 'number':
                return float(obj.default_value)
        except ValueError:
            return obj.default_value
    
    class Meta:
        model = models.RichLayerControl
        exclude = ('id', 'richlayer',)

class RichLayerSerializer(serializers.ModelSerializer):
    alternate_views = RichLayerAlternateViewSerializer(many=True, read_only=True)
    timeline = RichLayerTimelineSerializer(many=True, read_only=True)
    controls = RichLayerControlSerializer(many=True, read_only=True)

    class Meta:
        model = models.RichLayer
        fields = '__all__'

class RegionReportSerializer(serializers.ModelSerializer):
    bounding_box = serializers.SerializerMethodField()

    def get_bounding_box(self, obj):
        return {
            'west': float(obj.minx),
            'south': float(obj.miny),
            'east': float(obj.maxx),
            'north': float(obj.maxy)
        }

    class Meta:
        model = models.RegionReport
        exclude = ('id','minx', 'miny', 'maxx', 'maxy',)

class PressureSerializer(serializers.ModelSerializer):
    label = serializers.SerializerMethodField()

    def get_label(self, obj):
        return obj.layer.name
    class Meta:
        model = models.Pressure
        exclude = ('region_report',)


class DynamicPillLayerSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.DynamicPillLayer
        fields = ('layer', 'metadata')

class DynamicPillSerializer(serializers.ModelSerializer):
    region_control = serializers.SerializerMethodField()
    layers = serializers.SerializerMethodField()

    def get_region_control(self, obj):
        return {
            'cql_property': obj.region_control_cql_property,
            'label': obj.region_control_label,
            'data_type': obj.region_control_data_type,
            'controller_type': obj.region_control_controller_type,
            'icon': obj.region_control_icon,
            'tooltip': obj.region_control_tooltip,
            'default_value': obj.region_control_default_value,
        }
    
    def get_layers(self, obj):
        dynamic_pill_layers = models.DynamicPillLayer.objects.filter(dynamic_pill=obj)
        return DynamicPillLayerSerializer(dynamic_pill_layers, many=True).data

    class Meta:
        model = models.DynamicPill
        exclude = ('region_control_cql_property', 'region_control_label', 'region_control_data_type', 'region_control_controller_type', 'region_control_icon', 'region_control_tooltip', 'region_control_default_value',)
