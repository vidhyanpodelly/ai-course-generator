import apiClient from '@/lib/api-client';
import { ApiResponse, CourseProgress } from '@/types';

export const progressService = {
  async markComplete(lessonId: string, completed: boolean): Promise<ApiResponse<CourseProgress>> {
    const response = await apiClient.post<ApiResponse<CourseProgress>>(
      `/api/progress/complete?lessonId=${lessonId}&completed=${completed}`
    );
    return response.data;
  },

  async getCourseProgress(courseId: string): Promise<ApiResponse<CourseProgress>> {
    const response = await apiClient.get<ApiResponse<CourseProgress>>(`/api/progress/${courseId}`);
    return response.data;
  }
};
