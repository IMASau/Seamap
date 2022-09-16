import logging

from extractor import extract
from transformer import transform
from email_manager import send_error_email

sources = ['squidle', 'mars', 'global_archive']

for source in sources:
    try:
        logging.info(f"Extracting {source}")
        extract(source)
        logging.info(f"Transforming {source}")
        transform(source)
    except Exception as e:
        logging.error(f"An error happened when processing {source}! Sending email notification")
        # send email and continue to next source
        send_error_email(source, str(e))

