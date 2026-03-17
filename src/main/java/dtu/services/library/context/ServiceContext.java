package dtu.services.library.context;

public class ServiceContext
{
    private Long start = System.currentTimeMillis();
    private static final ThreadLocal<ServiceContext> client = new ThreadLocal<>();


    public static ServiceContext create()
    {
        ServiceContext state = new ServiceContext();
        client.set(state); state.start(); return(state);
    }

    public static void setContext(ServiceContext context)
    {
        client.set(context);
    }

    public static ServiceContext getContext()
    {
        return(client.get());
    }

    public static void clear()
    {
        client.remove();
    }


    public void start()
    {
        start = System.currentTimeMillis();
    }

    public long finish()
    {
        long delta = System.currentTimeMillis() - start;
        this.start = System.currentTimeMillis();
        return(delta);
    }
}
