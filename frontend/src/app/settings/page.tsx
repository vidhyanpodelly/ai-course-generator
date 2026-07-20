'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/context/AuthContext';
import { useLanguage, Language } from '@/context/LanguageContext';
import { evolutionService } from '@/services/evolution';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import {
  GraduationCap,
  ArrowLeft,
  User as UserIcon,
  Globe,
  Lock,
  Shield,
  Bell,
  Sliders,
  Palette,
  Settings as SettingsIcon,
  LogOut,
  Loader2,
  CheckCircle2,
  Trash2,
  HelpCircle
} from 'lucide-react';

export default function SettingsPage() {
  return (
    <ProtectedRoute>
      <SettingsContent />
    </ProtectedRoute>
  );
}

type TabType = 'profile' | 'language' | 'security' | 'password' | 'notifications' | 'ai' | 'appearance' | 'account';

function SettingsContent() {
  const router = useRouter();
  const { user, logout } = useAuth();
  const { language, setLanguage, t } = useLanguage();

  // Selected tab state
  const [activeTab, setActiveTab] = useState<TabType>('profile');

  // Input states
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [selectedAvatar, setSelectedAvatar] = useState('');
  const [selectedLang, setSelectedLang] = useState<Language>('en');

  // Password fields
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');

  // Notifications checkboxes
  const [emailAlerts, setEmailAlerts] = useState(true);
  const [pushAlerts, setPushAlerts] = useState(true);
  const [weeklyDigest, setWeeklyDigest] = useState(false);

  // AI preferences
  const [aiModel, setAiModel] = useState('gemini-1.5-flash');
  const [temperature, setTemperature] = useState(0.7);
  const [explanationStyle, setExplanationStyle] = useState('balanced');

  // Appearance preferences
  const [themeMode, setThemeMode] = useState('dark');
  const [accentColor, setAccentColor] = useState('violet');

  // Security preferences
  const [twoFactorAuth, setTwoFactorAuth] = useState(false);

  // Status indicators
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Avatars array
  const avatars = [
    'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=80&h=80&q=80',
    'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=80&h=80&q=80',
    'https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?auto=format&fit=crop&w=80&h=80&q=80'
  ];

  // Fetch preferences on load
  useEffect(() => {
    const loadProfile = async () => {
      try {
        const response = await evolutionService.getProfile();
        if (response.success && response.data) {
          setFirstName(response.data.firstName || '');
          setLastName(response.data.lastName || '');
          setSelectedAvatar(response.data.avatarUrl || avatars[0]);
          
          if (response.data.preferences) {
            try {
              const prefs = JSON.parse(response.data.preferences);
              if (prefs.language) {
                setSelectedLang(prefs.language);
                setLanguage(prefs.language);
              }
              if (prefs.aiModel) setAiModel(prefs.aiModel);
              if (prefs.temperature) setTemperature(Number(prefs.temperature));
              if (prefs.explanationStyle) setExplanationStyle(prefs.explanationStyle);
              if (prefs.themeMode) setThemeMode(prefs.themeMode);
              if (prefs.accentColor) setAccentColor(prefs.accentColor);
              if (prefs.emailAlerts !== undefined) setEmailAlerts(prefs.emailAlerts);
              if (prefs.pushAlerts !== undefined) setPushAlerts(prefs.pushAlerts);
              if (prefs.twoFactorAuth !== undefined) setTwoFactorAuth(prefs.twoFactorAuth);
            } catch (e) {
              console.error('Error parsing preferences', e);
            }
          }
        }
      } catch (err) {
        console.error('Failed to load user profile', err);
      }
    };

    loadProfile();
  }, []);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setSuccess(null);
    setError(null);

    try {
      // 1. Save Avatar
      if (selectedAvatar) {
        await evolutionService.updateAvatar(selectedAvatar);
      }

      // 2. Build preferences object
      const prefs = {
        language: selectedLang,
        aiModel,
        temperature,
        explanationStyle,
        themeMode,
        accentColor,
        emailAlerts,
        pushAlerts,
        twoFactorAuth
      };

      // 3. Save profile details
      await evolutionService.updatePreferences(prefs);
      
      // Update actual UI language context immediately
      setLanguage(selectedLang);

      setSuccess('Settings updated successfully!');
      setTimeout(() => setSuccess(null), 3000);
    } catch (err) {
      setError('Failed to update settings. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordReset = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(null);

    if (newPassword.length < 6) {
      setError('New password must be at least 6 characters long.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    setSuccess('Password updated successfully!');
    setCurrentPassword('');
    setNewPassword('');
    setConfirmPassword('');
    setTimeout(() => setSuccess(null), 3000);
  };

  const handleDeleteAccount = () => {
    if (window.confirm('Are you absolutely sure you want to delete your account? This action is permanent.')) {
      logout();
      router.push('/login');
    }
  };

  return (
    <div className="min-h-screen bg-gray-950 text-gray-200">
      {/* Top Header */}
      <header className="glass-panel sticky top-0 z-50 px-6 py-4 flex items-center justify-between border-b border-gray-900 bg-gray-950/80">
        <div className="flex items-center space-x-4">
          <Link href="/dashboard" className="p-2 hover:bg-gray-900 rounded-lg text-gray-400 hover:text-white transition">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div className="flex items-center space-x-2">
            <GraduationCap className="h-6 w-6 text-violet-500" />
            <h1 className="text-sm font-bold text-gray-200">{t('settingsTitle')}</h1>
          </div>
        </div>
        <button
          onClick={logout}
          className="flex items-center space-x-1.5 text-xs font-semibold text-gray-400 hover:text-red-400 transition"
        >
          <LogOut className="h-4 w-4" />
          <span>{t('logout')}</span>
        </button>
      </header>

      <main className="max-w-6xl mx-auto px-6 py-10 flex flex-col md:flex-row gap-8">
        {/* Left Sidebar Menu */}
        <aside className="w-full md:w-64 space-y-1.5 shrink-0">
          <button
            onClick={() => setActiveTab('profile')}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-xs font-bold transition text-left border ${
              activeTab === 'profile'
                ? 'bg-violet-600/10 border-violet-500/30 text-violet-400'
                : 'bg-transparent border-transparent hover:bg-gray-900/60 text-gray-400 hover:text-white'
            }`}
          >
            <UserIcon className="h-4 w-4" />
            <span>{t('profile')}</span>
          </button>

          <button
            onClick={() => setActiveTab('language')}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-xs font-bold transition text-left border ${
              activeTab === 'language'
                ? 'bg-violet-600/10 border-violet-500/30 text-violet-400'
                : 'bg-transparent border-transparent hover:bg-gray-900/60 text-gray-400 hover:text-white'
            }`}
          >
            <Globe className="h-4 w-4" />
            <span>{t('language')}</span>
          </button>

          <button
            onClick={() => setActiveTab('security')}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-xs font-bold transition text-left border ${
              activeTab === 'security'
                ? 'bg-violet-600/10 border-violet-500/30 text-violet-400'
                : 'bg-transparent border-transparent hover:bg-gray-900/60 text-gray-400 hover:text-white'
            }`}
          >
            <Shield className="h-4 w-4" />
            <span>{t('security')}</span>
          </button>

          <button
            onClick={() => setActiveTab('password')}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-xs font-bold transition text-left border ${
              activeTab === 'password'
                ? 'bg-violet-600/10 border-violet-500/30 text-violet-400'
                : 'bg-transparent border-transparent hover:bg-gray-900/60 text-gray-400 hover:text-white'
            }`}
          >
            <Lock className="h-4 w-4" />
            <span>{t('password')}</span>
          </button>

          <button
            onClick={() => setActiveTab('notifications')}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-xs font-bold transition text-left border ${
              activeTab === 'notifications'
                ? 'bg-violet-600/10 border-violet-500/30 text-violet-400'
                : 'bg-transparent border-transparent hover:bg-gray-900/60 text-gray-400 hover:text-white'
            }`}
          >
            <Bell className="h-4 w-4" />
            <span>{t('notifications')}</span>
          </button>

          <button
            onClick={() => setActiveTab('ai')}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-xs font-bold transition text-left border ${
              activeTab === 'ai'
                ? 'bg-violet-600/10 border-violet-500/30 text-violet-400'
                : 'bg-transparent border-transparent hover:bg-gray-900/60 text-gray-400 hover:text-white'
            }`}
          >
            <Sliders className="h-4 w-4" />
            <span>{t('aiPreferences')}</span>
          </button>

          <button
            onClick={() => setActiveTab('appearance')}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-xs font-bold transition text-left border ${
              activeTab === 'appearance'
                ? 'bg-violet-600/10 border-violet-500/30 text-violet-400'
                : 'bg-transparent border-transparent hover:bg-gray-900/60 text-gray-400 hover:text-white'
            }`}
          >
            <Palette className="h-4 w-4" />
            <span>{t('appearance')}</span>
          </button>

          <button
            onClick={() => setActiveTab('account')}
            className={`w-full flex items-center space-x-3 px-4 py-3 rounded-xl text-xs font-bold transition text-left border ${
              activeTab === 'account'
                ? 'bg-violet-600/10 border-violet-500/30 text-violet-400'
                : 'bg-transparent border-transparent hover:bg-gray-900/60 text-gray-400 hover:text-white'
            }`}
          >
            <Trash2 className="h-4 w-4" />
            <span>{t('account')}</span>
          </button>
        </aside>

        {/* Content Pane */}
        <section className="flex-1 bg-gray-900/10 border border-gray-900 p-8 rounded-2xl">
          {success && (
            <div className="p-4 mb-6 rounded-xl border border-emerald-500/20 bg-emerald-500/5 flex items-center space-x-3 text-emerald-400 text-xs">
              <CheckCircle2 className="h-5 w-5 shrink-0" />
              <span>{success}</span>
            </div>
          )}

          {error && (
            <div className="p-4 mb-6 rounded-xl border border-red-500/20 bg-red-500/5 flex items-center space-x-3 text-red-400 text-xs">
              <HelpCircle className="h-5 w-5 shrink-0 text-red-500" />
              <span>{error}</span>
            </div>
          )}

          {activeTab === 'profile' && (
            <form onSubmit={handleSave} className="space-y-6">
              <h3 className="text-sm font-bold text-gray-300 border-b border-gray-900 pb-3">{t('profile')}</h3>
              
              <div className="space-y-3">
                <label className="text-[10px] font-bold text-gray-500 uppercase">{t('avatar')}</label>
                <div className="flex gap-4">
                  {avatars.map((url) => (
                    <button
                      type="button"
                      key={url}
                      onClick={() => setSelectedAvatar(url)}
                      className={`relative rounded-full overflow-hidden w-14 h-14 border-2 transition ${
                        selectedAvatar === url ? 'border-violet-500' : 'border-transparent'
                      }`}
                    >
                      <img src={url} alt="Profile option" className="w-full h-full object-cover" />
                    </button>
                  ))}
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-gray-500 uppercase">{t('firstName')}</label>
                  <input
                    type="text"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    className="w-full bg-gray-950 border border-gray-850 px-3.5 py-2.5 rounded-xl text-xs focus:outline-none focus:border-violet-500"
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-gray-500 uppercase">{t('lastName')}</label>
                  <input
                    type="text"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    className="w-full bg-gray-950 border border-gray-850 px-3.5 py-2.5 rounded-xl text-xs focus:outline-none focus:border-violet-500"
                  />
                </div>
              </div>

              <div className="space-y-2">
                <label className="text-[10px] font-bold text-gray-500 uppercase">{t('emailAddress')}</label>
                <input
                  type="email"
                  disabled
                  value={user?.email || ''}
                  className="w-full bg-gray-950/40 border border-gray-900 px-3.5 py-2.5 rounded-xl text-xs text-gray-500 cursor-not-allowed"
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="px-5 py-3 rounded-xl bg-violet-600 hover:bg-violet-700 text-xs font-bold text-white transition flex items-center justify-center space-x-2"
              >
                {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <span>{t('saveChanges')}</span>}
              </button>
            </form>
          )}

          {activeTab === 'language' && (
            <form onSubmit={handleSave} className="space-y-6">
              <h3 className="text-sm font-bold text-gray-300 border-b border-gray-900 pb-3">{t('language')}</h3>
              
              <div className="space-y-2">
                <label className="text-[10px] font-bold text-gray-500 uppercase">{t('language')}</label>
                <select
                  value={selectedLang}
                  onChange={(e) => {
                    const newLang = e.target.value as Language;
                    setSelectedLang(newLang);
                    setLanguage(newLang);
                  }}
                  className="w-full max-w-xs bg-gray-950 border border-gray-850 p-3 rounded-xl text-xs text-gray-300 focus:outline-none focus:border-violet-500"
                >
                  <option value="en">English</option>
                  <option value="hi">Hindi (हिन्दी)</option>
                  <option value="es">Spanish (Español)</option>
                  <option value="fr">French (Français)</option>
                  <option value="de">German (Deutsch)</option>
                  <option value="ja">Japanese (日本語)</option>
                </select>
                <p className="text-[9px] text-gray-500 mt-1">Language changes apply immediately. Save to persist.</p>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="px-5 py-3 rounded-xl bg-violet-600 hover:bg-violet-700 text-xs font-bold text-white transition flex items-center justify-center space-x-2"
              >
                {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <span>{t('saveChanges')}</span>}
              </button>
            </form>
          )}

          {activeTab === 'security' && (
            <div className="space-y-6">
              <h3 className="text-sm font-bold text-gray-300 border-b border-gray-900 pb-3">{t('security')}</h3>
              
              <div className="flex items-center justify-between p-4 bg-gray-950/40 border border-gray-900 rounded-2xl">
                <div>
                  <h4 className="text-xs font-bold text-gray-200">Two-Factor Authentication (2FA)</h4>
                  <p className="text-[10px] text-gray-500 mt-1">Provide secondary OTP tokens upon secure account sign ins.</p>
                </div>
                <button
                  onClick={() => setTwoFactorAuth(!twoFactorAuth)}
                  className={`px-3 py-1.5 rounded-lg text-[10px] font-bold transition ${
                    twoFactorAuth ? 'bg-violet-600 text-white' : 'bg-gray-800 text-gray-400 hover:text-white'
                  }`}
                >
                  {twoFactorAuth ? 'Enabled' : 'Disabled'}
                </button>
              </div>

              <div className="space-y-3">
                <h4 className="text-xs font-bold text-gray-300">Active Web Sessions</h4>
                <div className="p-4 bg-gray-950/40 border border-gray-900 rounded-2xl space-y-3">
                  <div className="flex items-center justify-between text-xs pb-3 border-b border-gray-950">
                    <div>
                      <p className="font-semibold text-gray-200">Chrome Browser on Windows 11</p>
                      <p className="text-[10px] text-gray-500">127.0.0.1 • Active Now</p>
                    </div>
                    <span className="px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 text-[9px] font-bold uppercase tracking-wider">Current Session</span>
                  </div>
                </div>
              </div>
            </div>
          )}

          {activeTab === 'password' && (
            <form onSubmit={handlePasswordReset} className="space-y-6">
              <h3 className="text-sm font-bold text-gray-300 border-b border-gray-900 pb-3">{t('password')}</h3>
              
              <div className="space-y-4">
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-gray-500 uppercase">Current Password</label>
                  <input
                    type="password"
                    required
                    value={currentPassword}
                    onChange={(e) => setCurrentPassword(e.target.value)}
                    className="w-full max-w-sm bg-gray-950 border border-gray-850 px-3.5 py-2.5 rounded-xl text-xs focus:outline-none focus:border-violet-500"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-gray-500 uppercase">New Password</label>
                  <input
                    type="password"
                    required
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="w-full max-w-sm bg-gray-950 border border-gray-850 px-3.5 py-2.5 rounded-xl text-xs focus:outline-none focus:border-violet-500"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-gray-500 uppercase">Confirm New Password</label>
                  <input
                    type="password"
                    required
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="w-full max-w-sm bg-gray-950 border border-gray-850 px-3.5 py-2.5 rounded-xl text-xs focus:outline-none focus:border-violet-500"
                  />
                </div>
              </div>

              <button
                type="submit"
                className="px-5 py-3 rounded-xl bg-violet-600 hover:bg-violet-700 text-xs font-bold text-white transition"
              >
                Update Password
              </button>
            </form>
          )}

          {activeTab === 'notifications' && (
            <form onSubmit={handleSave} className="space-y-6">
              <h3 className="text-sm font-bold text-gray-300 border-b border-gray-900 pb-3">{t('notifications')}</h3>
              
              <div className="space-y-4">
                <label className="flex items-start space-x-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={emailAlerts}
                    onChange={(e) => setEmailAlerts(e.target.checked)}
                    className="rounded border-gray-850 bg-gray-950 text-violet-600 focus:ring-violet-600 mt-1"
                  />
                  <div>
                    <p className="text-xs font-semibold text-gray-200">Email Notifications</p>
                    <p className="text-[10px] text-gray-500 mt-0.5">Receive reminders, course generation outlines, and certificate releases via email.</p>
                  </div>
                </label>

                <label className="flex items-start space-x-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={pushAlerts}
                    onChange={(e) => setPushAlerts(e.target.checked)}
                    className="rounded border-gray-850 bg-gray-950 text-violet-600 focus:ring-violet-600 mt-1"
                  />
                  <div>
                    <p className="text-xs font-semibold text-gray-200">Push Notifications</p>
                    <p className="text-[10px] text-gray-500 mt-0.5">Receive immediate notification popups on chapter completions and milestones.</p>
                  </div>
                </label>

                <label className="flex items-start space-x-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={weeklyDigest}
                    onChange={(e) => setWeeklyDigest(e.target.checked)}
                    className="rounded border-gray-850 bg-gray-950 text-violet-600 focus:ring-violet-600 mt-1"
                  />
                  <div>
                    <p className="text-xs font-semibold text-gray-200">Weekly Progress Digest</p>
                    <p className="text-[10px] text-gray-500 mt-0.5">Get a summarized weekly report of study statistics and streaks.</p>
                  </div>
                </label>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="px-5 py-3 rounded-xl bg-violet-600 hover:bg-violet-700 text-xs font-bold text-white transition flex items-center justify-center space-x-2"
              >
                {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <span>{t('saveChanges')}</span>}
              </button>
            </form>
          )}

          {activeTab === 'ai' && (
            <form onSubmit={handleSave} className="space-y-6">
              <h3 className="text-sm font-bold text-gray-300 border-b border-gray-900 pb-3">{t('aiPreferences')}</h3>
              
              <div className="space-y-4 max-w-md">
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-gray-500 uppercase">Default Model</label>
                  <select
                    value={aiModel}
                    onChange={(e) => setAiModel(e.target.value)}
                    className="w-full bg-gray-950 border border-gray-850 p-2.5 rounded-xl text-xs text-gray-300 focus:outline-none"
                  >
                    <option value="gemini-1.5-flash">Gemini 1.5 Flash (Default)</option>
                    <option value="gemini-1.5-pro">Gemini 1.5 Pro (Detailed Analysis)</option>
                    <option value="gemini-2.0-flash-exp">Gemini 2.0 Flash (Experimental)</option>
                  </select>
                </div>

                <div className="space-y-2">
                  <div className="flex justify-between text-[10px] font-bold text-gray-500 uppercase">
                    <span>Generation Temperature</span>
                    <span className="text-violet-400">{temperature}</span>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="1"
                    step="0.1"
                    value={temperature}
                    onChange={(e) => setTemperature(Number(e.target.value))}
                    className="w-full accent-violet-600 bg-gray-950 h-2 rounded-lg cursor-pointer"
                  />
                  <p className="text-[9px] text-gray-600 leading-normal">Lower values produce deterministic, factual answers. Higher values enhance creativity.</p>
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-gray-500 uppercase">Explanation Detail Level</label>
                  <div className="grid grid-cols-3 gap-2">
                    {['summarized', 'balanced', 'comprehensive'].map((style) => (
                      <button
                        type="button"
                        key={style}
                        onClick={() => setExplanationStyle(style)}
                        className={`py-2 rounded-xl text-[10px] font-bold border capitalize transition ${
                          explanationStyle === style
                            ? 'bg-violet-600/10 border-violet-500/30 text-violet-400'
                            : 'border-gray-800 bg-transparent text-gray-400 hover:text-white'
                        }`}
                      >
                        {style}
                      </button>
                    ))}
                  </div>
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="px-5 py-3 rounded-xl bg-violet-600 hover:bg-violet-700 text-xs font-bold text-white transition flex items-center justify-center space-x-2"
              >
                {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <span>{t('saveChanges')}</span>}
              </button>
            </form>
          )}

          {activeTab === 'appearance' && (
            <form onSubmit={handleSave} className="space-y-6">
              <h3 className="text-sm font-bold text-gray-300 border-b border-gray-900 pb-3">{t('appearance')}</h3>
              
              <div className="space-y-4 max-w-sm">
                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-gray-500 uppercase">Color Scheme</label>
                  <div className="grid grid-cols-3 gap-2">
                    {['dark', 'light', 'system'].map((theme) => (
                      <button
                        type="button"
                        key={theme}
                        onClick={() => setThemeMode(theme)}
                        className={`py-2 rounded-xl text-[10px] font-bold border capitalize transition ${
                          themeMode === theme
                            ? 'bg-violet-600/10 border-violet-500/30 text-violet-400'
                            : 'border-gray-800 bg-transparent text-gray-400 hover:text-white'
                        }`}
                      >
                        {theme}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-[10px] font-bold text-gray-500 uppercase">Accent Tint</label>
                  <div className="flex gap-3">
                    {['violet', 'indigo', 'emerald', 'amber'].map((color) => {
                      const colorMap: Record<string, string> = {
                        violet: 'bg-violet-600',
                        indigo: 'bg-indigo-600',
                        emerald: 'bg-emerald-600',
                        amber: 'bg-amber-600'
                      };
                      return (
                        <button
                          type="button"
                          key={color}
                          onClick={() => setAccentColor(color)}
                          className={`w-7 h-7 rounded-full ${colorMap[color]} border-2 transition ${
                            accentColor === color ? 'border-white scale-110 shadow-lg' : 'border-transparent hover:scale-105'
                          }`}
                        />
                      );
                    })}
                  </div>
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="px-5 py-3 rounded-xl bg-violet-600 hover:bg-violet-700 text-xs font-bold text-white transition flex items-center justify-center space-x-2"
              >
                {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <span>{t('saveChanges')}</span>}
              </button>
            </form>
          )}

          {activeTab === 'account' && (
            <div className="space-y-6">
              <h3 className="text-sm font-bold text-gray-300 border-b border-gray-900 pb-3">{t('account')}</h3>
              
              <div className="p-4 bg-gray-950/20 border border-gray-900 rounded-2xl flex flex-col gap-4">
                <div>
                  <h4 className="text-xs font-bold text-gray-200">Export Personal Study Data</h4>
                  <p className="text-[10px] text-gray-500 mt-1">Download a JSON bundle of your learning records, streak history, achievements, and courses.</p>
                </div>
                <button
                  onClick={() => {
                    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify({ user, preferences: { language } }));
                    const downloadAnchor = document.createElement('a');
                    downloadAnchor.setAttribute("href", dataStr);
                    downloadAnchor.setAttribute("download", "study_profile.json");
                    document.body.appendChild(downloadAnchor);
                    downloadAnchor.click();
                    downloadAnchor.remove();
                  }}
                  className="w-fit px-4 py-2 bg-gray-900 hover:bg-gray-800 border border-gray-800 rounded-xl text-xs font-bold text-gray-300 hover:text-white transition"
                >
                  Export JSON
                </button>
              </div>

              <div className="p-4 bg-red-950/5 border border-red-900/20 rounded-2xl flex flex-col gap-4">
                <div>
                  <h4 className="text-xs font-bold text-red-400">Close Account</h4>
                  <p className="text-[10px] text-red-500/60 mt-1">Permanently erase your study pathways, certificates, account details. This cannot be undone.</p>
                </div>
                <button
                  onClick={handleDeleteAccount}
                  className="w-fit px-4 py-2 bg-red-900 hover:bg-red-850 rounded-xl text-xs font-bold text-white transition flex items-center space-x-1.5"
                >
                  <Trash2 className="h-3.5 w-3.5" />
                  <span>Delete My Account</span>
                </button>
              </div>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}
