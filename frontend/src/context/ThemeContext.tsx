'use client';

import React, { createContext, useContext, useState, useEffect } from 'react';
import { profileService } from '@/services/profile';

type ThemeType = 'light' | 'dark' | 'system';
type PrimaryColorType = 'violet' | 'indigo' | 'emerald' | 'amber';

interface ThemePreferences {
  theme: ThemeType;
  primaryColor: PrimaryColorType;
}

interface ThemeContextType {
  preferences: ThemePreferences;
  updatePreferences: (prefs: Partial<ThemePreferences>) => Promise<void>;
  isLoaded: boolean;
}

const defaultPreferences: ThemePreferences = {
  theme: 'dark',
  primaryColor: 'violet',
};

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [preferences, setPreferences] = useState<ThemePreferences>(defaultPreferences);
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    const stored = localStorage.getItem('theme_preferences');
    if (stored) {
      try {
        const parsed = JSON.parse(stored);
        setPreferences({ ...defaultPreferences, ...parsed });
      } catch (e) {
        // Ignore
      }
    }
    setIsLoaded(true);
  }, []);

  useEffect(() => {
    if (!isLoaded) return;
    
    const root = document.documentElement;
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    
    const applyTheme = () => {
      let activeTheme = preferences.theme;
      if (activeTheme === 'system') {
        activeTheme = mediaQuery.matches ? 'dark' : 'light';
      }
      
      if (activeTheme === 'light') {
        root.classList.remove('dark');
        root.classList.add('light');
      } else {
        root.classList.remove('light');
        root.classList.add('dark');
      }
    };

    applyTheme();
    
    if (preferences.theme === 'system') {
      mediaQuery.addEventListener('change', applyTheme);
    }
    
    // Apply Primary Color as data attribute for CSS targeting
    root.setAttribute('data-primary-color', preferences.primaryColor);

    localStorage.setItem('theme_preferences', JSON.stringify(preferences));
    
    return () => {
      mediaQuery.removeEventListener('change', applyTheme);
    };

  }, [preferences, isLoaded]);

  const updatePreferences = async (newPrefs: Partial<ThemePreferences>) => {
    const updated = { ...preferences, ...newPrefs };
    setPreferences(updated);
    
    try {
      await profileService.updatePreferences(updated);
    } catch (e) {
      console.error('Failed to sync preferences with backend', e);
    }
  };

  return (
    <ThemeContext.Provider value={{ preferences, updatePreferences, isLoaded }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
}
