package com.wingedsheep.assay.explore

import com.wingedsheep.sdk.model.CardDefinition
import kotlinx.serialization.Serializable

/**
 * What the engine AI understands of a card — an optional panel on the explorer's card page.
 *
 * ## Why a hook and not a dependency
 *
 * The readers that answer this live in `:ai`, which depends on the rules engine. `:oracle-assay`
 * depends on the SDK and its tooling and nothing else, on purpose (see its build file): Assay must
 * never inherit the engine. So the explorer only declares the *shape* of the answer, and whoever
 * mounts it may supply one. `game-server` has both modules on its classpath and passes an
 * implementation backed by `AiReadingAudit`; standalone `just assay-explore` passes none, and the
 * card page says the panel needs the game server.
 *
 * Nothing here decides what the AI reads. The explorer only picks *which* definition to hand over —
 * the engine's own, else the hand-written golden, else Assay's whole-card compile as a labelled
 * preview — and renders the answer.
 */
interface AiReadingProvider {

    /** The engine's own definition of the card named [name], or null when the engine has none. */
    fun implemented(name: String): CardDefinition?

    /** What the AI's static readers make of each ability of [card]. */
    fun read(card: CardDefinition): AiReadingView

    /**
     * Whether the AI reads the implemented card [name] fully: null when the engine does not implement
     * it, or the answer is not known yet (see [corpusReady]). Backs the card list's "AI gaps" filter,
     * so it must be cheap — an index lookup, not a read.
     */
    fun readsFully(name: String): Boolean? = null

    /**
     * Whether [readsFully] has an answer for every implemented card yet. A provider that indexes the
     * corpus in the background says false until it is done, and the card list says so instead of
     * showing a filter that silently matches too little.
     */
    fun corpusReady(): Boolean = true

    /**
     * How the AI rates [card] — for draft and deckbuilding, and as a permanent in play. Null when the
     * provider has no rating to give. The provider decides which sets the card is printed in; the
     * draft half reports one row per set.
     */
    fun rate(card: CardDefinition): AiRatingView? = null
}

/** One card's reading. [abilities] per face: the spell, triggered, activated, then static. */
@Serializable
data class AiReadingView(
    val readFully: Boolean,
    val abilities: List<AiAbilityView>,
    /** The corpus audit's per-card flag keys (`intent: AddCountersEffect`), for a developer. */
    val flags: List<String>,
)

@Serializable
data class AiAbilityView(
    /** `SPELL`, `ACTIVATED`, `TRIGGERED`, `STATE_TRIGGER` or `STATIC`. */
    val kind: String,
    /** "Spell", "Activated ability 2". */
    val title: String,
    /** The face it is on, for a card with more than one; null otherwise. */
    val face: String? = null,
    /** The ability's rules text as the SDK describes it. */
    val text: String,
    /** Whether any reader was asked about it — static abilities are not. */
    val checked: Boolean,
    val readFully: Boolean,
    val findings: List<AiFindingView>,
)

@Serializable
data class AiFindingView(
    /** The reader, in words: "Target polarity", "Card intent", … */
    val reader: String,
    /** The audit check's short name: `polarity`, `intent`, `combat`, `timing`, `modal`. */
    val check: String,
    val read: Boolean,
    /** The answer in plain English: "target 1: can't tell if this helps or hurts". */
    val reason: String,
    /** The SDK effect type it is about — secondary detail for a developer. */
    val effectType: String? = null,
    /** 1-based step of the ability's effect the finding points at. */
    val step: Int? = null,
    val stepText: String? = null,
    /** 1-based target slot. */
    val target: Int? = null,
    /** 1-based mode of a modal ability. */
    val mode: Int? = null,
)

/**
 * How the engine AI rates a card. Every number is on its own scale — [pick] on the 0–5 limited pick
 * scale, [board] in the evaluator's board-value points — and every part list sums to its number, so
 * the page can draw each as a segmented bar.
 */
@Serializable
data class AiRatingView(
    val pick: AiPickView,
    val board: AiBoardView,
    val intent: AiIntentView,
    /** The hand-written card advisor the live AI consults, by class name; null for most cards. */
    val advisor: String? = null,
)

@Serializable
data class AiPickView(
    /** What decks built outside a set's draft use: sealed pools, generated decks, constructed. */
    val rating: Double,
    /** `basicLand`, `land`, `winRate`, `table` or `heuristic`. */
    val source: String,
    val tableSet: String? = null,
    val winRate: Double? = null,
    val games: Int? = null,
    /** The effect heuristic's terms, always — they sum to [heuristicRating] before its 0–5 clamp. */
    val heuristic: List<AiPartView>,
    val heuristicRating: Double,
    /** The draft pick scorer's starting number, per set the card is printed in. */
    val sets: List<AiSetPickView>,
)

@Serializable
data class AiSetPickView(
    val set: String,
    val rating: Double? = null,
    val fromTable: Boolean,
    val removal: Boolean,
    val archetypes: List<String> = emptyList(),
)

@Serializable
data class AiPartView(val label: String, val value: Double, val detail: String? = null)

@Serializable
data class AiBoardView(
    /** `creature`, `variableCreature`, `permanent`, `planeswalker`, `land` or `spell`. */
    val kind: String,
    val value: Double? = null,
    val parts: List<AiPartView>,
    /** A sentence on what the number leaves to play. */
    val rule: String? = null,
)

@Serializable
data class AiIntentView(val tags: List<AiTagView>, val speed: String, val facts: List<String>)

@Serializable
data class AiTagView(val key: String, val label: String)
