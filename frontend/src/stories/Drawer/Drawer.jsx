import React from 'react';
import PropTypes from "prop-types";
import * as BPCore from '@blueprintjs/core';

export function Drawer({isOpen, title, onClose, children, position, size}) {
	return (
		<BPCore.Drawer
			isOpen={isOpen}
			title={title}
			position={position}
			size={size}
			onClose={() => onClose ? onClose() : null}
			isCloseButtonShown={true}
			canOutsideClickClose={false}
		>
			{children}
		</BPCore.Drawer>
	);
}