from rest_framework import serializers
from catalogue.models import Layer



class LayerSerializer(serializers.ModelSerializer):
	class Meta:
		model = Layer
		fields = ('name', 'url', 'category', 'bounding_box', 
				'metadata_url', 'description', 'zoom_info', 'server_type', 
				'legend_url', 'date_start', 'date_end')
