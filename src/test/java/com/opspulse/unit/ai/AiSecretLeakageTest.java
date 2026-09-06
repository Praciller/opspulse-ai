package com.opspulse.unit.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AiSecretLeakageTest {

    @Test
    void sourceDoesNotContainHardCodedProviderKeysOrAuthorizationValues() throws IOException {
        try (Stream<Path> files = Files.walk(Path.of("src/main"))) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java")
                            || path.toString().endsWith(".yml")
                            || path.toString().endsWith(".yaml"))
                    .forEach(path -> {
                        try {
                            String source = Files.readString(path);
                            assertThat(source)
                                    .as("provider secret pattern in %s", path)
                                    .doesNotMatch("(?s).*\\bsk-[A-Za-z0-9]{20,}\\b.*");
                            assertThat(source)
                                    .as("hard-coded authorization value in %s", path)
                                    .doesNotMatch("(?is).*authorization\\s*[:=]\\s*[\\\"']bearer\\s+[A-Za-z0-9._-]{20,}[\\\"'].*");
                        } catch (IOException exception) {
                            throw new IllegalStateException("Unable to inspect " + path, exception);
                        }
                    });
        }
    }
}
