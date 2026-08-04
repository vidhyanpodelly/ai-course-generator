import apiClient from '@/lib/api-client';
import { ApiResponse } from '@/types';

export const profileService = {
  async updatePreferences(preferences: Record<string, any>): Promise<ApiResponse<void>> {
    const response = await apiClient.put<ApiResponse<void>>('/api/users/profile/preferences', preferences);
    return response.data;
  }
};
