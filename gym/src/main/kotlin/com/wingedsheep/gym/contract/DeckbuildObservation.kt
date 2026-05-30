package com.wingedsheep.gym.contract

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Observation for a **deckbuild** env — a solitaire environment in which one agent
 * turns an opened sealed pool into a legal deck.
 *
 * The agent reads [pool] (and the unlimited [basics]), then drives [Observation.legalActions]
 * — `ADD_CARD` / `REMOVE_CARD` / `ADD_BASIC` / `FINALIZE` — until the deck is legal and it
 * commits with `FINALIZE`. On finalize the env is [Observation.terminated]; [selected] is the
 * finished decklist (`cardName → count`) the caller feeds to a game env via
 * `DeckSpec.Explicit`, and [deckScore] is an intrinsic quality estimate (see
 * `com.wingedsheep.ai.engine.LimitedCardRater`).
 *
 * Unlike a game observation there is no hidden information and no engine `pendingDecision`;
 * the whole pool is visible and every choice is an enumerated legal action.
 */
@Serializable
@SerialName("Deckbuild")
data class DeckbuildObservation(
    override val schemaHash: String,

    /** The (synthetic) player building this deck. */
    override val agentToAct: EntityId?,

    /** The opened sealed pool — each distinct card with how many copies remain unselected. */
    val pool: List<PoolCardView>,

    /** Basic lands available to add without limit (one entry per land name). */
    val basics: List<PoolCardView>,

    /** Current decklist: card name → count (includes added basics). */
    val selected: Map<String, Int>,

    /** Total cards currently in [selected]. */
    val selectedCount: Int,

    /** Minimum legal deck size; `FINALIZE` is affordable once [selectedCount] reaches it. */
    val targetSize: Int,

    /** Intrinsic deck-quality estimate. Only populated once [Observation.terminated]. */
    val deckScore: Double? = null,

    override val pendingDecision: PendingDecisionView? = null,
    override val legalActions: List<LegalActionView>,
    override val terminated: Boolean,
    override val stateDigest: String
) : Observation

/**
 * A card as seen in a deckbuild pool. Carries enough for an agent to evaluate it —
 * name, cost, color/type, P/T, and oracle text — plus how many copies are still
 * available to add ([remaining]).
 */
@Serializable
data class PoolCardView(
    val name: String,
    val manaCost: String,
    val manaValue: Int,
    val colors: Set<String>,
    val types: Set<String>,
    val subtypes: Set<String>,
    /** "power/toughness" for creatures, null otherwise. */
    val pt: String? = null,
    val oracleText: String = "",
    /** Copies of this card still available to add (selected copies subtracted). */
    val remaining: Int
)
