from django.apps import AppConfig


class NhatConfig(AppConfig):
    name = 'nhat'

    # This will ensure the nhat.urls module is included in the root
    # urls, namespaced under 'nhat' (see webapp.urls for the
    # dynamic-loading implementation):
    url_prefix = 'nhat'

    def ready(self):
        import nhat.signals # pylint: disable=import-outside-toplevel, unused-import
