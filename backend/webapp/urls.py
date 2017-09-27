from django.conf import settings
from django.conf.urls import include, url
from django.contrib import admin
from rest_framework.routers import DefaultRouter

from catalogue.viewsets import DescriptorViewset, LayerViewset, GroupViewset, GroupPriorityViewset, OrganisationViewset
from habitat.viewsets import HabitatViewSet


router = DefaultRouter()
router.register(r'layers', LayerViewset)
router.register(r'groups', GroupViewset)
router.register(r'organisations', OrganisationViewset)
router.register(r'priorities', GroupPriorityViewset)
router.register(r'habitat', HabitatViewSet, 'Habitat')
router.register(r'descriptors', DescriptorViewset)

urlpatterns = [
    url(r'^api/', include(router.urls)),
]

if settings.DEBUG:
    urlpatterns += [
        url(r'^admin/', admin.site.urls),
        url(r'^auth/', include('rest_framework.urls', namespace='rest_framework')),
    ]
