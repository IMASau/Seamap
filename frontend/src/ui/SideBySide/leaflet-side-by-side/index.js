import L from 'leaflet';

let mapWasDragEnabled;
let mapWasTapEnabled;

function getRangeEvent (rangeInput) {
  return 'oninput' in rangeInput ? 'input' : 'change'
}

function cancelMapDrag () {
  mapWasDragEnabled = this._map.dragging.enabled()
  mapWasTapEnabled = this._map.tap && this._map.tap.enabled()
  this._map.dragging.disable()
  this._map.tap && this._map.tap.disable()
}

function uncancelMapDrag (e) {
  this._refocusOnMap(e)
  if (mapWasDragEnabled) {
    this._map.dragging.enable()
  }
  if (mapWasTapEnabled) {
    this._map.tap.enable()
  }
}

const SideBySide = L.Control.extend({
  options: {
    thumbSize: 42,
    padding: 0,
    onRangeUpdated: null,
    rangeValue: 0.5, // Where the range appears when first created
  },

  initialize: function (leftPane, rightPane, options) {
    this.setLeftPane(leftPane)
    this.setRightPane(rightPane)
    L.setOptions(this, options)
  },

  getPosition: function () {
    var rangeValue = this._range.value
    var offset = (0.5 - rangeValue) * (2 * this.options.padding + this.options.thumbSize)
    return this._map.getSize().x * rangeValue + offset
  },

  setPosition: null,

  includes: L.Evented.prototype || L.Mixin.Events,

  addTo: function (map) {
    this.remove()
    this._map = map

    var container = this._container = L.DomUtil.create('div', 'leaflet-sbs', map._controlContainer)
    L.DomEvent.disableClickPropagation(container);

    this._divider = L.DomUtil.create('div', 'leaflet-sbs-divider', container)
    this._leftLabel = L.DomUtil.create('div', 'leaflet-sbs-left-label', container);
    this._rightLabel = L.DomUtil.create('div', 'leaflet-sbs-right-label', container);
    this.setLeftLabelText(this.options.leftLabelText);
    this.setRightLabelText(this.options.rightLabelText);
    var range = this._range = L.DomUtil.create('input', 'leaflet-sbs-range', container)
    range.type = 'range'
    range.min = 0
    range.max = 1
    range.step = 'any'
    range.value = this.options.rangeValue;
    range.style.paddingLeft = range.style.paddingRight = this.options.padding + 'px'
    this._addEvents()
    this._updateClip();
    return this
  },

  remove: function () {
    if (!this._map) {
      return this
    }
    if (this._leftPane) {
      this._leftPane.style.clip = '';
    }
    if (this._rightPane) {
      this._rightPane.style.clip = '';
    }
    this._removeEvents()
    L.DomUtil.remove(this._container)

    this._map = null

    return this
  },

  setLeftPane: function (leftPane) {
    this._leftPane = leftPane;
    this._updateClip();
    return this
  },

  setRightPane: function (rightPane) {
    this._rightPane = rightPane;
    this._updateClip();
    return this
  },

  setLeftLabelText: function (leftLabelText) {
    this.options.leftLabelText = leftLabelText;
    this._leftLabel.innerText = leftLabelText ?? '';
    this._leftLabel.title = leftLabelText ?? '';
    this._leftLabel.style.display = leftLabelText ? 'block' : 'none';
  },

  setRightLabelText: function (rightLabelText) {
    this.options.rightLabelText = rightLabelText;
    this._rightLabel.innerText = rightLabelText ?? '';
    this._rightLabel.title = rightLabelText ?? '';
    this._rightLabel.style.display = rightLabelText ? 'block' : 'none';
  },

  _updateClip: function () {
    var map = this._map
    if (!map) return;
    var nw = map.containerPointToLayerPoint([0, 0])
    var se = map.containerPointToLayerPoint(map.getSize())
    var clipX = nw.x + this.getPosition()
    var dividerX = this.getPosition()

    this._divider.style.left = dividerX + 'px'
    this._leftLabel.style.left = dividerX + 'px';
    this._rightLabel.style.left = dividerX + 'px';
    this.fire('dividermove', {x: dividerX})
    var clipLeft = 'rect(' + [nw.y, clipX, se.y, nw.x].join('px,') + 'px)'
    var clipRight = 'rect(' + [nw.y, se.x, se.y, clipX].join('px,') + 'px)'
    if (this._leftPane) {
      this._leftPane.style.clip = clipLeft
    }
    if (this._rightPane) {
      this._rightPane.style.clip = clipRight
    }
  },

  _dragStart: function () {
    cancelMapDrag.call(this);
  },

  _dragEnd: function () {
    uncancelMapDrag.call(this);
    this.onRangeUpdated()
  },

  onRangeUpdated: function () {
    if (this.options.onRangeUpdated) {
      this.options.onRangeUpdated(this._range.value, this.getPosition());
    }
  },

  _addEvents: function () {
    var range = this._range;
    var map = this._map;
    if (!map || !range) return;
    map.on("move", this._updateClip, this);
    map.on("moveend", this.onRangeUpdated, this); // The range's position (container xy coordinate) also changes if the map is moved
    L.DomEvent.on(range, getRangeEvent(range), this._updateClip, this);
    L.DomEvent.on(range, "touchstart", this._dragStart, this);
    L.DomEvent.on(range, "touchend", this._dragEnd, this);
    L.DomEvent.on(range, "mousedown", this._dragStart, this);
    L.DomEvent.on(range, "mouseup", this._dragEnd, this);
  },

  _removeEvents: function () {
    var range = this._range;
    var map = this._map;
    if (range) {
      L.DomEvent.off(range, getRangeEvent(range), this._updateClip, this);
      L.DomEvent.off(range, "touchstart", this._dragStart, this);
      L.DomEvent.off(range, "touchend", this._dragEnd, this);
      L.DomEvent.off(range, "mousedown", this._dragStart, this);
      L.DomEvent.off(range, "mouseup", this._dragEnd, this);
    }
    if (map) {
      map.off("move", this._updateClip, this);
    }
  },
})

export const sideBySide = function (leftPane, rightPane, options) {
  return new SideBySide(leftPane, rightPane, options)
}
