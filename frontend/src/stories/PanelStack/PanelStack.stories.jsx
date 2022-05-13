import React from 'react';

import {PanelStack, Panel} from './PanelStack';
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
		setStack(stack => [...stack, panel])
	}

	function close() {
		setStack(stack.slice(0, stack.length-1))
	}

	const chocolatePanel = {
		props: {
			content: () => (
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
		},
		component: Panel,
		title: "Chocolate"
	}

	const straweberryPanel = {
		props: {
			content: () => (
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
		},
		component: Panel,
		title: "Strawberry"
	}

	const vanillaPanel = {
		props: {
			content: () => (
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
		},
		component: Panel,
		title: "Vanilla"
	}

	const [stack, setStack] = React.useState([vanillaPanel])

	return (
		<PanelStack
			{...args}
			stack={stack}
			onClose={close}
			onOpen={open}
		/>
	);
};

export const SimplePanelStack = FieldTemplate.bind({});
SimplePanelStack.args = {
};