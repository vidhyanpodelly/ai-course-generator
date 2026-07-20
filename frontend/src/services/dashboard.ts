import apiClient from '@/lib/api-client';
import { ApiResponse, Course } from '@/types';

export interface DashboardStats {
  totalPdfsCount: number;
  totalCoursesCount: number;
  totalCompletedLessonsCount: number;
  averageQuizScore: number;
  recentCourses: Array<{
    courseId: string;
    pdfMetadataId: string;
    title: string;
    status: string;
    completionPercentage: number;
    completedLessons: number;
    totalLessons: number;
    failureReason?: string;
  }>;
}

export const dashboardService = {
  async getStats(): Promise<ApiResponse<DashboardStats>> {
    const response = await apiClient.get<ApiResponse<DashboardStats>>('/api/dashboard');
    return response.data;
  },

  async generateCourse(pdfId: string): Promise<ApiResponse<Course>> {
    const response = await apiClient.post<ApiResponse<Course>>(`/api/courses/generate?pdfId=${pdfId}`);
    return response.data;
  }
};
