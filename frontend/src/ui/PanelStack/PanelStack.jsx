import React from 'react';
import PropTypes from "prop-types";
import * as BPCore from "@blueprintjs/core";

export function PanelStack({panels, onClose}) {
	const stack = panels.map(
		({content, title}) => {
			return {
				props: {
					content: content
				},
				component: ({content}) => content,
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
