import React from 'react';

import {Drawer} from './Drawer';
//import './Drawer.css';
import '@blueprintjs/core/lib/css/blueprint.css';
import {Button, Intent} from '@blueprintjs/core';

export default {
	title: 'Example/Drawer',
	component: Drawer,
	argTypes: {
	}
};

const FieldTemplate = (args) => {
	const [isOpen, setOpen] = React.useState(false);

	function open() {
		setOpen(true)
	}

	function close() {
		setOpen(false)
	}

	return (<div>
		<Button onClick={open} intent={Intent.PRIMARY}>Open Drawer</Button>
		<Drawer
			{...args}
			isOpen={isOpen}
			onClose={close}
		/>
	</div>);
}

export const SimpleDrawer = FieldTemplate.bind({});
SimpleDrawer.args = {
	title: "Drawer Title",
	children: <div style={{padding: "20px 32px"}}>
		Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
	</div>,
	position: "left",
	className: "test-class"
};
