-- database schema for Curricula.AI -- Modular Monolith PostgreSQL

-- 1. users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. pdf_metadata table
CREATE TABLE IF NOT EXISTS pdf_metadata (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(512) NOT NULL,
    file_size BIGINT,
    total_pages INTEGER,
    status VARCHAR(50) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. pdf_chunks table
CREATE TABLE IF NOT EXISTS pdf_chunks (
    id UUID PRIMARY KEY,
    pdf_metadata_id UUID NOT NULL REFERENCES pdf_metadata(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL
);

-- 4. courses table
CREATE TABLE IF NOT EXISTS courses (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    pdf_metadata_id UUID NOT NULL REFERENCES pdf_metadata(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    estimated_duration VARCHAR(50),
    difficulty_level VARCHAR(50),
    prerequisites JSONB,
    learning_objectives JSONB,
    status VARCHAR(50) NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. chapters table
CREATE TABLE IF NOT EXISTS chapters (
    id UUID PRIMARY KEY,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    summary TEXT,
    sequence_number INTEGER NOT NULL,
    quiz_data JSONB
);

-- 6. lessons table
CREATE TABLE IF NOT EXISTS lessons (
    id UUID PRIMARY KEY,
    chapter_id UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    explanation TEXT,
    key_takeaways JSONB,
    important_notes JSONB,
    real_world_examples JSONB,
    sequence_number INTEGER NOT NULL
);

-- 7. learning_progress table
CREATE TABLE IF NOT EXISTS learning_progress (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    lesson_id UUID NOT NULL REFERENCES lessons(id) ON DELETE CASCADE,
    completed BOOLEAN DEFAULT FALSE NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT unique_user_lesson UNIQUE (user_id, lesson_id)
);

-- 8. chat_messages table
CREATE TABLE IF NOT EXISTS chat_messages (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id UUID NOT NULL REFERENCES courses(id) ON DELETE CASCADE,
    sender VARCHAR(50) NOT NULL,
    message_content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Indices for performance
CREATE INDEX IF NOT EXISTS idx_pdf_chunks_metadata ON pdf_chunks(pdf_metadata_id);
CREATE INDEX IF NOT EXISTS idx_chapters_course ON chapters(course_id);
CREATE INDEX IF NOT EXISTS idx_lessons_chapter ON lessons(chapter_id);

-- GIN Index for Full-Text Search (RAG & course content lookup)
CREATE INDEX IF NOT EXISTS idx_pdf_chunks_fts ON pdf_chunks USING GIN(to_tsvector('english', content));

-- ==========================================
-- PREMIUM FEATURES EVOLUTION TABLES
-- ==========================================

-- 9. otp_verifications table
CREATE TABLE IF NOT EXISTS otp_verifications (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    otp_type VARCHAR(50) NOT NULL, -- SIGNUP, FORGOT_PASSWORD
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_otp_email ON otp_verifications(email);

-- 10. bookmarks table
CREATE TABLE IF NOT EXISTS bookmarks (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bookmark_type VARCHAR(50) NOT NULL, -- LESSON, CHAPTER, CHAT, SEARCH
    target_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_bookmarks_user ON bookmarks(user_id);

-- 11. notifications table
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL, -- COURSE_READY, LESSON_READY, QUIZ_GRADED, CERTIFICATE_READY
    is_read BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);

-- 12. flashcards table
CREATE TABLE IF NOT EXISTS flashcards (
    id UUID PRIMARY KEY,
    chapter_id UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    front TEXT NOT NULL,
    back TEXT NOT NULL,
    difficulty VARCHAR(50) DEFAULT 'MEDIUM' NOT NULL, -- EASY, MEDIUM, HARD
    box INTEGER DEFAULT 1 NOT NULL, -- Spaced repetition Leitner box
    next_review TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_flashcards_chapter ON flashcards(chapter_id);

-- 13. pdf_chunk_embeddings table
CREATE TABLE IF NOT EXISTS pdf_chunk_embeddings (
    id UUID PRIMARY KEY,
    pdf_chunk_id UUID NOT NULL REFERENCES pdf_chunks(id) ON DELETE CASCADE,
    embedding float8[] NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_embeddings_chunk ON pdf_chunk_embeddings(pdf_chunk_id);

-- 14. mind_maps table
CREATE TABLE IF NOT EXISTS mind_maps (
    id UUID PRIMARY KEY,
    chapter_id UUID NOT NULL REFERENCES chapters(id) ON DELETE CASCADE,
    mermaid_data TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Modify users table to support profile preferences, streaks, and achievements
ALTER TABLE users ADD COLUMN IF NOT EXISTS learning_streak INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_active_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(512);
ALTER TABLE users ADD COLUMN IF NOT EXISTS achievements JSONB DEFAULT '[]'::jsonb;
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferences JSONB DEFAULT '{"language": "en"}'::jsonb;

