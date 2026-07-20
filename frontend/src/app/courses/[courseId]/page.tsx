'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import { useLanguage } from '@/context/LanguageContext';
import { coursesService } from '@/services/courses';
import { progressService } from '@/services/progress';
import { evolutionService, PDFSummary } from '@/services/evolution';
import { Course, Chapter, CourseProgress } from '@/types';
import {
  ArrowLeft,
  BookOpen,
  Calendar,
  Layers,
  CheckCircle,
  HelpCircle,
  Clock,
  Sparkles,
  ChevronRight,
  RotateCw,
  Trophy,
  Award,
  Download,
  FileText,
  Key,
  Flame,
  Info,
  ExternalLink,
  Compass,
  MessageSquare
} from 'lucide-react';

export default function CourseOutlinePage() {
  const params = useParams();
  const courseId = params.courseId as string;

  return (
    <ProtectedRoute>
      <CourseOutlineContent courseId={courseId} />
    </ProtectedRoute>
  );
}

function CourseOutlineContent({ courseId }: { courseId: string }) {
  const router = useRouter();
  const { t } = useLanguage();
  
  const [course, setCourse] = useState<Course | null>(null);
  const [chapters, setChapters] = useState<Chapter[]>([]);
  const [progress, setProgress] = useState<CourseProgress | null>(null);
  
  const [summary, setSummary] = useState<PDFSummary | null>(null);
  const [showSummaryDrawer, setShowSummaryDrawer] = useState(false);
  const [loadingSummary, setLoadingSummary] = useState(false);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [expandedChapter, setExpandedChapter] = useState<string | null>(null);

  const fetchCourseData = async () => {
    try {
      setLoading(true);
      setError(null);
      
      const [courseRes, chaptersRes, progressRes] = await Promise.all([
        coursesService.getCourse(courseId),
        coursesService.getCourseChapters(courseId),
        progressService.getCourseProgress(courseId)
      ]);

      if (courseRes.success) setCourse(courseRes.data);
      if (chaptersRes.success) {
        setChapters(chaptersRes.data);
        if (chaptersRes.data.length > 0) {
          setExpandedChapter(chaptersRes.data[0].id); // Expand first chapter by default
        }
      }
      if (progressRes.success) setProgress(progressRes.data);

    } catch (err: any) {
      setError('Failed to load course details. Make sure the backend is active.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCourseData();
  }, [courseId]);

  const loadSummaryData = async () => {
    if (summary) {
      setShowSummaryDrawer(true);
      return;
    }
    
    try {
      setLoadingSummary(true);
      const res = await evolutionService.getCourseSummary(courseId);
      if (res.success) {
        setSummary(res.data);
        setShowSummaryDrawer(true);
      }
    } catch (e) {
      alert("Failed to load PDF summaries.");
    } finally {
      setLoadingSummary(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-950">
        <div className="flex flex-col items-center space-y-4">
          <RotateCw className="h-10 w-10 text-violet-500 animate-spin" />
          <span className="text-sm text-gray-400 font-medium">Assembling course materials...</span>
        </div>
      </div>
    );
  }

  if (error || !course) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-950 px-4 text-center">
        <AlertTriangle className="h-12 w-12 text-red-500 mb-4 animate-pulse" />
        <h3 className="text-xl font-bold mb-2">Error Loading Course</h3>
        <p className="text-gray-400 text-sm max-w-sm mb-6">{error || 'Course record does not exist.'}</p>
        <Link href="/dashboard" className="px-5 py-2.5 rounded-xl bg-violet-600 hover:bg-violet-700 text-sm font-semibold transition">
          {t('backToDashboard')}
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 pb-20">
      {/* Navbar Header */}
      <header className="glass-panel sticky top-0 z-50 px-6 py-4 flex items-center justify-between border-b border-gray-900 bg-gray-950/80">
        <div className="flex items-center space-x-4">
          <Link href="/dashboard" className="p-2 hover:bg-gray-900 rounded-lg text-gray-400 hover:text-white transition">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-lg font-bold text-gray-200 line-clamp-1">{course.title}</h1>
            <p className="text-xs text-gray-400">{t('courseSyllabus')}</p>
          </div>
        </div>
        
        {progress && (
          <div className="hidden sm:flex items-center space-x-4">
            <div className="text-right">
              <p className="text-xs font-semibold text-gray-400">Course Completed</p>
              <p className="text-sm font-bold text-violet-400">{progress.percentage}%</p>
            </div>
            <div className="w-24 h-2 bg-gray-900 rounded-full overflow-hidden">
              <div className="h-full bg-violet-600 rounded-full" style={{ width: `${progress.percentage}%` }}></div>
            </div>
          </div>
        )}
      </header>

      {/* Hero Layout */}
      <main className="max-w-7xl mx-auto px-6 mt-10 grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left Side: Metadata Summary & Shortcuts */}
        <div className="lg:col-span-1 space-y-6">
          {/* Quick study aids buttons */}
          <div className="glass-card p-6 rounded-2xl border border-gray-900 space-y-4">
            <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider">AI Study Resources</h3>
            
            <div className="grid grid-cols-1 gap-2.5">
              <button
                onClick={loadSummaryData}
                disabled={loadingSummary}
                className="w-full flex items-center justify-between px-4 py-3 rounded-xl border border-gray-800 bg-gray-900/20 hover:bg-gray-900/60 text-xs font-semibold text-gray-300 hover:text-white transition"
              >
                <div className="flex items-center space-x-2">
                  <FileText className="h-4 w-4 text-violet-400" />
                  <span>Syllabus Summaries & Takeaways</span>
                </div>
                {loadingSummary ? <RotateCw className="h-3.5 w-3.5 animate-spin" /> : <ChevronRight className="h-3.5 w-3.5" />}
              </button>

              <Link
                href={`/courses/${courseId}/flashcards`}
                className="w-full flex items-center justify-between px-4 py-3 rounded-xl border border-gray-800 bg-gray-900/20 hover:bg-gray-900/60 text-xs font-semibold text-gray-300 hover:text-white transition"
              >
                <div className="flex items-center space-x-2">
                  <Sparkles className="h-4 w-4 text-amber-400" />
                  <span>Review Flashcards (Leitner repetition)</span>
                </div>
                <ChevronRight className="h-3.5 w-3.5" />
              </Link>

              <Link
                href={`/courses/${courseId}/mindmap`}
                className="w-full flex items-center justify-between px-4 py-3 rounded-xl border border-gray-800 bg-gray-900/20 hover:bg-gray-900/60 text-xs font-semibold text-gray-300 hover:text-white transition"
              >
                <div className="flex items-center space-x-2">
                  <Compass className="h-4 w-4 text-indigo-400" />
                  <span>Interactive Diagrams & Mind Maps</span>
                </div>
                <ChevronRight className="h-3.5 w-3.5" />
              </Link>

              <Link
                href={`/courses/${courseId}/chat`}
                className="w-full flex items-center justify-between px-4 py-3 rounded-xl border border-gray-800 bg-gray-900/20 hover:bg-gray-900/60 text-xs font-semibold text-gray-300 hover:text-white transition"
              >
                <div className="flex items-center space-x-2">
                  <MessageSquare className="h-4 w-4 text-violet-400" />
                  <span>AI Chat Tutor (Contextual PDF bot)</span>
                </div>
                <ChevronRight className="h-3.5 w-3.5" />
              </Link>

              {progress && progress.percentage >= 80 && (
                <Link
                  href={`/courses/${courseId}/certificate`}
                  className="w-full flex items-center justify-between px-4 py-3 rounded-xl border border-emerald-500/20 bg-emerald-500/5 hover:bg-emerald-500/10 text-xs font-semibold text-emerald-400 transition"
                >
                  <div className="flex items-center space-x-2">
                    <Award className="h-4 w-4 text-emerald-400" />
                    <span>Graduation Certificate Unlocked!</span>
                  </div>
                  <ChevronRight className="h-3.5 w-3.5" />
                </Link>
              )}
            </div>

            {/* Export buttons */}
            <div className="pt-4 border-t border-gray-900/60 space-y-2">
              <span className="text-[10px] text-gray-500 font-bold uppercase tracking-wider">Export Complete Course</span>
              <div className="flex gap-2">
                <a
                  href={`/api/courses/${courseId}/export?format=MD`}
                  className="flex-1 flex items-center justify-center space-x-1.5 px-3 py-2 bg-gray-900 hover:bg-gray-850 border border-gray-850 rounded-xl text-[10px] font-bold text-gray-400 hover:text-white transition"
                >
                  <Download className="h-3.5 w-3.5" />
                  <span>Export Markdown</span>
                </a>
                <a
                  href={`/api/courses/${courseId}/export?format=HTML`}
                  className="flex-1 flex items-center justify-center space-x-1.5 px-3 py-2 bg-gray-900 hover:bg-gray-850 border border-gray-850 rounded-xl text-[10px] font-bold text-gray-400 hover:text-white transition"
                >
                  <Download className="h-3.5 w-3.5" />
                  <span>Export HTML</span>
                </a>
              </div>
            </div>
          </div>

          {/* Core Info */}
          <div className="glass-card p-6 rounded-2xl border border-gray-900 space-y-6">
            <div>
              <h2 className="text-lg font-bold text-gray-200 mb-2">About the Course</h2>
              <p className="text-sm text-gray-400 leading-relaxed">{course.description}</p>
            </div>

            {/* Quick Metrics */}
            <div className="grid grid-cols-2 gap-4 pt-4 border-t border-gray-900">
              <div className="flex items-center space-x-2 text-sm text-gray-300">
                <Clock className="h-4.5 w-4.5 text-violet-400" />
                <span>{course.estimatedDuration}</span>
              </div>
              <div className="flex items-center space-x-2 text-sm text-gray-300">
                <Layers className="h-4.5 w-4.5 text-indigo-400" />
                <span>{course.difficultyLevel}</span>
              </div>
            </div>

            {/* Prerequisites */}
            {course.prerequisites && course.prerequisites.length > 0 && (
              <div className="space-y-2 pt-4 border-t border-gray-900">
                <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wide">Prerequisites</h4>
                <div className="flex flex-wrap gap-2">
                  {course.prerequisites.map((p, idx) => (
                    <span key={idx} className="text-xs px-2.5 py-1 rounded bg-gray-900 text-gray-400 border border-gray-800 font-medium">
                      {p}
                    </span>
                  ))}
                </div>
              </div>
            )}

            {/* Learning Objectives */}
            {course.learningObjectives && course.learningObjectives.length > 0 && (
              <div className="space-y-3 pt-4 border-t border-gray-900">
                <h4 className="text-xs font-semibold text-gray-400 uppercase tracking-wide">Learning Objectives</h4>
                <ul className="space-y-2">
                  {course.learningObjectives.map((o, idx) => (
                    <li key={idx} className="text-xs text-gray-400 flex items-start space-x-2 leading-relaxed">
                      <CheckCircle className="h-4 w-4 text-emerald-500 shrink-0 mt-0.5" />
                      <span>{o}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>

        {/* Right Side: Chapter/Lesson Accordion */}
        <div className="lg:col-span-2 space-y-6">
          <h3 className="text-xl font-bold text-gray-100 flex items-center space-x-2">
            <BookOpen className="h-5 w-5 text-violet-500" />
            <span>{t('courseSyllabus')}</span>
          </h3>

          <div className="space-y-4">
            {chapters.map((chapter) => {
              const isExpanded = expandedChapter === chapter.id;
              return (
                <div key={chapter.id} className="glass-panel rounded-2xl overflow-hidden border border-gray-900">
                  {/* Chapter Banner */}
                  <button
                    onClick={() => setExpandedChapter(isExpanded ? null : chapter.id)}
                    className="w-full text-left p-6 flex justify-between items-center hover:bg-gray-900/10 transition"
                  >
                    <div className="space-y-1 pr-4">
                      <span className="text-[10px] text-violet-400 font-bold tracking-wider uppercase">Chapter {chapter.sequenceNumber}</span>
                      <h4 className="font-bold text-gray-200 text-base">{chapter.title}</h4>
                      <p className="text-xs text-gray-400 line-clamp-1">{chapter.summary}</p>
                    </div>
                    <ChevronRight className={`h-5 w-5 text-gray-500 transition-transform ${isExpanded ? 'rotate-90' : ''}`} />
                  </button>

                  {/* Lessons list */}
                  {isExpanded && (
                    <div className="border-t border-gray-900 bg-gray-950/40 p-6 space-y-4">
                      <div className="space-y-2">
                        {chapter.lessons.map((lesson) => {
                          const isLessonCompleted = progress?.completedLessonIds.includes(lesson.id) ?? false;
                          return (
                            <div key={lesson.id} className="flex items-center justify-between p-3.5 rounded-xl bg-gray-900/20 hover:bg-gray-900/40 border border-gray-900 transition gap-4">
                              <div className="flex items-center space-x-3.5 min-w-0">
                                <div className={`h-5 w-5 rounded-full border flex items-center justify-center shrink-0 ${isLessonCompleted ? 'bg-violet-600 border-violet-600 text-white' : 'border-gray-800'}`}>
                                  {isLessonCompleted && <CheckCircle className="h-4 w-4" />}
                                </div>
                                <span className={`text-sm font-semibold truncate ${isLessonCompleted ? 'text-gray-500 line-through' : 'text-gray-300'}`}>
                                  {lesson.title}
                                </span>
                              </div>
                              <Link
                                href={`/courses/${courseId}/lesson/${lesson.id}`}
                                className="shrink-0 flex items-center space-x-1 text-xs text-violet-400 hover:text-violet-300 font-semibold"
                              >
                                <span>Study</span>
                                <ChevronRight className="h-3 w-3" />
                              </Link>
                            </div>
                          );
                        })}
                      </div>

                      {/* Quiz Button */}
                      <div className="pt-4 border-t border-gray-900/60 flex justify-end">
                        <Link
                          href={`/courses/${courseId}/quiz/${chapter.id}`}
                          className="glow-button flex items-center space-x-2 px-5 py-2.5 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 text-xs font-bold text-white shadow-lg"
                        >
                          <Trophy className="h-4 w-4 text-amber-400" />
                          <span>Take Chapter Quiz</span>
                        </Link>
                      </div>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </div>

      </main>

      {/* Floating Summary Drawer Modal */}
      {showSummaryDrawer && summary && (
        <div className="fixed inset-0 z-50 flex justify-end bg-black/60 backdrop-blur-sm">
          <div className="w-full max-w-2xl bg-gray-950 h-full overflow-y-auto p-8 border-l border-gray-900 shadow-2xl flex flex-col justify-between">
            <div className="space-y-8">
              <div className="flex justify-between items-center border-b border-gray-900 pb-4">
                <div className="flex items-center space-x-2">
                  <FileText className="h-5 w-5 text-violet-400" />
                  <h2 className="text-lg font-bold text-gray-200">Syllabus Summaries</h2>
                </div>
                <button
                  onClick={() => setShowSummaryDrawer(false)}
                  className="px-3 py-1.5 rounded-lg border border-gray-800 text-xs text-gray-400 hover:text-white"
                >
                  Close
                </button>
              </div>

              {/* One liner & short summaries */}
              <div className="space-y-4">
                <div className="p-4 bg-violet-600/5 border border-violet-500/10 rounded-xl space-y-1">
                  <span className="text-[9px] text-violet-400 font-extrabold uppercase tracking-wide">Key Focus Summary</span>
                  <p className="text-xs text-gray-200 leading-relaxed italic">&ldquo;{summary.oneLineSummary}&rdquo;</p>
                </div>

                <div className="space-y-2">
                  <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wide">Core Summary</h4>
                  <p className="text-xs text-gray-400 leading-relaxed">{summary.shortSummary}</p>
                </div>

                <div className="space-y-2">
                  <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wide">Detailed Executive Briefing</h4>
                  <p className="text-xs text-gray-400 leading-relaxed whitespace-pre-wrap">{summary.detailedSummary || summary.executiveSummary}</p>
                </div>
              </div>

              {/* Takeaways */}
              {summary.keyTakeaways && summary.keyTakeaways.length > 0 && (
                <div className="space-y-3">
                  <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wide flex items-center space-x-1">
                    <Key className="h-3.5 w-3.5 text-violet-400" />
                    <span>Key Takeaways</span>
                  </h4>
                  <ul className="space-y-2">
                    {summary.keyTakeaways.map((tk, idx) => (
                      <li key={idx} className="text-xs text-gray-400 flex items-start space-x-2 leading-relaxed">
                        <span className="h-1.5 w-1.5 rounded-full bg-violet-400 shrink-0 mt-1.5" />
                        <span>{tk}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {/* Chapters Outlines summaries */}
              {summary.chapterSummaries && summary.chapterSummaries.length > 0 && (
                <div className="space-y-4">
                  <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wide">Chapters Outlines summaries</h4>
                  <div className="space-y-3">
                    {summary.chapterSummaries.map((ch, idx) => (
                      <div key={idx} className="p-4 bg-gray-900/20 border border-gray-900 rounded-xl space-y-1">
                        <p className="text-xs font-bold text-gray-300">{ch.chapterTitle}</p>
                        <p className="text-[11px] text-gray-500 leading-relaxed">{ch.summary}</p>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            <div className="pt-6 border-t border-gray-900 mt-10">
              <button
                onClick={() => setShowSummaryDrawer(false)}
                className="w-full py-3 bg-violet-600 hover:bg-violet-700 text-xs font-bold rounded-xl text-white transition"
              >
                Begin Learning Units
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function AlertTriangle(props: any) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z" />
      <line x1="12" y1="9" x2="12" y2="13" />
      <line x1="12" y1="17" x2="12.01" y2="17" />
    </svg>
  );
}
