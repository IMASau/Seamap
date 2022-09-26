import configparser
import logging
import logging.handlers

import pyodbc

config = configparser.ConfigParser()
config.read('config.ini')

# set up logging
log_handler = logging.handlers.WatchedFileHandler('etl.log')
formatter = logging.Formatter(
    '%(asctime)s program_name [%(process)d]: %(message)s',
    '%b %d %H:%M:%S')
log_handler.setFormatter(formatter)
logger = logging.getLogger()
logger.addHandler(log_handler)
logger.setLevel(logging.INFO)

# open a connection to the database
cnxn = pyodbc.connect(config['database']['connection_string'])
cnxn.autocommit = False
