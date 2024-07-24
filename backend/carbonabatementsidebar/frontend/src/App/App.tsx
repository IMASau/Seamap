import { RegionType, carbonPrices, CarbonPrice, abatements, Abatement, } from '../types';
import CarbonAbatementRegion from '../CarbonAbatementSection/CarbonAbatementSection';

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
            {layers.map(layer => {
                const abatement = layerToAbatement(layer);
                const carbonPrice = layerToCarbonPrice(layer);

                if (abatement === "CarbonAbatement") {
                    return <CarbonAbatementRegion key={layer} regionType={regionType} carbonPrice={carbonPrice} regions={regions} />
                } else {
                    throw new Error(`Unknown abatement type: '${abatement}'`);
                }
            })}
        </>
    )
}
