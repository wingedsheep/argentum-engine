package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.hidden.OpponentKnowledge
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import java.util.concurrent.ConcurrentHashMap

/**
 * "Is a better creature still coming?" — the half of [RemovalPatience]'s bet the board cannot see.
 *
 * [RemovalPatience] holds a removal spell back from a creature worth less than the spell's own mana
 * value. That bar is blind to *what the opponent is playing*: a 2/3 mana creature clears a two-mana
 * removal's bar whether the deck behind it holds nothing bigger or Ruric Thar, two Titanbones and a
 * Hexhaven Invigorator. In live games the AI knows the opponent's registered decklist (a deliberate
 * design decision — see [OpponentKnowledge]), so it can tell those apart.
 *
 * The reading, in the evaluator's own board-value currency:
 *
 *  1. **Threats** are the creature cards of their list not yet seen this game
 *     ([OpponentKnowledge.unseenCards]) whose printed body is worth clearly more than the current
 *     target ([BoardPresence.creatureBodyValue], the reading `BoardPresence.spellValue` gives a
 *     creature spell), and that this removal could actually answer — a damage or -N/-N spell only
 *     counts threats within its [CardIntent.removalReach].
 *  2. **Scarcity.** One-card answers in our hand (this one included) are set against those threats:
 *     with more answers than threats the hold buys nothing and the reading is silent; one answer
 *     against many threats holds hardest.
 *  3. **Probability.** A threat matters only if they can cast it soon. The bar is the expected
 *     excess of the *best* threat among their hand plus their next [DRAW_HORIZON] draws, out of
 *     every unseen card (hypergeometric) — the creature this card would get to answer instead. A
 *     thinned list — threats already seen, a big library — prices the hold down on its own, so it
 *     never freezes the removal for good.
 *  4. **Not while it hurts.** A target that would kill us on its own within
 *     [HURTING_WITHIN_ATTACKS] attacks is answered now ([RemovalPatience] skips this bar for it).
 *
 * The result is a *shortfall* in board value that [RemovalPatience] takes alongside its own
 * fair-trade shortfall, so every release in [Patience.factorFor] — we are dying, the hand is full,
 * the game has moved on — still applies, and the evaluator can outvote it. Capped at
 * [CAP_OF_FAIR_TRADE] times the removal's own bar.
 */
class UnseenThreats(
    private val knowledge: OpponentKnowledge,
    private val intents: IntentCatalog,
) {
    /** Printed body value of a creature card by name; null when it is not a fixed-stat creature. */
    private val bodyValues = ConcurrentHashMap<String, BodyReading>()

    private data class BodyReading(val value: Double?, val toughness: Int?)

    private fun body(name: String): BodyReading = bodyValues.getOrPut(name) {
        val definition = intents.definition(name)
        val stats = definition?.creatureStats
        val power = stats?.basePower
        val toughness = stats?.baseToughness
        if (definition == null || !definition.typeLine.isCreature || power == null || toughness == null) {
            BodyReading(null, null)
        } else {
            BodyReading(
                BoardPresence.creatureBodyValue(power, toughness, (definition.keywords.map { it.name } + definition.flags.map { it.name }).toHashSet()),
                toughness,
            )
        }
    }

    /**
     * How far short of the creatures still in [opponentId]'s deck a target worth [worth] falls, in
     * board value: the expected excess of the **best** threat this card would get to answer instead
     * — over their hand plus their next [DRAW_HORIZON] draws — scaled by how few answers we hold.
     * `0.0` without a decklist, or when nothing clearly better is left.
     */
    fun shortfall(
        state: GameState,
        playerId: EntityId,
        opponentId: EntityId,
        worth: Double,
        removal: CardComponent,
        intent: CardIntent,
    ): Double {
        val unseen = knowledge.unseenCards(state, playerId, opponentId) ?: return 0.0
        val pool = unseen.values.sum()
        if (pool <= 0) return 0.0

        val reach = intent.removalReach
        // One entry per copy, best first.
        val excesses = buildList {
            for ((name, copies) in unseen) {
                val body = body(name)
                val value = body.value ?: continue
                if (value < worth + CLEARLY_BETTER) continue
                if (reach != null && (body.toughness ?: Int.MAX_VALUE) > reach) continue
                repeat(copies) { add(value - worth) }
            }
        }.sortedDescending()
        if (excesses.isEmpty()) return 0.0

        val answers = answersInHand(state, playerId).coerceAtLeast(1)
        if (answers > excesses.size) return 0.0
        val scarcity = (excesses.size - answers + 1).toDouble() / excesses.size

        // E[excess of the best threat they can reach]: the i-th best is the best one reached when
        // the i-1 above it are all missed and it is not.
        val accessible = (state.getHand(opponentId).size + DRAW_HORIZON).coerceAtMost(pool)
        var expected = 0.0
        var noneAbove = 1.0
        for ((i, excess) in excesses.withIndex()) {
            val noneThrough = noneDrawn(pool, i + 1, accessible)
            expected += excess * (noneAbove - noneThrough)
            noneAbove = noneThrough
        }

        val fairTrade = Patience.FAIR_TRADE_VALUE_PER_MANA * removal.manaValue.coerceAtLeast(1)
        return (scarcity * expected).coerceAtMost(CAP_OF_FAIR_TRADE * fairTrade)
    }

    /** One-card answers in [playerId]'s hand — the spells [RemovalPatience] judges. */
    private fun answersInHand(state: GameState, playerId: EntityId): Int =
        state.getHand(playerId).count { id ->
            val card = state.getEntity(id)?.get<CardComponent>() ?: return@count false
            if (card.isCreature) return@count false
            val intent = intents.forName(card.name) ?: return@count false
            RemovalPatience.isOneCardAnswer(intent)
        }

    companion object {
        /** A threat must be worth this much more board value than the target — about a 2/2's worth. */
        const val CLEARLY_BETTER = 2.0

        /** Their draws counted as "soon": about as many turns as a held answer waits. */
        const val DRAW_HORIZON = 4

        /** The unseen-threat shortfall never exceeds this multiple of the removal's own fair-trade bar. */
        const val CAP_OF_FAIR_TRADE = 3.0

        /**
         * A target that kills us on its own within this many attacks is hurting us *now*, and is
         * never held for a better one still in their deck.
         */
        const val HURTING_WITHIN_ATTACKS = 4

        /** Hypergeometric: the chance that none of [hits] marked cards is among [draws] from [pool]. */
        internal fun noneDrawn(pool: Int, hits: Int, draws: Int): Double {
            var p = 1.0
            for (i in 0 until draws) {
                val misses = pool - hits - i
                if (misses <= 0) return 0.0
                p *= misses.toDouble() / (pool - i)
            }
            return p
        }
    }
}
