import apiClient from '@/lib/api-client';
import { ApiResponse, PDFMetadata } from '@/types';

export const pdfsService = {
  async uploadPDF(file: File): Promise<ApiResponse<PDFMetadata>> {
    const formData = new FormData();
    formData.append('file', file);
    
    const response = await apiClient.post<ApiResponse<PDFMetadata>>('/api/pdf/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  async getPDFs(): Promise<ApiResponse<PDFMetadata[]>> {
    const response = await apiClient.get<ApiResponse<PDFMetadata[]>>('/api/pdf');
    return response.data;
  },

  async getPDF(id: string): Promise<ApiResponse<PDFMetadata>> {
    const response = await apiClient.get<ApiResponse<PDFMetadata>>(`/api/pdf/${id}`);
    return response.data;
  },

  async deletePDF(id: string): Promise<ApiResponse<void>> {
    const response = await apiClient.delete<ApiResponse<void>>(`/api/pdf/${id}`);
    return response.data;
  }
};
