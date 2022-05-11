import React from 'react';
import PropTypes from "prop-types";
import * as BPCore from '@blueprintjs/core';

export function Drawer({isOpen}) {
	return (
		<BPCore.Drawer
			isOpen={isOpen}
		>
		</BPCore.Drawer>
	);
}