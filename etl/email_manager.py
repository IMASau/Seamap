import smtplib
from email.message import EmailMessage

from settings import config


def send_error_email(source_name, error):
    s = smtplib.SMTP(config['email']['smtp_server'])
    msg = EmailMessage()
    msg.set_content(f"There was an error when handling {source_name}:\n {error}")
    msg['Subject'] = 'Error in Seapmap ETL'
    msg['To'] = config['email']['recipients']
    msg['From'] = config['email']['sender']
    s.send_message(msg)
    s.quit()
