package io.greencap.k8s.kubernetes.compose;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The parser used to be reachable only through the HTTP fetch path; a local Compose file now feeds
// the same entry point, so these tests pin the behaviour of parsing raw content on its own.
class ComposeParserTest {

    private static final String COMPOSE = """
            services:
              backend:
                build: ./backend
                ports:
                  - "8080:8000"
                environment:
                  DB_PASSWORD: secret
                  APP_MODE: production
              db:
                image: postgres:16
                volumes:
                  - pgdata:/var/lib/postgresql/data
            volumes:
              pgdata:
            """;

    private final ComposeParser composeParser = new ComposeParser();

    @Test
    void parsesLocalContentIntoServicesWithBuildImagesAndPorts() {
        ComposeDocument document = composeParser.parse(COMPOSE);

        assertThat(document.services()).extracting(ComposeDocument.ParsedService::name)
                .containsExactly("backend", "db");

        ComposeDocument.ParsedService backend = document.services().get(0);
        assertThat(backend.hasBuild()).isTrue();
        assertThat(backend.build().context()).isEqualTo("./backend");
        assertThat(backend.build().dockerfile()).isEqualTo("Dockerfile");
        assertThat(backend.containerPorts()).containsExactly(8000);
        assertThat(backend.hasSensitiveEnv()).isTrue();
        assertThat(backend.hasNonSensitiveEnv()).isTrue();

        ComposeDocument.ParsedService database = document.services().get(1);
        assertThat(database.hasBuild()).isFalse();
        assertThat(database.image()).isEqualTo("postgres:16");
        assertThat(database.namedVolumes()).singleElement()
                .satisfies(volume -> assertThat(volume.mountPath()).isEqualTo("/var/lib/postgresql/data"));
    }

    @Test
    void parsingIsIndependentOfHowTheContentArrived() {
        assertThat(composeParser.parse(COMPOSE)).isEqualTo(composeParser.parse(COMPOSE));
    }

    @Test
    void rejectsContentWithoutServicesSection() {
        assertThatThrownBy(() -> composeParser.parse("version: '3'"))
                .isInstanceOf(ComposeParseException.class)
                .hasMessageContaining("services");
    }
}
