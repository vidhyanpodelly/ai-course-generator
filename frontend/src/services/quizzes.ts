import apiClient from '@/lib/api-client';
import { ApiResponse, QuizQuestion, QuizAttempt } from '@/types';

export const quizzesService = {
  async getQuizQuestions(chapterId: string): Promise<ApiResponse<QuizQuestion[]>> {
    const response = await apiClient.get<ApiResponse<QuizQuestion[]>>(`/api/quiz/${chapterId}`);
    return response.data;
  },

  async submitQuiz(chapterId: string, answers: Record<string, string>): Promise<ApiResponse<QuizAttempt>> {
    const response = await apiClient.post<ApiResponse<QuizAttempt>>(`/api/quiz/${chapterId}/submit`, {
      answers
    });
    return response.data;
  }
};
