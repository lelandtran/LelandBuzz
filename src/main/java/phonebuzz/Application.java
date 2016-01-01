package phonebuzz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class Application implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String args[]) {
        SpringApplication.run(Application.class, args);
    }

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... strings) throws Exception {

        
        //Below code is used to create the phonecalls table. Can be used to reset the table.
        /*
        jdbcTemplate.execute("DROP TABLE IF EXISTS phonecalls");
        jdbcTemplate.execute("CREATE TABLE phonecalls(" +
        		"id SERIAL PRIMARY KEY, time TIMESTAMPTZ, phonenum TEXT, delay INT, digits INT)");
        
        log.info("Created phonecalls table");
		*/
       
    }
}