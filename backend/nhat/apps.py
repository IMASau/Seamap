from django.apps import AppConfig


class NhatConfig(AppConfig):
    name = 'nhat'

    def ready(self):
        import nhat.signals # pylint: disable=import-outside-toplevel, unused-import
