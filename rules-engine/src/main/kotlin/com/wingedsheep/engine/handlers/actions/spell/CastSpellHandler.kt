package com.wingedsheep.engine.handlers.actions.spell
import com.wingedsheep.sdk.dsl.Patterns

import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CastWithCreatureTypeContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.AdditionalCostSelectionKind
import com.wingedsheep.engine.core.CastSpellAdditionalCostContinuation
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.mechanics.FlashbackGrants
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.mechanics.SneakWindow
import com.wingedsheep.engine.mechanics.WarpGrants
import com.wingedsheep.engine.mechanics.MiracleGrants
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.TappedEvent
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.permissions.activeMayPlayFor
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.PlayerCantPlayFromHandComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.EventPattern as SdkGameEvent
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.core.Keyword

import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.state.components.player.GrantedSpellKeywordsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.PermanentSnapshot
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.capturePermanentSnapshots
import kotlin.reflect.KClass

/**
 * Handler for the CastSpell action.
 *
 * Orchestrates spell casting by delegating to focused components:
 * - [CastZoneResolver]: Determines where a card can be cast from
 * - [CastPaymentProcessor]: Handles mana payment via three strategies
 *
 * This handler owns the top-level validate/execute flow, cast restrictions,
 * additional cost processing, and trigger detection.
 */
/**
 * True if this cast's [CastSpell.alternativeCostType] permits the given alternative cost [type] —
 * either because the player explicitly chose it, or because no choice was recorded (`null`, the
 * legacy path) and the handler should fall back to its priority chain. Used to gate each branch of
 * the alternative-cost resolution so an explicit choice (e.g. evoke) isn't overridden by a
 * higher-priority cost that also happens to be available (e.g. a granted warp).
 */
private fun CastSpell.altAllows(type: AlternativeCostType): Boolean =
    alternativeCostType == null || alternativeCostType == type

class CastSpellHandler(
    private val cardRegistry: CardRegistry,
    private val turnManager: TurnManager,
    private val manaSolver: ManaSolver,
    private val costCalculator: CostCalculator,
    private val alternativePaymentHandler: AlternativePaymentHandler,
    private val costHandler: CostHandler,
    private val stackResolver: StackResolver,
    private val targetValidator: TargetValidator,
    private val conditionEvaluator: ConditionEvaluator,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
    private val manaAbilitySideEffectExecutor: com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor,
    private val targetFinder: com.wingedsheep.engine.handlers.TargetFinder = com.wingedsheep.engine.handlers.TargetFinder(),
) : ActionHandler<CastSpell> {
    override val actionType: KClass<CastSpell> = CastSpell::class

    private val predicateEvaluator = PredicateEvaluator()
    private val zoneResolver = CastZoneResolver(cardRegistry, conditionEvaluator)
    private val castPermissionUtils = com.wingedsheep.engine.legalactions.utils.CastPermissionUtils(
        cardRegistry, predicateEvaluator, conditionEvaluator
    )
    private val paymentProcessor = CastPaymentProcessor(manaSolver, costHandler, manaAbilitySideEffectExecutor)
    private val grantedKeywordResolver = com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver(cardRegistry)
    private val costEnumerationUtils = com.wingedsheep.engine.legalactions.utils.CostEnumerationUtils(
        manaSolver, costCalculator, predicateEvaluator, cardRegistry
    )

    override fun validate(state: GameState, action: CastSpell): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"

        val handZone = ZoneKey(action.playerId, Zone.HAND)
        val inHand = action.cardId in state.getZone(handZone)
        val onTopOfLibrary = !inHand && zoneResolver.isOnTopOfLibraryWithPermission(state, action.playerId, action.cardId)
        val mayPlayFromExile = !inHand && !onTopOfLibrary && zoneResolver.isInExileWithPlayPermission(state, action.playerId, action.cardId)
        val mayCastFromZone = !inHand && !onTopOfLibrary && !mayPlayFromExile &&
            zoneResolver.hasMayCastSelfFromZonePermission(state, action.playerId, action.cardId)
        val mayCastFromGraveyard = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone &&
            zoneResolver.hasMayPlayPermanentFromGraveyardPermission(state, action.playerId, action.cardId, cardComponent)
        val hasFlashback = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard &&
            zoneResolver.hasFlashbackPermission(state, action.playerId, action.cardId)
        // Harmonize (e.g., Channeled Dragonfire) — cast from graveyard for its harmonize
        // cost; `hasHarmonizePermission` checks the graveyard zone + Harmonize keyword.
        val hasHarmonize = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback &&
            zoneResolver.hasHarmonizePermission(state, action.playerId, action.cardId)
        val hasGraveyardCast = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasHarmonize &&
            zoneResolver.hasMayCastFromGraveyardPermission(state, action.playerId, action.cardId, cardComponent)
        val hasForageFromGraveyard = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasHarmonize && !hasGraveyardCast &&
            zoneResolver.hasMayCastCreaturesFromGraveyardWithForage(state, action.playerId, action.cardId, cardComponent)
        // Warp from graveyard (e.g., Timeline Culler) — `hasWarpPermission` already
        // checks both hand and graveyard; this branch covers the graveyard case
        // when `inHand` is false.
        val hasWarpFromGraveyard = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasHarmonize && !hasGraveyardCast && !hasForageFromGraveyard &&
            action.useAlternativeCost &&
            zoneResolver.hasWarpPermission(state, action.playerId, action.cardId)
        val hasCommanderCast = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasHarmonize && !hasGraveyardCast && !hasForageFromGraveyard && !hasWarpFromGraveyard &&
            zoneResolver.hasCommanderCastPermission(state, action.playerId, action.cardId)
        if (!inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasHarmonize && !hasGraveyardCast && !hasForageFromGraveyard && !hasWarpFromGraveyard && !hasCommanderCast) {
            return "Card is not in your hand"
        }

        // Memory Vessel: "they can't play cards from their hand" — hand-scoped, so casts from
        // exile/graveyard granted by a may-play permission still resolve.
        if (inHand && state.getEntity(action.playerId)?.has<PlayerCantPlayFromHandComponent>() == true) {
            return "You can't play cards from your hand"
        }

        // Single cast-legality chokepoint: per-turn spell limit (Yawgmoth's Agenda),
        // Silence-style can't-cast, Mana Maze color sharing, and PlayersCantCastSpells
        // (Voice of Victory, …) all resolve to a reason here, or null if the cast is allowed.
        castPermissionUtils.reasonCannotCast(state, action.playerId, action.cardId)?.let { return it }

        if (hasForageFromGraveyard) {
            if (!costHandler.canPayAdditionalCost(state, AdditionalCost.Forage, action.playerId)) {
                return "Cannot forage: need 3 other cards in graveyard or a Food"
            }
        }

        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        // Handle face-down casting (morph)
        if (action.castFaceDown) {
            val morphAbility = cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Morph>()?.firstOrNull()
                ?: return "This card cannot be cast face down (no morph ability)"

            if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "You can only cast face-down creatures at sorcery speed"
            }

            val morphCastCost = costCalculator.calculateFaceDownCost(state, action.playerId)
            return validatePayment(state, action, morphCastCost)
        }

        // Check timing — for Adventure / split faces use the face's type line (CR 715 / 709.4)
        val effectiveTypeLine = action.faceIndex
            ?.let { cardDef?.cardFaces?.getOrNull(it)?.typeLine }
            ?: cardComponent.typeLine
        // Sneak (CR 702.190a) grants an instant-speed casting permission during the active
        // player's declare blockers step — bypassing the normal sorcery-speed timing.
        val castingForSneak = action.useAlternativeCost &&
            action.altAllows(AlternativeCostType.SNEAK) &&
            cardDef?.keywordAbilities?.any { it is KeywordAbility.Sneak } == true
        if (!effectiveTypeLine.isInstant) {
            val hasFlash = cardDef?.keywords?.contains(Keyword.FLASH) == true
            val grantedFlash = hasFlash || zoneResolver.hasGrantedFlash(state, action.cardId)
            // A flash-timing kicker unlocks instant-speed casting when paid — whether the
            // optional cost is mana (Ghitu Fire) or a non-mana cost like Behold (Molten Exhale).
            val flashTimingKicker = action.wasKicked && cardDef?.keywordAbilities
                ?.filterIsInstance<KeywordAbility.OptionalAdditionalCost>()
                ?.any { it.grantsFlashTiming } == true
            if (!grantedFlash && !flashTimingKicker && !castingForSneak &&
                !turnManager.canPlaySorcerySpeed(state, action.playerId)
            ) {
                return "You can only cast sorcery-speed spells during your main phase with an empty stack"
            }
        }

        // Sneak (CR 702.190a): legal only during the active player's declare blockers step,
        // and the player must return exactly one unblocked attacker they control to its
        // owner's hand as the non-mana portion of the cost.
        if (castingForSneak) {
            if (!SneakWindow.isWindowOpen(state, action.playerId)) {
                return "You can only cast this for its sneak cost during your declare blockers step while you control an unblocked attacker"
            }
            val bounced = action.additionalCostPayment?.bouncedPermanents ?: emptyList()
            if (bounced.size != 1) {
                return "Sneak requires returning exactly one unblocked attacker you control to its owner's hand"
            }
            if (bounced.first() !in SneakWindow.unblockedAttackers(state, action.playerId)) {
                return "The chosen creature is not an unblocked attacker you control"
            }
        }

        // Check cast restrictions
        if (cardDef != null && cardDef.script.castRestrictions.isNotEmpty()) {
            val restrictionError = validateCastRestrictions(state, cardDef.script.castRestrictions, action.playerId)
            if (restrictionError != null) {
                return restrictionError
            }
        }

        // Choose-N modal shape checks (rules 700.2a / 700.2d). Enforced only when the
        // action arrives with chosenModes populated — the cast-time continuation flow
        // starts with an empty list which falls through to the pause in execute().
        if (cardDef != null && action.chosenModes.isNotEmpty()) {
            val modalEffect = cardDef.script.spellEffect as? ModalEffect
            if (modalEffect != null) {
                val modalError = validateChosenModeShape(state, modalEffect, action)
                if (modalError != null) return modalError
            }
        }

        // Validate additional costs (use per-mode costs if the chosen mode overrides them)
        if (cardDef != null) {
            val modeAdditionalCosts = resolveAdditionalCostsForMode(cardDef, action)
            val additionalCostError = validateAdditionalCosts(state, modeAdditionalCosts, action)
            if (additionalCostError != null) {
                return additionalCostError
            }
        }

        // Validate linked-exile granter's additional cost (e.g. Dawnhand Dissident)
        val linkedExileGranter = zoneResolver.findLinkedExileGranter(state, action.playerId, action.cardId)
        val linkedExileAdditionalCost = linkedExileGranter?.additionalCost
        if (linkedExileAdditionalCost != null) {
            val linkedCostError = validateAdditionalCosts(state, listOf(linkedExileAdditionalCost), action)
            if (linkedCostError != null) return linkedCostError
        }

        // Validate runtime additional costs from PlayWithAdditionalCostComponent (e.g., The Infamous Cruelclaw)
        val runtimeAdditionalCostComponent = state.getEntity(action.cardId)
            ?.get<PlayWithAdditionalCostComponent>()
            ?.takeIf { it.controllerId == action.playerId }
        if (runtimeAdditionalCostComponent != null) {
            val runtimeCostError = validateAdditionalCosts(state, runtimeAdditionalCostComponent.additionalCosts, action)
            if (runtimeCostError != null) return runtimeCostError
        }

        // Validate kicker/offspring: card must have a Kicker keyword ability
        if (action.wasKicked && cardDef != null) {
            val kickers = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.OptionalAdditionalCost>()
            if (kickers.isEmpty()) return "This card does not have kicker"

            // Validate non-mana kicker additional cost (sacrifice, etc.)
            val kickerAdditionalCost = kickers.firstOrNull { it.additionalCost != null }?.additionalCost
            if (kickerAdditionalCost != null) {
                val kickerCostError = validateAdditionalCosts(state, listOf(kickerAdditionalCost), action)
                if (kickerCostError != null) return kickerCostError
            }
        }

        // Validate self-alternative cost's additional costs when using alternative cost
        if (action.useAlternativeCost && cardDef != null && action.altAllows(AlternativeCostType.SELF_ALTERNATIVE)) {
            val selfAltCost = cardDef.script.selfAlternativeCost
            if (selfAltCost != null && selfAltCost.additionalCosts.isNotEmpty()) {
                val selfAltCostError = validateAdditionalCosts(state, selfAltCost.additionalCosts, action)
                if (selfAltCostError != null) return selfAltCostError
            }
        }

        // Validate flashback's bundled additional cost (e.g., "Flashback—{1}{R}, Behold three Elementals")
        if (action.useAlternativeCost && cardDef != null && hasFlashback && action.altAllows(AlternativeCostType.FLASHBACK)) {
            val flashbackAdditional = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.Flashback>()
                .firstOrNull()
                ?.additionalCost
            if (flashbackAdditional != null) {
                val flashbackCostError = validateAdditionalCosts(state, listOf(flashbackAdditional), action)
                if (flashbackCostError != null) return flashbackCostError
            }
        }

        // Validate warp's bundled additional cost (e.g., "Warp—{B}, Pay 2 life." on Timeline Culler).
        // Granted warps ([com.wingedsheep.sdk.scripting.GrantWarpToCardsInHand]) currently carry no
        // additional cost, but [WarpGrants] is the source of truth either way.
        if (action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.WARP) &&
            zoneResolver.hasWarpPermission(state, action.playerId, action.cardId)
        ) {
            val warpAdditional = WarpGrants.effectiveWarp(
                state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator
            )?.additionalCost
            if (warpAdditional != null) {
                val warpCostError = validateAdditionalCosts(state, listOf(warpAdditional), action)
                if (warpCostError != null) return warpCostError
            }
        }

        // Validate Conspire optional additional cost (CR 702.78). Two untapped creatures the
        // caster controls, each sharing a color with the spell. The spell must have Conspire
        // either printed or granted (e.g., Raiding Schemes: "Each noncreature spell you cast
        // has conspire").
        if (action.conspiredCreatures.isNotEmpty()) {
            if (cardDef == null) return "Conspire requires a card definition"
            val conspireError = validateConspire(state, action, cardDef)
            if (conspireError != null) return conspireError
        }

        // Validate Casualty optional additional cost (CR 702.153). One creature the caster controls
        // with projected power >= the spell's casualty threshold. The spell must have Casualty
        // either printed or granted (e.g., Silverquill: "Each instant and sorcery spell you cast
        // has casualty 1").
        if (action.casualtyCreature != null) {
            if (cardDef == null) return "Casualty requires a card definition"
            val casualtyError = validateCasualty(state, action, cardDef)
            if (casualtyError != null) return casualtyError
        }

        // Calculate effective cost (free if PlayWithoutPayingCostComponent is present, or if a
        // MayCastWithoutPayingManaCost battlefield source (e.g. Weftwalking) is the chosen alt).
        val playForFreeFromComponent = zoneResolver.hasPlayWithoutPayingCost(state, action.playerId, action.cardId)
        if (action.useWithoutPayingManaCost) {
            // CR 118.9a — only one alternative cost can apply to a given cast.
            if (action.useAlternativeCost) {
                return "Cannot combine 'without paying its mana cost' with another alternative cost"
            }
            if (!costCalculator.hasFreeCastPermission(state, action.playerId, cardDef)) {
                return "'Without paying its mana cost' is not available (gate closed or no source on the battlefield)"
            }
        }
        val playForFree = playForFreeFromComponent || action.useWithoutPayingManaCost
        // Split-layout (CR 709.3a) — only the chosen half is evaluated for legality. When
        // `faceIndex` is set, the cost is the face's printed mana cost passed through the
        // standard battlefield cost-modifier pipeline (CR 118.9a applies cost modifiers to
        // the chosen half just like to a normal cast).
        val faceManaCostOverride: ManaCost? = action.faceIndex?.let { idx ->
            cardDef?.cardFaces?.getOrNull(idx)?.manaCost
        }
        var effectiveCost = if (playForFree) {
            ManaCost.ZERO
        } else if (faceManaCostOverride != null && cardDef != null) {
            costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, faceManaCostOverride, action.playerId)
        } else if (action.useAlternativeCost && cardDef != null) {
            // Check flashback cost first (printed on the card, or granted at runtime by Archmage's Newt).
            val flashbackAbility = FlashbackGrants.effectiveFlashback(state, action.cardId, cardDef)
            // Harmonize may be printed on the card or granted at runtime (Songcrafter Mage).
            val harmonizeAbility = HarmonizeGrants.effectiveHarmonize(state, action.cardId, cardDef)
            // Each branch is gated by [CastSpell.altAllows] so an explicit player choice (e.g.
            // evoke) isn't overridden by a higher-priority cost that also happens to be legal
            // (e.g. a granted warp). With no choice recorded, every gate is open and this falls
            // back to the original priority order.
            if (action.altAllows(AlternativeCostType.FLASHBACK) && flashbackAbility != null && zoneResolver.hasFlashbackPermission(state, action.playerId, action.cardId)) {
                costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, flashbackAbility.cost, action.playerId)
            } else if (action.altAllows(AlternativeCostType.HARMONIZE) && harmonizeAbility != null && zoneResolver.hasHarmonizePermission(state, action.playerId, action.cardId)) {
                // Harmonize cost (printed or granted). The per-creature power reduction is
                // applied afterward via alternativePayment.
                costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, harmonizeAbility.cost, action.playerId)
            } else {
                // Check warp cost (hand only — CR 702.185a). Re-casts from exile pay the regular
                // mana cost. Printed warp wins; a battlefield grant ([GrantWarpToCardsInHand])
                // supplies the cost when the card has no printed warp.
                val warpAbility = WarpGrants.effectiveWarp(
                    state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator
                )
                if (action.altAllows(AlternativeCostType.WARP) && warpAbility != null && zoneResolver.hasWarpPermission(state, action.playerId, action.cardId)) {
                    costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, warpAbility.cost, action.playerId)
                } else {
                    // Check sneak cost (CR 702.190 — mana portion; the bounce is paid separately)
                    val sneakAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Sneak>().firstOrNull()
                    // Check evoke cost
                    val evokeAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Evoke>().firstOrNull()
                    if (action.altAllows(AlternativeCostType.SNEAK) && sneakAbility != null) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, sneakAbility.cost, action.playerId)
                    } else if (action.altAllows(AlternativeCostType.EVOKE) && evokeAbility != null) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, evokeAbility.cost, action.playerId)
                    } else {
                        // Check impending cost
                        val impendingAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Impending>().firstOrNull()
                        // Check miracle cost (CR 702.94 — printed or granted in hand, window-gated).
                        // The window component must be present (opened when drawn as the first card
                        // this turn); without it, the miracle alternative cost is unavailable.
                        val miracleWindowOpen = state.getEntity(action.cardId)
                            ?.has<com.wingedsheep.engine.state.components.identity.MiracleWindowComponent>() == true
                        val miracleAbility = if (miracleWindowOpen) MiracleGrants.effectiveMiracle(
                            state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator
                        ) else null
                        if (action.altAllows(AlternativeCostType.IMPENDING) && impendingAbility != null) {
                            costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, impendingAbility.cost, action.playerId)
                        } else if (action.altAllows(AlternativeCostType.MIRACLE) && miracleAbility != null) {
                            costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, miracleAbility.cost, action.playerId)
                        } else {
                            // Check self-alternative cost (e.g., Zahid's {3}{U} + tap artifact)
                            val selfAltCost = cardDef.script.selfAlternativeCost
                            if (action.altAllows(AlternativeCostType.SELF_ALTERNATIVE) && selfAltCost != null) {
                                val altMana = selfAltCost.manaCost
                                costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altMana, action.playerId)
                            } else {
                                // Fall back to battlefield-granted alternative cost (e.g., Jodah's {W}{U}{B}{R}{G})
                                val altCosts = costCalculator.findAlternativeCastingCosts(state, action.playerId)
                                if (altCosts.isEmpty()) return "No alternative casting cost available"
                                costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altCosts.first())
                            }
                        }
                    }
                }
            }
        } else if (cardDef != null) {
            costCalculator.calculateEffectiveCost(
                state,
                cardDef,
                action.playerId,
                action.targets.map { it.toEntityId() },
                fromZone = if (hasCommanderCast) Zone.COMMAND else castSourceZone(state, action.cardId),
            )
        } else {
            cardComponent.manaCost
        }

        // Add kicker/offspring mana cost if kicked (only for mana-based kicker/offspring)
        if (action.wasKicked && !playForFree && !action.useAlternativeCost && cardDef != null) {
            val kickerManaCost = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.OptionalAdditionalCost>()
                .firstOrNull { it.manaCost != null }
                ?.manaCost
            if (kickerManaCost != null) {
                effectiveCost = ManaCost(effectiveCost.symbols + kickerManaCost.symbols)
            }
        }

        // Apply BlightOrPay "pay mana" adjustment in validation
        if (cardDef != null && !playForFree) {
            val blightOrPay = cardDef.script.additionalCosts
                .filterIsInstance<AdditionalCost.BlightOrPay>()
                .firstOrNull()
            if (blightOrPay != null) {
                val choseBlight = action.additionalCostPayment?.blightTargets?.isNotEmpty() == true
                if (!choseBlight) {
                    effectiveCost = effectiveCost + ManaCost.parse(blightOrPay.alternativeManaCost)
                }
            }
        }

        // Apply BeholdOrPay "pay mana" adjustment in validation
        if (cardDef != null && !playForFree) {
            val beholdOrPay = cardDef.script.additionalCosts
                .filterIsInstance<AdditionalCost.BeholdOrPay>()
                .firstOrNull()
            if (beholdOrPay != null) {
                val choseBehold = action.additionalCostPayment?.beheldCards?.isNotEmpty() == true
                if (!choseBehold) {
                    effectiveCost = effectiveCost + ManaCost.parse(beholdOrPay.alternativeManaCost)
                }
            }
        }

        // Apply ExileFromGraveyardOrPay "pay mana" adjustment in validation
        if (cardDef != null && !playForFree) {
            val exileOrPay = cardDef.script.additionalCosts
                .filterIsInstance<AdditionalCost.ExileFromGraveyardOrPay>()
                .firstOrNull()
            if (exileOrPay != null) {
                val choseExile = action.additionalCostPayment?.exiledCards?.isNotEmpty() == true
                if (!choseExile) {
                    effectiveCost = effectiveCost + ManaCost.parse(exileOrPay.alternativeManaCost)
                }
            }
        }

        // Apply runtime mana tax from exile permissions (e.g., Soul Partition).
        if (!playForFree) {
            val runtimeCostIncrease = state.getEntity(action.cardId)
                ?.get<PlayWithCostIncreaseComponent>()
                ?.takeIf { it.controllerId == action.playerId }
            if (runtimeCostIncrease != null) {
                effectiveCost = effectiveCost + ManaCost.parse("{${runtimeCostIncrease.amount}}")
            }
        }

        // Apply sacrifice-for-cost-reduction before validating payment
        if (cardDef != null && action.additionalCostPayment != null) {
            for (cost in cardDef.script.additionalCosts) {
                if (cost is AdditionalCost.SacrificeCreaturesForCostReduction) {
                    val sacrificeCount = action.additionalCostPayment.sacrificedPermanents.size
                    val reduction = sacrificeCount * cost.costReductionPerCreature
                    if (reduction > 0) {
                        effectiveCost = effectiveCost.reduceGeneric(reduction)
                    }
                }
            }
        }

        // Account for Delve/Convoke reduction before validating payment
        val costAfterAltPayment = if (action.alternativePayment != null && !action.alternativePayment.isEmpty && cardDef != null) {
            alternativePaymentHandler.calculateReducedCost(
                effectiveCost,
                action.alternativePayment,
                cardDef,
                state,
                action.playerId,
                action.cardId
            )
        } else {
            effectiveCost
        }

        // Validate payment. For an X-cost Harmonize cast where a creature is tapped, the
        // creature's power reduces generic mana — and {X} is generic (TDM release notes) —
        // so the leftover reduction beyond any printed generic comes off the X mana paid.
        val paymentXValue = harmonizePaymentXValue(state, action, cardDef, effectiveCost)
        val paymentError = validatePayment(state, action, costAfterAltPayment, paymentXValue)
        if (paymentError != null) {
            return paymentError
        }

        // Validate targets (include auraTarget as a target requirement for aura spells)
        // Use mode-specific targets for modal spells, kickerTargetRequirements when kicked
        if (cardDef != null) {
            // Adventure / split face cast (CR 715 / 709) — read targets from the face's script.
            val faceScript = action.faceIndex?.let { cardDef.cardFaces.getOrNull(it)?.script }
            val effectiveScript = faceScript ?: cardDef.script
            val modalEffect = effectiveScript.spellEffect as? com.wingedsheep.sdk.scripting.effects.ModalEffect
            // A choose-N modal cast that arrives with modes chosen but targets deferred
            // (the single-panel client mode selector submits `chosenModes` only) is target-
            // validated later by the cast-time per-mode target pause in execute(); skip the
            // top-level target check here so the deferred-targets action isn't rejected.
            val modalTargetsDeferred = modalEffect != null &&
                action.chosenModes.isNotEmpty() &&
                action.targets.isEmpty() &&
                action.modeTargetsOrdered.isEmpty()
            val baseTargetReqs = if (modalTargetsDeferred) {
                emptyList()
            } else if (action.chosenModes.isNotEmpty() && modalEffect != null) {
                // Modal spell with mode(s) chosen at cast time — validate against the union of per-mode requirements.
                action.chosenModes.flatMap { modeIndex ->
                    modalEffect.modes.getOrNull(modeIndex)?.targetRequirements ?: emptyList()
                }
            } else if (action.wasKicked && cardDef.script.kickerTargetRequirements.isNotEmpty()) {
                cardDef.script.kickerTargetRequirements
            } else {
                effectiveScript.targetRequirements
            }
            val targetRequirements = buildList {
                addAll(baseTargetReqs)
                cardDef.script.auraTarget?.let { add(it) }
            }
            if (targetRequirements.isNotEmpty()) {
                // Reject casting if spell requires targets but none were provided
                if (action.targets.isEmpty()) {
                    val requiredCount = targetRequirements.sumOf { it.effectiveMinCount }
                    if (requiredCount > 0) {
                        return "No valid targets available"
                    }
                }
                val targetError = targetValidator.validateTargets(
                    state,
                    action.targets,
                    targetRequirements,
                    action.playerId,
                    sourceColors = cardDef.colors,
                    sourceSubtypes = cardDef.typeLine.subtypes.map { it.value }.toSet(),
                    sourceId = action.cardId,
                    xValue = action.xValue
                )
                if (targetError != null) {
                    return targetError
                }
            }
        }

        // Validate damage distribution for DividedDamageEffect spells
        // Use kickerSpellEffect when kicked and available
        val spellEffect = if (action.wasKicked && cardDef?.script?.kickerSpellEffect != null) {
            cardDef.script.kickerSpellEffect
        } else {
            cardDef?.script?.spellEffect
        }
        if (spellEffect is DividedDamageEffect && action.targets.size > 1) {
            val distribution = action.damageDistribution
            if (distribution == null) {
                return "Damage distribution required for this spell when targeting multiple creatures"
            }

            // Check that distribution targets match chosen targets
            val targetIds = action.targets.map { it.toEntityId() }.toSet()
            val distributionTargets = distribution.keys
            if (distributionTargets != targetIds) {
                return "Damage distribution targets must match chosen targets"
            }

            // Check that total damage equals the spell's total damage
            val totalDistributed = distribution.values.sum()
            if (totalDistributed != spellEffect.totalDamage) {
                return "Total distributed damage ($totalDistributed) must equal ${spellEffect.totalDamage}"
            }

            // Check that each target gets at least 1 damage (per MTG rules)
            val minPerTarget = 1
            for ((targetId, damage) in distribution) {
                if (damage < minPerTarget) {
                    return "Each target must receive at least $minPerTarget damage"
                }
            }
        }

        // Validate that the caster can afford any additional life cost imposed by opponent
        // permanents via ModifySpellCost + OpponentsCastTargeting + IncreaseLife (e.g. Terror
        // of the Peaks: "Spells your opponents cast that target this creature cost an
        // additional 3 life to cast.").
        if (action.targets.isNotEmpty()) {
            val additionalLifeCost = costCalculator.calculateAdditionalLifeCost(
                state, action.playerId, action.targets
            )
            if (additionalLifeCost > 0) {
                val currentLife = state.lifeTotal(action.playerId) // CR 810.9a — team's shared total
                if (currentLife < additionalLifeCost) {
                    return "Not enough life to pay additional life cost ($additionalLifeCost life required)"
                }
            }
        }

        return null
    }

    /**
     * X value used for *mana payment* of a Harmonize cast (≤ `action.xValue`).
     *
     * Harmonize lets the player tap one creature to reduce the cost by generic mana equal
     * to its power; {X} is generic mana (TDM release notes), but colored pips are never
     * reduced. [AlternativePaymentHandler] already lowers the printed generic via
     * `reduceGeneric`; the leftover reduction beyond the printed generic must come off the
     * mana paid for X. The spell's own X value ([CastSpell.xValue], which drives the
     * "mana value X or less" search) is unchanged — only the mana paid for X drops.
     *
     * Returns `action.xValue` unchanged when this isn't an X-cost Harmonize cast with a
     * validly-tapped creature, mirroring [AlternativePaymentHandler.applyHarmonize]'s guards
     * so validation, payment, and the actual tap stay consistent.
     */
    private fun harmonizePaymentXValue(
        state: GameState,
        action: CastSpell,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        harmonizeCost: ManaCost,
    ): Int {
        val xValue = action.xValue ?: 0
        if (xValue <= 0) return xValue
        val creatureId = action.alternativePayment?.harmonizeCreature ?: return xValue
        // Harmonize may be printed or granted at runtime (Songcrafter Mage).
        if (HarmonizeGrants.effectiveHarmonize(state, action.cardId, cardDef) == null) return xValue
        if (!zoneResolver.hasHarmonizePermission(state, action.playerId, action.cardId)) return xValue
        // Mirror applyHarmonize's validity gate: a creature that wouldn't actually be tapped
        // grants no reduction, so payment must not assume one.
        if (creatureId !in state.getZone(ZoneKey(action.playerId, Zone.BATTLEFIELD))) return xValue
        val container = state.getEntity(creatureId) ?: return xValue
        val projected = state.projectedState
        if (!projected.isCreature(creatureId)) return xValue
        if (container.has<TappedComponent>()) return xValue
        if (container.get<ControllerComponent>()?.playerId != action.playerId) return xValue
        val power = (projected.getPower(creatureId) ?: 0).coerceAtLeast(0)
        if (power <= 0) return xValue
        // reduceGeneric eats the printed generic first; whatever power is left reduces the
        // X mana. xCount > 1 (no current card) floors conservatively so payment never
        // under-charges.
        val leftover = (power - harmonizeCost.genericAmount).coerceAtLeast(0)
        val xCount = harmonizeCost.xCount.coerceAtLeast(1)
        return ((xValue * xCount - leftover).coerceAtLeast(0)) / xCount
    }

    private fun validatePayment(state: GameState, action: CastSpell, cost: ManaCost, paymentXValue: Int = action.xValue ?: 0): String? {
        val xValue = paymentXValue

        // Build spell context for conditional mana validation
        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>()
        val spellCtx = if (cardComponent != null) {
            SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = action.wasKicked,
                isCreature = cardComponent.typeLine.isCreature,
                isLegendary = cardComponent.typeLine.isLegendary,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX,
                subtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet(),
                isFromExile = isCastFromExile(state, action.cardId),
                isFromHand = isCastFromHand(state, action.cardId),
                cardTypes = cardComponent.typeLine.cardTypes,
            )
        } else null

        // "Mana of any type can be spent" — relax colored requirements when the cast
        // permission carries that flag (e.g. Taster of Wares, Cruelclaw's Heist).
        val effectiveCost = if (isCastWithAnyManaType(state, action)) cost.relaxColors() else cost

        // "Spend only [colors] on X" restriction (Soul Burn) — limits which mana can pay X.
        val cardDef = cardComponent?.let { cardRegistry.getCard(it.cardDefinitionId) }
        val xManaRestriction = (action.faceIndex?.let { cardDef?.cardFaces?.getOrNull(it)?.script }
            ?: cardDef?.script)?.xManaRestriction ?: emptySet()

        return when (action.paymentStrategy) {
            is PaymentStrategy.AutoPay -> {
                if (!manaSolver.canPay(state, action.playerId, effectiveCost, xValue, spellContext = spellCtx, xManaRestriction = xManaRestriction)) {
                    "Not enough mana to cast this spell"
                } else null
            }
            is PaymentStrategy.FromPool -> {
                val poolComponent = state.getEntity(action.playerId)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                val pool = ManaPool(
                    white = poolComponent.white,
                    blue = poolComponent.blue,
                    black = poolComponent.black,
                    red = poolComponent.red,
                    green = poolComponent.green,
                    colorless = poolComponent.colorless,
                    restrictedMana = poolComponent.restrictedMana
                )
                if (!pool.canPay(effectiveCost, spellCtx)) {
                    "Insufficient mana in pool to cast this spell"
                } else null
            }
            is PaymentStrategy.Explicit -> {
                for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                    val sourceContainer = state.getEntity(sourceId)
                        ?: return "Mana source not found: $sourceId"
                    if (sourceContainer.has<TappedComponent>()) {
                        return "Mana source is already tapped: $sourceId"
                    }
                }
                // Mirror what [CastPaymentProcessor.autoPay] actually does: pay from the
                // floating pool first, then verify the chosen sources can cover the rest.
                // Otherwise a player who has already floated mana before clicking cast
                // gets a false "Selected mana sources cannot pay this spell's cost"
                // because the validator demands the chosen sources alone cover the full
                // (post-convoke/delve) cost.
                val poolComponent = state.getEntity(action.playerId)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                val pool = ManaPool(
                    white = poolComponent.white,
                    blue = poolComponent.blue,
                    black = poolComponent.black,
                    red = poolComponent.red,
                    green = poolComponent.green,
                    colorless = poolComponent.colorless,
                    restrictedMana = poolComponent.restrictedMana
                )
                val partial = pool.payPartial(effectiveCost, spellCtx)
                val remainingCost = partial.remainingCost
                if (remainingCost.isEmpty() && xValue == 0) {
                    null
                } else {
                    val chosen = action.paymentStrategy.manaAbilitiesToActivate.toSet()
                    val excluded = manaSolver.findAvailableManaSources(state, action.playerId)
                        .map { it.entityId }
                        .filter { it !in chosen }
                        .toSet()
                    if (manaSolver.solve(state, action.playerId, remainingCost, xValue, excludeSources = excluded, spellContext = spellCtx) == null) {
                        "Selected mana sources cannot pay this spell's cost"
                    } else null
                }
            }
        }
    }

    /**
     * True if the spell is being cast via a [com.wingedsheep.engine.state.permissions.MayPlayPermission]
     * that allows mana of any type to be spent. The card must currently be in a zone a may-play
     * permission can grant casting from — exile (the card's owner's, which may be an opponent —
     * e.g. Taster of Wares leaves the exiled card in the revealing player's exile) or a graveyard
     * (per-card grants that leave the card in the graveyard — e.g. Tinybones, the Pickpocket lets
     * you cast a targeted nonland permanent card from the damaged player's graveyard). An active
     * permission must be granted to the casting player with its condition gate open, and the
     * `withAnyManaType` flag must be set on at least one of those active permissions.
     */
    private fun isCastWithAnyManaType(state: GameState, action: CastSpell): Boolean {
        val inGrantableZone = state.turnOrder.any { ownerId ->
            action.cardId in state.getZone(ZoneKey(ownerId, Zone.EXILE)) ||
                action.cardId in state.getZone(ZoneKey(ownerId, Zone.GRAVEYARD))
        }
        if (!inGrantableZone) return false
        return state.activeMayPlayFor(action.cardId, action.playerId, conditionEvaluator)
            .any { it.withAnyManaType }
    }

    private fun isCastFromExile(state: GameState, cardId: EntityId): Boolean =
        state.turnOrder.any { ownerId -> cardId in state.getZone(ZoneKey(ownerId, Zone.EXILE)) }

    private fun isCastFromHand(state: GameState, cardId: EntityId): Boolean =
        state.turnOrder.any { ownerId -> cardId in state.getZone(ZoneKey(ownerId, Zone.HAND)) }

    /**
     * The zone the card is being cast from, used to apply cast-from-zone cost modifiers
     * (e.g. Aven Interrupter's "spells your opponents cast from graveyards or exile cost {2}
     * more"). A spell card still occupies its source zone when the cost is computed during
     * cast validation/execution (it hasn't moved to the stack yet). Stack means it's already
     * being moved; returns null then. Commander casts are handled separately via the
     * dedicated `Zone.COMMAND` flag.
     */
    private fun castSourceZone(state: GameState, cardId: EntityId): Zone? {
        for (ownerId in state.turnOrder) {
            for (zone in listOf(Zone.HAND, Zone.GRAVEYARD, Zone.EXILE, Zone.LIBRARY)) {
                if (cardId in state.getZone(ZoneKey(ownerId, zone))) return zone
            }
        }
        return null
    }

    private fun validateConspire(
        state: GameState,
        action: CastSpell,
        cardDef: com.wingedsheep.sdk.model.CardDefinition
    ): String? {
        if (!grantedKeywordResolver.hasKeyword(state, action.playerId, cardDef, Keyword.CONSPIRE)) {
            return "This spell does not have conspire"
        }
        val chosen = action.conspiredCreatures
        if (chosen.size != 2) return "Conspire requires tapping exactly two creatures"
        if (chosen[0] == chosen[1]) return "Conspire requires two distinct creatures"
        val spellColors = cardDef.colors
        if (spellColors.isEmpty()) return "Cannot conspire: a colorless spell has no color to share"
        val projected = state.projectedState
        val battlefield = state.getBattlefield()
        for (creatureId in chosen) {
            if (creatureId !in battlefield) return "Conspire creature is not on the battlefield"
            val container = state.getEntity(creatureId)
                ?: return "Conspire creature not found: $creatureId"
            if (projected.getController(creatureId) != action.playerId) {
                return "Conspire creature is not controlled by you"
            }
            if (!projected.isCreature(creatureId)) return "Conspire requires creatures"
            if (container.has<TappedComponent>()) return "Conspire creature is already tapped"
            val sharesColor = spellColors.any { projected.hasColor(creatureId, it) }
            if (!sharesColor) return "Conspire creature shares no color with this spell"
        }
        return null
    }

    private fun validateCasualty(
        state: GameState,
        action: CastSpell,
        cardDef: com.wingedsheep.sdk.model.CardDefinition
    ): String? {
        val threshold = grantedKeywordResolver.casualtyThreshold(state, action.playerId, cardDef)
            ?: return "This spell does not have casualty"
        val creatureId = action.casualtyCreature ?: return "Casualty requires a creature to sacrifice"
        val projected = state.projectedState
        if (creatureId !in state.getBattlefield()) return "Casualty creature is not on the battlefield"
        state.getEntity(creatureId) ?: return "Casualty creature not found: $creatureId"
        if (projected.getController(creatureId) != action.playerId) {
            return "Casualty creature is not controlled by you"
        }
        if (!projected.isCreature(creatureId)) return "Casualty requires a creature"
        val power = projected.getPower(creatureId) ?: 0
        if (power < threshold) return "Casualty creature must have power $threshold or greater"
        return null
    }

    private fun validateCastRestrictions(
        state: GameState,
        restrictions: List<CastRestriction>,
        playerId: EntityId
    ): String? {
        val context = EffectContext(
            sourceId = null,
            controllerId = playerId,
            targets = emptyList(),
            xValue = 0
        )

        for (restriction in restrictions) {
            val error = validateSingleRestriction(state, restriction, context)
            if (error != null) return error
        }
        return null
    }

    private fun validateSingleRestriction(
        state: GameState,
        restriction: CastRestriction,
        context: EffectContext
    ): String? {
        return when (restriction) {
            is CastRestriction.OnlyDuringStep -> {
                if (state.step != restriction.step) {
                    "Can only be cast during the ${restriction.step.name.lowercase().replace('_', ' ')} step"
                } else null
            }
            is CastRestriction.OnlyDuringPhase -> {
                if (state.phase != restriction.phase) {
                    "Can only be cast during the ${restriction.phase.name.lowercase().replace('_', ' ')} phase"
                } else null
            }
            is CastRestriction.OnlyIfCondition -> {
                if (!conditionEvaluator.evaluate(state, restriction.condition, context)) {
                    "Casting condition not met"
                } else null
            }
            is CastRestriction.TimingRequirement -> null
            is CastRestriction.All -> {
                for (subRestriction in restriction.restrictions) {
                    val error = validateSingleRestriction(state, subRestriction, context)
                    if (error != null) return error
                }
                null
            }
        }
    }

    /**
     * Validates the shape of a choose-N modal cast action (rules 700.2a / 700.2d).
     *
     * Checks: mode indices are in range, chosen count falls within
     * `[minChooseCount, chooseCount]`, duplicates only appear when `allowRepeat`, and
     * `modeTargetsOrdered` (if provided) is aligned 1:1 with `chosenModes`.
     */
    private fun validateChosenModeShape(state: GameState, modalEffect: ModalEffect, action: CastSpell): String? {
        val chosen = action.chosenModes
        for (idx in chosen) {
            if (idx < 0 || idx >= modalEffect.modes.size) {
                return "Invalid mode index: $idx"
            }
        }
        val (effectiveMin, effectiveMax) = effectiveModalChooseCounts(state, modalEffect, action)
        if (chosen.size < effectiveMin) {
            return "Too few modes chosen: ${chosen.size} (minimum $effectiveMin)"
        }
        if (chosen.size > effectiveMax) {
            return "Too many modes chosen: ${chosen.size} (maximum $effectiveMax)"
        }
        if (!modalEffect.allowRepeat && chosen.distinct().size != chosen.size) {
            return "Modes cannot be chosen more than once for this spell"
        }
        if (action.modeTargetsOrdered.isNotEmpty() && action.modeTargetsOrdered.size != chosen.size) {
            return "modeTargetsOrdered size (${action.modeTargetsOrdered.size}) must match chosenModes size (${chosen.size})"
        }
        return null
    }

    /**
     * Compute the effective `[minChooseCount, chooseCount]` range for a modal spell,
     * accounting for `ModalEffect.chooseAllIfBlightPaid` and `ModalEffect.dynamicChooseCount`.
     *
     * - `chooseAllIfBlightPaid`: when the flag is set and the player paid the spell's optional
     *   `BlightOrPay` cost via blight, the player must choose all modes; otherwise the regular
     *   range applies.
     * - `dynamicChooseCount`: evaluated against cast-time battlefield state to produce the upper
     *   bound (clamped to `[minChooseCount, modes.size]`). Models the printed "Choose one. If you
     *   control a Wizard as you cast this spell, you may choose two instead." pattern (Flame of
     *   Anor) — `minChooseCount` stays the mandatory floor, unlike the resolution-time
     *   `chooseUpToDynamic` shape which always allows declining to 0.
     */
    private fun effectiveModalChooseCounts(
        state: GameState,
        modalEffect: ModalEffect,
        action: CastSpell
    ): Pair<Int, Int> {
        if (modalEffect.chooseAllIfBlightPaid) {
            val blightPaid = action.additionalCostPayment?.blightTargets?.isNotEmpty() == true
            return if (blightPaid) {
                modalEffect.modes.size to modalEffect.modes.size
            } else {
                modalEffect.minChooseCount to modalEffect.minChooseCount
            }
        }
        val dynamic = modalEffect.dynamicChooseCount
        if (dynamic != null) {
            val context = EffectContext(
                sourceId = action.cardId,
                controllerId = action.playerId,
                targets = emptyList(),
                xValue = 0
            )
            val evaluated = DynamicAmountEvaluator(conditionEvaluator = conditionEvaluator)
                .evaluate(state, dynamic, context)
            val max = evaluated.coerceIn(modalEffect.minChooseCount, modalEffect.modes.size)
            return modalEffect.minChooseCount to max
        }
        return modalEffect.minChooseCount to modalEffect.chooseCount
    }

    /**
     * Resolves the additional costs for a spell, considering per-mode overrides.
     *
     * If any chosen mode specifies its own additionalCosts, costs from every such mode are unioned
     * (rule 700.2h — per-mode additional costs stack). Modes with a null override fall through to
     * the card-level costs. If no chosen mode provides overrides, card-level costs are used.
     */
    private fun counterTypeToCountersString(type: CounterType): String = when (type) {
        CounterType.PLUS_ONE_PLUS_ONE -> Counters.PLUS_ONE_PLUS_ONE
        CounterType.MINUS_ONE_MINUS_ONE -> Counters.MINUS_ONE_MINUS_ONE
        else -> type.name.lowercase()
    }

    /**
     * Resolve the distributed counter removals to apply for a
     * [AdditionalCost.RemoveCountersFromYourCreatures] cost.
     *
     * Web clients send the typed [AdditionalCostPayment.distributedCounterRemovals] —
     * one entry per (entity, counterType, count) — so the player explicitly picks
     * which counter types come off each creature. The CastSpell flow does not honour
     * the legacy `counterRemovals: Map<EntityId, Int>` payload; that field remains
     * only for activated-ability X-cost (`RemoveXPlusOnePlusOneCounters`), which is
     * single-type by definition and routes through [CostHandler] instead.
     */
    private fun resolveDistributedCounterRemovalsForPayment(
        action: CastSpell
    ): List<com.wingedsheep.sdk.scripting.DistributedCounterRemoval> {
        val payment = action.additionalCostPayment ?: return emptyList()
        return payment.distributedCounterRemovals
    }

    private fun resolveAdditionalCostsForMode(
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        action: CastSpell
    ): List<AdditionalCost> {
        if (action.chosenModes.isEmpty()) return cardDef.script.additionalCosts
        val modalEffect = cardDef.script.spellEffect as? ModalEffect ?: return cardDef.script.additionalCosts

        val perModeOverrides = action.chosenModes.mapNotNull { modeIndex ->
            modalEffect.modes.getOrNull(modeIndex)?.additionalCosts
        }
        if (perModeOverrides.isEmpty()) return cardDef.script.additionalCosts
        return perModeOverrides.flatten()
    }

    private fun validateAdditionalCosts(
        state: GameState,
        additionalCosts: List<AdditionalCost>,
        action: CastSpell
    ): String? {
        val projected = state.projectedState
        val flattenedCosts = additionalCosts.flatMap {
            if (it is AdditionalCost.Composite) it.steps else listOf(it)
        }
        for (additionalCost in flattenedCosts) {
            when (additionalCost) {
                is AdditionalCost.Atom -> when (val atom = additionalCost.atom) {
                    is CostAtom.Sacrifice -> {
                        val sacrificed = action.additionalCostPayment?.sacrificedPermanents ?: emptyList()
                        val filterDesc = atom.filter.description
                        if (sacrificed.size < atom.count) {
                            return "You must sacrifice ${atom.count} $filterDesc to cast this spell"
                        }
                        for (permId in sacrificed) {
                            val permContainer = state.getEntity(permId)
                                ?: return "Sacrificed permanent not found: $permId"
                            val permCard = permContainer.get<CardComponent>()
                                ?: return "Sacrificed entity is not a card: $permId"
                            val permController = projected.getController(permId)
                            if (permController != action.playerId) {
                                return "You can only sacrifice permanents you control"
                            }
                            if (permId !in state.getBattlefield()) {
                                return "Sacrificed permanent is not on the battlefield: $permId"
                            }
                            // Use unified filter with projected state
                            val context = PredicateContext(controllerId = action.playerId)
                            val matches = predicateEvaluator.matches(state, projected, permId, atom.filter, context)
                            if (!matches) {
                                return "${permCard.name} doesn't match the required filter: $filterDesc"
                            }
                        }
                    }
                    is CostAtom.ExileFrom -> {
                        val exiled = action.additionalCostPayment?.exiledCards ?: emptyList()
                        val zoneDesc = atom.zone.name.lowercase()
                        if (exiled.size < atom.count) {
                            return "You must exile ${atom.count} ${atom.filter.description}(s) from your $zoneDesc"
                        }
                        val zoneCards = state.getZone(ZoneKey(action.playerId, atom.zone))
                        val context = PredicateContext(controllerId = action.playerId)
                        for (cardId in exiled) {
                            if (cardId !in zoneCards) {
                                return "Card to exile is not in your $zoneDesc"
                            }
                            if (!predicateEvaluator.matches(state, projected, cardId, atom.filter, context)) {
                                val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                                return "$cardName doesn't match the required filter: ${atom.filter.description}"
                            }
                        }
                    }
                    is CostAtom.Discard -> {
                        val discarded = action.additionalCostPayment?.discardedCards ?: emptyList()
                        if (discarded.size < atom.count) {
                            return "You must discard ${atom.count} card(s) to cast this spell"
                        }
                        val handCards = state.getZone(ZoneKey(action.playerId, Zone.HAND))
                        val context = PredicateContext(controllerId = action.playerId)
                        for (cardId in discarded) {
                            if (cardId !in handCards) {
                                return "Card to discard is not in your hand"
                            }
                            if (cardId == action.cardId) {
                                return "Cannot discard the spell being cast"
                            }
                            if (atom.filter != com.wingedsheep.sdk.scripting.GameObjectFilter.Any) {
                                if (!predicateEvaluator.matches(state, state.projectedState, cardId, atom.filter, context)) {
                                    val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                                    return "$cardName doesn't match the required filter: ${atom.filter.description}"
                                }
                            }
                        }
                    }
                    is CostAtom.TapPermanents -> {
                        val tapped = action.additionalCostPayment?.tappedPermanents ?: emptyList()
                        if (tapped.size < atom.count) {
                            return "You must tap ${atom.count} ${atom.filter.description}(s) to cast this spell"
                        }
                        val context = PredicateContext(controllerId = action.playerId)
                        for (permId in tapped) {
                            val permContainer = state.getEntity(permId)
                                ?: return "Tapped permanent not found: $permId"
                            val permCard = permContainer.get<CardComponent>()
                                ?: return "Tapped entity is not a card: $permId"
                            val permController = projected.getController(permId)
                            if (permController != action.playerId) {
                                return "You can only tap permanents you control"
                            }
                            if (permContainer.has<TappedComponent>()) {
                                return "${permCard.name} is already tapped"
                            }
                            if (permId !in state.getBattlefield()) {
                                return "Tapped permanent is not on the battlefield: $permId"
                            }
                            val matches = predicateEvaluator.matches(state, projected, permId, atom.filter, context)
                            if (!matches) {
                                return "${permCard.name} doesn't match the required filter: ${atom.filter.description}"
                            }
                        }
                    }
                    is CostAtom.PayLife -> {
                        val currentLife = state.lifeTotal(action.playerId) // CR 810.9a — team's shared total
                        // CR 119.4 — you can't pay life unless you have at least that much
                        if (currentLife < atom.amount) {
                            return "Not enough life to pay ${atom.amount} life"
                        }
                    }
                    is CostAtom.ReturnToHand -> {
                        val bounced = action.additionalCostPayment?.bouncedPermanents ?: emptyList()
                        if (bounced.size < atom.count) {
                            return "You must return ${atom.count} ${atom.filter.description}(s) you control to its owner's hand to cast this spell"
                        }
                        val context = PredicateContext(controllerId = action.playerId)
                        for (permId in bounced) {
                            val permContainer = state.getEntity(permId)
                                ?: return "Returned permanent not found: $permId"
                            val permCard = permContainer.get<CardComponent>()
                                ?: return "Returned entity is not a card: $permId"
                            val permController = projected.getController(permId)
                            if (permController != action.playerId) {
                                return "You can only return permanents you control"
                            }
                            if (permId !in state.getBattlefield()) {
                                return "Returned permanent is not on the battlefield: $permId"
                            }
                            val matches = predicateEvaluator.matches(state, projected, permId, atom.filter, context)
                            if (!matches) {
                                return "${permCard.name} doesn't match the required filter: ${atom.filter.description}"
                            }
                        }
                    }
                    // Mana / reveal are not produced as spell additional costs today.
                    is CostAtom.Mana, is CostAtom.RevealFromHand -> {}
                }
                is AdditionalCost.ExileVariableCards -> {
                    val exiled = action.additionalCostPayment?.exiledCards ?: emptyList()
                    if (exiled.size < additionalCost.minCount) {
                        return "You must exile at least ${additionalCost.minCount} ${additionalCost.filter.description}(s) from your ${additionalCost.fromZone.description}"
                    }
                    val zone = when (additionalCost.fromZone) {
                        com.wingedsheep.sdk.scripting.CostZone.GRAVEYARD -> Zone.GRAVEYARD
                        com.wingedsheep.sdk.scripting.CostZone.HAND -> Zone.HAND
                        com.wingedsheep.sdk.scripting.CostZone.LIBRARY -> Zone.LIBRARY
                        com.wingedsheep.sdk.scripting.CostZone.BATTLEFIELD -> Zone.BATTLEFIELD
                    }
                    val zoneKey = ZoneKey(action.playerId, zone)
                    val zoneCards = state.getZone(zoneKey)
                    val context = PredicateContext(controllerId = action.playerId)
                    for (cardId in exiled) {
                        if (cardId !in zoneCards) {
                            return "Card to exile is not in your ${additionalCost.fromZone.description}"
                        }
                        if (!predicateEvaluator.matches(state, projected, cardId, additionalCost.filter, context)) {
                            val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                            return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                        }
                    }
                }
                is AdditionalCost.SacrificeCreaturesForCostReduction -> {
                    // Sacrificing 0 creatures is valid (optional sacrifice)
                    val sacrificed = action.additionalCostPayment?.sacrificedPermanents ?: emptyList()
                    for (permId in sacrificed) {
                        val permContainer = state.getEntity(permId)
                            ?: return "Sacrificed permanent not found: $permId"
                        val permCard = permContainer.get<CardComponent>()
                            ?: return "Sacrificed entity is not a card: $permId"
                        val permController = projected.getController(permId)
                        if (permController != action.playerId) {
                            return "You can only sacrifice permanents you control"
                        }
                        if (permId !in state.getBattlefield()) {
                            return "Sacrificed permanent is not on the battlefield: $permId"
                        }
                        val context = PredicateContext(controllerId = action.playerId)
                        val matches = predicateEvaluator.matches(state, projected, permId, additionalCost.filter, context)
                        if (!matches) {
                            return "${permCard.name} doesn't match the required filter: ${additionalCost.filter.description}"
                        }
                    }
                }
                is AdditionalCost.Behold -> {
                    val chosen = action.additionalCostPayment?.beheldCards ?: emptyList()
                    if (chosen.size < additionalCost.count) {
                        return "You must behold ${additionalCost.count} ${additionalCost.filter.description}(s)"
                    }
                    val handZone = ZoneKey(action.playerId, Zone.HAND)
                    val handCards = state.getZone(handZone)
                    val battlefieldCards = state.getBattlefield()
                    val context = PredicateContext(controllerId = action.playerId)
                    for (cardId in chosen) {
                        val inHand = cardId in handCards && cardId != action.cardId
                        val onBattlefield = cardId in battlefieldCards &&
                            projected.getController(cardId) == action.playerId
                        if (!inHand && !onBattlefield) {
                            return "Beheld card must be a card in your hand or a permanent you control"
                        }
                        if (onBattlefield) {
                            if (!predicateEvaluator.matches(state, projected, cardId, additionalCost.filter, context)) {
                                val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                                return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                            }
                        } else {
                            if (!predicateEvaluator.matches(state, state.projectedState, cardId, additionalCost.filter, context)) {
                                val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                                return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                            }
                        }
                    }
                }
                is AdditionalCost.ExileFromStorage -> {
                    // Validated by the preceding Behold cost — nothing extra needed
                }
                is AdditionalCost.BlightOrPay -> {
                    // BlightOrPay: player chose blight if blightTargets is non-empty,
                    // otherwise chose to pay extra mana (validated via mana payment)
                    val blightTargets = action.additionalCostPayment?.blightTargets ?: emptyList()
                    if (blightTargets.isNotEmpty()) {
                        // Validate the blight target
                        val targetId = blightTargets.first()
                        val container = state.getEntity(targetId)
                            ?: return "Blight target not found: $targetId"
                        container.get<CardComponent>()
                            ?: return "Blight target is not a card: $targetId"
                        val controller = projected.getController(targetId)
                        if (controller != action.playerId) {
                            return "You can only blight creatures you control"
                        }
                        if (targetId !in state.getBattlefield()) {
                            return "Blight target is not on the battlefield: $targetId"
                        }
                        if (!projected.isCreature(targetId)) {
                            return "Blight target must be a creature"
                        }
                        if (!projected.canReceiveCounters(targetId)) {
                            return "Blight target can't have counters put on it"
                        }
                    }
                    // If blightTargets is empty, the player is paying extra mana instead
                }
                is AdditionalCost.BlightVariable -> {
                    val amount = action.additionalCostPayment?.blightAmount ?: 0
                    if (amount < additionalCost.minCount) {
                        return "Blight X must be at least ${additionalCost.minCount} (got $amount)"
                    }
                    if (amount < 0) {
                        return "Blight X cannot be negative"
                    }
                    val maxToughness = state.getBattlefield()
                        .filter { permId ->
                            projected.getController(permId) == action.playerId &&
                                projected.isCreature(permId) &&
                                projected.canReceiveCounters(permId)
                        }
                        .maxOfOrNull { projected.getToughness(it) ?: 0 } ?: 0
                    if (amount > maxToughness) {
                        return "Blight X ($amount) cannot exceed the greatest toughness among creatures you control ($maxToughness)"
                    }
                    if (amount > 0) {
                        val blightTargets = action.additionalCostPayment?.blightTargets ?: emptyList()
                        if (blightTargets.size != 1) {
                            return "Blight X must target exactly one creature you control"
                        }
                        val targetId = blightTargets.first()
                        val container = state.getEntity(targetId)
                            ?: return "Blight target not found: $targetId"
                        container.get<CardComponent>()
                            ?: return "Blight target is not a card: $targetId"
                        if (projected.getController(targetId) != action.playerId) {
                            return "You can only blight creatures you control"
                        }
                        if (targetId !in state.getBattlefield()) {
                            return "Blight target is not on the battlefield: $targetId"
                        }
                        if (!projected.isCreature(targetId)) {
                            return "Blight target must be a creature"
                        }
                        if (!projected.canReceiveCounters(targetId)) {
                            return "Blight target can't have counters put on it"
                        }
                    }
                }
                is AdditionalCost.PayXLife -> {
                    val amount = action.additionalCostPayment?.payXLifeAmount ?: 0
                    if (amount < additionalCost.minCount) {
                        return "Pay X life: X must be at least ${additionalCost.minCount} (got $amount)"
                    }
                    if (amount < 0) {
                        return "Pay X life: X cannot be negative"
                    }
                    val currentLife = state.lifeTotal(action.playerId)
                    if (amount > currentLife) {
                        return "Pay X life: X ($amount) cannot exceed your life total ($currentLife)"
                    }
                }
                is AdditionalCost.BeholdOrPay -> {
                    // BeholdOrPay: player chose behold if beheldCards is non-empty,
                    // otherwise chose to pay extra mana (validated via mana payment)
                    val beheld = action.additionalCostPayment?.beheldCards ?: emptyList()
                    if (beheld.isNotEmpty()) {
                        val handZone = ZoneKey(action.playerId, Zone.HAND)
                        val handCards = state.getZone(handZone)
                        val battlefieldCards = state.getBattlefield()
                        val context = PredicateContext(controllerId = action.playerId)
                        for (cardId in beheld) {
                            val inHand = cardId in handCards && cardId != action.cardId
                            val onBattlefield = cardId in battlefieldCards &&
                                projected.getController(cardId) == action.playerId
                            if (!inHand && !onBattlefield) {
                                return "Beheld card must be a card in your hand or a permanent you control"
                            }
                            if (onBattlefield) {
                                if (!predicateEvaluator.matches(state, projected, cardId, additionalCost.filter, context)) {
                                    val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                                    return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                                }
                            } else {
                                if (!predicateEvaluator.matches(state, state.projectedState, cardId, additionalCost.filter, context)) {
                                    val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                                    return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                                }
                            }
                        }
                    }
                    // If beheldCards is empty, the player is paying extra mana instead
                }
                is AdditionalCost.ExileFromGraveyardOrPay -> {
                    // ExileFromGraveyardOrPay: player chose the exile path if exiledCards is
                    // non-empty, otherwise they pay extra mana (validated via mana payment).
                    val exiled = action.additionalCostPayment?.exiledCards ?: emptyList()
                    if (exiled.isNotEmpty()) {
                        if (exiled.size != additionalCost.exileCount) {
                            return "You must exile exactly ${additionalCost.exileCount} card(s) from your graveyard"
                        }
                        val graveyard = state.getZone(ZoneKey(action.playerId, Zone.GRAVEYARD))
                        val context = PredicateContext(controllerId = action.playerId)
                        for (cardId in exiled) {
                            if (cardId !in graveyard) {
                                return "Card to exile is not in your graveyard"
                            }
                            if (!predicateEvaluator.matches(state, state.projectedState, cardId, additionalCost.filter, context)) {
                                val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "Card"
                                return "$cardName doesn't match the required filter: ${additionalCost.filter.description}"
                            }
                        }
                    }
                    // If exiledCards is empty, the player is paying extra mana instead
                }
                is AdditionalCost.RemoveCountersFromYourCreatures -> {
                    // Web client sends a typed list of (entity, counterType, count)
                    // entries so the player picks which counter type comes off each
                    // creature; the engine validates totals and per-type availability.
                    val removals = resolveDistributedCounterRemovalsForPayment(action)
                    val total = removals.sumOf { it.count }
                    if (total < additionalCost.totalCount) {
                        return "You must remove ${additionalCost.totalCount} counters from among creatures you control to cast this spell"
                    }
                    // Tally demanded removals per (entity, counterType) so we can validate
                    // against actual counter counts.
                    val demanded = mutableMapOf<Pair<EntityId, CounterType>, Int>()
                    for (removal in removals) {
                        if (removal.count <= 0) {
                            return "Counter removal count must be positive"
                        }
                        val permContainer = state.getEntity(removal.entityId)
                            ?: return "Counter removal target not found: ${removal.entityId}"
                        permContainer.get<CardComponent>()
                            ?: return "Counter removal target is not a card: ${removal.entityId}"
                        if (projected.getController(removal.entityId) != action.playerId) {
                            return "You can only remove counters from creatures you control"
                        }
                        if (removal.entityId !in state.getBattlefield()) {
                            return "Counter removal target is not on the battlefield"
                        }
                        if (!projected.isCreature(removal.entityId)) {
                            return "Counter removal target must be a creature"
                        }
                        val resolvedType =
                            com.wingedsheep.engine.handlers.effects.permanent.counters.resolveCounterType(removal.counterType)
                        val key = removal.entityId to resolvedType
                        demanded[key] = (demanded[key] ?: 0) + removal.count
                    }
                    for ((key, demandedCount) in demanded) {
                        val (entityId, counterType) = key
                        val actual = state.getEntity(entityId)
                            ?.get<CountersComponent>()
                            ?.getCount(counterType) ?: 0
                        if (actual < demandedCount) {
                            return "Creature does not have $demandedCount $counterType counters to remove"
                        }
                    }
                }
                is AdditionalCost.PayLifePerTarget -> {
                    val required = additionalCost.amountPerTarget * action.targets.size
                    val currentLife = state.lifeTotal(action.playerId) // CR 810.9a — team's shared total
                    // CR 119.4 — you can't pay life unless you have at least that much
                    if (currentLife < required) {
                        return "Not enough life to pay $required life for ${action.targets.size} targets"
                    }
                }
                is AdditionalCost.ChooseEntity -> {
                    val chosen = action.additionalCostPayment?.beheldCards ?: emptyList()
                    if (chosen.isEmpty()) {
                        return "You must ${additionalCost.description}"
                    }
                    if (chosen.size > 1) {
                        return "You may only choose one entity for: ${additionalCost.description}"
                    }
                    val entityId = chosen.first()
                    val candidates = costHandler.findChooseEntityCandidates(state, additionalCost, action.playerId)
                    if (entityId !in candidates) {
                        val name = state.getEntity(entityId)?.get<CardComponent>()?.name ?: entityId.toString()
                        return "$name is not a valid choice for: ${additionalCost.description}"
                    }
                }
                else -> {}
            }
        }
        return null
    }

    override fun execute(state: GameState, action: CastSpell): ExecutionResult {
        var currentState = state
        val events = mutableListOf<GameEvent>()

        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Card not found")

        val xValue = action.xValue ?: 0
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        // Rule 400.7: a card that changed zones is a new object. Drop any stale
        // LinkedExileComponent carried over from a previous battlefield visit (e.g.
        // Veteran Survivor bounced to hand, then recast) before additional costs run —
        // a behold-and-exile cost on this same cast will attach a fresh one afterwards.
        currentState = currentState.updateEntity(action.cardId) { c -> c.without<LinkedExileComponent>() }

        // Cast-time mode selection for modal spells (CR 601.2b — the controller announces
        // the mode choice while casting the spell, before it goes on the stack). Must run
        // before cost payment so cancellation leaves no side effects.
        //
        // Applies uniformly to choose-1 and choose-N modal spells. The web client supplies
        // `chosenModes` up front for choose-1 spells (the local mode picker), so it
        // bypasses this pause; synthesized free casts (Sunbird's Invocation, Cascade) and
        // any other server-initiated cast that doesn't pre-supply a mode hits the pause
        // here. The legacy resolution-time mode picker in
        // [com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor] remains
        // for modal *triggered* / *activated* abilities (CR 603.3c), which don't go
        // through the cast pipeline at all.
        val modalEffect = cardDef?.script?.spellEffect as? ModalEffect
        if (modalEffect != null && action.chosenModes.isEmpty() && modalEffect.chooseCount >= 1) {
            return pauseForCastTimeModeSelection(currentState, action, cardComponent, modalEffect)
        }

        // Per-mode target selection for a modal cast whose modes were chosen up front but
        // whose targets were deferred to the engine — the single-panel client mode selector
        // submits `chosenModes` only and lets the server drive on-battlefield targeting. This
        // runs the same per-mode target flow the sequential mode-selection pause transitions
        // into, then re-enters execute() with a fully-populated action so cost payment and
        // stack placement happen exactly once. The choose-1 client path and AI supply flat
        // `targets`, so they skip this and fall through to deriveModeTargetsFromFlat below.
        if (modalEffect != null &&
            action.chosenModes.isNotEmpty() &&
            action.modeTargetsOrdered.isEmpty() &&
            action.targets.isEmpty() &&
            action.chosenModes.any { modalEffect.modes.getOrNull(it)?.targetRequirements?.isNotEmpty() == true }
        ) {
            return presentCastModalTargetDecision(
                state = currentState,
                cardId = action.cardId,
                casterId = action.playerId,
                cardName = cardComponent.name,
                baseCastAction = action,
                modes = modalEffect.modes,
                chosenModeIndices = action.chosenModes,
                resolvedModeTargets = emptyList(),
                currentOrdinal = 0
            )
        }

        // Capture the linked-exile granter (if any) before the cast removes the card from
        // exile — once the spell moves to the stack the LinkedExileComponent lookup would
        // fail, but we still need the entry to enforce once-per-turn marking after a
        // successful cast.
        val linkedExileGranterEntry = zoneResolver.findLinkedExileGranterEntry(currentState, action.playerId, action.cardId)

        // Calculate effective cost (free if PlayWithoutPayingCostComponent is present, or if a
        // MayCastWithoutPayingManaCost battlefield source (e.g. Weftwalking) is the chosen alt).
        // Mutual-exclusion + gate already enforced in validate().
        val playForFreeFromComponentExecute = zoneResolver.hasPlayWithoutPayingCost(currentState, action.playerId, action.cardId)
        val playForFreeInExecute = playForFreeFromComponentExecute || action.useWithoutPayingManaCost
        // Split-layout (CR 709.3a) — see validate() for the rationale. Mirror the override here.
        val faceManaCostOverrideExecute: ManaCost? = action.faceIndex?.let { idx ->
            cardDef?.cardFaces?.getOrNull(idx)?.manaCost
        }
        var effectiveCost = if (playForFreeInExecute) {
            ManaCost.ZERO
        } else if (faceManaCostOverrideExecute != null && cardDef != null) {
            costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, faceManaCostOverrideExecute, action.playerId)
        } else if (action.useAlternativeCost && cardDef != null) {
            // Check flashback cost first (printed on the card, or granted at runtime by Archmage's Newt).
            val flashbackAbility = FlashbackGrants.effectiveFlashback(currentState, action.cardId, cardDef)
            // Harmonize may be printed on the card or granted at runtime (Songcrafter Mage).
            val harmonizeAbility = HarmonizeGrants.effectiveHarmonize(currentState, action.cardId, cardDef)
            // Branches gated by [CastSpell.altAllows] — mirrors validate(); honors the player's
            // explicit alternative-cost choice instead of a fixed priority order.
            if (action.altAllows(AlternativeCostType.FLASHBACK) && flashbackAbility != null && zoneResolver.hasFlashbackPermission(currentState, action.playerId, action.cardId)) {
                costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, flashbackAbility.cost, action.playerId)
            } else if (action.altAllows(AlternativeCostType.HARMONIZE) && harmonizeAbility != null && zoneResolver.hasHarmonizePermission(currentState, action.playerId, action.cardId)) {
                costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, harmonizeAbility.cost, action.playerId)
            } else {
                // Check warp cost (hand only — CR 702.185a). Re-casts from exile pay the regular
                // mana cost. Printed warp wins; a battlefield grant ([GrantWarpToCardsInHand])
                // supplies the cost when the card has no printed warp.
                val warpAbility = WarpGrants.effectiveWarp(
                    currentState, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator
                )
                if (action.altAllows(AlternativeCostType.WARP) && warpAbility != null && zoneResolver.hasWarpPermission(currentState, action.playerId, action.cardId)) {
                    costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, warpAbility.cost, action.playerId)
                } else {
                    // Check sneak cost (CR 702.190 — mana portion; the bounce is paid separately)
                    val sneakAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Sneak>().firstOrNull()
                    // Check evoke cost
                    val evokeAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Evoke>().firstOrNull()
                    if (action.altAllows(AlternativeCostType.SNEAK) && sneakAbility != null) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, sneakAbility.cost, action.playerId)
                    } else if (action.altAllows(AlternativeCostType.EVOKE) && evokeAbility != null) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, evokeAbility.cost, action.playerId)
                    } else {
                        // Check impending cost
                        val impendingAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Impending>().firstOrNull()
                        // Check miracle cost (CR 702.94 — printed or granted in hand, window-gated).
                        val miracleWindowOpen = currentState.getEntity(action.cardId)
                            ?.has<com.wingedsheep.engine.state.components.identity.MiracleWindowComponent>() == true
                        val miracleAbility = if (miracleWindowOpen) MiracleGrants.effectiveMiracle(
                            currentState, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator
                        ) else null
                        if (action.altAllows(AlternativeCostType.IMPENDING) && impendingAbility != null) {
                            costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, impendingAbility.cost, action.playerId)
                        } else if (action.altAllows(AlternativeCostType.MIRACLE) && miracleAbility != null) {
                            costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, miracleAbility.cost, action.playerId)
                        } else {
                            val selfAltCost = cardDef.script.selfAlternativeCost
                            if (action.altAllows(AlternativeCostType.SELF_ALTERNATIVE) && selfAltCost != null) {
                                val altMana = selfAltCost.manaCost
                                costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, altMana, action.playerId)
                            } else {
                                val altCosts = costCalculator.findAlternativeCastingCosts(currentState, action.playerId)
                                if (altCosts.isNotEmpty()) {
                                    costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, altCosts.first())
                                } else {
                                    cardComponent.manaCost
                                }
                            }
                        }
                    }
                }
            }
        } else if (action.castFaceDown) {
            costCalculator.calculateFaceDownCost(currentState, action.playerId)
        } else if (cardDef != null) {
            // Detect cast-from-command-zone for commander tax (CR 903.8). The card may have moved
            // out of the command zone in `state` between validate() and here, but `currentState`
            // still has it because we haven't called `castSpell` yet.
            val castingFromCommand = zoneResolver.hasCommanderCastPermission(
                currentState, action.playerId, action.cardId,
            )
            costCalculator.calculateEffectiveCost(
                currentState,
                cardDef,
                action.playerId,
                action.targets.map { it.toEntityId() },
                fromZone = if (castingFromCommand) Zone.COMMAND else castSourceZone(currentState, action.cardId),
            )
        } else {
            cardComponent.manaCost
        }

        // Add kicker/offspring cost if kicked (not applicable with alternative costs)
        if (action.wasKicked && !playForFreeInExecute && !action.useAlternativeCost && cardDef != null) {
            val kickerManaCost = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.OptionalAdditionalCost>()
                .firstOrNull { it.manaCost != null }
                ?.manaCost
            if (kickerManaCost != null) {
                effectiveCost = ManaCost(effectiveCost.symbols + kickerManaCost.symbols)
            }
        }

        // Apply per-mode additional mana cost (e.g., Feed the Cycle "pay {B}" mode).
        // With choose-N (rule 700.2h), the additional mana cost of every chosen mode stacks.
        if (cardDef != null && action.chosenModes.isNotEmpty()) {
            val modalEffect = cardDef.script.spellEffect as? ModalEffect
            if (modalEffect != null) {
                for (modeIndex in action.chosenModes) {
                    val modeManaCost = modalEffect.modes.getOrNull(modeIndex)?.additionalManaCost ?: continue
                    effectiveCost = effectiveCost + ManaCost.parse(modeManaCost)
                }
            }
        }

        // Apply BlightOrPay: if player chose "pay mana" path (no blight targets), add extra mana
        if (cardDef != null && !playForFreeInExecute) {
            val blightOrPay = cardDef.script.additionalCosts
                .filterIsInstance<AdditionalCost.BlightOrPay>()
                .firstOrNull()
            if (blightOrPay != null) {
                val choseBlight = action.additionalCostPayment?.blightTargets?.isNotEmpty() == true
                if (!choseBlight) {
                    effectiveCost = effectiveCost + ManaCost.parse(blightOrPay.alternativeManaCost)
                }
            }
        }

        // Apply BeholdOrPay: if player chose "pay mana" path (no beheld cards), add extra mana
        if (cardDef != null && !playForFreeInExecute) {
            val beholdOrPay = cardDef.script.additionalCosts
                .filterIsInstance<AdditionalCost.BeholdOrPay>()
                .firstOrNull()
            if (beholdOrPay != null) {
                val choseBehold = action.additionalCostPayment?.beheldCards?.isNotEmpty() == true
                if (!choseBehold) {
                    effectiveCost = effectiveCost + ManaCost.parse(beholdOrPay.alternativeManaCost)
                }
            }
        }

        // Apply ExileFromGraveyardOrPay: if player chose the "pay mana" path (no exiled cards),
        // add the extra mana on top of the base cost.
        if (cardDef != null && !playForFreeInExecute) {
            val exileOrPay = cardDef.script.additionalCosts
                .filterIsInstance<AdditionalCost.ExileFromGraveyardOrPay>()
                .firstOrNull()
            if (exileOrPay != null) {
                val choseExile = action.additionalCostPayment?.exiledCards?.isNotEmpty() == true
                if (!choseExile) {
                    effectiveCost = effectiveCost + ManaCost.parse(exileOrPay.alternativeManaCost)
                }
            }
        }

        // Apply runtime mana tax from exile permissions (e.g., Soul Partition).
        if (!playForFreeInExecute) {
            val runtimeCostIncrease = currentState.getEntity(action.cardId)
                ?.get<PlayWithCostIncreaseComponent>()
                ?.takeIf { it.controllerId == action.playerId }
            if (runtimeCostIncrease != null) {
                effectiveCost = effectiveCost + ManaCost.parse("{${runtimeCostIncrease.amount}}")
            }
        }

        // Process additional costs (sacrifice, exile, etc.)
        val sacrificedSnapshots = mutableListOf<PermanentSnapshot>()
        var exiledCardCount = 0
        val beheldCards = mutableListOf<EntityId>()
        /**
         * LKI snapshots for entities chosen via [AdditionalCost.ChooseEntity] when
         * `captureSnapshot = true`. Captured at cost-pay time so downstream effects
         * (e.g. `EntityProperty(FromCostStorage(...), Power)`) can read "power as it
         * last existed on the battlefield" if the chosen entity leaves before resolution.
         */
        val chosenEntitySnapshots = mutableListOf<PermanentSnapshot>()
        /** Pipeline storage populated by Behold, consumed by ExileFromStorage */
        val costPipelineCollections = mutableMapOf<String, List<EntityId>>()

        // Collect all additional costs: script costs + kicker additional cost (if kicked)
        // + self-alternative cost's additional costs (if using alternative cost)
        // + runtime additional costs from PlayWithAdditionalCostComponent
        // Per-mode additional costs override card-level costs when present
        val allAdditionalCosts = buildList {
            if (cardDef != null) addAll(resolveAdditionalCostsForMode(cardDef, action))
            if (action.wasKicked && cardDef != null) {
                val kickerAdditionalCost = cardDef.keywordAbilities
                    .filterIsInstance<KeywordAbility.OptionalAdditionalCost>()
                    .firstOrNull { it.additionalCost != null }
                    ?.additionalCost
                if (kickerAdditionalCost != null) add(kickerAdditionalCost)
            }
            if (action.useAlternativeCost && cardDef != null) {
                // Each bundled additional cost is gated by the chosen alternative-cost type so a
                // collision (e.g. granted warp on a card also being evoked) doesn't drag in the
                // unchosen cost's bundled additional cost.
                val selfAltCost = cardDef.script.selfAlternativeCost
                if (selfAltCost != null && action.altAllows(AlternativeCostType.SELF_ALTERNATIVE)) addAll(selfAltCost.additionalCosts)
                // Flashback's bundled additional cost (e.g., Behold three Elementals)
                if (action.altAllows(AlternativeCostType.FLASHBACK) &&
                    zoneResolver.hasFlashbackPermission(currentState, action.playerId, action.cardId)) {
                    val flashbackAdditional = cardDef.keywordAbilities
                        .filterIsInstance<KeywordAbility.Flashback>()
                        .firstOrNull()
                        ?.additionalCost
                    if (flashbackAdditional != null) add(flashbackAdditional)
                }
                // Warp's bundled additional cost (e.g., "Pay 2 life" on Timeline Culler). Use
                // [WarpGrants] so granted warps ([GrantWarpToCardsInHand]) participate too —
                // currently they carry no additional cost, but routing through the same helper
                // keeps the seam.
                if (action.altAllows(AlternativeCostType.WARP) &&
                    zoneResolver.hasWarpPermission(currentState, action.playerId, action.cardId)) {
                    val warpAdditional = WarpGrants.effectiveWarp(
                        currentState, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator
                    )?.additionalCost
                    if (warpAdditional != null) add(warpAdditional)
                }
            }
            // Runtime additional costs from entity component (e.g., The Infamous Cruelclaw)
            val runtimeCostComp = currentState.getEntity(action.cardId)
                ?.get<PlayWithAdditionalCostComponent>()
                ?.takeIf { it.controllerId == action.playerId }
            if (runtimeCostComp != null) addAll(runtimeCostComp.additionalCosts)

            // Linked-exile granter additional cost (e.g., Dawnhand Dissident's
            // "remove three counters from among creatures you control")
            val linkedGranter = zoneResolver.findLinkedExileGranter(currentState, action.playerId, action.cardId)
            linkedGranter?.additionalCost?.let { add(it) }
        }

        val flattenedAllCosts = allAdditionalCosts.flatMap {
            if (it is AdditionalCost.Composite) it.steps else listOf(it)
        }

        // Server-initiated free cast: pay the spell's printed additional costs even though the
        // mana cost is waived (CR 601.2f / 118.9). A normal client cast arrives with the
        // selections already in `additionalCostPayment` (validated in validate()); copy-and-cast
        // pipelines (Roving Actuator, Shiko, Cascade) call execute() directly with no payment, so
        // we surface the selection here. The pause sits before any cost is paid, so the re-entry
        // on resume (with the chosen entities merged into the payment) is side-effect free. Returns
        // null when every selection-requiring cost is already satisfied — the normal path.
        surfaceUnpaidAdditionalCostSelection(currentState, action, flattenedAllCosts)?.let { return it }

        // PayLife additional costs (e.g., Timeline Culler's "Warp—{B}, Pay 2 life")
        // are auto-paid: the amount is fixed, so no player choice is required and the
        // payment is applied regardless of whether the client included an
        // AdditionalCostPayment object.
        for (additionalCost in flattenedAllCosts) {
            val atom = (additionalCost as? AdditionalCost.Atom)?.atom
            val lifeToPay = when {
                atom is CostAtom.PayLife -> atom.amount
                additionalCost is AdditionalCost.PayLifePerTarget -> additionalCost.amountPerTarget * action.targets.size
                additionalCost is AdditionalCost.PayXLife -> action.additionalCostPayment?.payXLifeAmount ?: 0
                else -> continue
            }
            if (lifeToPay == 0) continue
            val currentLife = currentState.lifeTotal(action.playerId) // CR 810.9a — team's shared total
            val newLife = currentLife - lifeToPay
            currentState = currentState.withLifeTotal(action.playerId, newLife)
            currentState = DamageUtils.markLifeLostThisTurn(currentState, action.playerId)
            events.add(LifeChangedEvent(action.playerId, currentLife, newLife, LifeChangeReason.PAYMENT))
        }
        if (flattenedAllCosts.isNotEmpty() && action.additionalCostPayment != null) {
            for (additionalCost in flattenedAllCosts) {
                when (additionalCost) {
                    is AdditionalCost.Atom -> when (val atom = additionalCost.atom) {
                        is CostAtom.Sacrifice -> {
                            // Snapshot projected subtypes and P/T before zone change
                            // (Rule 112.7a / 608.2h — "as it last existed on the battlefield")
                            val projectedBeforeSacrifice = currentState.projectedState
                            sacrificedSnapshots.addAll(
                                capturePermanentSnapshots(action.additionalCostPayment.sacrificedPermanents, projectedBeforeSacrifice)
                            )
                            for (permId in action.additionalCostPayment.sacrificedPermanents) {
                                val permContainer = currentState.getEntity(permId) ?: continue
                                val permCard = permContainer.get<CardComponent>() ?: continue
                                val controllerId = permContainer.get<ControllerComponent>()?.playerId ?: action.playerId
                                val ownerId = permCard.ownerId ?: action.playerId
                                val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                                val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

                                currentState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                                    .trackPermanentSacrifice(currentState, listOf(permId), action.playerId)
                                currentState = currentState.removeFromZone(battlefieldZone, permId)
                                currentState = currentState.addToZone(graveyardZone, permId)

                                events.add(PermanentsSacrificedEvent(action.playerId, listOf(permId), listOf(permCard.name)))
                                events.add(ZoneChangeEvent(
                                    entityId = permId,
                                    entityName = permCard.name,
                                    fromZone = Zone.BATTLEFIELD,
                                    toZone = Zone.GRAVEYARD,
                                    ownerId = ownerId
                                ))
                            }
                        }
                        is CostAtom.Discard -> {
                            val discardedCards = action.additionalCostPayment.discardedCards
                            for (cardId in discardedCards) {
                                val cardContainer = currentState.getEntity(cardId) ?: continue
                                val card = cardContainer.get<CardComponent>() ?: continue
                                val handZone = ZoneKey(action.playerId, Zone.HAND)
                                val graveyardZone = ZoneKey(action.playerId, Zone.GRAVEYARD)

                                currentState = currentState.removeFromZone(handZone, cardId)
                                currentState = currentState.addToZone(graveyardZone, cardId)

                                events.add(ZoneChangeEvent(
                                    entityId = cardId,
                                    entityName = card.name,
                                    fromZone = Zone.HAND,
                                    toZone = Zone.GRAVEYARD,
                                    ownerId = action.playerId
                                ))
                            }
                            val discardNames = discardedCards.map { currentState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
                            events.add(CardsDiscardedEvent(action.playerId, discardedCards, discardNames))
                        }
                        is CostAtom.ExileFrom -> {
                            val exiledCards = action.additionalCostPayment.exiledCards
                            for (cardId in exiledCards) {
                                val cardContainer = currentState.getEntity(cardId) ?: continue
                                val card = cardContainer.get<CardComponent>() ?: continue
                                val sourceZone = ZoneKey(action.playerId, atom.zone)
                                val exileZone = ZoneKey(action.playerId, Zone.EXILE)

                                currentState = currentState.removeFromZone(sourceZone, cardId)
                                currentState = currentState.addToZone(exileZone, cardId)

                                events.add(ZoneChangeEvent(
                                    entityId = cardId,
                                    entityName = card.name,
                                    fromZone = atom.zone,
                                    toZone = Zone.EXILE,
                                    ownerId = action.playerId
                                ))
                            }
                            exiledCardCount = exiledCards.size
                        }
                        is CostAtom.TapPermanents -> {
                            // Tap permanents as additional cost (e.g., Zahid's tap an artifact)
                            val tappedPerms = action.additionalCostPayment.tappedPermanents
                            for (permId in tappedPerms) {
                                val permContainer = currentState.getEntity(permId) ?: continue
                                if (!permContainer.has<TappedComponent>()) {
                                    currentState = currentState.updateEntity(permId) { c ->
                                        c.with(TappedComponent)
                                    }
                                    val permCard = permContainer.get<CardComponent>()
                                    events.add(TappedEvent(permId, permCard?.name ?: "Permanent"))
                                }
                            }
                        }
                        is CostAtom.ReturnToHand -> {
                            // Return permanents you control to their owner's hand as an additional
                            // cost (e.g., Fear of Isolation). ZoneTransitionService.moveToZone
                            // handles attached auras/equipment and tokens ceasing to exist.
                            for (permId in action.additionalCostPayment.bouncedPermanents) {
                                val tr = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                                    .moveToZone(currentState, permId, Zone.HAND)
                                currentState = tr.state
                                events.addAll(tr.events)
                            }
                        }
                        // PayLife is auto-paid in the loop above; mana / reveal aren't spell additional costs.
                        is CostAtom.PayLife, is CostAtom.Mana, is CostAtom.RevealFromHand -> {}
                    }
                    is AdditionalCost.ExileVariableCards -> {
                        val exiledCards = action.additionalCostPayment.exiledCards
                        val zone = additionalCost.fromZone.toZone()
                        for (cardId in exiledCards) {
                            val cardContainer = currentState.getEntity(cardId) ?: continue
                            val card = cardContainer.get<CardComponent>() ?: continue
                            val sourceZone = ZoneKey(action.playerId, zone)
                            val exileZone = ZoneKey(action.playerId, Zone.EXILE)

                            currentState = currentState.removeFromZone(sourceZone, cardId)
                            currentState = currentState.addToZone(exileZone, cardId)

                            events.add(ZoneChangeEvent(
                                entityId = cardId,
                                entityName = card.name,
                                fromZone = zone,
                                toZone = Zone.EXILE,
                                ownerId = action.playerId
                            ))
                        }
                        exiledCardCount = exiledCards.size
                    }
                    is AdditionalCost.SacrificeCreaturesForCostReduction -> {
                        // Process sacrifices for cost reduction (e.g., Torgaar)
                        val projectedBeforeSacrifice = currentState.projectedState
                        sacrificedSnapshots.addAll(
                            capturePermanentSnapshots(action.additionalCostPayment.sacrificedPermanents, projectedBeforeSacrifice)
                        )
                        for (permId in action.additionalCostPayment.sacrificedPermanents) {
                            val permContainer = currentState.getEntity(permId) ?: continue
                            val permCard = permContainer.get<CardComponent>() ?: continue
                            val controllerId = permContainer.get<ControllerComponent>()?.playerId ?: action.playerId
                            val ownerId = permCard.ownerId ?: action.playerId
                            val battlefieldZone = ZoneKey(controllerId, Zone.BATTLEFIELD)
                            val graveyardZone = ZoneKey(ownerId, Zone.GRAVEYARD)

                            currentState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                                .trackPermanentSacrifice(currentState, listOf(permId), action.playerId)
                            currentState = currentState.removeFromZone(battlefieldZone, permId)
                            currentState = currentState.addToZone(graveyardZone, permId)

                            events.add(PermanentsSacrificedEvent(action.playerId, listOf(permId), listOf(permCard.name)))
                            events.add(ZoneChangeEvent(
                                entityId = permId,
                                entityName = permCard.name,
                                fromZone = Zone.BATTLEFIELD,
                                toZone = Zone.GRAVEYARD,
                                ownerId = ownerId
                            ))
                        }
                        // Apply cost reduction based on number of creatures sacrificed
                        val reduction = action.additionalCostPayment.sacrificedPermanents.size * additionalCost.costReductionPerCreature
                        if (reduction > 0) {
                            effectiveCost = effectiveCost.reduceGeneric(reduction)
                        }
                    }
                    is AdditionalCost.Behold -> {
                        // Store beheld card IDs in pipeline for downstream costs/effects
                        val chosen = action.additionalCostPayment.beheldCards
                        beheldCards.addAll(chosen)
                        costPipelineCollections[additionalCost.storeAs] = chosen

                        // Behold reveals the chosen card(s) to all players
                        if (chosen.isNotEmpty()) {
                            val cardNames = chosen.mapNotNull { currentState.getEntity(it)?.get<CardComponent>()?.name }
                            val imageUris = chosen.map { id ->
                                val defId = currentState.getEntity(id)?.get<CardComponent>()?.cardDefinitionId
                                defId?.let { cardRegistry.getCard(it)?.metadata?.imageUri }
                            }
                            val battlefield = currentState.getBattlefield()
                            val anyOnBattlefield = chosen.any { it in battlefield }
                            events.add(CardsRevealedEvent(
                                revealingPlayerId = action.playerId,
                                cardIds = chosen,
                                cardNames = cardNames,
                                imageUris = imageUris,
                                source = cardComponent.name,
                                // Deliver to the revealing player when the beheld card is on the
                                // battlefield (public info) so their client can show the behold
                                // pulse. Suppress when revealing from hand — the caster already
                                // knows and the reveal overlay would be redundant.
                                revealToSelf = anyOnBattlefield
                            ))
                        }
                    }
                    is AdditionalCost.ExileFromStorage -> {
                        // Exile cards from pipeline collection (e.g., beheld cards)
                        val cardsToExile = costPipelineCollections[additionalCost.from] ?: emptyList()
                        for (cardId in cardsToExile) {
                            val cardContainer = currentState.getEntity(cardId) ?: continue
                            val card = cardContainer.get<CardComponent>() ?: continue

                            // Determine source zone (could be battlefield or hand)
                            val controllerId = cardContainer.get<ControllerComponent>()?.playerId ?: action.playerId
                            val ownerId = card.ownerId ?: action.playerId
                            val sourceZone = if (cardId in currentState.getBattlefield()) {
                                ZoneKey(controllerId, Zone.BATTLEFIELD)
                            } else {
                                ZoneKey(action.playerId, Zone.HAND)
                            }
                            val exileZone = ZoneKey(ownerId, Zone.EXILE)

                            currentState = currentState.removeFromZone(sourceZone, cardId)
                            currentState = currentState.addToZone(exileZone, cardId)

                            events.add(ZoneChangeEvent(
                                entityId = cardId,
                                entityName = card.name,
                                fromZone = sourceZone.zoneType,
                                toZone = Zone.EXILE,
                                ownerId = ownerId
                            ))
                        }
                        // Link exiled cards to spell entity for LTB triggers
                        if (additionalCost.linkToSource && cardsToExile.isNotEmpty()) {
                            currentState = currentState.updateEntity(action.cardId) { c ->
                                c.with(com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent(
                                    exiledIds = cardsToExile
                                ))
                            }
                        }
                    }
                    is AdditionalCost.BlightOrPay -> {
                        // Apply -1/-1 counters if the player chose the blight path
                        val blightTargets = action.additionalCostPayment.blightTargets
                        if (blightTargets.isNotEmpty()) {
                            val targetId = blightTargets.first()
                            val targetContainer = currentState.getEntity(targetId)
                            if (targetContainer != null) {
                                val counters = targetContainer.get<CountersComponent>() ?: CountersComponent()
                                val firstThisTurn = DamageUtils.isFirstCounterThisTurn(currentState, targetId)
                                currentState = currentState.updateEntity(targetId) { c ->
                                    c.with(counters.withAdded(CounterType.MINUS_ONE_MINUS_ONE, additionalCost.blightAmount))
                                }
                                currentState = DamageUtils.markCounterPlacedOnCreature(
                                    currentState,
                                    action.playerId,
                                    targetId
                                )
                                val targetName = targetContainer.get<CardComponent>()?.name ?: "Creature"
                                events.add(CountersAddedEvent(
                                    entityId = targetId,
                                    counterType = Counters.MINUS_ONE_MINUS_ONE,
                                    amount = additionalCost.blightAmount,
                                    entityName = targetName,
                                    firstThisTurn = firstThisTurn
                                ))
                            }
                        }
                        // If blightTargets is empty, "pay mana" path — extra mana already added to effectiveCost
                    }
                    is AdditionalCost.BlightVariable -> {
                        val amount = action.additionalCostPayment.blightAmount
                        if (amount > 0) {
                            val targetId = action.additionalCostPayment.blightTargets.firstOrNull()
                            val targetContainer = targetId?.let { currentState.getEntity(it) }
                            if (targetId != null && targetContainer != null) {
                                val counters = targetContainer.get<CountersComponent>() ?: CountersComponent()
                                val firstThisTurn = DamageUtils.isFirstCounterThisTurn(currentState, targetId)
                                currentState = currentState.updateEntity(targetId) { c ->
                                    c.with(counters.withAdded(CounterType.MINUS_ONE_MINUS_ONE, amount))
                                }
                                currentState = DamageUtils.markCounterPlacedOnCreature(
                                    currentState,
                                    action.playerId,
                                    targetId
                                )
                                val targetName = targetContainer.get<CardComponent>()?.name ?: "Creature"
                                events.add(CountersAddedEvent(
                                    entityId = targetId,
                                    counterType = Counters.MINUS_ONE_MINUS_ONE,
                                    amount = amount,
                                    entityName = targetName,
                                    firstThisTurn = firstThisTurn
                                ))
                            }
                        }
                    }
                    is AdditionalCost.BeholdOrPay -> {
                        // Store beheld card IDs in pipeline and reveal them, if behold path chosen
                        val chosen = action.additionalCostPayment.beheldCards
                        if (chosen.isNotEmpty()) {
                            beheldCards.addAll(chosen)
                            costPipelineCollections[additionalCost.storeAs] = chosen

                            val cardNames = chosen.mapNotNull { currentState.getEntity(it)?.get<CardComponent>()?.name }
                            val imageUris = chosen.map { id ->
                                val defId = currentState.getEntity(id)?.get<CardComponent>()?.cardDefinitionId
                                defId?.let { cardRegistry.getCard(it)?.metadata?.imageUri }
                            }
                            val battlefield = currentState.getBattlefield()
                            val anyOnBattlefield = chosen.any { it in battlefield }
                            events.add(CardsRevealedEvent(
                                revealingPlayerId = action.playerId,
                                cardIds = chosen,
                                cardNames = cardNames,
                                imageUris = imageUris,
                                source = cardComponent.name,
                                revealToSelf = anyOnBattlefield
                            ))
                        }
                        // If beheldCards is empty, "pay mana" path — extra mana already added to effectiveCost
                    }
                    is AdditionalCost.ExileFromGraveyardOrPay -> {
                        // Exile the chosen graveyard cards if the player chose the exile path.
                        // If exiledCards is empty, "pay mana" path — extra mana already added above.
                        val exiledCards = action.additionalCostPayment.exiledCards
                        for (cardId in exiledCards) {
                            val cardContainer = currentState.getEntity(cardId) ?: continue
                            val card = cardContainer.get<CardComponent>() ?: continue
                            val sourceZone = ZoneKey(action.playerId, Zone.GRAVEYARD)
                            val exileZone = ZoneKey(action.playerId, Zone.EXILE)

                            currentState = currentState.removeFromZone(sourceZone, cardId)
                            currentState = currentState.addToZone(exileZone, cardId)

                            events.add(ZoneChangeEvent(
                                entityId = cardId,
                                entityName = card.name,
                                fromZone = Zone.GRAVEYARD,
                                toZone = Zone.EXILE,
                                ownerId = action.playerId
                            ))
                        }
                        exiledCardCount += exiledCards.size
                    }
                    is AdditionalCost.RemoveCountersFromYourCreatures -> {
                        // Remove the chosen counters from the designated creatures.
                        // The typed `distributedCounterRemovals` payload tells us
                        // exactly which counter type to take off each creature.
                        val resolvedRemovals = resolveDistributedCounterRemovalsForPayment(action)
                        for (removal in resolvedRemovals) {
                            val container = currentState.getEntity(removal.entityId) ?: continue
                            val existing = container.get<CountersComponent>() ?: continue
                            val resolvedType =
                                com.wingedsheep.engine.handlers.effects.permanent.counters.resolveCounterType(removal.counterType)
                            currentState = currentState.updateEntity(removal.entityId) { c ->
                                c.with(existing.withRemoved(resolvedType, removal.count))
                            }
                            val entityName = container.get<CardComponent>()?.name ?: "Creature"
                            events.add(com.wingedsheep.engine.core.CountersRemovedEvent(
                                entityId = removal.entityId,
                                counterType = removal.counterType,
                                amount = removal.count,
                                entityName = entityName
                            ))
                        }
                    }
                    is AdditionalCost.PayLifePerTarget -> {
                        // Handled in the auto-pay pre-pass above (life total scales with target count).
                    }
                    is AdditionalCost.ChooseEntity -> {
                        // Choosing does not change zones. Record the chosen entity id under
                        // [beheldCards] (shared "chosen-as-additional-cost" storage) and in
                        // the pipeline under [storeAs] so the spell effect can reference it.
                        // When `captureSnapshot` is set, freeze a power/toughness/subtype
                        // snapshot for battlefield choices so downstream effects can fall
                        // back to LKI when the entity leaves between cost-pay and resolution
                        // (Rule 112.7a).
                        val chosen = action.additionalCostPayment.beheldCards
                        if (chosen.isNotEmpty()) {
                            beheldCards.addAll(chosen)
                            costPipelineCollections[additionalCost.storeAs] = chosen
                            if (additionalCost.captureSnapshot) {
                                val battlefieldChosen = chosen.filter { it in currentState.getBattlefield() }
                                if (battlefieldChosen.isNotEmpty()) {
                                    chosenEntitySnapshots.addAll(
                                        capturePermanentSnapshots(battlefieldChosen, currentState.projectedState)
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // Pay Conspire's optional additional cost: tap the two chosen creatures (CR 702.78).
        // Validated in validate(); we just apply the tap and emit TappedEvent so "becomes
        // tapped" self-triggers fire (mirrors the attack-declare TappedEvent fix).
        if (action.conspiredCreatures.isNotEmpty()) {
            for (creatureId in action.conspiredCreatures) {
                val creatureContainer = currentState.getEntity(creatureId) ?: continue
                if (!creatureContainer.has<TappedComponent>()) {
                    currentState = currentState.updateEntity(creatureId) { c ->
                        c.with(TappedComponent)
                    }
                    val creatureName = creatureContainer.get<CardComponent>()?.name ?: "Creature"
                    events.add(TappedEvent(creatureId, creatureName))
                }
            }
        }

        // Pay Casualty's optional additional cost: sacrifice the chosen creature (CR 702.153).
        // Validated in validate(); mirror the additional-cost Sacrifice zone move, including the
        // LKI snapshot (Rule 112.7a / 608.2h) and the leave-the-battlefield events so dies/leaves
        // triggers and the "cards leave your graveyard" family see the move.
        action.casualtyCreature?.let { permId ->
            val projectedBeforeSacrifice = currentState.projectedState
            sacrificedSnapshots.addAll(capturePermanentSnapshots(listOf(permId), projectedBeforeSacrifice))
            val permContainer = currentState.getEntity(permId)
            val permCard = permContainer?.get<CardComponent>()
            if (permContainer != null && permCard != null) {
                val controllerId = permContainer.get<ControllerComponent>()?.playerId ?: action.playerId
                val ownerId = permCard.ownerId ?: action.playerId
                currentState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                    .trackPermanentSacrifice(currentState, listOf(permId), action.playerId)
                currentState = currentState.removeFromZone(ZoneKey(controllerId, Zone.BATTLEFIELD), permId)
                currentState = currentState.addToZone(ZoneKey(ownerId, Zone.GRAVEYARD), permId)
                events.add(PermanentsSacrificedEvent(action.playerId, listOf(permId), listOf(permCard.name)))
                events.add(ZoneChangeEvent(
                    entityId = permId,
                    entityName = permCard.name,
                    fromZone = Zone.BATTLEFIELD,
                    toZone = Zone.GRAVEYARD,
                    ownerId = ownerId
                ))
            }
        }

        // X mana to pay (≤ action.xValue). For an X-cost Harmonize cast with a tapped
        // creature, the leftover power beyond the printed generic reduces the X mana paid;
        // computed from the pre-reduction cost so the printed-generic split matches what
        // AlternativePaymentHandler.reduceGeneric does below. action.xValue (the effect's X)
        // is untouched.
        val paymentXValue = harmonizePaymentXValue(currentState, action, cardDef, effectiveCost)

        // Apply alternative payment (Delve/Convoke/Harmonize)
        if (action.alternativePayment != null && !action.alternativePayment.isEmpty && cardDef != null) {
            val altPaymentResult = alternativePaymentHandler.apply(
                currentState,
                effectiveCost,
                action.alternativePayment,
                action.playerId,
                cardDef,
                action.cardId
            )
            effectiveCost = altPaymentResult.reducedCost
            currentState = altPaymentResult.newState
            events.addAll(altPaymentResult.events)
        }

        // Build spell context for conditional mana restrictions
        val spellContext = SpellPaymentContext(
            isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
            isKicked = action.wasKicked,
            isCreature = cardComponent.typeLine.isCreature,
            isLegendary = cardComponent.typeLine.isLegendary,
            manaValue = cardComponent.manaCost.cmc,
            hasXInCost = cardComponent.manaCost.hasX,
            subtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet(),
            isFromExile = isCastFromExile(currentState, action.cardId),
            isFromHand = isCastFromHand(currentState, action.cardId),
            cardTypes = cardComponent.typeLine.cardTypes,
        )

        // "Mana of any type can be spent" — relax colored requirements for cast-from-exile
        // permissions that carry the flag (Taster of Wares, Cruelclaw's Heist).
        if (isCastWithAnyManaType(currentState, action)) {
            effectiveCost = effectiveCost.relaxColors()
        }

        // "Spend only [colors] on X" restriction (Soul Burn). Use the cast face's script for
        // split/adventure cards, otherwise the card's own script.
        val xManaRestriction = (action.faceIndex?.let { cardDef?.cardFaces?.getOrNull(it)?.script }
            ?: cardDef?.script)?.xManaRestriction ?: emptySet()

        // Handle mana payment via dedicated processor
        val paymentResult = paymentProcessor.processPayment(currentState, action, effectiveCost, cardComponent.name, paymentXValue, spellContext, xManaRestriction)
        if (paymentResult.error != null) {
            return ExecutionResult.error(currentState, paymentResult.error)
        }
        currentState = paymentResult.state
        events.addAll(paymentResult.events)

        // Track total mana spent on spells this turn (for Expend triggers)
        val manaSpentThisCast = paymentResult.events
            .filterIsInstance<ManaSpentEvent>()
            .sumOf { it.total }
        if (manaSpentThisCast > 0) {
            currentState = currentState.updateEntity(action.playerId) { container ->
                val existing = container.get<ManaSpentOnSpellsThisTurnComponent>()
                    ?: ManaSpentOnSpellsThisTurnComponent()
                container.with(existing.copy(totalSpent = existing.totalSpent + manaSpentThisCast))
            }
        }

        // Pay forage additional cost when casting a creature from graveyard via
        // MayCastCreaturesFromGraveyardWithForageComponent (e.g., Osteomancer Adept).
        // Auto-pay: prefer sacrificing a Food; otherwise exile 3 other graveyard cards.
        val isForageCast = zoneResolver.hasMayCastCreaturesFromGraveyardWithForage(
            currentState, action.playerId, action.cardId, cardComponent
        ) && action.cardId in currentState.getZone(ZoneKey(action.playerId, Zone.GRAVEYARD))
        if (isForageCast) {
            val projected = currentState.projectedState
            val foods = currentState.getBattlefield().filter { permId ->
                currentState.getEntity(permId) != null &&
                    projected.getController(permId) == action.playerId &&
                    projected.hasSubtype(permId, Subtype.FOOD.value)
            }
            if (foods.isNotEmpty()) {
                val foodId = foods.first()
                val foodContainer = currentState.getEntity(foodId)
                val foodName = foodContainer?.get<CardComponent>()?.name ?: "Food"
                val foodController = foodContainer?.get<ControllerComponent>()?.playerId ?: action.playerId
                currentState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                    .trackPermanentSacrifice(currentState, listOf(foodId), foodController)
                val transition = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                    .moveToZone(currentState, foodId, Zone.GRAVEYARD)
                currentState = transition.state
                events.add(PermanentsSacrificedEvent(foodController, listOf(foodId), listOf(foodName)))
                events.addAll(transition.events)
            } else {
                val graveyardZone = ZoneKey(action.playerId, Zone.GRAVEYARD)
                val toExile = currentState.getZone(graveyardZone)
                    .filter { it != action.cardId }
                    .take(3)
                if (toExile.size < 3) {
                    return ExecutionResult.error(currentState, "Cannot forage: need 3 other cards in graveyard or a Food")
                }
                for (exileId in toExile) {
                    val transition = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                        .moveToZone(currentState, exileId, Zone.EXILE)
                    currentState = transition.state
                    events.addAll(transition.events)
                }
            }
        }

        // Pay additional life cost (e.g., Festival of Embers graveyard casting)
        if (action.graveyardLifeCost > 0) {
            val currentLife = currentState.lifeTotal(action.playerId) // CR 810.9a — team's shared total
            val newLife = currentLife - action.graveyardLifeCost
            currentState = currentState.withLifeTotal(action.playerId, newLife)
            events.add(LifeChangedEvent(action.playerId, currentLife, newLife, LifeChangeReason.LIFE_LOSS))
            currentState = com.wingedsheep.engine.handlers.effects.DamageUtils.markLifeLostThisTurn(currentState, action.playerId)
        }

        // Pay any additional life cost from opponent permanents' ModifySpellCost abilities
        // (e.g. Terror of the Peaks: "Spells your opponents cast that target this creature
        // cost an additional 3 life to cast.").
        if (action.targets.isNotEmpty()) {
            val additionalLifeCost = costCalculator.calculateAdditionalLifeCost(
                currentState, action.playerId, action.targets
            )
            if (additionalLifeCost > 0) {
                val currentLife = currentState.lifeTotal(action.playerId) // CR 810.9a — team's shared total
                val newLife = currentLife - additionalLifeCost
                currentState = currentState.withLifeTotal(action.playerId, newLife)
                events.add(LifeChangedEvent(action.playerId, currentLife, newLife, LifeChangeReason.PAYMENT))
                currentState = DamageUtils.markLifeLostThisTurn(currentState, action.playerId)
            }
        }

        // Compute target requirements for resolution-time re-validation (Rule 608.2b).
        // For modal spells with cast-time mode picks, union the per-mode requirements so resolution can
        // re-check every targeted slot. Per-mode breakdown is persisted on SpellOnStackComponent.modeTargetRequirements.
        val modalEffectForTargets = cardDef?.script?.spellEffect as? com.wingedsheep.sdk.scripting.effects.ModalEffect
        val perModeTargetRequirements: Map<Int, List<TargetRequirement>> =
            if (modalEffectForTargets != null && action.chosenModes.isNotEmpty()) {
                action.chosenModes.distinct().associateWith { idx ->
                    modalEffectForTargets.modes.getOrNull(idx)?.targetRequirements ?: emptyList()
                }
            } else emptyMap()

        val spellTargetRequirements = if (cardDef != null) {
            // Adventure / split face cast (CR 715 / 709) — read targets from the face's script.
            val faceScriptForTargets = action.faceIndex?.let { cardDef.cardFaces.getOrNull(it)?.script }
            val baseTargetReqs = if (action.chosenModes.isNotEmpty() && modalEffectForTargets != null) {
                // Modal spell with modes chosen at cast time — union per-mode requirements
                action.chosenModes.flatMap { idx ->
                    modalEffectForTargets.modes.getOrNull(idx)?.targetRequirements ?: emptyList()
                }
            } else if (action.wasKicked && cardDef.script.kickerTargetRequirements.isNotEmpty()) {
                cardDef.script.kickerTargetRequirements
            } else {
                (faceScriptForTargets ?: cardDef.script).targetRequirements
            }
            buildList {
                addAll(baseTargetReqs)
                cardDef.script.auraTarget?.let { add(it) }
            }
        } else {
            emptyList()
        }

        // Check if spell requires a creature type choice during casting (e.g., Aphetto Dredging)
        val castTimeChoice = cardDef?.script?.castTimeCreatureTypeChoice
        if (castTimeChoice != null) {
            val pauseResult = pauseForCreatureTypeChoice(
                currentState, action, castTimeChoice, sacrificedSnapshots, spellTargetRequirements, events
            )
            if (pauseResult != null) return pauseResult
        }

        // Sneak (CR 702.190a): pay the "return an unblocked creature you control to its owner's
        // hand" portion of the cost. The {cost} mana was paid by the standard payment pipeline
        // above. Capture the defender the returned creature was attacking first, so a resolving
        // permanent spell can enter attacking the same player/planeswalker (CR 702.190b).
        val wasSneaked = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.SNEAK) &&
            cardDef.keywordAbilities.any { it is KeywordAbility.Sneak }
        var sneakAttackDefenderId: EntityId? = null
        if (wasSneaked) {
            val bounceId = action.additionalCostPayment?.bouncedPermanents?.firstOrNull()
            if (bounceId != null) {
                sneakAttackDefenderId = currentState.getEntity(bounceId)
                    ?.get<com.wingedsheep.engine.state.components.combat.AttackingComponent>()
                    ?.defenderId
                val bounceResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                    currentState, bounceId, Zone.HAND
                )
                currentState = bounceResult.state
                events.addAll(bounceResult.events)
            }
        }

        // Determine if this spell is being cast using warp. Gated by the chosen alternative-cost
        // type so that when warp collides with another alternative cost (e.g. a granted warp on a
        // card being evoked) only the chosen one drives its post-resolution behavior. With no
        // choice recorded, falls back to the legacy "card has warp" heuristic.
        val wasWarped = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.WARP) &&
            WarpGrants.effectiveWarp(
                currentState, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator
            ) != null

        // Determine if this spell is being cast using evoke
        val wasEvoked = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.EVOKE) &&
            cardDef.keywordAbilities.any { it is KeywordAbility.Evoke }

        // Determine if this spell is being cast using impending
        val wasImpending = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.IMPENDING) &&
            cardDef.keywordAbilities.any { it is KeywordAbility.Impending }

        // Extract per-color mana spent from payment events (for mana-spent-gated triggers)
        val manaSpentEvent = paymentResult.events.filterIsInstance<ManaSpentEvent>().firstOrNull()

        // Capture storm count before incrementing (spells cast before this one)
        val stormCount = currentState.spellsCastThisTurn

        // Increment spell count for this turn (global and per-player)
        val playerCount = currentState.playerSpellsCastThisTurn[action.playerId] ?: 0
        currentState = currentState.copy(
            spellsCastThisTurn = stormCount + 1,
            playerSpellsCastThisTurn = currentState.playerSpellsCastThisTurn +
                (action.playerId to playerCount + 1),
            spellWarpedThisTurn = currentState.spellWarpedThisTurn || wasWarped
        )

        // Track spell records cast this turn (for conditional evasion like Relic Runner, and "first of type" triggers)
        run {
            val record = com.wingedsheep.engine.state.CastSpellRecord(
                typeLine = cardComponent.typeLine,
                manaValue = cardComponent.manaValue,
                colors = cardComponent.colors,
                isFaceDown = action.castFaceDown,
                paidWithTreasureMana = paymentResult.paidWithTreasureMana,
                // The cast card moves to the stack keeping its entity id, so this matches the
                // resolving spell's EffectContext.sourceId (used by SpellsCastThisTurn excludeSelf).
                sourceEntityId = action.cardId,
                // Origin zone of the cast (HAND for a normal cast; GRAVEYARD/EXILE/COMMAND for
                // flashback/forage, plot/foretell, commander, …). The card is still in its origin
                // zone here — stackResolver.castSpell (below) moves it — so this resolves the same
                // way castSpell stamps SpellOnStackComponent.castFromZone. Powers "you haven't cast
                // a spell from your hand this turn" (Prairie Dog cycle).
                castFromZone = stackResolver.findCastFromZone(currentState, action.cardId, action.playerId),
            )
            val existing = currentState.spellsCastThisTurnByPlayer[action.playerId] ?: emptyList()
            currentState = currentState.copy(
                spellsCastThisTurnByPlayer = currentState.spellsCastThisTurnByPlayer +
                    (action.playerId to existing + record),
                // "the spell most recently cast this turn" — read by Mana Maze's cast restriction.
                lastCastSpellColors = record.colors
            )
        }

        // Check if casting from graveyard via MayPlayPermanentsFromGraveyard (Muldrotha)
        val castingFromGraveyardViaMuldrotha = action.cardId in currentState.getZone(ZoneKey(action.playerId, Zone.GRAVEYARD)) &&
            zoneResolver.hasMayPlayPermanentFromGraveyardPermission(currentState, action.playerId, action.cardId, cardComponent)

        // Derive per-mode target groups from the flat target list when the action arrived
        // with chosenModes but no modeTargetsOrdered (current web-client cast-time UI for
        // choose-1 modal spells). Slice action.targets in mode order using each mode's
        // total target slot count so modal resolution can read per-mode targets.
        val effectiveModeTargetsOrdered = if (
            action.modeTargetsOrdered.isEmpty() &&
            action.chosenModes.isNotEmpty() &&
            modalEffectForTargets != null &&
            action.targets.isNotEmpty()
        ) {
            deriveModeTargetsFromFlat(modalEffectForTargets, action.chosenModes, action.targets)
        } else {
            action.modeTargetsOrdered
        }

        // Evaluate "as you cast this spell" condition captures (CR 601.2i). The spell has finished
        // being cast (costs paid) but isn't on the stack yet; freezing the answers now lets the
        // resolving effect read the cast-time board even if it has since changed (Steer Clear's
        // "if you controlled a Mount as you cast this spell"). The caster is the controller; the
        // captured names are carried onto SpellOnStackComponent.castTimeFlags.
        val castTimeScript = action.faceIndex?.let { cardDef?.cardFaces?.getOrNull(it)?.script } ?: cardDef?.script
        val castTimeCaptures = castTimeScript?.castTimeCaptures.orEmpty()
        val castTimeFlags: Set<String> = if (castTimeCaptures.isEmpty()) {
            emptySet()
        } else {
            val captureContext = EffectContext(
                sourceId = action.cardId,
                controllerId = action.playerId,
                targets = emptyList(),
                xValue = 0
            )
            castTimeCaptures
                .filter { conditionEvaluator.evaluate(currentState, it.condition, captureContext) }
                .map { it.flag }
                .toSet()
        }

        // Pay-X-life additional cost (AdditionalCost.PayXLife): record the declared X (non-null,
        // including 0) only when the spell actually carries this cost, so it's coalesced into the
        // resolution X value. Other spells leave this null and keep xValue purely from {X}.
        val payXLifeAmount: Int? =
            if (castTimeScript?.additionalCosts?.any { it is AdditionalCost.PayXLife } == true) {
                action.additionalCostPayment?.payXLifeAmount ?: 0
            } else null

        // Cast the spell
        val castResult = stackResolver.castSpell(
            currentState,
            action.cardId,
            action.playerId,
            action.targets,
            action.xValue,
            sacrificedSnapshots,
            castFaceDown = action.castFaceDown,
            damageDistribution = action.damageDistribution,
            targetRequirements = spellTargetRequirements,
            exiledCardCount = exiledCardCount,
            additionalCostBlightAmount = action.additionalCostPayment?.blightAmount ?: 0,
            additionalCostPayXLifeAmount = payXLifeAmount,
            wasKicked = action.wasKicked,
            wasBlightPaid = (action.additionalCostPayment?.blightTargets?.isNotEmpty() == true),
            wasWarped = wasWarped,
            wasEvoked = wasEvoked,
            wasImpending = wasImpending,
            wasSneaked = wasSneaked,
            sneakAttackDefenderId = sneakAttackDefenderId,
            chosenModes = action.chosenModes,
            modeTargetsOrdered = effectiveModeTargetsOrdered,
            modeTargetRequirements = perModeTargetRequirements,
            modeDamageDistribution = action.modeDamageDistribution,
            totalManaSpent = manaSpentThisCast,
            beheldCards = beheldCards,
            chosenEntitySnapshots = chosenEntitySnapshots,
            manaSpentWhite = manaSpentEvent?.white ?: 0,
            manaSpentBlue = manaSpentEvent?.blue ?: 0,
            manaSpentBlack = manaSpentEvent?.black ?: 0,
            manaSpentRed = manaSpentEvent?.red ?: 0,
            manaSpentGreen = manaSpentEvent?.green ?: 0,
            manaSpentColorless = manaSpentEvent?.colorless ?: 0,
            manaSpentOnXByColor = paymentResult.xManaSpentByColor,
            faceIndex = action.faceIndex,
            paidWithTreasureMana = paymentResult.paidWithTreasureMana,
            castTimeFlags = castTimeFlags
        )

        if (!castResult.isSuccess) {
            return castResult
        }

        var currentCastState = castResult.newState
        var allEvents = events + castResult.events

        // Apply any spell riders carried by the mana that paid for this spell.
        // Some riders mutate the spell directly (e.g., Cavern's MakesSpellUncounterable
        // stamps a component) while others queue a triggered ability above the spell
        // (e.g., Path of Ancestry's conditional scry).
        val riderPendingTriggers = mutableListOf<PendingTrigger>()
        for (rider in paymentResult.consumedRiders) {
            val (newState, riderTriggers) = applyManaSpellRider(
                currentCastState, action, cardComponent, rider
            )
            currentCastState = newState
            riderPendingTriggers.addAll(riderTriggers)
        }

        // Record Muldrotha graveyard cast permission usage
        if (castingFromGraveyardViaMuldrotha) {
            val typeName = zoneResolver.choosePermanentTypeForGraveyardPermission(currentCastState, action.playerId, cardComponent)
            if (typeName != null) {
                currentCastState = zoneResolver.recordGraveyardPlayPermissionUsage(currentCastState, action.playerId, typeName)
            }
        }

        // Record once-per-turn linked-exile permission usage (e.g., Maralen, Fae Ascendant).
        // Captured against the pre-cast state since the card has now left exile and the granter
        // would no longer be located via its LinkedExileComponent.
        if (linkedExileGranterEntry?.ability?.oncePerTurn == true) {
            currentCastState = currentCastState.updateEntity(linkedExileGranterEntry.granterId) { c ->
                c.with(com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent)
            }
        }

        // Record once-per-turn free-cast permission usage (e.g., Zaffai and the Tempests). Only a
        // `MayCastWithoutPayingManaCost(oncePerTurn = true)` source consumes a use, and only when
        // no unlimited free-cast source could have paid instead.
        if (action.useWithoutPayingManaCost) {
            val onceSource = costCalculator.oncePerTurnFreeCastSourceToConsume(currentCastState, action.playerId, cardDef)
            if (onceSource != null) {
                currentCastState = currentCastState.updateEntity(onceSource) { c ->
                    c.with(com.wingedsheep.engine.state.components.battlefield.MayCastWithoutPayingCostUsedThisTurnComponent)
                }
            }
        }

        // Handle Storm keyword: build one PendingTrigger per instance of Storm.
        // Per CR 702.40b each instance of Storm triggers separately. Sources of Storm:
        //   1. The card's printed keyword (Keyword.STORM in keywords) — counts once.
        //   2. Each matching grant in GrantedSpellKeywordsComponent (e.g., Ral's storm emblem) —
        //      counts once per matching grant.
        // Per CR 702.40a Storm triggers whenever the spell is cast; it copies zero times when
        // no other spells have been cast this turn. The executor is a no-op at copyCount == 0
        // but the trigger must still land on the stack so "whenever an ability triggers /
        // is put onto the stack" effects see it.
        val stormGrantCount = run {
            // Source 2a: GrantedSpellKeywordsComponent — emblem-style player grants (Ral, Crackling Wit).
            val playerContainer = currentCastState.getEntity(action.playerId)
            val grants = playerContainer?.get<GrantedSpellKeywordsComponent>()?.grants ?: emptyList()
            val evalContext = PredicateContext(controllerId = action.playerId)
            val componentGrants = grants.count { grant ->
                grant.keyword == Keyword.STORM &&
                    predicateEvaluator.matches(currentCastState, currentCastState.projectedState, action.cardId, grant.spellFilter, evalContext)
            }
            // Source 2b: GrantKeywordToOwnSpells static abilities on battlefield permanents the
            // caster controls (Prismari, the Inspiration). Each matching permanent is a separate
            // instance of storm (CR 702.40b), so count them all rather than short-circuiting.
            val staticGrants = if (cardDef != null) {
                grantedKeywordResolver.countGrants(currentCastState, action.playerId, cardDef, Keyword.STORM)
            } else 0
            componentGrants + staticGrants
        }
        val printedStormCount = if (cardDef != null && cardDef.hasKeyword(Keyword.STORM)) 1 else 0
        val stormInstanceCount = printedStormCount + stormGrantCount
        val stormPendingTriggers: List<PendingTrigger> =
            if (!action.castFaceDown && cardDef != null && stormInstanceCount > 0) {
                val spellEffect = cardDef.script.spellEffect
                if (spellEffect != null) {
                    List(stormInstanceCount) {
                        val stormEffect = StormCopyEffect(
                            copyCount = stormCount,
                            spellEffect = spellEffect,
                            spellTargetRequirements = spellTargetRequirements,
                            spellName = cardComponent.name
                        )
                        val ability = TriggeredAbility(
                            id = AbilityId.generate(),
                            trigger = SdkGameEvent.SpellCastEvent(player = Player.You),
                            binding = TriggerBinding.SELF,
                            effect = stormEffect,
                            activeZone = Zone.STACK,
                            descriptionOverride = "Storm — copy ${cardComponent.name} $stormCount time(s)"
                        )
                        PendingTrigger(
                            ability = ability,
                            sourceId = action.cardId,
                            sourceName = cardComponent.name,
                            controllerId = action.playerId,
                            triggerContext = TriggerContext(
                                triggeringEntityId = action.cardId,
                                triggeringPlayerId = action.playerId
                            )
                        )
                    }
                } else emptyList()
            } else emptyList()

        // Handle Conspire (CR 702.78): when the optional additional cost was paid, a reflexive
        // trigger goes on the stack above the spell: "When you do, copy it and you may choose
        // new targets for the copy." Reuses StormCopyEffect with copyCount=1 so the existing
        // retargeting, modal-copy, and SpellOnStackComponent-clone plumbing applies unchanged.
        val conspirePendingTriggers: List<PendingTrigger> =
            if (!action.castFaceDown && cardDef != null && action.conspiredCreatures.isNotEmpty()) {
                val spellEffect = cardDef.script.spellEffect
                if (spellEffect != null) {
                    val copyEffect = StormCopyEffect(
                        copyCount = 1,
                        spellEffect = spellEffect,
                        spellTargetRequirements = spellTargetRequirements,
                        spellName = cardComponent.name
                    )
                    val ability = TriggeredAbility(
                        id = AbilityId.generate(),
                        trigger = SdkGameEvent.SpellCastEvent(player = Player.You),
                        binding = TriggerBinding.SELF,
                        effect = copyEffect,
                        activeZone = Zone.STACK,
                        descriptionOverride = "Conspire — copy ${cardComponent.name}"
                    )
                    listOf(
                        PendingTrigger(
                            ability = ability,
                            sourceId = action.cardId,
                            sourceName = cardComponent.name,
                            controllerId = action.playerId,
                            triggerContext = TriggerContext(
                                triggeringEntityId = action.cardId,
                                triggeringPlayerId = action.playerId
                            )
                        )
                    )
                } else emptyList()
            } else emptyList()

        // Handle Casualty (CR 702.153): when the optional additional cost (sacrifice a creature
        // with power N or greater) was paid, a reflexive trigger goes on the stack above the spell:
        // "When you do, copy it and you may choose new targets for the copy." Identical copy shape
        // to Conspire — reuses StormCopyEffect with copyCount=1.
        val casualtyPendingTriggers: List<PendingTrigger> =
            if (!action.castFaceDown && cardDef != null && action.casualtyCreature != null) {
                val spellEffect = cardDef.script.spellEffect
                if (spellEffect != null) {
                    val copyEffect = StormCopyEffect(
                        copyCount = 1,
                        spellEffect = spellEffect,
                        spellTargetRequirements = spellTargetRequirements,
                        spellName = cardComponent.name
                    )
                    val ability = TriggeredAbility(
                        id = AbilityId.generate(),
                        trigger = SdkGameEvent.SpellCastEvent(player = Player.You),
                        binding = TriggerBinding.SELF,
                        effect = copyEffect,
                        activeZone = Zone.STACK,
                        descriptionOverride = "Casualty — copy ${cardComponent.name}"
                    )
                    listOf(
                        PendingTrigger(
                            ability = ability,
                            sourceId = action.cardId,
                            sourceName = cardComponent.name,
                            controllerId = action.playerId,
                            triggerContext = TriggerContext(
                                triggeringEntityId = action.cardId,
                                triggeringPlayerId = action.playerId
                            )
                        )
                    )
                } else emptyList()
            } else emptyList()

        // Handle pending spell copies (e.g., Howl of the Horde). Each pending entry carries its own
        // spellFilter (instant or sorcery by default, but e.g. "creature" is expressible), matched
        // against the spell just cast. Face-down spells have no characteristics, so they never match.
        if (!action.castFaceDown) {
            val copyEvalContext = PredicateContext(controllerId = action.playerId)
            val matchingCopies = currentCastState.pendingSpellCopies.filter { pending ->
                pending.controllerId == action.playerId &&
                    predicateEvaluator.matches(
                        currentCastState, currentCastState.projectedState, action.cardId, pending.spellFilter, copyEvalContext
                    )
            }
            if (matchingCopies.isNotEmpty()) {
                val totalCopies = matchingCopies.sumOf { it.copies }
                // Remove consumed pending copies (keep persistent ones like The Mirari Conjecture Ch. III,
                // and any non-matching entries waiting for a different spell type).
                val remainingPending = currentCastState.pendingSpellCopies.filter { pending ->
                    pending.persistent || pending !in matchingCopies
                }
                currentCastState = currentCastState.copy(pendingSpellCopies = remainingPending)

                // Create copies using Storm copy infrastructure
                val spellEffect = cardDef?.script?.spellEffect
                if (spellEffect != null && totalCopies > 0) {
                    val copyEffect = StormCopyEffect(
                        copyCount = totalCopies,
                        spellEffect = spellEffect,
                        spellTargetRequirements = spellTargetRequirements,
                        spellName = cardComponent.name
                    )
                    // sourceId must point to the spell being copied (action.cardId), not the
                    // originating permanent (e.g., Howl of the Horde). StormCopyEffectExecutor
                    // uses sourceId to clone the SpellOnStackComponent via putSpellCopy (Phase 1
                    // of spell-copies-as-spells); the originating permanent may be in the
                    // graveyard by the time the trigger resolves.
                    val copyAbility = TriggeredAbilityOnStackComponent(
                        sourceId = action.cardId,
                        sourceName = cardComponent.name,
                        controllerId = action.playerId,
                        effect = copyEffect,
                        description = "Copy ${cardComponent.name} $totalCopies time(s)"
                    )
                    val copyResult = stackResolver.putTriggeredAbility(currentCastState, copyAbility)
                    if (!copyResult.isSuccess) return copyResult
                    currentCastState = copyResult.newState
                    allEvents = allEvents + copyResult.events
                }
            }
        }

        // Handle pending "next spell can't be countered" riders (e.g., Mistrise Village). Each entry
        // carries its own spellFilter (any spell by default) matched against the spell just cast. The
        // first matching cast stamps the spell uncounterable and consumes every matching entry; later
        // spells aren't protected. Unlike the copy rider above, face-down spells aren't excluded — a
        // face-down spell is still "the next spell you cast", and the default Any filter matches it.
        run {
            val uncounterableEvalContext = PredicateContext(controllerId = action.playerId)
            val matchingRiders = currentCastState.pendingUncounterableSpells.filter { pending ->
                pending.controllerId == action.playerId &&
                    predicateEvaluator.matches(
                        currentCastState, currentCastState.projectedState, action.cardId, pending.spellFilter, uncounterableEvalContext
                    )
            }
            if (matchingRiders.isNotEmpty()) {
                val remainingRiders = currentCastState.pendingUncounterableSpells.filter { it !in matchingRiders }
                currentCastState = currentCastState
                    .copy(pendingUncounterableSpells = remainingRiders)
                    .updateEntity(action.cardId) { c -> c.with(CantBeCounteredComponent) }
            }
        }

        // Detect and process triggers from casting (including additional cost events like sacrifice).
        // Storm pending triggers (built above) are prepended so they go on the stack just above the
        // spell itself — per CR 603.3b Storm goes on top of the spell that caused it to trigger.
        // Other AP spell-cast triggers follow (placed higher on the stack), then NAP triggers on top,
        // matching APNAP ordering within processTriggers.
        val detectedTriggers = triggerDetector.detectTriggers(currentCastState, allEvents)
        val triggers = riderPendingTriggers + conspirePendingTriggers + casualtyPendingTriggers + stormPendingTriggers + detectedTriggers
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(currentCastState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state.withPriority(action.playerId),
                    triggerResult.pendingDecision!!,
                    allEvents + triggerResult.events
                ).copy(triggersAlreadyProcessed = true)
            }

            allEvents = allEvents + triggerResult.events
            return ExecutionResult.success(
                triggerResult.newState.withPriority(action.playerId),
                allEvents
            ).copy(triggersAlreadyProcessed = true)
        }

        // detectTriggers ran above (no matches) — flag the result so resumers don't
        // re-scan the cast events.
        return ExecutionResult.success(
            currentCastState.withPriority(action.playerId),
            allEvents
        ).copy(triggersAlreadyProcessed = true)
    }

    /**
     * Check if the spell needs a creature type choice during casting (e.g., Aphetto Dredging).
     * If so, scan the appropriate zone for creature types and pause for the choice.
     * Returns null if no pause is needed (e.g., no creature types found).
     */
    private fun pauseForCreatureTypeChoice(
        currentState: GameState,
        action: CastSpell,
        source: com.wingedsheep.sdk.model.CastTimeCreatureTypeSource,
        sacrificedSnapshots: List<PermanentSnapshot>,
        spellTargetRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
        priorEvents: List<GameEvent>
    ): ExecutionResult? {
        // Determine which zone to scan based on source
        val zone = when (source) {
            com.wingedsheep.sdk.model.CastTimeCreatureTypeSource.GRAVEYARD ->
                ZoneKey(action.playerId, Zone.GRAVEYARD)
        }
        val zoneCards = currentState.getZone(zone)

        // Collect creature subtypes and which cards have each type
        val typeToCardIds = mutableMapOf<String, MutableList<EntityId>>()
        for (cardId in zoneCards) {
            val cc = currentState.getEntity(cardId)?.get<CardComponent>() ?: continue
            val typeLine = cc.typeLine
            if (typeLine.isCreature) {
                for (subtype in typeLine.subtypes) {
                    typeToCardIds.getOrPut(subtype.value) { mutableListOf() }.add(cardId)
                }
            }
        }

        // If no creature types found, skip the decision — casting proceeds normally
        if (typeToCardIds.isEmpty()) return null

        val sortedTypes = typeToCardIds.keys.sorted()
        val cardComponent = currentState.getEntity(action.cardId)?.get<CardComponent>()
        val sourceName = cardComponent?.name

        // Build option index → card IDs mapping for client preview
        val optionCardIds = sortedTypes.mapIndexed { index, type ->
            index to typeToCardIds[type]!!.toList()
        }.toMap()

        val decisionId = java.util.UUID.randomUUID().toString()
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = action.playerId,
            prompt = "Choose a creature type",
            context = DecisionContext(
                sourceId = action.cardId,
                sourceName = sourceName,
                phase = DecisionPhase.CASTING
            ),
            options = sortedTypes,
            optionCardIds = optionCardIds
        )

        val continuation = CastWithCreatureTypeContinuation(
            decisionId = decisionId,
            cardId = action.cardId,
            casterId = action.playerId,
            targets = action.targets,
            xValue = action.xValue,
            sacrificedPermanents = sacrificedSnapshots,
            targetRequirements = spellTargetRequirements,
            count = 0,
            creatureTypes = sortedTypes
        )

        val pausedState = currentState
            .pushContinuation(continuation)
            .withPendingDecision(decision)

        return ExecutionResult.paused(
            pausedState.withPriority(action.playerId),
            decision,
            priorEvents + DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = action.playerId,
                decisionType = "CHOOSE_OPTION",
                prompt = decision.prompt
            )
        )
    }

    /**
     * Initial entry point for choose-N modal cast-time mode selection (rule 700.2).
     *
     * Pre-filters modes by 700.2a target legality, then pauses with a ChooseOption
     * decision in the CASTING phase. The resumer iterates until `chooseCount` modes
     * are picked (or "Done" fires once `minChooseCount` is satisfied), then
     * transitions to per-mode target selection or directly back into [execute] with
     * a fully populated action.
     */
    private fun pauseForCastTimeModeSelection(
        currentState: GameState,
        action: CastSpell,
        cardComponent: CardComponent,
        modalEffect: ModalEffect
    ): ExecutionResult {
        // Apply chooseAllIfBlightPaid: if the player paid blight, force choosing all
        // modes; otherwise the regular [minChooseCount, chooseCount] range applies.
        val (effectiveMin, effectiveMax) = effectiveModalChooseCounts(currentState, modalEffect, action)
        val effectiveModalEffect = if (effectiveMin == modalEffect.minChooseCount &&
            effectiveMax == modalEffect.chooseCount) {
            modalEffect
        } else {
            modalEffect.copy(chooseCount = effectiveMax, minChooseCount = effectiveMin)
        }

        val available = effectiveModalEffect.modes.withIndex()
            .filter { (_, mode) -> modeHasSatisfiableTargets(currentState, action.playerId, action.cardId, mode) }
            .map { it.index }

        if (available.size < effectiveModalEffect.minChooseCount) {
            return ExecutionResult.error(currentState, "No legal mode selection available for ${cardComponent.name}")
        }

        return presentCastModalModeDecision(
            state = currentState,
            cardId = action.cardId,
            casterId = action.playerId,
            cardName = cardComponent.name,
            baseCastAction = action,
            modalEffect = effectiveModalEffect,
            selectedModeIndices = emptyList(),
            availableIndices = if (effectiveModalEffect.allowRepeat) null else available,
            repeatAvailableIndices = if (effectiveModalEffect.allowRepeat) available else null
        )
    }

    /**
     * Check whether a modal mode can potentially be cast — either it has no targets, or
     * at least one legal target exists for each of its [TargetRequirement]s (rule 700.2a).
     */
    private fun modeHasSatisfiableTargets(
        state: GameState,
        casterId: EntityId,
        sourceId: EntityId,
        mode: com.wingedsheep.sdk.scripting.effects.Mode
    ): Boolean {
        if (mode.targetRequirements.isEmpty()) return true
        return mode.targetRequirements.all { req ->
            req.effectiveMinCount == 0 ||
                targetFinder.findLegalTargets(state, req, casterId, sourceId).isNotEmpty()
        }
    }

    /**
     * Build a ChooseOptionDecision + CastModalModeSelectionContinuation for the next
     * mode pick. Shared between the initial pause (here) and the iterative resumer.
     */
    internal fun presentCastModalModeDecision(
        state: GameState,
        cardId: EntityId,
        casterId: EntityId,
        cardName: String,
        baseCastAction: CastSpell,
        modalEffect: ModalEffect,
        selectedModeIndices: List<Int>,
        availableIndices: List<Int>?,
        repeatAvailableIndices: List<Int>?
    ): ExecutionResult {
        val offerIndices = availableIndices ?: repeatAvailableIndices ?: modalEffect.modes.indices.toList()
        val doneOffered = selectedModeIndices.size >= modalEffect.minChooseCount &&
            selectedModeIndices.size < modalEffect.chooseCount

        val optionLabels = offerIndices.map { modalEffect.modes[it].description } +
            (if (doneOffered) listOf("Done") else emptyList())

        val decisionId = java.util.UUID.randomUUID().toString()
        val pickNumber = selectedModeIndices.size + 1
        val alreadyPicked = if (selectedModeIndices.isNotEmpty()) {
            val labels = selectedModeIndices.map { modalEffect.modes[it].description }
            "\nAlready picked: ${labels.joinToString("; ")}"
        } else ""
        val prompt = "Choose a mode for $cardName ($pickNumber of ${modalEffect.chooseCount})$alreadyPicked"
        val decision = ChooseOptionDecision(
            id = decisionId,
            playerId = casterId,
            prompt = prompt,
            context = DecisionContext(
                sourceId = cardId,
                sourceName = cardName,
                phase = DecisionPhase.CASTING
            ),
            options = optionLabels,
            // Cast-time mode selection must be cancellable (rule 601.2b–c, K1 in plan):
            // the pause happens before any cost is paid, so aborting is safe.
            canCancel = true
        )

        val continuation = com.wingedsheep.engine.core.CastModalModeSelectionContinuation(
            decisionId = decisionId,
            cardId = cardId,
            casterId = casterId,
            baseCastAction = baseCastAction,
            modes = modalEffect.modes,
            chooseCount = modalEffect.chooseCount,
            minChooseCount = modalEffect.minChooseCount,
            allowRepeat = modalEffect.allowRepeat,
            offeredIndices = offerIndices,
            availableIndices = availableIndices,
            selectedModeIndices = selectedModeIndices,
            doneOptionOffered = doneOffered
        )

        val pausedState = state
            .pushContinuation(continuation)
            .withPendingDecision(decision)
            .withPriority(casterId)

        return ExecutionResult.paused(
            pausedState,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = casterId,
                    decisionType = "CHOOSE_OPTION",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Build a ChooseTargetsDecision + CastModalTargetSelectionContinuation for the next
     * mode that needs targets. Skips modes whose requirements are empty, advancing the
     * ordinal and appending an empty target list until it finds one that needs targets
     * or all modes are resolved.
     */
    /**
     * Surface the first selection-requiring additional cost on a server-initiated free cast that
     * the action hasn't already paid, pausing for the caster's choice. See
     * [CastSpellAdditionalCostContinuation] for the re-entry contract.
     *
     * - Only the *selection* atoms need a player choice (Sacrifice / Discard / ExileFrom /
     *   TapPermanents / ReturnToHand). PayLife / mana / reveal-from-hand are auto-paid downstream
     *   and need no prompt, so they're ignored here.
     * - A cost already satisfied by the action's payment (the normal client-cast path, which is
     *   gated by `validate()`) is skipped — so this never fires for a normal cast.
     * - If a mandatory cost can't be paid at all (fewer legal options than the count required),
     *   the cast can't be completed (CR 601.2h — unpayable costs can't be paid): return an error so
     *   the free-cast caller treats it as a no-op and the card stays where it is.
     *
     * Returns null when nothing needs choosing — the cast proceeds inline.
     */
    private fun surfaceUnpaidAdditionalCostSelection(
        state: GameState,
        action: CastSpell,
        flattenedCosts: List<AdditionalCost>,
    ): ExecutionResult? {
        val payment = action.additionalCostPayment
        for (cost in flattenedCosts) {
            val atom = (cost as? AdditionalCost.Atom)?.atom ?: continue
            val (kind, count, options) = when (atom) {
                is CostAtom.Sacrifice -> Triple(
                    AdditionalCostSelectionKind.SACRIFICE,
                    atom.count,
                    costEnumerationUtils.findSacrificeTargets(state, action.playerId, atom)
                )
                is CostAtom.Discard -> {
                    if (atom.random) continue // random discard needs no selection
                    Triple(
                        AdditionalCostSelectionKind.DISCARD,
                        atom.count,
                        costEnumerationUtils.findDiscardTargets(state, action.playerId, atom.filter)
                            .filter { it != action.cardId }
                    )
                }
                is CostAtom.ExileFrom -> Triple(
                    AdditionalCostSelectionKind.EXILE,
                    atom.count,
                    costEnumerationUtils.findExileTargets(state, action.playerId, atom.filter, atom.zone)
                        .filter { it != action.cardId }
                )
                is CostAtom.TapPermanents -> Triple(
                    AdditionalCostSelectionKind.TAP,
                    atom.count,
                    costEnumerationUtils.findAbilityTapTargets(state, action.playerId, atom.filter)
                        .let { if (atom.excludeSelf) it.filter { id -> id != action.cardId } else it }
                )
                is CostAtom.ReturnToHand -> Triple(
                    AdditionalCostSelectionKind.RETURN_TO_HAND,
                    atom.count,
                    costEnumerationUtils.findAbilityBounceTargets(state, action.playerId, atom.filter)
                        .filter { id -> id != action.cardId }
                )
                else -> continue
            }
            if (count <= 0) continue

            val alreadyPaid = when (kind) {
                AdditionalCostSelectionKind.SACRIFICE -> payment?.sacrificedPermanents?.size ?: 0
                AdditionalCostSelectionKind.DISCARD -> payment?.discardedCards?.size ?: 0
                AdditionalCostSelectionKind.EXILE -> payment?.exiledCards?.size ?: 0
                AdditionalCostSelectionKind.TAP -> payment?.tappedPermanents?.size ?: 0
                AdditionalCostSelectionKind.RETURN_TO_HAND -> payment?.bouncedPermanents?.size ?: 0
            }
            if (alreadyPaid >= count) continue // supplied by the caller (normal cast) — nothing to choose

            if (options.size < count) {
                // CR 601.2h — "Unpayable costs can't be paid": the additional cost can't be met,
                // so the cast can't be completed.
                return ExecutionResult.error(state, "Cannot pay additional cost: not enough valid choices")
            }

            // No real choice (exactly enough legal options) — auto-pay and re-enter, so a forced
            // single sacrifice doesn't prompt. The re-entry sees this cost satisfied and moves on.
            if (options.size == count) {
                return execute(state, withAdditionalCostSelection(action, kind, options))
            }

            val cardName = state.getEntity(action.cardId)?.get<CardComponent>()?.name ?: "spell"
            val decisionId = java.util.UUID.randomUUID().toString()
            val verb = when (kind) {
                AdditionalCostSelectionKind.SACRIFICE -> "sacrifice"
                AdditionalCostSelectionKind.DISCARD -> "discard"
                AdditionalCostSelectionKind.EXILE -> "exile"
                AdditionalCostSelectionKind.TAP -> "tap"
                AdditionalCostSelectionKind.RETURN_TO_HAND -> "return to hand"
            }
            val prompt = "Choose $count ${if (count > 1) "cards" else "card"} to $verb for $cardName"
            // Permanents you control are chosen on the battlefield; hidden/zone cards via overlay.
            val useTargetingUI = kind == AdditionalCostSelectionKind.SACRIFICE ||
                kind == AdditionalCostSelectionKind.TAP ||
                kind == AdditionalCostSelectionKind.RETURN_TO_HAND
            val decision = SelectCardsDecision(
                id = decisionId,
                playerId = action.playerId,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = action.cardId,
                    sourceName = cardName,
                    phase = DecisionPhase.CASTING,
                ),
                options = options,
                minSelections = count,
                maxSelections = count,
                useTargetingUI = useTargetingUI,
            )
            val continuation = CastSpellAdditionalCostContinuation(
                decisionId = decisionId,
                cardId = action.cardId,
                casterId = action.playerId,
                baseCastAction = action,
                costKind = kind,
            )
            val pausedState = state
                .pushContinuation(continuation)
                .withPendingDecision(decision)
                .withPriority(action.playerId)
            return ExecutionResult.paused(
                pausedState,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = action.playerId,
                        decisionType = "SELECT_CARDS",
                        prompt = prompt,
                    )
                ),
            )
        }
        return null
    }

    /**
     * Merge a chosen additional-cost payment into [base]'s [AdditionalCostPayment] for the given
     * [kind], appending to whatever was already paid. Used by the free-cast additional-cost
     * resumer to re-enter [execute] with the selection recorded.
     */
    internal fun withAdditionalCostSelection(
        base: CastSpell,
        kind: AdditionalCostSelectionKind,
        chosen: List<EntityId>,
    ): CastSpell {
        val payment = base.additionalCostPayment ?: AdditionalCostPayment()
        val merged = when (kind) {
            AdditionalCostSelectionKind.SACRIFICE ->
                payment.copy(sacrificedPermanents = payment.sacrificedPermanents + chosen)
            AdditionalCostSelectionKind.DISCARD ->
                payment.copy(discardedCards = payment.discardedCards + chosen)
            AdditionalCostSelectionKind.EXILE ->
                payment.copy(exiledCards = payment.exiledCards + chosen)
            AdditionalCostSelectionKind.TAP ->
                payment.copy(tappedPermanents = payment.tappedPermanents + chosen)
            AdditionalCostSelectionKind.RETURN_TO_HAND ->
                payment.copy(bouncedPermanents = payment.bouncedPermanents + chosen)
        }
        return base.copy(additionalCostPayment = merged)
    }

    internal fun presentCastModalTargetDecision(
        state: GameState,
        cardId: EntityId,
        casterId: EntityId,
        cardName: String,
        baseCastAction: CastSpell,
        modes: List<com.wingedsheep.sdk.scripting.effects.Mode>,
        chosenModeIndices: List<Int>,
        resolvedModeTargets: List<List<ChosenTarget>>,
        currentOrdinal: Int
    ): ExecutionResult {
        var ordinal = currentOrdinal
        var targetsAccum = resolvedModeTargets

        while (ordinal < chosenModeIndices.size) {
            val modeIndex = chosenModeIndices[ordinal]
            val mode = modes[modeIndex]
            if (mode.targetRequirements.isEmpty()) {
                targetsAccum = targetsAccum + listOf(emptyList())
                ordinal++
                continue
            }

            // Find legal targets per requirement. If any required slot has no legal
            // targets (and is mandatory), this mode can't resolve — surface an error.
            val legalTargetsMap = mutableMapOf<Int, List<EntityId>>()
            val requirementInfos = mode.targetRequirements.mapIndexed { index, req ->
                val legal = targetFinder.findLegalTargets(state, req, casterId, cardId)
                legalTargetsMap[index] = legal
                com.wingedsheep.engine.core.TargetRequirementInfo(
                    index = index,
                    description = req.description,
                    minTargets = req.effectiveMinCount,
                    maxTargets = req.count
                )
            }
            val allSatisfied = requirementInfos.all { info ->
                (legalTargetsMap[info.index]?.isNotEmpty() == true) || info.minTargets == 0
            }
            if (!allSatisfied) {
                return ExecutionResult.error(state, "No legal targets for mode: ${mode.description}")
            }

            val decisionId = java.util.UUID.randomUUID().toString()
            val pickNumber = ordinal + 1
            val prompt = "Choose targets for $cardName — ${mode.description} ($pickNumber of ${chosenModeIndices.size})"
            val decision = com.wingedsheep.engine.core.ChooseTargetsDecision(
                id = decisionId,
                playerId = casterId,
                prompt = prompt,
                context = DecisionContext(
                    sourceId = cardId,
                    sourceName = cardName,
                    phase = DecisionPhase.CASTING,
                    effectHint = mode.description
                ),
                targetRequirements = requirementInfos,
                legalTargets = legalTargetsMap,
                // Cast-time per-mode target selection must be cancellable (K2 in plan):
                // the pause sits before cost payment, so aborting rolls back cleanly.
                canCancel = true
            )

            val continuation = com.wingedsheep.engine.core.CastModalTargetSelectionContinuation(
                decisionId = decisionId,
                cardId = cardId,
                casterId = casterId,
                baseCastAction = baseCastAction,
                modes = modes,
                chosenModeIndices = chosenModeIndices,
                resolvedModeTargets = targetsAccum,
                currentOrdinal = ordinal
            )

            val pausedState = state
                .pushContinuation(continuation)
                .withPendingDecision(decision)
                .withPriority(casterId)

            return ExecutionResult.paused(
                pausedState,
                decision,
                listOf(
                    DecisionRequestedEvent(
                        decisionId = decisionId,
                        playerId = casterId,
                        decisionType = "CHOOSE_TARGETS",
                        prompt = decision.prompt
                    )
                )
            )
        }

        // All modes resolved without needing another decision — finalize directly.
        return finalizeModalCast(state, baseCastAction, chosenModeIndices, targetsAccum)
    }

    /**
     * Complete a choose-N modal cast by re-entering [execute] with a finalized
     * [CastSpell] action. `chosenModes`, `modeTargetsOrdered`, and the flat `targets`
     * union are populated so the normal cost / target / stack flow runs exactly once.
     */
    internal fun finalizeModalCast(
        state: GameState,
        baseCastAction: CastSpell,
        chosenModeIndices: List<Int>,
        resolvedModeTargets: List<List<ChosenTarget>>
    ): ExecutionResult {
        val flatTargets = resolvedModeTargets.flatten()
        val finalAction = baseCastAction.copy(
            chosenModes = chosenModeIndices,
            modeTargetsOrdered = resolvedModeTargets,
            targets = flatTargets
        )
        return execute(state, finalAction)
    }

    /**
     * Slice a flat target list into per-mode groups using each chosen mode's total
     * target slot count. Used when an action arrives with [CastSpell.chosenModes] and
     * [CastSpell.targets] populated but [CastSpell.modeTargetsOrdered] empty (the
     * web-client choose-1 modal cast path), so resolution can read targets per mode.
     *
     * If the flat target count doesn't line up with the modes' summed slot counts
     * (truncated, missing optional slots, etc.), returns an empty list — the cast
     * proceeds with the pre-existing flat-targets behavior rather than risking a
     * mis-sliced binding.
     */
    private fun deriveModeTargetsFromFlat(
        modalEffect: com.wingedsheep.sdk.scripting.effects.ModalEffect,
        chosenModes: List<Int>,
        flatTargets: List<ChosenTarget>
    ): List<List<ChosenTarget>> {
        // Choose-1: all flat targets belong to the single chosen mode. Using the mode's
        // max `count` here would mis-slice "up to N target" modes when the player picks
        // fewer than the maximum (e.g. Dewdrop Cure's "return up to two/three").
        if (chosenModes.size == 1) {
            return listOf(flatTargets.toList())
        }

        val perModeSlotCounts = chosenModes.map { idx ->
            modalEffect.modes.getOrNull(idx)?.targetRequirements?.sumOf { it.count } ?: 0
        }
        if (perModeSlotCounts.sum() != flatTargets.size) return emptyList()

        val result = mutableListOf<List<ChosenTarget>>()
        var cursor = 0
        for (slotCount in perModeSlotCounts) {
            result.add(flatTargets.subList(cursor, cursor + slotCount).toList())
            cursor += slotCount
        }
        return result
    }

    /**
     * Apply a single [com.wingedsheep.sdk.scripting.effects.ManaSpellRider] to a
     * spell on the stack. Each rider variant maps to either a state mutation on
     * the spell card (e.g. stamping a component) or a [PendingTrigger] that is
     * queued onto the stack above the spell (for riders whose effect needs the
     * stack — typically because it requires a player decision like scry).
     */
    private fun applyManaSpellRider(
        state: GameState,
        action: CastSpell,
        cardComponent: CardComponent,
        rider: com.wingedsheep.sdk.scripting.effects.ManaSpellRider
    ): Pair<GameState, List<PendingTrigger>> = when (rider) {
        is com.wingedsheep.sdk.scripting.effects.ManaSpellRider.MakesSpellUncounterable ->
            state.updateEntity(action.cardId) { c -> c.with(CantBeCounteredComponent) } to emptyList()

        is com.wingedsheep.sdk.scripting.effects.ManaSpellRider.ScryOnSharedTypeWithCommander ->
            buildScryOnSharedTypeWithCommanderTrigger(state, action, cardComponent, rider.amount)
    }

    /**
     * Path of Ancestry's rider: if the cast spell is a creature spell that shares
     * a creature type with any of the controller's commanders, queue a scry trigger
     * above the spell. Otherwise no-op.
     *
     * Subtypes are read from base [CardComponent] for both the spell (it's on the
     * stack, not the battlefield, so projected battlefield state doesn't apply) and
     * for each commander (looked up via [com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent]
     * and the [CardRegistry]). This matches the printed Scryfall ruling that the
     * commander's creature types are checked at the moment the mana is spent.
     */
    private fun buildScryOnSharedTypeWithCommanderTrigger(
        state: GameState,
        action: CastSpell,
        cardComponent: CardComponent,
        amount: Int,
    ): Pair<GameState, List<PendingTrigger>> {
        if (!cardComponent.typeLine.isCreature) return state to emptyList()
        val spellSubtypes = cardComponent.typeLine.subtypes.mapTo(mutableSetOf()) { it.value.lowercase() }
        if (spellSubtypes.isEmpty()) return state to emptyList()

        val registry = state.getEntity(action.playerId)
            ?.get<com.wingedsheep.engine.state.components.identity.CommanderRegistryComponent>()
            ?: return state to emptyList()
        val sharesType = registry.commanderIds.any { commanderId ->
            val commanderCard = state.getEntity(commanderId)?.get<CardComponent>() ?: return@any false
            val commanderTypes = commanderCard.typeLine.subtypes
                .mapTo(mutableSetOf()) { it.value.lowercase() }
            commanderTypes.any { it in spellSubtypes }
        }
        if (!sharesType) return state to emptyList()

        val scryAbility = TriggeredAbility(
            id = AbilityId.generate(),
            trigger = SdkGameEvent.SpellCastEvent(player = Player.You),
            binding = TriggerBinding.SELF,
            effect = com.wingedsheep.sdk.dsl.Patterns.Library.scry(amount),
            activeZone = Zone.STACK,
            descriptionOverride = "Scry $amount"
        )
        val pending = PendingTrigger(
            ability = scryAbility,
            sourceId = action.cardId,
            sourceName = cardComponent.name,
            controllerId = action.playerId,
            triggerContext = TriggerContext(
                triggeringEntityId = action.cardId,
                triggeringPlayerId = action.playerId
            )
        )
        return state to listOf(pending)
    }

    companion object {
        fun create(services: EngineServices): CastSpellHandler {
            return CastSpellHandler(
                services.cardRegistry,
                services.turnManager,
                services.manaSolver,
                services.costCalculator,
                services.alternativePaymentHandler,
                services.costHandler,
                services.stackResolver,
                services.targetValidator,
                services.conditionEvaluator,
                services.triggerDetector,
                services.triggerProcessor,
                services.manaAbilitySideEffectExecutor,
                services.targetFinder
            )
        }
    }
}
