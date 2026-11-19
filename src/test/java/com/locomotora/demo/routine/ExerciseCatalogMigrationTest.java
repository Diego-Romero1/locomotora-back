package com.locomotora.demo.routine;

import static org.assertj.core.api.Assertions.assertThat;

import com.locomotora.demo.routine.dto.ExerciseGroups;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * V16 sobre un Postgres real: migra hasta V15, guarda los ejercicios actuales y aplica el catalogo.
 * Comprueba que los ids no cambian (rutinas e historial siguen apuntando bien) y que los datos
 * nuevos son coherentes.
 */
@Testcontainers
class ExerciseCatalogMigrationTest {
    private static final Set<String> EQUIPMENT = Set.of(
            "BARBELL", "DUMBBELL", "MACHINE", "CABLE", "BODYWEIGHT", "FREE_WEIGHT", "SMITH_MACHINE", "BAND", "KETTLEBELL",
            "EZ_BAR", "OTHER");
    private static final Set<String> ZONES = Set.of(
            "KNEE", "LOWER_BACK", "SHOULDER", "ELBOW", "WRIST", "NECK", "HIP", "ANKLE");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Test
    void v16KeepsExistingExercisesAndAddsTheCatalog() throws SQLException {
        flyway("15").migrate();
        Map<String, String> before = new HashMap<>();
        int routineRefsBefore;
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery("SELECT id, name FROM exercises WHERE created_by_user_id IS NULL")) {
                while (rows.next()) {
                    before.put(rows.getString("id"), rows.getString("name"));
                }
            }
            routineRefsBefore = count(statement, "SELECT count(*) FROM routine_exercises");
        }
        assertThat(before).hasSizeGreaterThan(100);

        flyway(null).migrate();

        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            int total = count(statement, "SELECT count(*) FROM exercises WHERE created_by_user_id IS NULL");
            assertThat(total).isEqualTo(before.size() + 780);
            assertThat(count(statement,
                    "SELECT count(DISTINCT lower(name)) FROM exercises WHERE created_by_user_id IS NULL")).isEqualTo(total);

            // Mismos ids que antes, todos enriquecidos y marcados como core.
            List<String> missing = new ArrayList<>();
            for (String id : before.keySet()) {
                try (ResultSet row = statement.executeQuery(
                        "SELECT is_core FROM exercises WHERE id = '" + id + "'")) {
                    if (!row.next() || !row.getBoolean(1)) {
                        missing.add(before.get(id));
                    }
                }
            }
            assertThat(missing).isEmpty();
            assertThat(count(statement, "SELECT count(*) FROM routine_exercises re JOIN exercises e ON e.id = re.exercise_id"))
                    .isEqualTo(routineRefsBefore);
            assertThat(count(statement, "SELECT count(*) FROM exercises WHERE name = 'Press de banca plano' "
                    + "AND external_id = 'Barbell_Bench_Press_-_Medium_Grip'")).isEqualTo(1);
            assertThat(count(statement, "SELECT count(*) FROM exercises WHERE name = 'Sentadilla búlgara'")).isEqualTo(1);

            // Datos validos en todo el catalogo global.
            assertThat(values(statement, "SELECT DISTINCT primary_muscle_group FROM exercises "
                    + "WHERE created_by_user_id IS NULL AND primary_muscle_group IS NOT NULL"))
                    .isSubsetOf(ExerciseGroups.MUSCLES);
            assertThat(values(statement, "SELECT DISTINCT jsonb_array_elements_text(secondary_muscle_groups) FROM exercises"))
                    .isSubsetOf(ExerciseGroups.MUSCLES);
            assertThat(values(statement, "SELECT DISTINCT equipment FROM exercises "
                    + "WHERE created_by_user_id IS NULL AND equipment IS NOT NULL")).isSubsetOf(EQUIPMENT);
            assertThat(values(statement, "SELECT DISTINCT jsonb_array_elements_text(stress_zones) FROM exercises"))
                    .isSubsetOf(ZONES);
            assertThat(count(statement, "SELECT count(*) FROM exercises WHERE source = 'FREE_EXERCISE_DB' "
                    + "AND (instructions IS NULL OR instructions NOT LIKE '1. %' OR difficulty IS NULL)")).isZero();
            assertThat(count(statement, "SELECT count(*) FROM exercises WHERE is_core")).isBetween(120, 200);
            assertThat(count(statement, "SELECT count(*) FROM exercises WHERE exercise_type = 'STRETCHING' "
                    + "AND stress_zones <> '[]'::jsonb")).isZero();

            // V17: ilustraciones de wger. Cada fila con cuadros tiene al menos 2, con atribucion, y cada
            // archivo existe entre los recursos estaticos que sirve el backend.
            assertThat(count(statement, "SELECT count(*) FROM exercises WHERE media_frames <> '[]'::jsonb"))
                    .isEqualTo(48);
            assertThat(count(statement, "SELECT count(*) FROM exercises WHERE media_frames <> '[]'::jsonb AND "
                    + "(jsonb_array_length(media_frames) < 2 OR media_author IS NULL OR media_license <> 'CC-BY-SA-3.0' "
                    + "OR media_source_url IS NULL OR thumb_url <> media_frames->>0)")).isZero();
            for (String frame : values(statement, "SELECT DISTINCT jsonb_array_elements_text(media_frames) FROM exercises")) {
                assertThat(getClass().getResource("/static" + frame)).as(frame).isNotNull();
            }
            assertThat(count(statement, "SELECT count(*) FROM exercises WHERE name = 'Press de banca plano' "
                    + "AND media_frames = '[\"/exercise-media/wger-73-1.webp\", \"/exercise-media/wger-73-2.webp\"]'::jsonb"))
                    .isEqualTo(1);
        }
    }

    private static Flyway flyway(String target) {
        var config = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration");
        if (target != null) {
            config.target(target);
        }
        return config.load();
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private static int count(Statement statement, String sql) throws SQLException {
        try (ResultSet row = statement.executeQuery(sql)) {
            row.next();
            return row.getInt(1);
        }
    }

    private static List<String> values(Statement statement, String sql) throws SQLException {
        List<String> values = new ArrayList<>();
        try (ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                values.add(rows.getString(1));
            }
        }
        return values;
    }
}
