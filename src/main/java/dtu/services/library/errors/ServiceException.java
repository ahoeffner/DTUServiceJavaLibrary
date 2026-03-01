package dtu.services.library.errors;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;


public class ServiceException extends RuntimeException
{
    private final HttpStatus status;

    public ServiceException(HttpStatus status, String message)
    {
        super(message);
        this.status = status;
    }


    public ServiceException(HttpStatusCode status, String message)
    {
        this(status.value(),message);
    }


    public ServiceException(int status, String message)
    {
        super(message);
        this.status = HttpStatus.valueOf(status);
    }


    public ServiceException(HttpStatus status, String message, Throwable cause)
    {
        super(message, cause);
        this.status = status;
    }


    public HttpStatus getStatus()
    {
        return(this.status);
    }


    public int getStatusCode()
    {
        return(this.status.value());
    }
}