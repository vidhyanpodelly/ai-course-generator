'use client';

import React, { useState, useEffect, useRef } from 'react';
import Link from 'next/link';
import { useParams } from 'next/navigation';
import ProtectedRoute from '@/components/common/ProtectedRoute';
import { coursesService } from '@/services/courses';
import { chatService } from '@/services/chat';
import { Course, ChatMessage, ChatSession } from '@/types';
import {
  ArrowLeft,
  MessageSquare,
  Send,
  Loader2,
  HelpCircle,
  RotateCw,
  Sparkles,
  Plus,
  Trash2,
  Edit2
} from 'lucide-react';

export default function CourseChatPage() {
  const params = useParams();
  const courseId = params.courseId as string;

  return (
    <ProtectedRoute>
      <CourseChatContent courseId={courseId} />
    </ProtectedRoute>
  );
}

function CourseChatContent({ courseId }: { courseId: string }) {
  const [course, setCourse] = useState<Course | null>(null);
  const [sessions, setSessions] = useState<ChatSession[]>([]);
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [chatMessage, setChatMessage] = useState('');
  const [sendingChat, setSendingChat] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const suggestedQuestions = [
    "Summarize the key objectives of this course.",
    "Explain the most difficult concepts in the syllabus.",
    "Create a practice quiz question from the course material.",
    "Give me real-world examples of these topics."
  ];

  const fetchInitialData = async () => {
    try {
      setLoading(true);
      setError(null);

      const [courseRes, sessionsRes] = await Promise.all([
        coursesService.getCourse(courseId),
        chatService.getSessions(courseId)
      ]);

      if (courseRes.success) setCourse(courseRes.data);
      if (sessionsRes.success) {
        setSessions(sessionsRes.data);
        if (sessionsRes.data.length > 0) {
          setActiveSessionId(sessionsRes.data[0].id);
        } else {
          await createNewSession();
        }
      }
    } catch (err: any) {
      setError('Failed to load chatbot data. Ensure the backend is running.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInitialData();
  }, [courseId]);

  useEffect(() => {
    if (activeSessionId) {
      loadMessagesForSession(activeSessionId);
    }
  }, [activeSessionId]);

  const loadMessagesForSession = async (sessionId: string) => {
    try {
      const chatRes = await chatService.getChatHistory(sessionId);
      if (chatRes.success) {
        setMessages(chatRes.data);
      }
    } catch (err) {
      console.error("Failed to load messages for session", err);
    }
  };

  const createNewSession = async () => {
    try {
      const res = await chatService.createSession(courseId, `Chat ${new Date().toLocaleDateString()}`);
      if (res.success) {
        setSessions([res.data, ...sessions]);
        setActiveSessionId(res.data.id);
      }
    } catch (err) {
      console.error("Failed to create session", err);
    }
  };

  const renameSession = async (sessionId: string, currentTitle: string) => {
    const newTitle = prompt("Enter new chat name:", currentTitle);
    if (!newTitle || newTitle.trim() === currentTitle) return;
    try {
      const res = await chatService.renameSession(sessionId, newTitle.trim());
      if (res.success) {
        setSessions(sessions.map(s => s.id === sessionId ? res.data : s));
      }
    } catch (err) {
      console.error("Failed to rename session", err);
    }
  };

  const deleteSession = async (sessionId: string) => {
    if (!confirm("Are you sure you want to delete this chat?")) return;
    try {
      await chatService.deleteSession(sessionId);
      const remaining = sessions.filter(s => s.id !== sessionId);
      setSessions(remaining);
      if (activeSessionId === sessionId) {
        setActiveSessionId(remaining.length > 0 ? remaining[0].id : null);
        if (remaining.length === 0) {
          await createNewSession();
        }
      }
    } catch (err) {
      console.error("Failed to delete session", err);
    }
  };

  // Autoscroll chat
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, sendingChat]);

  // SSE streaming chat sender
  const handleSendChat = async (userText: string) => {
    if (!userText.trim() || sendingChat || !activeSessionId) return;

    setChatMessage('');
    setSendingChat(true);

    // 1. Optimistically append user message
    const tempUserMsg: ChatMessage = {
      id: Math.random().toString(),
      sessionId: activeSessionId,
      sender: 'USER',
      messageContent: userText,
      createdAt: new Date().toISOString()
    };
    setMessages(prev => [...prev, tempUserMsg]);

    // 2. Append empty AI response placeholder
    const tempAiMsgId = Math.random().toString();
    const tempAiMsg: ChatMessage = {
      id: tempAiMsgId,
      sessionId: activeSessionId,
      sender: 'AI',
      messageContent: '',
      createdAt: new Date().toISOString()
    };
    setMessages(prev => [...prev, tempAiMsg]);

    try {
      const apiBase = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
      const response = await fetch(`${apiBase}/api/chat/sessions/${activeSessionId}/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('auth_token') || ''}`
        },
        body: JSON.stringify({ message: userText })
      });

      if (!response.ok) {
        throw new Error("Chat stream request failed");
      }

      const reader = response.body?.getReader();
      const decoder = new TextDecoder();
      let finished = false;
      let aiTextAccumulator = "";

      if (reader) {
        let buffer = "";
        let currentEventName = "";

        while (!finished) {
          const { value, done } = await reader.read();
          finished = done;
          if (value) {
            buffer += decoder.decode(value, { stream: !done });
            const packets = buffer.split("\n\n");
            buffer = packets.pop() || "";

            for (const packet of packets) {
              const lines = packet.split("\n");
              currentEventName = "";

              for (let i = 0; i < lines.length; i++) {
                const line = lines[i];
                if (line.startsWith("event:")) {
                  currentEventName = line.substring(6).trim();
                } else if (line.startsWith("data:")) {
                  if (currentEventName === "complete" || currentEventName === "error") {
                    continue;
                  }
                  // SSE data lines have an optional space after the colon
                  let dataVal = line.substring(5);
                  if (dataVal.startsWith(" ")) {
                    dataVal = dataVal.substring(1);
                  }
                  
                  if (dataVal && dataVal !== "[DONE]") {
                    // If this is a subsequent data line in the same event, it implies a newline
                    if (i > 0 && lines[i-1].startsWith("data:")) {
                      aiTextAccumulator += "\n";
                    }
                    aiTextAccumulator += dataVal;
                    setMessages(prev =>
                      prev.map(m =>
                        m.id === tempAiMsgId
                          ? { ...m, messageContent: aiTextAccumulator }
                          : m
                      )
                    );
                  }
                }
              }
            }
          }
        }
      }
    } catch (err) {
      console.error(err);
      setMessages(prev =>
        prev.map(m =>
          m.id === tempAiMsgId
            ? { ...m, messageContent: "Error generating response. Please check backend log details." }
            : m
        )
      );
    } finally {
      setSendingChat(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-950">
        <div className="flex flex-col items-center space-y-4">
          <RotateCw className="h-10 w-10 text-violet-500 animate-spin" />
          <span className="text-sm text-gray-400 font-medium font-sans">Connecting to AI Companion...</span>
        </div>
      </div>
    );
  }

  if (error || !course) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center bg-gray-950 px-4 text-center">
        <HelpCircle className="h-12 w-12 text-red-500 mb-4 animate-pulse" />
        <h3 className="text-xl font-bold mb-2">Error Connecting Chat</h3>
        <p className="text-gray-400 text-sm max-w-sm mb-6">{error || 'Course record does not exist.'}</p>
        <Link href="/dashboard" className="px-5 py-2.5 rounded-xl bg-violet-600 hover:bg-violet-700 text-sm font-semibold transition">
          Back to Dashboard
        </Link>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex flex-col h-screen overflow-hidden">
      {/* Header */}
      <header className="glass-panel px-6 py-4 flex items-center justify-between border-b border-gray-900 bg-gray-950/80 z-20 shrink-0">
        <div className="flex items-center space-x-4">
          <Link href={`/courses/${courseId}`} className="p-2 hover:bg-gray-900 rounded-lg text-gray-400 hover:text-white transition">
            <ArrowLeft className="h-5 w-5" />
          </Link>
          <div>
            <h1 className="text-sm font-bold text-gray-200 line-clamp-1">AI Chat Tutor</h1>
            <p className="text-[10px] text-violet-400 font-semibold uppercase">{course.title}</p>
          </div>
        </div>
      </header>

      {/* Main layout splitting sidebar and chat body */}
      <div className="flex flex-1 overflow-hidden">
        {/* Left Sidebar Info Card */}
        <div className="hidden md:flex w-72 border-r border-gray-900 bg-gray-950/50 p-4 flex-col shrink-0">
          <button
            onClick={createNewSession}
            className="w-full mb-4 flex items-center justify-center space-x-2 py-2.5 px-4 bg-violet-600 hover:bg-violet-700 text-white text-sm font-semibold rounded-xl transition"
          >
            <Plus className="h-4 w-4" />
            <span>New Chat</span>
          </button>
          
          <div className="flex-1 overflow-y-auto space-y-2">
            <h4 className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-3 px-2">History</h4>
            {sessions.map(session => (
              <div
                key={session.id}
                className={`group flex items-center justify-between px-3 py-2.5 rounded-lg cursor-pointer transition ${activeSessionId === session.id ? 'bg-gray-800' : 'hover:bg-gray-900/60'}`}
                onClick={() => setActiveSessionId(session.id)}
              >
                <div className="flex items-center space-x-3 overflow-hidden">
                  <MessageSquare className={`h-4 w-4 shrink-0 ${activeSessionId === session.id ? 'text-violet-400' : 'text-gray-500'}`} />
                  <div className="flex flex-col overflow-hidden">
                    <span className={`text-sm truncate ${activeSessionId === session.id ? 'text-gray-200 font-medium' : 'text-gray-400'}`}>
                      {session.title}
                    </span>
                    <span className="text-[10px] text-gray-600 truncate">
                      {new Date(session.updatedAt).toLocaleString()}
                    </span>
                  </div>
                </div>
                <div className="flex items-center space-x-1 opacity-0 group-hover:opacity-100 transition">
                  <button onClick={(e) => { e.stopPropagation(); renameSession(session.id, session.title); }} className="p-1 hover:text-white text-gray-400">
                    <Edit2 className="h-3.5 w-3.5" />
                  </button>
                  <button onClick={(e) => { e.stopPropagation(); deleteSession(session.id); }} className="p-1 hover:text-red-400 text-gray-400">
                    <Trash2 className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>

          <div className="pt-4 border-t border-gray-900/60 text-[10px] text-gray-600 flex items-center justify-center space-x-1">
            <Sparkles className="h-3 w-3 text-amber-500 animate-pulse" />
            <span>Powered by Gemini</span>
          </div>
        </div>

        {/* Chat Window */}
        <div className="flex-1 flex flex-col bg-gray-950/20 overflow-hidden relative">
          
          {/* Messages Container */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6">
            {messages.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center text-center max-w-sm mx-auto">
                <div className="p-4 bg-violet-600/10 rounded-full border border-violet-500/20 mb-4 animate-bounce">
                  <MessageSquare className="h-8 w-8 text-violet-400" />
                </div>
                <h4 className="text-sm font-bold text-gray-300 mb-2">Start your learning session</h4>
                <p className="text-xs text-gray-500 leading-relaxed mb-6">
                  Ask any questions about this course. Our AI Tutor will retrieve specific document details to answer your queries correctly.
                </p>
                
                <div className="grid grid-cols-1 gap-2 w-full">
                  {suggestedQuestions.map((q, idx) => (
                    <button
                      key={idx}
                      onClick={() => handleSendChat(q)}
                      className="text-left text-xs p-3 bg-gray-900/40 hover:bg-gray-900 border border-gray-850 hover:border-gray-700 rounded-xl text-gray-300 hover:text-white transition shadow-sm"
                    >
                      {q}
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              messages.map((msg) => {
                const isAI = msg.sender === 'AI';
                return (
                  <div
                    key={msg.id}
                    className={`flex flex-col max-w-[80%] ${isAI ? 'self-start' : 'ml-auto items-end'}`}
                  >
                    <div
                      className={`p-4 rounded-2xl text-[12px] leading-relaxed ${
                        isAI
                          ? 'bg-gray-900/60 border border-gray-900 text-gray-300 shadow-md'
                          : 'bg-violet-600 text-white shadow-lg shadow-violet-600/10'
                      }`}
                    >
                      <MessageBubbleRenderer text={msg.messageContent} />
                    </div>
                    <span className="text-[9px] text-gray-600 mt-1 px-1.5 font-bold uppercase tracking-wider">
                      {isAI ? 'AI Tutor' : 'You'} • {new Date(msg.createdAt).toLocaleTimeString()}
                    </span>
                  </div>
                );
              })
            )}
            
            {sendingChat && (
              <div className="flex items-center space-x-2.5 text-xs text-gray-500 pl-2">
                <Loader2 className="h-4 w-4 animate-spin text-violet-500" />
                <span>AI Tutor is writing a verified answer...</span>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Panel */}
          <div className="p-4 border-t border-gray-900 bg-gray-950 shrink-0">
            <form
              onSubmit={(e) => { e.preventDefault(); handleSendChat(chatMessage); }}
              className="max-w-4xl mx-auto flex gap-3 items-center"
            >
              <input
                type="text"
                placeholder="Type your question here about this course..."
                value={chatMessage}
                onChange={(e) => setChatMessage(e.target.value)}
                className="flex-1 px-4 py-3 rounded-xl border border-gray-850 bg-gray-900/40 text-xs text-gray-200 placeholder-gray-500 focus:outline-none focus:border-violet-500 transition focus:ring-1 focus:ring-violet-500"
              />
              <button
                type="submit"
                disabled={sendingChat || !chatMessage.trim() || !activeSessionId}
                className="p-3 rounded-xl bg-violet-600 hover:bg-violet-700 text-white disabled:opacity-50 transition shadow-md hover:shadow-violet-600/20 shrink-0"
              >
                <Send className="h-4 w-4" />
              </button>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}

// Markdown and mathematical LaTeX block parser
function MessageBubbleRenderer({ text }: { text: string }) {
  if (!text) return null;

  const lines = text.split("\n");
  const parsedElements = [];

  let inCodeBlock = false;
  let codeBlockContent = "";

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    // Code Block Check
    if (line.startsWith("```")) {
      if (inCodeBlock) {
        inCodeBlock = false;
        parsedElements.push(
          <pre key={`code-${i}`} className="bg-black/40 border border-gray-850 p-3 rounded-lg overflow-x-auto my-2 font-mono text-[10px] text-violet-300">
            <code>{codeBlockContent}</code>
          </pre>
        );
        codeBlockContent = "";
      } else {
        inCodeBlock = true;
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
        <div key={`list-${i}`} className="flex items-start space-x-2 my-1 pl-2 text-[12px]">
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
      parsedElements.push(<p key={`para-${i}`} className="my-1 text-[12px]">{renderInlineStyles(line)}</p>);
    }
  }

  return <div className="space-y-1">{parsedElements}</div>;
}

function renderInlineStyles(text: string) {
  if (!text) return "";

  const parts = [];
  let remaining = text;

  const starRegex = /\*\*([^*]+)\*\*/g;
  const backtickRegex = /`([^`]+)`/g;
  const citationRegex = /\[(Source:\s*Chunk\s*\d+)\]/gi;
  const mathRegex = /\$([^$]+)\$/g;

  let match;
  let items: { index: number; length: number; type: 'BOLD' | 'CODE' | 'CITATION' | 'MATH'; value: string }[] = [];

  while ((match = starRegex.exec(text)) !== null) {
    items.push({ index: match.index, length: match[0].length, type: 'BOLD', value: match[1] });
  }
  while ((match = backtickRegex.exec(text)) !== null) {
    items.push({ index: match.index, length: match[0].length, type: 'CODE', value: match[1] });
  }
  while ((match = citationRegex.exec(text)) !== null) {
    items.push({ index: match.index, length: match[0].length, type: 'CITATION', value: match[1] });
  }
  while ((match = mathRegex.exec(text)) !== null) {
    items.push({ index: match.index, length: match[0].length, type: 'MATH', value: match[1] });
  }

  items.sort((a, b) => a.index - b.index);

  let lastIdx = 0;
  for (const item of items) {
    if (item.index < lastIdx) continue;
    
    if (item.index > lastIdx) {
      parts.push(<span key={`text-${lastIdx}`}>{remaining.substring(lastIdx, item.index)}</span>);
    }

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
