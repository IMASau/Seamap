import logging
import smtplib
from email.message import EmailMessage

from settings import config


def send_error_email(source_name, error):
    try:
        s = smtplib.SMTP(config['email']['smtp_server'])
        msg = EmailMessage()
        msg.set_content(f"There was an error when handling {source_name}:\n {error}\n\nEndpoint: {config[source_name]['url']}")
        msg['Subject'] = 'Error in Seamap ETL'
        msg['To'] = config['email']['recipients']
        msg['From'] = config['email']['sender']
        s.send_message(msg)
        s.quit()
    except Exception:
        logging.exception("Error sending email")


def send_generic_error_email(error):
    try:
        s = smtplib.SMTP(config['email']['smtp_server'])
        msg = EmailMessage()
        msg.set_content(f"There was an error in Seamap ETL:\n {error}")
        msg['Subject'] = 'Error in Seamap ETL'
        msg['To'] = config['email']['recipients']
        msg['From'] = config['email']['sender']
        s.send_message(msg)
        s.quit()
    except Exception:
        logging.exception("Error sending email")
