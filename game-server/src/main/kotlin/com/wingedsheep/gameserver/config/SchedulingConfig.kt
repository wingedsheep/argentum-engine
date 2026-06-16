package com.wingedsheep.gameserver.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

/**
 * Scheduler backing `@EnableScheduling` / `@Scheduled` (e.g. [com.wingedsheep.gameserver.session.ZombieSessionSweeper]).
 *
 * Defining a [org.springframework.scheduling.TaskScheduler] bean replaces the one Spring Boot would
 * otherwise auto-create — and that default scheduler uses **non-daemon** threads (the `scheduling-N`
 * threads). A live non-daemon thread keeps the JVM alive.
 *
 * In production the scheduler is stopped on context close, so daemon-ness is just a safety net there.
 * It matters in tests: `@SpringBootTest` contexts are cached and only closed by a JVM shutdown hook,
 * and a shutdown hook only fires once the JVM is *already* exiting — which a live non-daemon thread
 * prevents. The Gradle test-worker JVM then never terminates after the last test, Gradle blocks
 * waiting to reap it, and the backend CI build freezes at `:game-server:build` with `BUILD SUCCESSFUL`
 * never printed. Daemon threads break that deadlock.
 *
 * This mirrors the same fix already applied to `SessionRegistry.disconnectScheduler`; that one was
 * not the only non-daemon scheduler in the context.
 */
@Configuration
class SchedulingConfig {

    @Bean
    fun taskScheduler(): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 2
            setThreadNamePrefix("scheduling-")
            setDaemon(true)
        }
}
