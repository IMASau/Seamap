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

export function Select({value, options, onChange, isSearchable, isClearable}) {
	return (
		<ReactSelect
			value={options.filter(({id}) => id == value)}
			options={options}
			getOptionValue={({id})=> id}
			isSearchable={isSearchable}
			isClearable={isClearable}
			filterOption={(option, inputValue) => {
				inputValue = inputValue.toLowerCase();
				const breadcrumbContains = option.data.breadcrumbs.map(e => e.toLowerCase().includes(inputValue)).reduce(
					(e1, e2) => e1 || e2
				);
				return option.data.text.toLowerCase().includes(inputValue) || breadcrumbContains;
			}}
			formatOptionLabel={ItemRenderer}
			onChange={e => onChange(e ? e.id : e)}
		/>
	);
}

Select.propTypes = {
	value: PropTypes.any,
	options: PropTypes.arrayOf(PropTypes.shape({
		id: PropTypes.any.isRequired,
		text: PropTypes.string.isRequired,
		breadcrumbs: PropTypes.arrayOf(PropTypes.string)
	})).isRequired,
	onChange: PropTypes.func.isRequired
}
