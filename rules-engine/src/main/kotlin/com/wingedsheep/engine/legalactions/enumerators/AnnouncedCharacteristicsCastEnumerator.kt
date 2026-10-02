package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.handlers.actions.spell.CastAdditionalCosts
import com.wingedsheep.engine.handlers.actions.spell.CastZoneResolver
import com.wingedsheep.engine.handlers.actions.spell.CastCostTotaller
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.BestowCasts
import com.wingedsheep.engine.mechanics.CastCharacteristics
import com.wingedsheep.engine.mechanics.PrototypeCasts
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.engine.mechanics.cost.spell.SpellCostEnumeration
import com.wingedsheep.engine.mechanics.cost.spell.SpellCosts
import com.wingedsheep.engine.mechanics.mana.TapForGeneric
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * A way to cast a card announced with characteristics other than its printed ones — bestow's Aura
 * (CR 702.103) or prototype's cost and size (CR 718.3). The announced characteristics decide
 * every cost, payment and timing check, through [CastCharacteristics].
 */
enum class CharacteristicsAnnouncement(val actionType: String) {
    BESTOW("CastWithAlternativeCost") {
        override fun offers(printed: CardDefinition) = printed.keywordAbilities.any { it is KeywordAbility.Bestow }
        override fun action(player: EntityId, card: EntityId) =
            CastSpell(player, card, useAlternativeCost = true, alternativeCostType = AlternativeCostType.BESTOW)
        override val targets = listOf(BestowCasts.enchantCreature)
        override fun description(printed: CardDefinition, card: CardComponent) = "Bestow ${card.name}"
    },
    PROTOTYPE("CastSpell") {
        override fun offers(printed: CardDefinition) = PrototypeCasts.prototypeOf(printed) != null
        override fun action(player: EntityId, card: EntityId) = CastSpell(player, card, castPrototyped = true)
        override val targets = emptyList<TargetRequirement>()
        override fun description(printed: CardDefinition, card: CardComponent) =
            PrototypeCasts.prototypeOf(printed)!!.let { "Cast ${card.name} prototyped (${it.power}/${it.toughness})" }
    };

    abstract fun offers(printed: CardDefinition): Boolean
    abstract fun action(player: EntityId, card: EntityId): CastSpell
    abstract val targets: List<TargetRequirement>
    abstract fun description(printed: CardDefinition, card: CardComponent): String
}

/** Offers each [CharacteristicsAnnouncement] cast on the ordinary casting rails. */
class AnnouncedCharacteristicsCastEnumerator(private val kind: CharacteristicsAnnouncement) : ActionEnumerator {
    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val player = context.playerId
        val zones = CastZoneResolver(context.cardRegistry, context.conditionEvaluator, context.legality)
        val candidates = buildSet {
            addAll(context.state.getHand(player))
            if (!context.restrictedToHandCasting) {
                for (pid in context.state.turnOrder) {
                    addAll(context.state.getGraveyard(pid))
                    addAll(context.state.getZone(ZoneKey(pid, Zone.EXILE)))
                }
                addAll(context.state.getLibrary(player))
                addAll(context.state.getZone(ZoneKey(player, Zone.COMMAND)))
            }
        }
        return buildList {
            for (id in candidates) {
                val original = context.state.getEntity(id)?.get<CardComponent>() ?: continue
                val printed = context.cardRegistry.getCard(original.cardDefinitionId) ?: continue
                if (!kind.offers(printed)) continue
                val action = kind.action(player, id)
                val state = CastCharacteristics.announce(context.state, action, context.cardRegistry)
                val def = CastCharacteristics.definitionForCast(printed, action)!!
                val card = state.getEntity(id)!!.get<CardComponent>()!!
                val inHand = id in state.getHand(player)
                if (inHand && context.cantPlayCardsFromHand) continue
                if (!inHand && !(zones.isOnTopOfLibraryWithPermission(state, player, id) ||
                    zones.isInExileWithPlayPermission(state, player, id) ||
                    zones.hasMayCastSelfFromZonePermission(state, player, id, action) ||
                    zones.hasMayPlayPermanentFromGraveyardPermission(state, player, id, card) ||
                    zones.hasMayCastFromGraveyardPermission(state, player, id, card) ||
                    zones.hasCommanderCastPermission(state, player, id))) continue
                if (context.castPermissionUtils.reasonCannotCast(state, player, id) != null) continue
                if (zones.hasPlayWithoutPayingCost(state, player, id) ||
                    state.getEntity(id)?.has<com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent>() == true) continue
                if (Keyword.FLASH !in def.keywords && !zones.hasGrantedFlash(state, id) && !context.canPlaySorcerySpeed) continue
                if (!context.legality.castRestrictionsMet(state, player, def.script.castRestrictions)) continue

                val totals = CastCostTotaller(context.cardRegistry, context.costCalculator,
                    AlternativePaymentHandler(context.grantedKeywordResolver), zones, context.predicateEvaluator)
                val cost = totals.totalCost(state, action, def, card, false, zones.hasCommanderCastPermission(state, player, id)) ?: continue
                val sources = context.availableManaSources
                val paymentContext = com.wingedsheep.engine.mechanics.mana.SpellPaymentContext(
                    isCreature = card.typeLine.isCreature, isLegendary = card.typeLine.isLegendary,
                    manaValue = card.manaCost.cmc, hasXInCost = card.manaCost.hasX,
                    isColorless = card.colors.isEmpty(),
                    subtypes = card.typeLine.subtypes.mapTo(mutableSetOf()) { it.value }, cardTypes = card.typeLine.cardTypes,
                    isFromHand = inHand,
                    isFromExile = state.turnOrder.any { id in state.getZone(ZoneKey(it, Zone.EXILE)) }
                )
                fun hasKeyword(keyword: Keyword) =
                    context.grantedKeywordResolver.hasKeyword(state, player, def, keyword, id)
                val hasConvoke = hasKeyword(Keyword.CONVOKE)
                val convokeCreatures = if (hasConvoke) context.costUtils.findConvokeCreatures(state, player) else emptyList()
                val hasDelve = hasKeyword(Keyword.DELVE)
                val delveCards = if (hasDelve) context.costUtils.findDelveCards(state, player).filter { it.entityId != id } else emptyList()
                val hasImprovise = hasKeyword(Keyword.IMPROVISE)
                val improviseArtifacts = if (hasImprovise) {
                    context.costUtils.findTapForGenericPermanents(state, player, TapForGeneric.IMPROVISE)
                } else emptyList()
                fun manaAffordable(priced: ManaCost) = context.manaSolver.canPay(
                    state, player, priced, spellContext = paymentContext, precomputedSources = sources)
                fun affordable(priced: ManaCost): Boolean {
                    val payable = context.castPermissionUtils.relaxSpellCostColorsIfAny(state, player, id, priced)
                    if (manaAffordable(payable)) return true
                    if (convokeCreatures.isNotEmpty() && (
                        context.costUtils.canAffordWithConvoke(state, player, payable, convokeCreatures,
                            precomputedSources = sources, spellContext = paymentContext) ||
                        context.costUtils.canAffordWithConvoke(state, player, payable, convokeCreatures,
                            precomputedSources = sources, spellContext = paymentContext,
                            tapForGenericPermanents = improviseArtifacts))) return true
                    if (delveCards.isNotEmpty() && (
                        context.costUtils.canAffordWithDelve(state, player, payable, delveCards,
                            precomputedSources = sources, spellContext = paymentContext) ||
                        context.costUtils.canAffordWithDelve(state, player, payable, delveCards,
                            precomputedSources = sources, spellContext = paymentContext,
                            tapForGenericPermanents = improviseArtifacts))) return true
                    return improviseArtifacts.isNotEmpty() && context.costUtils.canAffordWithTapForGeneric(
                        state, player, payable, improviseArtifacts,
                        precomputedSources = sources, spellContext = paymentContext)
                }
                if (!affordable(cost)) continue
                val costs = CastAdditionalCosts(context.cardRegistry, context.costCalculator, zones, context.predicateEvaluator)
                    .owedAdditionalCosts(state, action, def)
                val env = SpellCostEnumeration(state, player, id, context.costUtils, context.predicateEvaluator)
                var costInfo: AdditionalCostData? = null
                var payable = SpellCosts.enumerateAll(env, costs, com.wingedsheep.engine.mechanics.cost.spell.SpellCostOffer())
                for (term in SpellCosts.flattenComposites(costs)) {
                    val choices = SpellCosts.candidates(env, term)
                    if (!SpellCosts.canPayFrom(env, term, choices)) payable = false
                    if (costInfo == null) costInfo = SpellCosts.present(env, term, choices)?.second
                }
                if (!payable) continue
                val targets = context.targetUtils.buildTargetInfos(state, player, kind.targets, id, TargetingSourceType.SPELL)
                if (!context.targetUtils.allRequirementsSatisfied(targets)) continue
                val maxX = if (cost.hasX) {
                    // Reprice announced X before reductions: an enchantment discount can pay X.
                    fun affordableX(x: Int): Boolean {
                        val priced = totals.totalCost(state, action.copy(xValue = x), def, card, false,
                            zones.hasCommanderCastPermission(state, player, id)) ?: return false
                        return affordable(priced)
                    }
                    var low = 0
                    var high = 1
                    while (high < Int.MAX_VALUE / 2 && affordableX(high)) { low = high; high *= 2 }
                    while (high - low > 1) {
                        val mid = low + (high - low) / 2
                        if (affordableX(mid)) low = mid else high = mid
                    }
                    low
                } else null
                add(LegalAction(
                    actionType = kind.actionType, description = kind.description(printed, card), action = action,
                    requiresTargets = targets.isNotEmpty(), validTargets = targets.firstOrNull()?.validTargets,
                    targetCount = kind.targets.size.coerceAtLeast(1), minTargets = kind.targets.size,
                    targetDescription = kind.targets.firstOrNull()?.description,
                    manaCostString = cost.toString(), hasXCost = cost.hasX, maxAffordableX = maxX,
                    additionalCostInfo = costInfo,
                    hasConvoke = hasConvoke, convokeCreatures = convokeCreatures.takeIf { hasConvoke },
                    hasDelve = hasDelve, delveCards = delveCards.takeIf { hasDelve },
                    minDelveNeeded = if (hasDelve) context.costUtils.calculateMinDelveNeeded(
                        state, player, cost, delveCards, precomputedSources = sources, spellContext = paymentContext) else null,
                    hasTapForGeneric = hasImprovise, tapForGenericPermanents = improviseArtifacts.takeIf { hasImprovise },
                    tapForGenericLabel = TapForGeneric.IMPROVISE.label.takeIf { hasImprovise },
                    tapForGenericRequired = if (hasImprovise) !manaAffordable(cost) else null,
                    sourceZone = if (inHand) null else state.zones.entries.firstOrNull { id in it.value }?.key?.zoneType?.name,
                    autoTapPreview = if (context.skipAutoTapPreview) null else context.manaSolver.solve(state, player, cost, spellContext = paymentContext, precomputedSources = sources)?.sources?.map { it.entityId }
                ))
            }
        }
    }
}
