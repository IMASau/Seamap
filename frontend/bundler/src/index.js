import L from 'leaflet';
import 'leaflet-draw';

import React from 'react';
import ReactDOM from 'react-dom';
import CSSTransitionGroup from 'react-addons-css-transition-group';

import * as ReactLeaflet from 'react-leaflet';
import { EditControl } from 'react-leaflet-draw';

window.L = L;

React.addons = React.addons || {};
React.addons.CSSTransitionGroup = CSSTransitionGroup;

window.React = React;
window.ReactDOM = ReactDOM;

ReactLeaflet.EditControl = EditControl;
window.ReactLeaflet = ReactLeaflet;
