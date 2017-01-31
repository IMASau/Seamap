from django.contrib import admin
from .models import Category, ServerType, Layer

admin.site.register(Category)
admin.site.register(ServerType)
admin.site.register(Layer)