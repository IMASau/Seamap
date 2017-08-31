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

import * as ReactSidebar from 'react-leaflet-sidebarv2';

import * as Blueprint from '@blueprintjs/core';

window.L = L;
window.L.Proj = Proj;

window.proj4 = proj4;

React.addons = React.addons || {};
React.addons.CSSTransitionGroup = CSSTransitionGroup;

React.ContainerDimensions = ContainerDimensions;

window.React = React;
window.ReactDOM = ReactDOM;

window.ReactLeaflet = ReactLeaflet;
window.ReactLeaflet.EditControl = EditControl;

window.ReactSidebar = ReactSidebar;

window.Blueprint = Blueprint;
