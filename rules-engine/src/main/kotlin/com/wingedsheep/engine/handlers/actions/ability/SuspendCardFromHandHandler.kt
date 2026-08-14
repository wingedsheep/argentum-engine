package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SuspendCardFromHand
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SuspendedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import kotlin.reflect.KClass

/**
 * Handler for the [SuspendCardFromHand] special action (CR 702.62 / 116.2f, Time Spiral).
 *
 * Suspend is a special action: it does not use the stack and cannot be responded to once
 * announced. Unlike Flashback/Warp/Mayhem/Plot/Foretell (all alternative costs or setup fees
 * for a *cast*), the card is never put on the stack at all — CR 702.62a is explicit: "Suspend
 * N—[cost]" means "If you could begin to cast this card by putting it onto the stack from your
 * hand, you may pay [cost] and exile it with N time counters on it. This action doesn't use the
 * stack." The legality gate is still cast-shaped though (CR 702.62c): the player must be able to
 * *begin* to cast the card by putting it on the stack right now — same timing window as a normal
 * cast (instant speed for an instant or a card with flash, sorcery speed otherwise) — even for a
 * card like Ancestral Vision that has no mana cost and so can never actually be cast.
 *
 * The handler pays the card's printed suspend cost, moves it from hand to the owner's exile,
 * puts N time counters on it, and stamps [SuspendedComponent] + a dormant haste floating
 * effect exactly like [com.wingedsheep.engine.handlers.effects.library.GrantSuspendExecutor]
 * (the runtime-grant path used by cards like Taigam, Master Opportunist) — so a printed and a
 * granted suspend share the same exile-side lifecycle from here on: the engine's synthesized
 * [com.wingedsheep.sdk.scripting.Suspend.countdownAbility] removes a counter each upkeep and,
 * when the last is removed, plays the card for free.
 */
class SuspendCardFromHandHandler(
    private val cardRegistry: CardRegistry,
    private val manaSolver: ManaSolver,
    private val manaAbilitySideEffectExecutor: ManaAbilitySideEffectExecutor,
    private val turnManager: TurnManager,
    private val castPermissionUtils: com.wingedsheep.engine.legalactions.utils.CastPermissionUtils,
) : ActionHandler<SuspendCardFromHand> {
    override val actionType: KClass<SuspendCardFromHand> = SuspendCardFromHand::class

    private fun suspendAbilityOf(state: GameState, cardId: EntityId): KeywordAbility.Suspend? {
        val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return null
        return cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Suspend>().firstOrNull()
    }

    override fun validate(state: GameState, action: SuspendCardFromHand): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }
        if (action.cardId !in state.getZone(ZoneKey(action.playerId, Zone.HAND))) {
            return "This card is not in your hand"
        }
        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>()
            ?: return "Card not found"
        val suspend = suspendAbilityOf(state, action.cardId)
            ?: return "This card does not have suspend"

        // CR 702.62c/116.2f: only when the player could begin to cast this card by putting it
        // on the stack right now — the same timing window as a normal cast, independent of
        // whether the card's own mana cost could ever actually be paid. Printed flash is read
        // off the CardDefinition, not projected state: projection is only ever built for
        // battlefield entities (StateProjector.project iterates state.getBattlefield()), so
        // hasKeyword() on a hand-zone card silently returns false regardless of what's printed.
        // A battlefield-granted flash (GrantFlashToSpellType, e.g. Quick Sliver) counts too — the
        // permission side of "could begin to cast" is exactly as real as printed flash.
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        val hasFlash = cardDef?.keywords?.contains(Keyword.FLASH) == true ||
            castPermissionUtils.hasGrantedFlash(state, action.cardId)
        val isInstantSpeed = cardComponent.typeLine.isInstant || hasFlash
        if (!isInstantSpeed && !turnManager.canPlaySorcerySpeed(state, action.playerId)) {
            return "This card can only be suspended at a time you could cast it"
        }

        // CR 702.62c: "take into consideration any effects that would prohibit that card from
        // being cast" — a Silence-style CantCastSpellsComponent, a per-turn spell cast limit, Mana
        // Maze's color-sharing lock, etc. all block suspend exactly as they'd block a real cast,
        // even though suspend never actually puts the card on the stack.
        castPermissionUtils.reasonCannotCast(state, action.playerId, action.cardId)?.let { return it }

        if (action.paymentStrategy is PaymentStrategy.Explicit) {
            val chosenSources = action.paymentStrategy.manaAbilitiesToActivate
            val availableSourceIds = manaSolver.findAvailableManaSources(state, action.playerId)
                .mapTo(mutableSetOf()) { it.entityId }
            for (sourceId in chosenSources) {
                val sourceContainer = state.getEntity(sourceId)
                    ?: return "Mana source not found: $sourceId"
                if (sourceContainer.has<TappedComponent>()) {
                    return "Mana source is already tapped: $sourceId"
                }
                if (sourceId !in availableSourceIds) {
                    return "Invalid mana source: $sourceId"
                }
            }
            // Mirror CastSpellHandler's Explicit-payment validation: pay from the floating pool
            // first, then confirm the *chosen* sources (and only those) can cover what's left —
            // otherwise naming an off-color source (e.g. a Forest for a {R} cost) would silently
            // validate without ever actually paying the cost.
            val poolComponent = state.getEntity(action.playerId)?.get<ManaPoolComponent>()
                ?: ManaPoolComponent()
            val pool = ManaPool(
                white = poolComponent.white,
                blue = poolComponent.blue,
                black = poolComponent.black,
                red = poolComponent.red,
                green = poolComponent.green,
                colorless = poolComponent.colorless
            )
            val remainingCost = pool.payPartial(suspend.cost).remainingCost
            if (!remainingCost.isEmpty()) {
                val chosenSet = chosenSources.toSet()
                val excluded = availableSourceIds
                    .filter { it !in chosenSet }
                    .toSet()
                if (manaSolver.solve(state, action.playerId, remainingCost, excludeSources = excluded) == null) {
                    return "Selected mana sources cannot pay this card's suspend cost"
                }
            }
        } else if (!manaSolver.canPay(state, action.playerId, suspend.cost)) {
            return "Not enough mana to suspend this card"
        }
        return null
    }

    override fun execute(state: GameState, action: SuspendCardFromHand): ExecutionResult {
        val container = state.getEntity(action.cardId)
            ?: return ExecutionResult.error(state, "Card not found")
        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")
        val suspend = suspendAbilityOf(state, action.cardId)
            ?: return ExecutionResult.error(state, "This card does not have suspend")

        var currentState = state
        val events = mutableListOf<GameEvent>()
        val ownerId = cardComponent.ownerId ?: action.playerId

        // Pay the suspend cost — drain mana pool first, then tap lands for the remainder.
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
        val partialResult = pool.payPartial(suspend.cost)
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
            // Explicit payment restricts the solver to only the named sources — mirrors
            // CastPaymentProcessor.explicitPay — so a chosen source that can't actually produce
            // what's needed fails here rather than being blindly tapped for nothing (validate()
            // already proved a solution exists, but the solve must run again here to know which
            // colors were actually produced, for the ManaSpentEvent below).
            val excludeSources = if (action.paymentStrategy is PaymentStrategy.Explicit) {
                val chosenSet = action.paymentStrategy.manaAbilitiesToActivate.toSet()
                manaSolver.findAvailableManaSources(currentState, action.playerId)
                    .map { it.entityId }
                    .filter { it !in chosenSet }
                    .toSet()
            } else emptySet()

            val solution = manaSolver.solve(currentState, action.playerId, remainingCost, 0, excludeSources = excludeSources)
                ?: return ExecutionResult.error(state, "Not enough mana to suspend")
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

        events.add(
            ManaSpentEvent(
                playerId = action.playerId,
                reason = "Suspend ${cardComponent.name}",
                white = whiteSpent,
                blue = blueSpent,
                black = blackSpent,
                red = redSpent,
                green = greenSpent,
                colorless = colorlessSpent
            )
        )

        // Move the card from hand → owner's exile (face up — a suspended card is public).
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

        // Put N time counters on it (CR 702.62a). Never the "first counter this turn" for a
        // Stalwart Successor-style trigger — the card isn't a creature while sitting in exile.
        val currentCounters = currentState.getEntity(action.cardId)?.get<CountersComponent>() ?: CountersComponent()
        currentState = currentState.updateEntity(action.cardId) { c ->
            c.with(currentCounters.withAdded(CounterType.TIME, suspend.timeCounters))
        }
        events.add(
            CountersAddedEvent(
                entityId = action.cardId,
                counterType = Counters.TIME,
                amount = suspend.timeCounters,
                entityName = cardComponent.name,
                firstThisTurn = false,
                placedBy = action.playerId
            )
        )

        // Mark it suspended and pre-arm haste — identical to GrantSuspendExecutor, so the
        // engine's synthesized countdown-and-cast trigger (Suspend.countdownAbility) takes
        // over from here exactly as it does for a runtime-granted suspend.
        currentState = currentState.updateEntity(action.cardId) { c -> c.with(SuspendedComponent) }
        currentState = currentState.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantKeyword(Keyword.HASTE.name),
            affectedEntities = setOf(action.cardId),
            duration = Duration.WhileControlledByController,
            context = EffectContext(sourceId = action.cardId, controllerId = action.playerId),
        )

        currentState = currentState.tick()

        // Suspend is a special action — does not change priority and does not use the stack.
        return ExecutionResult.success(currentState, events)
    }

    companion object {
        fun create(services: EngineServices): SuspendCardFromHandHandler {
            return SuspendCardFromHandHandler(
                services.cardRegistry,
                services.manaSolver,
                services.manaAbilitySideEffectExecutor,
                services.turnManager,
                com.wingedsheep.engine.legalactions.utils.CastPermissionUtils(
                    services.cardRegistry, services.predicateEvaluator, services.conditionEvaluator
                ),
            )
        }
    }
}
