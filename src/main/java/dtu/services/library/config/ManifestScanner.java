package dtu.services.library.config;

import java.util.Map;
import org.slf4j.Logger;
import java.util.HashMap;
import org.slf4j.LoggerFactory;


class ManifestScanner
{
    private static final Logger log = LoggerFactory.getLogger(ManifestScanner.class);

    public static Map<String, Object> getMetadata()
    {
        Map<String, Object> props = new HashMap<>();

        try
        {
            // Search all manifests available on the classpath
            java.util.Enumeration<java.net.URL> resources =
                ManifestScanner.class.getClassLoader().getResources("META-INF/MANIFEST.MF");

            while (resources.hasMoreElements())
            {
                java.net.URL url = resources.nextElement();

                try (java.io.InputStream is = url.openStream())
                {
                    java.util.jar.Manifest manifest = new java.util.jar.Manifest(is);
                    java.util.jar.Attributes attr = manifest.getMainAttributes();

                    // The "Main" JAR manifest will have a Start-Class or Main-Class entry
                    String startClass = attr.getValue("Start-Class");

                    String title = attr.getValue("Implementation-Title");
                    String version = attr.getValue("Implementation-Version");

                    if ("dtu-service-library".equals(title))
                    {
                        props.put("library.name",title);
                        props.put("library.version",version);
                    }

                    if (startClass != null)
                    {
                        props.put("service.version", version);
                        props.put("spring.application.name",title);
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.error("Error reading manifest: " + e.getMessage());
        }

        return(props);
    }
}