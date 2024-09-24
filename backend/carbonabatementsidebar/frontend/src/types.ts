export type RegionType = "STE_NAME11" | "sa2int" | "ID_Primary";
export type CarbonPrice = "cp35" | "cp50" | "cp65" | "cp80" | "cpmax";
export type Abatement = "CarbonAbatement" | "AbatementArea";
export type CarbonAbatementUnits = "tCO₂" | "MtCO₂";

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

export interface CarbonPriceAbatementData {
    carbon_price: CarbonPrice;
    carbonPriceString: string;
}

export interface CarbonPriceCarbonAbatement extends CarbonPriceAbatementData {
    carbon_abatement: number;
}

export interface CarbonPriceAbatementArea extends CarbonPriceAbatementData {
    abatement_area: number;
}
