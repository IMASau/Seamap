import configparser

import pyodbc

config = configparser.ConfigParser()
config.read('config.ini')


cnxn = pyodbc.connect(config['database']['connection_string'])
cnxn.autocommit = False
