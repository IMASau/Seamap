import { arcgisToGeoJSON } from '@terraformer/arcgis';


// Util.js
// We can't import {EsriUtil} from 'esri-leaflet' here because that
// tries to access the window object which doesn't exist in a worker
// context:
var knownFieldNames = /^(OBJECTID|FID|OID|ID)$/i;

// Attempts to find the ID Field from response
function _findIdAttributeFromResponse (response) {
  var result;

  if (response.objectIdFieldName) {
    // Find Id Field directly
    result = response.objectIdFieldName;
  } else if (response.fields) {
    // Find ID Field based on field type
    for (var j = 0; j <= response.fields.length - 1; j++) {
      if (response.fields[j].type === 'esriFieldTypeOID') {
        result = response.fields[j].name;
        break;
      }
    }
    if (!result) {
      // If no field was marked as being the esriFieldTypeOID try well known field names
      for (j = 0; j <= response.fields.length - 1; j++) {
        if (response.fields[j].name.match(knownFieldNames)) {
          result = response.fields[j].name;
          break;
        }
      }
    }
  }
  return result;
}

// This is the 'last' resort, find the Id field from the specified feature
function _findIdAttributeFromFeature (feature) {
  for (var key in feature.attributes) {
    if (key.match(knownFieldNames)) {
      return key;
    }
  }
}

export function responseToFeatureCollection (response, idAttribute) {
  var objectIdField;
  var features = response.features || response.results;
  var count = features && features.length;

  if (idAttribute) {
    objectIdField = idAttribute;
  } else {
    objectIdField = _findIdAttributeFromResponse(response);
  }

  var featureCollection = {
    type: 'FeatureCollection',
    features: []
  };

  if (count) {
    for (var i = features.length - 1; i >= 0; i--) {
      var feature = arcgisToGeoJSON(features[i], objectIdField || _findIdAttributeFromFeature(features[i]));
      featureCollection.features.push(feature);
    }
  }

  return featureCollection;
}


////////////////////////////////////////////////////////////////
// web-worker interface

self.addEventListener('message', function(e) {
  var data = e.data;

  self.postMessage(responseToFeatureCollection(data));
  // I think this causes timing issues where the thread dies before
  // the event has been received and deletes the payload...something
  // like that?:
  // self.close();
}, false);
