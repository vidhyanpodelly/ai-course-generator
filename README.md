# Curricula.AI - PDF to E-Course Learning Platform

Curricula.AI is a production-quality, Full Stack AI-powered educational application built using a **Modular Monolith** architecture. It transforms static academic PDFs (books, lecture slides, papers, documentation) into complete interactive learning pathways, featuring chapter navigation sidebar outlines, on-demand lazy lesson text generation, automatic quiz generation, and a cognitive RAG-based chatbot tutor.

---

## 1. Project Folder Tree Layout

```
.
├── backend/                       # Spring Boot 3.x Backend Service
│   ├── pom.xml                    # Maven dependency descriptor
│   └── src/
│       └── main/
│           ├── java/com/aicoursegenerator/
│           │   ├── AICourseGeneratorApplication.java # Launcher
│           │   ├── ai/            # LLM Adapters and Strategies
│           │   │   ├── provider/
│           │   │   └── service/   # PromptLoader, AiProviderFactory, GroqProvider
│           │   ├── auth/          # Authentication controllers and requests validation
│           │   │   ├── controller/
│           │   │   └── dto/
│           │   ├── chat/          # Chat messages and RAG context search services
│           │   │   ├── controller/
│           │   │   ├── dto/
│           │   │   ├── entity/
│           │   │   └── repository/
│           │   ├── common/        # Standard wrappers and Exception Advices
│           │   │   ├── dto/       # ApiResponse records
│           │   │   └── exception/
│           │   ├── config/        # CORS, Security filters, Async Thread pool executors
│           │   ├── course/        # Courses, Chapters, Lazy lessons, Progress contexts
│           │   │   ├── controller/
│           │   │   ├── dto/
│           │   │   ├── entity/
│           │   │   └── repository/
│           │   ├── pdf/           # PDF file uploads and Apache PDFBox extractors
│           │   │   ├── controller/
│           │   │   ├── dto/
│           │   │   ├── entity/
│           │   │   └── repository/
│           │   ├── quiz/          # Quiz attempt logging and score checks
│           │   │   ├── controller/
│           │   │   ├── dto/
│           │   │   ├── entity/
│           │   │   └── repository/
│           │   └── user/          # User databases records queries
│           │       ├── entity/
│           │       ├── repository/
│           │       └── service/
│           └── resources/
│               ├── application.properties # Configurations
│               └── prompts/       # Raw AI externalized prompt templates
│                   ├── chat_companion.txt
│                   ├── course_outline.txt
│                   ├── lesson_explanation.txt
│                   └── quiz_generation.txt
├── frontend/                      # Next.js 14+ Frontend Application
│   ├── package.json               # Package dependencies
│   ├── tailwind.config.js         # Tailwind slate-mode styles
│   ├── tsconfig.json              # TypeScript compiler configs
│   └── src/
│       ├── app/                   # App Router pages and page structures
│       │   ├── layout.tsx
│       │   ├── page.tsx           # Premium Landing
│       │   ├── login/
│       │   ├── register/
│       │   ├── dashboard/         # Study Dashboard (uploads & stats)
│       │   └── courses/           # Syllabus outlines, Lessons, & Chapter Quizzes
│       ├── components/
│       │   ├── common/            # ProtectedRoute guards
│       │   └── course/            # SidebarNavigation recursive trees & LessonRenderer
│       ├── context/               # AuthContext React hooks session provider
│       ├── hooks/                 # useAuth session caches
│       ├── lib/                   # api-client Axios setup
│       ├── services/              # API adapter interfaces
│       └── types/                 # TypeScript type interfaces
├── .env.example                   # Environment variable template
├── schema.sql                     # SQL database tables schema
└── README.md                      # Documentation
```

---

## 2. Technology Stack

* **Backend**: Java 21, Spring Boot 3.3.x, Spring Security (Stateless Authorization Filter), Spring Data JPA, Hibernate, PostgreSQL, Apache PDFBox, JWT, Jackson.
* **Frontend**: Next.js 14, React 18, TypeScript, Tailwind CSS, Axios.
* **Database**: PostgreSQL (JSONB semi-structured indices + GIN Full-Text Search).

---

## 3. Run and Setup Instructions

### Database Setup
1. Create a PostgreSQL database instance named `aicourse`.
2. Connect to the database and run the queries defined inside [schema.sql](file:///schema.sql) to initialize the tables and search indexes.

### Backend Configurations
1. Copy [.env.example](file:///.env.example) to a new file named `.env` in the root workspace.
2. Edit the `.env` parameters to specify your database host/credentials and Base64 JWT secret.
3. Configure the `GEMINI_API_KEY` parameter (or leave it as `mock_gemini_key` to run in mock simulation mode).

### Running Backend
```bash
cd backend
mvn spring-boot:run
```
The server starts listening on port `8080` (HTTP).

### Running Frontend
1. Ensure node dependencies are installed inside the `/frontend` directory:
   ```bash
   cd frontend
   npm install
   ```
2. Run the Next.js development server:
   ```bash
   npm run dev
   ```
3. Open `http://localhost:3000` in your web browser.

---

## 4. REST API Documentation

All API responses are wrapped in a standard JSON structure:
```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... },
  "timestamp": "2026-07-18T17:48:12"
}
```

### Public Authentication Endpoints
* **`POST /api/auth/register`**: Registers a new user.
* **`POST /api/auth/login`**: Authenticates credentials and returns a 1-hour JWT.

### Protected Endpoints (Requires `Authorization: Bearer <token>` Header)
* **`GET /api/auth/me`**: Fetches the authenticated user profile.
* **`POST /api/pdf/upload`**: Uploads study material (`multipart/form-data`). Extracts text and saves chunks.
* **`GET /api/pdf`**: Lists metadata of user uploaded files.
* **`POST /api/courses/generate?pdfId=<UUID>`**: Starts outline compilation in the background.
* **`GET /api/courses`**: Lists courses enrolled by the student.
* **`GET /api/courses/{courseId}/chapters`**: Fetches course chapters and lessons.
* **`GET /api/courses/lessons/{lessonId}`**: Fetches a single lesson (triggers lazy lesson text generation if empty).
* **`POST /api/progress/complete?lessonId=<UUID>&completed=<true|false>`**: Marks a lesson checkpoint.
* **`GET /api/quiz/{chapterId}`**: Fetches/lazily generates 5-question chapter quiz.
* **`POST /api/quiz/{chapterId}/submit`**: Grade quiz attempts and logs score sheets.
* **`GET /api/chat/{courseId}`**: Retrieves chatbot dialogue feed logs.
* **`POST /api/chat/{courseId}`**: Sends user question, matches PDF chunks via FTS, and returns tutor answers.
* **`GET /api/courses/search?q=<query>`**: Performs keyword searches across course and lesson contents.
* **`GET /api/dashboard`**: Aggregates Analytical panels (percentages, averages, PDFs, courses).

---

## 5. Testing Instructions

### Backend Unit & Integration Verification
* Run tests inside the backend project:
  ```bash
  cd backend
  mvn test
  ```

### Manual E2E Validation Flow
1. **Register & Log In**: Go to `http://localhost:3000/register`, fill the form, and sign up. You are redirected to the Dashboard.
2. **Library Upload**: Click the dashed upload area to choose any study PDF. The document parsing pipeline chunks text and maps indexes.
3. **Build Course**: Locate the parsed document inside the "Library" list, click the **Build** button. This spawns the background async outline thread pool.
4. **Open Syllabus**: The course card switches to ready status. Click **Open** to navigate the course structure and view prerequisites/learning goals.
5. **Lazy Load Lessons**: Expand a chapter and click **Study** on any lesson. LLaMA 3 extracts contextual matches and generates rich markdown details in ~2-4 seconds.
6. **Chat with AI Tutor**: Type a query in the chatbox (e.g. "What does modularity mean?"). The RAG chatbot performs FTS retrieval on the document chunks and responds.
7. **Complete Checkpoint**: Click **Mark as Complete** at the bottom of the lesson. The progress bar updates.
8. **Quiz Time**: Click **Take Chapter Quiz** at the end of the chapter list. Answer the 5 generated questions, submit, and read explanations.
