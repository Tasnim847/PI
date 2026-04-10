// src/app/shared/models/insurance-product.model.ts
import { ProductType } from '../enums/product-type.enum';
import { ProductStatus } from '../enums/product-status.enum';

export interface InsuranceProduct {
    productId: number;      // Note: productId, pas id
    name: string;
    description: string;
    basePrice: number;
    productType: ProductType;
    status: ProductStatus;
    otherType?: string;
    imageUrl?: string;
    // Propriétés supplémentaires pour le frontend
    displayImageUrl?: string;
    price?: number;  // Alias pour basePrice
}