// src/app/shared/enums/product-type.enum.ts
export enum ProductType {
    AUTO = 'AUTO',
    HEALTH = 'HEALTH',  // Changé de SANTE à HEALTH
    HOME = 'HOME',      // Changé de HABITATION à HOME
    LIFE = 'LIFE',      // Changé de VIE à LIFE
    OTHER = 'OTHER'     // Changé de AUTRE à OTHER
}

// Helper pour afficher les labels en français
export const ProductTypeLabels: Record<ProductType, string> = {
    [ProductType.AUTO]: 'Auto',
    [ProductType.HEALTH]: 'Santé',
    [ProductType.HOME]: 'Habitation',
    [ProductType.LIFE]: 'Vie',
    [ProductType.OTHER]: 'Autre'
};