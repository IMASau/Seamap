import React from 'react';
import {Select} from './Select';
import './Select.css';
import '@blueprintjs/core/lib/css/blueprint.css';

export default {
	title: 'Select/Select',
	component: Select,
	argTypes: {
	}
};

const options = [
	{
		id: 'chocolate',
		text: "Menu item",
		breadcrumbs: ["Core", "Components", "Menu"],
		keywords: "Chocolate Strawberry"
	},
	{
		id: 'strawberry',
		text: "Extended example",
		breadcrumbs: ["Core", "Components", "Numeric input"],
		keywords: "Strawberry Vanilla"
	},
	{
		id: 'vanilla',
		text: "Basic example",
		breadcrumbs: ["Core", "Components", "Numeric input"],
		keywords: "Vanilla Chocolate"
	}
]

const FieldTemplate = (args) => {
	const [value, setValue] = React.useState('a');

	return (
		<Select
			{...args}
			value={value}
			options={options}
			onChange={setValue}
		/>
	);
}

export const SimpleSelect = FieldTemplate.bind({});
SimpleSelect.args = {}
