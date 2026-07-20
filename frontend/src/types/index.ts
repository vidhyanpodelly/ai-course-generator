export interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface Course {
  id: string;
  userId: string;
  pdfMetadataId: string;
  title: string;
  description: string;
  estimatedDuration: string;
  difficultyLevel: string;
  prerequisites: string[];
  learningObjectives: string[];
  status: 'PENDING' | 'GENERATING_OUTLINE' | 'OUTLINE_GENERATED' | 'READY' | 'FAILED';
  failureReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface Chapter {
  id: string;
  courseId: string;
  title: string;
  summary: string;
  sequenceNumber: number;
  lessons: Lesson[];
  quizData?: QuizQuestion[];
}

export interface Lesson {
  id: string;
  chapterId: string;
  title: string;
  explanation?: string;
  keyTakeaways: string[];
  importantNotes: string[];
  realWorldExamples: string[];
  sequenceNumber: number;
  completed?: boolean;
}

export interface QuizQuestion {
  questionText: string;
  type: 'MCQ' | 'TRUE_FALSE' | 'SHORT_ANSWER';
  options?: string[]; // for MCQ
  correctAnswer: string;
  explanation: string;
}

export interface QuizAttempt {
  id: string;
  userId: string;
  chapterId: string;
  score: number;
  totalQuestions: number;
  answers: Record<string, string>; // question index -> user answer
  createdAt: string;
}

export interface ChatSession {
  id: string;
  courseId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  sender: 'USER' | 'AI';
  messageContent: string;
  createdAt: string;
}

export interface CourseProgress {
  completedLessonsCount: number;
  totalLessonsCount: number;
  percentage: number;
  completedLessonIds: string[];
}

export interface PDFMetadata {
  id: string;
  userId: string;
  filename: string;
  fileSize: number;
  totalPages: number;
  status: 'PENDING' | 'PARSED' | 'FAILED';
  failureReason?: string;
  createdAt: string;
}
