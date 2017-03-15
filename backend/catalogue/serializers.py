from rest_framework import serializers
from catalogue.models import Layer


class LayerSerializer(serializers.ModelSerializer):
    category = serializers.SerializerMethodField()
    server_type = serializers.SerializerMethodField()

    def get_category(self, obj):
        return obj.category.name

    def get_server_type(self, obj):
        return obj.server_type.name

    class Meta:
        model = Layer
        fields = '__all__'
