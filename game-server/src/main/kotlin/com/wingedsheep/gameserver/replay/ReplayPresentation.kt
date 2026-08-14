package com.wingedsheep.gameserver.replay

import com.wingedsheep.gameserver.handler.MessageSender
import com.wingedsheep.gameserver.protocol.ServerMessage
import kotlinx.serialization.builtins.ListSerializer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * The archived *output* of a recorded game: the `{initialSnapshot, deltas}` stream the replay viewer
 * consumes, serialized once at record time and stored next to the compact input log.
 *
 * ## Why keep both
 * The input log is the good record — kilobytes, exact, and the only thing that can rebuild a real
 * [com.wingedsheep.engine.state.GameState] for "share frame as scenario". Its weakness is that it is
 * a *recipe*: reading it means re-running the engine, and the engine moves. [ReplayCardPin] pins the
 * part of the engine that moves most, and [ReplayFingerprint] catches the rest, but "catches" means
 * the viewer gets a truncated game — correct, and useless to the player who wanted to watch it.
 *
 * So at game over — the one moment we are provably running the build that played the game — we fold
 * the input log once and keep the frames it produces. That is a *result*, not a recipe: it renders
 * years later regardless of what happened to the engine, because nothing needs to be re-derived. It
 * is bigger (hundreds of KB gzipped against a handful for the inputs), which is exactly why it's the
 * fallback rather than the primary: [ReplayService] serves the re-simulation whenever it is faithful
 * and reaches for this only when it isn't.
 *
 * This is the same trade every deterministic-lockstep game makes when it wants replays to survive
 * patches — inputs for size, recorded output for longevity — and storing both is what lets us have
 * the compact record without betting the archive on it.
 *
 * Stored as the already-composed JSON body rather than a DTO: serving it is then a byte passthrough,
 * with no chance of a DTO reshape making an old archive unreadable.
 */
@Component
class ReplayPresentation(
    private val messageSender: MessageSender,
    /**
     * Skip archiving when the *stored* (gzipped) stream exceeds this many bytes. A full game is
     * ~160 KB stored, so the 4 MB default only bites on something pathological — a mill-loop
     * stalemate, an AI grinding 400 turns — where the frames cost far more than they're worth.
     * Those replays keep their input log and degrade to a truncated view in the unlikely event they
     * ever stop re-simulating. 0 disables archiving entirely.
     */
    @Value("\${game.replay.presentation-max-stored-bytes:4194304}")
    private val maxStoredBytes: Int,
) {
    private val logger = LoggerFactory.getLogger(ReplayPresentation::class.java)

    /**
     * Serialize [reconstructed] into the viewer body, or null when archiving is disabled, the
     * reconstruction is already broken (archiving a truncated stream just freezes the truncation),
     * or the stored form would exceed [maxStoredBytes].
     */
    fun materialize(reconstructed: ReconstructedReplay): String? {
        if (maxStoredBytes <= 0) return null
        if (!reconstructed.isComplete) {
            logger.warn("Not archiving a diverged reconstruction: {}", reconstructed.divergenceReason)
            return null
        }
        val body = compose(reconstructed)
        // Measured on the compressed form, because that is what the column costs — the raw body is
        // ~50x larger and a cap on it would reject perfectly ordinary games. The store re-encodes;
        // one extra gzip per finished game, on a background thread, is not worth avoiding.
        val storedBytes = ReplayCodec.encodeText(body).length
        if (storedBytes > maxStoredBytes) {
            logger.info(
                "Replay presentation is {} stored bytes (> {}) — keeping the input log only",
                storedBytes, maxStoredBytes,
            )
            return null
        }
        return body
    }

    /** The `{"initialSnapshot":…,"deltas":[…]}` body, exactly as the replay endpoints serve it. */
    fun compose(reconstructed: ReconstructedReplay): String {
        val initial = messageSender.json.encodeToString(
            ServerMessage.SpectatorStateUpdate.serializer(),
            reconstructed.initialSnapshot,
        )
        val deltas = messageSender.json.encodeToString(
            ListSerializer(SpectatorReplayDelta.serializer()),
            reconstructed.deltas,
        )
        return """{"initialSnapshot":$initial,"deltas":$deltas}"""
    }
}
