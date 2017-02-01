import L from 'leaflet';
import 'leaflet-draw';

import React from 'react';
import ReactDOM from 'react-dom';

import * as ReactLeaflet from 'react-leaflet';
import { EditControl } from 'react-leaflet-draw';

window.L = L;

window.React = React;
window.ReactDOM = ReactDOM;

ReactLeaflet.EditControl = EditControl;
window.ReactLeaflet = ReactLeaflet;
