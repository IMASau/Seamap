import React from 'react';
import PropTypes from "prop-types";
import * as BPCore from '@blueprintjs/core';

export function Drawer({isOpen, title, onClose, children, position, size, hasBackdrop, isCloseButtonShown, className}) {
	return (
		<BPCore.Drawer
			isOpen={isOpen}
			title={title}
			position={position}
			size={size}
			hasBackdrop={hasBackdrop}
			onClose={() => onClose ? onClose() : null}
			isCloseButtonShown={isCloseButtonShown}
			canOutsideClickClose={false}
			className={className}
		>
			{children}
		</BPCore.Drawer>
	);
}

Drawer.propTypes = {
    isOpen: PropTypes.bool.isRequired,
	title: PropTypes.node.isRequired,
	onClose: PropTypes.func.isRequired,
	children: PropTypes.node.isRequired,
	position: PropTypes.string,
	size: PropTypes.string,
}
