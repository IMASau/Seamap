import React from 'react';

import {MyMapContainer} from './MyMapContainer';
// import './boxmap.css';
import 'leaflet/dist/leaflet.css';
import 'leaflet-draw/dist/leaflet.draw.css';

export default {
    title: 'Example/MyMapContainer',
    component: MyMapContainer,
    argTypes: {
    },
};

const Template = (args) => <MyMapContainer {...args} />;

export const EmptyMap = Template.bind({});
EmptyMap.args = {
};
