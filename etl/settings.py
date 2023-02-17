import configparser
import logging.handlers
import os

import pyodbc

config = configparser.ConfigParser()
dir_path = os.path.dirname(os.path.realpath(__file__))
config.read(os.path.join(dir_path, 'config.ini'))

# set up logging
log_handler = logging.handlers.WatchedFileHandler(os.path.join(dir_path, 'etl.log'))
formatter = logging.Formatter(
    '%(asctime)s program_name [%(process)d]: %(message)s',
    '%b %d %H:%M:%S')
log_handler.setFormatter(formatter)
logger = logging.getLogger()
logger.addHandler(log_handler)
logger.setLevel(logging.INFO)


