import { RegionType, CarbonPrice, Abatement, AbatementFilters, } from '../types';
import CarbonAbatementSection from '../CarbonAbatementSection/CarbonAbatementSection';
import AbatementAreaSection from '../AbatementAreaSection/AbatementAreaSection';
import CarbonPriceCarbonAbatementSection from '../CarbonPriceCarbonAbatementSection/CarbonPriceCarbonAbatementSection';
import CarbonPriceAbatementAreaSection from '../CarbonPriceAbatementAreaSection/CarbonPriceAbatementAreaSection';

import './App.scss'


export default function App({ apiUrl, abatementTypes, carbonPrices }: { apiUrl: string, abatementTypes: Abatement[], carbonPrices: CarbonPrice[] }) {
    const urlParams = new URLSearchParams(window.location.search);
    const regionType: RegionType = urlParams.get('region-type') as RegionType;
    const regions: string[] = JSON.parse(urlParams.get('regions')!);
    const layers: string[] = JSON.parse(urlParams.get('layers')!);
    const abatementFilters: AbatementFilters[] = JSON.parse(urlParams.get('filters')!);

    return (
        <div className="abatement-sidebar">
            {layers.map((layer, i) => {
                const abatement = abatementTypes[i];
                const carbonPrice = carbonPrices[i];

                if (abatement === "CarbonAbatement" && regions?.length === 1) {
                    return (
                        <CarbonPriceCarbonAbatementSection
                            key={layer}
                            apiUrl={apiUrl}
                            regionType={regionType}
                            region={regions[0]}
                            abatementFilters={abatementFilters[i]}
                        />
                    );
                } else if (abatement === "CarbonAbatement" && regions?.length !== 1) {
                    return (
                        <CarbonAbatementSection
                            key={layer}
                            apiUrl={apiUrl}
                            regionType={regionType}
                            carbonPrice={carbonPrice}
                            regions={regions}
                            abatementFilters={abatementFilters[i]}
                        />
                    );
                } else if (abatement === "AbatementArea" && regions?.length === 1) {
                    return (
                        <CarbonPriceAbatementAreaSection
                            key={layer}
                            apiUrl={apiUrl}
                            regionType={regionType}
                            region={regions[0]}
                            abatementFilters={abatementFilters[i]}
                        />
                    );
                } else if (abatement === "AbatementArea" && regions?.length !== 1) {
                    return (
                        <AbatementAreaSection
                            key={layer}
                            apiUrl={apiUrl}
                            regionType={regionType}
                            carbonPrice={carbonPrice}
                            regions={regions}
                            abatementFilters={abatementFilters[i]}
                        />
                    );
                } else {
                    throw new Error(`Unknown abatement type: '${abatement}'`);
                }
            })}
        </div>
    )
}
