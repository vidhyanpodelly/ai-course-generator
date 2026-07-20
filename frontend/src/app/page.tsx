'use client';

import React from 'react';
import Link from 'next/link';
import { BookOpen, Brain, Sparkles, HelpCircle, GraduationCap, ChevronRight } from 'lucide-react';
import { useAuth } from '@/context/AuthContext';

export default function LandingPage() {
  const { isAuthenticated } = useAuth();

  return (
    <div className="min-h-screen relative overflow-hidden bg-gray-950">
      {/* Background Gradients */}
      <div className="absolute top-[-10%] left-[-10%] w-[50%] h-[50%] bg-violet-900/10 rounded-full blur-[120px]"></div>
      <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] bg-emerald-950/10 rounded-full blur-[120px]"></div>

      {/* Header */}
      <header className="glass-panel sticky top-0 z-50 px-6 py-4 flex items-center justify-between border-b border-gray-900">
        <Link href="/" className="flex items-center space-x-2 text-xl font-bold tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-violet-400 to-indigo-200">
          <GraduationCap className="h-6 w-6 text-violet-500" />
          <span>Curricula.AI</span>
        </Link>
        <nav className="flex items-center space-x-4">
          {isAuthenticated ? (
            <Link href="/dashboard" className="px-4 py-2 rounded-lg bg-violet-600 hover:bg-violet-700 text-sm font-semibold transition-all duration-300 shadow-[0_0_15px_rgba(124,58,237,0.3)]">
              Dashboard
            </Link>
          ) : (
            <>
              <Link href="/login" className="text-gray-300 hover:text-white text-sm font-semibold transition-colors">
                Sign In
              </Link>
              <Link href="/register" className="glow-button px-4 py-2 rounded-lg bg-violet-600 hover:bg-violet-700 text-sm font-semibold transition-all duration-300 shadow-[0_0_15px_rgba(124,58,237,0.3)]">
                Get Started
              </Link>
            </>
          )}
        </nav>
      </header>

      {/* Hero Section */}
      <main className="max-w-7xl mx-auto px-6 pt-20 pb-32 flex flex-col items-center text-center relative z-10">
        <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full border border-violet-500/30 bg-violet-500/5 text-xs text-violet-300 mb-8 animate-pulse">
          <Sparkles className="h-3 w-3" />
          <span>Powered by Groq & LLaMA 3</span>
        </div>
        
        <h1 className="text-5xl md:text-7xl font-extrabold tracking-tight mb-6 bg-clip-text text-transparent bg-gradient-to-b from-white to-gray-400">
          Turn Any PDF Into An<br />
          <span className="bg-clip-text text-transparent bg-gradient-to-r from-violet-400 via-indigo-300 to-emerald-400">
            Interactive E-Course
          </span>
        </h1>
        
        <p className="text-lg md:text-xl text-gray-400 max-w-2xl mb-12">
          Upload books, research papers, or study guides. Curricula.AI parses documents dynamically, builds structured lesson plans, generates quizzes, and provides a RAG-backed tutor.
        </p>

        <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
          <Link href={isAuthenticated ? "/dashboard" : "/register"} className="glow-button flex items-center space-x-2 px-8 py-4 rounded-xl bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 font-semibold shadow-lg shadow-violet-900/30 transition-all duration-300">
            <span>Create Your First Course</span>
            <ChevronRight className="h-4 w-4" />
          </Link>
          <Link href="/login" className="px-8 py-4 rounded-xl border border-gray-800 bg-gray-950/50 hover:bg-gray-900/50 font-semibold transition-colors duration-300">
            View Live Demo
          </Link>
        </div>

        {/* Feature Grid */}
        <section className="grid grid-cols-1 md:grid-cols-3 gap-8 mt-32 w-full">
          <div className="glass-card p-8 rounded-2xl text-left">
            <div className="w-12 h-12 bg-violet-500/10 border border-violet-500/30 rounded-xl flex items-center justify-center mb-6">
              <BookOpen className="h-6 w-6 text-violet-400" />
            </div>
            <h3 className="text-xl font-bold mb-3">Modular Curriculums</h3>
            <p className="text-gray-400 text-sm leading-relaxed">
              Dynamically split large books into chapters, lessons, and subtopics. Navigate with a course outline sidebar designed for retention.
            </p>
          </div>

          <div className="glass-card p-8 rounded-2xl text-left">
            <div className="w-12 h-12 bg-indigo-500/10 border border-indigo-500/30 rounded-xl flex items-center justify-center mb-6">
              <Brain className="h-6 w-6 text-indigo-400" />
            </div>
            <h3 className="text-xl font-bold mb-3">Adaptive Quiz Engine</h3>
            <p className="text-gray-400 text-sm leading-relaxed">
              Verify your comprehension with automatically generated chapter-end quizzes including MCQs, true/false, and short answers with inline explanations.
            </p>
          </div>

          <div className="glass-card p-8 rounded-2xl text-left">
            <div className="w-12 h-12 bg-emerald-500/10 border border-emerald-500/30 rounded-xl flex items-center justify-center mb-6">
              <HelpCircle className="h-6 w-6 text-emerald-400" />
            </div>
            <h3 className="text-xl font-bold mb-3">Cognitive AI Chat Tutor</h3>
            <p className="text-gray-400 text-sm leading-relaxed">
              Stuck on a difficult concept? Query our contextual chatbot. It retrieves target PDF segments using fast Full-Text search vectors.
            </p>
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="border-t border-gray-900 py-8 px-6 text-center text-sm text-gray-500 relative z-10 bg-gray-950/80">
        &copy; {new Date().getFullYear()} Curricula.AI. Built for professional software engineering. All rights reserved.
      </footer>
    </div>
  );
}
