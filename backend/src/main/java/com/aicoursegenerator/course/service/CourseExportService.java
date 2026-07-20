package com.aicoursegenerator.course.service;

import com.aicoursegenerator.common.exception.ResourceNotFoundException;
import com.aicoursegenerator.course.entity.Chapter;
import com.aicoursegenerator.course.entity.Course;
import com.aicoursegenerator.course.entity.Lesson;
import com.aicoursegenerator.course.repository.ChapterRepository;
import com.aicoursegenerator.course.repository.CourseRepository;
import com.aicoursegenerator.course.repository.LessonRepository;
import com.aicoursegenerator.user.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CourseExportService {

    private final CourseRepository courseRepository;
    private final ChapterRepository chapterRepository;
    private final LessonRepository lessonRepository;

    public CourseExportService(
            CourseRepository courseRepository,
            ChapterRepository chapterRepository,
            LessonRepository lessonRepository) {
        this.courseRepository = courseRepository;
        this.chapterRepository = chapterRepository;
        this.lessonRepository = lessonRepository;
    }

    public String exportMarkdown(UUID courseId, User user) {
        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(course.getTitle()).append("\n\n");
        sb.append("## Description\n").append(course.getDescription()).append("\n\n");
        sb.append("- **Difficulty Level:** ").append(course.getDifficultyLevel()).append("\n");
        sb.append("- **Estimated Duration:** ").append(course.getEstimatedDuration()).append("\n\n");

        if (course.getPrerequisites() != null && !course.getPrerequisites().isEmpty()) {
            sb.append("## Prerequisites\n");
            for (String pr : course.getPrerequisites()) {
                sb.append("- ").append(pr).append("\n");
            }
            sb.append("\n");
        }

        if (course.getLearningObjectives() != null && !course.getLearningObjectives().isEmpty()) {
            sb.append("## Learning Objectives\n");
            for (String obj : course.getLearningObjectives()) {
                sb.append("- ").append(obj).append("\n");
            }
            sb.append("\n");
        }

        sb.append("---\n\n");

        List<Chapter> chapters = chapterRepository.findByCourseOrderBySequenceNumberAsc(course);
        for (Chapter ch : chapters) {
            sb.append("# Chapter ").append(ch.getSequenceNumber()).append(": ").append(ch.getTitle()).append("\n\n");
            sb.append("### Summary\n").append(ch.getSummary() != null ? ch.getSummary() : "").append("\n\n");

            List<Lesson> lessons = lessonRepository.findByChapterOrderBySequenceNumberAsc(ch);
            for (Lesson les : lessons) {
                sb.append("## Lesson ").append(ch.getSequenceNumber()).append(".").append(les.getSequenceNumber())
                  .append(": ").append(les.getTitle()).append("\n\n");
                
                sb.append("### Content\n").append(les.getExplanation() != null ? les.getExplanation() : "*Content not generated yet. Visit this lesson online to trigger lazy AI compilation.*").append("\n\n");

                if (les.getKeyTakeaways() != null && !les.getKeyTakeaways().isEmpty()) {
                    sb.append("#### Key Takeaways\n");
                    for (String tk : les.getKeyTakeaways()) {
                        sb.append("- ").append(tk).append("\n");
                    }
                    sb.append("\n");
                }

                if (les.getImportantNotes() != null && !les.getImportantNotes().isEmpty()) {
                    sb.append("#### Important Notes\n");
                    for (String note : les.getImportantNotes()) {
                        sb.append("- ").append(note).append("\n");
                    }
                    sb.append("\n");
                }

                if (les.getRealWorldExamples() != null && !les.getRealWorldExamples().isEmpty()) {
                    sb.append("#### Real-World Context\n");
                    for (String ex : les.getRealWorldExamples()) {
                        sb.append("- ").append(ex).append("\n");
                    }
                    sb.append("\n");
                }
            }
            sb.append("---\n\n");
        }

        return sb.toString();
    }

    public String exportHtml(UUID courseId, User user) {
        Course course = courseRepository.findByIdAndUser(courseId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n");
        sb.append("<title>").append(course.getTitle()).append(" - Export</title>\n");
        sb.append("<style>\n")
          .append("  body { background-color: #030712; color: #e5e7eb; font-family: system-ui, -apple-system, sans-serif; line-height: 1.7; max-width: 900px; margin: 40px auto; padding: 0 24px; }\n")
          .append("  h1, h2, h3, h4 { color: #a78bfa; font-weight: 700; }\n")
          .append("  h1 { border-bottom: 2px solid #374151; padding-bottom: 12px; margin-top: 50px; }\n")
          .append("  h2 { border-bottom: 1px solid #1f2937; padding-bottom: 8px; margin-top: 40px; }\n")
          .append("  .metadata-panel { background: #111827; border: 1px solid #1f2937; padding: 20px; border-radius: 12px; margin-bottom: 30px; }\n")
          .append("  .card { background: #111827/50; border: 1px solid #1f2937/80; padding: 18px; border-radius: 10px; margin: 15px 0; }\n")
          .append("  ul { padding-left: 20px; }\n")
          .append("  li { margin-bottom: 8px; }\n")
          .append("  code { background: #1f2937; padding: 2px 6px; border-radius: 4px; font-family: monospace; font-size: 0.9em; }\n")
          .append("  pre { background: #0b0f19; border: 1px solid #1f2937; padding: 16px; border-radius: 8px; overflow-x: auto; }\n")
          .append("  pre code { background: none; padding: 0; }\n")
          .append("</style>\n</head>\n<body>\n");

        sb.append("<h1>").append(course.getTitle()).append("</h1>\n");
        sb.append("<div class=\"metadata-panel\">\n");
        sb.append("  <p><strong>Description:</strong> ").append(course.getDescription()).append("</p>\n");
        sb.append("  <p><strong>Difficulty Level:</strong> ").append(course.getDifficultyLevel()).append(" | <strong>Estimated Duration:</strong> ").append(course.getEstimatedDuration()).append("</p>\n");
        
        if (course.getPrerequisites() != null && !course.getPrerequisites().isEmpty()) {
            sb.append("  <p><strong>Prerequisites:</strong></p>\n<ul>\n");
            for (String pr : course.getPrerequisites()) {
                sb.append("    <li>").append(pr).append("</li>\n");
            }
            sb.append("  </ul>\n");
        }

        if (course.getLearningObjectives() != null && !course.getLearningObjectives().isEmpty()) {
            sb.append("  <p><strong>Learning Objectives:</strong></p>\n<ul>\n");
            for (String obj : course.getLearningObjectives()) {
                sb.append("    <li>").append(obj).append("</li>\n");
            }
            sb.append("  </ul>\n");
        }
        sb.append("</div>\n");

        List<Chapter> chapters = chapterRepository.findByCourseOrderBySequenceNumberAsc(course);
        for (Chapter ch : chapters) {
            sb.append("<h1>Chapter ").append(ch.getSequenceNumber()).append(": ").append(ch.getTitle()).append("</h1>\n");
            sb.append("<p><em>").append(ch.getSummary() != null ? ch.getSummary() : "").append("</em></p>\n");

            List<Lesson> lessons = lessonRepository.findByChapterOrderBySequenceNumberAsc(ch);
            for (Lesson les : lessons) {
                sb.append("<h2>Lesson ").append(ch.getSequenceNumber()).append(".").append(les.getSequenceNumber())
                  .append(": ").append(les.getTitle()).append("</h2>\n");
                
                sb.append("<div>").append(les.getExplanation() != null ? formatMarkdownToHtml(les.getExplanation()) : "<p><em>Content not generated yet. Visit lesson page to trigger lazy loading.</em></p>").append("</div>\n");

                if (les.getKeyTakeaways() != null && !les.getKeyTakeaways().isEmpty()) {
                    sb.append("<div class=\"card\">\n  <h3>Key Takeaways</h3>\n  <ul>\n");
                    for (String tk : les.getKeyTakeaways()) {
                        sb.append("    <li>").append(tk).append("</li>\n");
                    }
                    sb.append("  </ul>\n</div>\n");
                }

                if (les.getImportantNotes() != null && !les.getImportantNotes().isEmpty()) {
                    sb.append("<div class=\"card\" style=\"border-left: 4px solid #f59e0b;\">\n  <h3 style=\"color: #f59e0b;\">Important Notes</h3>\n  <ul>\n");
                    for (String note : les.getImportantNotes()) {
                        sb.append("    <li>").append(note).append("</li>\n");
                    }
                    sb.append("  </ul>\n</div>\n");
                }

                if (les.getRealWorldExamples() != null && !les.getRealWorldExamples().isEmpty()) {
                    sb.append("<div class=\"card\" style=\"border-left: 4px solid #10b981;\">\n  <h3 style=\"color: #10b981;\">Real-World Context</h3>\n  <ul>\n");
                    for (String ex : les.getRealWorldExamples()) {
                        sb.append("    <li>").append(ex).append("</li>\n");
                    }
                    sb.append("  </ul>\n</div>\n");
                }
            }
        }

        sb.append("</body>\n</html>\n");
        return sb.toString();
    }

    private String formatMarkdownToHtml(String md) {
        if (md == null) return "";
        // Simple markdown line-break and header rendering for clean export styling
        String html = md
            .replace("\r\n", "\n")
            .replaceAll("(?m)^### (.*)$", "<h3>$1</h3>")
            .replaceAll("(?m)^#### (.*)$", "<h4>$1</h4>")
            .replaceAll("(?m)^## (.*)$", "<h2>$1</h2>")
            .replaceAll("(?m)^# (.*)$", "<h1>$1</h1>")
            .replaceAll("(?m)^\\* (.*)$", "<li>$1</li>")
            .replaceAll("(?m)^- (.*)$", "<li>$1</li>");
            
        // Wrap adjacent li elements in ul
        html = html.replaceAll("(<li>.*</li>)", "<ul>$1</ul>");
        // Clean double wrapped uls
        html = html.replaceAll("</ul>\\s*<ul>", "");
        
        // Render simple block code snippets
        html = html.replaceAll("```([a-zA-Z]*)\\n([\\s\\S]*?)```", "<pre><code>$2</code></pre>");
        
        return html;
    }
}
