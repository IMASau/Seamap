import React from 'react';

import {Omnibar} from './Omnibar';
import './Omnibar.css';
import '@blueprintjs/core/lib/css/blueprint.css';
import "@blueprintjs/select/lib/css/blueprint-select.css";
import {Button, Intent} from '@blueprintjs/core';

export default {
	title: 'Example/Omnibar',
	component: Omnibar,
	argTypes: {
	}
};

const items = [
	{
		id: 1,
		text: "Menu item",
		breadcrumbs: ["Core", "Components", "Menu"]
	},
	{
		id: 2,
		text: "Extended example",
		breadcrumbs: ["Core", "Components", "Numeric input"]
	},
	{
		id: 3,
		text: "Basic example",
		breadcrumbs: ["Core", "Components", "Numeric input"]
	}
]

const FieldTemplate = (args) => {
	const [isOpen, setOpen] = React.useState(false);

	function open() {
		setOpen(true)
	}

	function close() {
		setOpen(false)
	}

	return (<div>
		<Button onClick={open} intent={Intent.PRIMARY}>Open Omnibar</Button>
		<Omnibar
			{...args}
			isOpen={isOpen}
			onClose={close}
			items={items}
		/>
	</div>);
}

export const SimpleOmnibar = FieldTemplate.bind({});
SimpleOmnibar.args = {
};
