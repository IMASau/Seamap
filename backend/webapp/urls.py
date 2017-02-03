from django.conf.urls import include, url
from django.contrib import admin
from catalogue.viewsets import LayerViewset
from habitat.viewsets import HabitatViewSet
from rest_framework.routers import DefaultRouter

router = DefaultRouter()
router.register(r'layers', LayerViewset)
router.register(r'habitat', HabitatViewSet, 'Habitat')

urlpatterns = [
    url(r'^admin/', admin.site.urls),
    url(r'^', include(router.urls)),
    url(r'^api-auth.', include('rest_framework.urls', namespace='rest_framework'))
]