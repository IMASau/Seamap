import React from 'react';

import { ItemsSelectionList } from './SelectionList';
import { SimpleListItem } from '../ListItem/ListItem';
import './SelectionList.css';
import NOTES from './ItemsNOTES.mdx';

export default {
    title: 'Example/ItemsSelectionList',
    component: ItemsSelectionList,
    argTypes: {
        // Most are inferred from propTypes
        onChange: { action: 'onChange' }
    }
};

const exampleItems = [
    {
        key: 'chocolate',
        content:
        <div style={{border: "8px solid #5e3816", borderRadius: "12px", backgroundColor: "white", margin: "4px" }}>
            <ul>
                <li>Chocolate 1</li>
                <li>Chocolate 2</li>
                <li>Chocolate 3</li>
            </ul>
        </div>
    },
    {
        key: 'strawberry',
        content:
        <div style={{border: "8px solid #f7d7da", borderRadius: "12px", backgroundColor: "white", margin: "4px" }}>
            <ul>
                <li>Strawberry 1</li>
                <li>Strawberry 2</li>
                <li>Strawberry 3</li>
            </ul>
        </div>
    },
    {
        key: 'vanilla',
        content:
        <div style={{border: "8px solid #ebe8df", borderRadius: "12px", backgroundColor: "white", margin: "4px" }}>
            <ul>
                <li>Vanilla 1</li>
                <li>Vanilla 2</li>
                <li>Vanilla 3</li>
            </ul>
        </div>
    }
]

const FieldTemplate = (args) => <ItemsSelectionList {...args} />;

export const SimpleField = FieldTemplate.bind({});
SimpleField.args = {
    items: exampleItems
};

const reorder = (list, startIndex, endIndex) => {
    const result = Array.from(list);
    const [removed] = result.splice(startIndex, 1);
    result.splice(endIndex, 0, removed);

    return result;
};

export const ReorderingExample = (args) => {
    const [items, setItems] = React.useState(exampleItems);
    return <FieldTemplate {...args}
        items={items}
        onReorder={
            (startIndex, endIndex) =>
                setItems(reorder(items, startIndex, endIndex))
        }
    />;
};

ReorderingExample.args = {
    renderItem: SimpleListItem
};


export const DesignDecisions = NOTES;
