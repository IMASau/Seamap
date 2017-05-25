from django.contrib import admin
from .models import Category, DataClassification, Organisation, ServerType, Layer, LayerGroup

admin.site.register(Category)
admin.site.register(DataClassification)
admin.site.register(Organisation)
admin.site.register(ServerType)
admin.site.register(Layer)
admin.site.register(LayerGroup)
