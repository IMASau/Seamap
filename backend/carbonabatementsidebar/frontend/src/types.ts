export type RegionType = "STE_NAME11" | "sa2int" | "ID_Primary";

export const carbonPrices = ["cp35", "cp50", "cp65", "cp80", "cpmax"];
export type CarbonPrice = "cp35" | "cp50" | "cp65" | "cp80" | "cpmax";

export const abatements = ["CarbonAbatement", "AbatementArea"];
export type Abatement = "CarbonAbatement" | "AbatementArea";

export interface AbatementData {
    region: string;
}

export interface CarbonAbatement extends AbatementData {
    carbon_abatement: number;
}

export interface AbatementArea extends AbatementData {
    abatement_area: number;
}
