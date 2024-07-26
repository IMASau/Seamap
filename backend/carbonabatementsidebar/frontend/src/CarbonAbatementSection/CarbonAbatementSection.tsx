import { useEffect, useState } from 'react';
import { Spinner, Tab, Tabs } from '@blueprintjs/core';

import { RegionType, CarbonPrice, CarbonAbatement } from '../types';
import { AbatementChart, AbatementScenarioMessage, AbatementSection, AbatementTable } from '../Components/Components';


function carbonPriceToScenario(carbonPrice: CarbonPrice): string {
    if (carbonPrice === 'cpmax') {
        return "maximum potential cumulative abatement";
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
        return `carbon price $${carbonPriceValue}/tCO2`;
    }
}


export default function CarbonAbatementSection({ regionType, carbonPrice, regions }: { regionType: RegionType, carbonPrice: CarbonPrice, regions: string[] }) {
    const [abatementData, setAbatementData] = useState<CarbonAbatement[]>([]);
    const [loaded, setLoaded] = useState(false);

    useEffect(() => {
        const fetchAbatementData = async () => {
            const url = new URL('http://localhost:8000/api/carbonabatementsidebar/carbonabatement');
            url.searchParams.append('carbon-price', carbonPrice);
            url.searchParams.append('region-type', regionType);
            if (regions) url.searchParams.append('regions', JSON.stringify(regions));

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
                                <AbatementScenarioMessage scenario={carbonPriceToScenario(carbonPrice)} />
                                <AbatementTable
                                regionType={regionType}
                                abatementData={abatementData}
                                metricHeading="Carbon (MtCO₂)"
                                metricToString={row =>
                                    row.carbon_abatement.toLocaleString(
                                        undefined,
                                        { minimumFractionDigits: 2, maximumFractionDigits: 2 }
                                    )
                                } />
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
                                <AbatementScenarioMessage scenario={carbonPriceToScenario(carbonPrice)} />
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
