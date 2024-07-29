import { useEffect, useState } from 'react';
import { Spinner, Tab, Tabs } from '@blueprintjs/core';

import { RegionType, CarbonPrice, RegionAbatementArea, AbatementFilters } from '../types';
import { AbatementChart, AbatementScenarioMessage, AbatementSection, AbatementTable } from '../Components/Components';


export default function AbatementAreaSection({ apiUrl, regionType, carbonPrice, regions, abatementFilters }: { apiUrl: string, regionType: RegionType, carbonPrice: CarbonPrice, regions: string[], abatementFilters: AbatementFilters }) {
    const [abatementData, setAbatementData] = useState<RegionAbatementArea[]>([]);
    const [loaded, setLoaded] = useState(false);

    useEffect(() => {
        const fetchAbatementData = async () => {
            const url = new URL(`${apiUrl}carbonabatementsidebar/abatementarea`,  window.location.origin);
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
        <AbatementSection title="Abatement Area">
            <Tabs id="carbon-abatement-tabs" onChange={() => setTimeout(() => window.dispatchEvent(new Event('resize')), 0)}> {/* Hack for Vega chart resizing */}
                <Tab
                    id="breakdown"
                    title="Breakdown"
                    panel={
                        loaded
                            ? <>
                                <AbatementScenarioMessage
                                    carbonPrice={carbonPrice}
                                    abatement={"AbatementArea"}
                                    abatementFilters={abatementFilters}
                                />
                                <AbatementTable
                                    regionType={regionType}
                                    abatementData={abatementData}
                                    metricHeading="Area (ha)"
                                    metricToString={row =>
                                        row.abatement_area.toLocaleString(
                                            undefined,
                                            { maximumFractionDigits: 0 }
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
                                    carbonPrice={carbonPrice}
                                    abatement={"AbatementArea"}
                                    abatementFilters={abatementFilters}
                                />
                                <AbatementChart
                                    abatementData={abatementData}
                                    metricField="abatement_area"
                                />
                            </>
                            : <Spinner />
                    }
                />
            </Tabs>
        </AbatementSection>
    )
}
