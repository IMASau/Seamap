// Code taken from leaflet.ScaleFactor by Marc Chasse (https://github.com/MarcChasse/leaflet.ScaleFactor)

(function (factory) {
    var L;
    if (typeof define === 'function' && define.amd) {
        // AMD
        define(['leaflet'], factory);
    } else if (typeof module !== 'undefined') {
        // Node/CommonJS
        L = require('leaflet');
        module.exports = factory(L);
    } else {
        // Browser globals
        if (typeof window.L === 'undefined') {
            throw new Error('Leaflet must be loaded first');
        }
        factory(window.L);
    }
}(function (L) {
    'use strict';

    L.Control.ScaleFactor = (function () {
        var ScaleFactor = L.Control.extend({
            options: {
                position: 'bottomleft',
                updateWhenIdle: true
            },

            onAdd: function (map) {
                var className = 'leaflet-control-scalefactor',
                    container = L.DomUtil.create('div', className),
                    options = this.options;

                this._mScale = L.DomUtil.create('div', className + '-line', container);

                map.on(options.updateWhenIdle ? 'moveend' : 'move', this._update, this);
                map.whenReady(this._update, this);

                return container;
            },

            onRemove: function (map) {
                map.off(this.options.updateWhenIdle ? 'moveend' : 'move', this._update, this);
            },

            _pxTOmm: (function () {
                var heightRef = document.createElement('div');
                heightRef.style = 'height:1mm;';
                heightRef.id = 'heightRef';
                document.body.appendChild(heightRef);

                heightRef = document.getElementById('heightRef');
                console.log(heightRef)
                console.log(heightRef.parentNode)
                var pxPermm = heightRef.offsetHeight;
                console.log("pxPermm")
                console.log(pxPermm)

                //heightRef.parentNode.removeChild(heightRef);

                return function pxTOmm(px) {
                    return px / pxPermm;
                }
            })(),

            _update: function () {
                var map = this._map;

                var CenterOfMap = map.getSize().y / 2;

                var RealWorlMetersPer100Pixels = map.distance(
                    map.containerPointToLatLng([0, CenterOfMap]),
                    map.containerPointToLatLng([100, CenterOfMap])
                );

                var ScreenMetersPer100Pixels = this._pxTOmm(100) / 1000;

                var scaleFactor = RealWorlMetersPer100Pixels / ScreenMetersPer100Pixels;

                //.replace formats the scale with commas 50000 -> 50,000
                console.log(RealWorlMetersPer100Pixels)
                console.log(ScreenMetersPer100Pixels)
                this._mScale.innerHTML = '1:' + Math.round(scaleFactor).toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
            }

        });

        return ScaleFactor;
    })();

    L.Map.addInitHook(function () {
        if (this.options.scaleFactor) {
            this.scaleFactor = new L.Control.ScaleFactor();
            this.addControl(this.scaleFactor);
        }
    });

    L.control.scaleFactor = function (options) {
        return new L.Control.ScaleFactor(options);
    };
}));
