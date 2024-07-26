import { RegionType, carbonPrices, CarbonPrice, abatements, Abatement, Filters, } from '../types';
import CarbonAbatementSection from '../CarbonAbatementSection/CarbonAbatementSection';

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
    const filters: Filters[] = JSON.parse(urlParams.get('filters')!);

    return (
        <>
            {layers.map((layer, i) => {
                const abatement = layerToAbatement(layer);
                const carbonPrice = layerToCarbonPrice(layer);

                if (abatement === "CarbonAbatement") {
                    return (
                        <CarbonAbatementSection
                            key={layer}
                            regionType={regionType}
                            carbonPrice={carbonPrice}
                            regions={regions}
                            filters={filters[i]}
                        />
                    );
                } else {
                    throw new Error(`Unknown abatement type: '${abatement}'`);
                }
            })}
        </>
    )
}
