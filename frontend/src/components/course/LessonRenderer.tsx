'use client';

import React from 'react';

interface LessonRendererProps {
  explanationText: string;
}

export const LessonRenderer: React.FC<LessonRendererProps> = ({ explanationText }) => {
  if (!explanationText) return null;

  // Simple, robust client-side parser to transform markdown blocks into styled JSX
  const lines = explanationText.split('\n');

  return (
    <div className="space-y-4 text-gray-300 leading-relaxed text-sm">
      {lines.map((line, idx) => {
        const trimmed = line.trim();
        
        if (trimmed.startsWith('####')) {
          return (
            <h5 key={idx} className="text-sm font-bold text-gray-200 mt-6 mb-2 uppercase tracking-wide">
              {parseInlineMarkdown(trimmed.replace(/^####\s*/, ''))}
            </h5>
          );
        }

        if (trimmed.startsWith('###')) {
          return (
            <h4 key={idx} className="text-base font-bold text-violet-400 mt-8 mb-3">
              {parseInlineMarkdown(trimmed.replace(/^###\s*/, ''))}
            </h4>
          );
        }

        if (trimmed.startsWith('##')) {
          return (
            <h3 key={idx} className="text-lg font-bold text-violet-300 mt-8 mb-4 border-b border-gray-900 pb-2">
              {parseInlineMarkdown(trimmed.replace(/^##\s*/, ''))}
            </h3>
          );
        }

        if (trimmed.startsWith('#')) {
          return (
            <h2 key={idx} className="text-xl font-extrabold text-white mt-10 mb-6 bg-clip-text text-transparent bg-gradient-to-r from-violet-300 to-indigo-200">
              {parseInlineMarkdown(trimmed.replace(/^#\s*/, ''))}
            </h2>
          );
        }

        // Ordered/Unordered Lists
        if (trimmed.startsWith('-') || trimmed.startsWith('*')) {
          return (
            <div key={idx} className="flex items-start space-x-2 pl-4">
              <span className="text-violet-500 shrink-0 mt-1.5 font-bold">•</span>
              <p>{parseInlineMarkdown(trimmed.replace(/^[-*]\s*/, ''))}</p>
            </div>
          );
        }

        if (/^\d+\./.test(trimmed)) {
          const numMatch = trimmed.match(/^(\d+)\./);
          const num = numMatch ? numMatch[1] : '1';
          return (
            <div key={idx} className="flex items-start space-x-2 pl-4">
              <span className="text-violet-400 font-bold shrink-0 mt-0.5">{num}.</span>
              <p>{parseInlineMarkdown(trimmed.replace(/^\d+\.\s*/, ''))}</p>
            </div>
          );
        }

        if (!trimmed) {
          return <div key={idx} className="h-2"></div>;
        }

        // Paragraphs
        return (
          <p key={idx} className="leading-relaxed text-gray-300">
            {parseInlineMarkdown(trimmed)}
          </p>
        );
      })}
    </div>
  );
};

// Sub-parser for inline formatting like bolding (**text**)
function parseInlineMarkdown(text: string) {
  const parts = [];
  let remaining = text;
  
  // Match **bold**
  const regex = /\*\*(.*?)\*\*/g;
  let match;
  let lastIndex = 0;

  while ((match = regex.exec(remaining)) !== null) {
    // text before match
    const before = remaining.substring(lastIndex, match.index);
    if (before) parts.push(before);
    
    // bolded match
    parts.push(
      <strong key={match.index} className="font-bold text-white">
        {match[1]}
      </strong>
    );
    lastIndex = regex.lastIndex;
  }

  const rest = remaining.substring(lastIndex);
  if (rest) parts.push(rest);

  return parts.length > 0 ? parts : text;
}
