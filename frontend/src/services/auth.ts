import apiClient from '@/lib/api-client';
import { ApiResponse, AuthResponse, User } from '@/types';


export interface AuthRegisterPayload {
  email: string;
  passwordHash?: string; // or password
  password?: string;
  firstName: string;
  lastName: string;
}

export const authService = {
  async register(payload: AuthRegisterPayload): Promise<ApiResponse<AuthResponse>> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/api/auth/register', {
      email: payload.email,
      password: payload.password,
      firstName: payload.firstName,
      lastName: payload.lastName
    });
    return response.data;
  },

  async login(payload: any): Promise<ApiResponse<AuthResponse>> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/api/auth/login', payload);
    return response.data;
  },

  async initiateSignUp(email: string): Promise<ApiResponse<void>> {
    const response = await apiClient.post<ApiResponse<void>>(`/api/auth/register/initiate?email=${encodeURIComponent(email)}`);
    return response.data;
  },

  async registerWithOtp(payload: { email: string; otpCode: string; password?: string; firstName: string; lastName: string }): Promise<ApiResponse<AuthResponse>> {
    const response = await apiClient.post<ApiResponse<AuthResponse>>('/api/auth/register/verify', payload);
    return response.data;
  },

  async forgotPassword(email: string): Promise<ApiResponse<void>> {
    const response = await apiClient.post<ApiResponse<void>>(`/api/auth/password/forgot?email=${encodeURIComponent(email)}`);
    return response.data;
  },

  async resetPassword(payload: { email: string; otpCode: string; newPassword?: string }): Promise<ApiResponse<void>> {
    const response = await apiClient.post<ApiResponse<void>>('/api/auth/password/reset', payload);
    return response.data;
  },

  async getMe(): Promise<ApiResponse<User>> {
    const response = await apiClient.get<ApiResponse<User>>('/api/auth/me');
    return response.data;
  }
};
