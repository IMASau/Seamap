from django.conf import settings
from django.conf.urls import include, url
from django.contrib import admin
from rest_framework.routers import DefaultRouter

from catalogue.viewsets import LayerViewset
from habitat.viewsets import HabitatViewSet


router = DefaultRouter()
router.register(r'layers', LayerViewset)
router.register(r'habitat', HabitatViewSet, 'Habitat')

urlpatterns = [
    url(r'^', include(router.urls)),
]

if settings.DEBUG:
    urlpatterns += [
        url(r'^admin/', admin.site.urls),
        url(r'^auth/', include('rest_framework.urls', namespace='rest_framework')),
    ]
