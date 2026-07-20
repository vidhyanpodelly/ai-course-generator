import apiClient from '@/lib/api-client';
import { ApiResponse, ChatMessage, ChatSession } from '@/types';

export const chatService = {
  async getSessions(courseId: string): Promise<ApiResponse<ChatSession[]>> {
    const response = await apiClient.get<ApiResponse<ChatSession[]>>(`/api/chat/sessions/${courseId}`);
    return response.data;
  },

  async createSession(courseId: string, title: string): Promise<ApiResponse<ChatSession>> {
    const response = await apiClient.post<ApiResponse<ChatSession>>(`/api/chat/sessions/${courseId}`, { title });
    return response.data;
  },

  async renameSession(sessionId: string, title: string): Promise<ApiResponse<ChatSession>> {
    const response = await apiClient.put<ApiResponse<ChatSession>>(`/api/chat/sessions/${sessionId}`, { title });
    return response.data;
  },

  async deleteSession(sessionId: string): Promise<ApiResponse<void>> {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/chat/sessions/${sessionId}`);
    return response.data;
  },

  async getChatHistory(sessionId: string): Promise<ApiResponse<ChatMessage[]>> {
    const response = await apiClient.get<ApiResponse<ChatMessage[]>>(`/api/chat/sessions/${sessionId}/messages`);
    return response.data;
  },

  async sendMessage(sessionId: string, message: string): Promise<ApiResponse<ChatMessage>> {
    const response = await apiClient.post<ApiResponse<ChatMessage>>(`/api/chat/sessions/${sessionId}/stream`, {
      message
    });
    return response.data;
  }
};
