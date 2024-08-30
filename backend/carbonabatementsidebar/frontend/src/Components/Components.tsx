import { VegaLite, VisualizationSpec } from 'react-vega';
import { RegionAbatementData, AbatementFilters, RegionType, CarbonPriceAbatementData, CarbonPrice, Abatement } from '../types';

import './Components.scss'
import { Tab, TabId, Tabs } from '@blueprintjs/core';
import { useState } from 'react';
import { LayerSpec, UnitSpec } from 'vega-lite/build/src/spec';
import { Field } from 'vega-lite/build/src/channeldef';


interface DataRow {
    [key: string]: any;
}


export function donutChart({ values, thetaField, colorField, sortField, percentageField, legendTitle }: { values: DataRow[], thetaField: string, colorField: string, sortField?: string, percentageField?: string, legendTitle?: string | string[] }): VisualizationSpec {
    const layer: (LayerSpec<Field> | UnitSpec<Field>)[] = [
        {
            mark: {
                type: 'arc',
                innerRadius: 40,
                outerRadius: 80,
            },
            encoding: {
                tooltip: percentageField
                    ? [
                        {
                            field: colorField,
                            title: legendTitle,
                            type: 'nominal'
                        },
                        {
                            field: percentageField,
                            format: '.2%',
                            title: '%',
                            type: 'quantitative'
                        }
                    ]
                    : undefined
            }
        }
    ];
    if (percentageField) {
        layer.push({
            mark: {
                type: 'text',
                radius: 100,
                fill: 'black',
            },
            encoding: {
                text: {
                    value: { expr: `if(datum.${percentageField} > 0.05, (round(datum.${percentageField} * 10000) / 100) + "%", "")` },
                    format: '.2%',
                }
            }
        });
    }

    return {
        width: 'container',
        encoding: {
            theta: {
                field: thetaField,
                type: 'quantitative',
                stack: true,
            },
            order: { field: sortField, sort: 'ascending' },
            color: {
                field: colorField,
                type: 'nominal',
                legend: { title: legendTitle || colorField },
                sort: sortField ? { field: sortField, order: 'ascending' } : undefined,
            },
        },
        data: { values: values },
        layer: layer
    };
}

function regionTypeToString(regionType: RegionType): string {
    if (regionType === 'STE_NAME11') {
        return 'State';
    } else if (regionType === 'sa2int') {
        return 'Statistical Area';
    } else if (regionType === 'ID_Primary') {
        return 'Primary Sediment Compartment';
    } else {
        throw new Error(`Unknown region type: '${regionType}'`);
    }
}

function regionTypeToLegendTitle(regionType: RegionType): string | string[] {
    if (regionType === 'STE_NAME11') {
        return 'State';
    } else if (regionType === 'sa2int') {
        return 'Statistical Area';
    } else if (regionType === 'ID_Primary') {
        return ['Primary', 'Sediment', 'Compartment'];
    } else {
        throw new Error(`Unknown region type: '${regionType}'`);
    }
}


interface SortAndPercentageDataRow extends DataRow {
    percentage: number;
    sort: number;
}


/**
 * Sorts an array of objects by a specified numeric field and calculates the
 * percentage of each object's value relative to the total sum of the field values.
 *
 * @param {DataRow[]} data - The array of objects to be sorted and processed. Each
 *  object should have at least one numeric field specified by `valueField`.
 * @param {string} valueField - The key in each object to be used for sorting and
 *  calculating percentages.
 * @returns {SortAndPercentageDataRow[]} A new array of objects where each object
 *  includes the original properties, the calculated percentage of its `valueField`
 *  relative to the total, and its sort index.
 *
 * @example
 * const data = [
 *   { name: 'A', value: 10 },
 *   { name: 'B', value: 30 },
 *   { name: 'C', value: 20 }
 * ];
 * const result = sortAndPercentage(data, 'value');
 * console.log(result);
 * // Output:
 * // [
 * //   { name: 'B', value: 30, percentage: 0.5, sort: 0 },
 * //   { name: 'C', value: 20, percentage: 0.3333, sort: 1 },
 * //   { name: 'A', value: 10, percentage: 0.1667, sort: 2 }
 * // ]
 */
function sortAndPercentage(data: DataRow[], valueField: string): SortAndPercentageDataRow[] {
    const totalValue = data.map(row => row[valueField]).reduce((a, b) => a + b); // sum of all values in the data

    return data.slice() // shallow copy the data
        .sort((a, b) => b[valueField] - a[valueField]) // sort by value
        .map(
            (row, index) => {
                const percentage = row[valueField] / totalValue; // percentage of total value

                // return new row with percentage and sort index
                return {
                    ...row,
                    percentage,
                    sort: index
                };
            }
        );
}


/**
 * Merges rows in an array based on a specified percentage threshold, combining
 * rows that fall below the threshold into a single "Other" row.
 *
 * @param {SortAndPercentageDataRow[]} data - The array of objects to be processed.
 *  Each object should have properties for sorting, percentage calculation, and the
 *  fields specified by `keyField` and `valueField`.
 * @param {string} keyField - The name of the field used to identify rows. The
 *  "Other" row will use this field to set its value.
 * @param {string} valueField - The name of the field used to aggregate values.
 *  This field is used to calculate the sum of values and percentages.
 * @param {number} percentageThreshold - The threshold percentage (0 to 1) used to
 *  determine which rows are merged into the "Other" row.
 * @returns {SortAndPercentageDataRow[]} A new array of objects where rows below
 *  the percentage threshold are merged into a single "Other" row.
 *
 * @example
 * const data = [
 *   { name: 'A', value: 124, percentage: 0.496, sort: 0 },
 *   { name: 'B', value: 124, percentage: 0.496, sort: 1 },
 *   { name: 'C', value: 1, percentage: 0.004, sort: 2 },
 *   { name: 'D', value: 1, percentage: 0.004, sort: 3 }
 * ];
 * const result = mergeRowsToOther(data, 'name', 'value', 0.005);
 * console.log(result);
 * // Output:
 * // [
 * //   { name: 'A', value: 124, percentage: 0.496, sort: 0 },
 * //   { name: 'B', value: 124, percentage: 0.496, sort: 1 },
 * //   { name: 'Other', value: 2, percentage: 0.008, sort: 2 }
 * // ]
 */
function mergeRowsToOther(data: SortAndPercentageDataRow[], keyField: string, valueField: string, percentageThreshold: number): SortAndPercentageDataRow[] {
    const totalValue = data.map(row => row[valueField]).reduce((a, b) => a + b); // sum of all values in the data

    const filtered = data.filter(row => row[valueField] / totalValue > percentageThreshold); // filter out rows with less than `percentageThreshold` of total value
    const others = data.filter(row => row[valueField] / totalValue <= percentageThreshold); // "other" rows with less than `percentageThreshold` of total value

    // if we have "others" merge their values, and insert a new row for "other"
    if (others.length > 0) {
        const otherValue = others.map(row => row[valueField]).reduce((a, b) => a + b); // sum of all "other" values

        // create a new row for "other" with the sum of all "other" values
        const other = {
            [keyField]: 'Other',
            [valueField]: otherValue,
            percentage: otherValue / totalValue,
            sort: filtered.length
        };
        filtered.push(other);
    }

    return filtered;
}

export function AbatementChart({ regionType, abatementData, metricField }: { regionType: RegionType, abatementData: RegionAbatementData[], metricField: string }) {
    const data = mergeRowsToOther(
        sortAndPercentage(abatementData, metricField), // sort and calculate percentage
        'region',
        metricField,
        0.005
    ); // merge rows with less than 0.5% of total abatement

    return (
        <VegaLite
            className="abatement-chart"
            spec={donutChart({
                values: data,
                thetaField: metricField,
                colorField: 'region',
                sortField: 'sort',
                percentageField: 'percentage',
                legendTitle: regionTypeToLegendTitle(regionType),
            })}
            actions={false}
        />
    );
}

export function AbatementSection({ title, regionType, abatementType, breakdown, chart }: { title: string, regionType: RegionType, abatementType: Abatement, breakdown: React.JSX.Element, chart: React.JSX.Element }) {
    const sectionId = `${regionType}_${abatementType}`;
    const saveSidebarTab = (selectedTabId: TabId) => {
        const carbonAbatementSidebarTabs = JSON.parse(window.localStorage.getItem('carbonAbatementSidebarTabs') || '{}');
        carbonAbatementSidebarTabs[sectionId] = selectedTabId;
        window.localStorage.setItem('carbonAbatementSidebarTabs', JSON.stringify(carbonAbatementSidebarTabs));
    }

    const [selectedTabId, setSelectedTabId] = useState<TabId>(JSON.parse(window.localStorage.getItem('carbonAbatementSidebarTabs') || '{}')[sectionId] || "breakdown");

    return (
        <div className="abatement-section">
            <div className="abatement-section-heading">
                <h1>{title}</h1>
            </div>
            <div className="abatement-section-content">
                <Tabs
                    id="carbon-abatement-tabs"
                    onChange={selectedTabId => {
                        setSelectedTabId(selectedTabId);
                        saveSidebarTab(selectedTabId);
                        setTimeout(() => window.dispatchEvent(new Event('resize'))); // Hack for Vega chart resizing
                    }}
                    selectedTabId={selectedTabId}
                >
                    <Tab
                        id="breakdown"
                        title="Breakdown"
                        panel={breakdown}
                    />
                    <Tab
                        id="chart"
                        title="Chart"
                        panel={chart}
                    />
                </Tabs>
            </div>
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
        return `Carbon price $${carbonPriceValue}/tCO₂`;
    }
}

export function carbonPriceTocarbonPriceString(carbonPrice: CarbonPrice): string {
    if (carbonPrice === 'cp35') {
        return "35";
    } else if (carbonPrice === 'cp50') {
        return "50";
    } else if (carbonPrice === 'cp65') {
        return "65";
    } else if (carbonPrice === 'cp80') {
        return "80";
    } else if (carbonPrice === 'cpmax') {
        return "Max";
    } else {
        throw new Error(`Unknown carbon price: '${carbonPrice}'`);
    }
}

export function AbatementScenarioMessage({ carbonPrice, abatement, abatementFilters }: { carbonPrice: CarbonPrice, abatement: Abatement, abatementFilters: AbatementFilters }) {
    return (
        <div className="abatement-scenario-message">
            Scenarios shown for current selection:
            <ul>
                <li><b>{regionAbatementScenarioToString(carbonPrice, abatement)}</b></li>
                <li><b>Discount rate of {abatementFilters.dr.toLocaleString()}% for net present values</b></li>
                <li><b>Establishment cost of ${abatementFilters.ec.toLocaleString()}/ha</b></li>
                <li><b>5-yearly abatement cost of ${abatementFilters.ac.toLocaleString()}/ha</b></li>
            </ul>
        </div>
    );
}

export function AbatementTable<T extends RegionAbatementData>({ regionType, abatementData, metricHeading, metricToString }: { regionType: RegionType, abatementData: T[], metricHeading: string, metricToString: (row: T) => string }) {
    return (
        <table className="abatement-table">
            <thead>
                <tr>
                    <th>{regionTypeToString(regionType)}</th>
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

export function CarbonPriceAbatementTable<T extends CarbonPriceAbatementData>({ abatementData, metricHeading, metricToString }: { abatementData: T[], metricHeading: string, metricToString: (row: T) => string }) {
    return (
        <table className="abatement-table">
            <thead>
                <tr>
                    <th>Carbon Price ($/tCO₂)</th>
                    <th>{metricHeading}</th>
                </tr>
            </thead>
            <tbody>
                {abatementData.map((row) => (
                    <tr key={row.carbon_price}>
                        <td>{row.carbonPriceString}</td>
                        <td>{metricToString(row)}</td>
                    </tr>
                ))}
            </tbody>
        </table>
    );
}

export function CarbonPriceAbatementScenarioMessage({ regionType, region, abatementFilters }: { regionType: RegionType, region: string, abatementFilters: AbatementFilters }) {
    return (
        <div className="abatement-scenario-message">
            Scenarios shown for current selection:
            <ul>
                <li><b>{regionTypeToString(regionType)} of {region}</b></li>
                <li><b>Discount rate of {abatementFilters.dr}% for net present values</b></li>
                <li><b>Establishment cost of ${abatementFilters.ec}/ha</b></li>
                <li><b>5-yearly abatement cost of ${abatementFilters.ac}/ha</b></li>
            </ul>
        </div>
    );
}

export function CarbonPriceAbatementChart({ abatementData, metricField }: { abatementData: CarbonPriceAbatementData[], metricField: string }) {
    const data = mergeRowsToOther(
        sortAndPercentage(abatementData, metricField), // sort and calculate percentage
        'carbonPriceString',
        metricField,
        0.005
    ); // merge rows with less than 0.5% of total abatement

    return (
        <VegaLite
            className="abatement-chart"
            spec={donutChart({
                values: data,
                thetaField: metricField,
                colorField: 'carbonPriceString',
                sortField: 'carbonPriceString',
                percentageField: 'percentage',
                legendTitle: "Carbon Price ($/tCO₂)",
            })}
            actions={false}
        />
    );
}
