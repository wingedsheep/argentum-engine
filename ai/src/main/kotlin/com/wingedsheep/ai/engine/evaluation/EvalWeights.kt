package com.wingedsheep.ai.engine.evaluation

import com.wingedsheep.ai.engine.AiProfile
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.training.ApprenticeArtifactLoader
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path

/**
 * The five-feature fallback evaluator's coefficients.
 *
 * Phase 9 will add a raw-feature evaluator; keeping this vector loadable makes the existing
 * evaluator a safe fallback and gives the arena a resource-backed A/B seam in the meantime.
 */
@Serializable
data class EvaluationWeights(
    val life: Double = 1.0,
    val boardPresence: Double = 1.5,
    val cardAdvantage: Double = 1.0,
    val threatAssessment: Double = 1.2,
    val tempo: Double = 0.6,
    /**
     * What an empty hand is worth, in the same units as [CardAdvantage]'s curve.
     *
     * A constant rather than a coefficient, and the one place the hand curve is not concave.
     * Marginal value runs 1st card **4.0**, 2nd 1.5, 3rd 1.5, 4th 0.8 — every card after the first
     * is worth less than the one before it, and then the first breaks the pattern by a factor of
     * nearly three. That spike is measurable as wrong play: it makes holding the last card beat
     * casting a spell that would put a 2/2 on the board, which is `sequencing-02` (never plays the
     * last land) and `noncreature-02` (declines a Disenchant that gains 3.6 against a 4.0 charge,
     * missing by 0.40).
     *
     * `-3.0` is the historical value and the default, because [AiProfile.LEGACY_V0] is the frozen
     * reference every published number is quoted against. A vector that sets this to `-1.0` makes
     * the curve concave everywhere — the first card still leads at 2.0 — without touching a single
     * board-side constant, which is the distinction between fixing the guess that is wrong and
     * inflating a second guess until the two cancel.
     */
    val topdeckPenalty: Double = -3.0,
) {
    fun toEvaluator(
        intents: IntentCatalog = IntentCatalog.NONE,
        landDropIsNotCardLoss: Boolean = false,
        sequenceLandsByUsableMana: Boolean = false,
        discountedRaceClock: Boolean = false,
        creatureValuation: CreatureValuation = CreatureValuation.LEGACY,
        priceLandsInHandAsMana: Boolean = false,
    ): BoardEvaluator = CompositeBoardEvaluator(
        listOf(
            life to LifeDifferential,
            boardPresence to BoardFeature { state, projected, playerId ->
                BoardPresence.score(
                    state, projected, playerId, intents, sequenceLandsByUsableMana, creatureValuation,
                )
            },
            cardAdvantage to BoardFeature { state, projected, playerId ->
                CardAdvantage.score(
                    state, projected, playerId, topdeckPenalty, landDropIsNotCardLoss,
                    priceLandsInHandAsMana,
                )
            },
            threatAssessment to BoardFeature { state, projected, playerId ->
                ThreatAssessment.score(state, projected, playerId, discountedRaceClock)
            },
            tempo to Tempo,
        )
    )

    companion object {
        /** Compiled fallback: resource loading can never make the production AI unavailable. */
        val DEFAULT = EvaluationWeights()
        val BLIND = EvaluationWeights(0.0, 0.0, 0.0, 0.0, 0.0)
    }
}

/**
 * Resource-backed evaluation vectors, keyed by the stable id carried by [AiProfile].
 *
 * A bad tuning artifact deliberately fails closed to [EvaluationWeights.DEFAULT]. Evaluation
 * tuning is an optimization, not a reason for a game server to fail during startup.
 */
object EvalWeights {
    const val DEFAULT_ID = "default"
    private const val RESOURCE = "ai/eval-weights.json"
    private const val RAW_RESOURCE = "ai/raw-eval-weights.json"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = false
    }

    private val resourceWeights: Map<String, EvaluationWeights> by lazy {
        runCatching {
            val text = EvalWeights::class.java.classLoader
                .getResourceAsStream(RESOURCE)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: return@runCatching emptyMap()
            decodeOrEmpty(text)
        }.getOrDefault(emptyMap())
    }

    private val rawResourceWeights: Map<String, RawEvaluationWeights> by lazy {
        runCatching {
            val text = EvalWeights::class.java.classLoader
                .getResourceAsStream(RAW_RESOURCE)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: return@runCatching emptyMap()
            decodeRawOrEmpty(text)
        }.getOrDefault(emptyMap())
    }

    /** Optional offline-installed artifacts, loaded once and shared. No artifact is required. */
    private val apprenticeWeights: Map<String, RawEvaluationWeights> by lazy {
        val directory = System.getProperty("argentum.ai.apprentice.dir")?.takeIf { it.isNotBlank() }
            ?: return@lazy emptyMap()
        listOf("shared-apprentice", "ecl-apprentice", "ecl-overlay").mapNotNull { id ->
            val path = Path.of(directory, "$id.json")
            if (!Files.isRegularFile(path)) return@mapNotNull null
            val expectedSet = if (id.startsWith("ecl-")) "ECL" else null
            ApprenticeArtifactLoader.decodeOrNull(Files.readString(path), expectedSet)?.let { artifact ->
                id to artifact.toEvaluationWeights(applyOverlay = id == "ecl-overlay")
            }
        }.toMap()
    }

    fun resolve(id: String): EvaluationWeights =
        resourceWeights[id]?.takeIf(::isFinite) ?: EvaluationWeights.DEFAULT

    /**
     * [landDropIsNotCardLoss], [sequenceLandsByUsableMana], [discountedRaceClock],
     * [creatureValuation] and [priceLandsInHandAsMana] reach only the composite fallback: the raw Phase 9 vectors price
     * `myHandSize` linearly, so a land drop already costs them one fitted coefficient with no cliff
     * to step off, and changing what any of them count would silently invalidate the fit.
     */
    fun resolveEvaluator(
        id: String,
        intents: IntentCatalog,
        landDropIsNotCardLoss: Boolean = false,
        sequenceLandsByUsableMana: Boolean = false,
        discountedRaceClock: Boolean = false,
        creatureValuation: CreatureValuation = CreatureValuation.LEGACY,
        priceLandsInHandAsMana: Boolean = false,
    ): BoardEvaluator =
        apprenticeWeights[id]?.takeIf(RawEvaluationWeights::isValid)?.toEvaluator(intents)
            ?: rawResourceWeights[id]?.takeIf(RawEvaluationWeights::isValid)?.toEvaluator(intents)
            ?: resolve(id).toEvaluator(
                intents, landDropIsNotCardLoss, sequenceLandsByUsableMana, discountedRaceClock,
                creatureValuation, priceLandsInHandAsMana,
            )

    /** Whether [id] selects a complete, finite raw vector rather than the composite fallback. */
    fun isRawProfile(id: String): Boolean = apprenticeWeights[id]?.isValid() == true || rawResourceWeights[id]?.isValid() == true

    fun winProbabilityScale(id: String): Double =
        (apprenticeWeights[id] ?: rawResourceWeights[id])?.takeIf(RawEvaluationWeights::isValid)?.winProbabilityScale
            ?: com.wingedsheep.ai.engine.rollout.WinProbability.SCALE

    /** Stable ids available to tooling such as the arena agent registry. */
    val ids: Set<String> get() = resourceWeights.keys + rawResourceWeights.keys + apprenticeWeights.keys

    internal fun decode(text: String): Map<String, EvaluationWeights> =
        json.decodeFromString<Map<String, EvaluationWeights>>(text)

    internal fun decodeOrEmpty(text: String): Map<String, EvaluationWeights> =
        runCatching { decode(text) }.getOrDefault(emptyMap())

    internal fun decodeRaw(text: String): Map<String, RawEvaluationWeights> =
        json.decodeFromString<Map<String, RawEvaluationWeights>>(text)

    internal fun decodeRawOrEmpty(text: String): Map<String, RawEvaluationWeights> =
        runCatching { decodeRaw(text) }.getOrDefault(emptyMap())

    private fun isFinite(weights: EvaluationWeights): Boolean =
        weights.life.isFinite() &&
            weights.boardPresence.isFinite() &&
            weights.cardAdvantage.isFinite() &&
            weights.threatAssessment.isFinite() &&
            weights.tempo.isFinite()
}
