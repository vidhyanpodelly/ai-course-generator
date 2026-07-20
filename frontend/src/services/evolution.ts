import apiClient from '@/lib/api-client';
import { ApiResponse } from '@/types';

export interface Flashcard {
  id: string;
  chapterId: string;
  front: string;
  back: string;
  difficulty: string;
  box: number;
  nextReview: string;
}

export interface PDFSummary {
  oneLineSummary: string;
  shortSummary: string;
  detailedSummary: string;
  executiveSummary: string;
  keyTakeaways: string[];
  chapterSummaries: { chapterTitle: string; summary: string }[];
}

export interface Certificate {
  certificateId: string;
  studentName: string;
  courseTitle: string;
  completionPercentage: number;
  dateFormatted: string;
  verificationQrUrl: string;
  verificationUrl: string;
}

export interface Bookmark {
  id: string;
  bookmarkType: string;
  targetId: string;
  title: string;
  content: string;
  createdAt: string;
}

export interface ActivityLog {
  description: string;
  type: string;
  timestamp: string;
}

export interface ProfileData {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  learningStreak: number;
  lastActiveAt: string;
  avatarUrl: string;
  achievements: string[];
  preferences: string;
  certificates: { courseId: string; courseTitle: string; certificateId: string; issueDate: string }[];
  recentActivity: ActivityLog[];
  weeklyStudyHours?: number[];
  heatmapLevels?: number[];
}

export interface GlobalSearchResponse {
  courses: any[];
  lessons: any[];
  semanticMatches: any[];
}

export const evolutionService = {
  // Global search
  async searchGlobal(q: string): Promise<ApiResponse<GlobalSearchResponse>> {
    const res = await apiClient.get<ApiResponse<GlobalSearchResponse>>(`/api/courses/search/global?q=${encodeURIComponent(q)}`);
    return res.data;
  },

  // Flashcards
  async getFlashcards(chapterId: string): Promise<ApiResponse<Flashcard[]>> {
    const res = await apiClient.get<ApiResponse<Flashcard[]>>(`/api/courses/chapters/${chapterId}/flashcards`);
    return res.data;
  },
  async getCourseFlashcards(courseId: string): Promise<ApiResponse<Flashcard[]>> {
    const res = await apiClient.get<ApiResponse<Flashcard[]>>(`/api/courses/${courseId}/flashcards`);
    return res.data;
  },
  async getDueFlashcards(): Promise<ApiResponse<Flashcard[]>> {
    const res = await apiClient.get<ApiResponse<Flashcard[]>>('/api/courses/flashcards/due');
    return res.data;
  },
  async reviewFlashcard(cardId: string, rating: 'EASY' | 'MEDIUM' | 'HARD'): Promise<ApiResponse<Flashcard>> {
    const res = await apiClient.post<ApiResponse<Flashcard>>(`/api/courses/flashcards/${cardId}/review?rating=${rating}`);
    return res.data;
  },

  // Summaries
  async getCourseSummary(courseId: string): Promise<ApiResponse<PDFSummary>> {
    const res = await apiClient.get<ApiResponse<PDFSummary>>(`/api/courses/${courseId}/summary`);
    return res.data;
  },

  // Diagrams
  async getMindMap(chapterId: string): Promise<ApiResponse<{ mermaidData: string }>> {
    const res = await apiClient.get<ApiResponse<{ mermaidData: string }>>(`/api/courses/chapters/${chapterId}/mindmap`);
    return res.data;
  },
  async getDiagram(chapterId: string, type: 'FLOWCHART' | 'SEQUENCE' | 'CLASS' | 'ERD'): Promise<ApiResponse<{ mermaidData: string }>> {
    const res = await apiClient.get<ApiResponse<{ mermaidData: string }>>(`/api/courses/chapters/${chapterId}/diagram?type=${type}`);
    return res.data;
  },

  // Certificates
  async getCertificate(courseId: string): Promise<ApiResponse<Certificate>> {
    const res = await apiClient.get<ApiResponse<Certificate>>(`/api/courses/${courseId}/certificate`);
    return res.data;
  },

  // Bookmarks
  async getBookmarks(): Promise<ApiResponse<Bookmark[]>> {
    const res = await apiClient.get<ApiResponse<Bookmark[]>>('/api/bookmarks');
    return res.data;
  },
  async addBookmark(type: string, targetId: string, title: string, content: string): Promise<ApiResponse<Bookmark>> {
    const res = await apiClient.post<ApiResponse<Bookmark>>('/api/bookmarks', { bookmarkType: type, targetId, title, content });
    return res.data;
  },
  async removeBookmark(bookmarkId: string): Promise<ApiResponse<void>> {
    const res = await apiClient.delete<ApiResponse<void>>(`/api/bookmarks/${bookmarkId}`);
    return res.data;
  },
  async removeBookmarkByTypeAndTarget(type: string, targetId: string): Promise<ApiResponse<void>> {
    const res = await apiClient.delete<ApiResponse<void>>(`/api/bookmarks?type=${type}&targetId=${targetId}`);
    return res.data;
  },

  // Profile
  async getProfile(): Promise<ApiResponse<ProfileData>> {
    const res = await apiClient.get<ApiResponse<ProfileData>>('/api/users/profile');
    return res.data;
  },
  async updatePreferences(preferences: Record<string, any>): Promise<ApiResponse<void>> {
    const res = await apiClient.put<ApiResponse<void>>('/api/users/profile/preferences', preferences);
    return res.data;
  },
  async updateAvatar(avatarUrl: string): Promise<ApiResponse<void>> {
    const res = await apiClient.put<ApiResponse<void>>(`/api/users/profile/avatar?avatarUrl=${encodeURIComponent(avatarUrl)}`);
    return res.data;
  }
};
