import React from 'react';

import {PanelStack} from './PanelStack';
import './PanelStack.css';
import '@blueprintjs/core/lib/css/blueprint.css';
import {Button, Intent} from '@blueprintjs/core';

export default {
	title: 'Example/PanelStackExample',
	component: PanelStack,
	argTypes: {
	}
};

const FieldTemplate  = (args) => {
	function open(panel) {
		setPanels(panels => [...panels, panel])
	}

	function close() {
		setPanels(panels.slice(0, panels.length-1))
	}

	const chocolatePanel = {
		content: (
			<div className='panel-stack-content'>
				<div>
					This is the chocolate panel!
				</div>
				<Button
					onClick={() => open(chocolatePanel)}
					intent={Intent.PRIMARY}
				>
					Open Another Chocolate Panel
				</Button>
			</div>
		),
		title: "Chocolate"
	}

	const straweberryPanel = {
		content: (
			<div className='panel-stack-content'>
				<div>
					This is the strawberry panel!
				</div>
				<Button
					onClick={() => open(chocolatePanel)}
					intent={Intent.PRIMARY}
				>
					Open Chocolate Panel
				</Button>
			</div>
		),
		title: "Strawberry"
	}

	const vanillaPanel = {
		content: (
			<div className='panel-stack-content'>
				<div>
					This is the vanilla panel!
				</div>
				<Button
					onClick={() => open(straweberryPanel)}
					intent={Intent.PRIMARY}
				>
					Open Strawberry Panel
				</Button>
			</div>
		),
		title: "Vanilla"
	}

	const [panels, setPanels] = React.useState([vanillaPanel])

	return (
		<PanelStack
			{...args}
			panels={panels}
			onClose={close}
			onOpen={open}
		/>
	);
};

export const SimplePanelStack = FieldTemplate.bind({});
SimplePanelStack.args = {
};