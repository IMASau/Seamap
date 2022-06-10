import React from 'react';
import PropTypes from 'prop-types';
import AsyncSelect from "react-select/async/dist/react-select.esm";
import Select, {components} from "react-select";
import {SimpleListItem} from "../ListItem/ListItem";

function getReactSelectCustomStyles({hasError, isAdded}) {
    return {
        clearIndicator: (provided, state) => {
            return {
                ...provided,
                padding: 4
            }
        },
        container: (provided, state) => {
            return {
                ...provided,
            }
        },
        control: (provided, state) => {
            return {
                ...provided,
                minHeight: 30,
                "&:hover": null,
                border: "none",
                borderRadius: "3px",
                backgroundColor: 
                    state.isDisabled ? "rgba(206, 217, 224, 0.5)" :
                    isAdded ? "#d9822b24" :
                    provided.backgroundColor,
                boxShadow:
                    state.isFocused && hasError ? "0 0 0 1px #db3737, 0 0 0 3px rgb(219 55 55 / 30%), inset 0 1px 1px rgb(16 22 26 / 20%)" :
                    hasError ? "0 0 0 0 rgb(219 55 55 / 0%), 0 0 0 0 rgb(219 55 55 / 0%), inset 0 0 0 1px #db3737, inset 0 0 0 1px rgb(16 22 26 / 15%), inset 0 1px 1px rgb(16 22 26 / 20%)" :
                    state.isFocused ? "0 0 0 1px #137cbd, 0 0 0 3px rgb(19 124 189 / 30%), inset 0 1px 1px rgb(16 22 26 / 20%)" :
                    state.isDisabled ? "none" :
                    "0 0 0 0 rgb(19 124 189 / 0%), 0 0 0 0 rgb(19 124 189 / 0%), inset 0 0 0 1px rgb(16 22 26 / 15%), inset 0 1px 1px rgb(16 22 26 / 20%)",
            }
        },
        dropdownIndicator: (provided, state) => {
            return {
                ...provided,
                padding: 4
            }
        },
        group: (provided, state) => {
            return {
                ...provided,
            }
        },
        groupHeading: (provided, state) => {
            return {
                ...provided,
            }
        },
        indicatorsContainer: (provided, state) => {
            return {
                ...provided,
            }
        },
        indicatorSeparator: (provided, state) => {
            return {
                ...provided,
            }
        },
        input: (provided, state) => {
            return {
                ...provided,
            }
        },
        loadingIndicator: (provided, state) => {
            return {
                ...provided,
            }
        },
        loadingMessage: (provided, state) => {
            return {
                ...provided,
            }
        },
        menu: (provided, state) => {
            return {
                ...provided,
            }
        },
        menuList: (provided, state) => {
            return {
                ...provided,
            }
        },
        menuPortal: (provided, state) => {
            return {
                ...provided,
                zIndex: 9999
            }
        },
        multiValue: (provided, state) => {
            return {
                ...provided,
            }
        },
        multiValueLabel: (provided, state) => {
            return {
                ...provided,
            }
        },
        multiValueRemove: (provided, state) => {
            return {
                ...provided,
            }
        },
        noOptionsMessage: (provided, state) => {
            return {
                ...provided,
            }
        },
        option: (provided, state) => {
            return {
                ...provided,
                padding: "2px 9px"
            }
        },
        placeholder: (provided, state) => {
            return {
                ...provided,
            }
        },
        singleValue: (provided, state) => {
            return {
                ...provided,
                color: state.isDisabled ? "rgba(92, 112, 128, 0.6)" : "#182026",
            }
        },
        valueContainer: (provided, state) => {
            return {
                ...provided,
                padding: "0 6px"
            }
        },
    }
}

export function getReactSelectComponents({Option}) {
    return {
        Option: (args) =>
            <components.Option {...args}><Option data={args.data}/></components.Option>
    }
}

export function AsyncSelectField({value, loadOptions, hasError, disabled, placeholder, getLabel, getValue, getAdded, Option, onChange, onBlur, noOptionsMessage}) {
    const defaultOptions = !disabled
    const isAdded = getAdded ? getAdded(value): false;
    return (
        <AsyncSelect
            styles={getReactSelectCustomStyles({hasError, isAdded})}
            components={getReactSelectComponents({Option})}
            getOptionValue={getValue}
            getOptionLabel={getLabel}
            value={value}
            loadOptions={loadOptions}
            placeholder={placeholder}
            onChange={(value) => onChange(value)}
            onBlur={() => onBlur ? onBlur(): null}
            noOptionsMessage={() => noOptionsMessage ? noOptionsMessage() : "No options"}
            isClearable={true}
            isDisabled={disabled}
            defaultOptions={defaultOptions}
            menuPortalTarget={document.body}
        >
        </AsyncSelect>
    );
}

AsyncSelectField.propTypes = {
    value: PropTypes.object,
    loadOptions: PropTypes.func.isRequired,
    placeholder: PropTypes.string,
    disabled: PropTypes.bool,
    hasError: PropTypes.bool,
    onChange: PropTypes.func.isRequired,
    onBlur: PropTypes.func,
    Option: PropTypes.func.isRequired,
    getValue: PropTypes.func.isRequired,
    getLabel: PropTypes.func.isRequired,
    noOptionsMessage: PropTypes.func,
}

export function SelectField({value, options, hasError, disabled, placeholder, getLabel, getValue, getAdded, Option, onChange, onBlur}) {
    const isAdded = getAdded ? getAdded(value): false;
    const placeholderWhenEnabled = disabled ? null: placeholder;
    return (
        <Select
            styles={getReactSelectCustomStyles({hasError, isAdded})}
            components={getReactSelectComponents({Option})}
            getOptionValue={getValue}
            getOptionLabel={getLabel}
            value={value}
            options={options}
            placeholder={placeholderWhenEnabled}
            onChange={(value) => onChange(value)}
            onBlur={() => onBlur ? onBlur(): null}
            isClearable={true}
            isDisabled={disabled}
            isLoading={false}
            isSearchable={true}
            menuPortalTarget={document.body}
        />
    );
}

SelectField.propTypes = {
    value: PropTypes.object,
    options: PropTypes.arrayOf(PropTypes.object).isRequired,
    placeholder: PropTypes.string,
    disabled: PropTypes.bool,
    hasError: PropTypes.bool,
    onChange: PropTypes.func.isRequired,
    onBlur: PropTypes.func,
    Option: PropTypes.func.isRequired,
    getValue: PropTypes.func.isRequired,
    getLabel: PropTypes.func.isRequired,
}

export function SelectValueField(args) {
  const {value, options, onChange} = args
  // Should use value-key
    const valueOption = options && options.find(option => option.value === value)
    const onValueChange = (option) => onChange(option ? option.value : null)
    return (
        <SimpleSelectField
            {...args}
            value={valueOption}
            onChange={onValueChange}
        >
        </SimpleSelectField>
    );
}

SelectValueField.propTypes = {
    value: PropTypes.string,
    options: PropTypes.arrayOf(PropTypes.object).isRequired,
    placeholder: PropTypes.string,
    disabled: PropTypes.bool,
    hasError: PropTypes.bool,
    onChange: PropTypes.func.isRequired,
    getValue: PropTypes.func.isRequired,
    getLabel: PropTypes.func.isRequired,
}

export function SimpleSelectField(args) {
    const {getLabel} = args
    const Option = ({data}) => <SimpleListItem item={data} getLabel={getLabel}/>
    return (
        <SelectField
            {...args}
            Option={Option}
        />
    );
}

SimpleSelectField.propTypes = {
    value: PropTypes.object,
    options: PropTypes.arrayOf(PropTypes.object).isRequired,
    placeholder: PropTypes.string,
    disabled: PropTypes.bool,
    hasError: PropTypes.bool,
    onChange: PropTypes.func.isRequired,
    getValue: PropTypes.func.isRequired,
    getLabel: PropTypes.func.isRequired,
}
