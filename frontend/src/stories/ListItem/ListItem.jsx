import React from 'react';
import PropTypes from 'prop-types';

export function SimpleListItem({ item, index, disabled, getLabel, onRemoveClick }) {
    return (
        <div className="SimpleListItem">
            <div className="SimpleListItemLabel">{getLabel(item)}</div>
        </div>
    )
}

SimpleListItem.propTypes = {
    item: PropTypes.object,
    disabled: PropTypes.bool,
    getLabel: PropTypes.func
}

export function BreadcrumbListItem({ item, getBreadcrumb, getLabel }) {
    return (
        <div className="BreadcrumbListItem">
            <div className="BreadcrumbListItemLabel">
                <div className="BreadcrumbListItemPath">{getBreadcrumb(item)}</div>
                <div className="BreadcrumbListItemText">{getLabel(item)}</div>
            </div>
        </div>
    )
}

BreadcrumbListItem.propTypes = {
    item: PropTypes.shape({
        item: PropTypes.object,
        disabled: PropTypes.bool,
        getLabel: PropTypes.func,
        getBreadcrumb: PropTypes.func
    }),
}

function TableListItemCol({ item, flex, getLabel }) {
    return (
        <span className="TableListItemCol" style={{ "flex": flex }}>
            {getLabel(item)}
        </span>
    )
}

export function TableListItem({ item, columns }) {
    return (
        <div className="TableListItemRow">
            {columns.map((columnProps, columnIndex) =>
                <TableListItemCol key={"col"+columnIndex} item={item} {...columnProps} />
            )}
        </div>
    )
}

TableListItem.propTypes = {
    item: PropTypes.object,
    disabled: PropTypes.bool,
    columns: PropTypes.arrayOf(PropTypes.shape({
        getLabel: PropTypes.func,
        flex: PropTypes.number
    })),
}

export function TableListColumnHeaderRow({ columns }) {
    return (
        <div className="TableListColumnHeaderRow">
            {columns.map((columnProps, columnIndex) =>
                <span key={columnIndex} className="TableListColumnHeaderCell" style={{ "flex": columnProps.flex }}>
                    {columnProps.columnHeader}
                </span>
            )}
        </div>
    )
}

TableListColumnHeaderRow.propTypes = {
    columns: PropTypes.arrayOf(PropTypes.shape({
        name: PropTypes.node,
    })),
}

