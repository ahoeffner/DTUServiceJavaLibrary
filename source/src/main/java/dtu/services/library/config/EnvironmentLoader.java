package dtu.services.library.config;

import java.util.List;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.boot.env.YamlPropertySourceLoader;


/**
 * Loads the library-specific environments.yaml as the absolute first step.
 * Can be overrided by any service application.yaml
 */
class EnvironmentLoader implements EnvironmentPostProcessor, Ordered
{
    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();


    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application)
    {
        Resource path = new ClassPathResource("environments.yaml");

        if (!path.exists())
            throw new IllegalStateException("Failed to load library configuration from environments.yaml");

        try
        {
            List<PropertySource<?>> sources = loader.load("runtime-environment",path);
            for (PropertySource<?> property : sources) environment.getPropertySources().addLast(property);
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("Failed to load library configuration from environments.yaml", ex);
        }
    }


    @Override
    public int getOrder()
    {
        return(Ordered.HIGHEST_PRECEDENCE);
    }
}