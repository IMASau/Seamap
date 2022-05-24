from django.http import JsonResponse
from .models import SerializedHashstate
from rest_framework.decorators import api_view
from uuid import uuid4

def get_exception_message(e):
    return e.args[0] if len(e.args) == 1 else ''

@api_view(['POST'])
def create_save_state(request):

	try:
		id = str(uuid4())
		SerializedHashstate.objects.create(
			id=id,
			hashstate=request.data["hashstate"]
		)
		return JsonResponse({"id": id})
	except Exception as e:
		print(e)
		return JsonResponse({"message": get_exception_message(e), "args": e.args}, status=400)
