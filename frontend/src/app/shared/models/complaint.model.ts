export interface Complaint {
  id: number;
  title: string;
  description: string;
  customerName: string;
  email: string;
  status: 'open' | 'in-progress' | 'resolved' | 'closed';
  priority: 'low' | 'medium' | 'high';
  createdAt: Date;
  updatedAt?: Date;
}

export type CreateComplaint = Omit<Complaint, 'id' | 'createdAt'>;
export type UpdateComplaint = Partial<Omit<Complaint, 'id'>>;