
from settings import config


def transform(cnxn, source_name):
    #TODO extract to be a config instead?
    proc_name = config[source_name]['table'].replace('EXTRACT', 'IMPORT')

    cursor = cnxn.cursor()
    try:
        cursor.execute(f"{{call {proc_name}()}}")
        row = cursor.fetchone()
        print(row)
        if row[0] != 0:
            raise ValueError(row[1:])
    finally:
        cursor.commit()
