package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.AnswerContinuation
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.core.YesNoResponse
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersOnlyIfPaid
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost

/**
 * [EntersOnlyIfPaid] — "if this would enter, you may [cost] instead; if you don't, put it into its
 * owner's graveyard" — asked *before* the card moves, on every entry path that can meet it:
 *
 *  - a permanent spell resolving ([com.wingedsheep.engine.mechanics.stack.PermanentSpellResolver]),
 *    asked once any entry copy is settled;
 *  - an effect putting the card onto the battlefield ([EffectEntryPayments]): single and collection
 *    moves, put-as-non-Aura, return-same-named, return-noted-exile and put-attached-to-chosen;
 *  - a duration return ("until this leaves the battlefield", [ZoneReturnService]), which can't pause
 *    inside a zone transition and so queues the return as a replacement rider that runs the
 *    single-move effect path at the next boundary.
 *
 * The cost is the shared [PayCost] vocabulary, priced and paid by [CostPaymentService] — only the
 * question is built here, because its wording has to name what declining costs ("…it goes to the
 * graveyard"), which a pay-or-suffer prompt can't say about a card that isn't on the battlefield.
 * A cost with objects to pick (discard a land card) is a card selection where picking nothing
 * declines; any other cost is a yes/no.
 *
 * [ZoneTransitionService.moveToZone] holds the other half: a card carrying the replacement that
 * reaches the battlefield without [ZoneEntryOptions.paidToEnter] goes to its owner's graveyard
 * instead. **Open gaps**, where that fallback is reached unasked: a land being *played*
 * (`PlayLandHandler` bypasses the zone transition), a Leyline-style opening-hand start, and a meld or
 * "return it transformed" entry. No printed card with this replacement can reach them today.
 */
object EntryPayment {

    /** The cost [cardDef]'s own [EntersOnlyIfPaid] asks for, or null when it has none. */
    fun costOf(cardDef: CardDefinition?): PayCost? =
        cardDef?.script?.replacementEffects?.filterIsInstance<EntersOnlyIfPaid>()?.firstOrNull()?.cost

    /**
     * The cost [entityId] asks for as it would exist on the battlefield (CR 614.12): the copied card's
     * when it enters as a copy — a granted entry copy (Mystic Reflection) or [entryCopy] — else its own.
     */
    fun enteringCost(
        state: GameState,
        entityId: EntityId,
        entryCopy: com.wingedsheep.engine.handlers.effects.copy.EntryCopyChoice?,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): PayCost? {
        val choice = com.wingedsheep.engine.handlers.effects.copy.GrantedEntryCopy
            .find(state, entityId, predicateEvaluator)?.chosen(entryCopy) ?: entryCopy
        val card = choice?.takeIf { it.copiedCard != null }
            ?.let { com.wingedsheep.engine.handlers.effects.copy.EffectCopyEntry.apply(state, entityId, it) }
            ?.getEntity(entityId)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
            ?: state.getEntity(entityId)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
            ?: return null
        return costOf(cardRegistry.getCard(card.cardDefinitionId))
    }

    /** Whether [payerId] can pay [cost] for [sourceId] right now (CR 118.3). */
    fun canPay(
        state: GameState,
        payerId: EntityId,
        cost: PayCost,
        sourceId: EntityId,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): Boolean = CostPaymentService.canAfford(
        state, payerId, cost, sourceId, ManaSolver(cardRegistry, predicateEvaluator), predicateEvaluator
    )

    /**
     * Pause to ask [payerId] whether to pay [cost] so [cardName] enters. Only call once [canPay] holds.
     * The [answer] frame's resumer reads the response with [settle].
     */
    fun ask(
        state: GameState,
        payerId: EntityId,
        sourceId: EntityId,
        cardName: String,
        cost: PayCost,
        predicateEvaluator: PredicateEvaluator,
        answer: AnswerContinuation,
        events: List<GameEvent> = emptyList(),
    ): ExecutionResult {
        val clause = cost.description.replaceFirstChar { it.uppercase() }
        val prompt = "$clause to put $cardName onto the battlefield? If you don't, it goes to the graveyard."
        val context = DecisionContext(sourceId = sourceId, sourceName = cardName, phase = DecisionPhase.RESOLUTION)
        val candidates = selectionCandidates(state, payerId, cost, sourceId, predicateEvaluator)
        return if (candidates != null) {
            state.suspendForDecision(
                question = { id ->
                    SelectCardsDecision(
                        id = id,
                        playerId = payerId,
                        prompt = prompt,
                        context = context,
                        options = candidates,
                        minSelections = 0,
                        maxSelections = costPaymentCount(cost),
                        useTargetingUI = candidates.any { it in state.getBattlefield() },
                    )
                },
                answer = answer,
                events = events,
            )
        } else {
            state.suspendForDecision(
                question = { id ->
                    YesNoDecision(
                        id = id,
                        playerId = payerId,
                        prompt = prompt,
                        context = context,
                        yesText = clause,
                        noText = "Put it into the graveyard",
                    )
                },
                answer = answer,
                events = events,
            )
        }
    }

    /** The outcome of [settle]: whether the cost was paid, and the state and events after paying it. */
    data class Settled(val paid: Boolean, val state: GameState, val events: List<GameEvent>)

    /**
     * Read [response] to the question [ask] built and pay the cost when the player chose to. A
     * selection short of the required count, or a payment that fails, is a decline. Null when the
     * response isn't the shape the question asked for.
     */
    fun settle(
        state: GameState,
        response: DecisionResponse,
        payerId: EntityId,
        sourceId: EntityId,
        cost: PayCost,
        costPayment: CostPaymentService,
    ): Settled? {
        val selected: Map<EntityId, Int> = when (response) {
            is CardsSelectedResponse -> {
                if (response.selectedCards.size < costPaymentCount(cost)) {
                    return Settled(false, state, emptyList())
                }
                response.selectedCards.groupingBy { it }.eachCount()
            }
            is YesNoResponse -> if (response.choice) emptyMap() else return Settled(false, state, emptyList())
            else -> return null
        }
        val resolved = CostPaymentService.resolve(state, cost, sourceId)
        val execution = costPayment.performPayment(state, payerId, resolved, sourceId, selected)
        return if (execution.success) Settled(true, execution.state, execution.events)
        else Settled(false, state, emptyList())
    }

    /**
     * The objects the player picks to pay [cost], or null for a cost paid with a yes/no — a random
     * discard has a domain but nothing to pick.
     */
    private fun selectionCandidates(
        state: GameState,
        payerId: EntityId,
        cost: PayCost,
        sourceId: EntityId,
        predicateEvaluator: PredicateEvaluator,
    ): List<EntityId>? {
        val atom = (cost as? PayCost.Atom)?.atom
        if (atom is CostAtom.Discard && atom.random) return null
        return CostPaymentService.selectionCandidates(state, payerId, cost, sourceId, predicateEvaluator)
    }
}

/** How many objects [cost] has the player pick — the [CostPaymentService.requiredCount] rule. */
private fun costPaymentCount(cost: PayCost): Int = (cost as? PayCost.Atom)?.atom?.selectionCount ?: 0

/**
 * [EntryPayment] for cards an *effect* puts onto the battlefield — a reanimation, "put a card from
 * your hand onto the battlefield", a library search. The same prepare-then-replay shape as
 * [EffectEntryChoices]: each entrant's controller is asked before anything moves, in APNAP order,
 * the answer is recorded on the [com.wingedsheep.engine.handlers.EffectContext]
 * ([EntryChoiceAnswers.paidToEnter]) and the move instruction is replayed; the zone transition then
 * reads it ([ZoneEntryOptions.paidToEnter]) and sends an unpaid entrant to the graveyard.
 */
object EffectEntryPayments {
    fun prepare(
        state: GameState,
        effect: com.wingedsheep.sdk.scripting.effects.Effect,
        context: com.wingedsheep.engine.handlers.EffectContext,
        entrants: Map<EntityId, EntityId>,
        registry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
    ): com.wingedsheep.engine.core.EffectResult? {
        val playerOrder = state.apnapOrder.withIndex().associate { it.value to it.index }
        var answered = context
        for ((id, controller) in entrants.entries.sortedBy { playerOrder[it.value] ?: Int.MAX_VALUE }) {
            if (id in state.getBattlefield()) continue
            if (answered.entryChoices[id]?.paidToEnter != null) continue
            val card = state.getEntity(id)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>() ?: continue
            val cost = EntryPayment.enteringCost(state, id, answered.entryCopies[id], registry, predicateEvaluator)
                ?: continue
            if (!EntryPayment.canPay(state, controller, cost, id, registry, predicateEvaluator)) {
                answered = recorded(answered, id, paid = false)
                continue
            }
            return com.wingedsheep.engine.core.EffectResult.from(
                EntryPayment.ask(
                    state, controller, id, card.name, cost, predicateEvaluator,
                    com.wingedsheep.engine.core.EffectEntryPaymentContinuation(effect, answered, id, controller, cost),
                )
            )
        }
        // Every entrant that couldn't pay is recorded so the replay doesn't price it again; when
        // nothing needs asking, the caller carries on with its own context (an unpaid entrant needs
        // no record to go to the graveyard).
        return null
    }

    /** [context] with [entityId]'s payment answer recorded. */
    fun recorded(
        context: com.wingedsheep.engine.handlers.EffectContext,
        entityId: EntityId,
        paid: Boolean,
    ): com.wingedsheep.engine.handlers.EffectContext {
        val prior = context.entryChoices[entityId] ?: EntryChoiceAnswers()
        return context.copy(entryChoices = context.entryChoices + (entityId to prior.copy(paidToEnter = paid)))
    }

    /** Whether [entityId]'s cost was paid before this move. */
    fun paid(context: com.wingedsheep.engine.handlers.EffectContext?, entityId: EntityId): Boolean =
        context?.entryChoices?.get(entityId)?.paidToEnter == true
}
