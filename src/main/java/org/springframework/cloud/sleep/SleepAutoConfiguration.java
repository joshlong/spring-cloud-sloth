package org.springframework.cloud.sleep;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication
public class SleepAutoConfiguration {

    @Bean
    public SleepFilter sleepFilter() {
        return new SleepFilter();
    }
}
