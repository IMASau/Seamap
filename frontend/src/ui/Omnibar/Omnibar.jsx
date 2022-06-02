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

function ItemRenderer(item, {handleClick, handleFocus, modifiers, query}) {
	var path = ["Core", "Components", "Menu"]

	return (
		<BPCore.MenuItem
			active={modifiers.active}
			disabled={modifiers.disabled}
			key={item}
			onClick={handleClick}
			onFocus={handleFocus}
			text={(
				<>
					<div>{item}</div>
					<Breadcrumbs items={path}/>
				</>
			)}
		/>
	);
}

export function Omnibar({isOpen, onClose}) {
	return (
		<BPSelect.Omnibar
			isOpen={isOpen}
			onClose={onClose}
			items={["Menu item", "B", "C"]}
			itemRenderer={ItemRenderer}
			onItemSelect={onClose}
		/>
	);
}

Omnibar.propTypes = {
	isOpen: PropTypes.bool.isRequired,
	onClose: PropTypes.func.isRequired
}
