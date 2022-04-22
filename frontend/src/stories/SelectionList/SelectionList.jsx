import React from 'react';
import PropTypes from 'prop-types';
import { DragDropContext, Draggable, Droppable } from "react-beautiful-dnd";
import { useCachedState } from "../utils";
import * as ReactDOM from "react-dom";
import { BreadcrumbListItem, SimpleListItem, TableListColumnHeaderRow, TableListItem } from "../ListItem/ListItem";
import * as BPCore from "@blueprintjs/core"

const getItemStyle = (isDragging, draggableStyle) => ({
    // some basic styles to make the items look a bit nicer
    userSelect: "none",

    // change background colour if dragging
    background: isDragging ? "rgba(.6,0.6,0.6,.25)" : null,

    // styles we need to apply on draggables
    ...draggableStyle
});

const getListStyle = isDraggingOver => ({
    background: isDraggingOver ? "lightblue" : null,
});


function reorder(list, startIndex, endIndex) {
    const result = Array.from(list);
    const [removed] = result.splice(startIndex, 1);
    result.splice(endIndex, 0, removed);
    return result;
}

function DragHandle() {
    return <BPCore.Icon icon="drag-handle-vertical" />
}

function RemoveButton({ index, onRemoveClick, disabled }) {
    if (!onRemoveClick) {
        return null
    } else {
        return (
            <BPCore.Button icon="cross"
                small={true}
                minimal={true}
                disabled={disabled}
                onClick={_ => onRemoveClick(index)} />
        )
    }
}

function ItemLabel({ itemProps, item, index, onItemClick, renderItem }) {
    const body = renderItem({ ...itemProps, item, index });
    const onClick = onItemClick ? (_ => onItemClick(index)) : null;
    return (
        <div onClick={onClick}>
            {body}
        </div>
    )
}

// NOTE: Workaround for conflict with blueprint expander (transform related?)
// https://github.com/vtereshyn/react-beautiful-dnd-ru/blob/master/docs/patterns/using-a-portal.md
const portal = document.createElement('div');
portal.classList.add('SelectionListPortal');

if (!document.body) {
    throw new Error('body not ready for portal creation!');
} else {
    document.body.appendChild(portal);
}

function PortalAwareItem({ provided, snapshot, itemLabel, removeButton, className, isDragDisabled }) {

    const usePortal = snapshot.isDragging;

    const child = (
        <div
            ref={provided.innerRef}
            {...provided.draggableProps}
            style={getItemStyle(
                snapshot.isDragging,
                provided.draggableProps.style
            )}
            className={"DragHandleWrapper " + className}
        >
            <div className="DragHandleWrapperHandle"
                {...provided.dragHandleProps}>
                { isDragDisabled ? <span/> : <DragHandle /> }
            </div>
            <div className="DragHandleWrapperLabel">
                {itemLabel}
            </div>
            <div className="DragHandleWrapperRemoveButton">
                {removeButton}
            </div>
        </div>
    );

    if (!usePortal) {
        return child;
    } else {
        return ReactDOM.createPortal(child, portal);
    }
}

function getSelectionListItemClass({ item, getAdded, onItemClick }) {
    const clickableClass = onItemClick ? "SelectionListItemClickable": "";
    if (getAdded) {
        const isAdded = getAdded && getAdded(item);
        const itemClass = isAdded ? "SelectionListAddedItem" : "SelectionListPickedItem"
        return [clickableClass, itemClass].join(" ")
    } else {
        return clickableClass
    }
}

// NOTE: Attempts to workaround glitch on recorder by caching state
// NOTE: Component should change key to flush invalid state if necessary
export function SelectionList({ items, itemProps, getValue, getAdded, onReorder, onItemClick, onRemoveClick, disabled, renderItem }) {

    const [stateValue, setStateValue] = useCachedState(items);
    const isDragDisabled = disabled || !onReorder;

    const onDragEnd = (result) => {
        if (result.destination) {
            // Optimistially reorder to avoid glitch
            setStateValue(reorder(
                stateValue,
                result.source.index,
                result.destination.index
            ))
            onReorder(
                result.source.index,
                result.destination.index
            )
        }
    }

    return (
        <DragDropContext onDragEnd={onDragEnd}>
            <Droppable droppableId="droppable">
                {(provided, snapshot) => (
                    <div
                        {...provided.droppableProps}
                        ref={provided.innerRef}
                        style={getListStyle(snapshot.isDraggingOver)}
                    >
                        {stateValue.map((item, index) => (
                            <Draggable key={getValue(item)}
                                draggableId={""+getValue(item)}
                                index={index}
                                isDragDisabled={isDragDisabled}>
                                {(provided, snapshot) => (
                                    <PortalAwareItem
                                        provided={provided}
                                        snapshot={snapshot}
                                        className={["SelectionListItem", getSelectionListItemClass({ item, getAdded, onItemClick })].join(" ")}
                                        itemLabel={ItemLabel({ itemProps, item, index, onItemClick, renderItem })}
                                        removeButton={RemoveButton({ disabled, onRemoveClick, index })}
                                        isDragDisabled={isDragDisabled}
                                    />
                                )}
                            </Draggable>
                        ))}
                        {provided.placeholder}
                    </div>
                )}
            </Droppable>
        </DragDropContext>
    )
}

SelectionList.propTypes = {
    items: PropTypes.arrayOf(PropTypes.shape({
        value: PropTypes.string,
        label: PropTypes.string
    })).isRequired,
    onReorder: PropTypes.func,
    onItemClick: PropTypes.func,
    onRemoveClick: PropTypes.func,
    renderItem: PropTypes.func.isRequired,
    getValue: PropTypes.func.isRequired,
    getAdded: PropTypes.func,
    disabled: PropTypes.bool,
}

export function TableSelectionList({ items, getValue, getAdded, onReorder, onItemClick, onRemoveClick, disabled, columns }) {
    if (items.length === 0) {
        return <div />
    }
    return (
        <div>
            <div className="TableSelectionListColumnHeaderRow">
                <TableListColumnHeaderRow columns={columns} />
            </div>
            <SelectionList
                items={items}
                onReorder={onReorder}
                onItemClick={onItemClick}
                onRemoveClick={onRemoveClick}
                getValue={getValue}
                getAdded={getAdded}
                disabled={disabled}
                renderItem={({ item }) =>
                    <TableListItem
                        item={item}
                        columns={columns}
                        disabled={disabled} />}>
            </SelectionList>
        </div>

    )
}

TableSelectionList.propTypes = {
    items: PropTypes.arrayOf(PropTypes.object).isRequired,
    getValue: PropTypes.func.isRequired,
    getAdded: PropTypes.func,
    onReorder: PropTypes.func,
    onItemClick: PropTypes.func,
    onRemoveClick: PropTypes.func,
    disabled: PropTypes.bool,
    columns: PropTypes.arrayOf(PropTypes.shape({
        flex: PropTypes.number.isRequired,
        getLabel: PropTypes.func.isRequired,
    }))
}

export function BreadcrumbSelectionList({ items, getValue, getLabel, getBreadcrumb, getAdded, onReorder, onItemClick, onRemoveClick, disabled }) {
    return (
        <SelectionList
            items={items}
            onReorder={onReorder}
            onItemClick={onItemClick}
            onRemoveClick={onRemoveClick}
            getValue={getValue}
            getAdded={getAdded}
            disabled={disabled}
            renderItem={({ item }) => (
                <BreadcrumbListItem
                    item={item}
                    disabled={disabled}
                    getBreadcrumb={getBreadcrumb}
                    getLabel={getLabel} />
            )}>
        </SelectionList>
    )
}

BreadcrumbSelectionList.propTypes = {
    items: PropTypes.arrayOf(PropTypes.object).isRequired,
    getValue: PropTypes.func.isRequired,
    getLabel: PropTypes.func.isRequired,
    getBreadcrumb: PropTypes.func.isRequired,
    getAdded: PropTypes.func,
    onReorder: PropTypes.func,
    onItemClick: PropTypes.func,
    onRemoveClick: PropTypes.func,
    disabled: PropTypes.bool,
}

export function SimpleSelectionList({ items, getValue, getLabel, getAdded, onReorder, onItemClick, onRemoveClick, disabled }) {
    return (
        <SelectionList
            items={items}
            onReorder={onReorder}
            onItemClick={onItemClick}
            onRemoveClick={onRemoveClick}
            getValue={getValue}
            getAdded={getAdded}
            disabled={disabled}
            renderItem={({ item }) => (
                <SimpleListItem
                    item={item}
                    disabled={disabled}
                    getLabel={getLabel} />
            )}>
        </SelectionList>
    )
}

SimpleSelectionList.propTypes = {
    items: PropTypes.arrayOf(PropTypes.object).isRequired,
    getValue: PropTypes.func.isRequired,
    getLabel: PropTypes.func.isRequired,
    getAdded: PropTypes.func,
    onReorder: PropTypes.func,
    onItemClick: PropTypes.func,
    onRemoveClick: PropTypes.func,
    disabled: PropTypes.bool,
}
