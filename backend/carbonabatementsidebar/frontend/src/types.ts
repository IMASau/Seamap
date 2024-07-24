export type RegionType = "STE_NAME11" | "sa2int" | "ID_Primary";

export const carbonPrices = ["cp35", "cp50", "cp65", "cp80", "cpmax"];
export type CarbonPrice = "cp35" | "cp50" | "cp65" | "cp80" | "cpmax";

export const abatements = ["CarbonAbatement", "AbatementArea"];
export type Abatement = "CarbonAbatement" | "AbatementArea";

export interface CarbonAbatement {
    region: string;
    carbon_abatement: number;
}

export interface AbatementArea {
    region: string;
    abatement_area: number;
}
