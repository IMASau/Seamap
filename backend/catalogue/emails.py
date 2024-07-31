from collections import namedtuple
from django.conf import settings
from django.core.mail import send_mail
from django.template.loader import render_to_string

def email_generate_layer_preview_summary(errors):
    """
    Email staff about failed layers during the generate_layer_previews run.
    """
    context = {'errors': errors}
    send_mail(
        subject="Layer Preview Generation Summary",
        message=render_to_string('email_generate_layer_preview_summary.txt', context),
        from_email=settings.DEFAULT_FROM_EMAIL,
        recipient_list=settings.ADMINS,
        fail_silently=False,
        html_message=render_to_string('email_generate_layer_preview_summary.html', context)
    )

# Union of a layer's name, ID, and error from the build_feature_index run.
LayerFeatureIndexError = namedtuple('LayerFeatureIndexError', 'layer_id error name')

def email_build_feature_index_summary(errors: list[LayerFeatureIndexError]) -> None:
    """
    Email staff about failed layers during the build_feature_index run.

    Args:
        errors (list[LayerFeatureIndexError]): List of errors to report.
    """
    context = {'errors': errors}
    send_mail(
        subject="Layer Preview Generation Summary",
        message=render_to_string('email_generate_build_feature_index_summary.txt', context),
        from_email=settings.DEFAULT_FROM_EMAIL,
        recipient_list=settings.ADMINS,
        fail_silently=False,
        html_message=render_to_string('email_generate_build_feature_index_summary.html', context)
    )
