package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.ai.engine.knowledge.EffectWalker
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.DistributedCounterRemoval
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.values.ContextPropertyKey
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Paying a plain selection cost — tap, sacrifice, discard, bounce, reveal N of something — the way
 * the AI would: with the candidates worth least to the payer.
 *
 * The engine never picks these for the activator; an action that names nothing is refused. The
 * [Strategist] fills them for its own plays, and the opponent models that play an opponent's
 * abilities out ([CombatResponses], [CrackBackStress]) fill them the same way — without it,
 * "Tap two untapped artifacts you control: put two +1/+1 counters on this creature" was offered,
 * refused, and never seen as the defender's answer.
 */
internal object SelectionCostPayment {

    /**
     * [gameAction] with [offer]'s selection costs paid, when every one of them is a plain
     * selection; unchanged otherwise, or when [offer] carries none. For callers with no
     * cost-specific pickers of their own.
     */
    fun withPlainCosts(
        state: GameState,
        offer: LegalAction,
        gameAction: GameAction,
        intents: IntentCatalog = IntentCatalog.NONE,
    ): GameAction {
        val info = offer.additionalCostInfo ?: return gameAction
        val (existing, payerId) = when (gameAction) {
            is CastSpell -> (gameAction.additionalCostPayment ?: AdditionalCostPayment()) to gameAction.playerId
            is ActivateAbility -> (gameAction.costPayment ?: AdditionalCostPayment()) to gameAction.playerId
            else -> return gameAction
        }
        val order = CandidateOrder(state, payerId, intents, discardsByManaValue(state, gameAction, intents))
        val xValue = (gameAction as? CastSpell)?.xValue ?: 0
        val primary = plain(info, existing, order, xValue) ?: return gameAction
        val payment = info.alsoRequired.fold(primary) { acc, extra -> plain(extra, acc, order, xValue) ?: acc }
        return when (gameAction) {
            is CastSpell -> gameAction.copy(additionalCostPayment = payment)
            is ActivateAbility -> gameAction.copy(costPayment = payment)
            else -> gameAction
        }
    }

    /**
     * The payment for a plain selection cost, merged into [existing]; null for a cost kind that
     * needs more than "take the N cheapest".
     *
     * Costs that give something up — sacrifice, discard, bounce, tap — are paid with the candidates
     * worth least to the payer, via [CandidateOrder]. They used to take the first N legal
     * candidates in enumeration order, so Tar Pitcher ("{T}, Sacrifice a Goblin") sacrificed a 2/3
     * flyer to ping a recursive 1/1 when it could have sacrificed itself (2026-10-12 logs, 2p game
     * 1 T18). The source is ranked like any other candidate when the enumerator lists it. Because
     * this runs before the action is simulated, the activation's own worth is then judged against
     * the cheapest price rather than an arbitrary one.
     */
    fun plain(
        info: AdditionalCostData,
        existing: AdditionalCostPayment,
        cheapestFirst: CandidateOrder,
        xValue: Int,
    ): AdditionalCostPayment? = selection(info, existing, cheapestFirst, xValue)
        ?.let { withFixedCounterRemovals(info, it, cheapestFirst) }

    /**
     * "Remove three counters from among creatures you control" (Lieutenant Commander Data): the
     * fixed total, riding on whichever payload carries it. -1/-1 counters go first — shedding one
     * is a gain — then the rest come off the least valuable permanents first, so the counters
     * that are given up are the cheapest ones. An X total is left to the caller that chose X.
     */
    private fun withFixedCounterRemovals(
        info: AdditionalCostData,
        payment: AdditionalCostPayment,
        cheapestFirst: CandidateOrder,
    ): AdditionalCostPayment {
        val total = info.distributedCounterRemovalTotal
        if (total <= 0 || payment.distributedCounterRemovals.isNotEmpty()) return payment
        val byId = info.counterRemovalCreatures.associateBy { it.entityId }
        val order = cheapestFirst.permanents(byId.keys.toList())
        val pool = order.flatMap { id ->
            byId.getValue(id).availableCountersByType.entries.map { (type, count) -> Triple(id, type, count) }
        }.sortedBy { (_, type, _) -> if (type == CounterType.MINUS_ONE_MINUS_ONE.printed) 0 else 1 }
        var remaining = total
        val removals = mutableListOf<DistributedCounterRemoval>()
        for ((id, type, available) in pool) {
            if (remaining == 0) break
            val take = minOf(remaining, available)
            removals += DistributedCounterRemoval(id, type, take)
            remaining -= take
        }
        return if (remaining == 0) payment.copy(distributedCounterRemovals = removals) else payment
    }

    private fun selection(
        info: AdditionalCostData,
        existing: AdditionalCostPayment,
        cheapestFirst: CandidateOrder,
        xValue: Int,
    ): AdditionalCostPayment? = when (info.costType) {
        "RemoveCounters" -> existing
        "Blight" -> existing.copy(blightTargets = info.validBlightTargets.take(1))
        // "Put a -1/-1 counter on a creature you control" (Lethal Sting): the cheapest creature takes it.
        "PutCounters" -> existing.copy(
            counterPlacementTargets = cheapestFirst.permanents(info.validCounterPlacementTargets).take(1)
        )
        "Behold" -> existing.copy(beheldCards = info.validBeholdTargets.take(info.beholdCount))
        // "Reveal an Elemental card from your hand" (Flamekin Bladewhirl): the cards stay in hand.
        "RevealCard" -> existing.copy(revealedCards = info.validRevealTargets.take(info.revealCount))
        "TapPermanents" -> existing.copy(
            tappedPermanents = cheapestFirst.permanents(info.validTapTargets).take(info.tapCount)
        )
        // "Discard X cards" (Nahiri's Wrath): exactly the cast's announced X.
        "DiscardCard" -> existing.copy(
            discardedCards = cheapestFirst.cardsInHand(info.validDiscardTargets)
                .take(if (info.countIsX) xValue else info.discardCount)
        )
        "SacrificePermanent" -> existing.copy(
            sacrificedPermanents = cheapestFirst.permanents(info.validSacrificeTargets).take(info.sacrificeCount)
        )
        "BouncePermanent" -> existing.copy(
            bouncedPermanents = cheapestFirst.permanents(info.validBounceTargets).take(info.bounceCount)
        )
        "ExileFromGraveyard" -> existing.copy(exiledCards = info.validExileTargets.take(info.exileMinCount))
        // "Sacrifice any number of …": only the floor, so a zero-floor cost sacrifices nothing.
        // "Sacrifice X creatures": exactly the cast's announced X.
        "SacrificeVariable" -> existing.copy(
            variableCostPermanents = cheapestFirst.permanents(info.validSacrificeTargets)
                .take(if (info.countIsX) xValue else info.sacrificeCount)
        )
        else -> null
    }

    /**
     * Whether [gameAction] casts a spell whose damage is "the total mana value of the discarded
     * cards" (Nahiri's Wrath) — then its discard cost is the fuel, and the cards worth the most
     * mana value are the ones to discard, not the ones worth least in hand.
     */
    fun discardsByManaValue(state: GameState, gameAction: GameAction, intents: IntentCatalog): Boolean {
        val cast = gameAction as? CastSpell ?: return false
        val name = state.getEntity(cast.cardId)?.get<CardComponent>()?.name ?: return false
        val effect = intents.definition(name)?.script?.spellEffect ?: return false
        return dealsDiscardedManaValue(effect)
    }

    private fun dealsDiscardedManaValue(effect: Effect): Boolean = EffectWalker.leaves(effect).any { leaf ->
        when (leaf) {
            is ForEachEffect -> dealsDiscardedManaValue(leaf.body)
            is DealDamageEffect -> readsDiscardedManaValue(leaf.amount)
            else -> false
        }
    }

    private fun readsDiscardedManaValue(amount: DynamicAmount): Boolean = when (amount) {
        is DynamicAmount.ContextProperty -> amount.key == ContextPropertyKey.DISCARDED_AS_COST_TOTAL_MANA_VALUE
        is DynamicAmount.Add -> readsDiscardedManaValue(amount.left) || readsDiscardedManaValue(amount.right)
        is DynamicAmount.Multiply -> readsDiscardedManaValue(amount.amount)
        else -> false
    }

    /**
     * Orders cost candidates least valuable first: permanents by [BoardPresence.permanentValue]
     * (the same price the craft-material pick and the sacrifice decision in [DecisionResponder]
     * use), cards in hand by [CardSelectionValue] (the discard decision's price). Stable, so equal
     * candidates keep the enumerator's order. The hand pricer is built lazily — most actions carry
     * no discard cost. With [discardsByManaValue] the hand goes highest mana value first instead,
     * least valuable among equals ([SelectionCostPayment.discardsByManaValue]).
     */
    class CandidateOrder(
        private val state: GameState,
        private val payerId: EntityId?,
        private val intents: IntentCatalog,
        private val discardsByManaValue: Boolean = false,
    ) {
        private val handValue by lazy { payerId?.let { CardSelectionValue.of(state, it, intents) } }

        fun permanents(candidates: List<EntityId>): List<EntityId> {
            if (candidates.size < 2) return candidates
            val projected = state.projectedState
            return candidates.sortedBy { id ->
                val card = state.getEntity(id)?.get<CardComponent>() ?: return@sortedBy 0.0
                BoardPresence.permanentValue(state, projected, id, card, intents)
            }
        }

        fun cardsInHand(candidates: List<EntityId>): List<EntityId> {
            if (candidates.size < 2) return candidates
            val value = handValue
            val manaValue = { id: EntityId -> state.getEntity(id)?.get<CardComponent>()?.manaValue ?: 0 }
            return when {
                discardsByManaValue && value != null ->
                    candidates.sortedWith(compareByDescending(manaValue).thenBy { value.score(it) })
                discardsByManaValue -> candidates.sortedByDescending(manaValue)
                value != null -> candidates.sortedBy { value.score(it) }
                else -> candidates
            }
        }
    }
}
