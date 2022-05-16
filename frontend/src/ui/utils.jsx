import React from 'react';
import {Intent} from "@blueprintjs/core";
import { FocusStyleManager } from "@blueprintjs/core";

export function setupBlueprint() {
    FocusStyleManager.onlyShowFocusOnTabs();
}

export function useCachedState(value) {
    const [stateValue, setStateValue] = React.useState(value);
    React.useEffect(() => {
        setStateValue(value)
    }, [value])
    return [stateValue, setStateValue]
}

export function hasErrorIntent({hasError, disabled}) {
    return (hasError && !disabled) ? Intent.DANGER : Intent.NONE;
}

export function requiredLabelInfo({required}) {
    return required ? "*" : null
}

// https://stackoverflow.com/a/52829183
export const downloadFile = (blob, fileName) => {
    const link = document.createElement('a');
    // create a blobURI pointing to our Blob
    link.href = URL.createObjectURL(blob);
    link.download = fileName;
    // some browser needs the anchor to be in the doc
    document.body.append(link);
    link.click();
    link.remove();
    // in case the Blob uses a lot of memory
    setTimeout(() => URL.revokeObjectURL(link.href), 7000);
};
