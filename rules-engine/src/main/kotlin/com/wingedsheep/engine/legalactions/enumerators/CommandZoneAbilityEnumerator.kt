package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.manaCostOrNull

/**
 * Enumerates activated abilities on cards in the player's command zone.
 *
 * The motivating case is the Momir Basic Vanguard avatar
 * ([com.wingedsheep.sdk.core.Format.MomirBasic]), which grants "{X}{X}{X}, Discard a card: …"
 * with `activateFromZone == Zone.COMMAND`. Modeled on [ZoneActivatedAbilityEnumerator]: scan a
 * non-battlefield zone, filter abilities by `activateFromZone`, gate sorcery timing on
 * [EnumerationContext.canPlaySorcerySpeed], honour activation restrictions, and surface
 * mana/discard cost payability plus X-cost info. [com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler]
 * already accepts non-battlefield `activateFromZone` generically (owner + zone-membership check),
 * so no handler change is needed.
 */
class CommandZoneAbilityEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId

        for (entityId in state.getZone(playerId, Zone.COMMAND)) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            val commandAbilities = cardDef.script.activatedAbilities.filter {
                it.activateFromZone == Zone.COMMAND
            }

            for (ability in commandAbilities) {
                // "Activate only as a sorcery" — mirror the graveyard/battlefield gate so the
                // action is never offered at instant speed (the handler would reject it anyway).
                if (ability.timing == TimingRule.SorcerySpeed && !context.canPlaySorcerySpeed) continue

                // Kang the Conqueror's turn-scoped power-up lockout is zone-independent, and so is
                // the matching check in `ActivateAbilityHandler.validate` — which sits before the
                // zone split and so would reject a command-zone power-up the same way. Guard here
                // too, or the action would be offered and then refused.
                if (context.castPermissionUtils.isPowerUpActivationRestricted(state, ability)) continue

                // An any-zone "players can't activate abilities" (Yuriko, Blade of the Mighty) or
                // name lock (Pithing Needle).
                if (context.castPermissionUtils.isActivationForbidden(
                        state, entityId, playerId, abilityIsManaAbility = ability.isManaAbility
                    )
                ) continue

                // Activation restrictions (e.g. once each turn).
                if (!context.legality.activationRestrictionsMet(state, playerId, entityId, ability)) continue

                // Cost payability — Mana and Discard, atom or composite (the avatar's "{X}{X}{X},
                // Discard a card"). Other atoms are validated by the handler at payment time.
                val effectiveCost = ability.cost
                val handCards = state.getZone(playerId, Zone.HAND)
                val abilityContext = com.wingedsheep.engine.mechanics.mana.buildAbilityPaymentContext(
                    cardComponent, context.projected, entityId, ability
                )
                var costCanBePaid = true
                var hasDiscardCost = false

                fun checkAtom(atom: CostAtom) {
                    when (atom) {
                        is CostAtom.Mana -> {
                            // X costs: payability is gated by maxAffordableX below; a {X} cost with
                            // no fixed mana is payable at X=0, so only reject a non-X mana cost the
                            // player can't afford.
                            if (!atom.cost.hasX &&
                                !context.manaSolver.canPay(
                                    state, playerId, atom.cost,
                                    precomputedSources = context.availableManaSources,
                                    spellContext = abilityContext
                                )
                            ) costCanBePaid = false
                        }
                        is CostAtom.Discard -> {
                            hasDiscardCost = true
                            if (handCards.size < atom.count) costCanBePaid = false
                        }
                        else -> {}
                    }
                }

                if (!com.wingedsheep.engine.mechanics.cost.PlayerCounterPayment.canAffordAbility(state, playerId, effectiveCost)) continue

                when (effectiveCost) {
                    is AbilityCost.Atom -> checkAtom(effectiveCost.atom)
                    is AbilityCost.Composite -> effectiveCost.costs.forEach { sub ->
                        (sub as? AbilityCost.Atom)?.let { checkAtom(it.atom) }
                    }
                    else -> {}
                }
                if (!costCanBePaid) continue

                val costInfo = if (hasDiscardCost) {
                    AdditionalCostData(
                        description = "Discard a card",
                        costType = "DiscardCard",
                        validDiscardTargets = handCards,
                        discardCount = 1
                    )
                } else null

                val abilityManaCost = when (effectiveCost) {
                    is AbilityCost.Atom -> effectiveCost.manaCostOrNull
                    is AbilityCost.Composite -> effectiveCost.costs.firstNotNullOfOrNull { it.manaCostOrNull }
                    else -> null
                }
                val hasXCost = abilityManaCost?.hasX == true || context.costUtils.hasPlayerChosenNonManaX(effectiveCost)
                val maxAffordableX = if (hasXCost) {
                    context.costUtils.calculateMaxAffordableX(
                        state, playerId, effectiveCost, abilityManaCost,
                        precomputedSources = context.availableManaSources,
                        // Same source scoping the battlefield enumerator passes: cost filters
                        // routinely resolve against the ability's own permanent.
                        sourceId = entityId
                    )
                } else null

                result.add(
                    LegalAction(
                        actionType = "ActivateAbility",
                        description = ability.description,
                        action = ActivateAbility(playerId, entityId, ability.id),
                        additionalCostInfo = costInfo,
                        hasXCost = hasXCost,
                        maxAffordableX = maxAffordableX,
                        manaCostString = abilityManaCost?.toString()
                    )
                )
            }
        }

        return result
    }
}
