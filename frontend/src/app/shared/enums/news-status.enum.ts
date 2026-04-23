// src/app/Features/News/enums/news-status.enum.ts

export enum NewsStatus {
    DRAFT = 'DRAFT',
    PUBLISHED = 'PUBLISHED',
    ARCHIVED = 'ARCHIVED'
}

export const NewsStatusLabels: Record<NewsStatus, string> = {
    [NewsStatus.DRAFT]: '📝 Brouillon',
    [NewsStatus.PUBLISHED]: '✅ Publié',
    [NewsStatus.ARCHIVED]: '📦 Archivé'
};

export const NewsStatusColors: Record<NewsStatus, string> = {
    [NewsStatus.DRAFT]: 'status-draft',
    [NewsStatus.PUBLISHED]: 'status-published',
    [NewsStatus.ARCHIVED]: 'status-archived'
};