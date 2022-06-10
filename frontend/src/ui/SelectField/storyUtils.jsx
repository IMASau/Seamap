
export const options = [
    {value: 'chocolate', label: 'Chocolate', breadcrumb: ''},
    {value: 'strawberry', label: 'Strawberry', breadcrumb: 'one'},
    {value: 'vanilla', label: 'Vanilla', breadcrumb: 'one > two'}
]
export const getValue = (option) => option ? option.value : null;
export const getLabel = (option) => option ? option.label : null;
export const getBreadcrumb = (option) => option ? option.breadcrumb : null;

export const loadOptions = (s) => {
    return Promise.resolve(options.filter(o => getLabel(o).toLowerCase().includes(s.toLowerCase())))
}
