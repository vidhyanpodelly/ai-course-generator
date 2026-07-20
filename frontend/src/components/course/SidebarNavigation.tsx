'use client';

import React from 'react';
import Link from 'next/link';
import { BookOpen, CheckCircle, ChevronDown, ChevronRight, FileText } from 'lucide-react';
import { Chapter, Lesson } from '@/types';

interface SidebarNavigationProps {
  courseId: string;
  chapters: Chapter[];
  currentLessonId: string;
  completedLessonIds: string[];
}

export const SidebarNavigation: React.FC<SidebarNavigationProps> = ({
  courseId,
  chapters,
  currentLessonId,
  completedLessonIds
}) => {
  return (
    <div className="w-80 h-[calc(100vh-4.5rem)] shrink-0 border-r border-gray-900 bg-gray-950 overflow-y-auto px-4 py-6 hidden lg:block">
      <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wide px-3 mb-4">Syllabus Outline</h3>
      
      <div className="space-y-4">
        {chapters.map((chapter) => (
          <div key={chapter.id} className="space-y-2">
            <div className="flex items-start space-x-2 px-3 py-1">
              <BookOpen className="h-4.5 w-4.5 text-violet-500 shrink-0 mt-0.5" />
              <div>
                <p className="text-xs text-gray-500 font-bold uppercase">Ch. {chapter.sequenceNumber}</p>
                <h4 className="text-xs font-bold text-gray-300 leading-tight">{chapter.title}</h4>
              </div>
            </div>
            
            <div className="space-y-1 pl-6">
              {chapter.lessons.map((lesson) => {
                const isActive = lesson.id === currentLessonId;
                const isCompleted = completedLessonIds.includes(lesson.id);
                return (
                  <Link
                    key={lesson.id}
                    href={`/courses/${courseId}/lesson/${lesson.id}`}
                    className={`group flex items-center justify-between px-3 py-2 rounded-lg text-xs font-medium transition ${
                      isActive
                        ? 'bg-violet-600 text-white shadow-lg shadow-violet-900/10'
                        : 'text-gray-400 hover:bg-gray-900 hover:text-gray-200'
                    }`}
                  >
                    <span className="truncate pr-2">{lesson.title}</span>
                    <span className="shrink-0">
                      <CheckCircle
                        className={`h-3.5 w-3.5 ${
                          isCompleted
                            ? isActive
                              ? 'text-white'
                              : 'text-emerald-500'
                            : 'text-gray-800 group-hover:text-gray-600'
                        }`}
                      />
                    </span>
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
