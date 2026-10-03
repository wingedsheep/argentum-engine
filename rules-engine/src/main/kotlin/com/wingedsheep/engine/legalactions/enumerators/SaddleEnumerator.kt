package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.handlers.actions.ability.CrewSaddleContributionEvaluator
import com.wingedsheep.engine.legalactions.TapForPowerCreatureData
import com.wingedsheep.engine.mechanics.combat.rules.AttackAvailability
import com.wingedsheep.engine.state.components.battlefield.SaddledComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CrewSaddleCost
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Enumerates Saddle actions for permanents with Saddle N controlled by the player (CR 702.171a).
 *
 * Saddle reuses the same "tap any number of creatures with total power >= N" selection as Crew,
 * but with two differences: it taps any number of *other* untapped creatures the player controls
 * (the mount itself can't saddle itself), and it can be activated **only as a sorcery**. Summoning
 * sickness does not prevent a creature from saddling (it's not attacking or using a tap ability of
 * its own — the same reasoning as crewing).
 */
class SaddleEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        // "Activate only as a sorcery" (CR 702.171a): your main phase, empty stack, your turn.
        if (!context.canPlaySorcerySpeed) return emptyList()

        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId
        val projected = context.projected
        // Every Mount sees the same candidate creatures, so answer "could it attack?" once per
        // creature rather than once per (Mount, creature) pair.
        val canAttackCache = HashMap<EntityId, Boolean>()

        for (entityId in context.battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            // By definition id, not name — `SaddleMountHandler` resolves the saddle keyword by id,
            // so a renamed copy of a Mount (CR 707.9) would otherwise be saddleable by the engine
            // but never offered the Saddle action.
            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue

            val saddleAbility = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.Numeric>()
                .firstOrNull { it.keyword == Keyword.SADDLE } ?: continue

            // Saddle is an activated ability (CR 702.171a) — mirror `SaddleMountHandler`'s
            // activation-prohibition check so it's never offered and then refused.
            if (context.castPermissionUtils.isActivationForbidden(state, entityId, playerId)) continue

            // Find all other untapped creatures the player controls that can saddle this mount.
            val validSaddleCreatures = mutableListOf<TapForPowerCreatureData>()
            var totalAvailablePower = 0
            for (creatureId in context.battlefieldPermanents) {
                if (creatureId == entityId) continue // a mount can't saddle itself ("other")
                if (!projected.isCreature(creatureId)) continue
                val creatureContainer = state.getEntity(creatureId) ?: continue
                if (creatureContainer.has<TappedComponent>()) continue
                // What it contributes, not its raw power — `SaddleMountHandler` measures the cost
                // through the same evaluator, so a creature that "saddles as though its power were
                // 2 greater" must read that way here or the client's progress bar would refuse a
                // saddle the engine accepts.
                val power = CrewSaddleContributionEvaluator.evaluate(
                    state, projected, context.cardRegistry, creatureId, CrewSaddleCost.SADDLE
                )
                val creatureName = creatureContainer.get<CardComponent>()?.name ?: "Unknown"
                val canAttack = canAttackCache.getOrPut(creatureId) {
                    AttackAvailability.canAttack(state, projected, creatureId, playerId, context.cardRegistry, context.predicateEvaluator)
                }
                validSaddleCreatures.add(
                    TapForPowerCreatureData(creatureId, creatureName, power, canAttack)
                )
                totalAvailablePower += power
            }

            val canAfford = totalAvailablePower >= saddleAbility.n
            // Saddle has no once-each-turn clause (CR 702.171a), and re-activating it adds
            // saddlers that "creatures that saddled it this turn" payoffs count — so keep offering
            // it on an already-saddled Mount, but label it so the player doesn't spend creatures
            // thinking they still need the designation.
            val alreadySaddled = container.has<SaddledComponent>()
            result.add(
                LegalAction(
                    actionType = "SaddleMount",
                    description = if (alreadySaddled) {
                        "Saddle ${cardComponent.name} again"
                    } else {
                        "Saddle ${cardComponent.name}"
                    },
                    action = SaddleMount(playerId, entityId, emptyList()),
                    affordable = canAfford,
                    tapForPower = true,
                    tapForPowerRequired = saddleAbility.n,
                    tapForPowerCreatures = validSaddleCreatures
                )
            )
        }

        return result
    }
}
