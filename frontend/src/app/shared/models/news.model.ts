// src/app/shared/models/news.model.ts

export enum NewsStatus {
    DRAFT = 'DRAFT',
    PUBLISHED = 'PUBLISHED',
    ARCHIVED = 'ARCHIVED'
}

export interface News {
    newsId: number;
    title: string;
    content: string;
    summary: string;
    author: string;
    category: string;
    imageUrl: string;
    viewCount: number;
    publishDate: string;
    status: NewsStatus;
    createdAt: string;
    updatedAt: string;
}

export interface CreateNews {
    title: string;
    content: string;
    summary: string;
    author: string;
    category: string;
    status: NewsStatus;
}

export interface UpdateNews {
    title?: string;
    content?: string;
    summary?: string;
    author?: string;
    category?: string;
    status?: NewsStatus;
}