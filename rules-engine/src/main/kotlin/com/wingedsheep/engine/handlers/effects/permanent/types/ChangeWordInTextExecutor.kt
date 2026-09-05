package com.wingedsheep.engine.handlers.effects.permanent.types

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ChangeWordInTextEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Lowercased word tokens of a card's oracle text (letters with intra-word hyphens/apostrophes kept,
 * e.g. "Assembly-Worker"). Used to detect which color words / creature types a text-change will
 * actually affect, since MTG text-changing alters "all instances of [a word]" in the rules text.
 */
internal fun oracleWords(oracleText: String?): Set<String> =
    if (oracleText.isNullOrBlank()) emptySet()
    else Regex("[^A-Za-z'\\-]+").split(oracleText).filter { it.isNotBlank() }.map { it.lowercase() }.toSet()

/**
 * Executor for [ChangeWordInTextEffect] (Crystal Spray).
 *
 * "Change the text of target spell or permanent by replacing all instances of one
 * color word with another or one basic land type with another."
 *
 * This executor:
 * 1. Resolves the target (battlefield or stack).
 * 2. Presents a ChooseOptionDecision with the FROM options — the five color words
 *    followed by the five basic land types.
 * 3. Pushes a [ChooseFromWordContinuation].
 *
 * The continuation handler ([com.wingedsheep.engine.handlers.continuations.WordChangeChoiceContinuationResumer])
 * handles the TO choice (constrained to the same category) and attaches the
 * [com.wingedsheep.engine.state.components.identity.TextReplacementComponent].
 */
class ChangeWordInTextExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ChangeWordInTextEffect> {

    override val effectType: KClass<ChangeWordInTextEffect> = ChangeWordInTextEffect::class

    companion object {
        /** Color words offered as FROM/TO options, in WUBRG order. */
        val COLOR_WORDS: List<String> = Color.entries.map { it.displayName }

        /** Combined FROM options: color words then basic land types. */
        val ALL_WORDS: List<String> = COLOR_WORDS + Subtype.ALL_BASIC_LAND_TYPES.toList()

        /** Color of the basic land type whose word this is (for the mana pip), or the color word itself. */
        private val WORD_TO_COLOR: Map<String, Color> = buildMap {
            Color.entries.forEach { put(it.displayName, it) }
            put("Plains", Color.WHITE)
            put("Island", Color.BLUE)
            put("Swamp", Color.BLACK)
            put("Mountain", Color.RED)
            put("Forest", Color.GREEN)
        }

        /** Mana-pip iconKey ("pip_w".."pip_g") for a color/land word, for the inline web-client pip. */
        fun pipKeyFor(word: String): String? = WORD_TO_COLOR[word]?.let { "pip_${it.symbol.lowercaseChar()}" }

        /**
         * The words that actually appear on the target — and so are the ones a replacement
         * will visibly affect: basic land types in its (projected) type line, plus colors it
         * has protection from or names in its rules text ("red" / "nonred"). Replacing any
         * other word is legal but commonly a no-op, so we surface these first and label them.
         */
        fun relevantWords(state: GameState, targetId: EntityId): Set<String> {
            val projected = state.projectedState
            val subtypes = projected.getSubtypes(targetId)
            val keywords = projected.getKeywords(targetId)
            val words = oracleWords(state.getEntity(targetId)?.get<CardComponent>()?.oracleText)
            return buildSet {
                Subtype.ALL_BASIC_LAND_TYPES.forEach { if (it in subtypes) add(it) }
                Color.entries.forEach { c ->
                    val name = c.displayName.lowercase()
                    if ("PROTECTION_FROM_${c.name}" in keywords || name in words || "non$name" in words) {
                        add(c.displayName)
                    }
                }
            }
        }

        /** Orders [ALL_WORDS] so relevant (on-card) words come first, each group in WUBRG/land order. */
        fun orderWords(relevant: Set<String>): List<String> =
            ALL_WORDS.filter { it in relevant } + ALL_WORDS.filter { it !in relevant }

        /**
         * Per-option metadata aligned to [words]: a small mana pip ("pip_w"..) the web client
         * renders inline, plus a label. Words present on [targetName] are tagged "On <card>"
         * so the player can tell which choices will actually do something; the rest show their
         * category ("Color word" / "Basic land type") so a color ("Red") reads apart from a
         * basic land type ("Mountain").
         */
        fun metadataFor(words: List<String>, relevant: Set<String>, targetName: String?): List<OptionMetadata> =
            words.map { word ->
                val category = if (word in COLOR_WORDS) "Color word" else "Basic land type"
                val description = if (word in relevant && targetName != null) "On $targetName — $category" else category
                OptionMetadata(id = word, description = description, iconKey = pipKeyFor(word))
            }

        /** Prompt naming the card whose text is being changed. */
        fun prompt(targetName: String?): String =
            if (targetName != null) "Change a color word or basic land type in $targetName's text"
            else "Change a color word or basic land type"

        /**
         * Index-aligned to [fromOptions]: entry i lists the [toOptions] indices that keep the
         * replacement in the same category (color↔color, land↔land) and exclude the chosen word.
         */
        fun allowedToByFrom(fromOptions: List<String>, toOptions: List<String>): List<List<Int>> =
            fromOptions.map { from ->
                val sameCategory = if (from in COLOR_WORDS) COLOR_WORDS else Subtype.ALL_BASIC_LAND_TYPES
                toOptions.indices.filter { toOptions[it] in sameCategory && toOptions[it] != from }
            }
    }

    override fun execute(
        state: GameState,
        effect: ChangeWordInTextEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state.tick())

        // Target must still exist (on battlefield or stack)
        if (targetId !in state.getBattlefield() && targetId !in state.stack) {
            return EffectResult.success(state.tick())
        }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val targetName = state.getEntity(targetId)?.get<CardComponent>()?.name

        val relevant = relevantWords(state, targetId)
        val fromOptions = orderWords(relevant)            // on-card words first
        val toOptions = ALL_WORDS                          // every word is a candidate replacement
        // Pre-select the first on-card word so the common single-relevant case is one click.
        val defaultFromIndex = fromOptions.indexOfFirst { it in relevant }.takeIf { it >= 0 }

        val decisionId = UUID.randomUUID().toString()
        val decision = ChooseReplacementDecision(
            id = decisionId,
            playerId = context.controllerId,
            prompt = prompt(targetName),
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            fromOptions = fromOptions,
            toOptions = toOptions,
            fromMetadata = metadataFor(fromOptions, relevant, targetName),
            toMetadata = metadataFor(toOptions, emptySet(), null),
            allowedToByFrom = allowedToByFrom(fromOptions, toOptions),
            defaultFromIndex = defaultFromIndex
        )

        val continuation = ChooseReplacementContinuation(
            decisionId = decisionId,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            objectReferences = context.objectReferences,
            sourceName = sourceName,
            targetId = targetId,
            fromOptions = fromOptions,
            toOptions = toOptions,
            mode = ReplacementMode.WORD,
            duration = effect.duration
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = context.controllerId,
                    decisionType = "CHOOSE_REPLACEMENT",
                    prompt = decision.prompt
                )
            )
        )
    }
}
