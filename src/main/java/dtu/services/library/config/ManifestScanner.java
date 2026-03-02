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

                    if (startClass != null)
                    {
                        String service = attr.getValue("Implementation-Title");
                        String version = attr.getValue("Implementation-Version");

                        props.put("spring.application.name",service);
                        props.put("service.version",version);

                        Package pkg = ManifestScanner.class.getPackage();

                        String libname = pkg.getImplementationTitle();
                        String libversion = pkg.getImplementationVersion();

                        props.put("library.name",libname);
                        props.put("library.version",libversion);


                        return(props);
                    }
                }
            }

            log.error("Metadata not found (Are you running from an unpackaged IDE?)");
        }
        catch (Exception e)
        {
            log.error("Error reading manifest: " + e.getMessage());
        }

        return(props);
    }
}