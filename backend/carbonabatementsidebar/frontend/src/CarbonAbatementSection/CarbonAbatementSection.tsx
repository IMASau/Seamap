import { useEffect, useState } from 'react';
import { Spinner } from '@blueprintjs/core';

import { RegionType, CarbonPrice, RegionCarbonAbatement, AbatementFilters, CarbonAbatementUnits } from '../types';
import { AbatementChart, AbatementScenarioMessage, AbatementSection, AbatementTable, CarbonAbatementUnitsSwitch } from '../Components/Components';


export default function CarbonAbatementSection({ apiUrl, regionType, carbonPrice, regions, abatementFilters }: { apiUrl: string, regionType: RegionType, carbonPrice: CarbonPrice, regions: string[], abatementFilters: AbatementFilters }) {
    const [abatementData, setAbatementData] = useState<RegionCarbonAbatement[]>([]);
    const [loaded, setLoaded] = useState(false);
    const [units, setUnits] = useState<CarbonAbatementUnits>('tCO₂');

    useEffect(() => {
        const fetchAbatementData = async () => {
            const url = new URL(`${apiUrl}/carbonabatementsidebar/carbonabatement`,  window.location.origin);
            url.searchParams.append('carbon-price', carbonPrice);
            url.searchParams.append('region-type', regionType);
            if (regions) url.searchParams.append('regions', JSON.stringify(regions));
            Object.entries(abatementFilters).forEach(([k, v]) => url.searchParams.append(k, v.toString()));

            const response = await fetch(url);
            const data = await response.json();

            setAbatementData(data);
            setLoaded(true);
        }
        fetchAbatementData();
    }, []);

    return (
        <AbatementSection
            title="Carbon Abatement"
            regionType={regionType}
            abatementType="AbatementArea"
            breakdown={
                loaded
                    ? <>
                        <AbatementScenarioMessage
                            carbonPrice={carbonPrice}
                            abatement={"CarbonAbatement"}
                            abatementFilters={abatementFilters}
                        />
                        <CarbonAbatementUnitsSwitch
                            units={units}
                            setUnits={setUnits}
                        />
                        <AbatementTable
                            regionType={regionType}
                            abatementData={abatementData}
                            metricHeading={`Carbon Abatement (${units})`}
                            metricToString={row => {
                                const value = row.carbon_abatement / (units === 'MtCO₂' ? 1000000 : 1);
                                return value.toLocaleString(
                                    undefined,
                                    units === 'MtCO₂'
                                        ? { minimumFractionDigits: 2, maximumFractionDigits: 2 }
                                        : { minimumFractionDigits: 0, maximumFractionDigits: 0 }
                                );
                            }}
                        />
                    </>
                    : <Spinner />
            }
            chart={
                loaded
                    ? <>
                        <AbatementScenarioMessage
                            carbonPrice={carbonPrice}
                            abatement={"CarbonAbatement"}
                            abatementFilters={abatementFilters}
                        />
                        <AbatementChart
                            regionType={regionType}
                            abatementData={abatementData}
                            metricField="carbon_abatement"
                        />
                    </>
                    : <Spinner />
            }
        />
    );
}
