package inpe.ccs.cora;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.MeterFilter;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.List;

@Singleton
public class MetricsConfiguration {
    @Produces
    @Singleton
    MeterFilter commonTags(@ConfigProperty(name = "quarkus.application.name") String application,
                           @ConfigProperty(name = "service.level") String level) {
        // Fixed deployment labels. Never add request IDs, names or payloads as metric tags.
        return MeterFilter.commonTags(List.of(Tag.of("application", application), Tag.of("level", level)));
    }
}
