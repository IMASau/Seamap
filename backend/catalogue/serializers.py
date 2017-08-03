from django.db.models import Min, Max
from rest_framework import serializers
from catalogue.models import HabitatDescriptor, Layer, LayerGroup, LayerGroupPriority


class HabitatSerializer(serializers.ModelSerializer):
    class Meta:
        model = HabitatDescriptor
        fields = ('name', 'title', 'colour')


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
        model = Layer
        exclude = ('minx', 'miny', 'maxx', 'maxy',)


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
        model = LayerGroup
        fields = '__all__'


class GroupPrioritySerializer(serializers.ModelSerializer):
    # We only want the ids here, so we don't need to follow the
    # foreign key relations here
    class Meta:
        model = LayerGroupPriority
        fields = ('layer', 'group', 'priority')
