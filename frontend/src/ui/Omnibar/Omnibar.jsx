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
			{caretted.map((e, i)=><span key={i}>{e}</span>)}
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

export function Omnibar({isOpen, onClose, items}) {
	return (
		<BPSelect.Omnibar
			isOpen={isOpen}
			onClose={onClose}
			items={items}
			itemRenderer={ItemRenderer}
			onItemSelect={({id}) => console.log(id)}
		/>
	);
}

Omnibar.propTypes = {
	isOpen: PropTypes.bool.isRequired,
	onClose: PropTypes.func.isRequired,
	items: PropTypes.arrayOf(PropTypes.shape({
		id: PropTypes.any.isRequired,
		text: PropTypes.string.isRequired,
		breadcrumbs: PropTypes.arrayOf(PropTypes.string)
	})).isRequired
}
