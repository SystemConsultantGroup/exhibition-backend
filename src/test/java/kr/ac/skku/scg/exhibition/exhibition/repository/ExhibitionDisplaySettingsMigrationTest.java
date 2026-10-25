package kr.ac.skku.scg.exhibition.exhibition.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

class ExhibitionDisplaySettingsMigrationTest {

    /** 개발 DB가 아닌 독립 H2에서 기존 행 보정, 신규 행 기본값, NOT NULL 제약을 검증한다. */
    @Test
    void migration_appliesDefaultsToExistingAndNewRows() throws Exception {
        String url = "jdbc:h2:mem:display-settings-" + UUID.randomUUID() + ";MODE=MySQL";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE exhibition_services (id INT PRIMARY KEY)");
            statement.execute("INSERT INTO exhibition_services (id) VALUES (1)");

            ScriptUtils.executeSqlScript(connection,
                    new FileSystemResource("docs/sql/20261025_add_exhibition_display_settings.sql"));
            statement.execute("INSERT INTO exhibition_services (id) VALUES (2)");

            try (ResultSet rows = statement.executeQuery(
                    "SELECT show_awards, default_to_latest_event_period FROM exhibition_services ORDER BY id")) {
                for (int i = 0; i < 2; i++) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getBoolean("show_awards")).isTrue();
                    assertThat(rows.wasNull()).isFalse();
                    assertThat(rows.getBoolean("default_to_latest_event_period")).isFalse();
                    assertThat(rows.wasNull()).isFalse();
                }
                assertThat(rows.next()).isFalse();
            }
            assertThatThrownBy(() -> statement.executeUpdate(
                    "UPDATE exhibition_services SET show_awards = NULL WHERE id = 1"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> statement.executeUpdate(
                    "UPDATE exhibition_services SET default_to_latest_event_period = NULL WHERE id = 1"))
                    .isInstanceOf(SQLException.class);
        }
    }
}
