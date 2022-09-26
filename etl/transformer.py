
from settings import config, cnxn


def transform(source_name):
    #TODO extract to be a config instead?
    proc_name = config[source_name]['table'].replace('EXTRACT', 'IMPORT')

    cursor = cnxn.cursor()
    cursor.execute(f"{{call {proc_name}()}}")
    row = cursor.fetchone()
    if row[0] != 0:
        raise ValueError(row[1:])
