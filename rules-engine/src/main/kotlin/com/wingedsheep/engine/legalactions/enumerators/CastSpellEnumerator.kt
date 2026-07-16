package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.ModalEnumerationMode
import com.wingedsheep.engine.legalactions.ModalLegalEnumeration
import com.wingedsheep.engine.legalactions.TargetInfo
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardFace
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.engine.mechanics.mana.ManaSource
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext

/**
 * Enumerates legal CastSpell actions for spells in hand.
 *
 * Handles all casting complexity: additional costs, alternative costs,
 * self-alternative costs, convoke, delve, X costs, modal spells,
 * targeting, auto-select player targets, and kicker.
 */
class CastSpellEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId

        val hand = state.getHand(playerId)

        // Memory Vessel: "they can't play cards from their hand." Every card iterated here is in
        // the hand zone, so a blanket skip enforces the restriction without touching exile/graveyard
        // casts (those are enumerated by CastFromZoneEnumerator / GraveyardAbilityEnumerator).
        if (context.cantPlayCardsFromHand) return result

        // --- Normal spell casting ---
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) {
                // A land's primary characteristics are *played*, not cast (PlayLandEnumerator
                // handles that). But a land//spell Adventure (FIN Towns — "Land — Town //
                // Sorcery — Adventure", CR 715) still exposes a castable Adventure spell face
                // from hand. Enumerate that face, then skip the normal cast path below, which
                // never applies to a land. When the Adventure resolves it exiles itself and
                // grants permission to *play the land* from exile (StackResolver / CR 715.3d).
                if (!context.cantCastSpell(cardId)) {
                    val landCardDef = context.cardRegistry.getCard(cardComponent.name)
                    if (landCardDef != null &&
                        (landCardDef.layout == com.wingedsheep.sdk.model.CardLayout.ADVENTURE ||
                            landCardDef.layout == com.wingedsheep.sdk.model.CardLayout.OMEN ||
                            landCardDef.layout == com.wingedsheep.sdk.model.CardLayout.MODAL_DFC) &&
                        landCardDef.cardFaces.isNotEmpty() &&
                        context.castPermissionUtils.checkCastRestrictions(
                            state, playerId, landCardDef.script.castRestrictions
                        )
                    ) {
                        // The land primary is always an available alternative, so surface the
                        // spell face even when unaffordable (grayed out in the drag-to-play menu).
                        enumerateSecondaryFace(
                            context, cardId, landCardDef, result, primaryFaceAffordable = true
                        )
                    }
                }
                continue
            }

            // Skip this spell if the player can't cast it — a blanket lock, Mana Maze color
            // sharing, or a PlayersCantCastSpells restriction (all routed through one chokepoint).
            if (context.cantCastSpell(cardId)) continue

            // Look up card definition for target requirements and cast restrictions
            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue

            // Check cast restrictions first
            val castRestrictions = cardDef.script.castRestrictions
            if (!context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) {
                continue
            }

            // Split-layout cards (CR 709) — Phase 2 minimal handling.
            //
            // A split card fans out into one cast option per face (CR 709.3a — only the chosen
            // half goes on the stack and is evaluated for legality). Each face has its own
            // mana cost, so we emit one CastSpell action per face with `faceIndex = i` and the
            // face's printed cost.
            //
            // Targeted instant/sorcery split halves are supported (the Invasion split cards —
            // Pain // Suffering, Stand // Deliver, Wax // Wane — read their face's
            // `targetRequirements` in CastSpellHandler). Modal effects, alternative costs, blight,
            // behold, kicker, and convoke on a split half are not yet wired up.
            // Adventure (CR 715) and modal DFC (CR 712) both expose a secondary spell face that
            // is castable from hand alongside the primary characteristics. Emit one cast option
            // for that face, then fall through so the surrounding code also enumerates the
            // primary (creature) face. The secondary face carries its own mana cost, type line,
            // target requirements, and spell effect; the primary face uses the card's top-level
            // fields. (MODAL_DFC, ADVENTURE, and OMEN differ only at resolution — exile-then-recast
            // for Adventure, shuffle-into-library for Omen, plain graveyard for modal DFC — which
            // is handled in StackResolver, not here.)
            // When true, the secondary (spell) face of an Adventure/Omen/modal-DFC card is
            // affordable. The primary-face logic below reads this so that, when the primary
            // (creature) face itself is unaffordable, it can still surface a grayed-out
            // placeholder — keeping both faces in the drag-to-play action menu.
            var secondaryFaceAffordable = false
            if ((cardDef.layout == com.wingedsheep.sdk.model.CardLayout.ADVENTURE ||
                    cardDef.layout == com.wingedsheep.sdk.model.CardLayout.OMEN ||
                    cardDef.layout == com.wingedsheep.sdk.model.CardLayout.MODAL_DFC) &&
                cardDef.cardFaces.isNotEmpty()
            ) {
                // Approximate primary-face affordability (plain mana only — convoke/delve/alt
                // costs are ignored here; a false negative just means we don't surface a
                // grayed-out secondary face in those rare cases). Used by enumerateSecondaryFace
                // to decide whether an unaffordable secondary face is still worth showing.
                val primaryFaceCost = context.costCalculator.calculateMinPossibleCost(state, cardDef, playerId)
                val primaryFaceAffordable = context.manaSolver.canPay(
                    state, playerId, primaryFaceCost, precomputedSources = context.availableManaSources
                )
                secondaryFaceAffordable = enumerateSecondaryFace(
                    context, cardId, cardDef, result, primaryFaceAffordable = primaryFaceAffordable
                )
                // Don't `continue` — let the surrounding loop also enumerate the primary face.
            }

            if (cardDef.layout == com.wingedsheep.sdk.model.CardLayout.SPLIT && cardDef.cardFaces.isNotEmpty()) {
                cardDef.cardFaces.forEachIndexed { faceIndex, face ->
                    enumerateSplitFace(context, cardId, cardDef, faceIndex, face, result)
                }
                continue
            }

            // Check timing - sorcery-speed spells need main phase, empty stack, your turn
            val isInstant = cardComponent.typeLine.isInstant
            val hasFlash = cardDef.keywords.contains(Keyword.FLASH)
            val grantedFlash = hasFlash || context.castPermissionUtils.hasGrantedFlash(state, cardId)
            if (!isInstant && !grantedFlash && !context.canPlaySorcerySpeed) continue

            // Check additional cost payability
            val additionalCosts = cardDef.script.additionalCosts
            val sacrificeTargets = mutableListOf<EntityId>()
            var variableSacrificeTargets = emptyList<EntityId>()
            var variableSacrificeReduction = 0
            var exileTargets = emptyList<EntityId>()
            var exileMinCount = 0
            var discardTargets = emptyList<EntityId>()
            var discardCount = 0
            var bounceTargets = emptyList<EntityId>()
            var bounceCount = 0
            var tapTargets = emptyList<EntityId>()
            var tapCount = 0
            var beholdTargets = emptyList<EntityId>()
            var beholdCount = 0
            var blightOrPayCost: AdditionalCost.BlightOrPay? = null
            var blightCreatures = emptyList<EntityId>()
            var blightVariableCost: AdditionalCost.BlightVariable? = null
            var blightVariableCreatures = emptyList<EntityId>()
            var blightVariableMaxX = 0
            var payXLifeCost: AdditionalCost.PayXLife? = null
            var payXLifeMaxX = 0
            var beholdOrPayCost: AdditionalCost.BeholdOrPay? = null
            var beholdOrPayTargets = emptyList<EntityId>()
            var exileOrPayCost: AdditionalCost.ExileFromGraveyardOrPay? = null
            var exileOrPayTargets = emptyList<EntityId>()
            var sacOrPayCost: AdditionalCost.SacrificeOrPay? = null
            var sacOrPayTargets = emptyList<EntityId>()
            var canPayAdditionalCosts = true
            val flattenedCosts = additionalCosts.flatMap {
                if (it is AdditionalCost.Composite) it.steps else listOf(it)
            }
            for (cost in flattenedCosts) {
                when (cost) {
                    is AdditionalCost.Atom -> when (val atom = cost.atom) {
                        is CostAtom.Sacrifice -> {
                            val validSacTargets = context.costUtils.findSacrificeTargets(state, playerId, atom)
                            if (validSacTargets.size < atom.count) {
                                canPayAdditionalCosts = false
                            }
                            sacrificeTargets.addAll(validSacTargets)
                        }
                        is CostAtom.ExileFrom -> {
                            val validExileTargets = context.costUtils.findExileTargets(state, playerId, atom.filter, atom.zone)
                            if (validExileTargets.size < atom.count) {
                                canPayAdditionalCosts = false
                            }
                            exileTargets = validExileTargets
                            exileMinCount = atom.count
                        }
                        is CostAtom.Discard -> {
                            val handZone = ZoneKey(playerId, Zone.HAND)
                            val handCards = state.getZone(handZone)
                                .filter { it != cardId } // Exclude the card being cast
                            val predicateContext = PredicateContext(controllerId = playerId)
                            val validDiscards = if (atom.filter == com.wingedsheep.sdk.scripting.GameObjectFilter.Any) {
                                handCards
                            } else {
                                handCards.filter { context.predicateEvaluator.matches(state, state.projectedState, it, atom.filter, predicateContext) }
                            }
                            if (validDiscards.size < atom.count) {
                                canPayAdditionalCosts = false
                            }
                            discardTargets = validDiscards
                            discardCount = atom.count
                        }
                        is CostAtom.ReturnToHand -> {
                            // "As an additional cost to cast this spell, return [count]
                            // permanent(s) matching [filter] you control to its owner's hand."
                            // Mirrors the TapPermanents selection model (permanents you control,
                            // no destruction), but the payment bounces instead of tapping.
                            val validBounceTargets = context.costUtils.findAbilityBounceTargets(state, playerId, atom.filter)
                            if (validBounceTargets.size < atom.count) {
                                canPayAdditionalCosts = false
                            }
                            bounceTargets = validBounceTargets
                            bounceCount = atom.count
                        }
                        is CostAtom.TapPermanents -> {
                            // "As an additional cost to cast this spell, tap [count] untapped
                            // [filter] you control" (e.g. Guardian of the Great Door). Mirrors the
                            // ReturnToHand selection model above — permanents you control, chosen by
                            // the caster — but the payment taps instead of bouncing. Without this
                            // case the cost fell through to `else -> {}`, so no `AdditionalCostData`
                            // reached the client: it couldn't prompt for the tap, yet the cast-time
                            // validator still rejected the empty payment.
                            val validTapTargets = context.costUtils.findAbilityTapTargets(state, playerId, atom.filter)
                                .let { if (atom.excludeSelf) it.filter { id -> id != cardId } else it }
                            if (validTapTargets.size < atom.count) {
                                canPayAdditionalCosts = false
                            }
                            tapTargets = validTapTargets
                            tapCount = atom.count
                        }
                        // Mana / reveal aren't produced as spell additional costs today.
                        else -> {}
                    }
                    is AdditionalCost.SacrificeCreaturesForCostReduction -> {
                        // Always payable (0 sacrifices is valid)
                        val validSacTargets = context.costUtils.findVariableSacrificeTargets(state, playerId, cost.filter)
                        variableSacrificeTargets = validSacTargets
                        variableSacrificeReduction = cost.costReductionPerCreature
                    }
                    is AdditionalCost.ExileVariableCards -> {
                        val validExileTargets = context.costUtils.findExileTargets(state, playerId, cost.filter, cost.fromZone.toZone())
                        if (validExileTargets.size < cost.minCount) {
                            canPayAdditionalCosts = false
                        }
                        exileTargets = validExileTargets
                        exileMinCount = cost.minCount
                    }
                    is AdditionalCost.Behold -> {
                        // Find matching permanents on battlefield (projected) + matching cards in hand
                        val projected = state.projectedState
                        val predicateContext = PredicateContext(controllerId = playerId)
                        val battlefieldMatches = projected.getBattlefieldControlledBy(playerId).filter { permId ->
                            context.predicateEvaluator.matches(state, projected, permId, cost.filter, predicateContext)
                        }
                        val handZone = ZoneKey(playerId, Zone.HAND)
                        val handMatches = state.getZone(handZone)
                            .filter { it != cardId } // Exclude the card being cast
                            .filter { context.predicateEvaluator.matches(state, state.projectedState, it, cost.filter, predicateContext) }
                        val allTargets = battlefieldMatches + handMatches
                        if (allTargets.size < cost.count) {
                            canPayAdditionalCosts = false
                        }
                        beholdTargets = allTargets
                        beholdCount = cost.count
                    }
                    is AdditionalCost.ExileFromStorage -> {
                        // Payability determined by the preceding Behold cost
                    }
                    is AdditionalCost.BlightOrPay -> {
                        // Always payable: player can always choose the "pay mana" path
                        // Find creatures for the blight path
                        blightOrPayCost = cost
                        val projected = state.projectedState
                        blightCreatures = projected.getBattlefieldControlledBy(playerId)
                            .filter { projected.isCreature(it) && projected.canReceiveCounters(it) }
                    }
                    is AdditionalCost.BlightVariable -> {
                        // Always payable when minCount = 0 (X = 0 is valid even with no
                        // creatures). Surface the creature pool + cap so the client can
                        // prompt the player for X and a creature.
                        val projected = state.projectedState
                        val ownCreatures = projected.getBattlefieldControlledBy(playerId)
                            .filter { projected.isCreature(it) && projected.canReceiveCounters(it) }
                        val maxToughness = ownCreatures.maxOfOrNull { projected.getToughness(it) ?: 0 } ?: 0
                        if (maxToughness < cost.minCount) {
                            canPayAdditionalCosts = false
                        }
                        blightVariableCost = cost
                        blightVariableCreatures = ownCreatures
                        blightVariableMaxX = maxToughness
                    }
                    is AdditionalCost.PayXLife -> {
                        // Always payable when minCount = 0 (X = 0 is valid). Surface the cap (current
                        // life total) so the client can bound the X slider (0..payXLifeMaxX).
                        val currentLife = state.lifeTotal(playerId)
                        if (currentLife < cost.minCount) {
                            canPayAdditionalCosts = false
                        }
                        payXLifeCost = cost
                        payXLifeMaxX = currentLife
                    }
                    is AdditionalCost.BeholdOrPay -> {
                        // Always payable: player can always choose the "pay mana" path
                        // Find valid behold targets (battlefield permanents + hand cards matching filter)
                        beholdOrPayCost = cost
                        val projected = state.projectedState
                        val predicateContext = PredicateContext(controllerId = playerId)
                        val battlefieldMatches = projected.getBattlefieldControlledBy(playerId).filter { permId ->
                            context.predicateEvaluator.matches(state, projected, permId, cost.filter, predicateContext)
                        }
                        val handZone = ZoneKey(playerId, Zone.HAND)
                        val handMatches = state.getZone(handZone)
                            .filter { it != cardId } // Exclude the card being cast
                            .filter { context.predicateEvaluator.matches(state, state.projectedState, it, cost.filter, predicateContext) }
                        beholdOrPayTargets = battlefieldMatches + handMatches
                    }
                    is AdditionalCost.ExileFromGraveyardOrPay -> {
                        // Always payable: player can always choose the "pay mana" path.
                        // Surface the graveyard cards eligible for the exile path.
                        exileOrPayCost = cost
                        exileOrPayTargets = context.costUtils.findExileTargets(
                            state, playerId, cost.filter, Zone.GRAVEYARD
                        )
                    }
                    is AdditionalCost.SacrificeOrPay -> {
                        // Always payable: player can always choose the "pay mana" path.
                        // Surface the permanents you control eligible for the sacrifice path.
                        sacOrPayCost = cost
                        sacOrPayTargets = context.costUtils.findSacrificeTargets(
                            state, playerId, CostAtom.Sacrifice(cost.filter, cost.count)
                        )
                    }
                    is AdditionalCost.ChooseEntity -> {
                        // Search each (zone, filter) pair in `cost.zoneFilters`. Battlefield
                        // uses projected state (continuous effects matter); hidden / card
                        // zones use base state, mirroring the Behold convention.
                        val projected = state.projectedState
                        val predicateContext = PredicateContext(controllerId = playerId)
                        val allTargets = cost.zoneFilters.flatMap { (zone, filter) ->
                            when (zone) {
                                Zone.BATTLEFIELD -> projected.getBattlefieldControlledBy(playerId)
                                    .filter {
                                        context.predicateEvaluator.matches(
                                            state, projected, it, filter, predicateContext
                                        )
                                    }
                                else -> state.getZone(ZoneKey(playerId, zone))
                                    .filter { it != cardId } // exclude the spell being cast
                                    .filter {
                                        context.predicateEvaluator.matches(
                                            state, state.projectedState, it, filter, predicateContext
                                        )
                                    }
                            }
                        }
                        if (allTargets.isEmpty()) {
                            canPayAdditionalCosts = false
                        }
                        beholdTargets = allTargets
                        beholdCount = 1
                    }
                    else -> {}
                }
            }
            if (!canPayAdditionalCosts) continue

            // Calculate effective cost after reductions (e.g., Goblin Warchief).
            // Uses minimum possible cost so target-conditional reductions (e.g., Dire Downdraft)
            // mark the spell as castable when a valid discounted target exists on the battlefield.
            var effectiveCost = context.costCalculator.calculateMinPossibleCost(state, cardDef, playerId)

            // Apply maximum possible sacrifice cost reduction for affordability check
            if (variableSacrificeTargets.isNotEmpty() && variableSacrificeReduction > 0) {
                val maxReduction = variableSacrificeTargets.size * variableSacrificeReduction
                effectiveCost = effectiveCost.reduceGeneric(maxReduction)
            }

            // Save base cost for blight path, then add extra mana for the "pay" path
            val blightBaseCost = effectiveCost
            if (blightOrPayCost != null) {
                effectiveCost = effectiveCost + ManaCost.parse(blightOrPayCost.alternativeManaCost)
            }

            // Save base cost for behold path, then add extra mana for the "pay" path
            val beholdBaseCost = effectiveCost
            if (beholdOrPayCost != null) {
                effectiveCost = effectiveCost + ManaCost.parse(beholdOrPayCost.alternativeManaCost)
            }

            // Save base cost for exile-from-graveyard path, then add extra mana for the "pay" path
            val exileOrPayBaseCost = effectiveCost
            if (exileOrPayCost != null) {
                effectiveCost = effectiveCost + ManaCost.parse(exileOrPayCost.alternativeManaCost)
            }

            // Save base cost for sacrifice path, then add extra mana for the "pay" path
            val sacOrPayBaseCost = effectiveCost
            if (sacOrPayCost != null) {
                effectiveCost = effectiveCost + ManaCost.parse(sacOrPayCost.alternativeManaCost)
            }

            // Spell-level waterbend additional cost (Avatar: The Last Airbender). A *mandatory*
            // waterbend amount is always part of the cost: a fixed {N}, or a literal {X} for the
            // "waterbend {X}" shape (Crashing Wave, Foggy Swamp Visions) — the spell carries no
            // printed {X}, so the waterbend cost is what makes it X-carrying. The tap-to-help
            // reduction is reflected in the affordability check below (and capped at N client-side).
            val spellWaterbend = cardDef.script.spellWaterbend
            val mandatoryWaterbend = spellWaterbend != null && !spellWaterbend.optional
            val waterbendPermanents = if (spellWaterbend != null) {
                context.costUtils.findWaterbendPermanents(state, playerId)
            } else emptyList()
            if (mandatoryWaterbend) {
                effectiveCost = effectiveCost + if (spellWaterbend!!.isX) {
                    ManaCost.parse("{X}")
                } else {
                    ManaCost.parse("{${spellWaterbend.amount}}")
                }
            }

            // Check mana affordability (including Convoke/Delve if available).
            // Convoke and Delve can be printed on the card or granted at runtime by a
            // battlefield permanent (e.g., Eirdu's "Creature spells you cast have convoke.").
            val hasConvoke = context.grantedKeywordResolver.hasKeyword(state, playerId, cardDef, Keyword.CONVOKE)
            val convokeCreatures = if (hasConvoke) {
                context.costUtils.findConvokeCreatures(state, playerId)
            } else null

            val hasDelve = context.grantedKeywordResolver.hasKeyword(state, playerId, cardDef, Keyword.DELVE)
            val delveCards = if (hasDelve) {
                context.costUtils.findDelveCards(state, playerId)
            } else null
            val minDelveNeeded = if (hasDelve && delveCards != null && delveCards.isNotEmpty()) {
                context.costUtils.calculateMinDelveNeeded(state, playerId, effectiveCost, delveCards, precomputedSources = context.availableManaSources)
            } else null

            // Build spell context for conditional mana restriction awareness
            val spellContext = SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = false,
                isCreature = cardComponent.typeLine.isCreature,
                isLegendary = cardComponent.typeLine.isLegendary,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX,
                subtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet(),
                cardTypes = cardComponent.typeLine.cardTypes,
            )

            // For Convoke/Delve spells, check if affordable with alternative payment help
            val cachedSources = context.availableManaSources
            val canAfford = if (hasConvoke && convokeCreatures != null && convokeCreatures.isNotEmpty()) {
                context.manaSolver.canPay(state, playerId, effectiveCost, spellContext = spellContext, precomputedSources = cachedSources) ||
                    context.costUtils.canAffordWithConvoke(state, playerId, effectiveCost, convokeCreatures, precomputedSources = cachedSources)
            } else if (hasDelve && delveCards != null && delveCards.isNotEmpty()) {
                context.manaSolver.canPay(state, playerId, effectiveCost, spellContext = spellContext, precomputedSources = cachedSources) ||
                    context.costUtils.canAffordWithDelve(state, playerId, effectiveCost, delveCards, precomputedSources = cachedSources)
            } else if (mandatoryWaterbend) {
                // effectiveCost already includes the mandatory waterbend {N}; taps can cover up to {N}.
                context.manaSolver.canPay(state, playerId, effectiveCost, spellContext = spellContext, precomputedSources = cachedSources) ||
                    context.costUtils.canAffordWithWaterbend(
                        state, playerId, effectiveCost,
                        waterbendPermanents.take(spellWaterbend!!.amount),
                        precomputedSources = cachedSources
                    )
            } else {
                context.manaSolver.canPay(state, playerId, effectiveCost, spellContext = spellContext, precomputedSources = cachedSources)
            }

            // Check alternative casting cost affordability (e.g., Jodah's {W}{U}{B}{R}{G})
            val canAffordAlternative = context.alternativeCastingCosts.isNotEmpty() &&
                context.alternativeCastingCosts.any { altCost ->
                    val altEffective = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altCost)
                    context.manaSolver.canPay(state, playerId, altEffective, precomputedSources = cachedSources)
                }

            // Check self-alternative cost (e.g., Zahid's {3}{U} + tap an artifact)
            val selfAltCost = cardDef.script.selfAlternativeCost
            val canAffordSelfAlternative = if (selfAltCost != null) {
                val selfAltMana = selfAltCost.manaCost
                val selfAltEffective = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, selfAltMana, playerId)
                val canPayMana = context.manaSolver.canPay(state, playerId, selfAltEffective, precomputedSources = cachedSources)
                val canPayAdditional = selfAltCost.additionalCosts.all { cost ->
                    when (val atom = (cost as? AdditionalCost.Atom)?.atom) {
                        is CostAtom.TapPermanents -> {
                            context.costUtils.findAbilityTapTargets(state, playerId, atom.filter).size >= atom.count
                        }
                        else -> true
                    }
                }
                canPayMana && canPayAdditional
            } else false

            // Check evoke cost (alternative cost from Evoke keyword)
            val evokeAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Evoke>().firstOrNull()
            val canAffordEvoke = if (evokeAbility != null) {
                val evokeMana = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, evokeAbility.cost, playerId)
                context.manaSolver.canPay(state, playerId, evokeMana, precomputedSources = cachedSources)
            } else false

            // Check impending cost (alternative cost from Impending keyword)
            val impendingAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Impending>().firstOrNull()
            val canAffordImpending = if (impendingAbility != null) {
                val impendingMana = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, impendingAbility.cost, playerId)
                context.manaSolver.canPay(state, playerId, impendingMana, precomputedSources = cachedSources)
            } else false

            // Check cleave cost (CR 702.148 — alternative cost from the Cleave keyword). Emitted as
            // its own legal action below since paying it swaps in the brackets-removed target set.
            val cleaveAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Cleave>().firstOrNull()
            val canAffordCleave = if (cleaveAbility != null) {
                val cleaveMana = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, cleaveAbility.cost, playerId)
                context.manaSolver.canPay(state, playerId, cleaveMana, precomputedSources = cachedSources)
            } else false

            // Check blight path affordability (base cost without the extra mana, but needs a creature)
            val canAffordBlightPath = if (blightOrPayCost != null && blightCreatures.isNotEmpty()) {
                context.manaSolver.canPay(state, playerId, blightBaseCost, spellContext = spellContext, precomputedSources = cachedSources)
            } else false

            // Check behold path affordability (base cost without the extra mana, but needs a beholdable target)
            val canAffordBeholdPath = if (beholdOrPayCost != null && beholdOrPayTargets.isNotEmpty()) {
                context.manaSolver.canPay(state, playerId, beholdBaseCost, spellContext = spellContext, precomputedSources = cachedSources)
            } else false

            // Check exile-from-graveyard path affordability (base cost without the extra mana, but
            // needs enough matching cards in the graveyard to exile).
            val canAffordExileOrPayPath = if (exileOrPayCost != null && exileOrPayTargets.size >= exileOrPayCost.exileCount) {
                context.manaSolver.canPay(state, playerId, exileOrPayBaseCost, spellContext = spellContext, precomputedSources = cachedSources)
            } else false

            // Check sacrifice path affordability (base cost without the extra mana, but needs
            // enough matching permanents on the battlefield to sacrifice).
            val canAffordSacOrPayPath = if (sacOrPayCost != null && sacOrPayTargets.size >= sacOrPayCost.count) {
                context.manaSolver.canPay(state, playerId, sacOrPayBaseCost, spellContext = spellContext, precomputedSources = cachedSources)
            } else false

            // A `MayCastWithoutPayingManaCost` battlefield permission (e.g. Weftwalking) makes the
            // spell affordable for {0} when its gates are open. Emitted by its own branch below;
            // don't continue out before reaching it.
            val canAffordFreeCast = context.freeCastPermissionFor(cardId)
            if (!canAfford && !canAffordAlternative && !canAffordSelfAlternative && !canAffordEvoke && !canAffordImpending && !canAffordCleave && !canAffordBlightPath && !canAffordBeholdPath && !canAffordExileOrPayPath && !canAffordSacOrPayPath && !canAffordFreeCast) {
                // The primary face can't be paid for by any path. Normally we skip it entirely.
                // But if this is an Adventure/Omen/modal-DFC card whose *secondary* face is
                // affordable, surface a grayed-out placeholder for the primary face so the
                // drag-to-play menu shows both faces — the player picks the affordable one or
                // cancels, instead of the menu silently auto-firing the only enumerated option.
                if (secondaryFaceAffordable) {
                    result.add(LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name}",
                        action = CastSpell(playerId, cardId),
                        affordable = false,
                        manaCostString = effectiveCost.toString(),
                    ))
                }
                continue
            }

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.auraTarget?.let { add(it) }
            }

            // Build additional cost info for the client
            val costInfo = buildAdditionalCostData(
                additionalCosts, sacrificeTargets, variableSacrificeTargets,
                exileTargets, exileMinCount, discardTargets, discardCount,
                bounceTargets, bounceCount,
                tapTargets, tapCount,
                beholdTargets, beholdCount,
                blightVariableCost, blightVariableCreatures, blightVariableMaxX,
                payXLifeCost, payXLifeMaxX
            )

            // Compute blight path info (separate legal action with lower mana cost + blight target selection)
            val blightPathInfo = if (canAffordBlightPath && blightOrPayCost != null) {
                val blightManaCostString = blightBaseCost.toString()
                val blightAutoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, blightBaseCost, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                val blightCostInfo = AdditionalCostData(
                    description = "creature to blight",
                    costType = "Blight",
                    validBlightTargets = blightCreatures,
                    blightAmount = blightOrPayCost.blightAmount
                )
                Triple(blightManaCostString, blightAutoTapPreview, blightCostInfo)
            } else null

            // Compute behold path info (separate legal action with lower mana cost + behold target selection)
            val beholdPathInfo = if (canAffordBeholdPath && beholdOrPayCost != null) {
                val beholdManaCostString = beholdBaseCost.toString()
                val beholdAutoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, beholdBaseCost, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                val beholdCostInfo = AdditionalCostData(
                    description = beholdOrPayCost.description,
                    costType = "Behold",
                    validBeholdTargets = beholdOrPayTargets,
                    beholdCount = 1
                )
                Triple(beholdManaCostString, beholdAutoTapPreview, beholdCostInfo)
            } else null

            // Compute exile-from-graveyard path info (separate legal action with lower mana cost +
            // exile card selection). The player exiles exactly `exileCount` matching graveyard cards.
            val exileOrPayPathInfo = if (canAffordExileOrPayPath && exileOrPayCost != null) {
                val exileManaCostString = exileOrPayBaseCost.toString()
                val exileAutoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, exileOrPayBaseCost, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                val exileCostInfo = AdditionalCostData(
                    description = "Exile ${exileOrPayCost.exileCount} card(s) from your graveyard",
                    costType = "ExileFromGraveyard",
                    validExileTargets = exileOrPayTargets,
                    exileMinCount = exileOrPayCost.exileCount,
                    exileMaxCount = exileOrPayCost.exileCount,
                )
                Triple(exileManaCostString, exileAutoTapPreview, exileCostInfo)
            } else null

            // Compute sacrifice path info (separate legal action with lower mana cost + permanent
            // selection). Reuses the "SacrificePermanent" cost type so the client drives the same
            // on-battlefield sacrifice selection used by Natural Order's plain sacrifice cost.
            val sacOrPayPathInfo = if (canAffordSacOrPayPath && sacOrPayCost != null) {
                val sacManaCostString = sacOrPayBaseCost.toString()
                val sacAutoTapPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, sacOrPayBaseCost, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                val sacCostInfo = AdditionalCostData(
                    description = sacOrPayCost.description,
                    costType = "SacrificePermanent",
                    validSacrificeTargets = sacOrPayTargets,
                    sacrificeCount = sacOrPayCost.count,
                )
                Triple(sacManaCostString, sacAutoTapPreview, sacCostInfo)
            } else null

            // Calculate X cost info if the spell has X in its cost (printed, or the waterbend {X}
            // folded in above).
            val hasXCost = effectiveCost.hasX
            val maxAffordableX: Int? = if (hasXCost) {
                // Pass the spell context so floating restricted mana this spell may spend
                // (e.g. "only to cast instant and sorcery spells") raises the X ceiling.
                val availableSources = context.manaSolver.getAvailableManaCount(state, playerId, precomputedSources = cachedSources, spellContext = spellContext)
                // Each delve card pays for one generic mana, so it raises the
                // X ceiling exactly like an additional mana source would.
                val delveAvailable = if (hasDelve && delveCards != null) delveCards.size else 0
                // For waterbend {X}, each tappable artifact/creature pays {1} of the X generic, so
                // it raises the X ceiling like an extra mana source.
                val waterbendAvailable = if (spellWaterbend?.isX == true) waterbendPermanents.size else 0
                val fixedCost = effectiveCost.cmc  // X contributes 0 to CMC
                val xSymbolCount = effectiveCost.xCount.coerceAtLeast(1)
                ((availableSources + delveAvailable + waterbendAvailable - fixedCost) / xSymbolCount).coerceAtLeast(0)
            } else null

            // Always include mana cost string for cast actions
            val manaCostString = effectiveCost.toString()

            // Compute auto-tap preview for UI highlighting (skipped in ACTIONS_ONLY mode).
            //
            // The solver runs against the worst-case *remaining* cost the player can be
            // on the hook for after a legal delve choice — i.e. the full effective cost
            // minus the minimum delve reduction that makes the spell affordable
            // (`minDelveNeeded`, 0 when the spell is affordable without any delve at all).
            // That gives an exact solve for the largest land set any legal cast might
            // need; the client's `trimAutoTapPreview` prunes the list as the player
            // delves past the minimum. The engine re-solves on submit anyway —
            // `CastPaymentProcessor.explicitPay` taps only the minimum subset needed
            // after the alt-payment reduction is actually applied.
            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                val costForPreview = if (hasDelve && minDelveNeeded != null && minDelveNeeded > 0) {
                    effectiveCost.reduceGeneric(minDelveNeeded)
                } else {
                    effectiveCost
                }
                context.manaSolver.solve(state, playerId, costForPreview, precomputedSources = cachedSources)
                    ?.sources?.map { it.entityId }
            }

            // Check for DividedDamageEffect to flag damage distribution requirement
            val spellEffect = cardDef.script.spellEffect
            val dividedDamageEffect = spellEffect as? DividedDamageEffect
            val requiresDamageDistribution = dividedDamageEffect != null
            val totalDamageToDistribute = dividedDamageEffect?.totalDamage
            val minDamagePerTarget = if (dividedDamageEffect != null) 1 else null

            // Compute alternative cost info for this spell (Jodah-style GrantAlternativeCastingCost).
            val altCostInfo = if (canAffordAlternative) {
                val altCost = context.alternativeCastingCosts.first()
                val altEffective = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altCost)
                val altPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, altEffective, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                Triple(altEffective.toString(), altPreview, context.manaSolver.canPay(state, playerId, altEffective, precomputedSources = cachedSources))
            } else null

            // Compute self-alternative cost info (e.g., Zahid)
            val selfAltCostResult = if (canAffordSelfAlternative && selfAltCost != null) {
                val selfAltMana = selfAltCost.manaCost
                val selfAltEffective = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, selfAltMana, playerId)
                val selfAltPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, selfAltEffective, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                val tapCost = selfAltCost.additionalCosts.firstNotNullOfOrNull { (it as? AdditionalCost.Atom)?.atom as? CostAtom.TapPermanents }
                val tapTargets = if (tapCost != null) context.costUtils.findAbilityTapTargets(state, playerId, tapCost.filter) else null
                val addlCostInfo = if (tapTargets != null && tapCost != null) {
                    AdditionalCostData(
                        description = tapCost.description,
                        costType = "TapPermanents",
                        validTapTargets = tapTargets,
                        tapCount = tapCost.count
                    )
                } else null
                SelfAltCostResult(
                    manaCostString = selfAltEffective.toString(),
                    autoTapPreview = selfAltPreview,
                    additionalCostInfo = addlCostInfo
                )
            } else null

            // Compute evoke cost info
            val evokeCostResult = if (canAffordEvoke && evokeAbility != null) {
                val evokeMana = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, evokeAbility.cost, playerId)
                val evokePreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, evokeMana, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                SelfAltCostResult(
                    manaCostString = evokeMana.toString(),
                    autoTapPreview = evokePreview,
                    additionalCostInfo = null
                )
            } else null

            // Compute impending cost info
            val impendingCostResult = if (canAffordImpending && impendingAbility != null) {
                val impendingMana = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, impendingAbility.cost, playerId)
                val impendingPreview = if (context.skipAutoTapPreview) null else {
                    context.manaSolver.solve(state, playerId, impendingMana, precomputedSources = cachedSources)
                        ?.sources?.map { it.entityId }
                }
                SelfAltCostResult(
                    manaCostString = impendingMana.toString(),
                    autoTapPreview = impendingPreview,
                    additionalCostInfo = null
                )
            } else null

            // Compute miracle cost info (CR 702.94). Only offered while this card carries an open
            // miracle window (it was the first card drawn this turn and has miracle, printed or
            // granted in hand). The window component is set by the draw flow and cleared at cleanup.
            val miracleWindowOpen = state.getEntity(cardId)
                ?.has<com.wingedsheep.engine.state.components.identity.MiracleWindowComponent>() == true
            val miracleAbility = if (miracleWindowOpen) {
                com.wingedsheep.engine.mechanics.MiracleGrants.effectiveMiracle(
                    state, cardId, cardDef, playerId, context.cardRegistry, context.predicateEvaluator
                )
            } else null
            val miracleCostResult = if (miracleAbility != null) {
                val miracleMana = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, miracleAbility.cost, playerId)
                if (context.manaSolver.canPay(state, playerId, miracleMana, precomputedSources = cachedSources)) {
                    val miraclePreview = if (context.skipAutoTapPreview) null else {
                        context.manaSolver.solve(state, playerId, miracleMana, precomputedSources = cachedSources)
                            ?.sources?.map { it.entityId }
                    }
                    SelfAltCostResult(
                        manaCostString = miracleMana.toString(),
                        autoTapPreview = miraclePreview,
                        additionalCostInfo = null
                    )
                } else null
            } else null

            // Free cast via `MayCastWithoutPayingManaCost` (e.g. Weftwalking) — its own variant,
            // parallel to [altCostInfo], so the player can pick it over Jodah-style
            // `GrantAlternativeCastingCost` and over any keyword alt (flashback, harmonize, warp,
            // evoke, impending) when both are legal (CR 118.9a — only one alternative cost may
            // apply to a cast, and which one is the player's choice). Routes through
            // [CastSpell.useWithoutPayingManaCost].
            val freeCastResult = if (context.freeCastPermissionFor(cardId)) {
                SelfAltCostResult(
                    manaCostString = "{0}",
                    autoTapPreview = if (context.skipAutoTapPreview) null else emptyList(),
                    additionalCostInfo = null
                )
            } else null

            // Modal spells: choose-1 emits one LegalAction per mode so the opponent
            // sees which mode was picked on the stack. Choose-N emits a single
            // CastSpellModal action with a [ModalLegalEnumeration] payload and lets
            // the client drive the cast-time mode/target decision loop (rules 601.2b–c,
            // 700.2a). Choose-N cartesian enumeration would blow up for allowRepeat
            // (Escalate/Spree) and for wide target pools.
            val modalEffect = spellEffect as? ModalEffect
            // Build the cast variants we'll emit. For modal spells with
            // `chooseAllIfBlightPaid` + `BlightOrPay`, the pay path locks in the
            // printed `minChooseCount` (typically "choose one") while the blight
            // path forces choosing every mode — so the two paths surface as
            // separate legal actions with different mana cost / cost info.
            val modalVariants = if (modalEffect != null) {
                buildList {
                    if (canAfford) {
                        val payChooseCount = if (modalEffect.chooseAllIfBlightPaid) {
                            modalEffect.minChooseCount
                        } else {
                            modalEffect.chooseCount
                        }
                        add(
                            ModalCastVariant(
                                effect = modalEffect.copy(
                                    chooseCount = payChooseCount,
                                    minChooseCount = modalEffect.minChooseCount
                                ),
                                baseEffectiveCost = effectiveCost,
                                additionalCostInfo = costInfo,
                                manaCostString = manaCostString,
                                autoTapPreview = autoTapPreview,
                                descriptionSuffix = ""
                            )
                        )
                    }
                    if (modalEffect.chooseAllIfBlightPaid &&
                        blightOrPayCost != null &&
                        blightPathInfo != null
                    ) {
                        val all = modalEffect.modes.size
                        add(
                            ModalCastVariant(
                                effect = modalEffect.copy(
                                    chooseCount = all,
                                    minChooseCount = all
                                ),
                                baseEffectiveCost = blightBaseCost,
                                additionalCostInfo = blightPathInfo.third,
                                manaCostString = blightPathInfo.first,
                                autoTapPreview = blightPathInfo.second,
                                descriptionSuffix = " (Blight ${blightOrPayCost.blightAmount})"
                            )
                        )
                    }
                }
            } else emptyList()

            if (modalEffect != null) {
                for (variant in modalVariants) {
                val variantEffect = variant.effect
                val modeEnumerations = variantEffect.modes.mapIndexed { modeIndex, mode ->
                    computeModeEnumeration(
                        context = context,
                        cardId = cardId,
                        playerId = playerId,
                        modeIndex = modeIndex,
                        mode = mode,
                        baseEffectiveCost = variant.baseEffectiveCost,
                        cardLevelAdditionalCostInfo = variant.additionalCostInfo,
                        baseAutoTapPreview = variant.autoTapPreview,
                        spellContext = spellContext,
                        cachedSources = cachedSources
                    )
                }

                if (variantEffect.chooseCount == 1) {
                    for (modeEnum in modeEnumerations) {
                        if (!modeEnum.available) continue

                        val modeIndex = modeEnum.modeIndex
                        val mode = modeEnum.mode
                        val modeTargetReqs = mode.targetRequirements
                        val modeTargetInfos = modeEnum.targetInfos

                        if (modeTargetReqs.isNotEmpty()) {
                            val firstReq = modeTargetReqs.first()
                            val firstInfo = modeTargetInfos.first()

                            // Check for auto-select (single player target, single valid choice)
                            val canAutoSelect = modeTargetReqs.size == 1 &&
                                context.targetUtils.shouldAutoSelectPlayerTarget(firstReq, firstInfo.validTargets)

                            if (canAutoSelect) {
                                val autoTarget = ChosenTarget.Player(firstInfo.validTargets.first())
                                result.add(LegalAction(
                                    actionType = "CastSpellMode",
                                    description = mode.description + variant.descriptionSuffix,
                                    action = CastSpell(
                                        playerId,
                                        cardId,
                                        targets = listOf(autoTarget),
                                        chosenModes = listOf(modeIndex),
                                        modeTargetsOrdered = listOf(listOf(autoTarget))
                                    ),
                                    hasXCost = hasXCost,
                                    maxAffordableX = maxAffordableX,
                                    additionalCostInfo = modeEnum.additionalCostInfo,
                                    hasConvoke = hasConvoke,
                                    convokeCreatures = convokeCreatures,
                                    hasDelve = hasDelve,
                                    delveCards = delveCards,
                                    minDelveNeeded = minDelveNeeded,
                                    manaCostString = modeEnum.manaCostString,
                                    autoTapPreview = modeEnum.autoTapPreview
                                ))
                            } else {
                                result.add(LegalAction(
                                    actionType = "CastSpellMode",
                                    description = mode.description + variant.descriptionSuffix,
                                    action = CastSpell(playerId, cardId, chosenModes = listOf(modeIndex)),
                                    validTargets = firstInfo.validTargets,
                                    requiresTargets = true,
                                    targetCount = firstReq.count,
                                    minTargets = firstReq.effectiveMinCount,
                                    targetDescription = firstReq.description,
                                    targetRequirements = if (modeTargetInfos.size > 1) modeTargetInfos else null,
                                    xConstrainsTargetManaValue = firstInfo.xConstrainsManaValue,
                                    xConstrainsTargetCount = firstInfo.xConstrainsCount,
                                    hasXCost = hasXCost,
                                    maxAffordableX = maxAffordableX,
                                    additionalCostInfo = modeEnum.additionalCostInfo,
                                    hasConvoke = hasConvoke,
                                    convokeCreatures = convokeCreatures,
                                    hasDelve = hasDelve,
                                    delveCards = delveCards,
                                    minDelveNeeded = minDelveNeeded,
                                    manaCostString = modeEnum.manaCostString,
                                    autoTapPreview = modeEnum.autoTapPreview
                                ))
                            }
                        } else {
                            // Mode has no targets
                            result.add(LegalAction(
                                actionType = "CastSpellMode",
                                description = mode.description + variant.descriptionSuffix,
                                action = CastSpell(playerId, cardId, chosenModes = listOf(modeIndex)),
                                hasXCost = hasXCost,
                                maxAffordableX = maxAffordableX,
                                additionalCostInfo = modeEnum.additionalCostInfo,
                                hasConvoke = hasConvoke,
                                convokeCreatures = convokeCreatures,
                                hasDelve = hasDelve,
                                delveCards = delveCards,
                                minDelveNeeded = minDelveNeeded,
                                manaCostString = modeEnum.manaCostString,
                                autoTapPreview = modeEnum.autoTapPreview
                            ))
                        }
                    }
                } else {
                    // Choose-N (> 1): emit a single LegalAction carrying the per-mode
                    // enumeration. The client drives cast-time mode + target selection.
                    val enumerationModes = modeEnumerations.map { modeEnum ->
                        ModalEnumerationMode(
                            index = modeEnum.modeIndex,
                            description = modeEnum.mode.description,
                            available = modeEnum.available,
                            additionalManaCost = modeEnum.mode.additionalManaCost,
                            additionalCostInfo = modeEnum.additionalCostInfo,
                            targetRequirements = modeEnum.targetInfos
                        )
                    }
                    val unavailableIndices = enumerationModes
                        .filterNot { it.available }
                        .map { it.index }

                    // If every mode is unavailable, the spell can't legally be cast —
                    // drop the action entirely rather than offering an unplayable UI.
                    val hasAnyAvailable = enumerationModes.any { it.available }
                    if (hasAnyAvailable) {
                        result.add(LegalAction(
                            actionType = "CastSpellModal",
                            description = "Cast ${cardComponent.name}${variant.descriptionSuffix}",
                            action = CastSpell(playerId, cardId),
                            hasXCost = hasXCost,
                            maxAffordableX = maxAffordableX,
                            additionalCostInfo = variant.additionalCostInfo,
                            hasConvoke = hasConvoke,
                            convokeCreatures = convokeCreatures,
                            hasDelve = hasDelve,
                            delveCards = delveCards,
                            minDelveNeeded = minDelveNeeded,
                            manaCostString = variant.manaCostString,
                            autoTapPreview = variant.autoTapPreview,
                            modalEnumeration = ModalLegalEnumeration(
                                chooseCount = variantEffect.chooseCount,
                                minChooseCount = variantEffect.minChooseCount,
                                allowRepeat = variantEffect.allowRepeat,
                                modes = enumerationModes,
                                unavailableIndices = unavailableIndices
                            )
                        ))
                    }
                }
                }
                // Skip the normal targeting logic for modal spells
            } else if (targetReqs.isNotEmpty()) {
                // Spell requires targets - find valid targets for all requirements
                val targetReqInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)

                // Check if all requirements can be satisfied
                val allRequirementsSatisfied = context.targetUtils.allRequirementsSatisfied(targetReqInfos)

                val firstReq = targetReqs.first()
                val firstReqInfo = targetReqInfos.first()

                // Only add the action if all requirements can be satisfied
                if (allRequirementsSatisfied) {
                    // Check if we can auto-select player targets (single target, single valid choice)
                    val canAutoSelect = targetReqs.size == 1 &&
                        context.targetUtils.shouldAutoSelectPlayerTarget(firstReq, firstReqInfo.validTargets)

                    if (canAutoSelect) {
                        // Auto-select the single valid player target
                        val autoSelectedTarget = ChosenTarget.Player(firstReqInfo.validTargets.first())
                        if (canAfford) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget)),
                                hasXCost = hasXCost,
                                maxAffordableX = maxAffordableX,
                                additionalCostInfo = costInfo,
                                hasConvoke = hasConvoke,
                                convokeCreatures = convokeCreatures,
                                hasDelve = hasDelve,
                                delveCards = delveCards,
                                minDelveNeeded = minDelveNeeded,
                                manaCostString = manaCostString,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = autoTapPreview
                            ))
                        }
                        if (altCostInfo?.third == true) {
                            result.add(LegalAction(
                                actionType = "CastWithAlternativeCost",
                                description = "Cast ${cardComponent.name} (${altCostInfo.first})",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), useAlternativeCost = true, alternativeCostType = AlternativeCostType.GRANTED),
                                manaCostString = altCostInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = altCostInfo.second
                            ))
                        }
                        if (selfAltCostResult != null) {
                            result.add(LegalAction(
                                actionType = "CastWithAlternativeCost",
                                description = "Cast ${cardComponent.name} (${selfAltCostResult.manaCostString})",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE),
                                manaCostString = selfAltCostResult.manaCostString,
                                additionalCostInfo = selfAltCostResult.additionalCostInfo,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = selfAltCostResult.autoTapPreview
                            ))
                        }
                        if (evokeCostResult != null) {
                            result.add(LegalAction(
                                actionType = "CastWithAlternativeCost",
                                description = "Evoke ${cardComponent.name} (${evokeCostResult.manaCostString})",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), useAlternativeCost = true, alternativeCostType = AlternativeCostType.EVOKE),
                                manaCostString = evokeCostResult.manaCostString,
                                autoTapPreview = evokeCostResult.autoTapPreview
                            ))
                        }
                        if (impendingCostResult != null) {
                            result.add(LegalAction(
                                actionType = "CastWithAlternativeCost",
                                description = "Impending ${cardComponent.name} (${impendingCostResult.manaCostString})",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), useAlternativeCost = true, alternativeCostType = AlternativeCostType.IMPENDING),
                                manaCostString = impendingCostResult.manaCostString,
                                autoTapPreview = impendingCostResult.autoTapPreview
                            ))
                        }
                        if (miracleCostResult != null) {
                            result.add(LegalAction(
                                actionType = "CastWithAlternativeCost",
                                description = "Miracle ${cardComponent.name} (${miracleCostResult.manaCostString})",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), useAlternativeCost = true, alternativeCostType = AlternativeCostType.MIRACLE),
                                manaCostString = miracleCostResult.manaCostString,
                                autoTapPreview = miracleCostResult.autoTapPreview
                            ))
                        }
                        if (freeCastResult != null) {
                            result.add(LegalAction(
                                actionType = "CastWithoutPayingManaCost",
                                description = "Cast ${cardComponent.name} (Free)",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), useWithoutPayingManaCost = true),
                                manaCostString = freeCastResult.manaCostString,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = freeCastResult.autoTapPreview
                            ))
                        }
                        if (blightPathInfo != null) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (Blight ${blightOrPayCost!!.blightAmount})",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget)),
                                additionalCostInfo = blightPathInfo.third,
                                manaCostString = blightPathInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = blightPathInfo.second
                            ))
                        }
                        if (beholdPathInfo != null) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (Behold)",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget)),
                                additionalCostInfo = beholdPathInfo.third,
                                manaCostString = beholdPathInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = beholdPathInfo.second
                            ))
                        }
                        if (exileOrPayPathInfo != null) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (Exile from graveyard)",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget)),
                                additionalCostInfo = exileOrPayPathInfo.third,
                                manaCostString = exileOrPayPathInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = exileOrPayPathInfo.second
                            ))
                        }
                        if (sacOrPayPathInfo != null) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (Sacrifice)",
                                action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget)),
                                additionalCostInfo = sacOrPayPathInfo.third,
                                manaCostString = sacOrPayPathInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = sacOrPayPathInfo.second
                            ))
                        }
                    } else {
                        if (canAfford) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                xConstrainsTargetManaValue = firstReqInfo.xConstrainsManaValue,
                                xConstrainsTargetCount = firstReqInfo.xConstrainsCount,
                                hasXCost = hasXCost,
                                maxAffordableX = maxAffordableX,
                                additionalCostInfo = costInfo,
                                hasConvoke = hasConvoke,
                                convokeCreatures = convokeCreatures,
                                hasDelve = hasDelve,
                                delveCards = delveCards,
                                minDelveNeeded = minDelveNeeded,
                                manaCostString = manaCostString,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = autoTapPreview
                            ))
                        }
                        if (altCostInfo?.third == true) {
                            result.add(LegalAction(
                                actionType = "CastWithAlternativeCost",
                                description = "Cast ${cardComponent.name} (${altCostInfo.first})",
                                action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.GRANTED),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                xConstrainsTargetManaValue = firstReqInfo.xConstrainsManaValue,
                                xConstrainsTargetCount = firstReqInfo.xConstrainsCount,
                                manaCostString = altCostInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = altCostInfo.second
                            ))
                        }
                        if (selfAltCostResult != null) {
                            result.add(LegalAction(
                                actionType = "CastWithAlternativeCost",
                                description = "Cast ${cardComponent.name} (${selfAltCostResult.manaCostString})",
                                action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                xConstrainsTargetManaValue = firstReqInfo.xConstrainsManaValue,
                                xConstrainsTargetCount = firstReqInfo.xConstrainsCount,
                                manaCostString = selfAltCostResult.manaCostString,
                                additionalCostInfo = selfAltCostResult.additionalCostInfo,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = selfAltCostResult.autoTapPreview
                            ))
                        }
                        if (evokeCostResult != null) {
                            result.add(LegalAction(
                                actionType = "CastWithAlternativeCost",
                                description = "Evoke ${cardComponent.name} (${evokeCostResult.manaCostString})",
                                action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.EVOKE),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                xConstrainsTargetManaValue = firstReqInfo.xConstrainsManaValue,
                                xConstrainsTargetCount = firstReqInfo.xConstrainsCount,
                                manaCostString = evokeCostResult.manaCostString,
                                autoTapPreview = evokeCostResult.autoTapPreview
                            ))
                        }
                        if (impendingCostResult != null) {
                            result.add(LegalAction(
                                actionType = "CastWithAlternativeCost",
                                description = "Impending ${cardComponent.name} (${impendingCostResult.manaCostString})",
                                action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.IMPENDING),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                xConstrainsTargetManaValue = firstReqInfo.xConstrainsManaValue,
                                xConstrainsTargetCount = firstReqInfo.xConstrainsCount,
                                manaCostString = impendingCostResult.manaCostString,
                                autoTapPreview = impendingCostResult.autoTapPreview
                            ))
                        }
                        if (miracleCostResult != null) {
                            result.add(LegalAction(
                                actionType = "CastWithAlternativeCost",
                                description = "Miracle ${cardComponent.name} (${miracleCostResult.manaCostString})",
                                action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.MIRACLE),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                xConstrainsTargetManaValue = firstReqInfo.xConstrainsManaValue,
                                xConstrainsTargetCount = firstReqInfo.xConstrainsCount,
                                manaCostString = miracleCostResult.manaCostString,
                                autoTapPreview = miracleCostResult.autoTapPreview
                            ))
                        }
                        if (freeCastResult != null) {
                            result.add(LegalAction(
                                actionType = "CastWithoutPayingManaCost",
                                description = "Cast ${cardComponent.name} (Free)",
                                action = CastSpell(playerId, cardId, useWithoutPayingManaCost = true),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                xConstrainsTargetManaValue = firstReqInfo.xConstrainsManaValue,
                                xConstrainsTargetCount = firstReqInfo.xConstrainsCount,
                                manaCostString = freeCastResult.manaCostString,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = freeCastResult.autoTapPreview
                            ))
                        }
                        if (blightPathInfo != null) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (Blight ${blightOrPayCost!!.blightAmount})",
                                action = CastSpell(playerId, cardId),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                xConstrainsTargetManaValue = firstReqInfo.xConstrainsManaValue,
                                xConstrainsTargetCount = firstReqInfo.xConstrainsCount,
                                additionalCostInfo = blightPathInfo.third,
                                manaCostString = blightPathInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = blightPathInfo.second
                            ))
                        }
                        if (beholdPathInfo != null) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (Behold)",
                                action = CastSpell(playerId, cardId),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                xConstrainsTargetManaValue = firstReqInfo.xConstrainsManaValue,
                                xConstrainsTargetCount = firstReqInfo.xConstrainsCount,
                                additionalCostInfo = beholdPathInfo.third,
                                manaCostString = beholdPathInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = beholdPathInfo.second
                            ))
                        }
                        if (exileOrPayPathInfo != null) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (Exile from graveyard)",
                                action = CastSpell(playerId, cardId),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                xConstrainsTargetManaValue = firstReqInfo.xConstrainsManaValue,
                                xConstrainsTargetCount = firstReqInfo.xConstrainsCount,
                                additionalCostInfo = exileOrPayPathInfo.third,
                                manaCostString = exileOrPayPathInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = exileOrPayPathInfo.second
                            ))
                        }
                        if (sacOrPayPathInfo != null) {
                            result.add(LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name} (Sacrifice)",
                                action = CastSpell(playerId, cardId),
                                validTargets = firstReqInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                                xConstrainsTargetManaValue = firstReqInfo.xConstrainsManaValue,
                                xConstrainsTargetCount = firstReqInfo.xConstrainsCount,
                                additionalCostInfo = sacOrPayPathInfo.third,
                                manaCostString = sacOrPayPathInfo.first,
                                requiresDamageDistribution = requiresDamageDistribution,
                                totalDamageToDistribute = totalDamageToDistribute,
                                minDamagePerTarget = minDamagePerTarget,
                                autoTapPreview = sacOrPayPathInfo.second
                            ))
                        }
                    }
                }
            } else {
                // No targets required
                if (canAfford) {
                    result.add(LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name}",
                        action = CastSpell(playerId, cardId),
                        hasXCost = hasXCost,
                        maxAffordableX = maxAffordableX,
                        additionalCostInfo = costInfo,
                        hasConvoke = hasConvoke,
                        convokeCreatures = convokeCreatures,
                        hasDelve = hasDelve,
                        delveCards = delveCards,
                        minDelveNeeded = minDelveNeeded,
                        manaCostString = manaCostString,
                        autoTapPreview = autoTapPreview
                    ))
                }
                if (altCostInfo?.third == true) {
                    result.add(LegalAction(
                        actionType = "CastWithAlternativeCost",
                        description = "Cast ${cardComponent.name} (${altCostInfo.first})",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.GRANTED),
                        manaCostString = altCostInfo.first,
                        autoTapPreview = altCostInfo.second
                    ))
                }
                if (selfAltCostResult != null) {
                    result.add(LegalAction(
                        actionType = "CastWithAlternativeCost",
                        description = "Cast ${cardComponent.name} (${selfAltCostResult.manaCostString})",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.SELF_ALTERNATIVE),
                        manaCostString = selfAltCostResult.manaCostString,
                        additionalCostInfo = selfAltCostResult.additionalCostInfo,
                        autoTapPreview = selfAltCostResult.autoTapPreview
                    ))
                }
                if (evokeCostResult != null) {
                    result.add(LegalAction(
                        actionType = "CastWithAlternativeCost",
                        description = "Evoke ${cardComponent.name} (${evokeCostResult.manaCostString})",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.EVOKE),
                        manaCostString = evokeCostResult.manaCostString,
                        autoTapPreview = evokeCostResult.autoTapPreview
                    ))
                }
                if (impendingCostResult != null) {
                    result.add(LegalAction(
                        actionType = "CastWithAlternativeCost",
                        description = "Impending ${cardComponent.name} (${impendingCostResult.manaCostString})",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.IMPENDING),
                        manaCostString = impendingCostResult.manaCostString,
                        autoTapPreview = impendingCostResult.autoTapPreview
                    ))
                }
                if (miracleCostResult != null) {
                    result.add(LegalAction(
                        actionType = "CastWithAlternativeCost",
                        description = "Miracle ${cardComponent.name} (${miracleCostResult.manaCostString})",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.MIRACLE),
                        manaCostString = miracleCostResult.manaCostString,
                        autoTapPreview = miracleCostResult.autoTapPreview
                    ))
                }
                if (freeCastResult != null) {
                    result.add(LegalAction(
                        actionType = "CastWithoutPayingManaCost",
                        description = "Cast ${cardComponent.name} (Free)",
                        action = CastSpell(playerId, cardId, useWithoutPayingManaCost = true),
                        manaCostString = freeCastResult.manaCostString,
                        autoTapPreview = freeCastResult.autoTapPreview
                    ))
                }
                if (blightPathInfo != null) {
                    result.add(LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name} (Blight ${blightOrPayCost!!.blightAmount})",
                        action = CastSpell(playerId, cardId),
                        additionalCostInfo = blightPathInfo.third,
                        manaCostString = blightPathInfo.first,
                        autoTapPreview = blightPathInfo.second
                    ))
                }
                if (beholdPathInfo != null) {
                    result.add(LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name} (Behold)",
                        action = CastSpell(playerId, cardId),
                        additionalCostInfo = beholdPathInfo.third,
                        manaCostString = beholdPathInfo.first,
                        autoTapPreview = beholdPathInfo.second
                    ))
                }
                if (exileOrPayPathInfo != null) {
                    result.add(LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name} (Exile from graveyard)",
                        action = CastSpell(playerId, cardId),
                        additionalCostInfo = exileOrPayPathInfo.third,
                        manaCostString = exileOrPayPathInfo.first,
                        autoTapPreview = exileOrPayPathInfo.second
                    ))
                }
                if (sacOrPayPathInfo != null) {
                    result.add(LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name} (Sacrifice)",
                        action = CastSpell(playerId, cardId),
                        additionalCostInfo = sacOrPayPathInfo.third,
                        manaCostString = sacOrPayPathInfo.first,
                        autoTapPreview = sacOrPayPathInfo.second
                    ))
                }
            }
        }

        // --- Kicker ---
        enumerateKicker(context, hand, result)

        // --- Cleave (CR 702.148) ---
        enumerateCleave(context, hand, result)

        // --- Conspire ---
        enumerateConspire(context, hand, result)

        // --- Casualty ---
        enumerateCasualty(context, hand, result)

        return applySpellWaterbendMetadata(context, result)
    }

    /**
     * Post-process: surface the spell-level waterbend additional cost (Avatar: The Last Airbender)
     * on the cast actions already enumerated. A *mandatory* waterbend cost (whose {N} is already
     * folded into the action's cost and affordability upstream) just gains the tap metadata and the
     * `wasWaterbendPaid` flag. An *optional* "you may waterbend {N}" keeps its no-waterbend action
     * and gains a second, paid variant costing {N} more — offered only when affordable with mana
     * plus up to {N} taps. The `{X}` shape (Crashing Wave, Foggy Swamp Visions) is a *mandatory*
     * cost whose {X} is already folded into the cast action upstream (so it reads as X-carrying);
     * here it just gains the tap metadata, with [LegalAction.waterbendAmount] left null so the
     * client caps taps at the chosen X.
     */
    private fun applySpellWaterbendMetadata(
        context: EnumerationContext,
        actions: List<LegalAction>
    ): List<LegalAction> {
        val state = context.state
        val out = mutableListOf<LegalAction>()
        for (la in actions) {
            val cs = la.action as? CastSpell
            val wb = if (cs != null && la.actionType == "CastSpell") {
                val name = state.getEntity(cs.cardId)?.get<CardComponent>()?.name
                name?.let { context.cardRegistry.getCard(it) }?.script?.spellWaterbend
            } else null
            if (cs == null || wb == null) {
                out.add(la)
                continue
            }
            val perms = context.costUtils.findWaterbendPermanents(state, cs.playerId)
            // The tap cap N the client enforces: a fixed amount, or null for "waterbend {X}"
            // (the client uses the chosen xValue).
            val waterbendCap = if (wb.isX) null else wb.amount
            if (!wb.optional) {
                // Mandatory: {N}/{X} is already in la.manaCostString; attach tap metadata + paid flag.
                out.add(la.copy(
                    hasWaterbend = true,
                    waterbendPermanents = perms,
                    waterbendAmount = waterbendCap,
                    action = cs.copy(wasWaterbendPaid = true)
                ))
            } else {
                // Optional: keep the unpaid action, then add a paid variant when affordable.
                out.add(la)
                val baseCost = la.manaCostString?.let { ManaCost.parse(it) }
                if (!la.affordable || baseCost == null) continue
                val paidCost = baseCost + ManaCost.parse("{${wb.amount}}")
                val affordablePaid = context.costUtils.canAffordWithWaterbend(
                    state, cs.playerId, paidCost, perms.take(wb.amount),
                    precomputedSources = context.availableManaSources
                )
                if (!affordablePaid) continue
                out.add(la.copy(
                    description = la.description + " (waterbend {${wb.amount}})",
                    manaCostString = paidCost.toString(),
                    hasWaterbend = true,
                    waterbendPermanents = perms,
                    waterbendAmount = waterbendCap,
                    // The unpaid action's auto-tap preview was solved for the cheaper base cost;
                    // it would pre-select too few lands for the paid {base+N}. Clear it so the
                    // client recomputes the preview against the higher paid cost.
                    autoTapPreview = null,
                    action = cs.copy(wasWaterbendPaid = true)
                ))
            }
        }
        return out
    }

    /**
     * Enumerates a "Cast with Conspire" variant for each spell in hand that has Conspire
     * (printed or granted by a permanent in play via [GrantKeywordToOwnSpells]) and for which
     * the caster controls at least two untapped creatures whose projected colors overlap with
     * the spell's. The two-creature selection is submitted as [CastSpell.conspiredCreatures].
     *
     * Skip colorless spells — a color-sharing creature cannot exist for them (CR 702.78).
     */
    private fun enumerateConspire(
        context: EnumerationContext,
        hand: List<EntityId>,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId
        if (context.cantCastSpells) return

        val projected = state.projectedState

        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            if (!context.grantedKeywordResolver.hasKeyword(state, playerId, cardDef, Keyword.CONSPIRE)) continue
            // A per-spell restriction (e.g. PlayersCantCastSpells with a filter) removes the
            // conspire variant for this card even though the blanket check above passed.
            if (context.cantCastSpell(cardId)) continue

            val spellColors = cardDef.colors
            if (spellColors.isEmpty()) continue

            // Check timing (same rules as normal cast)
            val isInstant = cardComponent.typeLine.isInstant
            val hasFlash = cardDef.keywords.contains(Keyword.FLASH)
            val grantedFlash = hasFlash || context.castPermissionUtils.hasGrantedFlash(state, cardId)
            if (!isInstant && !grantedFlash && !context.canPlaySorcerySpeed) continue

            val castRestrictions = cardDef.script.castRestrictions
            if (castRestrictions.isNotEmpty() && !context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) continue

            // Gather controlled, untapped creatures that share at least one color with the spell.
            val eligibleTapTargets = mutableListOf<EntityId>()
            for (permId in state.getBattlefield()) {
                val permContainer = state.getEntity(permId) ?: continue
                if (projected.getController(permId) != playerId) continue
                if (!projected.isCreature(permId)) continue
                if (permContainer.has<com.wingedsheep.engine.state.components.battlefield.TappedComponent>()) continue
                if (spellColors.none { projected.hasColor(permId, it) }) continue
                eligibleTapTargets.add(permId)
            }
            if (eligibleTapTargets.size < 2) continue

            val baseCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
            val spellContext = SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = false,
                isCreature = cardComponent.typeLine.isCreature,
                isLegendary = cardComponent.typeLine.isLegendary,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX,
                subtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet(),
                cardTypes = cardComponent.typeLine.cardTypes,
            )
            val canAfford = context.manaSolver.canPay(state, playerId, baseCost, spellContext = spellContext, precomputedSources = context.availableManaSources)
            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, baseCost, spellContext = spellContext, precomputedSources = context.availableManaSources)
                    ?.sources?.map { it.entityId }
            }

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.auraTarget?.let { add(it) }
            }

            val conspireCostInfo = AdditionalCostData(
                description = "Tap two untapped creatures you control that share a color with this spell",
                costType = "Conspire",
                validTapTargets = eligibleTapTargets,
                tapCount = 2
            )

            if (targetReqs.isNotEmpty()) {
                val targetReqInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                val allRequirementsSatisfied = context.targetUtils.allRequirementsSatisfied(targetReqInfos)
                if (!allRequirementsSatisfied) continue
                val firstReq = targetReqs.first()
                val firstReqInfo = targetReqInfos.first()
                result.add(LegalAction(
                    actionType = "CastWithConspire",
                    description = "Cast ${cardComponent.name} (Conspire)",
                    action = CastSpell(playerId, cardId),
                    validTargets = firstReqInfo.validTargets,
                    requiresTargets = true,
                    targetCount = firstReq.count,
                    minTargets = firstReq.effectiveMinCount,
                    targetDescription = firstReq.description,
                    targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                    affordable = canAfford,
                    manaCostString = baseCost.toString(),
                    autoTapPreview = autoTapPreview,
                    additionalCostInfo = conspireCostInfo
                ))
            } else {
                result.add(LegalAction(
                    actionType = "CastWithConspire",
                    description = "Cast ${cardComponent.name} (Conspire)",
                    action = CastSpell(playerId, cardId),
                    affordable = canAfford,
                    manaCostString = baseCost.toString(),
                    autoTapPreview = autoTapPreview,
                    additionalCostInfo = conspireCostInfo
                ))
            }
        }
    }

    /**
     * Enumerates a "Cast with Casualty" variant for each spell in hand that has Casualty (printed
     * via [com.wingedsheep.sdk.scripting.KeywordAbility.Casualty] or granted by a permanent via
     * [GrantKeywordToOwnSpells] with a [GrantKeywordToOwnSpells.keywordParameter]) and for which the
     * caster controls at least one creature whose projected power meets the threshold. The chosen
     * creature is submitted as [CastSpell.casualtyCreature] (CR 702.153).
     */
    private fun enumerateCasualty(
        context: EnumerationContext,
        hand: List<EntityId>,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId
        if (context.cantCastSpells) return

        val projected = state.projectedState

        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            val threshold = context.grantedKeywordResolver.casualtyThreshold(state, playerId, cardDef) ?: continue
            if (context.cantCastSpell(cardId)) continue

            // Timing (same rules as a normal cast).
            val isInstant = cardComponent.typeLine.isInstant
            val hasFlash = cardDef.keywords.contains(Keyword.FLASH)
            val grantedFlash = hasFlash || context.castPermissionUtils.hasGrantedFlash(state, cardId)
            if (!isInstant && !grantedFlash && !context.canPlaySorcerySpeed) continue

            val castRestrictions = cardDef.script.castRestrictions
            if (castRestrictions.isNotEmpty() && !context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) continue

            // Gather controlled creatures whose projected power meets the threshold.
            val eligibleSacrifices = mutableListOf<EntityId>()
            for (permId in state.getBattlefield()) {
                if (projected.getController(permId) != playerId) continue
                if (!projected.isCreature(permId)) continue
                if ((projected.getPower(permId) ?: 0) < threshold) continue
                eligibleSacrifices.add(permId)
            }
            if (eligibleSacrifices.isEmpty()) continue

            val baseCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
            val spellContext = SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = false,
                isCreature = cardComponent.typeLine.isCreature,
                isLegendary = cardComponent.typeLine.isLegendary,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX,
                subtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet(),
                cardTypes = cardComponent.typeLine.cardTypes,
            )
            val canAfford = context.manaSolver.canPay(state, playerId, baseCost, spellContext = spellContext, precomputedSources = context.availableManaSources)
            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, baseCost, spellContext = spellContext, precomputedSources = context.availableManaSources)
                    ?.sources?.map { it.entityId }
            }

            val targetReqs = buildList {
                addAll(cardDef.script.targetRequirements)
                cardDef.script.auraTarget?.let { add(it) }
            }

            val casualtyCostInfo = AdditionalCostData(
                description = "Sacrifice a creature with power $threshold or greater",
                costType = "Casualty",
                validSacrificeTargets = eligibleSacrifices,
                sacrificeCount = 1
            )

            if (targetReqs.isNotEmpty()) {
                val targetReqInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                if (!context.targetUtils.allRequirementsSatisfied(targetReqInfos)) continue
                val firstReq = targetReqs.first()
                val firstReqInfo = targetReqInfos.first()
                result.add(LegalAction(
                    actionType = "CastWithCasualty",
                    description = "Cast ${cardComponent.name} (Casualty $threshold)",
                    action = CastSpell(playerId, cardId),
                    validTargets = firstReqInfo.validTargets,
                    requiresTargets = true,
                    targetCount = firstReq.count,
                    minTargets = firstReq.effectiveMinCount,
                    targetDescription = firstReq.description,
                    targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                    affordable = canAfford,
                    manaCostString = baseCost.toString(),
                    autoTapPreview = autoTapPreview,
                    additionalCostInfo = casualtyCostInfo
                ))
            } else {
                result.add(LegalAction(
                    actionType = "CastWithCasualty",
                    description = "Cast ${cardComponent.name} (Casualty $threshold)",
                    action = CastSpell(playerId, cardId),
                    affordable = canAfford,
                    manaCostString = baseCost.toString(),
                    autoTapPreview = autoTapPreview,
                    additionalCostInfo = casualtyCostInfo
                ))
            }
        }
    }

    /**
     * Enumerates kicked spell actions for cards with Kicker or KickerWithAdditionalCost.
     */
    private fun enumerateKicker(
        context: EnumerationContext,
        hand: List<EntityId>,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId

        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue
            if (context.cantCastSpell(cardId)) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            val kickers = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.OptionalAdditionalCost>()
            val manaKicker = kickers.firstOrNull { it.manaCost != null && it.keyword != Keyword.OFFSPRING }
            val additionalCostKicker = kickers.firstOrNull { it.additionalCost != null }
            val offspringAbility = kickers.firstOrNull { it.keyword == Keyword.OFFSPRING }
            if (manaKicker == null && additionalCostKicker == null && offspringAbility == null) continue

            // Check timing (same rules as normal cast — but a flash-timing kicker unlocks
            // instant-speed casting when kicked, e.g. Ghitu Fire's pay-{2}-more clause).
            val isInstant = cardComponent.typeLine.isInstant
            val hasFlash = cardDef.keywords.contains(Keyword.FLASH)
            val grantedFlash = hasFlash || context.castPermissionUtils.hasGrantedFlash(state, cardId)
            val flashKicker = manaKicker?.grantsFlashTiming == true ||
                additionalCostKicker?.grantsFlashTiming == true
            if (!isInstant && !grantedFlash && !flashKicker && !context.canPlaySorcerySpeed) continue

            // Check cast restrictions
            val castRestrictions = cardDef.script.castRestrictions
            if (castRestrictions.isNotEmpty() && !context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) continue

            // Calculate kicked/offspring cost
            val baseCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
            val kickedManaCost = manaKicker?.manaCost ?: offspringAbility?.manaCost
            val kickedCost = if (kickedManaCost != null) baseCost + kickedManaCost else baseCost
            val kickedSpellContext = SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = true,
                isCreature = cardComponent.typeLine.isCreature,
                isLegendary = cardComponent.typeLine.isLegendary,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX,
                subtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet(),
                cardTypes = cardComponent.typeLine.cardTypes,
            )
            val canAffordKickedMana = context.manaSolver.canPay(state, playerId, kickedCost, spellContext = kickedSpellContext, precomputedSources = context.availableManaSources)
            val kickedCostString = kickedCost.toString()
            val kickedAutoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, kickedCost, spellContext = kickedSpellContext, precomputedSources = context.availableManaSources)
                    ?.sources?.map { it.entityId }
            }

            // Kicker {X} (e.g. Verdeloth the Ancient): the kicked cost carries {X}, so the
            // client must prompt for X exactly like a base-cost X spell. The chosen X flows
            // through CastSpell.xValue → SpellOnStackComponent.xValue → the ETB event's
            // xValue, which "create X tokens" reads via DynamicAmount.XValue.
            val kickedHasXCost = kickedCost.hasX
            val kickedMaxAffordableX: Int? = if (kickedHasXCost) {
                val availableSources = context.manaSolver.getAvailableManaCount(state, playerId, precomputedSources = context.availableManaSources, spellContext = kickedSpellContext)
                val fixedCost = kickedCost.cmc  // X contributes 0 to CMC
                val xSymbolCount = kickedCost.xCount.coerceAtLeast(1)
                ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
            } else null

            // Check additional cost payability (e.g., sacrifice a creature)
            var kickerCostInfo: AdditionalCostData? = null
            var canPayKickerAdditionalCost = true
            if (additionalCostKicker?.additionalCost != null) {
                when (val cost = additionalCostKicker.additionalCost) {
                    is AdditionalCost.Atom -> when (val atom = cost.atom) {
                        is CostAtom.Sacrifice -> {
                            val validSacTargets = context.costUtils.findSacrificeTargets(state, playerId, atom)
                            if (validSacTargets.size < atom.count) {
                                canPayKickerAdditionalCost = false
                            } else {
                                kickerCostInfo = AdditionalCostData(
                                    description = atom.description.replaceFirstChar { it.uppercase() },
                                    costType = "SacrificePermanent",
                                    validSacrificeTargets = validSacTargets,
                                    sacrificeCount = atom.count
                                )
                            }
                        }
                        else -> {}
                    }
                    is AdditionalCost.Behold -> {
                        // Behold a matching permanent you control or reveal a matching
                        // card from hand (e.g. Molten Exhale's "behold a Dragon" flash
                        // unlock). Mirrors the mandatory-additional-cost Behold path.
                        val projected = state.projectedState
                        val predicateContext = PredicateContext(controllerId = playerId)
                        val battlefieldMatches = projected.getBattlefieldControlledBy(playerId).filter { permId ->
                            context.predicateEvaluator.matches(state, projected, permId, cost.filter, predicateContext)
                        }
                        val handMatches = state.getZone(ZoneKey(playerId, Zone.HAND))
                            .filter { it != cardId }
                            .filter { context.predicateEvaluator.matches(state, state.projectedState, it, cost.filter, predicateContext) }
                        val beholdTargets = battlefieldMatches + handMatches
                        if (beholdTargets.size < cost.count) {
                            canPayKickerAdditionalCost = false
                        } else {
                            kickerCostInfo = AdditionalCostData(
                                description = cost.description,
                                costType = "Behold",
                                validBeholdTargets = beholdTargets,
                                beholdCount = cost.count
                            )
                        }
                    }
                    else -> {}
                }
            }

            val canAffordKicked = canAffordKickedMana && canPayKickerAdditionalCost

            // Build target info — use kickerTargetRequirements if available
            val kickerBaseReqs = if (cardDef.script.kickerTargetRequirements.isNotEmpty()) {
                cardDef.script.kickerTargetRequirements
            } else {
                cardDef.script.targetRequirements
            }
            val targetReqs = buildList {
                addAll(kickerBaseReqs)
                cardDef.script.auraTarget?.let { add(it) }
            }

            val kickLabel = when {
                offspringAbility != null -> "Offspring"
                flashKicker -> "with Flash"
                else -> "Kicked"
            }

            // Check for DividedDamageEffect in the kicked spell effect
            val kickerSpellEffect = cardDef.script.kickerSpellEffect ?: cardDef.script.spellEffect
            val kickerDividedDamage = kickerSpellEffect as? DividedDamageEffect
            val kickerRequiresDamageDistribution = kickerDividedDamage != null
            val kickerTotalDamage = kickerDividedDamage?.totalDamage
            val kickerMinDamagePerTarget = if (kickerDividedDamage != null) 1 else null

            if (targetReqs.isNotEmpty()) {
                val targetReqInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                val allRequirementsSatisfied = context.targetUtils.allRequirementsSatisfied(targetReqInfos)
                if (allRequirementsSatisfied) {
                    val firstReq = targetReqs.first()
                    val firstReqInfo = targetReqInfos.first()

                    val canAutoSelect = targetReqs.size == 1 &&
                        context.targetUtils.shouldAutoSelectPlayerTarget(firstReq, firstReqInfo.validTargets)

                    if (canAutoSelect) {
                        val autoSelectedTarget = ChosenTarget.Player(firstReqInfo.validTargets.first())
                        result.add(LegalAction(
                            actionType = "CastWithKicker",
                            description = "Cast ${cardComponent.name} ($kickLabel)",
                            action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), wasKicked = true),
                            affordable = canAffordKicked,
                            manaCostString = kickedCostString,
                            autoTapPreview = kickedAutoTapPreview,
                            additionalCostInfo = kickerCostInfo,
                            hasXCost = kickedHasXCost,
                            maxAffordableX = kickedMaxAffordableX,
                            requiresDamageDistribution = kickerRequiresDamageDistribution,
                            totalDamageToDistribute = kickerTotalDamage,
                            minDamagePerTarget = kickerMinDamagePerTarget
                        ))
                    } else {
                        result.add(LegalAction(
                            actionType = "CastWithKicker",
                            description = "Cast ${cardComponent.name} ($kickLabel)",
                            action = CastSpell(playerId, cardId, wasKicked = true),
                            validTargets = firstReqInfo.validTargets,
                            requiresTargets = true,
                            targetCount = firstReq.count,
                            minTargets = firstReq.effectiveMinCount,
                            targetDescription = firstReq.description,
                            targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                            affordable = canAffordKicked,
                            manaCostString = kickedCostString,
                            autoTapPreview = kickedAutoTapPreview,
                            additionalCostInfo = kickerCostInfo,
                            hasXCost = kickedHasXCost,
                            maxAffordableX = kickedMaxAffordableX,
                            requiresDamageDistribution = kickerRequiresDamageDistribution,
                            totalDamageToDistribute = kickerTotalDamage,
                            minDamagePerTarget = kickerMinDamagePerTarget
                        ))
                    }
                }
            } else {
                result.add(LegalAction(
                    actionType = "CastWithKicker",
                    description = "Cast ${cardComponent.name} ($kickLabel)",
                    action = CastSpell(playerId, cardId, wasKicked = true),
                    affordable = canAffordKicked,
                    manaCostString = kickedCostString,
                    autoTapPreview = kickedAutoTapPreview,
                    additionalCostInfo = kickerCostInfo,
                    hasXCost = kickedHasXCost,
                    maxAffordableX = kickedMaxAffordableX
                ))
            }

            // If normal cast is not affordable but kicker is (unlikely), ensure normal cast shows unaffordable
            if (!context.manaSolver.canPay(state, playerId, baseCost, precomputedSources = context.availableManaSources)) {
                result.add(LegalAction(
                    actionType = "CastSpell",
                    description = "Cast ${cardComponent.name}",
                    action = CastSpell(playerId, cardId),
                    affordable = false,
                    manaCostString = baseCost.toString()
                ))
            }
        }
    }

    /**
     * Enumerates the cleave cast (CR 702.148) for cards with the Cleave keyword. Cleave is an
     * *alternative* cost, so it emits a `CastWithAlternativeCost` legal action tagged
     * [AlternativeCostType.CLEAVE] — mirroring evoke/impending — but as its own pass (like
     * [enumerateKicker]) because paying it removes the bracketed text and can therefore change the
     * spell's legal target set (e.g. Fierce Retribution's "target [attacking] creature" widens to
     * "target creature"). The brackets-removed target requirements come from
     * [com.wingedsheep.sdk.model.CardScript.cleaveTargetRequirements]; when a card declares none, the
     * cleave variant is untargeted or targets identically to the base and this reuses the printed
     * requirements.
     */
    private fun enumerateCleave(
        context: EnumerationContext,
        hand: List<EntityId>,
        result: MutableList<LegalAction>
    ) {
        val state = context.state
        val playerId = context.playerId

        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            if (cardComponent.typeLine.isLand) continue
            if (context.cantCastSpell(cardId)) continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            val cleaveAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Cleave>().firstOrNull() ?: continue

            // Timing — cleave doesn't change when the spell can be cast; instants keep flash timing,
            // sorceries stay sorcery-speed.
            val isInstant = cardComponent.typeLine.isInstant
            val grantedFlash = cardDef.keywords.contains(Keyword.FLASH) || context.castPermissionUtils.hasGrantedFlash(state, cardId)
            if (!isInstant && !grantedFlash && !context.canPlaySorcerySpeed) continue

            // Check cast restrictions
            val castRestrictions = cardDef.script.castRestrictions
            if (castRestrictions.isNotEmpty() && !context.castPermissionUtils.checkCastRestrictions(state, playerId, castRestrictions)) continue

            // Cleave mana cost (CR 202.3b — mana value is still computed from the printed cost, not
            // the cleave cost; only affordability uses this).
            val cleaveCost = context.costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, cleaveAbility.cost, playerId)
            val canAffordCleave = context.manaSolver.canPay(state, playerId, cleaveCost, precomputedSources = context.availableManaSources)
            val cleaveCostString = cleaveCost.toString()
            val cleaveAutoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(state, playerId, cleaveCost, precomputedSources = context.availableManaSources)
                    ?.sources?.map { it.entityId }
            }

            // A cleave cost can itself carry {X} (Lantern Flare — cleave {X}{R}{W}). The printed
            // mode may bind X from board state, but the cleave mode's X is chosen and paid, so the
            // client must be prompted for it. Compute the affordable X ceiling exactly as the
            // printed X-cost path does (available mana minus the fixed portion, divided by the X
            // symbol count). `action.xValue` then flows through validation → payment → resolution
            // unchanged, so the resolving effect's `DynamicAmount.XValue` reads the chosen X.
            val cleaveHasX = cleaveCost.hasX
            val cleaveMaxAffordableX: Int? = if (cleaveHasX) {
                val spellContext = SpellPaymentContext(
                    isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                    isKicked = false,
                    isCreature = cardComponent.typeLine.isCreature,
                    isLegendary = cardComponent.typeLine.isLegendary,
                    manaValue = cardComponent.manaCost.cmc,
                    hasXInCost = cleaveCost.hasX,
                    subtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet(),
                    cardTypes = cardComponent.typeLine.cardTypes,
                )
                val availableSources = context.manaSolver.getAvailableManaCount(
                    state, playerId, precomputedSources = context.availableManaSources, spellContext = spellContext
                )
                val fixedCost = cleaveCost.cmc  // X contributes 0 to CMC
                val xSymbolCount = cleaveCost.xCount.coerceAtLeast(1)
                ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
            } else null

            // Target requirements for the brackets-removed variant.
            val cleaveBaseReqs = if (cardDef.script.cleaveTargetRequirements.isNotEmpty()) {
                cardDef.script.cleaveTargetRequirements
            } else {
                cardDef.script.targetRequirements
            }
            val targetReqs = buildList {
                addAll(cleaveBaseReqs)
                cardDef.script.auraTarget?.let { add(it) }
            }

            if (targetReqs.isNotEmpty()) {
                val targetReqInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                if (!context.targetUtils.allRequirementsSatisfied(targetReqInfos)) continue
                val firstReq = targetReqs.first()
                val firstReqInfo = targetReqInfos.first()

                val canAutoSelect = targetReqs.size == 1 &&
                    context.targetUtils.shouldAutoSelectPlayerTarget(firstReq, firstReqInfo.validTargets)

                if (canAutoSelect) {
                    val autoSelectedTarget = ChosenTarget.Player(firstReqInfo.validTargets.first())
                    result.add(LegalAction(
                        actionType = "CastWithAlternativeCost",
                        description = "Cleave ${cardComponent.name} ($cleaveCostString)",
                        action = CastSpell(playerId, cardId, targets = listOf(autoSelectedTarget), useAlternativeCost = true, alternativeCostType = AlternativeCostType.CLEAVE),
                        affordable = canAffordCleave,
                        manaCostString = cleaveCostString,
                        hasXCost = cleaveHasX,
                        maxAffordableX = cleaveMaxAffordableX,
                        autoTapPreview = cleaveAutoTapPreview
                    ))
                } else {
                    result.add(LegalAction(
                        actionType = "CastWithAlternativeCost",
                        description = "Cleave ${cardComponent.name} ($cleaveCostString)",
                        action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.CLEAVE),
                        validTargets = firstReqInfo.validTargets,
                        requiresTargets = true,
                        targetCount = firstReq.count,
                        minTargets = firstReq.effectiveMinCount,
                        targetDescription = firstReq.description,
                        targetRequirements = if (targetReqInfos.size > 1) targetReqInfos else null,
                        affordable = canAffordCleave,
                        manaCostString = cleaveCostString,
                        hasXCost = cleaveHasX,
                        maxAffordableX = cleaveMaxAffordableX,
                        autoTapPreview = cleaveAutoTapPreview
                    ))
                }
            } else {
                result.add(LegalAction(
                    actionType = "CastWithAlternativeCost",
                    description = "Cleave ${cardComponent.name} ($cleaveCostString)",
                    action = CastSpell(playerId, cardId, useAlternativeCost = true, alternativeCostType = AlternativeCostType.CLEAVE),
                    affordable = canAffordCleave,
                    manaCostString = cleaveCostString,
                    hasXCost = cleaveHasX,
                    maxAffordableX = cleaveMaxAffordableX,
                    autoTapPreview = cleaveAutoTapPreview
                ))
            }
        }
    }

    /**
     * Builds the AdditionalCostData for the client based on what additional costs the spell requires.
     */
    private fun buildAdditionalCostData(
        additionalCosts: List<AdditionalCost>,
        sacrificeTargets: List<EntityId>,
        variableSacrificeTargets: List<EntityId>,
        exileTargets: List<EntityId>,
        exileMinCount: Int,
        discardTargets: List<EntityId>,
        discardCount: Int,
        bounceTargets: List<EntityId> = emptyList(),
        bounceCount: Int = 0,
        tapTargets: List<EntityId> = emptyList(),
        tapCount: Int = 0,
        beholdTargets: List<EntityId> = emptyList(),
        beholdCount: Int = 0,
        blightVariableCost: AdditionalCost.BlightVariable? = null,
        blightVariableCreatures: List<EntityId> = emptyList(),
        blightVariableMaxX: Int = 0,
        payXLifeCost: AdditionalCost.PayXLife? = null,
        payXLifeMaxX: Int = 0
    ): AdditionalCostData? {
        if (blightVariableCost != null) {
            return AdditionalCostData(
                description = blightVariableCost.description,
                costType = "BlightVariable",
                validBlightTargets = blightVariableCreatures,
                blightVariableMaxX = blightVariableMaxX
            )
        }
        if (payXLifeCost != null) {
            return AdditionalCostData(
                description = payXLifeCost.description,
                costType = "PayXLife",
                payXLifeMaxX = payXLifeMaxX
            )
        }
        return if (variableSacrificeTargets.isNotEmpty()) {
            val varSacCost = additionalCosts.filterIsInstance<AdditionalCost.SacrificeCreaturesForCostReduction>().firstOrNull()
            AdditionalCostData(
                description = varSacCost?.description ?: "You may sacrifice any number of creatures",
                costType = "SacrificeForCostReduction",
                validSacrificeTargets = variableSacrificeTargets,
                sacrificeCount = 0 // min 0 — sacrifice is optional
            )
        } else if (sacrificeTargets.isNotEmpty()) {
            val sacCost = additionalCosts.firstNotNullOfOrNull { (it as? AdditionalCost.Atom)?.atom as? CostAtom.Sacrifice }
            AdditionalCostData(
                description = sacCost?.description?.replaceFirstChar { it.uppercase() } ?: "Sacrifice a creature",
                costType = "SacrificePermanent",
                validSacrificeTargets = sacrificeTargets,
                sacrificeCount = sacCost?.count ?: 1
            )
        } else if (exileTargets.isNotEmpty()) {
            val exileCostDesc = additionalCosts
                .filterIsInstance<AdditionalCost.ExileVariableCards>()
                .firstOrNull()?.description
                ?: additionalCosts
                    .firstNotNullOfOrNull { (it as? AdditionalCost.Atom)?.atom as? CostAtom.ExileFrom }
                    ?.description?.replaceFirstChar { it.uppercase() }
                ?: "Exile cards from your graveyard"
            AdditionalCostData(
                description = exileCostDesc,
                costType = "ExileFromGraveyard",
                validExileTargets = exileTargets,
                exileMinCount = exileMinCount,
                exileMaxCount = exileTargets.size
            )
        } else if (discardTargets.isNotEmpty()) {
            val discardCost = additionalCosts.firstNotNullOfOrNull { (it as? AdditionalCost.Atom)?.atom as? CostAtom.Discard }
            AdditionalCostData(
                description = discardCost?.description?.replaceFirstChar { it.uppercase() } ?: "Discard a card",
                costType = "DiscardCard",
                validDiscardTargets = discardTargets,
                discardCount = discardCount
            )
        } else if (bounceTargets.isNotEmpty()) {
            val bounceCost = additionalCosts.firstNotNullOfOrNull { (it as? AdditionalCost.Atom)?.atom as? CostAtom.ReturnToHand }
            AdditionalCostData(
                description = bounceCost?.description?.replaceFirstChar { it.uppercase() } ?: "Return a permanent you control to its owner's hand",
                costType = "ReturnToHand",
                validBounceTargets = bounceTargets,
                bounceCount = bounceCount
            )
        } else if (tapTargets.isNotEmpty()) {
            val tapCostAtom = additionalCosts.firstNotNullOfOrNull { (it as? AdditionalCost.Atom)?.atom as? CostAtom.TapPermanents }
            AdditionalCostData(
                description = tapCostAtom?.description?.replaceFirstChar { it.uppercase() } ?: "Tap permanents you control",
                costType = "TapPermanents",
                validTapTargets = tapTargets,
                tapCount = tapCount
            )
        } else if (beholdTargets.isNotEmpty()) {
            val flatCosts = additionalCosts.flatMap { if (it is AdditionalCost.Composite) it.steps else listOf(it) }
            val beholdCost = flatCosts.filterIsInstance<AdditionalCost.Behold>().firstOrNull()
            val chooseCost = flatCosts.filterIsInstance<AdditionalCost.ChooseEntity>().firstOrNull()
            AdditionalCostData(
                description = chooseCost?.description ?: beholdCost?.description ?: "Behold a card",
                costType = if (chooseCost != null) "ChooseEntity" else "Behold",
                validBeholdTargets = beholdTargets,
                beholdCount = beholdCount
            )
        } else null
    }

    /**
     * Internal data holder for self-alternative cost computation results.
     */
    private data class SelfAltCostResult(
        val manaCostString: String,
        val autoTapPreview: List<EntityId>?,
        val additionalCostInfo: AdditionalCostData?
    )

    /**
     * Internal per-mode enumeration snapshot for modal spells.
     *
     * Captures affordability, additional-cost payability, target infos, and
     * rendering fields for a single printed mode. Shared between the choose-1
     * and choose-N emission paths.
     */
    /**
     * One emit-variant of a modal cast (rule 601.2b). Modal spells whose
     * additional-cost choice changes the number of modes the player must pick
     * (e.g., Pyrrhic Strike: blight path forces choosing every mode) surface as
     * separate `LegalAction`s — one per cost path — each with its own effective
     * `chooseCount`/`minChooseCount`, mana cost, additional-cost info, and
     * description suffix.
     */
    private data class ModalCastVariant(
        val effect: ModalEffect,
        val baseEffectiveCost: ManaCost,
        val additionalCostInfo: AdditionalCostData?,
        val manaCostString: String,
        val autoTapPreview: List<EntityId>?,
        val descriptionSuffix: String
    )

    private data class ModeEnumeration(
        val modeIndex: Int,
        val mode: Mode,
        val effectiveCost: ManaCost,
        val manaCostString: String,
        val canAffordMana: Boolean,
        val canPayAdditionalCosts: Boolean,
        val additionalCostInfo: AdditionalCostData?,
        val autoTapPreview: List<EntityId>?,
        val targetInfos: List<TargetInfo>,
        val allTargetRequirementsSatisfied: Boolean
    ) {
        /** True when this mode is both payable and has its target requirements satisfied (700.2a). */
        val available: Boolean
            get() = canAffordMana && canPayAdditionalCosts && allTargetRequirementsSatisfied
    }

    /**
     * Compute a [ModeEnumeration] for a single printed mode.
     *
     * Evaluates per-mode cost deltas ([Mode.additionalManaCost]), per-mode additional
     * costs ([Mode.additionalCosts]), and target legality. Always returns an
     * enumeration — callers decide whether `available = false` means "skip this mode"
     * (choose-1) or "offer it greyed-out / non-pickable" (choose-N, rules 700.2a).
     */
    private fun computeModeEnumeration(
        context: EnumerationContext,
        cardId: EntityId,
        playerId: EntityId,
        modeIndex: Int,
        mode: Mode,
        baseEffectiveCost: ManaCost,
        cardLevelAdditionalCostInfo: AdditionalCostData?,
        baseAutoTapPreview: List<EntityId>?,
        spellContext: SpellPaymentContext,
        cachedSources: List<ManaSource>
    ): ModeEnumeration {
        val state = context.state

        val modeExtraManaCost = mode.additionalManaCost
        val modeEffectiveCost = if (modeExtraManaCost != null) {
            baseEffectiveCost + ManaCost.parse(modeExtraManaCost)
        } else {
            baseEffectiveCost
        }
        val canAffordMana = if (modeExtraManaCost != null) {
            context.manaSolver.canPay(
                state, playerId, modeEffectiveCost,
                spellContext = spellContext,
                precomputedSources = cachedSources
            )
        } else {
            true // base cost already checked upstream
        }

        var canPayAdditionalCosts = true
        val modeSacrificeTargets = mutableListOf<EntityId>()
        var modeExileTargets = emptyList<EntityId>()
        var modeExileMinCount = 0
        var modeDiscardTargets = emptyList<EntityId>()
        var modeDiscardCount = 0

        val modeAdditionalCosts = mode.additionalCosts
        if (modeAdditionalCosts != null) {
            for (cost in modeAdditionalCosts) {
                when (cost) {
                    is AdditionalCost.Atom -> when (val atom = cost.atom) {
                        is CostAtom.Sacrifice -> {
                            val validSacTargets = context.costUtils.findSacrificeTargets(state, playerId, atom)
                            if (validSacTargets.size < atom.count) canPayAdditionalCosts = false
                            modeSacrificeTargets.addAll(validSacTargets)
                        }
                        is CostAtom.ExileFrom -> {
                            val validExileTargets = context.costUtils.findExileTargets(state, playerId, atom.filter, atom.zone)
                            if (validExileTargets.size < atom.count) canPayAdditionalCosts = false
                            modeExileTargets = validExileTargets
                            modeExileMinCount = atom.count
                        }
                        is CostAtom.Discard -> {
                            val handZone = ZoneKey(playerId, Zone.HAND)
                            val handCards = state.getZone(handZone).filter { it != cardId }
                            val predicateContext = PredicateContext(controllerId = playerId)
                            val validDiscards = if (atom.filter == com.wingedsheep.sdk.scripting.GameObjectFilter.Any) {
                                handCards
                            } else {
                                handCards.filter { context.predicateEvaluator.matches(state, state.projectedState, it, atom.filter, predicateContext) }
                            }
                            if (validDiscards.size < atom.count) canPayAdditionalCosts = false
                            modeDiscardTargets = validDiscards
                            modeDiscardCount = atom.count
                        }
                        is CostAtom.PayLife -> {
                            // Per CR 119.4 you can't pay life unless you have that much. Mode-level
                            // affordability gate so "discard a card or pay 3 life" modes don't
                            // surface a Pay-3-Life action when the caster has fewer than 3 life
                            // (Bitter Triumph). Validation in CastSpellHandler still backstops.
                            val life = state.lifeTotal(playerId) // CR 810.9a — team's shared total
                            if (life < atom.amount) canPayAdditionalCosts = false
                        }
                        else -> {}
                    }
                    is AdditionalCost.Forage -> {
                        val graveyardSize = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD)).size
                        val projected = state.projectedState
                        val hasFood = state.getBattlefield().any { permId ->
                            state.getEntity(permId) ?: return@any false
                            projected.getController(permId) == playerId &&
                                projected.hasSubtype(permId, com.wingedsheep.sdk.core.Subtype.FOOD.value)
                        }
                        if (graveyardSize < 3 && !hasFood) canPayAdditionalCosts = false
                    }
                    else -> {}
                }
            }
        }

        val modeCostInfo = if (modeAdditionalCosts != null) {
            // A forage additional cost on a mode (e.g. Feed the Cycle's forage mode) surfaces the
            // forage cost picker so the player chooses which cards to exile / Food to sacrifice,
            // rather than the cost resolving silently. Prefer the exile mode when both are payable.
            if (modeAdditionalCosts.any { it is AdditionalCost.Forage }) {
                com.wingedsheep.engine.handlers.costs.ForageCostResolver.costInfos(
                    com.wingedsheep.engine.handlers.costs.ForageCostResolver.candidates(state, playerId)
                ).firstOrNull()
            } else {
                buildAdditionalCostData(
                    modeAdditionalCosts, modeSacrificeTargets, emptyList(),
                    modeExileTargets, modeExileMinCount, modeDiscardTargets, modeDiscardCount
                )
            }
        } else {
            cardLevelAdditionalCostInfo
        }

        val modeManaCostString = modeEffectiveCost.toString()
        val modeAutoTapPreview = if (context.skipAutoTapPreview) null
        else if (modeExtraManaCost != null) {
            context.manaSolver.solve(state, playerId, modeEffectiveCost, precomputedSources = cachedSources)
                ?.sources?.map { it.entityId }
        } else {
            baseAutoTapPreview
        }

        val modeTargetReqs = mode.targetRequirements
        val modeTargetInfos = if (modeTargetReqs.isNotEmpty()) {
            context.targetUtils.buildTargetInfos(state, playerId, modeTargetReqs)
        } else {
            emptyList()
        }
        val allTargetRequirementsSatisfied = modeTargetReqs.isEmpty() ||
            context.targetUtils.allRequirementsSatisfied(modeTargetInfos)

        return ModeEnumeration(
            modeIndex = modeIndex,
            mode = mode,
            effectiveCost = modeEffectiveCost,
            manaCostString = modeManaCostString,
            canAffordMana = canAffordMana,
            canPayAdditionalCosts = canPayAdditionalCosts,
            additionalCostInfo = modeCostInfo,
            autoTapPreview = modeAutoTapPreview,
            targetInfos = modeTargetInfos,
            allTargetRequirementsSatisfied = allTargetRequirementsSatisfied
        )
    }

    // =========================================================================
    // Split face (CR 709)
    // =========================================================================

    /**
     * Emit a [LegalAction] for casting one face of a split-layout card from hand (CR 709.4 —
     * only the chosen half goes on the stack and is evaluated for legality).
     *
     * Each face has its own mana cost, type line (so timing is per-face), and — crucially —
     * its own `targetRequirements`. Reading the face's targets here is what makes targeted
     * split halves (Pain // Suffering, Stand // Deliver, Wax // Wane) actually offer their
     * targets to the client; without it the cast surfaces with no valid targets even though
     * the validation layer ([CastSpellHandler]) reads from the face script and expects them.
     *
     * Modal effects, alternative costs, blight, behold, kicker, and convoke on a split half
     * are not yet wired up — cards that need them can extend this method later.
     */
    private fun enumerateSplitFace(
        context: EnumerationContext,
        cardId: EntityId,
        cardDef: CardDefinition,
        faceIndex: Int,
        face: CardFace,
        result: MutableList<LegalAction>,
    ) {
        val state = context.state
        val playerId = context.playerId

        // Per-face timing: the chosen half's own type line governs sorcery-speed restriction.
        if (!face.typeLine.isInstant && !context.canPlaySorcerySpeed) return

        val cachedSources = context.availableManaSources
        val effectiveCost = context.costCalculator
            .calculateEffectiveCostWithAlternativeBase(state, cardDef, face.manaCost, playerId)
        if (!context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = cachedSources)) return

        val autoTapPreview = if (context.skipAutoTapPreview) null else {
            context.manaSolver
                .solve(state, playerId, effectiveCost, precomputedSources = cachedSources)
                ?.sources?.map { it.entityId }
        }
        val manaCostString = effectiveCost.toString()

        val targetReqs = face.script.targetRequirements
        if (targetReqs.isEmpty()) {
            result.add(
                LegalAction(
                    actionType = "CastSpell",
                    description = "Cast ${face.name}",
                    action = CastSpell(playerId, cardId, faceIndex = faceIndex),
                    manaCostString = manaCostString,
                    autoTapPreview = autoTapPreview,
                )
            )
            return
        }

        val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
        if (!context.targetUtils.allRequirementsSatisfied(targetInfos)) return
        val firstReq = targetReqs.first()
        val firstInfo = targetInfos.first()

        // Auto-select a sole legal player target (e.g. "target player" in a 2-player game where
        // only one player is legal), matching the normal cast path's UX.
        val canAutoSelect = targetReqs.size == 1 &&
            context.targetUtils.shouldAutoSelectPlayerTarget(firstReq, firstInfo.validTargets)
        if (canAutoSelect) {
            val autoTarget = ChosenTarget.Player(firstInfo.validTargets.first())
            result.add(
                LegalAction(
                    actionType = "CastSpell",
                    description = "Cast ${face.name}",
                    action = CastSpell(playerId, cardId, faceIndex = faceIndex, targets = listOf(autoTarget)),
                    manaCostString = manaCostString,
                    autoTapPreview = autoTapPreview,
                )
            )
            return
        }

        result.add(
            LegalAction(
                actionType = "CastSpell",
                description = "Cast ${face.name}",
                action = CastSpell(playerId, cardId, faceIndex = faceIndex),
                validTargets = firstInfo.validTargets,
                requiresTargets = true,
                targetCount = firstReq.count,
                minTargets = firstReq.effectiveMinCount,
                targetDescription = firstReq.description,
                targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                manaCostString = manaCostString,
                autoTapPreview = autoTapPreview,
            )
        )
    }

    // =========================================================================
    // Secondary spell face (Adventure CR 715 / modal DFC CR 712)
    // =========================================================================

    /**
     * Emit a [LegalAction] for casting the secondary spell face of an Adventure or modal DFC card
     * from hand. The card's primary (creature) face is enumerated by the surrounding loop's normal
     * cast path; this method only adds the alternative-characteristics cast that uses
     * [CastSpell.faceIndex] = 0.
     *
     * Supports mana cost, instant/sorcery timing, and per-face target requirements. Additional
     * costs declared on the face's script are honoured for affordability but the full
     * additional-cost UX (sacrifice picker, blight, etc.) is not yet wired for these faces —
     * cards that need that can extend this method later.
     *
     * @param primaryFaceAffordable Whether the card's primary (creature) face is affordable.
     *        When the secondary face is unaffordable but the primary is affordable, a grayed-out
     *        placeholder for the secondary face is still emitted so the drag-to-play menu shows
     *        both faces.
     * @return True if an *affordable* secondary-face cast was emitted. The caller uses this to
     *        decide whether to emit a grayed-out placeholder for an unaffordable primary face.
     */
    private fun enumerateSecondaryFace(
        context: EnumerationContext,
        cardId: EntityId,
        cardDef: CardDefinition,
        result: MutableList<LegalAction>,
        primaryFaceAffordable: Boolean,
    ): Boolean {
        val state = context.state
        val playerId = context.playerId
        val face = cardDef.cardFaces.firstOrNull() ?: return false
        state.getEntity(playerId) ?: return false

        val isInstantAdventure = face.typeLine.isInstant
        if (!isInstantAdventure && !context.canPlaySorcerySpeed) return false

        val effectiveCost = context.costCalculator
            .calculateEffectiveCostWithAlternativeBase(state, cardDef, face.manaCost, playerId)
        val cachedSources = context.availableManaSources
        val canAfford = context.manaSolver
            .canPay(state, playerId, effectiveCost, precomputedSources = cachedSources)
        if (!canAfford) {
            // Secondary face is unaffordable. If the primary face is affordable, still surface
            // this face as a grayed-out option so the drag-to-play menu presents both and the
            // player can choose deliberately or cancel (rather than auto-firing the primary).
            if (primaryFaceAffordable) {
                result.add(
                    LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${face.name}",
                        action = CastSpell(playerId, cardId, faceIndex = 0),
                        affordable = false,
                        manaCostString = effectiveCost.toString(),
                    )
                )
            }
            return false
        }

        val targetReqs = face.script.targetRequirements
        val autoTapPreview = if (context.skipAutoTapPreview) null else {
            context.manaSolver
                .solve(state, playerId, effectiveCost, precomputedSources = cachedSources)
                ?.sources?.map { it.entityId }
        }
        val manaCostString = effectiveCost.toString()

        if (targetReqs.isEmpty()) {
            result.add(
                LegalAction(
                    actionType = "CastSpell",
                    description = "Cast ${face.name}",
                    action = CastSpell(playerId, cardId, faceIndex = 0),
                    manaCostString = manaCostString,
                    autoTapPreview = autoTapPreview,
                )
            )
            return true
        }

        val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
        if (!context.targetUtils.allRequirementsSatisfied(targetInfos)) return false
        val firstReq = targetReqs.first()
        val firstInfo = targetInfos.first()
        result.add(
            LegalAction(
                actionType = "CastSpell",
                description = "Cast ${face.name}",
                action = CastSpell(playerId, cardId, faceIndex = 0),
                validTargets = firstInfo.validTargets,
                requiresTargets = true,
                targetCount = firstReq.count,
                minTargets = firstReq.effectiveMinCount,
                targetDescription = firstReq.description,
                targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                manaCostString = manaCostString,
                autoTapPreview = autoTapPreview,
            )
        )
        return true
    }
}
