import { ClaimStatus } from '../enums/claim-status.enum';
import { AutoClaimDetails } from '../models/auto-claim-details.model';
import { HealthClaimDetails } from '../models/health-claim-details.model';
import { HomeClaimDetails } from '../models/home-claim-details.model';
import { DocumentDTO } from './document-dto.model';

export interface ClaimDTO {
  claimId?: number;
  claimDate?: Date;
  claimedAmount: number;
  approvedAmount?: number;
  description: string;
  status?: string;
  fraud?: boolean;
  message?: string;
  contractId: number;
  clientId?: number;
  documentIds?: number[];
  documents?: DocumentDTO[];
  autoDetails?: AutoClaimDetails;
  healthDetails?: HealthClaimDetails;
  homeDetails?: HomeClaimDetails;
}