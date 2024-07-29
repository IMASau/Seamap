import { VegaLite, VisualizationSpec } from 'react-vega';
import { RegionAbatementData, AbatementFilters, RegionType, CarbonPrice, Abatement } from '../types';

import './Components.scss'


export function donutChartSpec({ thetaField, colorField, sortField, legendTitle }: { thetaField: string, colorField: string, sortField?: string, legendTitle?: string }): VisualizationSpec {
    return {
        width: 'container',
        encoding: {
            theta: {
                field: thetaField,
                type: 'quantitative',
                stack: true,
            },
            color: {
                field: colorField,
                type: 'nominal',
                legend: { title: legendTitle || colorField },
                sort: sortField ? { field: sortField } : undefined,
            },
        },
        data: { name: 'values', },
        layer: [
            {
                mark: {
                    type: 'arc',
                    innerRadius: 40,
                    outerRadius: 80,
                },
            },
            {
                transform: [
                    {
                        joinaggregate: [{
                            op: 'sum',
                            field: thetaField,
                            as: 'total'
                        }]
                    },
                    {
                        sort: [{ field: colorField }],
                        window: [{
                            op: 'sum',
                            field: thetaField,
                            as: 'Cumulat'
                        }],
                        frame: [null, 0]
                    },
                    {
                        calculate: `(360 * ((datum.Cumulat - (datum.${thetaField} * 0.5)) / datum.total)) - 90`,
                        as: 'angle'
                    }
                ],
                mark: {
                    type: 'text',
                    radius: 85,
                    angle: { expr: 'datum.angle' }
                },
                encoding: {
                    text: { value: '—' },
                    color: {
                        field: colorField,
                        type: 'nominal',
                        legend: null
                    }
                }
            },
            {
                transform: [
                    {
                        joinaggregate: [{
                            op: 'sum',
                            field: thetaField,
                            as: 'total'
                        }]
                    },
                    {
                        calculate: `datum.${thetaField} / datum.total`,
                        as: 'percentage'
                    }
                ],
                mark: {
                    type: 'text',
                    radius: 100,
                    fill: 'black',
                },
                encoding: {
                    text: {
                        field: 'percentage',
                        type: 'nominal',
                        format: '.2%',
                    }
                }
            }
        ]
    };
}

export function AbatementChart({ abatementData, metricField }: { abatementData: RegionAbatementData[], metricField: string }) {
    return (
        <VegaLite
            className="abatement-chart"
            spec={donutChartSpec({
                thetaField: metricField,
                colorField: 'region',
                sortField: 'region',
                legendTitle: 'Region',
            })}
            data={{ values: abatementData }}
            actions={false}
        />
    );
}

export function AbatementSection({ title, children }: { title: string, children: React.ReactNode }) {
    return (
        <div className="abatement-section">
            <div className="abatement-section-heading">
                <h1>{title}</h1>
            </div>
            <div className="abatement-section-content">{children}</div>
        </div>
    )
}

function regionAbatementScenarioToString(carbonPrice: CarbonPrice, abatement: Abatement): string {
    if (carbonPrice === 'cpmax') {
        if (abatement === 'CarbonAbatement') {
            return "Maximum potential cumulative abatement"
        } else if (abatement === 'AbatementArea') {
            return "Maximum potential area of abatement"
        } else {
            throw new Error(`Unknown abatement type: '${abatement}'`);
        }
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
        } else {
            throw new Error(`Unknown carbon price: '${carbonPrice}'`);
        }
        return `Carbon price $${carbonPriceValue}/tCO2`;
    }
}

function regionAbatementScenarioToShort(carbonPrice: CarbonPrice): string {
    if (carbonPrice === 'cpmax') {
        return "Maximum";
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
        } else {
            throw new Error(`Unknown carbon price: '${carbonPrice}'`);
        }
        return `$${carbonPriceValue}/tCO2`;
    }
}

export function AbatementScenarioMessage({ carbonPrice, abatement, abatementFilters }: { carbonPrice: CarbonPrice, abatement: Abatement, abatementFilters: AbatementFilters }) {
    return (
        <div className="abatement-scenario-message">
            Scenarios shown for current selection:
            <ul>
                <li><b>{regionAbatementScenarioToString(carbonPrice, abatement)}</b></li>
                <li><b>Discount rate of {abatementFilters.dr}% for net present values</b></li>
                <li><b>Establishment cost of ${abatementFilters.ec}/ha</b></li>
                <li><b>Abatement cost of ${abatementFilters.ac}/ha</b></li>
            </ul>
        </div>
    );
}

export function AbatementTable<T extends RegionAbatementData>({ regionType, abatementData, metricHeading, metricToString }: { regionType: RegionType, abatementData: T[], metricHeading: string, metricToString: (row: T) => string}) {
    return (
        <table className="abatement-table">
            <thead>
                <tr>
                    <th>{regionType}</th>
                    <th>{metricHeading}</th>
                </tr>
            </thead>
            <tbody>
                {abatementData.map((row) => (
                    <tr key={row.region}>
                        <td>{row.region}</td>
                        <td>{metricToString(row)}</td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}
