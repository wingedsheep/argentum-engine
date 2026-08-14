package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.manaCostOrNull
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Enumerates activated abilities on cards in a non-battlefield [zone] the player owns —
 * one instance per zone (registered for [Zone.GRAVEYARD] and [Zone.HAND]).
 *
 * The abilities surfaced are those whose `activateFromZone` matches this enumerator's [zone]:
 * - **Graveyard** — Undead Gladiator ({1}{B}, Discard a card: Return this from your graveyard…),
 *   Renew, and the Blight/return-to-hand graveyard shells.
 * - **Hand** — the "discard this card from hand" activated abilities such as Steel Wrecking Ball
 *   ({1}{R}, Discard this card: Destroy target artifact) and Stegron the Dinosaur Man, plus
 *   Urban Retreat's "{2}, Return a tapped creature you control: put this onto the battlefield".
 *
 * Handles cost checking for Mana, Discard, DiscardSelf, ExileSelf, Blight, and Composite (with
 * ReturnToHand), plus target requirements and X/auto-tap info.
 * [com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler] already accepts any
 * non-battlefield `activateFromZone` generically (owner + zone-membership check), so no handler
 * change is needed.
 */
class ZoneActivatedAbilityEnumerator(private val zone: Zone) : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId

        val zoneCards = state.getZone(playerId, zone)
        for (entityId in zoneCards) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Granted abilities count alongside printed ones: a card can be *given* a
            // graveyard-activated ability at runtime (Cursecloth Wrappings granting embalm), and
            // that grant is keyed to the card entity, not to its definition — so a card whose
            // definition has no zone ability at all must still be considered.
            val grantedZoneAbilities = state.grantedActivatedAbilities
                .filter { it.entityId == entityId && it.ability.activateFromZone == zone }
                .map { it.ability }
            val printedZoneAbilities = context.cardRegistry.getCard(cardComponent.name)
                ?.script?.activatedAbilities
                ?.filter { it.activateFromZone == zone }
                .orEmpty()
            val zoneAbilities = printedZoneAbilities + grantedZoneAbilities
            if (zoneAbilities.isEmpty()) continue

            for (ability in zoneAbilities) {
                // "Activate only as a sorcery" — Renew and similar zone-activated abilities are
                // gated to sorcery timing. Mirrors ActivatedAbilityEnumerator's battlefield check
                // so the action is never offered at instant speed (where the handler would reject
                // it anyway).
                if (ability.timing == TimingRule.SorcerySpeed && !context.canPlaySorcerySpeed) continue

                // Check activation restrictions
                var restrictionsMet = true
                for (restriction in ability.restrictions) {
                    if (!context.castPermissionUtils.checkActivationRestriction(
                            state, playerId, restriction, entityId, ability.id, ability.isExhaust
                        )
                    ) {
                        restrictionsMet = false
                        break
                    }
                }
                if (!restrictionsMet) continue

                // Check cost requirements and build cost info
                val effectiveCost = ability.cost
                var costCanBePaid = true
                val handCards = state.getZone(playerId, Zone.HAND)
                var hasDiscardCost = false
                var blightCost: AbilityCost.Blight? = null
                var blightCreatures: List<EntityId> = emptyList()

                val abilityContext = com.wingedsheep.engine.mechanics.mana.buildAbilityPaymentContext(
                    cardComponent, context.projected, entityId
                )

                when (effectiveCost) {
                    is AbilityCost.Atom -> when (val atom = effectiveCost.atom) {
                        is CostAtom.Mana -> {
                            if (!context.manaSolver.canPay(state, playerId, atom.cost, precomputedSources = context.availableManaSources, spellContext = abilityContext)) costCanBePaid = false
                        }
                        is CostAtom.Discard -> {
                            hasDiscardCost = true
                            if (handCards.isEmpty()) costCanBePaid = false
                        }
                        // Other atoms — engine validates at payment.
                        else -> {}
                    }
                    is AbilityCost.Blight -> {
                        blightCost = effectiveCost
                        blightCreatures = context.projected.getBattlefieldControlledBy(playerId)
                            .filter { context.projected.isCreature(it) && context.projected.canReceiveCounters(it) }
                        if (blightCreatures.isEmpty()) costCanBePaid = false
                    }
                    is AbilityCost.Composite -> {
                        for (subCost in effectiveCost.costs) {
                            when (subCost) {
                                is AbilityCost.Atom -> when (val atom = subCost.atom) {
                                    is CostAtom.Mana -> {
                                        if (!context.manaSolver.canPay(state, playerId, atom.cost, precomputedSources = context.availableManaSources, spellContext = abilityContext)) {
                                            costCanBePaid = false; break
                                        }
                                    }
                                    is CostAtom.Discard -> {
                                        hasDiscardCost = true
                                        if (handCards.isEmpty()) {
                                            costCanBePaid = false; break
                                        }
                                    }
                                    is CostAtom.ReturnToHand -> {
                                        val targets = context.costUtils.findAbilityBounceTargets(state, playerId, atom.filter)
                                        if (targets.size < atom.count) {
                                            costCanBePaid = false; break
                                        }
                                    }
                                    // Other atoms — engine validates at payment.
                                    else -> {}
                                }
                                // Self-removal costs are always payable — the card is in [zone].
                                is AbilityCost.ExileSelf, is AbilityCost.DiscardSelf -> {}
                                is AbilityCost.Blight -> {
                                    blightCost = subCost
                                    blightCreatures = context.projected.getBattlefieldControlledBy(playerId)
                                        .filter { context.projected.isCreature(it) && context.projected.canReceiveCounters(it) }
                                    if (blightCreatures.isEmpty()) {
                                        costCanBePaid = false; break
                                    }
                                }
                                else -> {}
                            }
                        }
                    }
                    else -> {}
                }
                if (!costCanBePaid) continue

                // Build cost info for bounce or discard costs
                val bounceCostFromZone: CostAtom.ReturnToHand? = when (effectiveCost) {
                    is AbilityCost.Composite -> effectiveCost.costs
                        .firstNotNullOfOrNull { (it as? AbilityCost.Atom)?.atom as? CostAtom.ReturnToHand }
                    is AbilityCost.Atom -> effectiveCost.atom as? CostAtom.ReturnToHand
                    else -> null
                }
                val costInfo = if (bounceCostFromZone != null) {
                    val bounceTargets = context.costUtils.findAbilityBounceTargets(
                        state, playerId, bounceCostFromZone.filter
                    )
                    AdditionalCostData(
                        description = bounceCostFromZone.description.replaceFirstChar { it.uppercase() },
                        costType = "BouncePermanent",
                        validBounceTargets = bounceTargets,
                        bounceCount = bounceCostFromZone.count
                    )
                } else if (blightCost != null) {
                    AdditionalCostData(
                        description = "creature to blight",
                        costType = "Blight",
                        validBlightTargets = blightCreatures,
                        blightAmount = blightCost.amount
                    )
                } else if (hasDiscardCost) {
                    AdditionalCostData(
                        description = "Discard a card",
                        costType = "DiscardCard",
                        validDiscardTargets = handCards,
                        discardCount = 1
                    )
                } else null

                // Calculate X cost info
                val abilityManaCost = when (val c = ability.cost) {
                    is AbilityCost.Atom -> c.manaCostOrNull
                    is AbilityCost.Composite -> c.costs.firstNotNullOfOrNull { it.manaCostOrNull }
                    else -> null
                }
                val zoneManaCostString = abilityManaCost?.toString()
                val abilityHasXCost = abilityManaCost?.hasX == true
                val abilityMaxAffordableX: Int? = if (abilityHasXCost) {
                    val availableSources = context.manaSolver.getAvailableManaCount(state, playerId, precomputedSources = context.availableManaSources)
                    val fixedCost = abilityManaCost.cmc
                    (availableSources - fixedCost).coerceAtLeast(0)
                } else null

                // Compute auto-tap preview for UI highlighting (skipped in ACTIONS_ONLY mode)
                val abilityAutoTapPreview = if (context.skipAutoTapPreview || abilityManaCost == null || abilityHasXCost) null
                else context.manaSolver.solve(state, playerId, abilityManaCost, precomputedSources = context.availableManaSources)?.sources?.map { it.entityId }

                // Check for target requirements
                val targetReqs = ability.targetRequirements
                if (targetReqs.isNotEmpty()) {
                    val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs, sourceId = entityId)
                    val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                    if (!allSatisfied) continue

                    val firstReq = targetReqs.first()
                    val firstInfo = targetInfos.first()

                    if (targetReqs.size == 1 &&
                        context.targetUtils.shouldAutoSelectPlayerTarget(firstReq, firstInfo.validTargets)
                    ) {
                        val autoSelectedTarget = ChosenTarget.Player(firstInfo.validTargets.first())
                        result.add(
                            LegalAction(
                                actionType = "ActivateAbility",
                                description = ability.description,
                                action = ActivateAbility(
                                    playerId, entityId, ability.id,
                                    targets = listOf(autoSelectedTarget)
                                ),
                                additionalCostInfo = costInfo,
                                hasXCost = abilityHasXCost,
                                maxAffordableX = abilityMaxAffordableX,
                                autoTapPreview = abilityAutoTapPreview,
                                manaCostString = zoneManaCostString
                            )
                        )
                    } else {
                        result.add(
                            LegalAction(
                                actionType = "ActivateAbility",
                                description = ability.description,
                                action = ActivateAbility(playerId, entityId, ability.id),
                                validTargets = firstInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstInfo.maxTargets,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                                additionalCostInfo = costInfo,
                                hasXCost = abilityHasXCost,
                                maxAffordableX = abilityMaxAffordableX,
                                autoTapPreview = abilityAutoTapPreview,
                                manaCostString = zoneManaCostString
                            )
                        )
                    }
                } else {
                    result.add(
                        LegalAction(
                            actionType = "ActivateAbility",
                            description = ability.description,
                            action = ActivateAbility(playerId, entityId, ability.id),
                            additionalCostInfo = costInfo,
                            hasXCost = abilityHasXCost,
                            maxAffordableX = abilityMaxAffordableX,
                            autoTapPreview = abilityAutoTapPreview,
                            manaCostString = zoneManaCostString
                        )
                    )
                }
            }
        }

        return result
    }
}
