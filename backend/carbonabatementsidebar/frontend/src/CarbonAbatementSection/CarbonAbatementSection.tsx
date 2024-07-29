import { useEffect, useState } from 'react';
import { Spinner, Tab, Tabs } from '@blueprintjs/core';

import { RegionType, CarbonPrice, RegionCarbonAbatement, AbatementFilters } from '../types';
import { AbatementChart, AbatementScenarioMessage, AbatementSection, AbatementTable } from '../Components/Components';


function carbonPriceToScenario(carbonPrice: CarbonPrice): string {
    if (carbonPrice === 'cpmax') {
        return "Maximum potential cumulative abatement";
    } else {
        let carbonPriceValue;
        if (carbonPrice === 'cp35') {
            carbonPriceValue = 35;
        } else if (carbonPrice === 'cp50') {
            carbonPriceValue = 50;
        } else if (carbonPrice === 'cp65') {
            carbonPriceValue = 65;
        } else if (carbonPrice === 'cp80') {
            carbonPriceValue = 80;
        }
        return `Carbon price $${carbonPriceValue}/tCO2`;
    }
}

export default function CarbonAbatementSection({ apiUrl, regionType, carbonPrice, regions, abatementFilters }: { apiUrl: string, regionType: RegionType, carbonPrice: CarbonPrice, regions: string[], abatementFilters: AbatementFilters }) {
    const [abatementData, setAbatementData] = useState<RegionCarbonAbatement[]>([]);
    const [loaded, setLoaded] = useState(false);

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
        <AbatementSection title="Carbon Abatement">
            <Tabs id="carbon-abatement-tabs" onChange={() => setTimeout(() => window.dispatchEvent(new Event('resize')), 0)}> {/* Hack for Vega chart resizing */}
                <Tab
                    id="breakdown"
                    title="Breakdown"
                    panel={
                        loaded
                            ? <>
                                <AbatementScenarioMessage
                                    scenario={carbonPriceToScenario(carbonPrice)}
                                    abatementFilters={abatementFilters}
                                />
                                <AbatementTable
                                    regionType={regionType}
                                    abatementData={abatementData}
                                    metricHeading="Carbon (MtCO₂)"
                                    metricToString={row =>
                                        row.carbon_abatement.toLocaleString(
                                            undefined,
                                            { minimumFractionDigits: 2, maximumFractionDigits: 2 }
                                        )
                                    }
                                />
                            </>
                            : <Spinner />
                    }
                />
                <Tab
                    id="chart"
                    title="Chart"
                    panel={
                        loaded
                            ? <>
                                <AbatementScenarioMessage
                                    scenario={carbonPriceToScenario(carbonPrice)}
                                    abatementFilters={abatementFilters}
                                />
                                <AbatementChart
                                    abatementData={abatementData}
                                    metricField="carbon_abatement"
                                />
                            </>
                            : <Spinner />
                    }
                />
            </Tabs>
        </AbatementSection>
    )
}
