package com.wingedsheep.gym.deckbuild

import com.wingedsheep.ai.engine.LimitedCardRater
import com.wingedsheep.gym.GymEnv
import com.wingedsheep.gym.contract.ActionRegistry
import com.wingedsheep.gym.contract.DeckbuildObservation
import com.wingedsheep.gym.contract.LegalActionView
import com.wingedsheep.gym.contract.ObservationResult
import com.wingedsheep.gym.contract.PoolCardView
import com.wingedsheep.gym.contract.SchemaHash
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId

/**
 * A solitaire [GymEnv] in which one agent turns an opened sealed [pool] into a legal deck.
 *
 * The agent sees the whole pool (no hidden information) and drives the build through
 * enumerated legal actions — `ADD_CARD`, `REMOVE_CARD`, `ADD_BASIC`, `FINALIZE`. It commits
 * with `FINALIZE` once the deck reaches [targetSize]; the env then terminates and exposes the
 * finished list via [finalDeck] / [DeckbuildObservation.selected] plus an intrinsic
 * [DeckbuildObservation.deckScore]. The caller feeds that list to a game env (`DeckSpec.Explicit`)
 * to actually play it.
 *
 * This deliberately does **not** use the engine's `(GameState, GameAction)` model — there is no
 * game yet — which is exactly why deckbuilding is its own env type rather than a pre-game
 * pause inside a game env.
 *
 * @param pool the opened sealed pool (duplicates included; copies cap how many can be added)
 * @param basics one [CardDefinition] per basic-land name the agent may add without limit
 */
class DeckbuildEnvironment(
    pool: List<CardDefinition>,
    basics: List<CardDefinition>,
    val builderId: EntityId = EntityId.generate(),
    val targetSize: Int = 40,
    private val schemaHash: String = SchemaHash.CURRENT,
) : GymEnv {

    /** Available copies per pool card name. */
    private val poolCounts: Map<String, Int> = pool.groupingBy { it.name }.eachCount()

    /** Representative definition per name (pool + basics), for views and rating. */
    private val cardByName: Map<String, CardDefinition> =
        (pool + basics).associateBy { it.name }

    private val basicNames: List<String> = basics.map { it.name }.distinct().sorted()

    /** Current decklist, insertion-ordered for stable enumeration. */
    private val selected = LinkedHashMap<String, Int>()

    private var finalized = false

    /** Action list for the most recent observation; `step` indexes into it. */
    private var actions: List<DeckbuildAction> = emptyList()

    override val isTerminal: Boolean get() = finalized

    /** The finished decklist once finalized, else null. */
    val finalDeck: Map<String, Int>? get() = if (finalized) LinkedHashMap(selected) else null

    private fun selectedCount(): Int = selected.values.sum()

    private fun remainingOf(name: String): Int =
        (poolCounts[name] ?: 0) - (selected[name] ?: 0)

    // --- GymEnv --------------------------------------------------------------

    override fun observe(revealAll: Boolean?): ObservationResult {
        val acts = buildActions()
        actions = acts
        val obs = DeckbuildObservation(
            schemaHash = schemaHash,
            agentToAct = if (finalized) null else builderId,
            pool = poolCounts.keys.sorted().map { poolView(it, remainingOf(it)) },
            basics = basicNames.map { poolView(it, targetSize) },
            selected = LinkedHashMap(selected),
            selectedCount = selectedCount(),
            targetSize = targetSize,
            deckScore = if (finalized) scoreDeck() else null,
            legalActions = acts.mapIndexed { idx, a -> a.toView(idx) },
            terminated = finalized,
            stateDigest = digest()
        )
        return ObservationResult(obs, ActionRegistry.EMPTY)
    }

    override fun step(actionId: Int): ObservationResult {
        require(!finalized) { "Deck already finalized; env is terminal" }
        val action = actions.getOrNull(actionId)
            ?: throw IllegalArgumentException("Action ID $actionId is not valid for the current step")
        apply(action)
        return observe()
    }

    override fun fork(): GymEnv {
        // Rebuild an equivalent pool/basics from the immutable indices, then copy build state.
        val poolList = poolCounts.flatMap { (name, n) -> List(n) { cardByName.getValue(name) } }
        val basicsList = basicNames.map { cardByName.getValue(it) }
        val copy = DeckbuildEnvironment(poolList, basicsList, builderId, targetSize, schemaHash)
        copy.selected.putAll(selected)
        copy.finalized = finalized
        copy.observe()
        return copy
    }

    // --- actions -------------------------------------------------------------

    private fun buildActions(): List<DeckbuildAction> {
        if (finalized) return emptyList()
        val out = mutableListOf<DeckbuildAction>()
        poolCounts.keys.sorted().forEach { if (remainingOf(it) > 0) out += DeckbuildAction.AddCard(it) }
        basicNames.forEach { out += DeckbuildAction.AddBasic(it) }
        selected.keys.sorted().forEach { out += DeckbuildAction.RemoveCard(it) }
        if (selectedCount() >= targetSize) out += DeckbuildAction.Finalize
        return out
    }

    private fun apply(action: DeckbuildAction) {
        when (action) {
            is DeckbuildAction.AddCard -> {
                if (remainingOf(action.name) > 0) selected.merge(action.name, 1, Int::plus)
            }
            is DeckbuildAction.AddBasic -> selected.merge(action.name, 1, Int::plus)
            is DeckbuildAction.RemoveCard -> {
                val cur = selected[action.name] ?: return
                if (cur <= 1) selected.remove(action.name) else selected[action.name] = cur - 1
            }
            DeckbuildAction.Finalize -> {
                check(selectedCount() >= targetSize) { "Cannot finalize: ${selectedCount()} < $targetSize" }
                finalized = true
            }
        }
    }

    // --- views / scoring -----------------------------------------------------

    private fun poolView(name: String, remaining: Int): PoolCardView {
        val card = cardByName[name]
        return PoolCardView(
            name = name,
            manaCost = card?.manaCost?.toString() ?: "",
            manaValue = card?.cmc ?: 0,
            colors = card?.colors?.mapTo(mutableSetOf()) { it.name } ?: emptySet(),
            types = card?.typeLine?.cardTypes?.mapTo(mutableSetOf()) { it.name } ?: emptySet(),
            subtypes = card?.typeLine?.subtypes?.mapTo(mutableSetOf()) { it.value } ?: emptySet(),
            pt = card?.creatureStats?.let { "${it.power.description}/${it.toughness.description}" },
            oracleText = card?.oracleText ?: "",
            remaining = remaining
        )
    }

    /** Intrinsic quality: summed limited rating of the non-land cards in the deck. */
    private fun scoreDeck(): Double =
        selected.entries.sumOf { (name, count) ->
            val card = cardByName[name]
            if (card == null || card.isLand) 0.0 else LimitedCardRater.rate(card) * count
        }

    private fun digest(): String =
        buildString {
            append(if (finalized) "F|" else "B|")
            selected.toSortedMap().forEach { (n, c) -> append(n).append(':').append(c).append(';') }
        }
}

/** The moves available while building a deck. */
private sealed interface DeckbuildAction {
    fun toView(id: Int): LegalActionView

    data class AddCard(val name: String) : DeckbuildAction {
        override fun toView(id: Int) =
            LegalActionView(id, "ADD_CARD", "Add $name", affordable = true)
    }

    data class AddBasic(val name: String) : DeckbuildAction {
        override fun toView(id: Int) =
            LegalActionView(id, "ADD_BASIC", "Add basic $name", affordable = true)
    }

    data class RemoveCard(val name: String) : DeckbuildAction {
        override fun toView(id: Int) =
            LegalActionView(id, "REMOVE_CARD", "Remove $name", affordable = true)
    }

    object Finalize : DeckbuildAction {
        override fun toView(id: Int) =
            LegalActionView(id, "FINALIZE", "Finalize deck", affordable = true)
    }
}
