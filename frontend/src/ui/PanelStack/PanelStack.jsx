import React from 'react';
import PropTypes from "prop-types";
import * as BPCore from "@blueprintjs/core";

export function Panel({content}) {
	return content
}

export function PanelStack({panels, onClose, showPanelHeader}) {
	const stack = panels.map(
		({content, title}) => {
			return {
				props: {
					content: content
				},
				component: Panel, // Using normal function rather than inline function due to way React compares props (https://stackoverflow.com/a/47007354)
				title: title
			}
		}
	);

	return (
		<BPCore.PanelStack
			stack={stack}
			onClose={onClose}
			renderActivePanelOnly={false}
			showPanelHeader={showPanelHeader ?? true}
		/>
	);
}
