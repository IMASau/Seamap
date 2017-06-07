import L from 'leaflet';
import 'leaflet-draw';

import Proj from 'proj4leaflet';
import proj4 from 'proj4';

import React from 'react';
import ReactDOM from 'react-dom';
import CSSTransitionGroup from 'react-addons-css-transition-group';

import ContainerDimensions from 'react-container-dimensions';

import * as ReactLeaflet from 'react-leaflet';
import { EditControl } from 'react-leaflet-draw';

import Gallery from 'react-grid-gallery';

import * as Blueprint from '@blueprintjs/core';

window.L = L;
window.L.Proj = Proj;

window.proj4 = proj4;

React.addons = React.addons || {};
React.addons.CSSTransitionGroup = CSSTransitionGroup;

React.ContainerDimensions = ContainerDimensions;

window.React = React;
window.ReactDOM = ReactDOM;

ReactLeaflet.EditControl = EditControl;
window.ReactLeaflet = ReactLeaflet;

window.Gallery = Gallery;

window.Blueprint = Blueprint;
