import React from 'react';

import { BreadcrumbListItem } from './ListItem';
import './ListItem.css';
import NOTES from './NOTES.mdx';

export default {
    title: 'ListItem/BreadcrumbListItem',
    component: BreadcrumbListItem,
    argTypes: {
        // Most are inferred from propTypes
        onRemoveClick: { action: 'onChange' },
    }
};

const exampleItems = [
    { uri: 'chocolate', label: 'Chocolate', breadcrumb: "" },
    { uri: 'vanilla', label: 'Vanilla', breadcrumb: "spice" },
    { uri: 'strawberry', label: 'Strawberry', breadcrumb: "berry > red" },
]

const FieldTemplate = (args) => <BreadcrumbListItem {...args} />;

export const DeepBreadcrumbCase = FieldTemplate.bind({});
DeepBreadcrumbCase.args = {
    item: exampleItems[2],
    disabled: false,
    getLabel: x => x.label,
    getBreadcrumb: x => x.breadcrumb
};

export const NoBreadcrumbCase = FieldTemplate.bind({});
NoBreadcrumbCase.args = {
    item: exampleItems[0],
    disabled: false,
    getLabel: x => x.label,
    getBreadcrumb: x => x.breadcrumb
};

export const SimpleBreadcrumbCase = FieldTemplate.bind({});
SimpleBreadcrumbCase.args = {
    item: exampleItems[1],
    disabled: false,
    getLabel: x => x.label,
    getBreadcrumb: x => x.breadcrumb
};

export const DesignDecisions = NOTES;
