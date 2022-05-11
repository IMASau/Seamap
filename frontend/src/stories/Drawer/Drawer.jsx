import React from 'react';
import PropTypes from "prop-types";
import * as BPCore from '@blueprintjs/core';

export function Drawer({isOpen, title, onClose, children, position, size}) {
	const positionValue = (
		function(position) {
			switch(position) {
				case "TOP":
				case "Top":
				case "top":
				case "t":
					return BPCore.Position.TOP;
				case "BOTTOM":
				case "Bottom":
				case "bottom":
				case "b":
					return BPCore.Position.BOTTOM;
				case "LEFT":
				case "Left":
				case "left":
				case "l":
					return BPCore.Position.LEFT;
				default:
					return BPCore.Position.RIGHT;
			}
		}
	)(position);

	return (
		<BPCore.Drawer
			isOpen={isOpen}
			title={title}
			position={positionValue}
			size={size}
			onClose={() => onClose ? onClose() : null}
			isCloseButtonShown={true}
			canOutsideClickClose={false}
		>
			{children}
		</BPCore.Drawer>
	);
}