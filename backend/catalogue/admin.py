from django.contrib import admin
from .models import Category, DataClassification, ServerType, Layer

admin.site.register(Category)
admin.site.register(DataClassification)
admin.site.register(ServerType)
admin.site.register(Layer)
