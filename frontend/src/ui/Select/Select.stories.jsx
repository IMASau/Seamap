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
		breadcrumbs: ["Core", "Components", "Menu"]
	},
	{
		id: 'strawberry',
		text: "Extended example",
		breadcrumbs: ["Core", "Components", "Numeric input"]
	},
	{
		id: 'vanilla',
		text: "Basic example",
		breadcrumbs: ["Core", "Components", "Numeric input"]
	}
]

const FieldTemplate = (args) => {
	const [value, setValue] = React.useState();

	return (
		<Select
			{...args}
			value={value}
			options={options}
			onChange={setValue}
			isSearchable={true}
			isClearable={true}
			isDisabled={false}
		/>
	);
}

export const SimpleSelect = FieldTemplate.bind({});
SimpleSelect.args = {}
