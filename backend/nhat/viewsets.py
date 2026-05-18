import catalogue.models
from . import serializers

from django.core.cache import cache
from django.db.models import Value
from django.db.models.functions import Coalesce
from rest_framework import viewsets
from rest_framework.response import Response


class LayerViewset(viewsets.ReadOnlyModelViewSet):
    queryset = catalogue.models.Layer.objects.all() \
        .prefetch_related(
            'category',
            'data_classification',
            'organisation',
            'server_type'
        ) \
        .select_related('nhatlayer') \
        .annotate(sort_key_null=Coalesce('sort_key', Value('zzzzzzzz'))) \
        .order_by('sort_key_null', 'name')
    serializer_class = serializers.LayerSerializer

    def list(self, request, *args, **kwargs):
        """
        Override default list method to manually cache the response.
        By manually caching the response, we can give it an infinite timeout, so the
        cache may only be cleared by an explicit call to cache.clear()
        """
        cache_key = "nhatlayer_list"

        data = cache.get(cache_key)
        if data is not None:
            return Response(data)

        response = super().list(request, *args, **kwargs)

        cache.set(cache_key, response.data, timeout=None)
        return response
