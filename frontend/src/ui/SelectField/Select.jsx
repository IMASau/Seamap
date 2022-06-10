import React from 'react';
import PropTypes from 'prop-types';
import {default as ReactSelect} from "react-select";
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

function ItemRenderer({id, text, breadcrumbs}, {selectValue}) {
	const isSelected = selectValue[0] ? selectValue[0]['id'] == id : false;

	return (
		<div
			className={isSelected ? 'selected' : null}
		>
			<div>{text}</div>
			{breadcrumbs ? <Breadcrumbs items={breadcrumbs}/> : null}
		</div>
	);
}

export function Select({value, options, onChange}) {
	return (
		<ReactSelect
			value={options.filter(({id}) => id == value)}
			options={options}
			getOptionValue={({id})=> id}
			isSearchable={false}
			formatOptionLabel={ItemRenderer}
			onChange={({id}) => onChange(id)}
		/>
	);
}
