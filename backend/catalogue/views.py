from rest_framework.generics import ListCreateAPIView, ListAPIView

from catalogue import models, serializers


class SaveStateView(ListCreateAPIView):
    serializer_class = serializers.SaveStateSerializer

    def get_queryset(self):
        id = self.request.query_params.get("id")
        return models.SaveState.objects.filter(id=id) if id else models.SaveState.objects.all()


# Not really catalogue views - are they better put somewhere else (e.g. sql app?)

class SquidleAnnotationsDataView(ListAPIView):
    serializer_class = serializers.SquidleAnnotationsDataSerializer

    def get_queryset(self):
        filters = {k: self.request.query_params[k] for k in self.request.query_params if k in ['network', 'park', 'depth_zone', 'highlights']}
        if 'highlights' in filters:
            if filters['highlights'] in ['true', 'True']:
                filters['highlights'] = True
            elif filters['highlights'] in ['false', 'False']:
                filters['highlights'] = False
            else:
                raise ValueError("Invalid value for highlights: %s" % filters['highlights'])

        if 'depth_zone' in filters and not filters['depth_zone']:
            filters['depth_zone'] = None

        return models.SquidleAnnotationsData.objects.filter(**filters)
