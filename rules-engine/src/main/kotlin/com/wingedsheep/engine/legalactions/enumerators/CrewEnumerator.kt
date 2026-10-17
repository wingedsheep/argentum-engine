package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.handlers.actions.ability.CrewRequirement
import com.wingedsheep.engine.handlers.actions.ability.CrewSaddleContributionEvaluator
import com.wingedsheep.engine.handlers.actions.ability.TapForPowerActivationTax
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.TapForPowerCreatureData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.combat.rules.AttackAvailability
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.scripting.CrewSaddleCost

/**
 * Enumerates crew actions for Vehicle permanents controlled by the player.
 *
 * A Vehicle can be crewed by tapping any number of untapped creatures whose
 * total power meets or exceeds the crew requirement. Summoning sickness does
 * NOT prevent a creature from crewing.
 */
class CrewEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId
        val projected = context.projected
        // Every Vehicle sees the same candidate creatures, so answer "could it attack?" once per
        // creature rather than once per (Vehicle, creature) pair.
        val canAttackCache = HashMap<EntityId, Boolean>()

        for (entityId in context.battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            // Printed or granted ("Vehicles you control have crew 1"), minus a once-each-turn crew
            // already used — the same resolution `CrewVehicleHandler` validates against.
            val crewAbility = CrewRequirement.available(state, projected, context.cardRegistry, entityId) ?: continue

            // Crew is an activated ability (CR 702.122a) — mirror `CrewVehicleHandler`'s
            // activation-prohibition check so it's never offered and then refused.
            if (context.castPermissionUtils.isActivationForbidden(state, entityId, playerId)) continue

            // Find all untapped creatures controlled by the player that can crew.
            //
            // This eligibility rule (untapped, controlled by the payer, projected, no
            // summoning-sickness check) is duplicated in
            // `com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost.candidates`, which
            // Teamwork N (CR 702.194a) pays through — the two agree, but nothing enforces that.
            // The *measure* deliberately differs and must stay split: crew sums through
            // `CrewSaddleContributionEvaluator` so crew-specific statics ("crews Vehicles as though
            // its power were 2 greater") count, which they must not for teamwork. Folding the
            // selection (not the measure) into the shared helper is an open follow-up.
            val validCrewCreatures = mutableListOf<TapForPowerCreatureData>()
            var totalAvailablePower = 0
            for (creatureId in context.battlefieldPermanents) {
                if (creatureId == entityId) continue // Vehicle can't crew itself
                if (!projected.isCreature(creatureId)) continue
                val creatureContainer = state.getEntity(creatureId) ?: continue
                if (creatureContainer.has<TappedComponent>()) continue
                if (projected.hasKeyword(creatureId, AbilityFlag.CANT_CREW)) continue
                // Summoning sickness does NOT prevent crewing.
                // What it contributes, not its raw power — `CrewVehicleHandler` measures the cost
                // through the same evaluator, so a creature that "crews Vehicles as though its
                // power were 2 greater" must read that way here or the client's progress bar would
                // refuse a crew the engine accepts.
                val power = CrewSaddleContributionEvaluator.evaluate(
                    state, projected, context.cardRegistry, creatureId, CrewSaddleCost.CREW
                )
                val creatureName = creatureContainer.get<CardComponent>()?.name ?: "Unknown"
                val canAttack = canAttackCache.getOrPut(creatureId) {
                    AttackAvailability.canAttack(state, projected, creatureId, playerId, context.cardRegistry, context.predicateEvaluator)
                }
                validCrewCreatures.add(
                    TapForPowerCreatureData(creatureId, creatureName, power, canAttack)
                )
                totalAvailablePower += power
            }

            // A cost-modifying static can add mana to crew/saddle; the handler charges it after
            // tapping the creatures, so it's priced here the same way.
            val tax = TapForPowerActivationTax.of(context.castPermissionUtils, state, entityId, playerId)
            val canAfford = totalAvailablePower >= crewAbility.n &&
                (tax.isEmpty() || context.manaSolver.canPay(state, playerId, tax))
            result.add(
                LegalAction(
                    actionType = "CrewVehicle",
                    description = "Crew ${cardComponent.name}",
                    action = CrewVehicle(playerId, entityId, emptyList()),
                    affordable = canAfford,
                    manaCostString = tax.takeUnless { it.isEmpty() }?.toString(),
                    tapForPower = true,
                    tapForPowerRequired = crewAbility.n,
                    tapForPowerCreatures = validCrewCreatures
                )
            )
        }

        return result
    }
}
