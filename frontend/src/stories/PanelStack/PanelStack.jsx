import React from 'react';
import PropTypes from "prop-types";
import * as BPCore from "@blueprintjs/core";

export function Panel({content}) {
	return content()
}

export function PanelStack({stack, onClose}) {
	return (
		<BPCore.PanelStack
			stack={stack}
			onClose={onClose}
			renderActivePanelOnly={false}
		/>
	);
}