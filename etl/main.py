import logging

import pyodbc

from settings import config
from extractor import extract
from transformer import transform
from email_manager import send_error_email


def main():
    cnxn = None
    try:
        # open a connection to the database
        cnxn = pyodbc.connect(config['database']['connection_string'])
        cnxn.autocommit = False
        sources = ['squidle', 'mars', 'global_archive']
        for source in sources:
            try:
                logging.info(f"Extracting {source}")
                extract(cnxn, source)
                logging.info(f"Transforming {source}")
                transform(cnxn, source)
            except Exception as e:
                logging.error(f"An error happened when processing {source}! Sending email notification")
                # send email and continue to next source
                send_error_email(source, str(e))
    except Exception as e:
        logging.error("An error occurred")
        send_error_email(source_name=None, error=str(e))
    finally:
        if cnxn is not None:
            cnxn.close()


if __name__ == '__main__':
    main()
