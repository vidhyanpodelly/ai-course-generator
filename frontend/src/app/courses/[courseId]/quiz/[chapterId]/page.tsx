'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import { quizzesService } from '@/services/quizzes';
import { QuizQuestion, QuizAttempt } from '@/types';
import {
  ArrowLeft,
  Trophy,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RefreshCw,
  Award,
  HelpCircle,
  BookOpen
} from 'lucide-react';

export default function ChapterQuizPage() {
  const params = useParams();
  const courseId = params.courseId as string;
  const chapterId = params.chapterId as string;

  return (
    <ProtectedRoute>
      <ChapterQuizContent courseId={courseId} chapterId={chapterId} />
    </ProtectedRoute>
  );
}

function ChapterQuizContent({ courseId, chapterId }: { courseId: string; chapterId: string }) {
  const router = useRouter();

  const [questions, setQuestions] = useState<QuizQuestion[]>([]);
  const [answers, setAnswers] = useState<Record<string, string>>({});
  const [attempt, setAttempt] = useState<QuizAttempt | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const fetchQuestions = async () => {
    try {
      setLoading(true);
      setError(null);
      setAttempt(null);
      setAnswers({});
      
      const response = await quizzesService.getQuizQuestions(chapterId);
      if (response.success) {
        setQuestions(response.data);
      }
    } catch (err: any) {
      setError('Failed to fetch quiz questions. Make sure the database has content and AI prompts are accessible.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchQuestions();
  }, [chapterId]);

  const handleSelectOption = (questionIdx: number, option: string) => {
    if (attempt) return; // Locked on submission
    setAnswers((prev) => ({
      ...prev,
      [String(questionIdx)]: option
    }));
  };

  const handleShortAnswerChange = (questionIdx: number, val: string) => {
    if (attempt) return;
    setAnswers((prev) => ({
      ...prev,
      [String(questionIdx)]: val
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (attempt || submitting) return;

    setSubmitting(true);
    setError(null);
    try {
      const response = await quizzesService.submitQuiz(chapterId, answers);
      if (response.success) {
        setAttempt(response.data);
      }
    } catch (err) {
      setError('Could not grade quiz submission. Please check connection.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-950">
        <div className="flex flex-col items-center space-y-4">
          <RefreshCw className="h-10 w-10 text-violet-500 animate-spin" />
          <span className="text-sm text-gray-400 font-medium">AI generating chapter quiz questions...</span>
        </div>
      </div>
    );
  }

  if (error || questions.length === 0) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-950 px-4 text-center">
        <AlertTriangle className="h-12 w-12 text-red-500 mb-4 animate-pulse" />
        <h3 className="text-xl font-bold mb-2">Error Loading Quiz</h3>
        <p className="text-gray-400 text-sm max-w-sm mb-6">{error || 'Chapter quiz questions are currently unavailable.'}</p>
        <Link href={`/courses/${courseId}`} className="px-5 py-2.5 rounded-xl bg-violet-600 hover:bg-violet-700 text-sm font-semibold transition">
          Back to Syllabus
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 pb-20">
      {/* Header */}
      <header className="glass-panel sticky top-0 z-50 px-6 py-4 flex items-center justify-between border-b border-gray-900 bg-gray-950/80">
        <div className="flex items-center space-x-4">
          <Link href={`/courses/${courseId}`} className="p-2 hover:bg-gray-900 rounded-lg text-gray-400 hover:text-white transition">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-sm font-bold text-gray-200">Chapter Quiz Sheet</h1>
            <p className="text-[10px] text-gray-400 uppercase">Interactive Assessment</p>
          </div>
        </div>
      </header>

      <main className="max-w-3xl mx-auto px-6 mt-10">
        {/* Score banner */}
        {attempt && (
          <section className="glass-panel p-8 rounded-2xl border border-violet-500/20 bg-violet-500/5 mb-10 flex flex-col items-center text-center space-y-3">
            <div className="w-16 h-16 bg-violet-600/10 border border-violet-500/30 rounded-full flex items-center justify-center">
              <Award className="h-8 w-8 text-violet-400" />
            </div>
            <div>
              <h2 className="text-2xl font-extrabold text-white">Assessment Graded</h2>
              <p className="text-sm text-gray-400 mt-1">
                You scored <span className="text-violet-400 font-bold">{attempt.score}</span> out of <span className="text-violet-400 font-bold">{attempt.totalQuestions}</span> questions correctly.
              </p>
            </div>
            <div className="flex gap-4 pt-2">
              <button
                onClick={fetchQuestions}
                className="px-4 py-2 rounded-lg bg-gray-900 border border-gray-800 text-xs font-semibold text-gray-300 hover:bg-gray-800 transition"
              >
                Retry Quiz
              </button>
              <Link
                href={`/courses/${courseId}`}
                className="px-4 py-2 rounded-lg bg-violet-600 hover:bg-violet-700 text-xs font-semibold transition"
              >
                Return to Syllabus
              </Link>
            </div>
          </section>
        )}

        {/* Questions list */}
        <form onSubmit={handleSubmit} className="space-y-8">
          {questions.map((question, qIdx) => {
            const userAnswer = answers[String(qIdx)] || '';
            const isCorrect = attempt && userAnswer.trim().toLowerCase().includes(question.correctAnswer.trim().toLowerCase());
            
            return (
              <div
                key={qIdx}
                className={`glass-panel p-6 md:p-8 rounded-2xl border transition duration-300 ${
                  attempt
                    ? isCorrect
                      ? 'border-emerald-500/30 bg-emerald-500/5'
                      : 'border-red-500/30 bg-red-500/5'
                    : 'border-gray-900'
                }`}
              >
                {/* Question Info */}
                <div className="flex items-start space-x-3 mb-6">
                  <span className="text-xs font-bold text-violet-400 shrink-0 mt-1">Q{qIdx + 1}.</span>
                  <h4 className="font-bold text-gray-100 text-base leading-snug">{question.questionText}</h4>
                </div>

                {/* Question Inputs */}
                {question.type === 'MCQ' && question.options && (
                  <div className="grid grid-cols-1 gap-3.5 pl-7">
                    {question.options.map((opt, oIdx) => {
                      const isSelected = userAnswer === opt;
                      const isOptionCorrect = attempt && opt.trim().toLowerCase() === question.correctAnswer.trim().toLowerCase();
                      const isOptionWrongSelected = attempt && isSelected && !isCorrect;

                      return (
                        <button
                          key={oIdx}
                          type="button"
                          disabled={!!attempt}
                          onClick={() => handleSelectOption(qIdx, opt)}
                          className={`w-full text-left p-3.5 rounded-xl border text-xs font-semibold transition flex items-center justify-between gap-3 ${
                            attempt
                              ? isOptionCorrect
                                ? 'bg-emerald-600/10 border-emerald-500 text-emerald-400'
                                : isOptionWrongSelected
                                  ? 'bg-red-600/10 border-red-500 text-red-400'
                                  : 'bg-gray-900/10 border-gray-800 text-gray-500'
                              : isSelected
                                ? 'bg-violet-600/10 border-violet-500 text-violet-400'
                                : 'bg-gray-900/30 border-gray-900 text-gray-300 hover:bg-gray-900/50'
                          }`}
                        >
                          <span>{opt}</span>
                          {attempt && (
                            <span>
                              {isOptionCorrect && <CheckCircle2 className="h-4.5 w-4.5 shrink-0" />}
                              {isOptionWrongSelected && <XCircle className="h-4.5 w-4.5 shrink-0" />}
                            </span>
                          )}
                        </button>
                      );
                    })}
                  </div>
                )}

                {question.type === 'TRUE_FALSE' && (
                  <div className="flex gap-4 pl-7">
                    {['True', 'False'].map((opt) => {
                      const isSelected = userAnswer === opt;
                      const isOptionCorrect = attempt && opt.trim().toLowerCase() === question.correctAnswer.trim().toLowerCase();
                      const isOptionWrongSelected = attempt && isSelected && !isCorrect;

                      return (
                        <button
                          key={opt}
                          type="button"
                          disabled={!!attempt}
                          onClick={() => handleSelectOption(qIdx, opt)}
                          className={`px-6 py-3 rounded-xl border text-xs font-bold transition flex items-center space-x-2 ${
                            attempt
                              ? isOptionCorrect
                                ? 'bg-emerald-600/10 border-emerald-500 text-emerald-400'
                                : isOptionWrongSelected
                                  ? 'bg-red-600/10 border-red-500 text-red-400'
                                  : 'bg-gray-900/10 border-gray-800 text-gray-500'
                              : isSelected
                                ? 'bg-violet-600/10 border-violet-500 text-violet-400'
                                : 'bg-gray-900/30 border-gray-900 text-gray-300 hover:bg-gray-900/50'
                          }`}
                        >
                          <span>{opt}</span>
                        </button>
                      );
                    })}
                  </div>
                )}

                {question.type === 'SHORT_ANSWER' && (
                  <div className="pl-7 space-y-2">
                    <input
                      type="text"
                      disabled={!!attempt}
                      placeholder="Type your answer here..."
                      value={userAnswer}
                      onChange={(e) => handleShortAnswerChange(qIdx, e.target.value)}
                      className={`w-full max-w-md px-4 py-3 rounded-xl border bg-gray-900/30 text-gray-100 text-xs focus:outline-none focus:ring-2 focus:ring-violet-900/30 focus:border-violet-500 transition-all ${
                        attempt
                          ? isCorrect
                            ? 'border-emerald-500 text-emerald-400'
                            : 'border-red-500 text-red-400'
                          : 'border-gray-900'
                      }`}
                    />
                    {attempt && !isCorrect && (
                      <p className="text-[10px] text-emerald-400 font-semibold mt-1">
                        Correct Answer: <span className="font-bold">{question.correctAnswer}</span>
                      </p>
                    )}
                  </div>
                )}

                {/* Explanation text block */}
                {attempt && (
                  <div className="mt-6 pt-4 border-t border-gray-900 pl-7 text-[11px] text-gray-400 flex items-start space-x-2">
                    <HelpCircle className="h-4.5 w-4.5 text-violet-400 shrink-0 mt-0.5" />
                    <p className="leading-relaxed">{question.explanation}</p>
                  </div>
                )}
              </div>
            );
          })}

          {/* Submit Action */}
          {!attempt && (
            <button
              type="submit"
              disabled={submitting}
              className="glow-button w-full py-4 px-4 rounded-2xl bg-gradient-to-r from-violet-600 to-indigo-600 text-sm font-bold text-white transition disabled:opacity-50 flex items-center justify-center space-x-2"
            >
              {submitting ? (
                <>
                  <RefreshCw className="h-5 w-5 animate-spin" />
                  <span>Submitting assessment...</span>
                </>
              ) : (
                <span>Submit Answers</span>
              )}
            </button>
          )}
        </form>
      </main>
    </div>
  );
}
