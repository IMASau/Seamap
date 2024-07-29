export type RegionType = "STE_NAME11" | "sa2int" | "ID_Primary";

export const carbonPrices = ["cp35", "cp50", "cp65", "cp80", "cpmax"];
export type CarbonPrice = "cp35" | "cp50" | "cp65" | "cp80" | "cpmax";

export const abatements = ["CarbonAbatement", "AbatementArea"];
export type Abatement = "CarbonAbatement" | "AbatementArea";

export interface AbatementFilters {
    dr: number;
    ec: number;
    ac: number;
}

export interface RegionAbatementData {
    region: string;
}

export interface RegionCarbonAbatement extends RegionAbatementData {
    carbon_abatement: number;
}

export interface RegionAbatementArea extends RegionAbatementData {
    abatement_area: number;
}
