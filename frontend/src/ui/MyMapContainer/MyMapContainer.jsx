import React from 'react';
import { FeatureGroup, MapContainer, TileLayer } from 'react-leaflet';
import { EditControl } from 'react-leaflet-draw';

export const MyMapContainer = () => {
    console.log(React.version)

    return (
        <MapContainer
            style={{
                "height": 500,
                "zIndex": 10,
            }}
            center={[-28, 134]}
            zoom={4}
        >
            <TileLayer url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png" />
            <FeatureGroup>
                <EditControl />
            </FeatureGroup>
        </MapContainer>
    );
};

MyMapContainer.propTypes = {
};
