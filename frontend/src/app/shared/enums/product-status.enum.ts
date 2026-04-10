// src/app/shared/enums/product-status.enum.ts
export enum ProductStatus {
    ACTIVE = 'ACTIVE',
    INACTIVE = 'INACTIVE',
    REFUSED = 'REFUSED'
}

// Helper pour les labels
export const ProductStatusLabels: Record<ProductStatus, string> = {
    [ProductStatus.ACTIVE]: 'Actif',
    [ProductStatus.INACTIVE]: 'Inactif',
    [ProductStatus.REFUSED]: 'Refusé'
};