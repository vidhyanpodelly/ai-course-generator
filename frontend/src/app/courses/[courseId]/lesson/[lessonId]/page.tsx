'use client';

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import { SidebarNavigation } from '@/components/course/SidebarNavigation';
import { LessonRenderer } from '@/components/course/LessonRenderer';
import { coursesService } from '@/services/courses';
import { progressService } from '@/services/progress';
import { chatService } from '@/services/chat';
import { evolutionService } from '@/services/evolution';
import { Course, Chapter, Lesson, ChatMessage, CourseProgress } from '@/types';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ArrowLeft,
  CheckCircle,
  HelpCircle,
  FileText,
  Key,
  AlertTriangle,
  Lightbulb,
  Send,
  Loader2,
  RefreshCw,
  Menu,
  Volume2,
  VolumeX,
  Play,
  Pause,
  Sliders,
  Sparkles,
  Inbox,
  Share2,
  Award,
  Layers,
  Search,
  MessageCircle,
  Compass,
  Download,
  MessageSquare,
  X
} from 'lucide-react';

export default function LessonPage() {
  const params = useParams();
  const courseId = params.courseId as string;
  const lessonId = params.lessonId as string;

  return (
    <ProtectedRoute>
      <LessonViewerContent courseId={courseId} lessonId={lessonId} />
    </ProtectedRoute>
  );
}

function LessonViewerContent({ courseId, lessonId }: { courseId: string; lessonId: string }) {
  const router = useRouter();

  const [course, setCourse] = useState<Course | null>(null);
  const [chapters, setChapters] = useState<Chapter[]>([]);
  const [lesson, setLesson] = useState<Lesson | null>(null);
  const [progress, setProgress] = useState<CourseProgress | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  
  const [loadingLesson, setLoadingLesson] = useState(true);
  const [loadingProgress, setLoadingProgress] = useState(false);
  const [chatMessage, setChatMessage] = useState('');
  const [sendingChat, setSendingChat] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showChatMobile, setShowChatMobile] = useState(false);

  // Suggested questions list
  const [suggestedQuestions, setSuggestedQuestions] = useState<string[]>([
    "Can you clarify the primary mechanism?",
    "Show me a code example.",
    "Summarize this lesson in one sentence.",
    "Give me an analogy for this concept."
  ]);

  // TTS states
  const [isSpeaking, setIsSpeaking] = useState(false);
  const [isPaused, setIsPaused] = useState(false);
  const [ttsRate, setTtsRate] = useState(1.0);
  const [voices, setVoices] = useState<SpeechSynthesisVoice[]>([]);
  const [selectedVoiceName, setSelectedVoiceName] = useState<string>('');
  const [showTtsControls, setShowTtsControls] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const synthRef = useRef<SpeechSynthesis | null>(null);
  const utteranceRef = useRef<SpeechSynthesisUtterance | null>(null);

  // Initialize Speech Synthesis
  useEffect(() => {
    if (typeof window !== 'undefined' && 'speechSynthesis' in window) {
      synthRef.current = window.speechSynthesis;
      const loadVoices = () => {
        const availableVoices = window.speechSynthesis.getVoices();
        setVoices(availableVoices);
        if (availableVoices.length > 0) {
          // Default to first English or first available voice
          const defaultVoice = availableVoices.find(v => v.lang.startsWith('en')) || availableVoices[0];
          setSelectedVoiceName(defaultVoice.name);
        }
      };
      loadVoices();
      window.speechSynthesis.onvoiceschanged = loadVoices;
    }

    return () => {
      if (synthRef.current) {
        synthRef.current.cancel();
      }
    };
  }, []);

  const fetchLessonData = async () => {
    try {
      setLoadingLesson(true);
      setError(null);

      // 1. Fetch lesson details
      const lessonRes = await coursesService.getLesson(lessonId);
      if (lessonRes.success) setLesson(lessonRes.data);

      // 2. Fetch course outline & progress
      const [courseRes, chaptersRes, progressRes, chatRes] = await Promise.all([
        coursesService.getCourse(courseId),
        coursesService.getCourseChapters(courseId),
        progressService.getCourseProgress(courseId),
        chatService.getChatHistory(courseId)
      ]);

      if (courseRes.success) setCourse(courseRes.data);
      if (chaptersRes.success) setChapters(chaptersRes.data);
      if (progressRes.success) setProgress(progressRes.data);
      if (chatRes.success) setMessages(chatRes.data);

    } catch (err: any) {
      setError('Could not load study material. Make sure the backend is reachable.');
    } finally {
      setLoadingLesson(false);
    }
  };

  useEffect(() => {
    fetchLessonData();
    // Reset TTS when navigating lessons
    if (synthRef.current) {
      synthRef.current.cancel();
      setIsSpeaking(false);
      setIsPaused(false);
    }
  }, [lessonId]);

  // Autoscroll chat
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleToggleComplete = async () => {
    if (!lesson) return;
    setLoadingProgress(true);
    
    const isCurrentlyCompleted = progress?.completedLessonIds.includes(lessonId) ?? false;
    const nextCompletedState = !isCurrentlyCompleted;

    try {
      const response = await progressService.markComplete(lessonId, nextCompletedState);
      if (response.success) {
        setProgress(response.data);
      }
    } catch (err) {
      setError('Failed to update progress checkpoint.');
    } finally {
      setLoadingProgress(false);
    }
  };

  // SSE streaming chat sender
  const handleSendChat = async (userText: string) => {
    if (!userText.trim() || sendingChat) return;

    setChatMessage('');
    setSendingChat(true);

    // Save user message in list
    const tempUserMsg: ChatMessage = {
      id: Math.random().toString(),
      sessionId: '',
      sender: 'USER',
      messageContent: userText,
      createdAt: new Date().toISOString()
    };
    setMessages((prev) => [...prev, tempUserMsg]);

    // Add empty placeholder for streaming AI response
    const aiTempId = Math.random().toString();
    const tempAiMsg: ChatMessage = {
      id: aiTempId,
      sessionId: '',
      sender: 'AI',
      messageContent: '',
      createdAt: new Date().toISOString()
    };
    setMessages((prev) => [...prev, tempAiMsg]);

    try {
      const token = localStorage.getItem('auth_token') || '';
      
      const response = await fetch(`/api/chat/${courseId}/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': token ? `Bearer ${token}` : ''
        },
        body: JSON.stringify({ message: userText })
      });

      if (!response.ok) {
        throw new Error("Streaming call failed");
      }

      if (!response.body) {
        throw new Error("No response stream body");
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let accumulated = "";

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunkText = decoder.decode(value, { stream: true });
        
        // Split SSE data chunks
        const lines = chunkText.split("\n");
        for (const line of lines) {
          if (line.startsWith("data:")) {
            const content = line.substring(5).trim();
            if (content) {
              accumulated += content;
              setMessages((prev) =>
                prev.map((msg) =>
                  msg.id === aiTempId ? { ...msg, messageContent: accumulated } : msg
                )
              );
            }
          }
        }
      }

    } catch (err) {
      setMessages((prev) =>
        prev.map((msg) =>
          msg.id === aiTempId 
            ? { ...msg, messageContent: 'I encountered an error replying to that query. Please check your network connection.' } 
            : msg
        )
      );
    } finally {
      setSendingChat(false);
    }
  };

  // TTS Controls
  const handleTtsPlay = () => {
    if (!lesson || !synthRef.current) return;

    if (isPaused) {
      synthRef.current.resume();
      setIsPaused(false);
      setIsSpeaking(true);
      return;
    }

    synthRef.current.cancel();

    // Clean html markdown out of text for narration
    const cleanText = (lesson.explanation ?? '')
      .replace(/#+\s+/g, '')
      .replace(/`+/g, '')
      .replace(/[*_~|-]+/g, ' ')
      .trim();

    const utterance = new SpeechSynthesisUtterance(cleanText);
    
    if (selectedVoiceName) {
      const selectedVoice = voices.find(v => v.name === selectedVoiceName);
      if (selectedVoice) utterance.voice = selectedVoice;
    }

    utterance.rate = ttsRate;
    
    utterance.onend = () => {
      setIsSpeaking(false);
      setIsPaused(false);
    };

    utterance.onerror = () => {
      setIsSpeaking(false);
      setIsPaused(false);
    };

    utteranceRef.current = utterance;
    setIsSpeaking(true);
    setIsPaused(false);
    synthRef.current.speak(utterance);
  };

  const handleTtsPause = () => {
    if (synthRef.current && isSpeaking) {
      synthRef.current.pause();
      setIsPaused(true);
      setIsSpeaking(false);
    }
  };

  const handleTtsStop = () => {
    if (synthRef.current) {
      synthRef.current.cancel();
      setIsSpeaking(false);
      setIsPaused(false);
    }
  };

  if (loadingLesson) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-950">
        <div className="flex flex-col items-center space-y-4">
          <Loader2 className="h-10 w-10 text-violet-500 animate-spin" />
          <span className="text-sm text-gray-400 font-medium">Lazy loading lesson content...</span>
        </div>
      </div>
    );
  }

  if (error || !lesson || !course) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-950 px-4 text-center">
        <AlertTriangle className="h-12 w-12 text-red-500 mb-4 animate-pulse" />
        <h3 className="text-xl font-bold mb-2">Error Loading Lesson</h3>
        <p className="text-gray-400 text-sm max-w-sm mb-6">{error || 'Lesson content is currently unavailable.'}</p>
        <Link href={`/courses/${courseId}`} className="px-5 py-2.5 rounded-xl bg-violet-600 hover:bg-violet-700 text-sm font-semibold transition">
          Back to Syllabus
        </Link>
      </div>
    );
  }

  const isCompleted = progress?.completedLessonIds.includes(lessonId) ?? false;

  return (
    <div className="min-h-screen bg-gray-950 flex flex-col">
      {/* Header */}
      <header className="glass-panel sticky top-0 z-50 px-6 py-4 flex items-center justify-between border-b border-gray-900 bg-gray-950/80">
        <div className="flex items-center space-x-4">
          <Link href={`/courses/${courseId}`} className="p-2 hover:bg-gray-900 rounded-lg text-gray-400 hover:text-white transition">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-sm font-bold text-gray-200 line-clamp-1">{lesson.title}</h1>
            <p className="text-[10px] text-violet-400 font-semibold uppercase">{course.title}</p>
          </div>
        </div>

        {/* Toolbar Shortcuts */}
        <div className="flex items-center space-x-3">
          <Link
            href={`/courses/${courseId}/flashcards`}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg border border-gray-800 bg-gray-900/40 text-[10px] font-bold text-gray-400 hover:text-white transition"
          >
            <Sparkles className="h-3.5 w-3.5 text-amber-400" />
            <span>AI Flashcards</span>
          </Link>
          <Link
            href={`/courses/${courseId}/mindmap`}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg border border-gray-800 bg-gray-900/40 text-[10px] font-bold text-gray-400 hover:text-white transition"
          >
            <Compass className="h-3.5 w-3.5 text-violet-400" />
            <span>Interactive Diagrams</span>
          </Link>
          <button
            onClick={() => setShowChatMobile(!showChatMobile)}
            className="flex xl:hidden items-center space-x-1.5 px-3 py-1.5 rounded-lg border border-gray-800 bg-gray-900/40 text-[10px] font-bold text-gray-400 hover:text-white transition"
            title="Toggle AI Chat Companion"
          >
            <MessageSquare className="h-3.5 w-3.5 text-violet-400" />
            <span>AI Chat</span>
          </button>
          <a
            href={`/api/courses/${courseId}/export?format=HTML`}
            className="p-2 hover:bg-gray-900 rounded-lg text-gray-400 hover:text-white transition"
            title="Export Course Outline as HTML"
          >
            <Download className="h-4 w-4" />
          </a>
        </div>
      </header>

      {/* Main Workspace split */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left: Outline Sidebar */}
        <SidebarNavigation
          courseId={courseId}
          chapters={chapters}
          currentLessonId={lessonId}
          completedLessonIds={progress?.completedLessonIds ?? []}
        />

        {/* Center: Lesson Contents & Audio Player */}
        <div className="flex-1 overflow-y-auto px-6 md:px-12 py-10 space-y-10">
          
          {/* TTS Player Bar */}
          {synthRef.current && (
            <div className="glass-panel p-4 rounded-xl border border-gray-900 flex flex-col md:flex-row justify-between items-start md:items-center gap-4 bg-gray-900/5">
              <div className="flex items-center space-x-3">
                <Volume2 className="h-5 w-5 text-violet-400" />
                <div>
                  <h4 className="text-xs font-bold text-gray-300">Narrator Voice Reader</h4>
                  <p className="text-[10px] text-gray-500">Listen to lesson materials dynamically</p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                {isSpeaking ? (
                  <button
                    onClick={handleTtsPause}
                    className="p-2 rounded-lg bg-gray-950 border border-gray-800 text-gray-300 hover:text-white"
                  >
                    <Pause className="h-3.5 w-3.5" />
                  </button>
                ) : (
                  <button
                    onClick={handleTtsPlay}
                    className="p-2 rounded-lg bg-violet-600 text-white hover:bg-violet-700"
                  >
                    <Play className="h-3.5 w-3.5" />
                  </button>
                )}
                
                {isSpeaking || isPaused ? (
                  <button
                    onClick={handleTtsStop}
                    className="p-2 rounded-lg bg-gray-950 border border-gray-800 text-gray-400 hover:text-red-400"
                    title="Stop narration"
                  >
                    <VolumeX className="h-3.5 w-3.5" />
                  </button>
                ) : null}

                {/* Settings Toggle */}
                <button
                  onClick={() => setShowTtsControls(!showTtsControls)}
                  className="p-2 rounded-lg bg-gray-950 border border-gray-800 text-gray-400 hover:text-white"
                  title="Configure voice settings"
                >
                  <Sliders className="h-3.5 w-3.5" />
                </button>
              </div>

              {/* Collapsed Configuration Drawer */}
              {showTtsControls && (
                <div className="w-full border-t border-gray-900/40 pt-3 mt-1 flex flex-col md:flex-row gap-4 items-start md:items-center justify-between text-xs text-gray-400">
                  <div className="flex items-center space-x-2 w-full md:w-auto">
                    <span>Voice:</span>
                    <select
                      value={selectedVoiceName}
                      onChange={(e) => setSelectedVoiceName(e.target.value)}
                      className="bg-gray-950 border border-gray-850 px-2 py-1 rounded focus:outline-none text-[10px]"
                    >
                      {voices.map((v) => (
                        <option key={v.name} value={v.name}>
                          {v.name} ({v.lang})
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="flex items-center space-x-2">
                    <span>Speed:</span>
                    <input
                      type="range"
                      min="0.6"
                      max="1.8"
                      step="0.1"
                      value={ttsRate}
                      onChange={(e) => setTtsRate(parseFloat(e.target.value))}
                      className="w-24 accent-violet-500"
                    />
                    <span className="font-mono text-[10px]">{ttsRate.toFixed(1)}x</span>
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Explanation Text */}
          <section className="glass-panel p-8 md:p-10 rounded-2xl border border-gray-900">
            <LessonRenderer explanationText={lesson.explanation ?? ''} />
          </section>

          {/* Metadata Grids */}
          <section className="grid grid-cols-1 md:grid-cols-2 gap-6">
            
            {/* Takeaways */}
            {lesson.keyTakeaways && lesson.keyTakeaways.length > 0 && (
              <div className="glass-card p-6 rounded-2xl border border-gray-900/60">
                <h4 className="text-sm font-bold text-violet-300 flex items-center space-x-2 mb-4">
                  <Key className="h-4 w-4" />
                  <span>Key Takeaways</span>
                </h4>
                <ul className="space-y-3.5">
                  {lesson.keyTakeaways.map((takeaway, idx) => (
                    <li key={idx} className="text-xs text-gray-400 flex items-start space-x-2.5 leading-relaxed">
                      <span className="h-1.5 w-1.5 rounded-full bg-violet-500 shrink-0 mt-1.5"></span>
                      <span>{takeaway}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Warnings/Notes */}
            {lesson.importantNotes && lesson.importantNotes.length > 0 && (
              <div className="glass-card p-6 rounded-2xl border border-gray-900/60">
                <h4 className="text-sm font-bold text-amber-400 flex items-center space-x-2 mb-4">
                  <AlertTriangle className="h-4 w-4" />
                  <span>Important Notes</span>
                </h4>
                <ul className="space-y-3.5">
                  {lesson.importantNotes.map((note, idx) => (
                    <li key={idx} className="text-xs text-gray-400 flex items-start space-x-2.5 leading-relaxed">
                      <span className="h-1.5 w-1.5 rounded-full bg-amber-500 shrink-0 mt-1.5"></span>
                      <span>{note}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}

            {/* Real World Examples */}
            {lesson.realWorldExamples && lesson.realWorldExamples.length > 0 && (
              <div className="glass-card p-6 rounded-2xl border border-gray-900/60 md:col-span-2">
                <h4 className="text-sm font-bold text-emerald-400 flex items-center space-x-2 mb-4">
                  <Lightbulb className="h-4 w-4" />
                  <span>Real-World Context</span>
                </h4>
                <ul className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {lesson.realWorldExamples.map((example, idx) => (
                    <li key={idx} className="text-xs text-gray-400 bg-gray-900/10 border border-gray-900 p-4 rounded-xl leading-relaxed flex items-start space-x-2">
                      <span className="text-emerald-500 font-bold shrink-0 mt-0.5">✓</span>
                      <span>{example}</span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </section>

          {/* Completion Navigation Footer */}
          <footer className="pt-6 border-t border-gray-900 flex justify-between items-center gap-4">
            <button
              onClick={handleToggleComplete}
              disabled={loadingProgress}
              className={`glow-button px-6 py-3 rounded-xl text-xs font-bold transition flex items-center space-x-2 disabled:opacity-50 ${
                isCompleted
                  ? 'bg-emerald-600 hover:bg-emerald-700 text-white'
                  : 'bg-violet-600 hover:bg-violet-700 text-white'
              }`}
            >
              {loadingProgress ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <CheckCircle className="h-4 w-4" />
              )}
              <span>{isCompleted ? 'Completed Checkpoint' : 'Mark as Complete'}</span>
            </button>

            {/* If 100% complete, show certificate button */}
            {progress && progress.percentage >= 80 && (
              <Link
                href={`/courses/${courseId}/certificate`}
                className="flex items-center space-x-1 px-5 py-3 rounded-xl bg-gradient-to-r from-emerald-600 to-teal-700 text-xs font-bold text-white transition hover:shadow-lg hover:shadow-emerald-900/20"
              >
                <Award className="h-4 w-4" />
                <span>Claim Certificate</span>
              </Link>
            )}

            <Link
              href={`/courses/${courseId}`}
              className="text-xs font-bold text-gray-400 hover:text-white transition"
            >
              Syllabus Outline
            </Link>
          </footer>
        </div>

        {/* Right: AI Companion Chatbot Panel */}
        <div
          className={`w-80 border-l border-gray-900 bg-gray-950 flex flex-col ${
            showChatMobile
              ? 'fixed inset-y-0 right-0 z-50 flex border-l border-gray-800 shadow-2xl'
              : 'hidden'
          } xl:relative xl:flex`}
        >
          {/* Header */}
          <div className="p-4 border-b border-gray-900 flex items-center justify-between bg-gray-950">
            <div className="flex items-center space-x-2">
              <HelpCircle className="h-5 w-5 text-violet-400 animate-pulse" />
              <div>
                <h4 className="text-xs font-bold text-gray-200">AI Study Companion</h4>
                <p className="text-[10px] text-gray-500">Retrieves context from PDF</p>
              </div>
            </div>
            <button
              onClick={() => setShowChatMobile(false)}
              className="xl:hidden p-1.5 hover:bg-gray-900 rounded text-gray-400 hover:text-white"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          {/* Message List */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {messages.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-center p-4">
                <HelpCircle className="h-8 w-8 text-gray-700 mb-2" />
                <p className="text-[10px] text-gray-500 leading-relaxed">
                  Stuck on this lesson? Ask me questions! I search the PDF chunks to give factual answers.
                </p>
              </div>
            ) : (
              messages.map((msg) => {
                const isAI = msg.sender === 'AI';
                return (
                  <div
                    key={msg.id}
                    className={`flex flex-col max-w-[85%] ${isAI ? 'self-start' : 'ml-auto items-end'}`}
                  >
                    <div
                      className={`p-3 rounded-xl text-[11px] leading-relaxed ${
                        isAI
                          ? 'bg-gray-900/60 border border-gray-900 text-gray-300'
                          : 'bg-violet-600 text-white'
                      }`}
                    >
                      {/* Premium text parser rendering tables, markdown, code, and source badges */}
                      <MessageBubbleRenderer text={msg.messageContent} />
                    </div>
                    <span className="text-[8px] text-gray-600 mt-1 px-1">
                      {isAI ? 'AI Tutor' : 'You'}
                    </span>
                  </div>
                );
              })
            )}
            
            {sendingChat && (
              <div className="flex items-center space-x-2 text-xs text-gray-500 pl-2">
                <Loader2 className="h-3.5 w-3.5 animate-spin" />
                <span>Generating grounded answer...</span>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Suggested/Follow-up Questions Drawer */}
          {messages.length > 0 && !sendingChat && (
            <div className="p-3 border-t border-gray-900/60 bg-gray-950/20 space-y-2">
              <p className="text-[9px] font-bold text-gray-500 uppercase tracking-wider">Suggested Questions</p>
              <div className="flex flex-wrap gap-1.5">
                {suggestedQuestions.map((q, idx) => (
                  <button
                    key={idx}
                    onClick={() => handleSendChat(q)}
                    className="text-[9px] px-2 py-1 bg-gray-900 border border-gray-800 text-gray-400 hover:text-white rounded-lg hover:border-gray-700 transition"
                  >
                    {q}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Input Box */}
          <form onSubmit={(e) => { e.preventDefault(); handleSendChat(chatMessage); }} className="p-4 border-t border-gray-900 bg-gray-950 flex gap-2">
            <input
              type="text"
              placeholder="Ask the AI Tutor..."
              value={chatMessage}
              onChange={(e) => setChatMessage(e.target.value)}
              className="flex-1 px-3 py-2 rounded-lg border border-gray-800 bg-gray-900/20 text-xs text-gray-200 placeholder-gray-500 focus:outline-none focus:border-violet-500"
            />
            <button
              type="submit"
              disabled={sendingChat || !chatMessage.trim()}
              className="p-2.5 rounded-lg bg-violet-600 hover:bg-violet-700 text-white disabled:opacity-50"
            >
              <Send className="h-3.5 w-3.5" />
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

// Markdown and mathematical LaTeX block parser
function MessageBubbleRenderer({ text }: { text: string }) {
  if (!text) return null;

  // Simple and fast markdown formatting
  const lines = text.split("\n");
  const parsedElements = [];

  let inCodeBlock = false;
  let codeBlockContent = "";
  let codeBlockLang = "";

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // Code Block Check
    if (line.startsWith("```")) {
      if (inCodeBlock) {
        // End code block
        inCodeBlock = false;
        parsedElements.push(
          <pre key={`code-${i}`} className="bg-black/40 border border-gray-850 p-3 rounded-lg overflow-x-auto my-2 font-mono text-[10px] text-violet-300">
            <code>{codeBlockContent}</code>
          </pre>
        );
        codeBlockContent = "";
      } else {
        // Start code block
        inCodeBlock = true;
        codeBlockLang = line.substring(3).trim();
      }
      continue;
    }

    if (inCodeBlock) {
      codeBlockContent += line + "\n";
      continue;
    }

    // Block Math Check
    if (line.startsWith("$$") && line.endsWith("$$")) {
      const mathContent = line.slice(2, -2).trim();
      parsedElements.push(
        <div key={`math-block-${i}`} className="my-3 text-center font-serif text-[13px] bg-black/40 p-3 rounded-lg border border-gray-850 text-violet-200 overflow-x-auto">
          {mathContent}
        </div>
      );
      continue;
    }

    // Table Row Check
    if (line.startsWith("|") && line.endsWith("|")) {
      // Parse table row items
      const cols = line.split("|").map(c => c.trim()).filter((c, idx, arr) => idx > 0 && idx < arr.length - 1);
      if (cols.length > 0 && !line.includes("---")) {
        parsedElements.push(
          <div key={`table-${i}`} className="flex border-b border-gray-900/40 py-1 font-semibold text-[10px] text-gray-300">
            {cols.map((col, cIdx) => (
              <span key={cIdx} className="flex-1 px-1 truncate">{col}</span>
            ))}
          </div>
        );
      }
      continue;
    }

    // List Item Check
    if (line.startsWith("- ") || line.startsWith("* ")) {
      parsedElements.push(
        <div key={`list-${i}`} className="flex items-start space-x-2 my-1 pl-2">
          <span className="h-1.5 w-1.5 rounded-full bg-violet-400 mt-1.5 shrink-0" />
          <span>{renderInlineStyles(line.substring(2))}</span>
        </div>
      );
      continue;
    }

    // Header Check
    if (line.startsWith("### ")) {
      parsedElements.push(<h4 key={`h4-${i}`} className="text-xs font-bold text-violet-300 mt-3 mb-1">{renderInlineStyles(line.substring(4))}</h4>);
      continue;
    }
    if (line.startsWith("## ")) {
      parsedElements.push(<h3 key={`h3-${i}`} className="text-sm font-bold text-violet-300 mt-4 mb-2">{renderInlineStyles(line.substring(3))}</h3>);
      continue;
    }

    // Normal Text Paragraph
    if (line.trim().length > 0) {
      parsedElements.push(<p key={`para-${i}`} className="my-1">{renderInlineStyles(line)}</p>);
    }
  }

  return <div className="space-y-1">{parsedElements}</div>;
}

function renderInlineStyles(text: string) {
  if (!text) return "";

  // Highlight bold items (**bold**)
  // Highlight inline code blocks (`code`)
  // Style source references ([Source: Chunk X]) as clickable badges
  const parts = [];
  let remaining = text;

  // Simple regex style match
  const starRegex = /\*\*([^*]+)\*\*/g;
  const backtickRegex = /`([^`]+)`/g;
  const citationRegex = /\[(Source:\s*Chunk\s*\d+)\]/gi;
  const mathRegex = /\$([^$]+)\$/g;

  // Combine patterns
  let match;
  let items: { index: number; length: number; type: 'BOLD' | 'CODE' | 'CITATION' | 'MATH'; value: string }[] = [];

  // Bolds
  while ((match = starRegex.exec(text)) !== null) {
    items.push({ index: match.index, length: match[0].length, type: 'BOLD', value: match[1] });
  }
  // Code
  while ((match = backtickRegex.exec(text)) !== null) {
    items.push({ index: match.index, length: match[0].length, type: 'CODE', value: match[1] });
  }
  // Citation
  while ((match = citationRegex.exec(text)) !== null) {
    items.push({ index: match.index, length: match[0].length, type: 'CITATION', value: match[1] });
  }
  // Math
  while ((match = mathRegex.exec(text)) !== null) {
    items.push({ index: match.index, length: match[0].length, type: 'MATH', value: match[1] });
  }

  // Sort by index ascending
  items.sort((a, b) => a.index - b.index);

  let lastIdx = 0;
  for (const item of items) {
    if (item.index < lastIdx) continue; // skip overlapping
    
    // Add text preceding match
    if (item.index > lastIdx) {
      parts.push(<span key={`text-${lastIdx}`}>{remaining.substring(lastIdx, item.index)}</span>);
    }

    // Add styled match
    if (item.type === 'BOLD') {
      parts.push(<strong key={`bold-${item.index}`} className="font-extrabold text-white">{item.value}</strong>);
    } else if (item.type === 'CODE') {
      parts.push(<code key={`code-${item.index}`} className="bg-black/30 border border-gray-900 px-1 py-0.5 rounded font-mono text-[10px] text-violet-300">{item.value}</code>);
    } else if (item.type === 'CITATION') {
      parts.push(
        <span key={`cite-${item.index}`} className="inline-flex items-center px-1.5 py-0.5 rounded bg-violet-600/25 border border-violet-500/20 text-[9px] text-violet-400 font-extrabold mx-0.5">
          {item.value}
        </span>
      );
    } else if (item.type === 'MATH') {
      parts.push(
        <span key={`math-${item.index}`} className="font-serif italic text-violet-200 mx-0.5 font-medium">
          {item.value}
        </span>
      );
    }

    lastIdx = item.index + item.length;
  }

  if (lastIdx < remaining.length) {
    parts.push(<span key={`text-${lastIdx}`}>{remaining.substring(lastIdx)}</span>);
  }

  return parts.length > 0 ? parts : text;
}
