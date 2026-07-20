import { useState, useEffect, createContext, useContext } from 'react';
import { authService, AuthRegisterPayload } from '@/services/auth';
import { User } from '@/types';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  isAuthenticated: boolean;
  login: (payload: any) => Promise<void>;
  register: (payload: AuthRegisterPayload) => Promise<void>;
  logout: () => void;
}

export const useAuthInternal = () => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  const fetchUser = async () => {
    try {
      const token = localStorage.getItem('auth_token');
      if (token) {
        const response = await authService.getMe();
        if (response.success) {
          setUser(response.data);
        } else {
          logout();
        }
      }
    } catch (err) {
      logout();
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUser();
  }, []);

  const login = async (payload: any) => {
    setLoading(true);
    try {
      const response = await authService.login(payload);
      if (response.success && response.data) {
        localStorage.setItem('auth_token', response.data.token);
        localStorage.setItem('user_details', JSON.stringify(response.data));
        setUser({
          id: response.data.userId,
          email: response.data.email,
          firstName: response.data.firstName,
          lastName: response.data.lastName
        });
      }
    } catch (err) {
      setLoading(false);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const register = async (payload: AuthRegisterPayload) => {
    setLoading(true);
    try {
      const response = await authService.register(payload);
      if (response.success && response.data) {
        localStorage.setItem('auth_token', response.data.token);
        localStorage.setItem('user_details', JSON.stringify(response.data));
        setUser({
          id: response.data.userId,
          email: response.data.email,
          firstName: response.data.firstName,
          lastName: response.data.lastName
        });
      }
    } catch (err) {
      setLoading(false);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('auth_token');
    localStorage.removeItem('user_details');
    setUser(null);
    setLoading(false);
  };

  return {
    user,
    loading,
    isAuthenticated: !!user,
    login,
    register,
    logout
  };
};
