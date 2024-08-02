import { useEffect, useState } from 'react';
import { Spinner} from '@blueprintjs/core';

import { RegionType, AbatementFilters, CarbonPriceCarbonAbatement } from '../types';
import { AbatementSection, CarbonPriceAbatementChart, CarbonPriceAbatementScenarioMessage, CarbonPriceAbatementTable, carbonPriceTocarbonPriceString } from '../Components/Components';


export default function CarbonPriceCarbonAbatementSection({ apiUrl, regionType, region, abatementFilters }: { apiUrl: string, regionType: RegionType, region: string, abatementFilters: AbatementFilters }) {
    const [abatementData, setAbatementData] = useState<CarbonPriceCarbonAbatement[]>([]);
    const [loaded, setLoaded] = useState(false);

    useEffect(() => {
        const fetchAbatementData = async () => {
            const url = new URL(`${apiUrl}/carbonabatementsidebar/carbonpricecarbonabatement`,  window.location.origin);
            url.searchParams.append('region', region);
            url.searchParams.append('region-type', regionType);
            Object.entries(abatementFilters).forEach(([k, v]) => url.searchParams.append(k, v.toString()));

            const response = await fetch(url);
            const data = await response.json();
            data.forEach((element: any) => {
                element.carbonPriceString = carbonPriceTocarbonPriceString(element.carbon_price);
            });

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
                        <CarbonPriceAbatementScenarioMessage
                            regionType={regionType}
                            region={region}
                            abatementFilters={abatementFilters}
                        />
                        <CarbonPriceAbatementTable
                            abatementData={abatementData}
                            metricHeading="Carbon Abatement (MtCO₂)"
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
            chart={
                loaded
                    ? <>
                        <CarbonPriceAbatementScenarioMessage
                            regionType={regionType}
                            region={region}
                            abatementFilters={abatementFilters}
                        />
                        <CarbonPriceAbatementChart
                            abatementData={abatementData}
                            metricField="carbon_abatement"
                        />
                    </>
                    : <Spinner />
            }
        />
    );
}
