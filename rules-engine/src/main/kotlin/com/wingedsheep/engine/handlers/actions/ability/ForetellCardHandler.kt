package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.ForetoldEvent
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.ForetellGrants
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.SpecialActionCostReducer
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.ForetoldComponent
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.SpecialActionKind
import com.wingedsheep.sdk.scripting.conditions.SourceForetoldOnPriorTurn
import kotlin.reflect.KClass

/**
 * Handler for the [ForetellCard] special action (CR 702.143, Kaldheim).
 *
 * Foretell is a special action — it does not use the stack and cannot be responded to
 * once announced. The player must have priority during their own turn (CR 116.2h), or on any
 * turn with a `MayForetellOnAnyTurn` (Cosmos Charger). The handler pays the {2} foretell setup
 * cost (after `ModifySpecialActionCost(FORETELL)` statics), exiles the card from hand *face down*
 * (CR 708), stamps a [ForetoldComponent] (for the same-turn restriction) + a
 * [FaceDownComponent] (opponent masking) + a [PlayWithFixedAlternativeManaCostComponent]
 * carrying the card's foretell cost, and adds a permanent [MayPlayPermission] gated by
 * [SourceForetoldOnPriorTurn] so the card can be cast from exile for its foretell cost on
 * any later turn. It emits a [ForetoldEvent] for "whenever you foretell a card" triggers.
 *
 * Which cards can be foretold, and for what later cost, is [ForetellGrants]' call: a printed
 * foretell, or one a `GrantForetellToCardsInHand` (Dream Devourer) grants to cards in hand. The cost is
 * fixed on the card as it is foretold, so it outlives the grant (Dream Devourer rulings).
 *
 * The cast-later path reuses the same fixed-alternative-cost cast-from-exile machinery as
 * Airbend: [com.wingedsheep.engine.legalactions.enumerators.CastFromZoneEnumerator] and
 * [com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler] read the stamped fixed
 * cost when computing the spell's cost. On cast, [com.wingedsheep.engine.mechanics.stack.StackResolver]
 * strips the fixed-cost component and (on resolve) the may-play permission, and reveals the
 * card by stripping its [FaceDownComponent].
 *
 * This structurally mirrors [PlotCardHandler]; the differences are the fixed {2} setup cost
 * (vs. the plot ability's printed cost), the face-down exile, and the distinct foretell cost
 * paid on the later cast (vs. plot's free cast).
 */
class ForetellCardHandler(
    private val cardRegistry: CardRegistry,
    private val manaSolver: ManaSolver,
    private val predicateEvaluator: PredicateEvaluator,
    manaAbilitySideEffectExecutor: com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor,
) : ActionHandler<ForetellCard> {
    override val actionType: KClass<ForetellCard> = ForetellCard::class

    private val payment = SpecialActionManaPayment(manaSolver, manaAbilitySideEffectExecutor, FORETELL_SPEND_CONTEXT)

    private val specialActionCostReducer = SpecialActionCostReducer(cardRegistry)

    /** The printed setup cost to foretell a card, per CR 702.143a. */
    private val printedSetupCost: ManaCost = ManaCost.parse("{2}")

    private fun setupCost(state: GameState, playerId: com.wingedsheep.sdk.model.EntityId): ManaCost =
        specialActionCostReducer.effectiveCost(state, playerId, SpecialActionKind.FORETELL, printedSetupCost)

    private fun foretellCostOf(state: GameState, action: ForetellCard): ManaCost? {
        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return null
        return ForetellGrants.effectiveForetellCost(
            state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator
        )
    }

    private fun foretellFaceCosts(state: GameState, action: ForetellCard): Map<String, ManaCost> {
        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>() ?: return emptyMap()
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return emptyMap()
        return ForetellGrants.grantedFaceForetellCosts(
            state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator
        )
    }

    override fun validate(state: GameState, action: ForetellCard): String? {
        if (!state.hasPriority(action.playerId)) {
            return "You don't have priority"
        }
        // CR 116.2h / 702.143a: any time you have priority during your own turn (any turn with
        // Cosmos Charger).
        if (!ForetellGrants.mayForetellNow(state, action.playerId, cardRegistry)) {
            return "Foretell can only be activated during your own turn"
        }
        if (action.cardId !in state.getZone(ZoneKey(action.playerId, Zone.HAND))) {
            return "This card is not in your hand"
        }
        if (foretellCostOf(state, action) == null) {
            return "This card does not have foretell"
        }

        payment.validate(state, action.playerId, setupCost(state, action.playerId), action.paymentStrategy)?.let {
            return if (action.paymentStrategy is PaymentStrategy.Explicit) it else "Not enough mana to foretell this card"
        }
        return null
    }

    override fun execute(state: GameState, action: ForetellCard): ExecutionResult {
        val container = state.getEntity(action.cardId)
            ?: return ExecutionResult.error(state, "Card not found")
        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")
        val foretellCost = foretellCostOf(state, action)
            ?: return ExecutionResult.error(state, "This card does not have foretell")

        var currentState = state
        val events = mutableListOf<GameEvent>()
        val ownerId = cardComponent.ownerId ?: action.playerId

        // Pay the fixed {2} setup cost — drain mana pool first, then tap lands for the remainder.
        val (paidState, paymentEvents) = payment.pay(
            currentState, action.playerId, setupCost(state, action.playerId), action.paymentStrategy, "Foretell ${cardComponent.name}"
        ) ?: return ExecutionResult.error(state, "Not enough mana to foretell")
        currentState = paidState
        events.addAll(paymentEvents)

        // Move the card from hand → owner's exile, face down (CR 708 — hidden from opponents,
        // visible to its owner). Hand is keyed under the acting player (its owner).
        val fromZoneKey = ZoneKey(action.playerId, Zone.HAND)
        val exileZone = ZoneKey(ownerId, Zone.EXILE)
        currentState = currentState.removeFromZone(fromZoneKey, action.cardId)
        val oldObjectRef = currentState.objectRef(action.cardId)
        currentState = currentState.addToZone(exileZone, action.cardId)
        events.add(
            ZoneChangeEvent(
                entityId = action.cardId,
                entityName = cardComponent.name,
                fromZone = Zone.HAND,
                toZone = Zone.EXILE,
                ownerId = ownerId,
                oldObject = oldObjectRef,
                newObject = currentState.objectRef(action.cardId)
            )
        )

        // Stamp foretold-state components: the ForetoldComponent for the same-turn restriction,
        // FaceDownComponent for opponent masking, and the fixed foretell cost so the cast-from-exile
        // machinery lets the owner recast it for its foretell cost.
        currentState = currentState.updateEntity(action.cardId) { c ->
            c.with(ForetoldComponent(controllerId = action.playerId, turnForetold = currentState.turnNumber))
                .with(FaceDownComponent)
                .with(
                    PlayWithFixedAlternativeManaCostComponent(
                        controllerId = action.playerId,
                        fixedCost = foretellCost,
                        faceCosts = foretellFaceCosts(state, action),
                    )
                )
        }
        val (permId, stateWithPerm) = currentState.newEntity()
        currentState = stateWithPerm.addMayPlayPermission(
            MayPlayPermission(
                id = permId,
                cardIds = setOf(action.cardId),
                controllerId = action.playerId,
                sourceId = action.cardId,
                condition = SourceForetoldOnPriorTurn,
                permanent = true,
                timestamp = currentState.timestamp,
            )
        )

        currentState = currentState.tick()
        events.add(ForetoldEvent(playerId = action.playerId, cardId = action.cardId))

        // Foretell is a special action — it does not change priority and does not use the stack.
        return ExecutionResult.success(currentState, events)
    }

    companion object {
        fun create(services: EngineServices): ForetellCardHandler {
            return ForetellCardHandler(
                services.cardRegistry,
                services.manaSolver,
                services.predicateEvaluator,
                services.manaAbilitySideEffectExecutor,
            )
        }
    }
}

/**
 * The spend context of the foretell special action's {2}: what lets "spend this mana only to
 * foretell a card" (ManaRestriction.ForetellOnly) pay it. Shared with [ForetellEnumerator] so the
 * affordability preview and the payment agree.
 */
internal val FORETELL_SPEND_CONTEXT = com.wingedsheep.engine.mechanics.mana.SpellPaymentContext(isForetellAction = true)
