import { Claim } from './claim.model';

export interface HealthClaimDetails {
    patientName: string;
    hospitalName: string;
    doctorName: string;
    medicalCost: number;
    illnessType: string;
    claim?: Claim;
}
