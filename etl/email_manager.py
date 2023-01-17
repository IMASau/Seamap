import logging
import os
import smtplib
from email.message import EmailMessage

from settings import config


def send_error_email(source_name, error):
    try:
        s = smtplib.SMTP(config['email']['smtp_server'])
        msg = EmailMessage()
        hostname = os.environ.get('HOSTNAME', 'UNKNOWN_HOST')
        short_hostname = hostname.split('.')[0]
        if source_name is None:
            msg.set_content(f"There was an error in Seamap ETL:\n {error}\n\nServer: {hostname}")
        else:
            msg.set_content(f"There was an error when handling {source_name}:\n {error}\n\nEndpoint: {config[source_name]['url']}\n\nServer: {hostname}")
        msg['Subject'] = f"[{short_hostname}] Error in Seamap ETL"
        msg['To'] = config['email']['recipients']
        msg['From'] = config['email']['sender']
        s.send_message(msg)
        s.quit()
    except Exception:
        logging.exception("Error sending email")
