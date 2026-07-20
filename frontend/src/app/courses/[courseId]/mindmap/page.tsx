'use client';

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { useParams, useRouter } from 'next/navigation';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import { evolutionService } from '@/services/evolution';
import { coursesService } from '@/services/courses';
import { Course, Chapter } from '@/types';
import mermaid from 'mermaid';
import {
  ArrowLeft,
  Loader2,
  AlertTriangle,
  ZoomIn,
  ZoomOut,
  Maximize2,
  Download,
  GitFork,
  Workflow,
  ArrowRightLeft,
  Blocks,
  Network,
  Share2
} from 'lucide-react';

export default function MindMapPage() {
  const params = useParams();
  const courseId = params.courseId as string;

  return (
    <ProtectedRoute>
      <MindMapContent courseId={courseId} />
    </ProtectedRoute>
  );
}

function MindMapContent({ courseId }: { courseId: string }) {
  const router = useRouter();

  const [course, setCourse] = useState<Course | null>(null);
  const [chapters, setChapters] = useState<Chapter[]>([]);
  const [selectedChapterId, setSelectedChapterId] = useState<string>('');
  const [selectedType, setSelectedType] = useState<'MINDMAP' | 'FLOWCHART' | 'SEQUENCE' | 'CLASS' | 'ERD'>('MINDMAP');
  
  const [mermaidCode, setMermaidCode] = useState<string>('');
  const [svgContent, setSvgContent] = useState<string>('');
  const [loading, setLoading] = useState(true);
  const [loadingDiagram, setLoadingDiagram] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [renderError, setRenderError] = useState<string | null>(null);
  
  const [zoomScale, setZoomScale] = useState(1);
  const [panOffset, setPanOffset] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const dragStartRef = useRef({ x: 0, y: 0 });
  const canvasContainerRef = useRef<HTMLDivElement>(null);

  const handleMouseDown = (e: React.MouseEvent) => {
    if (e.button !== 0) return;
    setIsDragging(true);
    dragStartRef.current = { x: e.clientX - panOffset.x, y: e.clientY - panOffset.y };
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging) return;
    setPanOffset({
      x: e.clientX - dragStartRef.current.x,
      y: e.clientY - dragStartRef.current.y
    });
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  // Initialize Mermaid once
  useEffect(() => {
    try {
      mermaid.initialize({
        startOnLoad: false,
        theme: 'dark',
        securityLevel: 'loose',
        themeVariables: {
          background: '#0b0f19',
          primaryColor: '#8b5cf6',
          primaryTextColor: '#fff',
          lineColor: '#4b5563'
        }
      });
    } catch (e) {
      console.error("Failed to initialize Mermaid", e);
    }
  }, []);

  const fetchCourseData = async () => {
    try {
      setLoading(true);
      setError(null);

      const [courseRes, chaptersRes] = await Promise.all([
        coursesService.getCourse(courseId),
        coursesService.getCourseChapters(courseId)
      ]);

      if (courseRes.success) setCourse(courseRes.data);
      if (chaptersRes.success) {
        setChapters(chaptersRes.data);
        if (chaptersRes.data.length > 0) {
          setSelectedChapterId(chaptersRes.data[0].id);
        }
      }
    } catch (err) {
      setError('Could not load chapters.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCourseData();
  }, [courseId]);

  const loadDiagram = async () => {
    if (!selectedChapterId) return;

    try {
      setLoadingDiagram(true);
      setRenderError(null);
      setSvgContent('');
      
      let data = '';
      if (selectedType === 'MINDMAP') {
        const response = await evolutionService.getMindMap(selectedChapterId);
        if (response.success) data = response.data.mermaidData;
      } else {
        const response = await evolutionService.getDiagram(selectedChapterId, selectedType);
        if (response.success) data = response.data.mermaidData;
      }

      setMermaidCode(data);

    } catch (err) {
      setRenderError('Failed to fetch diagram structure from AI.');
    } finally {
      setLoadingDiagram(false);
    }
  };

  useEffect(() => {
    loadDiagram();
  }, [selectedChapterId, selectedType]);

  // Compile Mermaid to SVG
  useEffect(() => {
    if (!mermaidCode) return;

    const renderMermaid = async () => {
      try {
        setRenderError(null);
        // Chroma/Mermaid unique ID per compile
        const uniqueId = 'mermaid-svg-' + Math.random().toString(36).substring(2, 9);
        const { svg } = await mermaid.render(uniqueId, mermaidCode);
        setSvgContent(svg);
      } catch (err: any) {
        console.error(err);
        setRenderError('Mermaid layout compilation failed. Please try a different diagram type.');
      }
    };

    renderMermaid();
  }, [mermaidCode]);

  const handleZoom = (direction: 'IN' | 'OUT' | 'RESET') => {
    if (direction === 'IN') {
      setZoomScale((prev) => Math.min(2.5, prev + 0.15));
    } else if (direction === 'OUT') {
      setZoomScale((prev) => Math.max(0.4, prev - 0.15));
    } else {
      setZoomScale(1);
      setPanOffset({ x: 0, y: 0 });
    }
  };

  const downloadSVG = () => {
    if (!svgContent) return;
    const stylingOverride = `
      <style>
        svg { background-color: #0b0f19 !important; font-family: 'Outfit', 'Inter', sans-serif !important; }
        .node rect, .node circle, .node polygon, .node path { fill: #120e2e !important; stroke: #8b5cf6 !important; stroke-width: 2px !important; }
        .node .label { color: #ffffff !important; font-weight: 600 !important; }
        .edgePath .path { stroke: #6b21a8 !important; stroke-width: 2px !important; }
        .edgeLabel rect { fill: #0b0f19 !important; }
        .edgeLabel span { color: #c084fc !important; font-weight: 500 !important; }
        .actor { fill: #120e2e !important; stroke: #8b5cf6 !important; }
        text.actor { fill: #ffffff !important; stroke: none !important; }
        .actor-line { stroke: #4b5563 !important; }
        .messageLine0, .messageLine1 { stroke: #8b5cf6 !important; }
        #arrowhead-main { fill: #8b5cf6 !important; }
        .labelBox { fill: #120e2e !important; stroke: #8b5cf6 !important; }
        .labelText { fill: #ffffff !important; }
        .loopLine { stroke: #8b5cf6 !important; }
        .loopBox { fill: #120e2e !important; stroke: #8b5cf6 !important; }
      </style>
    `;
    const styledSvg = svgContent.replace(/>/, `>${stylingOverride}`);
    const blob = new Blob([styledSvg], { type: 'image/svg+xml' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `diagram_${selectedType.toLowerCase()}_${selectedChapterId}.svg`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-950 text-gray-200">
        <div className="flex flex-col items-center space-y-4">
          <Loader2 className="h-10 w-10 text-violet-500 animate-spin" />
          <span className="text-sm text-gray-400 font-medium">Loading syllabus structure...</span>
        </div>
      </div>
    );
  }

  if (error || !course) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-950 px-4 text-center text-gray-200">
        <AlertTriangle className="h-12 w-12 text-red-500 mb-4 animate-pulse" />
        <h3 className="text-xl font-bold mb-2">Error Loading Diagrams</h3>
        <p className="text-gray-400 text-sm max-w-sm mb-6">{error}</p>
        <Link href={`/courses/${courseId}`} className="px-5 py-2.5 rounded-xl bg-violet-600 hover:bg-violet-700 text-sm font-semibold transition">
          Back to Syllabus
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex flex-col">
      {/* Header */}
      <header className="glass-panel sticky top-0 z-50 px-6 py-4 flex items-center justify-between border-b border-gray-900 bg-gray-950/80">
        <div className="flex items-center space-x-4">
          <Link href={`/courses/${courseId}`} className="p-2 hover:bg-gray-900 rounded-lg text-gray-400 hover:text-white transition">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-sm font-bold text-gray-200">AI Visual Mind Maps & Diagrams</h1>
            <p className="text-[10px] text-violet-400 font-semibold uppercase">{course.title}</p>
          </div>
        </div>
      </header>

      {/* Main split work board */}
      <div className="flex-1 flex flex-col md:flex-row overflow-hidden">
        {/* Left Control Sidebar */}
        <div className="w-full md:w-80 border-b md:border-b-0 md:border-r border-gray-900 bg-gray-950 p-6 space-y-6 shrink-0 overflow-y-auto">
          {/* Chapter Selector */}
          <div className="space-y-2">
            <label className="text-xs font-bold text-gray-400 uppercase">Chapter Topic</label>
            <select
              value={selectedChapterId}
              onChange={(e) => setSelectedChapterId(e.target.value)}
              className="w-full px-3 py-2.5 rounded-xl border border-gray-800 bg-gray-900/50 text-xs text-gray-200 focus:outline-none focus:border-violet-500"
            >
              {chapters.map((ch) => (
                <option key={ch.id} value={ch.id}>
                  Chapter {ch.sequenceNumber}: {ch.title}
                </option>
              ))}
            </select>
          </div>

          {/* Diagram Type Selector */}
          <div className="space-y-3">
            <label className="text-xs font-bold text-gray-400 uppercase">Visual Layout Type</label>
            <div className="grid grid-cols-1 gap-2">
              <button
                onClick={() => setSelectedType('MINDMAP')}
                className={`flex items-center space-x-2.5 px-4 py-3 rounded-xl border text-left text-xs font-semibold transition ${
                  selectedType === 'MINDMAP'
                    ? 'border-violet-500/30 bg-violet-600/10 text-violet-400'
                    : 'border-gray-900 bg-gray-900/10 text-gray-400 hover:text-white hover:border-gray-800'
                }`}
              >
                <Network className="h-4 w-4 shrink-0" />
                <span>Mind Map</span>
              </button>
              
              <button
                onClick={() => setSelectedType('FLOWCHART')}
                className={`flex items-center space-x-2.5 px-4 py-3 rounded-xl border text-left text-xs font-semibold transition ${
                  selectedType === 'FLOWCHART'
                    ? 'border-violet-500/30 bg-violet-600/10 text-violet-400'
                    : 'border-gray-900 bg-gray-900/10 text-gray-400 hover:text-white hover:border-gray-800'
                }`}
              >
                <Workflow className="h-4 w-4 shrink-0" />
                <span>Concept Flowchart</span>
              </button>

              <button
                onClick={() => setSelectedType('SEQUENCE')}
                className={`flex items-center space-x-2.5 px-4 py-3 rounded-xl border text-left text-xs font-semibold transition ${
                  selectedType === 'SEQUENCE'
                    ? 'border-violet-500/30 bg-violet-600/10 text-violet-400'
                    : 'border-gray-900 bg-gray-900/10 text-gray-400 hover:text-white hover:border-gray-800'
                }`}
              >
                <ArrowRightLeft className="h-4 w-4 shrink-0" />
                <span>Sequence Diagram</span>
              </button>

              <button
                onClick={() => setSelectedType('CLASS')}
                className={`flex items-center space-x-2.5 px-4 py-3 rounded-xl border text-left text-xs font-semibold transition ${
                  selectedType === 'CLASS'
                    ? 'border-violet-500/30 bg-violet-600/10 text-violet-400'
                    : 'border-gray-900 bg-gray-900/10 text-gray-400 hover:text-white hover:border-gray-800'
                }`}
              >
                <Blocks className="h-4 w-4 shrink-0" />
                <span>Class Structure</span>
              </button>

              <button
                onClick={() => setSelectedType('ERD')}
                className={`flex items-center space-x-2.5 px-4 py-3 rounded-xl border text-left text-xs font-semibold transition ${
                  selectedType === 'ERD'
                    ? 'border-violet-500/30 bg-violet-600/10 text-violet-400'
                    : 'border-gray-900 bg-gray-900/10 text-gray-400 hover:text-white hover:border-gray-800'
                }`}
              >
                <GitFork className="h-4 w-4 shrink-0" />
                <span>Entity Relationship (ERD)</span>
              </button>
            </div>
          </div>
        </div>

        {/* Right Canvas Area */}
        <div className="flex-1 flex flex-col bg-gray-950 relative overflow-hidden">
          {/* Canvas Utilities toolbar */}
          {svgContent && !loadingDiagram && (
            <div className="absolute top-4 right-4 z-10 flex items-center space-x-2 bg-gray-900/90 border border-gray-850 p-2 rounded-xl backdrop-blur">
              <button
                onClick={() => handleZoom('IN')}
                className="p-2 hover:bg-gray-800 rounded-lg text-gray-400 hover:text-white transition"
                title="Zoom In"
              >
                <ZoomIn className="h-4 w-4" />
              </button>
              <button
                onClick={() => handleZoom('OUT')}
                className="p-2 hover:bg-gray-800 rounded-lg text-gray-400 hover:text-white transition"
                title="Zoom Out"
              >
                <ZoomOut className="h-4 w-4" />
              </button>
              <button
                onClick={() => handleZoom('RESET')}
                className="p-2 hover:bg-gray-800 rounded-lg text-gray-400 hover:text-white transition"
                title="Reset Zoom"
              >
                <Maximize2 className="h-4 w-4" />
              </button>
              <span className="h-4 w-px bg-gray-800 mx-1"></span>
              <button
                onClick={downloadSVG}
                className="p-2 hover:bg-gray-800 rounded-lg text-gray-400 hover:text-white transition flex items-center space-x-1 px-3 bg-violet-600 text-white rounded-lg hover:bg-violet-700"
                title="Export SVG"
              >
                <Download className="h-4 w-4" />
                <span className="text-[10px] font-bold">Export SVG</span>
              </button>
            </div>
          )}

          {/* Render Area */}
          <div 
            className="flex-1 overflow-hidden flex items-center justify-center p-8 min-h-[400px] select-none"
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onMouseLeave={handleMouseUp}
          >
            {loadingDiagram ? (
              <div className="flex flex-col items-center space-y-3">
                <Loader2 className="h-8 w-8 text-violet-500 animate-spin" />
                <span className="text-xs text-gray-500 font-semibold">Generating visual structure...</span>
              </div>
            ) : renderError ? (
              <div className="text-center max-w-sm space-y-3 p-6 border border-gray-900 glass-card rounded-2xl">
                <AlertTriangle className="h-10 w-10 text-amber-500 mx-auto" />
                <h4 className="font-bold text-gray-200">Diagram Compilation Failed</h4>
                <p className="text-xs text-gray-500 leading-relaxed">{renderError}</p>
                <button
                  onClick={loadDiagram}
                  className="px-4 py-2 bg-violet-600 hover:bg-violet-700 rounded-lg text-xs font-bold transition text-white"
                >
                  Regenerate Diagram
                </button>
              </div>
            ) : svgContent ? (
              <div
                ref={canvasContainerRef}
                className="transition-transform duration-75 flex items-center justify-center bg-gray-900/5 border border-gray-900/10 p-8 rounded-2xl shadow-inner min-w-[300px] min-h-[300px]"
                style={{ 
                  transform: `translate(${panOffset.x}px, ${panOffset.y}px) scale(${zoomScale})`,
                  cursor: isDragging ? 'grabbing' : 'grab'
                }}
                dangerouslySetInnerHTML={{ __html: svgContent }}
              />
            ) : (
              <p className="text-xs text-gray-600">Select a chapter topic and diagram style to view visualization</p>
            )}
          </div>
          
          {/* Temporary invisible render node for Mermaid.js parsing */}
          <div id="mermaid-render-temp" className="hidden" />
        </div>
      </div>
    </div>
  );
}
