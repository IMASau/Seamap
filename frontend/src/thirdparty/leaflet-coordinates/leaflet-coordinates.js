'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

function _interopDefault (ex) { return (ex && (typeof ex === 'object') && 'default' in ex) ? ex['default'] : ex; }

var L = _interopDefault(require('leaflet'));
var PropTypes = _interopDefault(require('prop-types'));
var reactLeaflet = require('react-leaflet');

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

function posmod(val, mod) {
	return (val % mod + mod) % mod;
}

L.Control.CoordinateControl = L.Control.extend({
	_style: null,
	_coordinatesContainer: null,
	_coordinates: 'decimal',
	initialize: function initialize(element) {
		this.options.position = element.position;

		this._coordinates = element.coordinates || 'decimal';
	},
	onAdd: function onAdd(map) {
		var _this = this;

		var coordinatesContainer = L.DomUtil.create('div');
		coordinatesContainer.setAttribute('style', this._style);
		coordinatesContainer.setAttribute('class', 'coordinate-control');

		if (_this._coordinates === 'degrees') {
			coordinatesContainer.innerHTML = _this.convertDecimalLatToDegrees(lat) + _this.convertDecimalLngToDegrees(lng);
		} else {
			coordinatesContainer.innerHTML = `<div class="coordinate-control-latitude">${(0).toFixed(2)}</div><div class="coordinate-control-longitude">${(0).toFixed(2)}</div>`;
		}

		map.on('mousemove', function (e) {
			const lat = e.latlng.lat ?? 0;
			const lng = posmod(((e.latlng.lng ?? 0) + 180), 360) - 180;

			if (_this._coordinates === 'degrees') {
				coordinatesContainer.innerHTML = _this.convertDecimalLatToDegrees(lat) + _this.convertDecimalLngToDegrees(lng);
			} else {
				coordinatesContainer.innerHTML = `<div class="coordinate-control-latitude">${lat.toFixed(2)}</div><div class="coordinate-control-longitude">${lng.toFixed(2)}</div>`;
			}
		});

		this._coordinatesContainer = coordinatesContainer;
		return coordinatesContainer;
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
