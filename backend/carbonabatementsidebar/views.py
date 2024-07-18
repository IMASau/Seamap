from django.conf import settings
from django.shortcuts import render
from django.views.decorators.cache import never_cache
from django.views.decorators.clickjacking import xframe_options_exempt
import json

@never_cache
@xframe_options_exempt
def carbon_abatement_sidebar(request):
    context = {
        'STATIC_URL': settings.STATIC_URL
    }
    if not settings.DEBUG:
        context.update({'DEBUG': True})
        return render(request, 'index.html', context)
    else:
        with open(f'{settings.BASE_DIR}/carbonabatementsidebar/frontend/dist/carbonabatementsidebar/.vite/manifest.json') as f:
            manifest = json.load(f)
        context.update({
            'main_js': f'carbonabatementsidebar/{manifest["src/main.tsx"]["file"]}',
            'main_css': f'carbonabatementsidebar/{manifest["src/main.tsx"]["css"][0]}',
            'DEBUG': False
        })
        return render(request, 'index.html', context)
