package dtu.services.library.config;

public enum OAuth2Server
{
    FUSION_HCM("fusion/hcm");


    private final String path;
    private OAuth2Server(String path) {this.path = path;}

    
    @Override
    public String toString()
    {
        return(path);
    }
}
