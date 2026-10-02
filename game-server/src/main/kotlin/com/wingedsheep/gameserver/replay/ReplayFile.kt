package com.wingedsheep.gameserver.replay

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Base64
import java.util.zip.GZIPInputStream

/** A replay file that can't be watched, with a message fit to show the person who uploaded it. */
class ReplayFileException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * The portable, downloadable form of a [CompactReplay]: its plain JSON, exactly as [ReplayCodec]
 * would store it minus the gzip+base64 wrapping. Exported for anyone to keep, and uploaded back to
 * be re-simulated and watched without being stored.
 *
 * An uploaded file is untrusted input that we then *run* — it seeds a game, names its decks,
 * supplies pinned card definitions, and drives the engine through its action list — so parsing it
 * caps every dimension that scales the cost of that run before reconstruction ever sees it. Gzipped
 * files are accepted too (`gzip replay.json` is the obvious way to shrink one), with the inflated
 * size capped so a small upload can't expand into a large one. So is [ReplayCodec.encode]'s
 * storage form (base64 of gzip) — what tools writing `.replay` files straight from the codec, such
 * as the trainer's self-play dumps, produce.
 */
object ReplayFile {

    /** Largest upload accepted, compressed or not. A long real game with pins is well under 2 MB. */
    const val MAX_UPLOAD_BYTES = 8 * 1024 * 1024

    /** Largest a gzipped upload may inflate to. */
    const val MAX_JSON_BYTES = 32 * 1024 * 1024

    const val MAX_PLAYERS = 8
    const val MAX_CARDS_PER_DECK = 500
    const val MAX_PINNED_CARDS = 4_000

    fun fileName(gameId: String): String = "argentum-replay-$gameId.json"

    fun export(replay: CompactReplay): String = ReplayCodec.encodeJson(replay)

    /**
     * Read at most [MAX_UPLOAD_BYTES] (+1, to tell "exactly at the limit" from "over it") from
     * [input] and parse it. Reading bounded rather than taking a pre-buffered body means an
     * oversized upload is refused after 8 MB, not after however much the client chose to send.
     */
    fun read(input: InputStream): CompactReplay {
        val bytes = input.readNBytes(MAX_UPLOAD_BYTES + 1)
        if (bytes.size > MAX_UPLOAD_BYTES) {
            throw ReplayFileException("Replay file is too large (limit ${MAX_UPLOAD_BYTES / (1024 * 1024)} MB).")
        }
        return parse(bytes)
    }

    fun parse(bytes: ByteArray): CompactReplay {
        if (bytes.isEmpty()) throw ReplayFileException("Replay file is empty.")
        val unwrapped = unwrapBase64(bytes)
        val json = if (isGzip(unwrapped)) inflate(unwrapped) else unwrapped.toString(Charsets.UTF_8)
        val replay = try {
            ReplayCodec.decodeJson(json)
        } catch (e: Exception) {
            throw ReplayFileException("Not an Argentum replay file.", e)
        }
        validate(replay)
        return replay
    }

    private fun validate(replay: CompactReplay) {
        val players = replay.setup.players
        if (players.isEmpty() || players.size > MAX_PLAYERS) {
            throw ReplayFileException("Replay has ${players.size} players; expected 1–$MAX_PLAYERS.")
        }
        if (replay.actions.size > ReplayRecordingPolicy.MAX_RECORDED_ACTIONS) {
            throw ReplayFileException(
                "Replay has ${replay.actions.size} actions; at most " +
                    "${ReplayRecordingPolicy.MAX_RECORDED_ACTIONS} are supported."
            )
        }
        for (player in players) {
            val deck = player.deck
            val size = maxOf(deck.cards.size, deck.cardEntries.size) + deck.sideboard.size
            if (size > MAX_CARDS_PER_DECK) {
                throw ReplayFileException("Deck for ${player.name} has $size cards; at most $MAX_CARDS_PER_DECK are supported.")
            }
        }
        if (replay.pinnedCards.size > MAX_PINNED_CARDS) {
            throw ReplayFileException("Replay pins ${replay.pinnedCards.size} card definitions; at most $MAX_PINNED_CARDS are supported.")
        }
    }

    /**
     * The decoded bytes when [bytes] is base64 text (the stored codec form), else [bytes] itself.
     * JSON starts with `{` after optional whitespace, which is never base64, so plain files pass
     * straight through. Base64 inflates by 4/3, so the decoded bytes stay under the upload cap.
     */
    private fun unwrapBase64(bytes: ByteArray): ByteArray {
        if (isGzip(bytes)) return bytes
        val text = bytes.toString(Charsets.US_ASCII).trim()
        if (text.isEmpty() || text.startsWith("{")) return bytes
        return try {
            Base64.getMimeDecoder().decode(text)
        } catch (e: IllegalArgumentException) {
            bytes
        }
    }

    private fun isGzip(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()

    private fun inflate(bytes: ByteArray): String {
        val inflated = try {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readNBytes(MAX_JSON_BYTES + 1) }
        } catch (e: Exception) {
            throw ReplayFileException("Replay file is not valid gzip.", e)
        }
        if (inflated.size > MAX_JSON_BYTES) {
            throw ReplayFileException("Replay file is too large once decompressed.")
        }
        return inflated.toString(Charsets.UTF_8)
    }
}
