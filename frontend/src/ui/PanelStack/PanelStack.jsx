import React from 'react';
import PropTypes from "prop-types";
import * as BPCore from "@blueprintjs/core";

export function Panel({content}) {
	return content
}

export function PanelStack({panels, onClose}) {
	const stack = panels.map(
		({content, title}) => {
			return {
				props: {
					content: content
				},
				component: Panel,
				title: title
			}
		}
	);

	return (
		<BPCore.PanelStack
			stack={stack}
			onClose={onClose}
			renderActivePanelOnly={false}
		/>
	);
}