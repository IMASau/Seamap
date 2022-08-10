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
        return {'west': obj.minx,
                'south': obj.miny,
                'east': obj.maxx,
                'north': obj.maxy}

    class Meta:
        model = models.Layer
        exclude = ('minx', 'miny', 'maxx', 'maxy', 'sort_key',)


class GroupSerializer(serializers.ModelSerializer):
    bounding_box = serializers.SerializerMethodField()

    def get_bounding_box(self, obj):
        bounds = obj.layerpriorities.aggregate(minx=Min('layer__minx'),
                                               miny=Min('layer__miny'),
                                               maxx=Max('layer__maxx'),
                                               maxy=Max('layer__maxy'))
        return {'west': bounds['minx'],
                'south': bounds['miny'],
                'east': bounds['maxx'],
                'north': bounds['maxy']}

    class Meta:
        model = models.LayerGroup
        fields = '__all__'


class GroupPrioritySerializer(serializers.ModelSerializer):
    # We only want the ids here, so we don't need to follow the
    # foreign key relations here
    class Meta:
        model = models.LayerGroupPriority
        fields = ('layer', 'group', 'priority')


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
        fields = ('keyword', 'layer')
