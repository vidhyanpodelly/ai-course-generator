import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TestOmniRoute {
    public static void main(String[] args) throws Exception {
        String jsonBody = """
            {
                "model": "course-generator",
                "stream": false,
                "messages": [
                    {"role": "system", "content": "You are a course builder."},
                    {"role": "user", "content": "Generate a full course outline for a course about Probability. Output a huge JSON with many chapters."}
                ]
            }
        """;

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:20128/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

        System.out.println("Sending request...");
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Status: " + response.statusCode());
        System.out.println("Body: " + response.body());
    }
}
