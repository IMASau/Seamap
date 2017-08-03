from django.contrib import admin
from .models import Category, DataClassification, Organisation, ServerType, Layer, LayerGroup, LayerGroupPriority, HabitatDescriptor

admin.site.register(Category)
admin.site.register(DataClassification)
admin.site.register(Organisation)
admin.site.register(ServerType)
admin.site.register(HabitatDescriptor)
admin.site.register(Layer)
admin.site.register(LayerGroup)
admin.site.register(LayerGroupPriority)
