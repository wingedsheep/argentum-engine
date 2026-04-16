package com.wingedsheep.gym.trainer.defaults

import com.wingedsheep.gym.trainer.spi.EvaluationResult
import com.wingedsheep.gym.trainer.spi.Evaluator
import com.wingedsheep.gym.trainer.spi.SlotEncoding
import com.wingedsheep.gym.trainer.spi.TrainerContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * JSON-over-HTTP policy/value inference, expected to be the most common
 * way projects wire their Python training server into the Kotlin trainer.
 *
 * Wire format — request:
 * ```json
 * {
 *   "features": <featuresJson>,
 *   "legal_slots": [{"head": "actions", "slot": 3}, ...],
 *   "decision": {"is_priority": true, "player_id": "..."}
 * }
 * ```
 * Wire format — response:
 * ```json
 * {
 *   "priors": {"actions": [0.1, 0.4, 0.2, 0.3]},
 *   "value": 0.25
 * }
 * ```
 *
 * The feature serializer is supplied by the caller — this evaluator doesn't
 * know what `T` is. For [StructuralFeatures] see
 * `RemoteHttpEvaluator.forStructural()`.
 *
 * Projects that want a different codec (MessagePack, Protobuf, custom
 * batching) should subclass this or write their own `Evaluator` from
 * scratch — the SPI is only four lines.
 */
open class RemoteHttpEvaluator<T>(
    private val url: String,
    private val featureSerializer: KSerializer<T>,
    private val timeout: Duration = Duration.ofSeconds(30),
    private val json: Json = DEFAULT_JSON
) : Evaluator<T> {

    private val client: HttpClient = HttpClient.newBuilder()
        .connectTimeout(timeout)
        .build()

    override fun evaluate(
        features: T,
        legalSlots: List<SlotEncoding>,
        ctx: TrainerContext
    ): EvaluationResult {
        val body = Request(
            features = json.encodeToJsonElement(featureSerializer, features).toString(),
            legalSlots = legalSlots.map { SlotDto(it.head, it.slot) },
            decision = DecisionDto(
                isPriority = ctx.isPriority,
                playerId = ctx.playerId.value
            )
        )
        val payload = json.encodeToString(Request.serializer(), body)

        val req = HttpRequest.newBuilder(URI.create(url))
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build()

        val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
        if (resp.statusCode() !in 200..299) {
            error("Remote evaluator returned HTTP ${resp.statusCode()}: ${resp.body()}")
        }
        val parsed = json.decodeFromString(Response.serializer(), resp.body())
        val priors = parsed.priors.mapValues { (_, list) -> list.toFloatArray() }
        return EvaluationResult(priors = priors, value = parsed.value)
    }

    @Serializable
    private data class Request(
        val features: String,              // pre-serialized JSON so feature serializer runs once
        val legalSlots: List<SlotDto>,
        val decision: DecisionDto
    )

    @Serializable
    private data class SlotDto(val head: String, val slot: Int)

    @Serializable
    private data class DecisionDto(val isPriority: Boolean, val playerId: String)

    @Serializable
    private data class Response(val priors: Map<String, List<Float>>, val value: Float)

    companion object {
        val DEFAULT_JSON: Json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

        /** Convenience factory for the common case where features are [StructuralFeatures]. */
        fun forStructural(
            url: String,
            timeout: Duration = Duration.ofSeconds(30)
        ): RemoteHttpEvaluator<StructuralFeatures> = RemoteHttpEvaluator(
            url = url,
            featureSerializer = StructuralFeatures.serializer(),
            timeout = timeout
        )

        /** Tiny helper — used in tests for list→FloatArray conversion. */
        private fun List<Float>.toFloatArray(): FloatArray = FloatArray(size) { this[it] }
    }
}
