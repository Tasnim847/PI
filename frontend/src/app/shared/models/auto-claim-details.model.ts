import { Claim } from './claim.model';

export interface AutoClaimDetails {
    driverA: string;
    driverB: string;
    vehicleA: string;
    vehicleB: string;
    accidentLocation: string;
    accidentDate: Date;
    claim?: Claim;
}
