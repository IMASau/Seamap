import React from 'react';
import PropTypes from "prop-types";
import * as BPSelect from '@blueprintjs/select';
import * as BPCore from "@blueprintjs/core";

function Breadcrumbs({items}) {
	const interleave = (arr, thing) => [].concat(...arr.map(n => [n, thing])).slice(0, -1)
	const caretted = interleave(
		items,
		<BPCore.Icon icon='caret-right'/>
	)

	return (
		<small className='bp3-text-muted'>
			{caretted.map((e, i) => <span key={i}>{e}</span>)}
		</small>
	)
}

function ItemRenderer({id, text, breadcrumbs}, {handleClick, handleFocus, modifiers, query}) {
	return (
		<BPCore.MenuItem
			active={modifiers.active}
			disabled={modifiers.disabled}
			key={id}
			onClick={handleClick}
			onFocus={handleFocus}
			text={(
				<>
					<div>{text}</div>
					{breadcrumbs ? <Breadcrumbs items={breadcrumbs}/> : null}
				</>
			)}
		/>
	);
}

function filterItems(query, items) {
	try {
		const queryTerms = query.trim().split(/\s+/)
		const patterns = queryTerms.map(e => `(?=.*${e})`)
		const searchRegExp = new RegExp(`^${patterns.join('')}.*$`, 'i')

		return items.filter(({keywords}) => searchRegExp.test(keywords))
	} catch {
		return items;
	}
}

export function Omnibar({placeholder, isOpen, onClose, items, onItemSelect}) {
	return (
		<BPSelect.Omnibar
			inputProps={typeof placeholder === 'string' ? {placeholder: placeholder} : null}
			isOpen={isOpen}
			onClose={onClose}
			items={items}
			itemRenderer={ItemRenderer}
			itemListPredicate={filterItems}
			onItemSelect={({id}) => onItemSelect(id)}
		/>
	);
}

Omnibar.propTypes = {
	placeholder: PropTypes.string,
	isOpen: PropTypes.bool.isRequired,
	onClose: PropTypes.func.isRequired,
	items: PropTypes.arrayOf(PropTypes.shape({
		id: PropTypes.any.isRequired,
		text: PropTypes.string.isRequired,
		breadcrumbs: PropTypes.arrayOf(PropTypes.string),
		keywords: PropTypes.string.isRequired
	})).isRequired,
	onItemSelect: PropTypes.func.isRequired
}
