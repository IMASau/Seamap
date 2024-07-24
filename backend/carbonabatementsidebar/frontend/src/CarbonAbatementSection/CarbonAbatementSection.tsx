import { useEffect, useState } from 'react';
import { Spinner, Tab, Tabs } from '@blueprintjs/core';

import { RegionType, CarbonPrice, CarbonAbatement } from '../types';
import { AbatementSection, AbatementTable } from '../Components/Components';


export default function CarbonAbatementSection({ regionType, carbonPrice, regions }: { regionType: RegionType, carbonPrice: CarbonPrice, regions: string[]}) {
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
            <Tabs id="carbon-abatement-tabs">
                        <Tab
                            id="breakdown"
                            title="Breakdown"
                            panel={
                                loaded
                                    ? <AbatementTable
                                        regionType={regionType}
                                        abatementData={abatementData}
                                        metricHeading="Carbon (MtCO₂)"
                                        metricToString={row =>
                                            row.carbon_abatement.toLocaleString(
                                                undefined,
                                                { minimumFractionDigits: 2, maximumFractionDigits: 2}
                                            )
                                        } />
                                    : <Spinner />
                            }
                        />
                        <Tab
                            id="chart"
                            title="Chart"
                            panel={
                                loaded
                                    ? <>Chart in progress...</>
                                    : <Spinner />
                            }
                        />
                    </Tabs>
        </AbatementSection>
    )
}
