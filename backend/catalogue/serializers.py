# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
import itertools
import logging

import numpy
from pyproj import CRS, Transformer
from rest_framework import serializers

import catalogue.models as models

logger = logging.getLogger(__name__)


transformer = Transformer.from_crs("epsg:4326", "epsg:3031", always_xy=True)
inverter = Transformer.from_crs("epsg:3031", "epsg:4326", always_xy=True)
crs = CRS("epsg:3031")

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
        CUTOFF_RATIO = 0.5
        usable_bbox = crs.area_of_use
        # Hack: because we know the CRS goes from 90 to 60, to
        # calculate the area of overlap we can just look at the
        # vertical ratio of segments above and below the northern CRS
        # edge:
        crs_top = usable_bbox.north
        if crs_top >= obj.maxy:
            overlap_ratio = 1
        elif crs_top <= obj.miny:
            overlap_ratio = 0
        else:
            portion_above = float(obj.maxy) - crs_top
            portion_below = crs_top - float(obj.miny)
            overlap_ratio = portion_below / (portion_above + portion_below)
        if overlap_ratio < CUTOFF_RATIO:
            # return unmodified:
            return {"west": obj.minx, "south": obj.miny, "east": obj.maxx, "north": obj.maxy}
        # https://gis.stackexchange.com/a/197336
        p_0 = numpy.array((float(obj.minx), float(obj.miny)))
        p_1 = numpy.array((float(obj.minx), float(obj.maxy)))
        p_2 = numpy.array((float(obj.maxx), float(obj.miny)))
        p_3 = numpy.array((float(obj.maxx), float(obj.maxy)))
        edge_samples = 11
        _transform = lambda p: transformer.transform(p[0], p[1])
        # list of list of points for each edge; note the '+' does
        # element-wise addition for numpy arrays, so this sneakily
        # iterates along each edge:
        edge_pts = [[_transform(x*i + y*(1-i)) for i in numpy.linspace(0,1,edge_samples)]
                    for x,y in [(p_0, p_1),
                                (p_1, p_2),
                                (p_2, p_3),
                                (p_3, p_0)]]
        # Flatten out (we just want all points; "edge" isn't a useful
        # concept at this point, we just want the extremes of each
        # axis to construct a new bounding box from):
        edge_pts = list( itertools.chain.from_iterable(edge_pts) )
        xs = [p[0] for p in edge_pts]
        ys = [p[1] for p in edge_pts]
        minx = min(xs)
        maxx = max(xs)
        miny = min(ys)
        maxy = max(ys)

        # now transform back again:
        minx, miny = inverter.transform(minx, miny)
        maxx, maxy = inverter.transform(maxx, maxy)
        return {"west": minx, "south": miny, "east": maxx, "north": maxy}

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
        fields = ('keyword', 'layer', 'sort_key', 'description')

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
            return obj.default_value
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


# Not really catalogue viewsets - are they better put somewhere else (e.g. sql app?)
class SquidleAnnotationsDataSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.SquidleAnnotationsDataView
        exclude = ('id',)


# SQL Views

class AmpDepthZonesSerializer(serializers.ModelSerializer):
    class Meta:
        model = models.AmpDepthZones
        fields = '__all__'
