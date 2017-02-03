from rest_framework import serializers
from habitat.models import Transect

class TransectSerializer(serializers.ModelSerializer):
    class Meta:
        model = Transect
        exclude = ['id']