package com.wingedsheep.gameserver.stats

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.OffsetDateTime

/** One change to the engine AI: an `ai/CHANGELOG.md` entry, placed on the commit that merged it to main. */
data class EngineAiChange(
    val sha: String,
    val committedAt: Instant,
    val pr: Int?,
    val subject: String,
    /** Position on main's first-parent line; higher is newer. Compared against [EngineAiTimeline.ordinalOf]. */
    val ordinal: Int,
)

/**
 * The engine AI's changelog as this build knows it, plus every main commit since its oldest entry —
 * so a deployed build's sha (`game_replays.engine_version`) can be placed on that history and
 * the AI changes it carried counted exactly, rather than guessed from dates.
 */
class EngineAiTimeline(
    /** False when the build had no git history to read (a shallow or source-only build). */
    val available: Boolean,
    /** Oldest first. */
    val changes: List<EngineAiChange>,
    private val ordinals: Map<String, Int>,
) {
    /** The position of the build [version] on main, or null for an unknown build (`dev`, pre-history). */
    fun ordinalOf(version: String): Int? =
        if (version.length < SHA_PREFIX) null else ordinals[version.take(SHA_PREFIX).lowercase()]

    companion object {
        const val SHA_PREFIX = 12
        val UNAVAILABLE = EngineAiTimeline(false, emptyList(), emptyMap())

        /**
         * Parse `engine-ai-history.tsv` (written by game-server's `generateEngineAiHistory` task):
         * newest first, one `sha12 TAB ISO TAB pr-or-dash TAB text` line per `ai/CHANGELOG.md` entry
         * on the commit that merged it, any other commit as a bare `sha12`, and `# unavailable` when
         * the build couldn't read git.
         */
        fun parse(text: String): EngineAiTimeline {
            val lines = text.lines().filter { it.isNotBlank() }
            if (lines.firstOrNull()?.trim() == "# unavailable") return UNAVAILABLE
            val commits = lines.filterNot { it.startsWith("#") }.asReversed()
            val ordinals = HashMap<String, Int>(commits.size * 2)
            val changes = ArrayList<EngineAiChange>()
            for (line in commits) {
                val parts = line.split('\t', limit = 4)
                val sha = parts[0].trim().lowercase()
                // A PR with several changelog entries writes one line each under the same sha.
                val ordinal = ordinals.getOrPut(sha) { ordinals.size }
                if (parts.size == 4) {
                    changes += EngineAiChange(
                        sha = sha,
                        committedAt = OffsetDateTime.parse(parts[1]).toInstant(),
                        pr = parts[2].toIntOrNull(),
                        subject = parts[3],
                        ordinal = ordinal,
                    )
                }
            }
            return EngineAiTimeline(true, changes, ordinals)
        }
    }
}

/** Loads the [EngineAiTimeline] baked into this build's jar, once. */
@Component
class EngineAiHistory {
    private val logger = LoggerFactory.getLogger(EngineAiHistory::class.java)

    val timeline: EngineAiTimeline by lazy {
        val text = javaClass.getResourceAsStream("/$RESOURCE")?.use { it.readBytes().toString(Charsets.UTF_8) }
        if (text == null) {
            logger.info("No {} on the classpath; AI change markers are unavailable", RESOURCE)
            EngineAiTimeline.UNAVAILABLE
        } else {
            runCatching { EngineAiTimeline.parse(text) }
                .onFailure { logger.warn("Unreadable {}: {}", RESOURCE, it.message) }
                .getOrDefault(EngineAiTimeline.UNAVAILABLE)
        }
    }

    private companion object {
        const val RESOURCE = "engine-ai-history.tsv"
    }
}
