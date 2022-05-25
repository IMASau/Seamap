from rest_framework.generics import ListCreateAPIView

from catalogue.models import SaveState
from catalogue.serializers import SaveStateSerializer


class SaveStateView(ListCreateAPIView):
	serializer_class = SaveStateSerializer

	def get_queryset(self):
		id = self.request.query_params.get("id")
		return SaveState.objects.filter(id=id) if id else SaveState.objects.all()
