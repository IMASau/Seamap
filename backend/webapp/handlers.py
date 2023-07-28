# Seamap: view and interact with Australian coastal habitat data
# Copyright (c) 2017, Institute of Marine & Antarctic Studies.  Written by Condense Pty Ltd.
# Released under the Affero General Public Licence (AGPL) v3.  See LICENSE file for details.
from django.db import DatabaseError
from rest_framework import status
from rest_framework.renderers import JSONRenderer
from rest_framework.response import Response
from rest_framework.serializers import ValidationError
from rest_framework.views import exception_handler

def custom_exception_handler(exc, context):
    # Call REST framework's default exception handler first,
    # to get the standard error response.
    response = exception_handler(exc, context)

    # Reset any custom renderer; rationale here is that the custom
    # renderer would be called regardless of whether an error occurred
    # or not, but if we've got here we probably aren't interested in
    # that any more:
    context['request'].accepted_renderer = JSONRenderer()

    # Now add the HTTP status code to the response.
    if isinstance(exc, ValidationError):
        response.data['status_code'] = response.status_code
    elif isinstance(exc, DatabaseError):
        return Response({'message': "Database error",
                         'detail': exc.message}, status=status.HTTP_500_INTERNAL_SERVER_ERROR)

    return response
