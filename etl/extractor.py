import csv
import re
import urllib.request

import pyodbc
from pyproj import Transformer

from settings import config, cnxn

transformer = Transformer.from_crs("epsg:4326", "epsg:3112", always_xy=True)


def convert_geometry(src):
    coords = re.findall(r'-?\d+(?:\.-?\d*)?', src)
    x_out, y_out = transformer.transform(float(coords[0]), float(coords[1]))
    return f"POINT({x_out} {y_out})"


def extract(source_name):
    cursor = cnxn.cursor()
    try:
        resp = urllib.request.urlopen(config[source_name]['url'])
        resp = [line.decode('utf-8') for line in resp.readlines()]
        csv_reader = csv.reader(resp)

        cursor.execute(f'TRUNCATE TABLE {config[source_name]["table"]}')
        columns = next(csv_reader)
        # convert column_name => [column_name] for column_name that can be keyword
        columns = list(map(lambda c: f'[{c.lower()}]', columns))
        # Add a new column for converted geom data
        columns.append('[geom]')
        query = 'insert into {}({}) values ({})'
        query = query.format(config[source_name]['table'], ','.join(columns), ','.join('?' * len(columns)))
        geom_column_index = columns.index(f"[{config[source_name]['geom_column_name']}]")

        # set input sizes to improve insert speed, also preventing an exception when inserting geometry column
        input_sizes = [(pyodbc.SQL_WVARCHAR, 1000, 0)] * (len(columns) - 1)
        # special size for geo data column
        input_sizes.append((pyodbc.SQL_WVARCHAR, 0, 0))
        cursor.setinputsizes(input_sizes)

        for idx, row in enumerate(csv_reader):
            geom_data = row[geom_column_index]
            if len(geom_data) == 0:
                geom_data = None
            else:
                geom_data = convert_geometry(geom_data)
            row.append(geom_data)
            cursor.execute(query, row)
    except Exception as e:
        cursor.rollback()
        raise
    cursor.commit()
