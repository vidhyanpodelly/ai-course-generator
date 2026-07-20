'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import { evolutionService, Flashcard } from '@/services/evolution';
import { coursesService } from '@/services/courses';
import { Course } from '@/types';
import { motion, AnimatePresence } from 'framer-motion';
import {
  ArrowLeft,
  BookOpen,
  RefreshCw,
  Loader2,
  AlertTriangle,
  HelpCircle,
  TrendingUp,
  Inbox,
  Flame,
  Award,
  Sparkles
} from 'lucide-react';

export default function FlashcardsPage() {
  const params = useParams();
  const courseId = params.courseId as string;

  return (
    <ProtectedRoute>
      <FlashcardsContent courseId={courseId} />
    </ProtectedRoute>
  );
}

function FlashcardsContent({ courseId }: { courseId: string }) {
  const router = useRouter();
  
  const [course, setCourse] = useState<Course | null>(null);
  const [cards, setCards] = useState<Flashcard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [currentIndex, setCurrentIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [reviewingDone, setReviewingDone] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);

  const fetchFlashcards = async () => {
    try {
      setLoading(true);
      setError(null);

      const [courseRes, cardsRes] = await Promise.all([
        coursesService.getCourse(courseId),
        evolutionService.getCourseFlashcards(courseId)
      ]);

      if (courseRes.success) setCourse(courseRes.data);
      if (cardsRes.success) {
        setCards(cardsRes.data);
        if (cardsRes.data.length === 0) {
          // If no cards exist, let's search if chapters exist to trigger generation
          const chaptersRes = await coursesService.getCourseChapters(courseId);
          if (chaptersRes.success && chaptersRes.data.length > 0) {
            // Dynamically generate cards for the first chapter
            const firstChId = chaptersRes.data[0].id;
            const genRes = await evolutionService.getFlashcards(firstChId);
            if (genRes.success) {
              setCards(genRes.data);
            }
          }
        }
      }
    } catch (err: any) {
      setError('Could not load course flashcards. Ensure your backend is running.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchFlashcards();
  }, [courseId]);

  const handleFlip = () => {
    setIsFlipped(!isFlipped);
  };

  const handleReview = async (rating: 'EASY' | 'MEDIUM' | 'HARD') => {
    if (cards.length === 0 || actionLoading) return;
    
    setActionLoading(true);
    const card = cards[currentIndex];
    
    try {
      const response = await evolutionService.reviewFlashcard(card.id, rating);
      if (response.success) {
        // Update local list
        const updated = [...cards];
        updated[currentIndex] = response.data;
        setCards(updated);
      }
      
      // Advance cards index
      if (currentIndex + 1 < cards.length) {
        setIsFlipped(false);
        // Wait minor delay for card slide transition
        setTimeout(() => {
          setCurrentIndex((prev) => prev + 1);
          setActionLoading(false);
        }, 300);
      } else {
        setReviewingDone(true);
        setActionLoading(false);
      }
    } catch (err) {
      setActionLoading(false);
      alert('Failed to submit review.');
    }
  };

  const handleRestart = () => {
    setCurrentIndex(0);
    setIsFlipped(false);
    setReviewingDone(false);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-950 text-gray-200">
        <div className="flex flex-col items-center space-y-4">
          <Loader2 className="h-10 w-10 text-violet-500 animate-spin" />
          <span className="text-sm text-gray-400 font-medium">Extracting concept details and creating flashcards...</span>
        </div>
      </div>
    );
  }

  if (error || !course) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-950 px-4 text-center text-gray-200">
        <AlertTriangle className="h-12 w-12 text-red-500 mb-4 animate-pulse" />
        <h3 className="text-xl font-bold mb-2">Failed to Load Flashcards</h3>
        <p className="text-gray-400 text-sm max-w-sm mb-6">{error}</p>
        <Link href={`/courses/${courseId}`} className="px-5 py-2.5 rounded-xl bg-violet-600 hover:bg-violet-700 text-sm font-semibold transition">
          Back to Syllabus
        </Link>
      </div>
    );
  }

  const currentCard = cards[currentIndex];
  
  // Calculate stats
  const totalCount = cards.length;
  const boxes = [0, 0, 0, 0, 0, 0];
  cards.forEach((c) => {
    boxes[c.box] = (boxes[c.box] || 0) + 1;
  });

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex flex-col pb-20">
      {/* Header */}
      <header className="glass-panel sticky top-0 z-50 px-6 py-4 flex items-center justify-between border-b border-gray-900 bg-gray-950/80">
        <div className="flex items-center space-x-4">
          <Link href={`/courses/${courseId}`} className="p-2 hover:bg-gray-900 rounded-lg text-gray-400 hover:text-white transition">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-sm font-bold text-gray-200">AI Flashcard Review</h1>
            <p className="text-[10px] text-violet-400 font-semibold uppercase">{course.title}</p>
          </div>
        </div>
        
        {totalCount > 0 && !reviewingDone && (
          <span className="text-xs text-gray-400 font-bold bg-gray-900 px-3 py-1.5 rounded-xl border border-gray-800">
            Card {currentIndex + 1} of {totalCount}
          </span>
        )}
      </header>

      {/* Main split dashboard layout */}
      <main className="max-w-5xl mx-auto w-full px-6 pt-10 grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left column: Study Leitner Progress Panels */}
        <div className="space-y-6">
          <div className="glass-card p-6 rounded-2xl border border-gray-900 space-y-4">
            <h3 className="text-sm font-bold text-gray-200 flex items-center space-x-2">
              <Inbox className="h-4.5 w-4.5 text-violet-400" />
              <span>Leitner Progression Box</span>
            </h3>
            <p className="text-[10px] text-gray-500 leading-relaxed">
              Spaced repetition organizes cards based on accuracy. Cards moved to higher boxes are reviewed less frequently.
            </p>
            
            {/* Box indicators */}
            <div className="space-y-3 pt-2">
              {[1, 2, 3, 4, 5].map((bNum) => {
                const count = boxes[bNum] ?? 0;
                const percentage = totalCount > 0 ? (count * 100) / totalCount : 0;
                return (
                  <div key={bNum} className="space-y-1">
                    <div className="flex justify-between text-xs font-semibold text-gray-400">
                      <span className="flex items-center space-x-1">
                        <span className="h-1.5 w-1.5 rounded-full bg-violet-500"></span>
                        <span>Box {bNum}</span>
                      </span>
                      <span>{count} cards</span>
                    </div>
                    <div className="w-full h-1.5 bg-gray-900 rounded-full overflow-hidden">
                      <div className="h-full bg-violet-600 rounded-full transition-all duration-500" style={{ width: `${percentage}%` }}></div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          <div className="glass-card p-6 rounded-2xl border border-gray-900/60 flex items-center justify-between">
            <div className="space-y-1">
              <h4 className="text-xs text-gray-400 font-bold uppercase">Study Streak</h4>
              <p className="text-xl font-bold flex items-center space-x-1.5">
                <Flame className="h-5 w-5 text-amber-500 fill-amber-500" />
                <span>Active study days</span>
              </p>
            </div>
            <div className="text-3xl font-extrabold text-amber-500 bg-amber-500/5 px-4 py-2 rounded-xl border border-amber-500/10">
              ⚡
            </div>
          </div>
        </div>

        {/* Center column: Flashcard Study Frame */}
        <div className="lg:col-span-2 flex flex-col items-center justify-start space-y-8">
          
          {totalCount === 0 ? (
            <div className="w-full glass-panel py-20 text-center rounded-3xl border border-gray-900 flex flex-col items-center justify-center space-y-4">
              <Sparkles className="h-12 w-12 text-gray-700 animate-pulse" />
              <h3 className="text-lg font-bold text-gray-300">Generating Flashcards...</h3>
              <p className="text-xs text-gray-500 max-w-xs leading-relaxed">
                We are compiling cards for this course based on the chapters in the outline. This will take a second.
              </p>
            </div>
          ) : reviewingDone ? (
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              className="w-full glass-panel p-10 text-center rounded-3xl border border-gray-900 flex flex-col items-center justify-center space-y-6"
            >
              <div className="w-16 h-16 bg-emerald-600/10 rounded-full flex items-center justify-center border border-emerald-500/20">
                <Award className="h-8 w-8 text-emerald-400" />
              </div>
              <div>
                <h3 className="text-xl font-extrabold text-gray-200">Session Complete!</h3>
                <p className="text-xs text-gray-500 max-w-sm mx-auto mt-2 leading-relaxed">
                  You completed all flashcard reviews in this course. Excellent work. Your review schedule has been updated using the Leitner repetition boxes.
                </p>
              </div>
              <div className="flex gap-4">
                <button
                  onClick={handleRestart}
                  className="flex items-center space-x-2 px-5 py-2.5 rounded-xl bg-violet-600 hover:bg-violet-700 text-xs font-bold transition shadow-lg shadow-violet-900/20"
                >
                  <RefreshCw className="h-3.5 w-3.5" />
                  <span>Restart Deck</span>
                </button>
                <Link
                  href={`/courses/${courseId}`}
                  className="px-5 py-2.5 rounded-xl border border-gray-800 hover:bg-gray-900 text-xs font-bold transition"
                >
                  Return to Syllabus
                </Link>
              </div>
            </motion.div>
          ) : (
            <div className="w-full space-y-8">
              {/* Flippable Card Container */}
              <div
                className="w-full h-80 relative cursor-pointer"
                onClick={handleFlip}
                style={{ perspective: 1000 }}
              >
                <motion.div
                  className="w-full h-full relative"
                  style={{ transformStyle: 'preserve-3d' }}
                  animate={{ rotateY: isFlipped ? 180 : 0 }}
                  transition={{ duration: 0.6, ease: 'easeInOut' }}
                >
                  {/* Front Side */}
                  <div
                    className="absolute inset-0 bg-gradient-to-br from-gray-900 to-gray-950 border border-gray-800 rounded-3xl p-8 flex flex-col justify-between shadow-2xl backface-hidden"
                    style={{ backfaceVisibility: 'hidden' }}
                  >
                    <span className="text-[9px] uppercase tracking-wider text-violet-400 font-extrabold flex items-center space-x-1.5">
                      <HelpCircle className="h-3 w-3" />
                      <span>Question</span>
                    </span>
                    <p className="text-base font-medium leading-relaxed text-gray-100 flex-1 flex items-center justify-center text-center px-4">
                      {currentCard?.front}
                    </p>
                    <p className="text-[10px] text-gray-500 text-center font-semibold">
                      Click card to reveal answer
                    </p>
                  </div>

                  {/* Back Side */}
                  <div
                    className="absolute inset-0 bg-gradient-to-br from-violet-950/20 to-gray-950 border border-violet-500/10 rounded-3xl p-8 flex flex-col justify-between shadow-2xl backface-hidden"
                    style={{
                      backfaceVisibility: 'hidden',
                      transform: 'rotateY(180deg)'
                    }}
                  >
                    <span className="text-[9px] uppercase tracking-wider text-emerald-400 font-extrabold flex items-center space-x-1.5">
                      <BookOpen className="h-3 w-3" />
                      <span>Answer Explanation</span>
                    </span>
                    <p className="text-sm leading-relaxed text-gray-300 flex-1 flex items-center justify-center text-center px-4 overflow-y-auto">
                      {currentCard?.back}
                    </p>
                    <p className="text-[10px] text-gray-500 text-center font-semibold">
                      Assess review difficulty below
                    </p>
                  </div>
                </motion.div>
              </div>

              {/* Leitner rating action buttons */}
              <AnimatePresence>
                {isFlipped && (
                  <motion.div
                    initial={{ y: 20, opacity: 0 }}
                    animate={{ y: 0, opacity: 1 }}
                    exit={{ y: 20, opacity: 0 }}
                    className="flex flex-col items-center space-y-4"
                  >
                    <p className="text-xs text-gray-400 font-semibold">How easy was it to recall?</p>
                    <div className="flex gap-4 w-full">
                      <button
                        onClick={() => handleReview('HARD')}
                        disabled={actionLoading}
                        className="flex-1 py-3 rounded-xl border border-red-500/20 bg-red-500/5 hover:bg-red-500/10 text-xs font-bold text-red-400 transition"
                      >
                        Hard (Repeat soon)
                      </button>
                      <button
                        onClick={() => handleReview('MEDIUM')}
                        disabled={actionLoading}
                        className="flex-1 py-3 rounded-xl border border-amber-500/20 bg-amber-500/5 hover:bg-amber-500/10 text-xs font-bold text-amber-400 transition"
                      >
                        Medium
                      </button>
                      <button
                        onClick={() => handleReview('EASY')}
                        disabled={actionLoading}
                        className="flex-1 py-3 rounded-xl border border-emerald-500/20 bg-emerald-500/5 hover:bg-emerald-500/10 text-xs font-bold text-emerald-400 transition"
                      >
                        Easy (Advance box)
                      </button>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
