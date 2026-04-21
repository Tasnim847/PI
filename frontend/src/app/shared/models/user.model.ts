import { Role } from '../enums/role.enum';

export interface User {
    id: number;
    firstName: string;
    lastName: string;
    email: string;
    password?: string;
    telephone: string;
    role: Role;
    otp?: string;
    otpExpiry?: Date;
    loginAttempts?: number;
    accountNonLocked?: boolean;
    lockTime?: Date;
    photo?: string;
}
