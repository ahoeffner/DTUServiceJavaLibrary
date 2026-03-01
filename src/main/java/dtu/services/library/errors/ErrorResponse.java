package dtu.services.library.errors;

import java.time.LocalDateTime;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;


public class ErrorResponse
{
    private int status;
    private String message;
    private LocalDateTime timestamp;

    public ErrorResponse(int status, String message)
    {
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(HttpStatus status, String message)
    {
        this.message = message;
        this.status = status.value();
        this.timestamp = LocalDateTime.now();
    }

    public int getStatus() { return status; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }

    @NonNull
    public HttpStatus getHttpStatus() {return HttpStatus.valueOf(status);}


    @Override
    public String toString()
    {
        return String.format
        (
            "{\"status\": %d, \"message\": \"%s\", \"timestamp\": \"%s\"}",
            status, message, timestamp
        );
    }
}