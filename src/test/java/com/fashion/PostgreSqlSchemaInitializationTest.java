package com.fashion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
        "spring.sql.init.mode=always"
})
@ActiveProfiles("test")
class PostgreSqlSchemaInitializationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DataSource dataSource;

    @Value("${spring.sql.init.schema-locations}")
    private String schemaLocations;

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void createsBaseSchemaAndAppliesAllMigrationsBeforeJpaValidation() {
        List<String> requiredTables = List.of(
                "roles", "brands", "categories", "products", "product_variants", "product_images",
                "users", "carts", "cart_items", "orders", "order_items", "reviews", "review_media",
                "order_status_history", "shipping_providers", "shipments", "shipment_tracking",
                "shipment_route_points", "product_size_charts", "campaign_events", "email_otps",
                "return_requests", "return_request_evidence", "vouchers", "wishlist_items");

        for (String table : requiredTables) {
            assertThat(tableExists(table)).as("table %s should be initialized", table).isTrue();
        }
        assertThat(columnExists("reviews", "order_item_id")).isTrue();
        assertThat(columnExists("orders", "vat_amount")).isTrue();
        assertThat(columnExists("order_items", "vat_amount")).isTrue();
    }

    @Test
    void reappliesConfiguredScriptsToAnExistingSchemaWithoutLosingData() {
        jdbcTemplate.update("INSERT INTO roles(name) VALUES (?)", "SCHEMA_REAPPLY_SENTINEL");

        ResourceDatabasePopulator scripts = new ResourceDatabasePopulator();
        scripts.setSeparator("@@");
        for (String location : schemaLocations.split(",")) {
            String classpathLocation = location.trim().replaceFirst("^classpath:", "");
            scripts.addScript(new ClassPathResource(classpathLocation));
        }
        DatabasePopulatorUtils.execute(scripts, dataSource);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM roles WHERE name = ?", Integer.class, "SCHEMA_REAPPLY_SENTINEL"))
                .isEqualTo(1);
    }

    private boolean tableExists(String tableName) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT to_regclass(?) IS NOT NULL", Boolean.class, "public." + tableName));
    }

    private boolean columnExists(String tableName, String columnName) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?)",
                Boolean.class, tableName, columnName));
    }
}
