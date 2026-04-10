// src/app/shared/dto/product-dto.model.ts
export interface ProductDTO {
    productId?: number;     // Changé: id → productId
    name: string;
    description: string;
    basePrice: number;
    productType: string;
    otherProductType?: string;
    status: string;
    // Les champs suivants ne sont pas dans l'entité backend
    // mais peuvent être utilisés pour d'autres fonctionnalités
    isNew?: boolean;
    promotion?: boolean;
    oldPrice?: number;
    rating?: number;
    reviewsCount?: number;
}