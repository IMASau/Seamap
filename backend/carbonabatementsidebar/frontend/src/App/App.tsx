import { RegionType, carbonPrices, CarbonPrice, abatements, Abatement, } from '../types';

import './App.scss'


function layerToCarbonPrice(layer: string): CarbonPrice {
    for (const carbonPrice of carbonPrices) {
        if (layer.includes(carbonPrice)) {
            return carbonPrice as CarbonPrice;
        }
        if (layer.includes("max")) {
            return "cpmax";
        }
    }
    throw new Error(`Could not find carbon price in layer name: ${layer}`);
}

function layerToAbatement(layer: string): Abatement {
    for (const abatement of abatements) {
        if (layer.includes(abatement)) {
            return abatement as Abatement;
        }
    }
    throw new Error(`Could not find abatement in layer name: ${layer}`);
}

export default function App() {
    const urlParams = new URLSearchParams(window.location.search);
    const regionType: RegionType = urlParams.get('region-type') as RegionType;
    const regions: string[] = JSON.parse(urlParams.get('regions')!);
    const layers: string[] = JSON.parse(urlParams.get('layers')!);

    return (
        <>
            Testing
        </>
    )
}
