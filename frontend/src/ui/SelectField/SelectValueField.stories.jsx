import React from 'react';

import {SimpleSelectField} from './SelectField';
import './SelectField.css';
import '@blueprintjs/core/lib/css/blueprint.css';
import NOTES from './NOTES.mdx';

export default {
	title: 'SelectField/SimpleSelectField',
	component: SimpleSelectField,
	argTypes: {
		// Most are inferred from propTypes
		onChange: {action: 'onChange'},
		onBlur: {action: 'onBlur'},
	}
};

const FieldTemplate = (args) => <SimpleSelectField {...args} />;

const options = [
	{value: 'chocolate', label: 'Chocolate'},
	{value: 'strawberry', label: 'Strawberry'},
	{value: 'vanilla', label: 'Vanilla'}
]

export const SimpleField = FieldTemplate.bind({});
SimpleField.args = {
	value: null,
	options: options,
	placeholder: "",
	disabled: false,
	hasError: false,
	getValue: (option => option.value),
	getLabel: (option => option.label)
};

export const FieldDisabledState = FieldTemplate.bind({});
FieldDisabledState.args = {
	value: options[0].value,
	options: options,
	disabled: true,
	getValue: (option => option.value),
	getLabel: (option => option.label)
};

export const FieldWithError = FieldTemplate.bind({});
FieldWithError.args = {
	value: options[0].value,
	options: options,
	hasError: true,
	getValue: (option => option.value),
	getLabel: (option => option.label)
};

export const FieldWithInvalidValue = FieldTemplate.bind({});
FieldWithInvalidValue.args = {
	value: 'marzipan',
	options: options,
	hasError: true,
	getValue: (option => option.value),
	getLabel: (option => option.label)
};


export const EmptyFieldWithPlaceholder = FieldTemplate.bind({});
EmptyFieldWithPlaceholder.args = {
	placeholder: "This is the placeholder",
	options: options,
	getValue: (option => option.value),
	getLabel: (option => option.label)
};

export const DesignDecisions = NOTES;
