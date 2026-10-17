package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.CrewOrSaddleContributionEvent
import com.wingedsheep.engine.core.CrewOrSaddleKind
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.scripting.CrewSaddleCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.AddCardTypeEffect
import com.wingedsheep.sdk.scripting.effects.BecomeCrewedEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Handler for the CrewVehicle action.
 *
 * Crew is an activated ability on Vehicles. The cost is tapping creatures with
 * total power >= the crew requirement. The effect (Vehicle becomes an artifact
 * creature until end of turn) goes on the stack.
 */
class CrewVehicleHandler(
    private val cardRegistry: CardRegistry,
    private val stackResolver: StackResolver,
    private val castPermissionUtils: com.wingedsheep.engine.legalactions.utils.CastPermissionUtils? = null,
    private val manaSolver: com.wingedsheep.engine.mechanics.mana.ManaSolver? = null,
    private val costPaymentService: com.wingedsheep.engine.mechanics.cost.CostPaymentService? = null,
) : ActionHandler<CrewVehicle> {
    override val actionType: KClass<CrewVehicle> = CrewVehicle::class

    override fun validate(state: GameState, action: CrewVehicle): String? {
        if (!state.hasPriority(action.playerId)) {
            return "You don't have priority"
        }

        val vehicleContainer = state.getEntity(action.vehicleId)
            ?: return "Vehicle not found: ${action.vehicleId}"

        vehicleContainer.get<CardComponent>()
            ?: return "Not a card: ${action.vehicleId}"

        // Vehicle must be on the battlefield
        if (action.vehicleId !in state.getBattlefield()) {
            return "Vehicle is not on the battlefield"
        }

        // Vehicle must be controlled by the player (use projected state)
        val projected = state.projectedState
        val vehicleController = projected.getController(action.vehicleId)
        if (vehicleController != action.playerId) {
            return "You don't control this vehicle"
        }

        // Crew is an activated ability of the Vehicle (CR 702.122a), so a "players can't activate
        // abilities" static (Yuriko, Blade of the Mighty; Grand Abolisher on an artifact) or a name
        // lock (Pithing Needle) forbids it.
        if (castPermissionUtils?.isActivationForbidden(state, action.vehicleId, action.playerId) == true) {
            return "An effect prevents you from activating that ability right now"
        }

        // Vehicle must have a crew ability it can activate now — printed or granted, minus a
        // once-each-turn crew (Luxurious Locomotive) already used this turn.
        val crewAbility = CrewRequirement.available(state, projected, cardRegistry, action.vehicleId)
            ?: return if (CrewRequirement.hasAny(state, projected, cardRegistry, action.vehicleId)) {
                "This Vehicle's crew ability can only be activated once each turn"
            } else {
                "This permanent doesn't have crew"
            }

        if (action.crewCreatures.isEmpty()) {
            return "Must select at least one creature to crew"
        }

        // Validate each crew creature
        var totalPower = 0
        for (creatureId in action.crewCreatures) {
            if (creatureId == action.vehicleId) {
                return "A vehicle cannot crew itself"
            }

            val creatureContainer = state.getEntity(creatureId)
                ?: return "Creature not found: $creatureId"

            if (creatureId !in state.getBattlefield()) {
                return "Creature is not on the battlefield: $creatureId"
            }

            // Must be controlled by the player
            val creatureController = projected.getController(creatureId)
            if (creatureController != action.playerId) {
                return "You don't control creature: $creatureId"
            }

            // Must be a creature (use projected state)
            if (!projected.isCreature(creatureId)) {
                return "Not a creature: $creatureId"
            }

            // Must not be tapped
            if (creatureContainer.has<TappedComponent>()) {
                return "Creature is already tapped: $creatureId"
            }

            // "Can't crew Vehicles" (Revoke Privileges) — a projected restriction flag.
            if (projected.hasKeyword(creatureId, AbilityFlag.CANT_CREW)) {
                return "That creature can't crew Vehicles: $creatureId"
            }

            // Summoning sickness does NOT prevent crewing (CR 702.122c)
            totalPower += CrewSaddleContributionEvaluator.evaluate(
                state = state,
                projected = projected,
                cardRegistry = cardRegistry,
                creatureId = creatureId,
                cost = CrewSaddleCost.CREW
            )
        }

        if (totalPower < crewAbility.n) {
            return "Total power ($totalPower) is less than crew requirement (${crewAbility.n})"
        }

        // A cost-modifying static can add mana to the cost (Anointed Peacekeeper naming this card).
        val tax = TapForPowerActivationTax.of(castPermissionUtils, state, action.vehicleId, action.playerId)
        if (!TapForPowerActivationTax.canPay(manaSolver, state, action.playerId, tax, action.crewCreatures)) {
            return "Not enough mana to pay the crew cost ($tax)"
        }

        return null
    }

    override fun execute(state: GameState, action: CrewVehicle): ExecutionResult {
        val vehicleContainer = state.getEntity(action.vehicleId)
            ?: return ExecutionResult.error(state, "Vehicle not found")

        val vehicleCard = vehicleContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        cardRegistry.getCard(vehicleCard.cardDefinitionId)
            ?: return ExecutionResult.error(state, "Card definition not found")

        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Pay the cost: tap each crew creature
        for (creatureId in action.crewCreatures) {
            val tapOutcome = tap(currentState, creatureId)
            currentState = tapOutcome.state
            events.addAll(tapOutcome.events)
            if (tapOutcome.tapped != null) {
                events.add(
                    CrewOrSaddleContributionEvent(
                        contributorId = creatureId,
                        permanentId = action.vehicleId,
                        controllerId = action.playerId,
                        kind = CrewOrSaddleKind.CREW
                    )
                )
            }
        }

        // Any tax is paid after the creatures are tapped, so none of them is also tapped for mana.
        val tax = TapForPowerActivationTax.of(castPermissionUtils, state, action.vehicleId, action.playerId)
        val (paidState, paidEvents) = TapForPowerActivationTax.pay(
            costPaymentService, currentState, state, action.playerId, action.vehicleId, tax
        ) ?: return TapForPowerActivationTax.unaffordable(state, "crew")
        currentState = paidState
        events.addAll(paidEvents)

        // Record the crew so Vehicle payoffs can read "creatures that crewed it this turn"
        // (e.g. Luxurious Locomotive). Union across activations within the turn.
        currentState = currentState.updateEntity(action.vehicleId) { c ->
            val existing = c.get<CrewSaddleContributorsComponent>()
            // Crew and saddle are activated abilities too — "was activated this turn".
            val activated = c.get<AbilityActivatedThisTurnComponent>() ?: AbilityActivatedThisTurnComponent()
            c.with(activated.withAnyActivated()).with(
                (existing ?: CrewSaddleContributorsComponent(creatureIds = emptySet())).let {
                    it.copy(
                        creatureIds = it.creatureIds + action.crewCreatures,
                        crewActivations = it.crewActivations + 1
                    )
                }
            )
        }

        // ...and on the activator's per-turn memory of permanents whose abilities they activated
        // (crewing a Vehicle is an artifact's activated ability to Tezzeret, Betrayer of Flesh).
        currentState = com.wingedsheep.engine.core.recordPermanentActivation(
            currentState, action.playerId, com.wingedsheep.engine.core.activationSourceSnapshot(state, action.vehicleId)
        )

        // Create the crew ability effect: "this Vehicle becomes an artifact creature until end of
        // turn" (CR 702.122a). Only the CREATURE type is added (the Vehicle is already an artifact);
        // crew does NOT set power and toughness — the Vehicle keeps its printed P/T, so an earlier
        // P/T-setting effect such as Kudo, King Among Bears's base 2/2 still applies to it.
        // Resolving it is also what makes the Vehicle "become crewed" (CR 702.122e), so the
        // BecomeCrewed step follows the type change.
        val crewEffect = CompositeEffect(
            listOf(
                AddCardTypeEffect(
                    cardType = "CREATURE",
                    target = EffectTarget.Self,
                    duration = Duration.EndOfTurn
                ),
                BecomeCrewedEffect(EffectTarget.Self)
            ),
            descriptionOverride = "This Vehicle becomes an artifact creature until end of turn."
        )

        // Put the crew ability on the stack
        val abilityOnStack = ActivatedAbilityOnStackComponent(
            sourceId = action.vehicleId,
            objectReferences = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(captured = true, origin = state.objectRef(action.vehicleId), source = state.objectRef(action.vehicleId)),
            sourceName = vehicleCard.name,
            controllerId = action.playerId,
            effect = crewEffect
        )

        val stackResult = stackResolver.putActivatedAbility(
            currentState, abilityOnStack
        )
        currentState = stackResult.newState
        events.addAll(stackResult.events)

        // Detect and process triggers from tapping creatures
        val allEvents = events.toList()
        return ExecutionResult.success(
            currentState.withPriority(action.playerId),
            allEvents
        )
    }

    companion object {
        fun create(services: EngineServices): CrewVehicleHandler {
            return CrewVehicleHandler(
                services.cardRegistry,
                services.stackResolver,
                services.castPermissionUtils,
                services.manaSolver,
                services.costPaymentService,
            )
        }
    }
}
