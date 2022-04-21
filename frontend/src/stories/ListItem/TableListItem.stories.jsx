import React from 'react';

import {TableListItem} from './ListItem';
import './ListItem.css';
import NOTES from './NOTES.mdx';

export default {
    title: 'ListItem/TableListItem',
    component: TableListItem,
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

const FieldTemplate = (args) => <TableListItem {...args} />;

export const SimpleField = FieldTemplate.bind({});
SimpleField.args = {
    item: exampleItems[0],
    disabled: false,
    columns: [{
        getLabel: x=>x.label,
        flex: 2
    },{
        getLabel: x=>x.value,
        flex: 1
    }]
};

export const DesignDecisions = NOTES;
