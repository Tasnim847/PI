import { ClientDTO } from '../dto/client-dto.model';
import { CompensationStatus } from '../enums/compensation-status.enum';
import { Claim } from './claim.model';

export interface Compensation {
    compensationId: number;
    amount: number;
    paymentDate: Date;
    clientOutOfPocket: number;
    coverageLimit: number;
    deductible: number;
    originalClaimedAmount: number;
    approvedAmount: number;
    message: string;
    status: CompensationStatus;
    riskScore: number;
    riskLevel: string;
    decisionSuggestion: string;
    scoringDetails: string;
    adjustedAmount: number;
    calculationDate: Date;
    claim?: Claim;
    client?: ClientDTO;  // 🔥 AJOUTER CETTE PROPRIÉTÉ

}
