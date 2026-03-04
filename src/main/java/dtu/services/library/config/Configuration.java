package dtu.services.library.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.context.annotation.Bean;
import dtu.services.library.metrics.MetricsAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import dtu.services.library.errors.ServiceResponseErrorHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;


@AutoConfiguration
class Configuration implements WebMvcConfigurer
{
    private static final Logger log = LoggerFactory.getLogger(MetadataInitializer.class);

    @Value("${spring.application.name}")
    private String service;

    @Autowired
    private MetricsAggregator metrics;

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        try
        {
            String clazz = null;
            HandlerInterceptor interceptor = null;

            clazz = "dtu.services.library.http.inbound.RequestInterceptor";
            interceptor = Reflection.newInstance(clazz,this.service,this.metrics);
            registry.addInterceptor(interceptor);

            clazz = "dtu.services.library.http.inbound.FinalInterceptor";
            interceptor = Reflection.newInstance(clazz);
            registry.addInterceptor(interceptor).order(Ordered.HIGHEST_PRECEDENCE);
        }
        catch (Exception e)
        {
            log.error("A fatal error occured", e);
        }
    }


    @Bean
    @NonNull
    public ClientHttpRequestInterceptor responseInterceptor()
    {
        try
        {
            String clazz = null;
            ClientHttpRequestInterceptor interceptor = null;
            clazz = "dtu.services.library.http.outbound.ResponseInterceptor";
            interceptor = Reflection.newInstance(clazz);
            return(interceptor);
        }
        catch (Exception e)
        {
            log.error("A fatal error occured", e);
            return(null);
        }
    }


    @Bean
    @ConditionalOnMissingBean
    public RestClient restClient(RestClient.Builder builder)
    {
        ClientHttpRequestInterceptor responseInterceptor = responseInterceptor();
        JacksonYamlHttpMessageConverter yamlConverter = new JacksonYamlHttpMessageConverter();

        List<MediaType> types = List.of(MediaType.parseMediaType("application/x-yaml"));
        yamlConverter.setSupportedMediaTypes(types);

        RestClient client = builder
            .configureMessageConverters(converters ->
            {converters.addCustomConverter(yamlConverter);})
            .requestInterceptor(responseInterceptor)
            .defaultStatusHandler(new ServiceResponseErrorHandler())
            .build();

        return(client);
    }


    @Bean
    public WebMvcConfigurer corsConfigurer()
    {
        return(new WebMvcConfigurer()
        {
            @Override
            public void addCorsMappings(CorsRegistry registry)
            {
                registry.addMapping("/**")
                        .allowedHeaders("*")
                        .allowedOrigins("*")
                        .exposedHeaders("X-Standard-Context")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }
        });
    }
}