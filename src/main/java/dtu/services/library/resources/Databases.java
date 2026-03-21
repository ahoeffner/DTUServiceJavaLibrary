package dtu.services.library.resources;

import java.util.Map;
import org.slf4j.Logger;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;
import dtu.services.library.config.Database;
import dtu.services.library.config.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;


@Component
public class Databases
{
    private final Secrets secrets;
    private final RestClient client;
    private final GenericApplicationContext context;
    private static final Logger log = LoggerFactory.getLogger(Databases.class);


    private Databases(Secrets secrets, RestClient client, GenericApplicationContext context)
    {
        this.client = client;
        this.context = context;
        this.secrets = secrets;
    }


    /**
     * Call this to register and get a JdbcTemplate
     */
    public synchronized JdbcTemplate getJdbcTemplate(String name)
    {
        String bean = name + "JdbcTemplate";
        String tgrbean = name + "Transaction";

        if (context.containsBean(bean))
            return(context.getBean(bean,JdbcTemplate.class));

        try
        {
            log.info("Creating datasource for database "+name);
            Map<String,Map<String,Object>> definitions = Database.get(client,name).config;


            if (definitions == null)
            {
                log.error("Database '" + name + "' not configured");
                return(null);
            }

            Map<String,Object> config = (Map<String,Object>) definitions.get(Environment.TYPE);
            Map<String,String> secrets = this.secrets.getSecrets("databases/"+name+"/"+Environment.TYPE);

            config = replace(config,secrets);

            DataSource ds = DataSourceBuilder.create().build();
            MapConfigurationPropertySource properties = new MapConfigurationPropertySource(config);

            Binder binder = new Binder(properties);
            binder.bind("",Bindable.ofInstance(ds));

            JdbcTemplate template = new JdbcTemplate(ds);

            String test = (String) config.get("jdbc-test");

            if (test == null || test.isEmpty())
                throw new Exception("No test query found for database "+name);

            template.execute(test);
            PlatformTransactionManager tm = new DataSourceTransactionManager(ds);

            context.registerBean(bean, JdbcTemplate.class, () -> template);
            context.registerBean(tgrbean, PlatformTransactionManager.class, () -> tm);

            return(template);
        }
        catch (Exception e)
        {
            log.error("Database coonection failed for database: " + name);
            return(null);
        }
    }


    private Map<String,Object> replace(Map<String,Object> config, Map<String,String> secrets)
    {
        if (secrets == null)
            return(config);

        Map<String,Object> resolved = config.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()));

        for (String key : resolved.keySet())
        {
            Object value = resolved.get(key);

            if (value instanceof String)
            {
                String sval = (String) value;

                sval = sval.trim();

                if (sval.startsWith("${") && sval.endsWith("}"))
                    sval = sval.substring(2, sval.length() - 1);

                sval = sval.trim();

                if (secrets.containsKey(sval))
                    resolved.put(key,secrets.get(sval));
            }
        }

        return(resolved);
    }


    @Bean
    @Primary
    /**
     * A dummy bean to prevent Spring
     * from calling getJdbcTemplate during startup
     * @return
     */
    DataSource dataSource()
    {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:noop://localhost");
        return(dataSource);
    }

}
