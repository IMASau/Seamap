# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from . import models

from rest_framework import serializers


class AbatementSerializer(serializers.ModelSerializer):
    class Meta:
        exclude = ('id', 'cp35', 'cp50', 'cp65', 'cp80', 'cpmax', 'dr', 'ec', 'ac',)


class CarbonAbatementSerializer(serializers.ModelSerializer):
    carbon_abatement = serializers.SerializerMethodField()

    def get_carbon_abatement(self, obj):
        carbon_price = self.context.get('carbon_price')
        assert carbon_price is not None
        if carbon_price == 'cp35':
            return obj.cp35 / 1000000
        elif carbon_price == 'cp50':
            return obj.cp50 / 1000000
        elif carbon_price == 'cp65':
            return obj.cp65 / 1000000
        elif carbon_price == 'cp80':
            return obj.cp80 / 1000000
        elif carbon_price == 'cpmax':
            return obj.cpmax / 1000000
        raise serializers.ValidationError(f"'{carbon_price}' is not a valid value for 'carbon_price'")

    class Meta(AbatementSerializer.Meta):
        pass


class CarbonAbatementByStateSerializer(CarbonAbatementSerializer):
    class Meta(CarbonAbatementSerializer.Meta):
        model = models.CarbonAbatementByState


class CarbonAbatementBySa2Serializer(CarbonAbatementSerializer):
    class Meta(CarbonAbatementSerializer.Meta):
        model = models.CarbonAbatementBySa2


class CarbonAbatementByPrimaryCompartmentSerializer(CarbonAbatementSerializer):
    class Meta(CarbonAbatementSerializer.Meta):
        model = models.CarbonAbatementByPrimaryCompartment


class AbatementAreaSerializer(AbatementSerializer):
    abatement_area = serializers.SerializerMethodField()

    def get_abatement_area(self, obj):
        carbon_price = self.context.get('carbon_price')
        assert carbon_price is not None
        if carbon_price == 'cp35':
            return obj.cp35
        elif carbon_price == 'cp50':
            return obj.cp50
        elif carbon_price == 'cp65':
            return obj.cp65
        elif carbon_price == 'cp80':
            return obj.cp80
        elif carbon_price == 'cpmax':
            return obj.cpmax
        raise serializers.ValidationError(f"'{carbon_price}' is not a valid value for 'carbon_price'")

    class Meta(AbatementSerializer.Meta):
        pass


class AbatementAreaByStateSerializer(AbatementAreaSerializer):
    class Meta(AbatementAreaSerializer.Meta):
        model = models.AbatementAreaByState


class AbatementAreaBySa2Serializer(AbatementAreaSerializer):
    class Meta(AbatementAreaSerializer.Meta):
        model = models.AbatementAreaBySa2


class AbatementAreaByPrimaryCompartmentSerializer(AbatementAreaSerializer):
    class Meta(AbatementAreaSerializer.Meta):
        model = models.AbatementAreaByPrimaryCompartment
