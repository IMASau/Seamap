import React from 'react';

import {Drawer} from './Drawer';
//import './Drawer.css';
import '@blueprintjs/core/lib/css/blueprint.css';
import {Button} from '@blueprintjs/core';

export default {
    title: 'Example/Drawer',
    component: Drawer
};

const FieldTemplate = (args) =>
	<div>
		<Button>A</Button>
		<Drawer {...args} />
	</div>;

export const SimpleDrawer = FieldTemplate.bind({});
SimpleDrawer.args = {
	isOpen: true,
};