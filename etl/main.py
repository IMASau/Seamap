from extractor import extract
from transformer import transform
from email_manager import send_error_email

sources = ['squidle', 'mars', 'global_archive']

for source in sources:
    try:
        extract(source)
        transform(source)
    except Exception as e:
        # send email and continue to next source
        send_error_email(source, str(e))

