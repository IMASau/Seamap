# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from . import models, serializers

import json
from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework.serializers import ValidationError


@api_view()
def carbon_abatement(request):
    for required in ['region-type', 'carbon-price', 'dr', 'ec', 'ac']:
        if required not in request.query_params:
            raise ValidationError({"message": "Required parameter '{}' is missing".format(required)})
    params = {k: v or None for k, v in request.query_params.items()}
    if params['region-type'] not in ['STE_NAME11', 'sa2int', 'ID_Primary']:
        raise ValidationError({"message": f"'{params['region-type']}' is not a valid value for 'region-type'"})
    if params['carbon-price'] not in ['cp35', 'cp50', 'cp65', 'cp80', 'cpmax']:
        raise ValidationError({"message": f"'{params['carbon-price']}' is not a valid value for 'carbon-price'"})
    if 'regions' in params:
        try:
            params['regions'] = json.loads(params['regions'])
            assert isinstance(params['regions'], list)
        except (json.JSONDecodeError, AssertionError):
            raise ValidationError({"message": f"'{params['regions']}' is not a valid value for 'regions'"})

    abatement_models = {
        'STE_NAME11': models.CarbonAbatementByState,
        'sa2int': models.CarbonAbatementBySa2,
        'ID_Primary': models.CarbonAbatementByPrimaryCompartment
    }
    model = abatement_models[params['region-type']]

    abatement_serializers = {
        'STE_NAME11': serializers.CarbonAbatementByStateSerializer,
        'sa2int': serializers.CarbonAbatementBySa2Serializer,
        'ID_Primary': serializers.CarbonAbatementByPrimaryCompartmentSerializer
    }
    serializer = abatement_serializers[params['region-type']]

    data = model.objects \
        .filter(dr=params['dr'], ec=params['ec'], ac=params['ac']) \
        .order_by('region')
    data = data
    if 'regions' in params:
        data = data.filter(region__in=params['regions'])

    return Response(serializer(data, many=True, context={'carbon_price': params['carbon-price']}).data)

@api_view()
def abatement_area(request):
    for required in ['region-type', 'carbon-price']:
        if required not in request.query_params:
            raise ValidationError({"message": "Required parameter '{}' is missing".format(required)})
    params = {k: v or None for k, v in request.query_params.items()}
    if params['region-type'] not in ['STE_NAME11', 'sa2int', 'ID_Primary']:
        raise ValidationError({"message": f"'{params['region-type']}' is not a valid value for 'region-type'"})
    if params['carbon-price'] not in ['cp35', 'cp50', 'cp65', 'cp80', 'cpmax']:
        raise ValidationError({"message": f"'{params['carbon-price']}' is not a valid value for 'carbon-price'"})
    if 'regions' in params:
        try:
            params['regions'] = json.loads(params['regions'])
            assert isinstance(params['regions'], list)
        except (json.JSONDecodeError, AssertionError):
            raise ValidationError({"message": f"'{params['regions']}' is not a valid value for 'regions'"})

    abatement_models = {
        'STE_NAME11': models.AbatementAreaByState,
        'sa2int': models.AbatementAreaBySa2,
        'ID_Primary': models.AbatementAreaByPrimaryCompartment
    }
    model = abatement_models[params['region-type']]

    abatement_serializers = {
        'STE_NAME11': serializers.AbatementAreaByStateSerializer,
        'sa2int': serializers.AbatementAreaBySa2Serializer,
        'ID_Primary': serializers.AbatementAreaByPrimaryCompartmentSerializer
    }
    serializer = abatement_serializers[params['region-type']]

    data = model.objects.all().order_by('region')
    min_dr = data.order_by('dr').first().dr
    min_ec = data.order_by('ec').first().ec
    min_ac = data.order_by('ac').first().ac
    data = data.filter(dr=min_dr, ec=min_ec, ac=min_ac)
    if 'regions' in params:
        data = data.filter(region__in=params['regions'])

    return Response(serializer(data, many=True, context={'carbon_price': params['carbon-price']}).data)
