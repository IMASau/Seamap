from catalogue import models, serializers
from django.shortcuts import get_object_or_404
from rest_framework.exceptions import ValidationError
from rest_framework.generics import ListCreateAPIView, RetrieveAPIView


class SaveStateView(ListCreateAPIView):
    serializer_class = serializers.SaveStateSerializer

    def get_queryset(self):
        id = self.request.query_params.get("id")
        return models.SaveState.objects.filter(id=id) if id else models.SaveState.objects.all()


# Not really catalogue views - are they better put somewhere else (e.g. sql app?)

class SquidleAnnotationsDataView(RetrieveAPIView):
    serializer_class = serializers.SquidleAnnotationsDataSerializer

    def get_object(self):
        # Validate required parameters
        required_params = ['network', 'highlights']
        missing_params = [param for param in required_params if param not in self.request.query_params]

        if missing_params:
            raise ValidationError({"detail": f"Missing required parameters: {', '.join(missing_params)}"})

        filters = {k: self.request.query_params[k] for k in self.request.query_params if k in ['network', 'park', 'depth_zone', 'highlights']}

        if 'park' not in filters:
            filters['park'] = None

        if 'depth_zone' not in filters:
            filters['depth_zone'] = None

        if filters['highlights'] in ['true', 'True']:
            filters['highlights'] = True
        elif filters['highlights'] in ['false', 'False']:
            filters['highlights'] = False
        else:
            raise ValidationError({"detail": f"Invalid value for highlights: {filters['highlights']}"})

        print(filters)
        return get_object_or_404(models.SquidleAnnotationsDataView, **filters)
