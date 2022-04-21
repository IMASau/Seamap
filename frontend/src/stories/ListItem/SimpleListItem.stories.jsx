import React from 'react';

import {SimpleListItem} from './ListItem';
import './ListItem.css';
import NOTES from './NOTES.mdx';

export default {
    title: 'ListItem/SimpleListItem',
    component: SimpleListItem,
    argTypes: {
        // Most are inferred from propTypes
        onRemoveClick: {action: 'onChange'},
    }
};

const exampleItems = [
    {value: 'chocolate', label: 'Chocolate'},
    {value: 'strawberry', label: 'Strawberry'},
    {value: 'vanilla', label: 'Vanilla'}
]

const FieldTemplate = (args) => <SimpleListItem {...args} />;

export const SimpleField = FieldTemplate.bind({});
SimpleField.args = {
    item: exampleItems[0],
    disabled: false,
    getLabel: x=>x.label
};

export const DesignDecisions = NOTES;
