import { useMap } from 'react-leaflet';
import { useEffect, useRef } from 'react';
import { sideBySide } from './leaflet-side-by-side/index'

// This component doesn't render children - it just sets up the side-by-side control
// Use it AFTER rendering your two layers, passing their pane name
export default function SideBySide({ leftPane, rightPane, leftLabelText, rightLabelText, onDragEnd, rangeValue }) {
  const map = useMap();
  const controlRef = useRef(null);

  useEffect(() => {
    const leftPaneEl = map.getPane(leftPane);
    const rightPaneEl = map.getPane(rightPane);

    // Load the plugin and create control
    const sideBySideControl = sideBySide(leftPaneEl, rightPaneEl, { onDragEnd, rangeValue, leftLabelText, rightLabelText });
    sideBySideControl.addTo(map);
    controlRef.current = sideBySideControl;

    return () => {
      if (controlRef.current && map) {
        map.removeControl(controlRef.current);
      }
    };
  }, [map]);

  useEffect(() => {
    if (!map || !controlRef.current) return;
    const leftPaneEl = map.getPane(leftPane);
    controlRef.current.setLeftPane(leftPaneEl);
  }, [leftPane]);

  useEffect(() => {
    if (!map || !controlRef.current) return;
    const rightPaneEl = map.getPane(rightPane);
    controlRef.current.setRightPane(rightPaneEl);
  }, [rightPane]);

  useEffect(() => {
    if (!map || !controlRef.current) return;
    controlRef.current.setLeftLabelText(leftLabelText);
  }, [leftLabelText]);

  useEffect(() => {
    if (!map || !controlRef.current) return;
    controlRef.current.setRightLabelText(rightLabelText);
  }, [rightLabelText]);

  return null;
}
