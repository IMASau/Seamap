'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

function _interopDefault (ex) { return (ex && (typeof ex === 'object') && 'default' in ex) ? ex['default'] : ex; }

var L = _interopDefault(require('leaflet'));
var PropTypes = _interopDefault(require('prop-types'));
var reactLeaflet = require('react-leaflet');

function styleInject(css, ref) {
  if ( ref === void 0 ) ref = {};
  var insertAt = ref.insertAt;

  if (!css || typeof document === 'undefined') { return; }

  var head = document.head || document.getElementsByTagName('head')[0];
  var style = document.createElement('style');
  style.type = 'text/css';

  if (insertAt === 'top') {
    if (head.firstChild) {
      head.insertBefore(style, head.firstChild);
    } else {
      head.appendChild(style);
    }
  } else {
    head.appendChild(style);
  }

  if (style.styleSheet) {
    style.styleSheet.cssText = css;
  } else {
    style.appendChild(document.createTextNode(css));
  }
}

var css = ".leaflet-container.crosshair-cursor-enabled {cursor:crosshair;}";
styleInject(css);

var classCallCheck = function (instance, Constructor) {
  if (!(instance instanceof Constructor)) {
    throw new TypeError("Cannot call a class as a function");
  }
};

var createClass = function () {
  function defineProperties(target, props) {
    for (var i = 0; i < props.length; i++) {
      var descriptor = props[i];
      descriptor.enumerable = descriptor.enumerable || false;
      descriptor.configurable = true;
      if ("value" in descriptor) descriptor.writable = true;
      Object.defineProperty(target, descriptor.key, descriptor);
    }
  }

  return function (Constructor, protoProps, staticProps) {
    if (protoProps) defineProperties(Constructor.prototype, protoProps);
    if (staticProps) defineProperties(Constructor, staticProps);
    return Constructor;
  };
}();

var _extends = Object.assign || function (target) {
  for (var i = 1; i < arguments.length; i++) {
    var source = arguments[i];

    for (var key in source) {
      if (Object.prototype.hasOwnProperty.call(source, key)) {
        target[key] = source[key];
      }
    }
  }

  return target;
};

var inherits = function (subClass, superClass) {
  if (typeof superClass !== "function" && superClass !== null) {
    throw new TypeError("Super expression must either be null or a function, not " + typeof superClass);
  }

  subClass.prototype = Object.create(superClass && superClass.prototype, {
    constructor: {
      value: subClass,
      enumerable: false,
      writable: true,
      configurable: true
    }
  });
  if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass;
};

var possibleConstructorReturn = function (self, call) {
  if (!self) {
    throw new ReferenceError("this hasn't been initialised - super() hasn't been called");
  }

  return call && (typeof call === "object" || typeof call === "function") ? call : self;
};

var reactToCSS = require('react-style-object-to-css');

var coordinatesControlDefaultStyle = {
	width: '290px',
	margin: '0',
	border: '1px solid rgba(0,0,0,0.2)',
	borderRadius: '4px',
	backgroundColor: 'rgba(255, 255, 255, 0.7)',
	outline: 'none',
	fontSize: '11px',
	boxShadow: 'none',
	color: '#333',
	padding: '2px 2px',
	minHeight: '18px'
};

L.Control.CoordinateControl = L.Control.extend({
	_style: null,
	_coordinateButton: null,
	_coordinates: 'decimal',
	initialize: function initialize(element) {
		this.options.position = element.position;

		this._coordinates = element.coordinates || 'decimal';

		if (element.style === undefined) {
			this._style = reactToCSS(coordinatesControlDefaultStyle);
		} else {
			this._style = reactToCSS(element.style);
		}
	},
	onAdd: function onAdd(map) {
		var _this = this;

		var coordinateButton = L.DomUtil.create('button');
		coordinateButton.setAttribute('style', this._style);
		coordinateButton.setAttribute('id', 'coorindate-control');

		map.on('mousemove', function (e) {
			if (_this._coordinates === 'degrees') {
				coordinateButton.innerHTML = "<strong>Latitude: </strong>" + _this.convertDecimalLatToDegrees(e.latlng.lat) + " <strong>Longitude: </strong> " + _this.convertDecimalLngToDegrees(e.latlng.lng);
			} else {
				var lat = e.latlng.lat.toLocaleString('en-US', { minimumFractionDigits: 8, useGrouping: false });
				var lng = e.latlng.lng.toLocaleString('en-US', { minimumFractionDigits: 8, useGrouping: false });
				coordinateButton.innerHTML = "<strong>Latitude: </strong>" + lat + "&nbsp; <strong>Longitude: </strong>" + lng;
			}
		});

		this._coordinateButton = coordinateButton;
		return coordinateButton;
	},
	convertDecimalLatToDegrees: function convertDecimalLatToDegrees(lat) {
		var dms = this.convertDDToDMS(lat, false);
		var dmsDeg = dms.deg.toLocaleString('en-US', { minimumIntegerDigits: 2, useGrouping: false });
		var dmsMin = dms.min.toLocaleString('en-US', { minimumIntegerDigits: 2, useGrouping: false });
		var dmsSec = dms.sec.toLocaleString('en-US', { minimumIntegerDigits: 2, minimumFractionDigits: 2, useGrouping: false });
		var dmsString = dmsDeg + 'º ' + dmsMin + '′ ' + dmsSec + '′′ ' + dms.dir;
		return dmsString;
	},
	convertDecimalLngToDegrees: function convertDecimalLngToDegrees(lng) {
		var dms = this.convertDDToDMS(lng, true);
		var dmsDeg = dms.deg.toLocaleString('en-US', { minimumIntegerDigits: 2, useGrouping: false });
		var dmsMin = dms.min.toLocaleString('en-US', { minimumIntegerDigits: 2, useGrouping: false });
		var dmsSec = dms.sec.toLocaleString('en-US', { minimumIntegerDigits: 2, minimumFractionDigits: 2, useGrouping: false });
		var dmsString = dmsDeg + 'º ' + dmsMin + '′ ' + dmsSec + '′′ ' + dms.dir;
		return dmsString;
	},
	convertDDToDMS: function convertDDToDMS(D, lng) {
		return {
			dir: D < 0 ? lng ? 'W' : 'S' : lng ? 'E' : 'N',
			deg: 0 | (D < 0 ? D = -D : D),
			min: 0 | D % 1 * 60,
			sec: (0 | D * 60 % 1 * 6000) / 100
		};
	}
});

L.control.coordinateControl = function (opts) {
	return new L.Control.CoordinateControl(_extends({}, opts));
};

var CoordinatesControl = function (_MapControl) {
	inherits(CoordinatesControl, _MapControl);

	function CoordinatesControl(props) {
		classCallCheck(this, CoordinatesControl);
		return possibleConstructorReturn(this, (CoordinatesControl.__proto__ || Object.getPrototypeOf(CoordinatesControl)).call(this, props));
	}

	createClass(CoordinatesControl, [{
		key: 'createLeafletElement',
		value: function createLeafletElement(props) {
			this.control = L.control.coordinateControl(_extends({}, props));
			return this.control;
		}
	}]);
	return CoordinatesControl;
}(reactLeaflet.MapControl);

var leafletCoordinates = reactLeaflet.withLeaflet(CoordinatesControl);

CoordinatesControl.propTypes = {
	style: PropTypes.element,
	coordinates: PropTypes.oneOf(['decimal', 'degrees']),
	position: PropTypes.oneOf(['topright', 'topleft', 'bottomright', 'bottomleft'])
};

exports.CoordinatesControl = leafletCoordinates;
//# sourceMappingURL=index.js.map
