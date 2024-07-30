from django.conf import settings
from django.shortcuts import render
from django.views.decorators.cache import never_cache
from django.views.decorators.clickjacking import xframe_options_exempt
from csp.decorators import csp_update
import json
import os


carbon_prices = ['cp35', 'cp50', 'cp65', 'cp80', 'cpmax']
abatement_types = ['CarbonAbatement', 'AbatementArea']

def layer_to_carbon_price(layer: str) -> str:
    for carbon_price in carbon_prices:
        if carbon_price in layer:
            return carbon_price
    if 'max' in layer:
        return 'cpmax'
    raise ValueError(f"Could not find carbon price in layer name: {layer}")

def layer_to_abatement_type(layer: str) -> str:
    for abatement_type in abatement_types:
        if abatement_type in layer:
            return abatement_type
    raise ValueError(f"Could not find abatement type in layer name: {layer}")

@never_cache
@xframe_options_exempt
@csp_update(FRAME_ANCESTORS=settings.CORS_ORIGIN_WHITELIST, SCRIPT_SRC=("'unsafe-eval'", "'unsafe-inline'"))
def carbon_abatement_sidebar(request):
    url_root = settings.URL_ROOT if hasattr(settings, 'URL_ROOT') else ''
    layers = json.loads(request.GET['layers']) if 'layers' in request.GET else []
    
    context = {
        'STATIC_URL': settings.STATIC_URL,
        'api_url': os.path.join(url_root, 'api'),
        'abatement_types': json.dumps([layer_to_abatement_type(layer) for layer in layers]),
        'carbon_prices': json.dumps([layer_to_carbon_price(layer) for layer in layers]),
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
