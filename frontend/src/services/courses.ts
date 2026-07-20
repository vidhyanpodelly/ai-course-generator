import apiClient from '@/lib/api-client';
import { ApiResponse, Course, Chapter, Lesson } from '@/types';

export const coursesService = {
  async getCourses(): Promise<ApiResponse<Course[]>> {
    const response = await apiClient.get<ApiResponse<Course[]>>('/api/courses');
    return response.data;
  },

  async getCourse(id: string): Promise<ApiResponse<Course>> {
    const response = await apiClient.get<ApiResponse<Course>>(`/api/courses/${id}`);
    return response.data;
  },

  async getCourseChapters(courseId: string): Promise<ApiResponse<Chapter[]>> {
    const response = await apiClient.get<ApiResponse<Chapter[]>>(`/api/courses/${courseId}/chapters`);
    return response.data;
  },

  async getLesson(lessonId: string): Promise<ApiResponse<Lesson>> {
    const response = await apiClient.get<ApiResponse<Lesson>>(`/api/courses/lessons/${lessonId}`);
    return response.data;
  },

  async retryCourse(courseId: string): Promise<ApiResponse<Course>> {
    const response = await apiClient.post<ApiResponse<Course>>(`/api/courses/${courseId}/retry`);
    return response.data;
  },

  async searchCourses(query: string): Promise<ApiResponse<Course[]>> {
    const response = await apiClient.get<ApiResponse<Course[]>>(`/api/courses/search?q=${encodeURIComponent(query)}`);
    return response.data;
  }
};
