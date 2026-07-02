package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.ForetoldComponent
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.SourceForetoldOnPriorTurn
import kotlin.reflect.KClass

/**
 * Handler for the [ForetellCard] special action (CR 702.143, Kaldheim).
 *
 * Foretell is a special action — it does not use the stack and cannot be responded to
 * once announced. The player must have priority during their own turn (CR 116.2h). The
 * handler pays the fixed {2} foretell setup cost, exiles the card from hand *face down*
 * (CR 708), stamps a [ForetoldComponent] (for the same-turn restriction) + a
 * [FaceDownComponent] (opponent masking) + a [PlayWithFixedAlternativeManaCostComponent]
 * carrying the card's foretell cost, and adds a permanent [MayPlayPermission] gated by
 * [SourceForetoldOnPriorTurn] so the card can be cast from exile for its foretell cost on
 * any later turn.
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
    private val manaAbilitySideEffectExecutor: com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor,
) : ActionHandler<ForetellCard> {
    override val actionType: KClass<ForetellCard> = ForetellCard::class

    /** The fixed setup cost to foretell a card, per CR 702.143a. */
    private val setupCost: ManaCost = ManaCost.parse("{2}")

    private fun foretellCostOf(state: GameState, cardId: com.wingedsheep.sdk.model.EntityId): ManaCost? {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return null
        return cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Foretell>().firstOrNull()?.cost
    }

    override fun validate(state: GameState, action: ForetellCard): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }
        // CR 116.2h / 702.143a: any time you have priority during your own turn.
        if (!state.isActiveTurnFor(action.playerId)) {
            return "Foretell can only be activated during your own turn"
        }
        if (action.cardId !in state.getZone(ZoneKey(action.playerId, Zone.HAND))) {
            return "This card is not in your hand"
        }
        if (foretellCostOf(state, action.cardId) == null) {
            return "This card does not have foretell"
        }

        if (action.paymentStrategy is PaymentStrategy.Explicit) {
            for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                val sourceContainer = state.getEntity(sourceId)
                    ?: return "Mana source not found: $sourceId"
                if (sourceContainer.has<TappedComponent>()) {
                    return "Mana source is already tapped: $sourceId"
                }
            }
        } else if (!manaSolver.canPay(state, action.playerId, setupCost)) {
            return "Not enough mana to foretell this card"
        }
        return null
    }

    override fun execute(state: GameState, action: ForetellCard): ExecutionResult {
        val container = state.getEntity(action.cardId)
            ?: return ExecutionResult.error(state, "Card not found")
        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")
        val foretellCost = foretellCostOf(state, action.cardId)
            ?: return ExecutionResult.error(state, "This card does not have foretell")

        var currentState = state
        val events = mutableListOf<GameEvent>()
        val ownerId = cardComponent.ownerId ?: action.playerId

        // Pay the fixed {2} setup cost — drain mana pool first, then tap lands for the remainder.
        val poolComponent = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>()
            ?: ManaPoolComponent()
        val pool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless
        )
        val partialResult = pool.payPartial(setupCost)
        val poolAfterPayment = partialResult.newPool
        val remainingCost = partialResult.remainingCost
        val manaSpentFromPool = partialResult.manaSpent

        var whiteSpent = manaSpentFromPool.white
        var blueSpent = manaSpentFromPool.blue
        var blackSpent = manaSpentFromPool.black
        var redSpent = manaSpentFromPool.red
        var greenSpent = manaSpentFromPool.green
        var colorlessSpent = manaSpentFromPool.colorless

        currentState = currentState.updateEntity(action.playerId) { c ->
            c.with(
                ManaPoolComponent(
                    white = poolAfterPayment.white,
                    blue = poolAfterPayment.blue,
                    black = poolAfterPayment.black,
                    red = poolAfterPayment.red,
                    green = poolAfterPayment.green,
                    colorless = poolAfterPayment.colorless
                )
            )
        }

        if (!remainingCost.isEmpty()) {
            if (action.paymentStrategy is PaymentStrategy.Explicit) {
                for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                    val (tappedState, tapEvent) = tap(currentState, sourceId)
                    currentState = tappedState
                    tapEvent?.let(events::add)
                }
            } else {
                val solution = manaSolver.solve(currentState, action.playerId, remainingCost, 0)
                    ?: return ExecutionResult.error(state, "Not enough mana to foretell")
                val (stateAfterTaps, tapEvents) = manaAbilitySideEffectExecutor
                    .tapSourcesWithSideEffects(currentState, solution, action.playerId)
                currentState = stateAfterTaps
                events.addAll(tapEvents)

                for ((_, production) in solution.manaProduced) {
                    when (production.color) {
                        Color.WHITE -> whiteSpent++
                        Color.BLUE -> blueSpent++
                        Color.BLACK -> blackSpent++
                        Color.RED -> redSpent++
                        Color.GREEN -> greenSpent++
                        null -> colorlessSpent += production.colorless
                    }
                }
            }
        }

        events.add(
            ManaSpentEvent(
                playerId = action.playerId,
                reason = "Foretell ${cardComponent.name}",
                white = whiteSpent,
                blue = blueSpent,
                black = blackSpent,
                red = redSpent,
                green = greenSpent,
                colorless = colorlessSpent
            )
        )

        // Move the card from hand → owner's exile, face down (CR 708 — hidden from opponents,
        // visible to its owner). Hand is keyed under the acting player (its owner).
        val fromZoneKey = ZoneKey(action.playerId, Zone.HAND)
        val exileZone = ZoneKey(ownerId, Zone.EXILE)
        currentState = currentState.removeFromZone(fromZoneKey, action.cardId)
        currentState = currentState.addToZone(exileZone, action.cardId)
        events.add(
            ZoneChangeEvent(
                entityId = action.cardId,
                entityName = cardComponent.name,
                fromZone = Zone.HAND,
                toZone = Zone.EXILE,
                ownerId = ownerId
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
                        fixedCost = foretellCost
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

        // Foretell is a special action — it does not change priority and does not use the stack.
        return ExecutionResult.success(currentState, events)
    }

    companion object {
        fun create(services: EngineServices): ForetellCardHandler {
            return ForetellCardHandler(
                services.cardRegistry,
                services.manaSolver,
                services.manaAbilitySideEffectExecutor,
            )
        }
    }
}
