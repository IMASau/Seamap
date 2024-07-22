from django.conf import settings
from django.shortcuts import render
from django.views.decorators.cache import never_cache
from django.views.decorators.clickjacking import xframe_options_exempt
from csp.decorators import csp_update
import json
import os

@never_cache
@xframe_options_exempt
@csp_update(FRAME_ANCESTORS=settings.CORS_ORIGIN_WHITELIST)
def carbon_abatement_sidebar(request):
    context = {
        'STATIC_URL': settings.STATIC_URL
    }
    if settings.DEBUG:
        context.update({'DEBUG': True})
        return render(request, 'index.html', context)
    else:
        with open(os.path.join(settings.STATIC_ROOT, 'carbonabatementsidebar/vite/manifest.json')) as f:
            manifest = json.load(f)
            main_js = os.path.join('carbonabatementsidebar', manifest['src/main.tsx']['file'])
            main_css = os.path.join('carbonabatementsidebar', manifest['src/main.tsx']['css'][0])
        context.update({
            'main_js': main_js,
            'main_css': main_css,
            'DEBUG': False
        })
        return render(request, 'index.html', context)
