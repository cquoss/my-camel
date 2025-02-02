package de.quoss.camel.sql.stream;

import de.quoss.camel.sql.stream.route.Stream;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.ExchangeBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    
    private void run() throws Exception {
        DataSource ds = createDataSource();
        LOGGER.info("Data source created.");
        Thread.sleep(10000L);
        LOGGER.info("Waited 10 seconds.");
        try (final CamelContext context = new DefaultCamelContext()) {
            final ProducerTemplate template = context.createProducerTemplate();
            final Exchange in = ExchangeBuilder.anExchange(context).build();
            context.setTracing(false);
            context.setUseBreadcrumb(true);
            context.setMessageHistory(true);
            context.addRoutes(new Stream());
            context.getRegistry().bind("dataSource", ds);
            context.start();
            LOGGER.info("Context started.");
            final Exchange out = template.send("direct:stream", in);
            LOGGER.info("Exchange out received: " + out);
            context.stop();
            LOGGER.info("Context stopped.");
        }
    }

    private DataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        // dataSource.setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        dataSource.setURL("jdbc:h2:tcp://localhost/C:/Daten/h2/test");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }
    
    private void executeSql(final DataSource ds, final String name) throws IOException, SQLException {
        try (final Connection c = ds.getConnection();
             final Statement s = c.createStatement()) {
            s.execute(getContent(name));
        }
        
    } 
    
    private String getContent(final String name) throws IOException {
        return Files.readString(getFile(name).toPath());
    }
    
    private File getFile(final String name) throws IOException {
        final ClassLoader cl = getClass().getClassLoader();
        final URL resource = cl.getResource(name);
        if (resource == null) {
            throw new IOException("File " + name + " not found in class path.");
        }
        return new File(resource.getFile());
    } 
    
    public static void main(final String[] args) throws Exception {
        new Main().run();
    }

}
