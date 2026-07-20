'use client';

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import { useAuth } from '@/context/AuthContext';
import { useLanguage } from '@/context/LanguageContext';
import { pdfsService } from '@/services/pdfs';
import { dashboardService, DashboardStats } from '@/services/dashboard';
import { coursesService } from '@/services/courses';
import { evolutionService, ProfileData, GlobalSearchResponse } from '@/services/evolution';
import { motion, AnimatePresence } from 'framer-motion';
import {
  FileText,
  BookOpen,
  CheckSquare,
  Award,
  Upload,
  Search,
  ChevronRight,
  LogOut,
  RefreshCw,
  AlertTriangle,
  GraduationCap,
  Plus,
  Trash2,
  Flame,
  Clock,
  Calendar,
  Grid,
  Bell,
  Sparkles,
  Compass,
  Sliders,
  Check,
  User as UserIcon
} from 'lucide-react';

export default function DashboardPage() {
  return (
    <ProtectedRoute>
      <DashboardContent />
    </ProtectedRoute>
  );
}

function DashboardContent() {
  const { user, logout } = useAuth();
  const { t, setLanguage } = useLanguage();
  // Helper to use translation with fallback to hardcoded text
  const _ = (key: string, fallback: string) => {
    const translated = t(key);
    return translated !== key ? translated : fallback;
  };
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [pdfs, setPdfs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // Global search states
  const [searchQuery, setSearchQuery] = useState('');
  const [searching, setSearching] = useState(false);
  const [searchResults, setSearchResults] = useState<GlobalSearchResponse | null>(null);

  // Profile and Streak statistics
  const [profile, setProfile] = useState<ProfileData | null>(null);
  const [selectedAvatar, setSelectedAvatar] = useState('https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=80&h=80&q=80');
  const [selectedLang, setSelectedLang] = useState('en');

  // System Notifications
  const [notifications, setNotifications] = useState<any[]>([
    { id: '1', title: 'Course compilation complete', message: 'Syllabus for astrophysics is ready.', read: false },
    { id: '2', title: 'Quiz Graded', message: 'You scored 100% on Chapter 1 Quiz.', read: false }
  ]);
  const [showNotifications, setShowNotifications] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const fetchDashboardData = async () => {
    try {
      const [statsRes, pdfsRes, profileRes] = await Promise.all([
        dashboardService.getStats(),
        pdfsService.getPDFs(),
        evolutionService.getProfile()
      ]);

      if (statsRes.success) setStats(statsRes.data);
      if (pdfsRes.success) setPdfs(pdfsRes.data);
      if (profileRes.success) {
        setProfile(profileRes.data);
        if (profileRes.data.avatarUrl) setSelectedAvatar(profileRes.data.avatarUrl);
        if (profileRes.data.preferences) {
          try {
            const prefs = JSON.parse(profileRes.data.preferences);
            if (prefs.language) {
              setSelectedLang(prefs.language);
              setLanguage(prefs.language);
            }
          } catch (e) {}
        }
      }
    } catch (err: any) {
      setError('Failed to fetch dashboard content. Please check database connectivity.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
    // Poll stats occasionally to update generation status
    const interval = setInterval(fetchDashboardData, 8000);
    return () => clearInterval(interval);
  }, []);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;
    
    setUploading(true);
    setError(null);
    try {
      const response = await pdfsService.uploadPDF(files[0]);
      if (response.success) {
        // Trigger background embedding sync notice
        setNotifications(prev => [
          { id: Date.now().toString(), title: 'Uploading document', message: `Indexed text segments for ${files[0].name}.`, read: false },
          ...prev
        ]);
        await fetchDashboardData();
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'File upload failed. Ensure the upload folders are configured.');
    } finally {
      setUploading(false);
    }
  };

  const handleCreateCourse = async (pdfId: string) => {
    try {
      setError(null);
      await dashboardService.generateCourse(pdfId);
      await fetchDashboardData();
      setNotifications(prev => [
        { id: Date.now().toString(), title: 'Building Course', message: 'Syllabus compilation initiated in background.', read: false },
        ...prev
      ]);
    } catch (err: any) {
      setError('Course outline generation failed to start.');
    }
  };

  const handleRetryCourse = async (courseId: string) => {
    try {
      setError(null);
      await coursesService.retryCourse(courseId);
      await fetchDashboardData();
    } catch (err: any) {
      setError('Failed to restart course outline generation.');
    }
  };

  const handleDeletePDF = async (pdfId: string) => {
    if (!confirm('Are you sure you want to delete this PDF? This will also delete any generated course.')) {
      return;
    }
    try {
      setError(null);
      const response = await pdfsService.deletePDF(pdfId);
      if (response.success) {
        await fetchDashboardData();
      } else {
        setError(response.message || 'Failed to delete PDF.');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete PDF.');
    }
  };

  // Global search trigger
  const handleGlobalSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) {
      setSearchResults(null);
      return;
    }

    setSearching(true);
    try {
      const response = await evolutionService.searchGlobal(searchQuery);
      if (response.success) {
        setSearchResults(response.data);
      }
    } catch (err) {
      setError('Global similarity search failed.');
    } finally {
      setSearching(false);
    }
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    const files = e.dataTransfer.files;
    if (!files || files.length === 0) return;

    setUploading(true);
    try {
      await pdfsService.uploadPDF(files[0]);
      await fetchDashboardData();
    } catch (err: any) {
      setError(err.response?.data?.message || 'File upload failed.');
    } finally {
      setUploading(false);
    }
  };

  const markAllNotificationsRead = () => {
    setNotifications(prev => prev.map(n => ({ ...n, read: true })));
  };

  // Heatmap helper representation
  const heatmapLevels = profile?.heatmapLevels || [];
  const heatmapDays = heatmapLevels.map((level, i) => ({ day: i + 1, level }));
  if (heatmapDays.length === 0) {
    for (let i = 0; i < 28; i++) {
      heatmapDays.push({ day: i + 1, level: 0 });
    }
  }

  return (
    <div className="min-h-screen bg-gray-950 pb-20 text-gray-200">
      {/* Header */}
      <header className="glass-panel sticky top-0 z-50 px-6 py-4 flex items-center justify-between border-b border-gray-900 bg-gray-950/80">
        <Link href="/" className="flex items-center space-x-2 text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-violet-400 to-indigo-200">
          <GraduationCap className="h-6 w-6 text-violet-500 animate-pulse" />
          <span>Curricula.AI</span>
        </Link>
        
        <div className="flex items-center space-x-6">
          {/* Notification Button */}
          <div className="relative">
            <button
              onClick={() => { setShowNotifications(!showNotifications); }}
              className="relative p-2 text-gray-400 hover:text-white hover:bg-gray-900 rounded-lg transition"
            >
              <Bell className="h-4.5 w-4.5" />
              {notifications.some(n => !n.read) && (
                <span className="absolute top-1.5 right-1.5 h-2 w-2 rounded-full bg-violet-500 animate-ping"></span>
              )}
            </button>
            
            {/* Notifications Dropdown Panel */}
            <AnimatePresence>
              {showNotifications && (
                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: 10 }}
                  className="absolute right-0 mt-3 w-80 glass-panel border border-gray-900 bg-gray-950 p-4 rounded-2xl shadow-2xl z-50 space-y-4"
                >
                  <div className="flex items-center justify-between border-b border-gray-900/60 pb-2">
                    <h4 className="text-xs font-bold text-gray-200">Notifications Center</h4>
                    <button onClick={markAllNotificationsRead} className="text-[10px] text-violet-400 hover:underline">
                      Mark read
                    </button>
                  </div>
                  <div className="space-y-3 max-h-60 overflow-y-auto">
                    {notifications.map((n) => (
                      <div key={n.id} className="text-xs space-y-0.5 border-b border-gray-950 pb-2">
                        <p className={`font-bold ${n.read ? 'text-gray-400' : 'text-violet-300'}`}>{n.title}</p>
                        <p className="text-[10px] text-gray-500">{n.message}</p>
                      </div>
                    ))}
                  </div>
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          <Link
            href="/settings"
            className="flex items-center space-x-2 text-sm text-gray-300 font-medium hover:text-white"
          >
            <img src={selectedAvatar} alt="Avatar" className="w-7 h-7 rounded-full border border-violet-500/20" />
            <span className="text-violet-400 font-bold hidden sm:inline">{user?.firstName}</span>
          </Link>
          
          <button
            onClick={logout}
            className="flex items-center space-x-1.5 text-xs font-semibold text-gray-400 hover:text-red-400 transition"
          >
            <LogOut className="h-4 w-4" />
            <span className="hidden sm:inline">{t('logout')}</span>
          </button>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 pt-10 space-y-12">
        {error && (
          <div className="p-4 rounded-xl border border-red-500/20 bg-red-500/5 flex items-start space-x-3 text-red-400 text-sm">
            <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        {/* Global Search Bar */}
        <form onSubmit={handleGlobalSearch} className="flex gap-4 max-w-2xl mx-auto w-full">
          <div className="relative flex-1">
            <Search className="absolute inset-y-0 left-0 pl-3.5 flex items-center text-gray-500 h-full w-5" />
            <input
              type="text"
              placeholder={_('searchPlaceholder', 'Search concepts, lessons, summaries, or document meaning...')}
              value={searchQuery}
              onChange={(e) => {
                setSearchQuery(e.target.value);
                if (!e.target.value.trim()) setSearchResults(null);
              }}
              className="w-full pl-11 pr-4 py-3 rounded-xl border border-gray-800 bg-gray-900/30 text-gray-100 placeholder-gray-500 focus:outline-none focus:border-violet-500 transition"
            />
          </div>
          <button
            type="submit"
            disabled={searching}
            className="glow-button px-6 rounded-xl bg-violet-600 hover:bg-violet-700 text-sm font-semibold transition flex items-center space-x-2 text-white"
          >
            {searching ? <RefreshCw className="h-4 w-4 animate-spin" /> :                <span>{_('search', 'Search')}</span>}
          </button>
        </form>

        {/* Search Results Display Area */}
        {searchResults !== null && (
          <div className="space-y-6 max-w-4xl mx-auto">
            <div className="flex items-center justify-between border-b border-gray-900 pb-2">
              <h2 className="text-base font-extrabold text-gray-200">Global Search Results</h2>
              <button onClick={() => { setSearchResults(null); setSearchQuery(''); }} className="text-xs text-violet-400 hover:underline">
                Clear search
              </button>
            </div>
            
            <div className="space-y-6">
              {/* Courses Matches */}
              {searchResults.courses.length > 0 && (
                <div className="space-y-3">
                  <h3 className="text-xs font-bold text-violet-300 uppercase tracking-wider">Courses Matched</h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {searchResults.courses.map((c) => (
                      <div key={c.id} className="glass-card p-4 rounded-xl border border-gray-900/60 flex justify-between items-center">
                        <div>
                          <p className="font-bold text-gray-200 text-sm">{c.title}</p>
                          <p className="text-[10px] text-gray-500 line-clamp-1">{c.description}</p>
                        </div>
                        <Link href={`/courses/${c.id}`} className="p-1.5 bg-violet-600/10 text-violet-400 rounded hover:bg-violet-600 hover:text-white transition">
                          <ChevronRight className="h-4 w-4" />
                        </Link>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Lessons Matches */}
              {searchResults.lessons.length > 0 && (
                <div className="space-y-3">
                  <h3 className="text-xs font-bold text-violet-300 uppercase tracking-wider">Lessons Matched</h3>
                  <div className="space-y-2">
                    {searchResults.lessons.map((l) => (
                      <div key={l.lessonId} className="glass-card p-4 rounded-xl border border-gray-900/60 flex justify-between items-center gap-4">
                        <div className="min-w-0 flex-1">
                          <p className="font-bold text-gray-200 text-xs">{l.lessonTitle}</p>
                          <p className="text-[9px] text-violet-400 font-semibold">{l.courseTitle} &bull; {l.chapterTitle}</p>
                          <p className="text-[10px] text-gray-500 italic mt-1 truncate">{l.snippet}</p>
                        </div>
                        <Link href={`/courses/${l.courseId}/lesson/${l.lessonId}`} className="shrink-0 flex items-center space-x-1 px-3 py-1.5 rounded-lg bg-gray-900 text-[10px] font-bold text-gray-400 hover:text-white border border-gray-800 transition">
                          <span>Study</span>
                          <ChevronRight className="h-3 w-3" />
                        </Link>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Semantic Document matches */}
              {searchResults.semanticMatches.length > 0 && (
                <div className="space-y-3">
                  <h3 className="text-xs font-bold text-indigo-300 uppercase tracking-wider">Semantic Document Matches (Meaning-based)</h3>
                  <div className="space-y-2">
                    {searchResults.semanticMatches.map((item, idx) => (
                      <div key={idx} className="glass-card p-4 rounded-xl border border-indigo-500/10 bg-indigo-950/5 space-y-2">
                        <div className="flex justify-between items-center text-[10px] text-indigo-400 font-bold">
                          <span>{item.courseTitle}</span>
                          <span>Text Segment Chunk #{item.chunkIndex}</span>
                        </div>
                        <p className="text-[10px] text-gray-400 leading-relaxed italic">
                          &ldquo;{item.content}&rdquo;
                        </p>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {searchResults.courses.length === 0 && searchResults.lessons.length === 0 && searchResults.semanticMatches.length === 0 && (
                <div className="p-8 text-center text-xs text-gray-500 glass-card rounded-2xl">
                  No courses, lessons, or document concepts matched your query.
                </div>
              )}
            </div>
          </div>
        )}

        {/* Stats Panels */}
        <section className="grid grid-cols-2 lg:grid-cols-4 gap-6">
          <div className="glass-card p-6 rounded-2xl flex items-center space-x-4">
            <div className="p-3 bg-violet-500/10 border border-violet-500/20 rounded-xl">
              <FileText className="h-6 w-6 text-violet-400" />
            </div>
            <div>
              <p className="text-[10px] text-gray-400 uppercase font-semibold">{_('documents', 'Documents')}</p>
              <h3 className="text-2xl font-bold">{stats?.totalPdfsCount ?? 0}</h3>
            </div>
          </div>

          <div className="glass-card p-6 rounded-2xl flex items-center space-x-4">
            <div className="p-3 bg-indigo-500/10 border border-indigo-500/20 rounded-xl">
              <BookOpen className="h-6 w-6 text-indigo-400" />
            </div>
            <div>
              <p className="text-[10px] text-gray-400 uppercase font-semibold">{_('courses', 'Courses')}</p>
              <h3 className="text-2xl font-bold">{stats?.totalCoursesCount ?? 0}</h3>
            </div>
          </div>

          <div className="glass-card p-6 rounded-2xl flex items-center space-x-4">
            <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl">
              <CheckSquare className="h-6 w-6 text-emerald-400" />
            </div>
            <div>
              <p className="text-[10px] text-gray-400 uppercase font-semibold">{_('completions', 'Completions')}</p>
              <h3 className="text-2xl font-bold">{stats?.totalCompletedLessonsCount ?? 0}</h3>
            </div>
          </div>

          <div className="glass-card p-6 rounded-2xl flex items-center space-x-4">
            <div className="p-3 bg-amber-500/10 border border-amber-500/20 rounded-xl">
              <Award className="h-6 w-6 text-amber-400" />
            </div>
            <div>
              <p className="text-[10px] text-gray-400 uppercase font-semibold">{_('avgQuizGrade', 'Avg Quiz Grade')}</p>
              <h3 className="text-2xl font-bold">{stats?.averageQuizScore ?? 0}%</h3>
            </div>
          </div>
        </section>

        {/* Study Statistics, Streak Heatmap & Weekly Graph split */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Weekly Learning & Heatmap Grid */}
          <div className="lg:col-span-2 glass-panel p-6 rounded-2xl border border-gray-900 space-y-6">
            <div className="flex items-center justify-between border-b border-gray-900/60 pb-3">
              <div className="flex items-center space-x-2">
                <Clock className="h-4.5 w-4.5 text-violet-400" />
                <h3 className="font-bold text-sm text-gray-200">{_('learningStats', 'Study Habits Tracker')}</h3>
              </div>
              <span className="text-[10px] text-emerald-400 font-bold bg-emerald-500/5 px-2 py-0.5 rounded border border-emerald-500/10 flex items-center space-x-1">
                <Flame className="h-3 w-3" />
                <span>{_('streak', 'Streak')}: {profile?.learningStreak ?? 0} {_('days', 'days')}</span>
              </span>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-center">
              {/* Vertical hours graph representation */}
              <div className="space-y-4">
                <p className="text-xs text-gray-400 font-semibold">{_('weeklyHours', 'Weekly Study Hours')}</p>
                <div className="flex justify-between items-end h-32 pt-4 px-2">
                  {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((day, idx) => {
                    const hrs = profile?.weeklyStudyHours?.[idx] ?? 0.0;
                    const pctHeight = Math.min((hrs * 100) / 3.0, 100);
                    return (
                      <div key={day} className="flex flex-col items-center space-y-2 flex-1">
                        <span className="text-[9px] font-mono text-gray-500">{hrs.toFixed(1)}h</span>
                        <div className="w-3 bg-gray-900 rounded-full h-20 overflow-hidden flex items-end">
                          <div className="w-full bg-violet-500 rounded-full" style={{ height: `${pctHeight}%` }}></div>
                        </div>
                        <span className="text-[9px] text-gray-400 font-bold">{day}</span>
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Heatmap Contribution Calendar */}
              <div className="space-y-3">
                <div className="flex justify-between items-center text-xs text-gray-400 font-semibold">
                  <span>Streak Heatmap (Past 28 Days)</span>
                  <span className="text-[10px] text-gray-600">Less &bull; More</span>
                </div>
                <div className="grid grid-cols-7 gap-2 p-3 bg-gray-950 rounded-xl border border-gray-900">
                  {heatmapDays.map((d) => (
                    <div
                      key={d.day}
                      className={`h-6 rounded-md transition hover:scale-115 cursor-pointer ${
                        d.level === 0 ? 'bg-gray-900/30' :
                        d.level === 1 ? 'bg-violet-900/20 border border-violet-500/10' :
                        d.level === 2 ? 'bg-violet-600/40' : 'bg-violet-500 shadow-md shadow-violet-500/20'
                      }`}
                      title={`Day ${d.day}: Study completed`}
                    />
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* User profile details & Badges Achievements */}
          <div className="glass-panel p-6 rounded-2xl border border-gray-900 space-y-4">
            <h3 className="text-sm font-bold text-gray-200 flex items-center space-x-2">
              <Award className="h-4.5 w-4.5 text-violet-400" />
              <span>{_('earnedBadges', 'Earned Badges')}</span>
            </h3>
            
            <div className="space-y-3 pt-2 max-h-52 overflow-y-auto">
              {profile?.achievements && profile.achievements.length > 0 ? (
                profile.achievements.map((ach, idx) => (
                  <div key={idx} className="flex items-center space-x-3 p-3 bg-gray-900/20 border border-gray-900 rounded-xl">
                    <span className="text-lg">🏅</span>
                    <div>
                      <p className="text-xs font-bold text-gray-300">{ach.split(":")[0]}</p>
                      <p className="text-[9px] text-gray-500">{ach.split(":")[1] || 'Badge details'}</p>
                    </div>
                  </div>
                ))
              ) : (
                <div className="py-6 text-center text-xs text-gray-500 border border-dashed border-gray-900 rounded-xl">
                  Take quizzes and study lessons to unlock badges.
                </div>
              )}
            </div>
          </div>
        </section>

        {/* Upload Zone */}
        <section
          onDragOver={(e) => e.preventDefault()}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          className="glass-panel border-2 border-dashed border-gray-800 hover:border-violet-500/50 rounded-2xl py-12 px-6 text-center cursor-pointer transition duration-300 flex flex-col items-center justify-center space-y-4"
        >
          <input
            type="file"
            ref={fileInputRef}
            onChange={handleFileUpload}
            accept=".pdf"
            className="hidden"
          />
          <div className="w-14 h-14 bg-violet-600/10 rounded-full flex items-center justify-center border border-violet-500/20">
            {uploading ? (
              <RefreshCw className="h-6 w-6 text-violet-400 animate-spin" />
            ) : (
              <Upload className="h-6 w-6 text-violet-400" />
            )}
          </div>
          <div>
            <h4 className="text-lg font-bold text-gray-200">
              {uploading ? 'Processing and chunking document...' : 'Upload PDF Learning Material'}
            </h4>
            <p className="text-sm text-gray-500 mt-1 max-w-sm mx-auto">
              Drag & drop your study PDF file here, or click to choose from system files. Maximum size 20MB.
            </p>
          </div>
        </section>

        {/* Documents & Courses Lists & Activity timeline split */}
        <section className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Courses & Activity Lists */}
          <div className="lg:col-span-2 space-y-8">
            <div className="space-y-6">
              <h3 className="text-xl font-bold text-gray-100 flex items-center space-x-2">
                <BookOpen className="h-5 w-5 text-violet-500" />
                <span>{_('yourCourses', 'Generated Courses')}</span>
              </h3>
              
              {loading ? (
                <div className="h-40 flex items-center justify-center glass-card rounded-2xl">
                  <RefreshCw className="h-8 w-8 text-violet-400 animate-spin" />
                </div>
              ) : stats?.recentCourses.length === 0 ? (
                <div className="h-40 flex flex-col items-center justify-center text-center p-6 glass-card rounded-2xl border border-gray-900">
                  <BookOpen className="h-8 w-8 text-gray-600 mb-2" />
                  <p className="text-sm text-gray-500">{_('noCourses', 'No courses generated yet. Upload a PDF first.')}</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {stats?.recentCourses.map((c) => (
                    <div key={c.courseId} className="glass-card p-6 rounded-2xl border border-gray-900 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 hover:border-violet-500/20 transition duration-300">
                      <div className="space-y-1.5 flex-1 w-full">
                        <h4 className="font-bold text-gray-200">{c.title}</h4>
                        
                        {c.status === 'GENERATING_OUTLINE' || c.status === 'PENDING' ? (
                          <div className="inline-flex items-center space-x-1.5 text-xs text-amber-400 font-semibold bg-amber-500/5 px-2 py-0.5 rounded border border-amber-500/10">
                            <RefreshCw className="h-3 w-3 animate-spin" />
                            <span>Generating course outline...</span>
                          </div>
                        ) : c.status === 'FAILED' ? (
                          <div className="inline-flex items-center space-x-1.5 text-xs text-red-400 font-semibold bg-red-500/5 px-2 py-0.5 rounded border border-red-500/10">
                            <AlertTriangle className="h-3 w-3" />
                            <span>{c.failureReason || 'Outline compilation failed'}</span>
                          </div>
                        ) : (
                          <div className="w-full max-w-sm space-y-1">
                            <div className="flex justify-between text-xs font-semibold text-gray-400">
                              <span>Progress ({c.completionPercentage}%)</span>
                              <span>{c.completedLessons}/{c.totalLessons} Lessons</span>
                            </div>
                            <div className="w-full h-1.5 bg-gray-900 rounded-full overflow-hidden">
                              <div className="h-full bg-violet-600 rounded-full transition-all duration-500" style={{ width: `${c.completionPercentage}%` }}></div>
                            </div>
                          </div>
                        )}
                      </div>

                      <div className="shrink-0 flex items-center gap-2">
                        {c.status === 'FAILED' && (
                          <button
                            onClick={() => handleRetryCourse(c.courseId)}
                            className="flex items-center space-x-1.5 px-4 py-2 text-xs font-bold rounded-lg border border-red-500/30 hover:bg-red-500/10 transition"
                          >
                            <RefreshCw className="h-3 w-3" />
                            <span>Retry</span>
                          </button>
                        )}

                        {c.status === 'READY' && (
                          <>
                            {/* Claim Cert Shortcut if completed */}
                            {c.completionPercentage >= 80 && (
                              <Link
                                href={`/courses/${c.courseId}/certificate`}
                                className="flex items-center space-x-1 px-3 py-2 text-[10px] font-extrabold rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white shadow-lg transition"
                                title="Completion Certificate"
                              >
                                <Award className="h-3.5 w-3.5" />
                                <span>Certificate</span>
                              </Link>
                            )}
                            <Link
                              href={`/courses/${c.courseId}`}
                              className="flex items-center space-x-1 px-4 py-2 text-xs font-bold rounded-lg bg-violet-600 hover:bg-violet-700 text-white shadow-lg transition"
                            >
                              <span>Open</span>
                              <ChevronRight className="h-3 w-3" />
                            </Link>
                          </>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Recent Activity Timeline */}
            <div className="space-y-6 pt-4">
              <h3 className="text-xl font-bold text-gray-100 flex items-center space-x-2">
                <Calendar className="h-5 w-5 text-indigo-500" />
                <span>{_('recentActivity', 'Recent Activity Feed')}</span>
              </h3>
              
              {profile?.recentActivity && profile.recentActivity.length > 0 ? (
                <div className="relative border-l border-gray-900 ml-3 pl-6 space-y-6">
                  {profile.recentActivity.map((act, index) => (
                    <div key={index} className="relative">
                      {/* Node Indicator dot */}
                      <span className="absolute -left-[30px] top-1.5 h-3.5 w-3.5 rounded-full border-2 border-gray-950 bg-violet-500 shadow-md shadow-violet-500/10"></span>
                      <div className="space-y-0.5">
                        <p className="text-xs text-gray-300 font-medium">{act.description}</p>
                        <span className="text-[9px] text-gray-500 font-semibold font-mono">
                          {new Date(act.timestamp).toLocaleDateString()} at {new Date(act.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="py-8 text-center text-xs text-gray-500 border border-dashed border-gray-900 rounded-2xl">
                  No activity recorded yet. Study lessons and submit quizzes.
                </div>
              )}
            </div>
          </div>

          {/* Uploaded Documents List */}
          <div className="space-y-6">
            <h3 className="text-xl font-bold text-gray-100 flex items-center space-x-2">
              <FileText className="h-5 w-5 text-indigo-500" />
              <span>{_('library', 'Library')}</span>
            </h3>
            
            {loading ? (
              <div className="h-40 flex items-center justify-center glass-card rounded-2xl">
                <RefreshCw className="h-8 w-8 text-indigo-400 animate-spin" />
              </div>
            ) : pdfs.length === 0 ? (
              <div className="h-40 flex flex-col items-center justify-center text-center p-6 glass-card rounded-2xl border border-gray-900">
                <FileText className="h-8 w-8 text-gray-600 mb-2" />                  <p className="text-sm text-gray-500">{_('noPdfs', 'No PDFs uploaded yet.')}</p>
              </div>
            ) : (
              <div className="space-y-4">
                {pdfs.map((p) => {
                  const alreadyGenerated = stats?.recentCourses.some((c) => c.pdfMetadataId === p.id);
                  return (
                    <div key={p.id} className="glass-card p-4 rounded-xl border border-gray-900 flex justify-between items-center gap-3">
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-bold text-gray-300 truncate">{p.filename}</p>
                        <div className="flex items-center space-x-2 text-[10px] text-gray-500 mt-1">
                          <span>{(p.fileSize / (1024 * 1024)).toFixed(2)} MB</span>
                          <span>&bull;</span>
                          <span>{p.status}</span>
                        </div>
                      </div>
                      
                      <div className="shrink-0 flex items-center gap-2">
                        {p.status === 'PARSED' && !alreadyGenerated && (
                          <button
                            onClick={() => handleCreateCourse(p.id)}
                            className="flex items-center space-x-1.5 px-3 py-1.5 text-xs font-bold rounded-lg border border-violet-500/30 text-violet-400 hover:bg-violet-500/10 transition"
                          >
                            <Plus className="h-3 w-3" />
                            <span>Build</span>
                          </button>
                        )}
                        <button
                          onClick={() => handleDeletePDF(p.id)}
                          className="flex items-center justify-center p-1.5 text-xs font-bold rounded-lg border border-red-500/30 text-red-400 hover:bg-red-500/10 transition"
                          title="Delete PDF"
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </section>
      </main>
    </div>
  );
}
