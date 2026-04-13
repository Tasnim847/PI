import { Claim } from './claim.model';

export interface HomeClaimDetails {
    damageType: string;
    address: string;
    estimatedLoss: number;
    claim?: Claim;
}
