package com.wingedsheep.engine.handlers.actions.spell
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.giftKeyword

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
import com.wingedsheep.engine.mechanics.DisturbCasts
import com.wingedsheep.engine.mechanics.EmergeCasts
import com.wingedsheep.engine.mechanics.SpliceCasts
import com.wingedsheep.engine.mechanics.EscalateCosts
import com.wingedsheep.engine.mechanics.FlashbackGrants
import com.wingedsheep.engine.mechanics.ModalChooseCounts
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.mechanics.MayhemGrants
import com.wingedsheep.engine.mechanics.SneakWindow
import com.wingedsheep.engine.mechanics.WebSlinging
import com.wingedsheep.engine.mechanics.WarpGrants
import com.wingedsheep.engine.mechanics.MiracleGrants
import com.wingedsheep.engine.mechanics.mana.paymentSubtypesOf
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.tap
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
import com.wingedsheep.engine.handlers.effects.bend.BendEvents
import com.wingedsheep.engine.handlers.effects.life.LifePaymentService
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
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
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.permissions.activeMayPlayFor
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.PlayerCantPlayFromHandComponent
import com.wingedsheep.engine.state.components.player.CantCastFromNonHandZonesComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.TapReason
import com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PermanentCostAction
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.Duration
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
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
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

/**
 * True if this cast is paying the card's cleave cost (CR 702.148). Cleave is an alternative cost,
 * so it's driven by [CastSpell.useAlternativeCost] gated on the chosen [AlternativeCostType.CLEAVE]
 * (never by `declaredCostSlot`, which names an *additional* cost). When true, the resolver swaps in the
 * brackets-removed effect / target-requirement variant (`cleaveSpellEffect` /
 * `cleaveTargetRequirements`).
 */
private fun isCleaveCast(action: CastSpell, cardDef: com.wingedsheep.sdk.model.CardDefinition): Boolean =
    action.useAlternativeCost &&
        action.altAllows(AlternativeCostType.CLEAVE) &&
        cardDef.keywordAbilities.any { it is KeywordAbility.Cleave }

/**
 * The card's optional-additional-cost keywords matching the slot this cast declared (CR 601.2b) —
 * empty when the cast declared none, or when the card has no keyword for the declared slot (which
 * `validate` turns into a rejection). A card can carry two entries for one slot (a mana kicker
 * alongside a sacrifice kicker), hence a list: the mana portion and the non-mana portion are read
 * separately.
 */
private fun declaredOptionalCosts(
    action: CastSpell,
    cardDef: com.wingedsheep.sdk.model.CardDefinition?,
): List<KeywordAbility.OptionalAdditionalCost> {
    val slot = action.declaredCostSlot ?: return emptyList()
    return cardDef?.keywordAbilities
        ?.filterIsInstance<KeywordAbility.OptionalAdditionalCost>()
        ?.filter { it.declaredSlot == slot }
        ?: emptyList()
}

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
        // Mayhem (CR 702.187, e.g. Swarm, Being of Bees) — cast from graveyard for its mayhem cost
        // if you discarded it this turn; `hasMayhemPermission` checks the keyword + the gate.
        val hasMayhem = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasHarmonize &&
            action.useAlternativeCost && action.altAllows(AlternativeCostType.MAYHEM) &&
            zoneResolver.hasMayhemPermission(state, action.playerId, action.cardId)
        val hasGraveyardCast = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasHarmonize && !hasMayhem &&
            zoneResolver.hasMayCastFromGraveyardPermission(state, action.playerId, action.cardId, cardComponent)
        val hasForageFromGraveyard = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasHarmonize && !hasMayhem && !hasGraveyardCast &&
            zoneResolver.hasMayCastCreaturesFromGraveyardWithForage(state, action.playerId, action.cardId, cardComponent)
        // Warp from graveyard (e.g., Timeline Culler) — `hasWarpPermission` already
        // checks both hand and graveyard; this branch covers the graveyard case
        // when `inHand` is false.
        val hasWarpFromGraveyard = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasHarmonize && !hasMayhem && !hasGraveyardCast && !hasForageFromGraveyard &&
            action.useAlternativeCost &&
            zoneResolver.hasWarpPermission(state, action.playerId, action.cardId)
        val hasCommanderCast = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasHarmonize && !hasMayhem && !hasGraveyardCast && !hasForageFromGraveyard && !hasWarpFromGraveyard &&
            zoneResolver.hasCommanderCastPermission(state, action.playerId, action.cardId)
        // Granted graveyard sneak (Ninja Teen): a creature card in the player's graveyard while they
        // control an active "creature cards in your graveyard have sneak {cost}" grant.
        val hasGraveyardSneak = !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasHarmonize && !hasMayhem && !hasGraveyardCast && !hasForageFromGraveyard && !hasWarpFromGraveyard && !hasCommanderCast &&
            action.useAlternativeCost && action.altAllows(AlternativeCostType.SNEAK) &&
            cardComponent.typeLine.isCreature &&
            action.cardId in state.getGraveyard(action.playerId) &&
            SneakWindow.graveyardSneakGrantCost(state, action.playerId, cardRegistry) != null
        // Disturb (CR 702.146a) — cast transformed from your graveyard for the disturb cost. The
        // face this cast puts on the stack is the back face, and it drives timing and targeting
        // below (CR 712.8c), so the permission check hands back the face itself.
        val disturbFace = if (
            !inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone &&
            action.useAlternativeCost && action.altAllows(AlternativeCostType.DISTURB)
        ) {
            zoneResolver.disturbCastFace(state, action.playerId, action.cardId)
        } else null
        if (!inHand && !onTopOfLibrary && !mayPlayFromExile && !mayCastFromZone && !mayCastFromGraveyard && !hasFlashback && !hasHarmonize && !hasMayhem && !hasGraveyardCast && !hasForageFromGraveyard && !hasWarpFromGraveyard && !hasCommanderCast && !hasGraveyardSneak && disturbFace == null) {
            return "Card is not in your hand"
        }

        // Modal DFC back face (CR 712.11b) — the hand-side counterpart of disturb. The caster chose
        // the back face, so the card goes on the stack transformed for that face's own mana cost.
        // No zone guard is needed beyond the resolver's own (it only looks in hand), and the
        // in-hand check above has already passed.
        val modalBackFace = if (
            action.useAlternativeCost && action.altAllows(AlternativeCostType.MODAL_BACK_FACE)
        ) {
            zoneResolver.modalBackCastFace(state, action.playerId, action.cardId)
        } else null

        // The face this cast puts on the stack when it is cast **transformed** (CR 712.8c / 712.8f)
        // — it drives timing, targeting, the aura target, colors and subtypes below, which must all
        // read this face rather than the printed front. Three sources, all meaning "back face up on
        // the stack": disturb's printed keyword, the modal-DFC face choice above, and a may-play
        // permission granted with `castTransformed` (CR 310.11b — "exile it, then you may cast it
        // transformed"). The zone legality of the last was already settled by `mayPlayFromExile` /
        // `mayCastFromZone` above, so that lookup only answers *which face*.
        val transformedFace = disturbFace
            ?: modalBackFace
            ?: zoneResolver.permissionTransformedCastFace(state, action.playerId, action.cardId)

        // Gift (CR 702.174a): the promise is an additional cost whose "payment" is choosing an
        // opponent, so the recipient must be an opponent of the caster and the card must actually
        // have gift.
        action.giftRecipient?.let { recipient ->
            val giftCard = cardRegistry.getCard(cardComponent.cardDefinitionId)
            if (giftCard?.giftKeyword() == null) {
                return "${cardComponent.name} has no gift cost to promise"
            }
            if (recipient !in state.getOpponents(action.playerId)) {
                return "A gift can only be promised to an opponent"
            }
        }

        // Memory Vessel: "they can't play cards from their hand" — hand-scoped, so casts from
        // exile/graveyard granted by a may-play permission still resolve.
        if (inHand && state.getEntity(action.playerId)?.has<PlayerCantPlayFromHandComponent>() == true) {
            return "You can't play cards from your hand"
        }

        // Avatar's Wrath: "your opponents can't cast spells from anywhere other than their hands."
        // A per-player, duration-bounded restriction to hand-only casting — any non-hand cast
        // (flashback/escape from graveyard, foretell/plot/may-play from exile, library top,
        // command zone) is illegal while the component is present. Ordinary hand casts (inHand)
        // are untouched.
        if (!inHand && state.getEntity(action.playerId)?.has<CantCastFromNonHandZonesComponent>() == true) {
            return "You can't cast spells from anywhere other than your hand right now"
        }

        // Single cast-legality chokepoint: per-turn spell limit (Yawgmoth's Agenda),
        // Silence-style can't-cast, Mana Maze color sharing, and PlayersCantCastSpells
        // (Voice of Victory, …) all resolve to a reason here, or null if the cast is allowed.
        castPermissionUtils.reasonCannotCast(state, action.playerId, action.cardId)?.let { return it }

        if (hasForageFromGraveyard) {
            // The spell being cast can't be one of the three cards it exiles to pay for itself, so
            // it's excluded from the forage exile pool here just as it is at payment time.
            if (!com.wingedsheep.engine.handlers.costs.ForageCostResolver.canPay(state, action.playerId, excludeCardId = action.cardId)) {
                return "Cannot forage: need 3 other cards in graveyard or a Food"
            }
        }

        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        // A may-play permission authorizes exactly one set of characteristics. By default that is
        // the card's primary face; a prepare-spell copy (Secrets of Strixhaven) or a permission
        // carrying `castFaceIndex` ("cast it from your graveyard as an Adventure" — Mosswood
        // Dreadknight, CR 715.3) authorizes an alternative face instead. `faceIndex` is
        // client-supplied, so reject any face the permission doesn't cover — otherwise a
        // hand-constructed action could cast the cheap Adventure half of a card that was only
        // granted its creature half, or vice versa.
        // Only permissions constrain faces. `mayPlayFromExile` is also true for a linked-exile
        // static grant (Valgavoth, Maralen), which carries no permission and no face notion — an
        // empty permission list means the authorization came from elsewhere, so leave it alone.
        if (mayPlayFromExile) {
            val permissions =
                state.activeMayPlayFor(action.cardId, action.playerId, conditionEvaluator, cardRegistry)
            if (permissions.isNotEmpty()) {
                val isPrepareCopy = container.has<PreparedSpellCopyComponent>() &&
                    cardDef?.layout == com.wingedsheep.sdk.model.CardLayout.PREPARE
                val authorizedFaces: Set<Int?> =
                    if (isPrepareCopy) setOf(0) else permissions.map { it.castFaceIndex }.toSet()
                if (action.faceIndex !in authorizedFaces) {
                    val faceName = action.faceIndex
                        ?.let { cardDef?.cardFaces?.getOrNull(it)?.name }
                        ?: cardComponent.name
                    return "You don't have permission to cast $faceName from there"
                }
                // "You may cast red spells from among them" (Chandra, Dressed to Kill −7). The
                // colour restriction is on the *spell*, so it is checked against the face being
                // cast — a red MDFC's blue back face is not castable through such a permission
                // even though the exiled card is red. Authoritative: the enumerator applies the
                // same rule, but the action is client-supplied.
                val castColors = action.faceIndex
                    ?.let { cardDef?.cardFaces?.getOrNull(it)?.manaCost?.colors }
                    ?: cardDef?.colors
                    ?: cardComponent.manaCost.colors
                if (permissions.none { it.castColorRestriction == null || it.castColorRestriction in castColors }) {
                    val required = permissions.firstNotNullOfOrNull { it.castColorRestriction }
                    return "You may only cast ${required?.name?.lowercase()} spells from there"
                }
            }
        }

        // Handle face-down casting — morph (CR 702.37a) or disguise (CR 702.168a). Both are
        // "cast this card face down as a 2/2 for {3}" at sorcery speed; the mode only decides
        // what the resulting permanent looks like and costs to turn up.
        if (action.castFaceDown) {
            val castableFaceDown = cardDef?.keywordAbilities?.any {
                it is KeywordAbility.Morph || it is KeywordAbility.Disguise
            } == true
            if (!castableFaceDown) {
                return "This card cannot be cast face down (no morph or disguise ability)"
            }

            if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "You can only cast face-down creatures at sorcery speed"
            }

            val morphCastCost = costCalculator.calculateFaceDownCost(state, action.playerId)
            return validatePayment(state, action, morphCastCost)
        }

        // Check timing — for Adventure / split faces use the face's type line (CR 715 / 709.4);
        // a disturb cast is timed by the back face it puts on the stack (CR 712.8c).
        val effectiveTypeLine = action.faceIndex
            ?.let { cardDef?.cardFaces?.getOrNull(it)?.typeLine }
            ?: transformedFace?.typeLine
            ?: cardComponent.typeLine
        // Sneak (CR 702.190a) grants an instant-speed casting permission during the active
        // player's declare blockers step — bypassing the normal sorcery-speed timing.
        val castingForSneak = action.useAlternativeCost &&
            action.altAllows(AlternativeCostType.SNEAK) &&
            cardDef != null &&
            SneakWindow.effectiveSneakCost(state, cardDef, action.cardId, action.playerId, cardRegistry) != null
        if (!effectiveTypeLine.isInstant) {
            // Printed flash comes off the same face as the type line above, for the same reason:
            // CR 712.11c evaluates only the face being cast, so a modal DFC whose *front* has flash
            // grants none to a sorcery-speed back. `transformedFace` is null for an ordinary cast,
            // which leaves this reading the card's own keywords. A *granted* flash below is a
            // property of the card object, not of a face, so it is unaffected.
            val faceKeywords = transformedFace?.keywords ?: cardDef?.keywords ?: emptySet()
            val hasFlash = faceKeywords.contains(Keyword.FLASH)
            val grantedFlash = hasFlash || zoneResolver.hasGrantedFlash(state, action.cardId)
            // A from-exile may-play permission with an "as though it had flash" rider (Azula,
            // Cunning Usurper) lets a non-instant exiled card be cast at instant speed (CR 702.8).
            val mayPlayFlash = state.activeMayPlayFor(action.cardId, action.playerId, conditionEvaluator, cardRegistry)
                .any { it.asThoughFlash }
            // A flash-timing kicker unlocks instant-speed casting when paid — whether the
            // optional cost is mana (Ghitu Fire) or a non-mana cost like Behold (Molten Exhale).
            val flashTimingKicker = declaredOptionalCosts(action, cardDef).any { it.grantsFlashTiming }
            if (!grantedFlash && !mayPlayFlash && !flashTimingKicker && !castingForSneak &&
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

        // Web-slinging (CR 702.188a): the player must return exactly one tapped creature they
        // control to its owner's hand as the non-mana portion of the alternative cost. Timing is
        // the spell's normal timing (checked above) — web-slinging grants no extra permission.
        val castingForWebSling = action.useAlternativeCost &&
            action.altAllows(AlternativeCostType.WEB_SLINGING) &&
            cardDef != null &&
            WebSlinging.effectiveWebSlinging(state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator) != null
        if (castingForWebSling) {
            val bounced = action.additionalCostPayment?.bouncedPermanents ?: emptyList()
            if (bounced.size != 1) {
                return "Web-slinging requires returning exactly one tapped creature you control to its owner's hand"
            }
            if (bounced.first() !in WebSlinging.tappedCreaturesYouControl(state, action.playerId)) {
                return "The chosen creature is not a tapped creature you control"
            }
        }

        // Emerge (CR 702.119a/c): the player must sacrifice exactly one creature they control as
        // the non-mana portion of the alternative cost, chosen as they choose to pay the emerge
        // cost (CR 601.2b). Timing is the spell's normal timing (checked above) — emerge grants no
        // extra permission. The chosen creature also fixes the generic reduction, so
        // computeTotalCastCost prices the cast against exactly this selection.
        val castingForEmerge = action.useAlternativeCost &&
            action.altAllows(AlternativeCostType.EMERGE) &&
            cardDef != null &&
            EmergeCasts.printedEmerge(cardDef) != null
        if (castingForEmerge) {
            val sacrificed = action.additionalCostPayment?.sacrificedPermanents ?: emptyList()
            if (sacrificed.size != 1) {
                return "Emerge requires sacrificing exactly one creature you control"
            }
            if (sacrificed.first() !in EmergeCasts.sacrificeCandidates(state, action.playerId)) {
                return "The permanent chosen for emerge is not a creature you control"
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

        // Gwenom: a spell cast from the top of the library under a PlayFromTopWithAlternativeCost
        // permission pays the grant's additional cost (pay life equal to its mana value).
        val topOfLibraryAdditionalCost = zoneResolver
            .topOfLibraryAlternativeGrant(state, action.playerId, action.cardId)?.additionalCost
        if (topOfLibraryAdditionalCost != null) {
            val topCostError = validateAdditionalCosts(state, listOf(topOfLibraryAdditionalCost), action)
            if (topCostError != null) return topCostError
        }

        // Validate a self-referential MayCastSelfFromZones grant's additional cost (e.g. Alien
        // Symbiosis: "cast this from your graveyard by discarding a card").
        val mayCastFromZoneAbility = zoneResolver.findMayCastSelfFromZoneAbility(state, action.playerId, action.cardId)
        val mayCastFromZoneAdditionalCost = mayCastFromZoneAbility?.additionalCost
        if (mayCastFromZoneAdditionalCost != null) {
            val zoneCostError = validateAdditionalCosts(state, listOf(mayCastFromZoneAdditionalCost), action)
            if (zoneCostError != null) return zoneCostError
        }

        // Validate runtime additional costs from PlayWithAdditionalCostComponent (e.g., The Infamous Cruelclaw)
        val runtimeAdditionalCostComponent = state.getEntity(action.cardId)
            ?.get<PlayWithAdditionalCostComponent>()
            ?.takeIf { it.controllerId == action.playerId }
        if (runtimeAdditionalCostComponent != null) {
            val runtimeCostError = validateAdditionalCosts(state, runtimeAdditionalCostComponent.additionalCosts, action)
            if (runtimeCostError != null) return runtimeCostError
        }

        // Validate the declared optional additional cost (kicker/offspring/bargain): the card must
        // actually have a keyword declaring that slot, so a hand-built action can't claim to have
        // bargained a kicker spell (or bargained a card with no bargain at all).
        if (action.declaredCostSlot != null && cardDef != null) {
            val declared = declaredOptionalCosts(action, cardDef)
            if (declared.isEmpty()) {
                val mechanic = when (action.declaredCostSlot) {
                    ChoiceSlot.BARGAINED -> "bargain"
                    ChoiceSlot.KICKED -> "kicker"
                    else -> action.declaredCostSlot.name.lowercase()
                }
                return "This card does not have $mechanic"
            }

            // Validate the non-mana portion (sacrifice a creature for kicker, an artifact /
            // enchantment / token for bargain, …).
            val declaredAdditionalCost = declared.firstOrNull { it.additionalCost != null }?.additionalCost
            if (declaredAdditionalCost != null) {
                val costError = validateAdditionalCosts(state, listOf(declaredAdditionalCost), action)
                if (costError != null) return costError
            }
        }

        // Validate self-alternative cost's additional costs when using alternative cost
        if (action.useAlternativeCost && cardDef != null && action.altAllows(AlternativeCostType.SELF_ALTERNATIVE)) {
            val selfAltCost = cardDef.script.selfAlternativeCost
            // "…rather than pay this spell's mana cost **if** <condition>" (Blasphemous Edict).
            // Mirrors the availability gate in CastSpellEnumerator so an authorization can't
            // outlive the enumeration that offered it.
            val selfAltCondition = selfAltCost?.condition
            if (selfAltCondition != null && !conditionEvaluator.evaluate(
                    state,
                    selfAltCondition,
                    EffectContext(sourceId = action.cardId, controllerId = action.playerId)
                )
            ) {
                return "Alternative cost is not available: ${selfAltCondition.description}"
            }
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

        // Validate splice (CR 702.47). Each revealed card must be in the caster's hand, carry splice,
        // splice onto a quality this spell actually has, and appear at most once. Checked before the
        // cost is computed, because each splice cost is folded into the total cost below (CR 601.2b/f).
        if (action.splicedCardIds.isNotEmpty()) {
            val spliceError = validateSplice(state, action, cardDef, cardComponent, transformedFace)
            if (spliceError != null) return spliceError
        }

        // Calculate effective cost (free if PlayWithoutPayingCostComponent is present, or if a
        // MayCastWithoutPayingManaCost battlefield source (e.g. Weftwalking) is the chosen alt).
        val playForFreeFromComponent = zoneResolver.hasPlayWithoutPayingCost(state, action.playerId, action.cardId)
        if (action.useWithoutPayingManaCost) {
            // CR 118.9a — only one alternative cost can apply to a given cast.
            if (action.useAlternativeCost) {
                return "Cannot combine 'without paying its mana cost' with another alternative cost"
            }
            // Pass the spell's origin zone so a `fromExileOnly` source (Warped Space) validates an
            // exile cast while staying withheld from hand casts.
            if (!costCalculator.hasFreeCastPermission(state, action.playerId, cardDef, castSourceZone(state, action.cardId))) {
                return "'Without paying its mana cost' is not available (gate closed or no source on the battlefield)"
            }
        }
        val playForFree = playForFreeFromComponent || action.useWithoutPayingManaCost
        val computedCost = computeTotalCastCost(state, action, cardDef, cardComponent, playForFree, hasCommanderCast)
            ?: return "No alternative casting cost available"
        val paymentError = validatePayment(state, action, computedCost.cost, computedCost.paymentXValue)
        if (paymentError != null) {
            return paymentError
        }

        // Validate targets (include auraTarget as a target requirement for aura spells)
        // Use mode-specific targets for modal spells, kickerTargetRequirements when kicked
        if (cardDef != null) {
            // Adventure / split face cast (CR 715 / 709) — read targets from the face's script.
            // A disturb cast reads the back face's script instead (CR 712.8c): the Innistrad
            // disturb cycle's Aura backs choose what to enchant as the spell is cast.
            val faceScript = action.faceIndex?.let { cardDef.cardFaces.getOrNull(it)?.script }
                ?: transformedFace?.script
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
            } else if (action.declaredCostSlot != null && cardDef.script.kickerTargetRequirements.isNotEmpty()) {
                cardDef.script.kickerTargetRequirements
            } else if (isCleaveCast(action, cardDef) && cardDef.script.cleaveTargetRequirements.isNotEmpty()) {
                // Cleave (CR 702.148): removing bracketed text can change the legal target set
                // (e.g. Fierce Retribution's "target [attacking] creature" → "target creature").
                cardDef.script.cleaveTargetRequirements
            } else {
                effectiveScript.targetRequirements
            }
            val targetRequirements = buildList {
                addAll(baseTargetReqs)
                (transformedFace ?: cardDef).script.auraTarget?.let { add(it) }
                // Splice (CR 702.47d): targets for the added text are chosen normally, as part of
                // casting this spell. They sit after the main spell's own requirements, so the flat
                // target list splits into the main slice followed by one slice per spliced card.
                addAll(SpliceCasts.targetRequirementsFor(state, action.splicedCardIds, cardRegistry))
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
                    sourceColors = (transformedFace ?: cardDef).colors,
                    sourceSubtypes = (transformedFace ?: cardDef).typeLine.subtypes.map { it.value }.toSet(),
                    sourceId = action.cardId,
                    xValue = action.xValue
                )
                if (targetError != null) {
                    return targetError
                }
            }
        }

        // Validate damage distribution for DividedDamageEffect spells
        // Use kickerSpellEffect when kicked, cleaveSpellEffect when cleaved, else the printed effect.
        val spellEffect = if (action.declaredCostSlot != null && cardDef?.script?.kickerSpellEffect != null) {
            cardDef.script.kickerSpellEffect
        } else if (cardDef != null && isCleaveCast(action, cardDef) && cardDef.script.cleaveSpellEffect != null) {
            cardDef.script.cleaveSpellEffect
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
    /**
     * Sacrifice a permanent paid as an additional cost of casting, routing the zone move through
     * the canonical [ZoneTransitionService] (the single source of truth for zone transitions).
     *
     * This is the *cost* analogue of [com.wingedsheep.engine.handlers.effects.zones.SacrificeExecutor]
     * (the *effect* "Sacrifice a creature: …"). Both must go through [ZoneTransitionService.moveToZone]
     * so the emitted [ZoneChangeEvent] carries the last-known-information snapshot (CR 603.10 /
     * 608.2h) *and* the full exit cleanup + graveyard-replacement redirect run. Dies/leaves triggers
     * that read the dying permanent's counters, power/toughness, keywords, or token-ness (e.g.
     * Explorer's Cache: "Whenever a creature you control with a +1/+1 counter on it dies …") only
     * fire when that snapshot is present — a hand-built `ZoneChangeEvent(lastKnown = null)` silently
     * drops them. [ZoneTransitionService.trackPermanentSacrifice] first marks the permanent so the
     * resulting event is tagged `wasSacrificed = true` (CR 701.21), honoring "if it wasn't
     * sacrificed" triggers.
     */
    private fun sacrificePermanentAsCost(
        state: GameState,
        permId: EntityId,
        sacrificingPlayerId: EntityId,
        events: MutableList<GameEvent>,
    ): GameState {
        val permName = state.getEntity(permId)?.get<CardComponent>()?.name
        val tracked = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
            .trackPermanentSacrifice(state, listOf(permId), sacrificingPlayerId)
        events.add(PermanentsSacrificedEvent(sacrificingPlayerId, listOf(permId), listOfNotNull(permName)))
        val transition = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
            .moveToZone(tracked, permId, Zone.GRAVEYARD)
        events.addAll(transition.events)
        return transition.state
    }

    /**
     * The waterbend amount this cast adds to its mana cost (Avatar: The Last Airbender), or 0 when
     * the spell has no waterbend additional cost, or its *optional* cost was declined. For
     * "waterbend {X}" the amount is the cast-time X ([CastSpell.xValue]).
     */
    private fun spellWaterbendAmount(
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        action: CastSpell,
    ): Int {
        val wb = cardDef.script.spellWaterbend ?: return 0
        val paid = !wb.optional || action.wasWaterbendPaid
        if (!paid) return 0
        return if (wb.isX) (action.xValue ?: 0) else wb.amount
    }

    /**
     * The generic amount of a waterbend-flagged *fixed alternative* cost this cast can pay by
     * tapping artifacts/creatures, or 0 when the cast has no such cost. Hama, the Bloodbender exiles
     * a card and grants a `PlayWithFixedAlternativeManaCostComponent(waterbend = true)` whose whole
     * fixed cost is `{mana value}` generic and entirely waterbend-reducible (CR 701.67). Unlike a
     * spell-level `waterbend {N}` additional cost — which is capped so taps never eat the printed
     * generic — the fixed alternative cost *replaces* the printed cost, so the cap is the whole cost.
     */
    private fun fixedAltWaterbendAmount(
        state: GameState,
        action: CastSpell,
        playForFree: Boolean,
    ): Int {
        if (playForFree) return 0
        val comp = state.getEntity(action.cardId)
            ?.get<PlayWithFixedAlternativeManaCostComponent>()
            ?.takeIf { it.controllerId == action.playerId && it.waterbend }
            ?: return 0
        return comp.fixedCost.genericAmount
    }

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

    /** The [cost] and adjusted X actually charged as mana at payment time for a cast. */
    private data class ComputedCastCost(val cost: ManaCost, val paymentXValue: Int)

    /**
     * The full mana-cost pipeline for a cast (CR 601.2f): alternative-cost base selection
     * (flashback/harmonize/warp/sneak/evoke/impending/miracle/…), kicker, Or-Pay additional
     * costs, waterbend, airbend fixed-alternative plus runtime cost increases,
     * sacrifice-for-reduction, and delve/convoke/waterbend/improvise alternative-payment
     * reductions — plus the harmonize/waterbend adjustment to the X actually paid as mana.
     *
     * Shared by [validate] and the cast-time modal affordability gate
     * ([canPayModeSelection]) so mode offers can never diverge from what payment will
     * actually charge. Returns null when the action requests an alternative cost but none
     * is available.
     */
    private fun computeTotalCastCost(
        state: GameState,
        action: CastSpell,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        cardComponent: CardComponent,
        playForFree: Boolean,
        castingFromCommandZone: Boolean
    ): ComputedCastCost? {
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
            // Check flashback cost first (printed, granted per-entity by Archmage's Newt, or
            // granted to the whole graveyard by a battlefield static — Iroh, Grand Lotus).
            val flashbackAbility = FlashbackGrants.effectiveFlashback(
                state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator
            )
            // Harmonize may be printed on the card or granted at runtime (Songcrafter Mage).
            val harmonizeAbility = HarmonizeGrants.effectiveHarmonize(state, action.cardId, cardDef)
            // The back face of a modal DFC whose back is a permanent, when this card is one and is
            // in hand (CR 712.11b). Resolved once here alongside the other face/keyword lookups so
            // the branch below can both test it and read its cost.
            val modalBackFace = zoneResolver.modalBackCastFace(state, action.playerId, action.cardId)
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
            } else if (action.altAllows(AlternativeCostType.MAYHEM) &&
                MayhemGrants.effectiveMayhem(state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator) != null &&
                zoneResolver.hasMayhemPermission(state, action.playerId, action.cardId)) {
                // Mayhem cost (CR 702.187) — cast from graveyard for its mayhem cost.
                costCalculator.calculateEffectiveCostWithAlternativeBase(
                    state, cardDef, MayhemGrants.effectiveMayhem(state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator)!!.cost, action.playerId
                )
            } else if (action.altAllows(AlternativeCostType.DISTURB) &&
                DisturbCasts.printedDisturb(cardDef) != null &&
                zoneResolver.disturbCastFace(state, action.playerId, action.cardId) != null) {
                // Disturb cost (CR 702.146a) — printed on the front face, which is also the face the
                // battlefield cost-modifier pipeline is applied against (the spell's mana value comes
                // from the front face, CR 712.8c).
                costCalculator.calculateEffectiveCostWithAlternativeBase(
                    state, cardDef, DisturbCasts.printedDisturb(cardDef)!!.cost, action.playerId
                )
            } else if (action.altAllows(AlternativeCostType.MODAL_BACK_FACE) && modalBackFace != null) {
                // Modal DFC back face (CR 712.11b) — you pay that face's *own* printed mana cost,
                // not an alternative one. It still runs through the alternative-base path so
                // battlefield cost modifiers apply; and unlike disturb the base is the back face's
                // cost, because CR 712.8f gives a modal back face its own mana value.
                costCalculator.calculateEffectiveCostWithAlternativeBase(
                    state, cardDef, modalBackFace.manaCost, action.playerId
                )
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
                    // Check sneak cost (CR 702.190 — mana portion; the bounce is paid separately).
                    // The effective sneak cost is the printed Sneak, or a granted graveyard sneak
                    // (Ninja Teen: "creature cards in your graveyard have sneak {3}{B}").
                    val sneakCost = SneakWindow.effectiveSneakCost(state, cardDef, action.cardId, action.playerId, cardRegistry)
                    // Check web-slinging cost (CR 702.188 — an alternative cost bundling a
                    // return-a-tapped-creature payment, cast at the spell's normal timing).
                    val webSlingingAbility = WebSlinging.effectiveWebSlinging(state, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator)
                    // Check evoke cost
                    val evokeAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Evoke>().firstOrNull()
                    // Check dash cost (CR 702.109 — hand only, printed only for now).
                    val dashAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Dash>().firstOrNull()
                    // Check emerge cost (CR 702.119 — mana portion; the sacrifice is paid separately).
                    val emergeAbility = EmergeCasts.printedEmerge(cardDef)
                    if (action.altAllows(AlternativeCostType.SNEAK) && sneakCost != null) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, sneakCost, action.playerId)
                    } else if (action.altAllows(AlternativeCostType.WEB_SLINGING) && webSlingingAbility != null) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, webSlingingAbility.cost, action.playerId)
                    } else if (action.altAllows(AlternativeCostType.EVOKE) && evokeAbility != null) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, evokeAbility.cost, action.playerId)
                    } else if (action.altAllows(AlternativeCostType.EMERGE) && emergeAbility != null) {
                        // CR 702.119a — the emerge cost, then reduced by an amount of *generic*
                        // mana equal to the sacrificed creature's mana value. The reduction lands
                        // after the battlefield cost-modifier pipeline because it is a cost
                        // reduction (CR 601.2f applies increases before reductions), and the
                        // creature is still on the battlefield here: it is sacrificed only as the
                        // total cost is paid (CR 601.2h), which execute() does after mana payment.
                        EmergeCasts.reduceForSacrifice(
                            costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, emergeAbility.cost, action.playerId),
                            state,
                            action.additionalCostPayment?.sacrificedPermanents?.firstOrNull()
                        )
                    } else if (action.altAllows(AlternativeCostType.DASH) && dashAbility != null && zoneResolver.hasDashPermission(state, action.playerId, action.cardId)) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, dashAbility.cost, action.playerId)
                    } else {
                        // Check impending cost
                        val impendingAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Impending>().firstOrNull()
                        // Check cleave cost (CR 702.148 — an alternative cost; the brackets-removed
                        // text variant is chosen structurally at resolution, not here).
                        val cleaveAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Cleave>().firstOrNull()
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
                        } else if (action.altAllows(AlternativeCostType.CLEAVE) && cleaveAbility != null) {
                            costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, cleaveAbility.cost, action.playerId)
                        } else if (action.altAllows(AlternativeCostType.MIRACLE) && miracleAbility != null) {
                            costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, miracleAbility.cost, action.playerId)
                        } else {
                            // Check self-alternative cost (e.g., Zahid's {3}{U} + tap artifact)
                            val selfAltCost = cardDef.script.selfAlternativeCost
                            if (action.altAllows(AlternativeCostType.SELF_ALTERNATIVE) && selfAltCost != null) {
                                val altMana = selfAltCost.manaCost
                                costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altMana, action.playerId)
                            } else if (action.altAllows(AlternativeCostType.GRANTED)) {
                                // Fall back to battlefield-granted alternative cost (e.g., Jodah's {W}{U}{B}{R}{G})
                                val altCosts = costCalculator.findAlternativeCastingCosts(state, action.playerId)
                                if (altCosts.isEmpty()) return null
                                costCalculator.calculateEffectiveCostWithAlternativeBase(state, cardDef, altCosts.first())
                            } else {
                                // A specific alternative cost was requested (e.g. DASH) but its own
                                // permission gate failed — never silently fall back to an unrelated
                                // battlefield-granted alternative cost the player didn't ask for.
                                return null
                            }
                        }
                    }
                }
            }
        } else if (cardDef != null) {
            // CR 202.1b/118.6: a card printed with genuinely no mana cost (Ancestral Vision)
            // represents an unpayable cost and can't be cast this way — every branch above
            // already covers the alternative costs and free-cast permissions that CAN play it
            // (Suspend routes through a completely separate free-cast pipeline and never reaches
            // this function at all). Defense in depth: CastSpellEnumerator never offers this as a
            // legal action in the first place. `hasNoManaCost` (not `manaCost` itself) is the
            // DSL-authored signal — a printed {0} stays normally castable, and test fixtures often
            // build `ManaCost.ZERO` directly to mean "free" without it implying "no mana cost."
            if (cardDef.hasNoManaCost) return null
            costCalculator.calculateEffectiveCost(
                state,
                cardDef,
                action.playerId,
                action.targets.map { it.toEntityId() },
                fromZone = if (castingFromCommandZone) Zone.COMMAND else castSourceZone(state, action.cardId),
                // Price the branch the player actually announced — a "costs {2} less to cast if
                // it's bargained" reduction is gated on the declaration (CR 702.166).
                declaredCostSlot = action.declaredCostSlot,
            )
        } else {
            cardComponent.manaCost
        }

        // Add kicker/offspring mana cost if kicked (only for mana-based kicker/offspring)
        if (!playForFree && !action.useAlternativeCost) {
            val kickerManaCost = declaredOptionalCosts(action, cardDef)
                .firstOrNull { it.manaCost != null }
                ?.manaCost
            if (kickerManaCost != null) {
                effectiveCost = ManaCost(effectiveCost.symbols + kickerManaCost.symbols)
            }
        }

        // Fold in the "… or pay {N}" alternative mana for every or-pay cost whose non-mana leg the
        // caster declined (validation side; execute() applies the same rule to the cost it charges).
        if (cardDef != null && !playForFree) {
            effectiveCost = applyOrPayManaAdjustments(
                effectiveCost, cardDef.script.additionalCosts, action.additionalCostPayment
            )
        }

        // Apply spell-level waterbend additional cost (Avatar: The Last Airbender). Adds the
        // waterbend amount {N} (or {X}) as generic mana; the tapped artifacts/creatures in
        // alternativePayment reduce that generic below, capped at N.
        if (cardDef != null && !playForFree) {
            val waterbendAmount = spellWaterbendAmount(cardDef, action)
            if (waterbendAmount > 0) {
                effectiveCost = effectiveCost + ManaCost.parse("{$waterbendAmount}")
            }
        }

        // Airbend: a fixed alternative cost ({2}) is paid *instead of* the printed cost — it
        // replaces the base. A cost increase (e.g. Soul Partition's tax, or a Thalia-style "costs
        // {1} more") is not part of the cost it replaces, so it still applies on top: an airbended
        // card cast under a {1}-tax costs {3}, not {2}.
        if (!playForFree) {
            val fixedAltCost = state.getEntity(action.cardId)
                ?.get<PlayWithFixedAlternativeManaCostComponent>()
                ?.takeIf { it.controllerId == action.playerId }
            if (fixedAltCost != null) {
                effectiveCost = fixedAltCost.fixedCost
            }
            // Apply runtime mana tax from exile permissions (e.g., Soul Partition) on top of
            // whichever base applies (printed cost, or the fixed alternative above).
            val runtimeCostIncrease = state.getEntity(action.cardId)
                ?.get<PlayWithCostIncreaseComponent>()
                ?.takeIf { it.controllerId == action.playerId }
            if (runtimeCostIncrease != null) {
                effectiveCost = effectiveCost + ManaCost.parse("{${runtimeCostIncrease.amount}}")
            }
        }

        // Splice (CR 702.47a): every revealed splice card's cost is an *additional* cost, so it lands
        // on top of whatever is paying for the spell itself — a free cast and an alternative cost both
        // waive only the mana cost, never the additional costs (CR 601.2f–h). Added after the airbend
        // branch above, which *replaces* effectiveCost outright and would otherwise wipe it.
        if (action.splicedCardIds.isNotEmpty()) {
            effectiveCost = SpliceCasts.addSpliceCosts(effectiveCost, state, action.splicedCardIds, cardRegistry)
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

        // Account for waterbend (Avatar): tapped artifacts/creatures reduce the waterbend generic,
        // capped at the waterbend amount. Two sources: a spell-level `waterbend {N}` additional cost
        // (capped so taps never eat the printed generic) and Hama's fixed-alternative waterbend cost
        // (the whole {mana value} is reducible). Only one is ever non-zero for a given cast.
        val validateWaterbendCap = (if (cardDef != null) spellWaterbendAmount(cardDef, action) else 0) +
            fixedAltWaterbendAmount(state, action, playForFree)
        val costAfterWaterbend = if (!playForFree && action.alternativePayment != null &&
            action.alternativePayment.tapForGenericPermanents.isNotEmpty() && validateWaterbendCap > 0
        ) {
            alternativePaymentHandler.calculateReducedCostForWaterbend(
                costAfterAltPayment, action.alternativePayment, validateWaterbendCap
            )
        } else {
            costAfterAltPayment
        }

        // Account for improvise (CR 702.126): each tapped artifact pays {1} of the generic in the
        // spell's *total* cost, with no cap beyond that generic. Shares the tap-for-generic carrier
        // with waterbend, and no card has both — the cap above being 0 is what tells them apart.
        val costAfterImprovise = if (!playForFree && cardDef != null && validateWaterbendCap == 0 &&
            action.alternativePayment != null && action.alternativePayment.tapForGenericPermanents.isNotEmpty()
        ) {
            alternativePaymentHandler.calculateReducedCostForImprovise(
                costAfterWaterbend, action.alternativePayment, cardDef, state, action.playerId
            )
        } else {
            costAfterWaterbend
        }

        // For an X-cost Harmonize cast where a creature is tapped, the
        // creature's power reduces generic mana — and {X} is generic (TDM release notes) —
        // so the leftover reduction beyond any printed generic comes off the X mana paid.
        // For a "waterbend {X}" spell the X is already materialized as generic in the cost
        // (and reduced by the waterbend taps), so it must NOT also be charged as {X} mana.
        val paymentXValue = if (cardDef?.script?.spellWaterbend?.isX == true) 0
            else harmonizePaymentXValue(state, action, cardDef, effectiveCost)
        return ComputedCastCost(costAfterImprovise, paymentXValue)
    }

    private fun validatePayment(state: GameState, action: CastSpell, cost: ManaCost, paymentXValue: Int = action.xValue ?: 0): String? {
        val xValue = paymentXValue

        // Build spell context for conditional mana validation
        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>()
        val spellCtx = if (action.castFaceDown) {
            // CR 708.2 — a face-down spell has none of the printed card's characteristics, so
            // conditional mana is judged against the nameless 2/2 creature it actually is.
            SpellPaymentContext.faceDownCast(isFromHand = isCastFromHand(state, action.cardId))
        } else if (cardComponent != null) {
            SpellPaymentContext(
                isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
                isKicked = action.declaredCostSlot == ChoiceSlot.KICKED,
                isCreature = cardComponent.typeLine.isCreature,
                isLegendary = cardComponent.typeLine.isLegendary,
                manaValue = cardComponent.manaCost.cmc,
                hasXInCost = cardComponent.manaCost.hasX,
                subtypes = paymentSubtypesOf(cardComponent),
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
                // Floating mana also covers the {X} portion (execution — explicitPay → autoPay —
                // spends it before tapping anything), so only ask the chosen sources for the X
                // the pool can't pay. Eligible restricted mana counts via ManaPool.xCoverage.
                val xSymbolCount = effectiveCost.xCount.coerceAtLeast(1)
                val totalXMana = xValue * xSymbolCount
                val xRemaining = totalXMana -
                    partial.newPool.xCoverage(totalXMana, xManaRestriction, spellCtx)
                if (remainingCost.isEmpty() && xRemaining == 0) {
                    null
                } else {
                    val chosen = action.paymentStrategy.manaAbilitiesToActivate.toSet()
                    val excluded = manaSolver.findAvailableManaSources(state, action.playerId)
                        .map { it.entityId }
                        .filter { it !in chosen }
                        .toSet()
                    if (manaSolver.solve(state, action.playerId, remainingCost, xRemaining, excludeSources = excluded, spellContext = spellCtx, xManaRestriction = xManaRestriction) == null) {
                        "Selected mana sources cannot pay this spell's cost"
                    } else null
                }
            }
        }
    }

    /**
     * True if mana of any type may be spent on this spell's mana cost (CR 118.14 / 609.4b). Two
     * independent sources:
     *
     * 1. A [com.wingedsheep.sdk.scripting.SpendAnyManaTypeForSpells] static controlled by the
     *    caster whose filter matches the card — the blanket "you can spend mana of any type to cast
     *    [these] spells" (Vizier of the Menagerie). Zone-agnostic, so it is checked first and covers
     *    hand and top-of-library casts too.
     * 2. A [com.wingedsheep.engine.state.permissions.MayPlayPermission] carrying the
     *    `withAnyManaType` rider. That is a *per-card* grant, so the card must currently be in a
     *    zone a may-play permission can grant casting from — exile (the card's owner's, which may be
     *    an opponent — e.g. Taster of Wares leaves the exiled card in the revealing player's exile)
     *    or a graveyard (per-card grants that leave the card in the graveyard — e.g. Tinybones, the
     *    Pickpocket lets you cast a targeted nonland permanent card from the damaged player's
     *    graveyard). An active permission must be granted to the casting player with its condition
     *    gate open, and the `withAnyManaType` flag must be set on at least one of them.
     */
    private fun isCastWithAnyManaType(state: GameState, action: CastSpell): Boolean {
        if (castPermissionUtils.canSpendAnyManaTypeForSpell(state, action.playerId, action.cardId)) {
            return true
        }
        val inGrantableZone = state.turnOrder.any { ownerId ->
            action.cardId in state.getZone(ZoneKey(ownerId, Zone.EXILE)) ||
                action.cardId in state.getZone(ZoneKey(ownerId, Zone.GRAVEYARD))
        }
        if (!inGrantableZone) return false
        return state.activeMayPlayFor(action.cardId, action.playerId, conditionEvaluator, cardRegistry)
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

    /**
     * Validate the splice declarations on this cast (CR 702.47).
     *
     * [GameAction] is client-supplied, so every leg of "you may reveal this card from your hand as you
     * cast a [quality] spell" is re-checked here rather than trusted: the card is still in the caster's
     * *hand* (it is revealed, never cast — CR 702.47a), it actually has splice, the quality it splices
     * onto is one this spell has, and no card is spliced onto the same spell twice (CR 702.47b).
     *
     * Also enforces CR 702.47b's "you can't choose to use a splice ability if you can't make the
     * required choices (targets, etc.) for that card's rules text" — a splice card whose text needs a
     * target has nothing to point at if no legal target exists, so it can't be spliced at all. The
     * target *validity* check itself runs with the rest of the cast's targets below.
     */
    private fun validateSplice(
        state: GameState,
        action: CastSpell,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?,
        cardComponent: CardComponent,
        transformedFace: com.wingedsheep.sdk.model.CardDefinition?,
    ): String? {
        // The quality is read off the face actually being cast (CR 702.47a checks the spell), so an
        // adventure / split cast — or a transformed one (disturb, modal DFC back, a `castTransformed`
        // permission) — is measured by the face on the stack, not the whole card.
        val castFace = action.faceIndex?.let { cardDef?.cardFaces?.getOrNull(it) }
        val spellSubtypes = when {
            castFace != null -> castFace.typeLine.subtypes.map { it.value }
            transformedFace != null -> transformedFace.typeLine.subtypes.map { it.value }
            cardDef != null -> cardDef.typeLine.subtypes.map { it.value }
            else -> cardComponent.typeLine.subtypes.map { it.value }
        }

        if (action.splicedCardIds.size != action.splicedCardIds.distinct().size) {
            return "Cannot splice the same card onto a spell more than once"
        }

        val hand = state.getZone(ZoneKey(action.playerId, Zone.HAND))
        for (splicedId in action.splicedCardIds) {
            if (splicedId == action.cardId) {
                return "Cannot splice a spell onto itself"
            }
            if (splicedId !in hand) {
                return "Spliced card is not in your hand"
            }
            val splicedDef = SpliceCasts.definitionOf(state, splicedId, cardRegistry)
                ?: return "Spliced card definition not found"
            val splice = SpliceCasts.printedSplice(splicedDef)
                ?: return "${splicedDef.name} does not have splice"
            if (!SpliceCasts.qualityMatches(splice, spellSubtypes)) {
                return "${splicedDef.name} can only be spliced onto a ${splice.onto} spell"
            }
            // CR 702.47b — the splice is illegal outright when its own text couldn't be given the
            // targets it demands.
            val requiredTargets = splicedDef.script.targetRequirements.sumOf { it.effectiveMinCount }
            if (requiredTargets > 0 && action.targets.size < requiredTargets) {
                return "${splicedDef.name} needs targets for its spliced text"
            }
        }
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
     * The effective `[min, max]` range of mode counts this cast may choose.
     *
     * Delegates to [ModalChooseCounts], the authority the legal-action enumerator also uses, so an
     * advertised cast and a validated one can't disagree.
     */
    private fun effectiveModalChooseCounts(
        state: GameState,
        modalEffect: ModalEffect,
        action: CastSpell
    ): Pair<Int, Int> {
        val range = ModalChooseCounts.forCast(
            state = state,
            modalEffect = modalEffect,
            cardId = action.cardId,
            controllerId = action.playerId,
            declaredCostSlot = action.declaredCostSlot,
            blightPaid = action.additionalCostPayment?.blightTargets?.isNotEmpty() == true,
            conditionEvaluator = conditionEvaluator
        )
        return range.first to range.last
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
        CounterType.PLUS_ONE_PLUS_ZERO -> Counters.PLUS_ONE_PLUS_ZERO
        CounterType.PLUS_ZERO_PLUS_ONE -> Counters.PLUS_ZERO_PLUS_ONE
        CounterType.MINUS_ONE_MINUS_ZERO -> Counters.MINUS_ONE_MINUS_ZERO
        CounterType.MINUS_ZERO_MINUS_ONE -> Counters.MINUS_ZERO_MINUS_ONE
        else -> type.name.lowercase()
    }

    /**
     * Resolve the distributed counter removals to apply for a
     * [CostAtom.RemoveCounters] additional cost.
     *
     * Web clients send the typed [AdditionalCostPayment.distributedCounterRemovals] —
     * one entry per (entity, counterType, count) — so the player explicitly picks
     * which counter types come off each creature. The CastSpell flow does not honour
     * the legacy `counterRemovals: Map<EntityId, Int>` payload; that field remains
     * The legacy map payload is intentionally ignored; counter removal uses the typed
     * per-entity, per-counter-type payload.
     */
    private fun resolveDistributedCounterRemovalsForPayment(
        action: CastSpell
    ): List<com.wingedsheep.sdk.scripting.DistributedCounterRemoval> {
        val payment = action.additionalCostPayment ?: return emptyList()
        return payment.distributedCounterRemovals
    }

    /**
     * The additional costs a modal spell owes for the modes it chose: per-mode overrides where the
     * chosen modes declare them (rule 700.2h — they stack), card-level costs otherwise, plus the
     * non-mana escalate cost when the card has one ([EscalateCosts.additionalCostFor]).
     */
    private fun resolveAdditionalCostsForMode(
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        action: CastSpell
    ): List<AdditionalCost> {
        if (action.chosenModes.isEmpty()) return cardDef.script.additionalCosts
        val modalEffect = cardDef.script.spellEffect as? ModalEffect ?: return cardDef.script.additionalCosts

        val perModeOverrides = action.chosenModes.mapNotNull { modeIndex ->
            modalEffect.modes.getOrNull(modeIndex)?.additionalCosts
        }
        val base = if (perModeOverrides.isEmpty()) cardDef.script.additionalCosts else perModeOverrides.flatten()
        val escalate = EscalateCosts.additionalCostFor(modalEffect, action.chosenModes.size)
        return if (escalate == null) base else base + escalate
    }

    /**
     * Reduce each cost that offers the caster a *choice of legs* to the leg they actually took, so
     * every downstream cost path (validation, application, free-cast selection) handles a plain
     * cost with no alternatives awareness.
     *
     * Cost-vs-cost ([AdditionalCost.Choice]) reduces to the single option being paid:
     *  1. the option whose [AdditionalCostPayment] field the client populated — a normal cast, where
     *     each option surfaced as its own legal action so exactly one field is filled; else
     *  2. the first option payable from the current board — server-initiated free/AI casts arrive with
     *     no payment (mirrors [ForageCostResolver]'s engine-direct fallback); else
     *  3. the first option (nothing payable — downstream validation/selection then rejects the cast).
     *
     * Cost-vs-mana ([AdditionalCost.OrPay]) reduces to its leg cost when the caster populated that
     * cost's payment field, and to *nothing* otherwise: declining the leg means they took the pay
     * path, whose only consequence — the extra mana — is already folded into the spell's cost by
     * [applyOrPayManaAdjustments]. Paying the leg then runs through that cost's ordinary machinery,
     * so the or-pay shape needs no validation or payment code of its own.
     *
     * [AdditionalCost.Composite] steps are flattened on the way in and out, so an alternative
     * nested inside a composite is reduced (and priced) like a top-level one.
     */
    private fun reduceCostAlternatives(
        costs: List<AdditionalCost>,
        state: GameState,
        playerId: EntityId,
        payment: AdditionalCostPayment?,
    ): List<AdditionalCost> = flattenComposites(costs).flatMap { cost ->
        when (cost) {
            is AdditionalCost.Choice -> listOf(
                cost.options.firstOrNull { paymentSatisfied(it, payment) }
                    ?: cost.options.firstOrNull { costHandler.canPayAdditionalCost(state, it, playerId) }
                    ?: cost.options.first()
            )
            is AdditionalCost.OrPay ->
                if (paymentSatisfied(cost.cost, payment)) listOf(cost.cost) else emptyList()
            else -> listOf(cost)
        }
    }.let(::flattenComposites)

    /** Expand [AdditionalCost.Composite] wrappers so every cost in the list stands on its own. */
    private fun flattenComposites(costs: List<AdditionalCost>): List<AdditionalCost> =
        costs.flatMap { if (it is AdditionalCost.Composite) flattenComposites(it.steps) else listOf(it) }

    /**
     * Fold the alternative mana of every "… or pay {N}" additional cost in [costs] whose non-mana
     * leg the caster declined into [cost] (CR 601.2f — the total cost is locked in as the spell is
     * cast). Which leg was taken is read off which [AdditionalCostPayment] field the client
     * populated, exactly as [reduceCostAlternatives] reads it — and composites are flattened the
     * same way, so the two always agree on which costs they see.
     *
     * Shared by validate() and execute() so the cast is priced identically in both.
     */
    private fun applyOrPayManaAdjustments(
        cost: ManaCost,
        costs: List<AdditionalCost>,
        payment: AdditionalCostPayment?,
    ): ManaCost = flattenComposites(costs).fold(cost) { acc, additionalCost ->
        val declinedLegPrice = when (additionalCost) {
            is AdditionalCost.OrPay ->
                additionalCost.alternativeManaCost.takeUnless { paymentSatisfied(additionalCost.cost, payment) }
            is AdditionalCost.BlightOrPay ->
                additionalCost.alternativeManaCost.takeIf { payment?.blightTargets.isNullOrEmpty() }
            else -> null
        }
        if (declinedLegPrice == null) acc else acc + ManaCost.parse(declinedLegPrice)
    }

    /**
     * True when [payment] carries a selection in the field [cost] consumes — the signal that the
     * caster paid this cost rather than an alternative offered alongside it. Costs with no
     * selection of their own (mana, life, mill, …) are never distinguishable this way, so they
     * report false; [AdditionalCost.OrPay] and [AdditionalCost.Choice] document that boundary.
     */
    private fun paymentSatisfied(cost: AdditionalCost, payment: AdditionalCostPayment?): Boolean {
        val p = payment ?: return false
        return when (cost) {
            is AdditionalCost.Behold -> p.beheldCards.isNotEmpty()
            is AdditionalCost.Atom -> when (cost.atom) {
                is CostAtom.Sacrifice -> p.sacrificedPermanents.isNotEmpty()
                is CostAtom.Discard -> p.discardedCards.isNotEmpty()
                is CostAtom.ExileFrom -> p.exiledCards.isNotEmpty()
                is CostAtom.CollectEvidence -> p.exiledCards.isNotEmpty()
                is CostAtom.TapPermanents -> p.tappedPermanents.isNotEmpty()
                is CostAtom.ReturnToHand -> p.bouncedPermanents.isNotEmpty()
                is CostAtom.VariablePermanents -> p.variableCostPermanents.isNotEmpty()
                else -> false
            }
            else -> false
        }
    }

    private fun validateAdditionalCosts(
        state: GameState,
        additionalCosts: List<AdditionalCost>,
        action: CastSpell
    ): String? {
        val projected = state.projectedState
        val flattenedCosts = reduceCostAlternatives(additionalCosts, state, action.playerId, action.additionalCostPayment)
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
                    // CR 701.59a — a *sum* gate, so the count of cards is irrelevant; the resolver
                    // owns the legality rule so cast-time validation can't drift from payment.
                    // A GameAction is client-supplied: never trust the submitted selection.
                    is CostAtom.CollectEvidence -> {
                        val exiled = action.additionalCostPayment?.exiledCards ?: emptyList()
                        if (!com.wingedsheep.engine.handlers.costs.CollectEvidenceResolver
                                .isLegalSelection(state, action.playerId, atom.amount, exiled)
                        ) {
                            return "You must exile cards with total mana value ${atom.amount} or " +
                                "greater from your graveyard to collect evidence ${atom.amount}"
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
                    is CostAtom.VariablePermanents -> {
                        val chosen = action.additionalCostPayment?.variableCostPermanents ?: emptyList()
                        val verb = VariablePermanentsCost.verb(atom.action)
                        if (chosen.size != chosen.distinct().size) {
                            return "The same permanent can't be chosen twice to $verb for this spell"
                        }
                        if (chosen.size < atom.minCount) {
                            return "You must $verb at least ${atom.minCount} ${atom.filter.description}(s) to cast this spell"
                        }
                        val context = PredicateContext(controllerId = action.playerId)
                        for (permId in chosen) {
                            val permContainer = state.getEntity(permId)
                                ?: return "Permanent to $verb not found: $permId"
                            val permCard = permContainer.get<CardComponent>()
                                ?: return "Entity to $verb is not a card: $permId"
                            if (permId !in state.getBattlefield()) {
                                return "Permanent to $verb is not on the battlefield: $permId"
                            }
                            if (projected.getController(permId) != action.playerId) {
                                return "You can only $verb permanents you control"
                            }
                            // CR 701.26a — only untapped permanents can be tapped. Summoning
                            // sickness (CR 302.6) governs the {T} symbol, not a tap paid as a
                            // cost, so a creature that entered this turn may still pay (as with
                            // crew, CR 702.122b).
                            if (atom.action == PermanentCostAction.TAP && permContainer.has<TappedComponent>()) {
                                return "${permCard.name} is already tapped"
                            }
                            if (!predicateEvaluator.matches(state, projected, permId, atom.filter, context)) {
                                return "${permCard.name} doesn't match the required filter: ${atom.filter.description}"
                            }
                        }
                        // The measure floor — Teamwork N's "with total power N or more"
                        // (CR 702.194a), summed from projected power so a lord bonus counts.
                        if (atom.minMeasure > 0) {
                            val measured = VariablePermanentsCost.measure(state, atom.xMeasure, chosen)
                            if (measured < atom.minMeasure) {
                                return "The permanents you chose have ${VariablePermanentsCost.measureName(atom.xMeasure)} " +
                                    "$measured; ${atom.minMeasure} or more is required"
                            }
                        }
                    }
                    // Mana / reveal are not produced as spell additional costs today;
                    // put-counters-on-self is ability-scoped (no permanent to accrue them on);
                    // Mill is an activated-ability-only cost, never a spell additional cost
                    // (canPayAdditionalCost already reports Mill unpayable).
                    is CostAtom.Mana, is CostAtom.RevealFromHand,
                    is CostAtom.PutCountersOnSelf,
                    is CostAtom.Mill -> {}
                    is CostAtom.RemoveCounters -> {
                        val needed = when (val c = atom.count) {
                            is com.wingedsheep.sdk.scripting.values.DynamicAmount.Fixed -> c.amount
                            else -> 0
                        }
                        val removals = resolveDistributedCounterRemovalsForPayment(action)
                        val total = removals.sumOf { it.count }
                        if (total < needed) {
                            val phrase = if (needed == 1) "1 counter from a" else "$needed counters from among"
                            val plural = if (needed == 1) "" else "s"
                            return "You must remove $phrase ${atom.filter.description}$plural you control to cast this spell"
                        }
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
                                return "You can only remove counters from permanents you control"
                            }
                            if (removal.entityId !in state.getBattlefield()) {
                                return "Counter removal target is not on the battlefield"
                            }
                            val ctx = com.wingedsheep.engine.handlers.PredicateContext(controllerId = action.playerId)
                            if (!predicateEvaluator.matches(state, projected, removal.entityId, atom.filter, ctx)) {
                                val permName = state.getEntity(removal.entityId)?.get<CardComponent>()?.name ?: "Permanent"
                                return "$permName doesn't match the required filter: ${atom.filter.description}"
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
                is AdditionalCost.OrPay -> {
                    // Unreachable: reduceCostAlternatives already replaced this with its leg cost
                    // (the caster paid the non-mana leg — validated by that cost's branch above) or
                    // dropped it (pay path — the alternative mana is validated with the mana payment).
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
                is AdditionalCost.PayLifeEqualToManaValueOfSpell -> {
                    val required = state.getEntity(action.cardId)?.get<CardComponent>()?.manaCost?.cmc ?: 0
                    val currentLife = state.lifeTotal(action.playerId) // CR 810.9a — team's shared total
                    // CR 119.4 — you can't pay life unless you have at least that much
                    if (currentLife < required) {
                        return "Not enough life to pay $required life (its mana value)"
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

        // Modal DFC back face (CR 712.11b) — resolved pre-cast, while the card is still in hand.
        // Kept as its own local because the cost branch below charges *this* face's printed mana
        // cost, which the merged `transformedFace` alone can't distinguish from the other routes.
        val modalBackFace = if (
            action.useAlternativeCost && action.altAllows(AlternativeCostType.MODAL_BACK_FACE)
        ) {
            zoneResolver.modalBackCastFace(state, action.playerId, action.cardId)
        } else null

        // The face this cast puts on the stack when it is cast **transformed**, resolved against the
        // pre-cast state while the card is still in its origin zone. Non-null means the back face
        // supplies the spell's characteristics (CR 712.8c / 712.8f). Three routes, mirroring
        // validate(): disturb (CR 702.146a) casts transformed from the graveyard for its disturb
        // cost; the modal-DFC face choice (CR 712.11b) casts the back face from hand for its own
        // mana cost; and a `castTransformed` may-play permission casts transformed from wherever the
        // permission covers (CR 310.11b — "exile it, then you may cast it transformed").
        val transformedFace = (
            if (action.useAlternativeCost && action.altAllows(AlternativeCostType.DISTURB)) {
                zoneResolver.disturbCastFace(state, action.playerId, action.cardId)
            } else null
        )
            ?: modalBackFace
            ?: zoneResolver.permissionTransformedCastFace(state, action.playerId, action.cardId)

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
        val limitedTopLibraryCastSource = if (action.cardId in currentState.getLibrary(action.playerId)) {
            zoneResolver.findLimitedTopLibraryCastSourceToConsume(currentState, action.playerId, action.cardId)
        } else null

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
            // Check flashback cost first (printed, granted per-entity by Archmage's Newt, or
            // granted to the whole graveyard by a battlefield static — Iroh, Grand Lotus).
            val flashbackAbility = FlashbackGrants.effectiveFlashback(
                currentState, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator
            )
            // Harmonize may be printed on the card or granted at runtime (Songcrafter Mage).
            val harmonizeAbility = HarmonizeGrants.effectiveHarmonize(currentState, action.cardId, cardDef)
            // Branches gated by [CastSpell.altAllows] — mirrors validate(); honors the player's
            // explicit alternative-cost choice instead of a fixed priority order.
            if (action.altAllows(AlternativeCostType.FLASHBACK) && flashbackAbility != null && zoneResolver.hasFlashbackPermission(currentState, action.playerId, action.cardId)) {
                costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, flashbackAbility.cost, action.playerId)
            } else if (action.altAllows(AlternativeCostType.HARMONIZE) && harmonizeAbility != null && zoneResolver.hasHarmonizePermission(currentState, action.playerId, action.cardId)) {
                costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, harmonizeAbility.cost, action.playerId)
            } else if (action.altAllows(AlternativeCostType.MAYHEM) &&
                MayhemGrants.effectiveMayhem(currentState, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator) != null &&
                zoneResolver.hasMayhemPermission(currentState, action.playerId, action.cardId)) {
                // Mayhem cost (CR 702.187) — cast from graveyard for its mayhem cost.
                costCalculator.calculateEffectiveCostWithAlternativeBase(
                    currentState, cardDef, MayhemGrants.effectiveMayhem(currentState, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator)!!.cost, action.playerId
                )
            } else if (action.altAllows(AlternativeCostType.DISTURB) &&
                DisturbCasts.printedDisturb(cardDef) != null &&
                zoneResolver.disturbCastFace(currentState, action.playerId, action.cardId) != null) {
                // Disturb cost (CR 702.146a) — mirrors validate().
                costCalculator.calculateEffectiveCostWithAlternativeBase(
                    currentState, cardDef, DisturbCasts.printedDisturb(cardDef)!!.cost, action.playerId
                )
            } else if (action.altAllows(AlternativeCostType.MODAL_BACK_FACE) && modalBackFace != null) {
                // Modal DFC back face (CR 712.11b) — mirrors validate().
                costCalculator.calculateEffectiveCostWithAlternativeBase(
                    currentState, cardDef, modalBackFace.manaCost, action.playerId
                )
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
                    // Check sneak cost (CR 702.190 — mana portion; the bounce is paid separately).
                    // Printed Sneak, or a granted graveyard sneak (Ninja Teen).
                    val sneakCost = SneakWindow.effectiveSneakCost(currentState, cardDef, action.cardId, action.playerId, cardRegistry)
                    // Check web-slinging cost (CR 702.188 — mana portion; the return-a-tapped-creature
                    // bounce is paid separately, alongside).
                    val webSlingingAbility = WebSlinging.effectiveWebSlinging(currentState, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator)
                    // Check evoke cost
                    val evokeAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Evoke>().firstOrNull()
                    // Check dash cost (CR 702.109 — hand only, printed only for now).
                    val dashAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Dash>().firstOrNull()
                    // Check emerge cost (CR 702.119 — mana portion; the sacrifice is paid below,
                    // after the mana payment, per CR 601.2f–h).
                    val emergeAbility = EmergeCasts.printedEmerge(cardDef)
                    if (action.altAllows(AlternativeCostType.SNEAK) && sneakCost != null) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, sneakCost, action.playerId)
                    } else if (action.altAllows(AlternativeCostType.WEB_SLINGING) && webSlingingAbility != null) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, webSlingingAbility.cost, action.playerId)
                    } else if (action.altAllows(AlternativeCostType.EVOKE) && evokeAbility != null) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, evokeAbility.cost, action.playerId)
                    } else if (action.altAllows(AlternativeCostType.EMERGE) && emergeAbility != null) {
                        // CR 702.119a — mirrors validate(): emerge cost reduced by an amount of
                        // generic mana equal to the sacrificed creature's mana value.
                        EmergeCasts.reduceForSacrifice(
                            costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, emergeAbility.cost, action.playerId),
                            currentState,
                            action.additionalCostPayment?.sacrificedPermanents?.firstOrNull()
                        )
                    } else if (action.altAllows(AlternativeCostType.DASH) && dashAbility != null && zoneResolver.hasDashPermission(currentState, action.playerId, action.cardId)) {
                        costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, dashAbility.cost, action.playerId)
                    } else {
                        // Check impending cost
                        val impendingAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Impending>().firstOrNull()
                        // Check cleave cost (CR 702.148 — an alternative cost).
                        val cleaveAbility = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Cleave>().firstOrNull()
                        // Check miracle cost (CR 702.94 — printed or granted in hand, window-gated).
                        val miracleWindowOpen = currentState.getEntity(action.cardId)
                            ?.has<com.wingedsheep.engine.state.components.identity.MiracleWindowComponent>() == true
                        val miracleAbility = if (miracleWindowOpen) MiracleGrants.effectiveMiracle(
                            currentState, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator
                        ) else null
                        if (action.altAllows(AlternativeCostType.IMPENDING) && impendingAbility != null) {
                            costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, impendingAbility.cost, action.playerId)
                        } else if (action.altAllows(AlternativeCostType.CLEAVE) && cleaveAbility != null) {
                            costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, cleaveAbility.cost, action.playerId)
                        } else if (action.altAllows(AlternativeCostType.MIRACLE) && miracleAbility != null) {
                            costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, miracleAbility.cost, action.playerId)
                        } else {
                            val selfAltCost = cardDef.script.selfAlternativeCost
                            if (action.altAllows(AlternativeCostType.SELF_ALTERNATIVE) && selfAltCost != null) {
                                val altMana = selfAltCost.manaCost
                                costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, altMana, action.playerId)
                            } else if (action.altAllows(AlternativeCostType.GRANTED)) {
                                val altCosts = costCalculator.findAlternativeCastingCosts(currentState, action.playerId)
                                if (altCosts.isNotEmpty()) {
                                    costCalculator.calculateEffectiveCostWithAlternativeBase(currentState, cardDef, altCosts.first())
                                } else {
                                    cardComponent.manaCost
                                }
                            } else {
                                // A specific alternative cost was requested (e.g. DASH) but its own
                                // permission gate failed — never silently fall back to an unrelated
                                // battlefield-granted alternative cost the player didn't ask for.
                                // validate() already rejected this cast via computeTotalCastCost
                                // returning null, so execute() should never actually reach here.
                                cardComponent.manaCost
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
                declaredCostSlot = action.declaredCostSlot,
            )
        } else {
            cardComponent.manaCost
        }

        // Add kicker/offspring cost if kicked (not applicable with alternative costs)
        if (!playForFreeInExecute && !action.useAlternativeCost) {
            val kickerManaCost = declaredOptionalCosts(action, cardDef)
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
                val perExtraMode = modalEffect.additionalManaCostPerExtraMode
                if (perExtraMode != null) {
                    repeat((action.chosenModes.size - 1).coerceAtLeast(0)) {
                        effectiveCost = effectiveCost + ManaCost.parse(perExtraMode)
                    }
                }
            }
        }

        // Fold in the "… or pay {N}" alternative mana for every or-pay cost whose non-mana leg the
        // caster declined — the same rule validate() priced the cast with.
        if (cardDef != null && !playForFreeInExecute) {
            effectiveCost = applyOrPayManaAdjustments(
                effectiveCost, cardDef.script.additionalCosts, action.additionalCostPayment
            )
        }

        // Apply spell-level waterbend additional cost (Avatar: The Last Airbender) — add the
        // waterbend {N}/{X} as generic mana; tapped artifacts/creatures reduce it below.
        if (cardDef != null && !playForFreeInExecute) {
            val waterbendAmount = spellWaterbendAmount(cardDef, action)
            if (waterbendAmount > 0) {
                effectiveCost = effectiveCost + ManaCost.parse("{$waterbendAmount}")
            }
        }

        // Airbend: a fixed alternative cost ({2}) replaces the printed cost; a cost increase (Soul
        // Partition / Thalia-style tax) still applies on top of it. Mirrors the validation-phase
        // branch above.
        if (!playForFreeInExecute) {
            val fixedAltCost = currentState.getEntity(action.cardId)
                ?.get<PlayWithFixedAlternativeManaCostComponent>()
                ?.takeIf { it.controllerId == action.playerId }
            if (fixedAltCost != null) {
                effectiveCost = fixedAltCost.fixedCost
            }
            // Apply runtime mana tax from exile permissions (e.g., Soul Partition) on top.
            val runtimeCostIncrease = currentState.getEntity(action.cardId)
                ?.get<PlayWithCostIncreaseComponent>()
                ?.takeIf { it.controllerId == action.playerId }
            if (runtimeCostIncrease != null) {
                effectiveCost = effectiveCost + ManaCost.parse("{${runtimeCostIncrease.amount}}")
            }
        }

        // Process additional costs (sacrifice, exile, etc.)
        val sacrificedSnapshots = mutableListOf<EntitySnapshot>()
        var exiledCardCount = 0
        val beheldCards = mutableListOf<EntityId>()
        // Cards discarded to pay an additional discard cost — threaded to the spell on the stack so
        // a resolution-time condition can test the discarded card (EffectTarget.DiscardedAsCost).
        val discardedAsCostCards = mutableListOf<EntityId>()
        /**
         * LKI snapshots for entities chosen via [AdditionalCost.ChooseEntity] when
         * `captureSnapshot = true`. Captured at cost-pay time so downstream effects
         * (e.g. `EntityProperty(FromCostStorage(...), Power)`) can read "power as it
         * last existed on the battlefield" if the chosen entity leaves before resolution.
         */
        val chosenEntitySnapshots = mutableListOf<EntitySnapshot>()
        /** Pipeline storage populated by Behold, consumed by ExileFromStorage */
        val costPipelineCollections = mutableMapOf<String, List<EntityId>>()

        // Collect all additional costs: script costs + kicker additional cost (if kicked)
        // + self-alternative cost's additional costs (if using alternative cost)
        // + runtime additional costs from PlayWithAdditionalCostComponent
        // Per-mode additional costs override card-level costs when present
        // The non-mana half of the optional cost the caster *declared* (kicker, bargain, teamwork —
        // `action.declaredCostSlot`), kept aside so the payment loop below can tell it apart from
        // the card's printed additional costs. Only this one carries the declared mechanic's
        // identity, which is what names a tap's cause ([TapReason.forChoiceSlot]). Reduced into
        // `declaredSlotCosts` below before the comparison is made.
        val declaredSlotAdditionalCost: AdditionalCost? = declaredOptionalCosts(action, cardDef)
            .firstOrNull { it.additionalCost != null }
            ?.additionalCost

        val allAdditionalCosts = buildList {
            if (cardDef != null) addAll(resolveAdditionalCostsForMode(cardDef, action))
            declaredSlotAdditionalCost?.let { add(it) }
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

            // Self-referential MayCastSelfFromZones grant's additional cost (e.g. Alien
            // Symbiosis' "by discarding a card")
            zoneResolver.findMayCastSelfFromZoneAbility(currentState, action.playerId, action.cardId)
                ?.additionalCost?.let { add(it) }

            // Gwenom: pay-life additional cost for a spell cast from the top of the library.
            zoneResolver.topOfLibraryAlternativeGrant(currentState, action.playerId, action.cardId)
                ?.additionalCost?.let { add(it) }
        }

        val flattenedAllCosts = reduceCostAlternatives(allAdditionalCosts, currentState, action.playerId, action.additionalCostPayment)

        // The declared slot's cost, put through the *same* reduction as the full list, so the
        // payment loop can recognise it by equality. Reducing both sides is what makes the match
        // survive an `AdditionalCost.Composite` or `Choice` wrapper: `reduceCostAlternatives`
        // flattens composites and picks a Choice's leg, so comparing an unreduced wrapper against
        // the reduced list would never match and would silently drop the tap cause.
        val declaredSlotCosts: List<AdditionalCost> = reduceCostAlternatives(
            listOfNotNull(declaredSlotAdditionalCost), currentState, action.playerId, action.additionalCostPayment
        )

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
                additionalCost is AdditionalCost.PayLifeEqualToManaValueOfSpell ->
                    currentState.getEntity(action.cardId)?.get<CardComponent>()?.manaCost?.cmc ?: 0
                else -> continue
            }
            if (lifeToPay == 0) continue
            val (afterPayment, paymentEvents) =
                LifePaymentService.pay(currentState, action.playerId, lifeToPay) ?: continue
            currentState = afterPayment
            events.addAll(paymentEvents)
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
                                captureEntitySnapshots(action.additionalCostPayment.sacrificedPermanents, projectedBeforeSacrifice)
                            )
                            for (permId in action.additionalCostPayment.sacrificedPermanents) {
                                if (currentState.getEntity(permId) == null) continue
                                currentState = sacrificePermanentAsCost(currentState, permId, action.playerId, events)
                            }
                        }
                        is CostAtom.Discard -> {
                            val discardedCards = action.additionalCostPayment.discardedCards
                            discardedAsCostCards.addAll(discardedCards)
                            // Through the shared discard path so a card-intrinsic discard
                            // replacement (madness, CR 702.35a) applies to a card discarded as an
                            // additional cost of casting a spell.
                            val discardResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                                .discardCards(currentState, action.playerId, discardedCards)
                            currentState = discardResult.state
                            events.addAll(discardResult.events)
                        }
                        is CostAtom.CollectEvidence -> {
                            val collected = com.wingedsheep.engine.handlers.costs
                                .CollectEvidenceResolver.collect(
                                    state = currentState,
                                    playerId = action.playerId,
                                    amount = atom.amount,
                                    chosenCards = action.additionalCostPayment.exiledCards,
                                    sourceName = cardDef?.name ?: "Collect evidence",
                                )
                            if (collected is com.wingedsheep.engine.handlers.costs
                                    .CollectEvidenceResolver.Result.Success
                            ) {
                                currentState = collected.state
                                events.addAll(collected.events)
                            }
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
                                val (tappedState, tapEvent) = tap(currentState, permId)
                                currentState = tappedState
                                tapEvent?.let(events::add)
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
                        is CostAtom.VariablePermanents -> {
                            // A variable-count permanent cost paid as a spell's additional cost —
                            // Teamwork N taps the chosen creatures (CR 702.194a). Validation above
                            // already re-checked control, filter, and the measure floor.
                            //
                            // The tap carries its *cause* ([TapReason]), which is what lets
                            // "whenever this becomes tapped to pay a teamwork cost" (Agent Maria
                            // Hill) tell a teamwork tap apart from an attack, crew, or mana tap —
                            // all of which are also performed by the creature's own controller, so
                            // `tappedById` can't separate them. The cause comes from the *declared
                            // cast-choice slot*, not from the atom: `VariablePermanents(TAP)` is a
                            // generic atom any mechanic may reuse, and it is teamwork's declaration
                            // (CR 601.2b / 702.194a) that makes this a teamwork tap. Stamped only on
                            // the cost the declared optional ability actually contributed, so a
                            // card's own printed tap cost isn't relabelled by an unrelated
                            // declaration. Tapping itself goes through
                            // [VariablePermanentsCost.tapAll] — the single tap site for this atom,
                            // shared with the activated-ability payer in `CostHandler`.
                            val chosen = action.additionalCostPayment.variableCostPermanents
                            when (atom.action) {
                                PermanentCostAction.TAP -> {
                                    val reason = if (additionalCost in declaredSlotCosts) {
                                        TapReason.forChoiceSlot(action.declaredCostSlot)
                                    } else {
                                        TapReason.UNSPECIFIED
                                    }
                                    val (tappedState, tapEvents) =
                                        VariablePermanentsCost.tapAll(currentState, chosen, reason)
                                    currentState = tappedState
                                    events.addAll(tapEvents)
                                }
                                PermanentCostAction.SACRIFICE -> {
                                    sacrificedSnapshots.addAll(
                                        captureEntitySnapshots(chosen, currentState.projectedState)
                                    )
                                    for (permId in chosen) {
                                        if (currentState.getEntity(permId) == null) continue
                                        currentState = sacrificePermanentAsCost(currentState, permId, action.playerId, events)
                                    }
                                }
                                PermanentCostAction.EXILE -> for (permId in chosen) {
                                    val tr = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                                        .moveToZone(currentState, permId, Zone.EXILE)
                                    currentState = tr.state
                                    events.addAll(tr.events)
                                }
                            }
                        }
                        // PayLife is auto-paid in the loop above; mana / reveal aren't spell additional
                        // costs; put-counters-on-self is ability-scoped (a spell on the stack has no
                        // permanent to accrue them on); Mill is an activated-ability-only cost, never a
                        // spell additional cost (and canPayAdditionalCost reports Mill unpayable, so
                        // this is unreachable).
                        is CostAtom.PayLife, is CostAtom.Mana, is CostAtom.RevealFromHand,
                        is CostAtom.PutCountersOnSelf,
                        is CostAtom.Mill -> {}
                        is CostAtom.RemoveCounters -> {
                            val resolvedRemovals = resolveDistributedCounterRemovalsForPayment(action)
                            for (removal in resolvedRemovals) {
                                val container = currentState.getEntity(removal.entityId) ?: continue
                                val existing = container.get<CountersComponent>() ?: continue
                                val resolvedType =
                                    com.wingedsheep.engine.handlers.effects.permanent.counters.resolveCounterType(removal.counterType)
                                currentState = currentState.updateEntity(removal.entityId) { c ->
                                    c.with(existing.withRemoved(resolvedType, removal.count))
                                }
                                val entityName = container.get<CardComponent>()?.name ?: "Permanent"
                                events.add(CountersRemovedEvent(
                                    entityId = removal.entityId,
                                    counterType = removal.counterType,
                                    amount = removal.count,
                                    entityName = entityName
                                ))
                            }
                        }
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
                            captureEntitySnapshots(action.additionalCostPayment.sacrificedPermanents, projectedBeforeSacrifice)
                        )
                        for (permId in action.additionalCostPayment.sacrificedPermanents) {
                            if (currentState.getEntity(permId) == null) continue
                            currentState = sacrificePermanentAsCost(currentState, permId, action.playerId, events)
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
                                    firstThisTurn = firstThisTurn,
                                    placedBy = action.playerId
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
                                    firstThisTurn = firstThisTurn,
                                    placedBy = action.playerId
                                ))
                            }
                        }
                    }
                    is AdditionalCost.OrPay -> {
                        // Unreachable: reduceCostAlternatives already replaced this with its leg
                        // cost (paid by that cost's branch above, including LKI snapshots, discard
                        // tracking and behold's pipeline storage) or dropped it (pay path — the
                        // alternative mana was folded into effectiveCost).
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
                                        captureEntitySnapshots(battlefieldChosen, currentState.projectedState)
                                    )
                                }
                            }
                        }
                    }
                    is AdditionalCost.Forage -> {
                        // Forage as an additional cost to cast (e.g. Feed the Cycle's forage mode).
                        // Honors the player's mode + card/Food choice via additionalCostPayment,
                        // falling back to a legal auto-payment otherwise. The spell is cast from
                        // hand here, so it's not in the exile pool — no exclusion needed.
                        when (val forageResult = com.wingedsheep.engine.handlers.costs.ForageCostResolver.pay(
                            currentState, action.playerId,
                            exileChoices = action.additionalCostPayment.exiledCards,
                            sacrificeChoices = action.additionalCostPayment.sacrificedPermanents,
                        )) {
                            is com.wingedsheep.engine.handlers.costs.ForageCostResolver.Result.Success -> {
                                currentState = forageResult.state
                                events.addAll(forageResult.events)
                            }
                            is com.wingedsheep.engine.handlers.costs.ForageCostResolver.Result.Failure ->
                                return ExecutionResult.error(currentState, forageResult.reason)
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
                val (tappedState, tapEvent) = tap(currentState, creatureId)
                currentState = tappedState
                tapEvent?.let(events::add)
            }
        }

        // Pay Casualty's optional additional cost: sacrifice the chosen creature (CR 702.153).
        // Validated in validate(); routes through the shared cost-sacrifice helper so the LKI
        // snapshot (CR 608.2h / 113.7a) and the leave-the-battlefield events are emitted for
        // dies/leaves triggers and the "cards leave your graveyard" family. The pre-sacrifice
        // EntitySnapshot is also captured into sacrificedSnapshots for the spell's own effect
        // context (copy-token P/T, etc.).
        action.casualtyCreature?.let { permId ->
            val projectedBeforeSacrifice = currentState.projectedState
            sacrificedSnapshots.addAll(captureEntitySnapshots(listOf(permId), projectedBeforeSacrifice))
            if (currentState.getEntity(permId) != null) {
                currentState = sacrificePermanentAsCost(currentState, permId, action.playerId, events)
            }
        }

        // X mana to pay (≤ action.xValue). For an X-cost Harmonize cast with a tapped
        // creature, the leftover power beyond the printed generic reduces the X mana paid;
        // computed from the pre-reduction cost so the printed-generic split matches what
        // AlternativePaymentHandler.reduceGeneric does below. action.xValue (the effect's X)
        // is untouched.
        // For a "waterbend {X}" spell the X is materialized as generic in the cost and paid by the
        // waterbend taps, so it isn't charged again as {X} mana here (action.xValue still feeds the
        // resolving effect via the stack).
        val paymentXValue = if (cardDef?.script?.spellWaterbend?.isX == true) 0
            else harmonizePaymentXValue(currentState, action, cardDef, effectiveCost)

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

        // Apply waterbend (Avatar): tap the chosen artifacts/creatures, each paying {1} of the
        // waterbend generic, bounded by the waterbend amount. Sums the spell-level `waterbend {N}`
        // additional cost and Hama's fixed-alternative waterbend cost (only one is ever non-zero).
        // > 0 exactly when a waterbend cost is actually being paid on this cast (an optional
        // "you may waterbend" that was declined yields 0).
        val waterbendPaidAmount = (if (cardDef != null) spellWaterbendAmount(cardDef, action) else 0) +
            fixedAltWaterbendAmount(currentState, action, playForFreeInExecute)
        if (waterbendPaidAmount > 0 &&
            action.alternativePayment != null &&
            action.alternativePayment.tapForGenericPermanents.isNotEmpty()
        ) {
            val waterbendResult = alternativePaymentHandler.applyWaterbendForSpell(
                currentState, effectiveCost, action.alternativePayment, action.playerId, waterbendPaidAmount
            )
            effectiveCost = waterbendResult.reducedCost
            currentState = waterbendResult.newState
            events.addAll(waterbendResult.events)
        }
        // Apply improvise (CR 702.126a): tap the chosen artifacts, each paying {1} of the generic in
        // the spell's total cost. Unlike waterbend there is no separate amount to cap at — improvise
        // is not a cost of its own (CR 702.126b) — so it runs only when no waterbend cost claimed the
        // taps, and the handler re-checks the keyword before tapping anything.
        if (waterbendPaidAmount == 0 && !playForFreeInExecute && cardDef != null &&
            action.alternativePayment != null &&
            action.alternativePayment.tapForGenericPermanents.isNotEmpty()
        ) {
            val improviseResult = alternativePaymentHandler.applyImproviseForSpell(
                currentState, effectiveCost, action.alternativePayment, action.playerId, cardDef
            )
            effectiveCost = improviseResult.reducedCost
            currentState = improviseResult.newState
            events.addAll(improviseResult.events)
        }

        // CR 701.67c: paying a spell's waterbend cost (however paid — taps above and/or plain mana)
        // fires "whenever you waterbend". A later payment failure rolls the cast (and this event)
        // back, so emitting here is safe.
        if (waterbendPaidAmount > 0) {
            val (bendState, bendEvent) = BendEvents.record(currentState, action.playerId, BendType.WATER)
            currentState = bendState
            events.add(bendEvent)
        }

        // Build spell context for conditional mana restrictions. A face-down cast (CR 708.2) is a
        // nameless 2/2 creature spell regardless of what the card says, so it gets its own context
        // rather than the printed card's — see `validatePayment`.
        val spellContext = if (action.castFaceDown) {
            SpellPaymentContext.faceDownCast(isFromHand = isCastFromHand(currentState, action.cardId))
        } else SpellPaymentContext(
            isInstantOrSorcery = cardComponent.typeLine.isInstant || cardComponent.typeLine.isSorcery,
            isKicked = action.declaredCostSlot == ChoiceSlot.KICKED,
            isCreature = cardComponent.typeLine.isCreature,
            isLegendary = cardComponent.typeLine.isLegendary,
            manaValue = cardComponent.manaCost.cmc,
            hasXInCost = cardComponent.manaCost.hasX,
            subtypes = paymentSubtypesOf(cardComponent),
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

        // Emerge (CR 702.119a/c): the chosen creature is sacrificed *as the total cost is paid*
        // (CR 601.2h), which is why this sits after the mana payment rather than in the additional-
        // cost block above — mana abilities are activated first (CR 601.2f–g), so the creature can
        // legally be tapped for mana toward its own emerge cost before it dies. Its mana value was
        // already taken off the generic portion of `effectiveCost` while it was on the battlefield.
        // The snapshot feeds "as it last existed on the battlefield" reads (CR 608.2h) exactly like
        // a scripted sacrifice cost does.
        if (action.useAlternativeCost && action.altAllows(AlternativeCostType.EMERGE) &&
            cardDef != null && EmergeCasts.printedEmerge(cardDef) != null
        ) {
            val emergeSacrifice = action.additionalCostPayment?.sacrificedPermanents?.firstOrNull()
            if (emergeSacrifice != null && currentState.getEntity(emergeSacrifice) != null) {
                // The [GameState] overload: besides last-known P/T it freezes the creature's *name*,
                // which is what lets the stack card and the game log say which body paid for this
                // cast once it is gone (a sacrificed token leaves no entity to read a name off).
                sacrificedSnapshots.addAll(
                    captureEntitySnapshots(listOf(emergeSacrifice), currentState)
                )
                currentState = sacrificePermanentAsCost(currentState, emergeSacrifice, action.playerId, events)
            }
        }

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
        // MayCastCreaturesFromGraveyardWithForageComponent (e.g., Osteomancer Adept). The spell
        // being cast is excluded from the exile pool — it has left the graveyard for the stack and
        // can't be one of the three cards it exiles to pay for itself. The player's mode + card/Food
        // choice (when supplied via additionalCostPayment) is honored; otherwise a legal mode is
        // auto-paid. See [com.wingedsheep.engine.handlers.costs.ForageCostResolver].
        val isForageCast = zoneResolver.hasMayCastCreaturesFromGraveyardWithForage(
            currentState, action.playerId, action.cardId, cardComponent
        ) && action.cardId in currentState.getZone(ZoneKey(action.playerId, Zone.GRAVEYARD))
        if (isForageCast) {
            when (val forageResult = com.wingedsheep.engine.handlers.costs.ForageCostResolver.pay(
                currentState, action.playerId,
                exileChoices = action.additionalCostPayment?.exiledCards ?: emptyList(),
                sacrificeChoices = action.additionalCostPayment?.sacrificedPermanents ?: emptyList(),
                excludeCardId = action.cardId,
            )) {
                is com.wingedsheep.engine.handlers.costs.ForageCostResolver.Result.Success -> {
                    currentState = forageResult.state
                    events.addAll(forageResult.events)
                }
                is com.wingedsheep.engine.handlers.costs.ForageCostResolver.Result.Failure ->
                    return ExecutionResult.error(currentState, forageResult.reason)
            }
        }

        // Pay additional life cost (e.g., Festival of Embers graveyard casting)
        if (action.graveyardLifeCost > 0) {
            val currentLife = currentState.lifeTotal(action.playerId) // CR 810.9a — team's shared total
            val newLife = currentLife - action.graveyardLifeCost
            currentState = currentState.withLifeTotal(action.playerId, newLife)
            events.add(LifeChangedEvent(action.playerId, currentLife, newLife, LifeChangeReason.LIFE_LOSS))
            currentState = com.wingedsheep.engine.handlers.effects.DamageUtils.markLifeLostThisTurn(
                currentState, action.playerId, action.graveyardLifeCost
            )
        }

        // Pay any additional life cost from opponent permanents' ModifySpellCost abilities
        // (e.g. Terror of the Peaks: "Spells your opponents cast that target this creature
        // cost an additional 3 life to cast.").
        if (action.targets.isNotEmpty()) {
            val additionalLifeCost = costCalculator.calculateAdditionalLifeCost(
                currentState, action.playerId, action.targets
            )
            if (additionalLifeCost > 0) {
                LifePaymentService.pay(currentState, action.playerId, additionalLifeCost)
                    ?.let { (afterPayment, paymentEvents) ->
                        currentState = afterPayment
                        events.addAll(paymentEvents)
                    }
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
            // Adventure / split face cast (CR 715 / 709) — read targets from the face's script;
            // a disturb cast reads the back face's (CR 712.8c). Mirrors validate().
            val faceScriptForTargets = action.faceIndex?.let { cardDef.cardFaces.getOrNull(it)?.script }
                ?: transformedFace?.script
            val baseTargetReqs = if (action.chosenModes.isNotEmpty() && modalEffectForTargets != null) {
                // Modal spell with modes chosen at cast time — union per-mode requirements
                action.chosenModes.flatMap { idx ->
                    modalEffectForTargets.modes.getOrNull(idx)?.targetRequirements ?: emptyList()
                }
            } else if (action.declaredCostSlot != null && cardDef.script.kickerTargetRequirements.isNotEmpty()) {
                cardDef.script.kickerTargetRequirements
            } else if (isCleaveCast(action, cardDef) && cardDef.script.cleaveTargetRequirements.isNotEmpty()) {
                cardDef.script.cleaveTargetRequirements
            } else {
                (faceScriptForTargets ?: cardDef.script).targetRequirements
            }
            buildList {
                addAll(baseTargetReqs)
                (transformedFace ?: cardDef).script.auraTarget?.let { add(it) }
                // Splice (CR 702.47d): the spliced text's own requirements, appended in splice order.
                // They must be here and not only in validate(): this list becomes the spell's
                // TargetsComponent, which drives resolution-time 608.2b re-validation and the tail that
                // StackResolver slices off to hand each spliced card its own targets.
                addAll(SpliceCasts.targetRequirementsFor(state, action.splicedCardIds, cardRegistry))
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
        // Printed Sneak, or a granted graveyard sneak (Ninja Teen). The card may already be on the
        // stack here, so detect the grant via the player's battlefield (zone-independent) rather
        // than the card's current zone.
        val wasSneaked = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.SNEAK) &&
            (cardDef.keywordAbilities.any { it.ninjutsuStyleCost != null } ||
                SneakWindow.graveyardSneakGrantCost(currentState, action.playerId, cardRegistry) != null)
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

        // Web-slinging (CR 702.188a): pay the alternative cost's non-mana portion by returning one
        // tapped creature you control to its owner's hand. Capture that creature's mana value first
        // (CR 118.9c — its own mana value, needed by Scarlet Spider, Ben Reilly) before it leaves.
        val wasWebSlung = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.WEB_SLINGING) &&
            WebSlinging.effectiveWebSlinging(currentState, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator) != null
        var webSlungReturnedManaValue = 0
        if (wasWebSlung) {
            val bounceId = action.additionalCostPayment?.bouncedPermanents?.firstOrNull()
            if (bounceId != null) {
                webSlungReturnedManaValue = currentState.getEntity(bounceId)
                    ?.get<CardComponent>()
                    ?.manaValue ?: 0
                val bounceResult = com.wingedsheep.engine.handlers.effects.ZoneTransitionService.moveToZone(
                    currentState, bounceId, Zone.HAND
                )
                currentState = bounceResult.state
                events.addAll(bounceResult.events)
            }
        }

        // Determine if this spell is being cast using mayhem (CR 702.187). Gated by the chosen
        // alternative-cost type + the card actually having mayhem + the "you discarded this card
        // this turn" record (zone-independent, so it holds even after the card moves to the stack).
        // Drives Sandman's Quicksand's "if this spell's mayhem cost was paid" rider.
        val wasMayhem = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.MAYHEM) &&
            MayhemGrants.effectiveMayhem(currentState, action.cardId, cardDef, action.playerId, cardRegistry, predicateEvaluator) != null &&
            currentState.getEntity(action.playerId)
                ?.get<com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent>()
                ?.cardIds?.contains(action.cardId) == true
        if (wasMayhem) {
            // The card is leaving the graveyard to become a spell (CR 400.7 — a new object). Drop
            // its "discarded this turn" gate mark now (casting bypasses ZoneTransitionService.moveToZone,
            // so §8c won't fire) so it can't be Mayhem-cast again each time it resolves back.
            currentState = com.wingedsheep.engine.handlers.effects.ZoneTransitionService
                .untrackDiscardedCard(currentState, action.cardId)
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

        // Determine if this spell is being cast using dash (CR 702.109). Printed-only for now —
        // no granted-dash resolver exists yet, mirroring evoke/impending/cleave's shape below
        // rather than warp's Grants-lookup (which exists because Warp can also be granted to
        // cards in hand by a battlefield static ability).
        val wasDashed = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.DASH) &&
            cardDef.keywordAbilities.any { it is KeywordAbility.Dash }

        // Determine if this spell is being cast using evoke
        val wasEvoked = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.EVOKE) &&
            cardDef.keywordAbilities.any { it is KeywordAbility.Evoke }

        // Determine if this spell is being cast using impending
        val wasImpending = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.IMPENDING) &&
            cardDef.keywordAbilities.any { it is KeywordAbility.Impending }

        // Determine if this spell is being cast using cleave (CR 702.148). When true, the spell
        // resolves with its brackets-removed effect/target variant (cleaveSpellEffect /
        // cleaveTargetRequirements) instead of its printed one.
        val wasCleaved = action.useAlternativeCost && cardDef != null &&
            action.altAllows(AlternativeCostType.CLEAVE) &&
            cardDef.keywordAbilities.any { it is KeywordAbility.Cleave }

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
                // A transformed cast is on the stack back face up, so "a Spirit spell was cast" and
                // colour/type history read the back face (CR 712.8c / 712.8f). Mana value splits by
                // route: a disturb cast keeps the front face's (CR 712.8c), which is what
                // `cardComponent` still holds here, while CR 712.8f gives a modal double-faced spell
                // "only the characteristics of the face that's up" with no such exception — so a
                // back-face cast reports that face's own mana value. Mirrors `StackResolver`'s
                // `spellManaValue`, which stamps the same number onto the SpellCastEvent.
                typeLine = transformedFace?.typeLine ?: cardComponent.typeLine,
                manaValue = modalBackFace?.manaCost?.cmc ?: cardComponent.manaValue,
                colors = transformedFace?.colors ?: cardComponent.colors,
                isFaceDown = action.castFaceDown,
                spentManaSubtypes = paymentResult.spentManaProvenance.spentSubtypes,
                // The cast card moves to the stack keeping its entity id, so this matches the
                // resolving spell's EffectContext.sourceId (used by SpellsCastThisTurn excludeSelf).
                sourceEntityId = action.cardId,
                // Origin zone of the cast (HAND for a normal cast; GRAVEYARD/EXILE/COMMAND for
                // flashback/forage, plot/foretell, commander, …). The card is still in its origin
                // zone here — stackResolver.castSpell (below) moves it — so this resolves the same
                // way castSpell stamps SpellOnStackComponent.castFromZone. Powers "you haven't cast
                // a spell from your hand this turn" (Prairie Dog cycle).
                castFromZone = stackResolver.findCastFromZone(currentState, action.cardId, action.playerId),
                // Face-down casts hide the card's identity; a face-up cast records the name so
                // name predicates ("the first Otter spell other than Alania") can match history.
                name = if (action.castFaceDown) null else (transformedFace?.name ?: cardComponent.name),
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
        // The Tomb of Aclazotz: capture the authorizing MayCastFromGraveyard grant now, while the
        // card is still in the graveyard (the input `state`), so its cast-this-way entry rider
        // (finality counter + Vampire) can be frozen onto the stack spell after it's cast (below).
        // Null unless casting from a graveyard under a rider-bearing grant.
        val graveyardCastRiderGrant =
            zoneResolver.findMayCastFromGraveyardGrant(
                state, action.playerId, action.cardId, cardComponent, action.graveyardCastRider
            )

        // Splice (CR 702.47a): reveal each spliced card from hand. The reveal is public — it is how
        // opponents learn what text the spell gained — but the caster picked the cards, so it doesn't
        // get an overlay of its own choice. The cards are *not* moved: they stay in hand, castable
        // later or splice-able onto a later spell, and can even be discarded to pay a discard cost of
        // the very spell they were spliced onto.
        val splicedCardNames = action.splicedCardIds.mapNotNull { splicedId ->
            currentState.getEntity(splicedId)?.get<CardComponent>()?.name
        }
        if (splicedCardNames.isNotEmpty()) {
            events.add(
                CardsRevealedEvent(
                    revealingPlayerId = action.playerId,
                    cardIds = action.splicedCardIds,
                    cardNames = splicedCardNames,
                    source = "Splice",
                    revealToSelf = false,
                    fromZone = Zone.HAND,
                    toZone = Zone.HAND
                )
            )
        }

        val castResult = stackResolver.castSpell(
            currentState,
            action.cardId,
            action.playerId,
            action.targets,
            action.xValue,
            sacrificedSnapshots,
            castFaceDown = action.castFaceDown,
            castTransformed = transformedFace != null,
            damageDistribution = action.damageDistribution,
            targetRequirements = spellTargetRequirements,
            exiledCardCount = exiledCardCount,
            additionalCostBlightAmount = action.additionalCostPayment?.blightAmount ?: 0,
            additionalCostPayXLifeAmount = payXLifeAmount,
            declaredCostSlot = action.declaredCostSlot,
            wasBlightPaid = (action.additionalCostPayment?.blightTargets?.isNotEmpty() == true),
            // True when the spell's waterbend additional cost was paid (Avatar) — mandatory costs
            // always, optional "you may waterbend {N}" only when the player elected it.
            wasWaterbendPaid = cardDef?.script?.spellWaterbend?.let { !it.optional || action.wasWaterbendPaid } == true,
            // Gift (CR 702.174a): the promised opponent, elected as part of casting. Only honored
            // for a card that actually has gift — validate() rejects the flag otherwise.
            giftRecipient = action.giftRecipient?.takeIf { cardDef?.giftKeyword() != null },
            wasWarped = wasWarped,
            wasDashed = wasDashed,
            wasEvoked = wasEvoked,
            wasImpending = wasImpending,
            wasCleaved = wasCleaved,
            wasSneaked = wasSneaked,
            sneakAttackDefenderId = sneakAttackDefenderId,
            wasWebSlung = wasWebSlung,
            webSlungReturnedManaValue = webSlungReturnedManaValue,
            wasMayhem = wasMayhem,
            chosenModes = action.chosenModes,
            modeTargetsOrdered = effectiveModeTargetsOrdered,
            modeTargetRequirements = perModeTargetRequirements,
            modeDamageDistribution = action.modeDamageDistribution,
            // Splice (CR 702.47a): the *text* the spell gained, recorded by card name. The cards
            // themselves stay in hand — nothing about splicing moves them.
            splicedCardNames = splicedCardNames,
            totalManaSpent = manaSpentThisCast,
            beheldCards = beheldCards,
            discardedAsCostCards = discardedAsCostCards,
            chosenEntitySnapshots = chosenEntitySnapshots,
            manaSpentWhite = manaSpentEvent?.white ?: 0,
            manaSpentBlue = manaSpentEvent?.blue ?: 0,
            manaSpentBlack = manaSpentEvent?.black ?: 0,
            manaSpentRed = manaSpentEvent?.red ?: 0,
            manaSpentGreen = manaSpentEvent?.green ?: 0,
            manaSpentColorless = manaSpentEvent?.colorless ?: 0,
            manaSpentOnXByColor = paymentResult.xManaSpentByColor,
            faceIndex = action.faceIndex,
            spentManaProvenance = paymentResult.spentManaProvenance,
            castTimeFlags = castTimeFlags,
            // Every enumerated alternative-cost offer names its mechanic explicitly, so this is the
            // declared choice rather than a guess. Descriptive only — the rules consequences of each
            // mechanic ride the `was*` flags above.
            alternativeCost = action.alternativeCostType?.takeIf { action.useAlternativeCost }
        )

        if (!castResult.isSuccess) {
            return castResult
        }

        var currentCastState = castResult.newState
        var allEvents = events + castResult.events

        // Freeze a graveyard-cast entry rider onto the stack spell now; StackResolver reads it back
        // and applies it when the permanent resolves onto the battlefield. Two sources feed it:
        //  - The Tomb of Aclazotz: a rider-bearing MayCastFromGraveyard grant (finality counter +
        //    added subtype), from the specific grant that authorized this cast.
        //  - Osteomancer Adept's forage permission: "that creature enters with a finality counter on
        //    it" (finality only, no added subtype) — reusing the same entry-rider plumbing.
        //  - Intrepid Paleontologist: a rider-bearing GrantMayCastFromLinkedExile ("If you cast a
        //    spell this way, that creature enters with a finality counter on it") — same plumbing,
        //    but the authorizing grant is the linked-exile cast permission captured pre-cast.
        val riderCounter: CounterType? = when {
            graveyardCastRiderGrant?.hasEntryRider == true -> graveyardCastRiderGrant.entersWithCounter
            isForageCast -> CounterType.FINALITY
            linkedExileGranterEntry?.ability?.entersWithCounter != null ->
                linkedExileGranterEntry.ability.entersWithCounter
            else -> null
        }
        val riderSubtype: String? =
            graveyardCastRiderGrant?.takeIf { it.hasEntryRider }?.addedSubtypeOnEntry
        if (riderCounter != null || riderSubtype != null) {
            currentCastState = currentCastState.updateEntity(action.cardId) { c ->
                c.with(
                    com.wingedsheep.engine.state.components.stack.GraveyardCastRiderComponent(
                        entersWithCounter = riderCounter,
                        addedSubtype = riderSubtype
                    )
                )
            }
        }

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

        // The permission belongs to its granting permanent, not to the player. Mark the source
        // captured before the card left the library; a source that leaves and returns is a new
        // object with a fresh allowance, and an unlimited matching source consumes nothing.
        if (limitedTopLibraryCastSource != null) {
            currentCastState = currentCastState.updateEntity(limitedTopLibraryCastSource) { c ->
                val tracker = c.get<com.wingedsheep.engine.state.components.battlefield.CastFromTopOfLibraryUsesThisTurnComponent>()
                c.with(
                    com.wingedsheep.engine.state.components.battlefield.CastFromTopOfLibraryUsesThisTurnComponent(
                        uses = (tracker?.uses ?: 0) + 1
                    )
                )
            }
        }

        // Record once-per-turn free-cast permission usage (e.g., Zaffai and the Tempests). Only a
        // `MayCastWithoutPayingManaCost(oncePerTurn = true)` source consumes a use, and only when
        // no unlimited free-cast source could have paid instead.
        if (action.useWithoutPayingManaCost) {
            // Use the pre-cast `state` to recover the spell's origin zone — by now the card has
            // left it for the stack — so a `fromExileOnly` source (Warped Space) is only consumed
            // for an actual exile cast.
            val castFromZone = castSourceZone(state, action.cardId)
            val onceSource = costCalculator.oncePerTurnFreeCastSourceToConsume(currentCastState, action.playerId, cardDef, castFromZone)
            if (onceSource != null) {
                currentCastState = currentCastState.updateEntity(onceSource) { c ->
                    c.with(com.wingedsheep.engine.state.components.battlefield.MayCastWithoutPayingCostUsedThisTurnComponent)
                }
            }
        }

        // Record once-per-turn graveyard-cast permission usage (Gisa and Geralf). The card has
        // already left the graveyard for the stack, so the grant lookup runs against the pre-cast
        // `state`; a use is only burned when no unlimited grant could have authorized the cast.
        if (castSourceZone(state, action.cardId) == Zone.GRAVEYARD) {
            val graveyardOnceSource =
                zoneResolver.oncePerTurnGraveyardCastSourceToConsume(state, action.playerId, action.cardId)
            if (graveyardOnceSource != null) {
                currentCastState = currentCastState.updateEntity(graveyardOnceSource) { c ->
                    c.with(com.wingedsheep.engine.state.components.battlefield.MayCastFromGraveyardUsedThisTurnComponent)
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
                            activeZones = setOf(Zone.STACK),
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
                        activeZones = setOf(Zone.STACK),
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
                        activeZones = setOf(Zone.STACK),
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

        // Consume any matching "next spell has affinity for X" riders (Don & Raph). The cost
        // reduction was already applied by the cost calculator while these riders were present;
        // here we just remove the riders that matched the spell just cast, so only the *next*
        // matching spell is affected.
        run {
            val affinityEvalContext = PredicateContext(controllerId = action.playerId)
            val matchingAffinityRiders = currentCastState.pendingNextSpellAffinities.filter { pending ->
                pending.controllerId == action.playerId &&
                    predicateEvaluator.matches(
                        currentCastState, currentCastState.projectedState, action.cardId, pending.spellFilter, affinityEvalContext
                    )
            }
            if (matchingAffinityRiders.isNotEmpty()) {
                currentCastState = currentCastState.copy(
                    pendingNextSpellAffinities = currentCastState.pendingNextSpellAffinities.filter { it !in matchingAffinityRiders }
                )
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
        sacrificedSnapshots: List<EntitySnapshot>,
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
     * Mana-affordability gate for cast-time mode selection: can the caster still pay
     * the spell's total cost if [chosenIndices] end up being the chosen modes? Chosen
     * modes' additional mana costs stack (rule 700.2h), so a pick that is affordable
     * alone can become unpayable combined with earlier picks — and by the time payment
     * runs (after target selection) the only way out is cancelling the whole cast.
     *
     * The base cost comes from [computeTotalCastCost] — the same pipeline payment uses —
     * so alternative costs, cost modifiers, and alternative payments (convoke/delve)
     * can't make the gate disagree with payment. A "without paying its mana cost" cast
     * still owes the stacked per-mode additional costs (CR 601.2b, 601.2f — additional
     * costs apply on top of an alternative cost).
     */
    private fun canPayModeSelection(
        state: GameState,
        action: CastSpell,
        modalEffect: ModalEffect,
        chosenIndices: List<Int>
    ): Boolean {
        // Escalate with a non-mana cost (CR 702.120a): each mode beyond the first owes another
        // discard / tap / …, so a pick can run the caster out of cards to pay with just as it can
        // run them out of mana.
        val escalatePayability = EscalateCosts.payability(
            state, action.playerId, action.cardId, modalEffect, costEnumerationUtils, predicateEvaluator
        )
        if (escalatePayability != null &&
            (chosenIndices.size - 1).coerceAtLeast(0) > escalatePayability.maxExtraModes
        ) {
            return false
        }

        val extraCosts = buildList {
            addAll(chosenIndices.mapNotNull { modalEffect.modes.getOrNull(it)?.additionalManaCost })
            modalEffect.additionalManaCostPerExtraMode?.let { perExtraMode ->
                repeat((chosenIndices.size - 1).coerceAtLeast(0)) { add(perExtraMode) }
            }
        }
        // Nothing stacks — base-cost affordability was already validated on the cast action.
        if (extraCosts.isEmpty()) return true
        val cardComponent = state.getEntity(action.cardId)?.get<CardComponent>() ?: return true
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId) ?: return true
        val playForFree = zoneResolver.hasPlayWithoutPayingCost(state, action.playerId, action.cardId) ||
            action.useWithoutPayingManaCost
        val computed = computeTotalCastCost(
            state,
            action,
            cardDef,
            cardComponent,
            playForFree,
            castingFromCommandZone = zoneResolver.hasCommanderCastPermission(state, action.playerId, action.cardId)
        ) ?: return false
        var cost = computed.cost
        for (extra in extraCosts) {
            cost = cost + ManaCost.parse(extra)
        }
        return validatePayment(state, action, cost, computed.paymentXValue) == null
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
        val candidateIndices = availableIndices ?: repeatAvailableIndices ?: modalEffect.modes.indices.toList()
        // Rule 700.2h — only offer a mode the caster can still pay for on top of the
        // modes already picked. Without this gate an unpayable combination sails
        // through mode + target selection and dead-ends at payment, where the pending
        // decision can never be answered legally (only cancelled).
        val offerIndices = candidateIndices.filter { candidate ->
            canPayModeSelection(state, baseCastAction, modalEffect, selectedModeIndices + candidate)
        }
        if (offerIndices.isEmpty() && selectedModeIndices.size < modalEffect.minChooseCount) {
            return ExecutionResult.error(
                state,
                "Cannot afford the additional cost of any remaining mode for $cardName"
            )
        }
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

        is com.wingedsheep.sdk.scripting.effects.ManaSpellRider.CopySpellWhenSpent ->
            buildCopySpellRiderTrigger(state, action, cardComponent, rider.spellFilter)

        is com.wingedsheep.sdk.scripting.effects.ManaSpellRider.GrantsKeywordWhenSpent ->
            applyKeywordGrantRider(state, action, rider.keyword, rider.spellFilter) to emptyList()
    }

    /**
     * Carnelian Orb of Dragonkind's rider: if the cast spell matches [spellFilter], float an
     * end-of-turn grant of [keyword] keyed to the spell. Otherwise no-op (the mana paid for
     * something else).
     *
     * Unlike the copy / scry riders this queues nothing onto the stack — "it gains haste until end
     * of turn" is a continuous effect the printed card applies without a triggered ability. The
     * grant is keyed to the spell's entity id, which a permanent spell keeps as it resolves onto the
     * battlefield (see [com.wingedsheep.engine.mechanics.stack.StackResolver.resolvePermanentSpell]),
     * so the keyword is live the instant the permanent exists — exactly what haste needs.
     *
     * The spell is matched with [PredicateEvaluator] against its stack characteristics, at payment
     * time rather than at resolution. That's what the printed rulings require: mana spent on a
     * non-Dragon spell that *becomes* a Dragon later in the turn grants nothing.
     *
     * The floating effect's source is the spell itself, not the mana's producer — the producer may
     * already have left the battlefield, and the source is only read for the effect's display name.
     */
    private fun applyKeywordGrantRider(
        state: GameState,
        action: CastSpell,
        keyword: String,
        spellFilter: com.wingedsheep.sdk.scripting.GameObjectFilter,
    ): GameState {
        val matches = predicateEvaluator.matches(
            state,
            state.projectedState,
            action.cardId,
            spellFilter,
            PredicateContext(controllerId = action.playerId)
        )
        if (!matches) return state

        return state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.GrantKeyword(keyword),
            affectedEntities = setOf(action.cardId),
            duration = Duration.EndOfTurn,
            context = EffectContext(sourceId = action.cardId, controllerId = action.playerId)
        )
    }

    /**
     * Pyromancer's Goggles' rider: if the cast spell matches [spellFilter], queue a copy trigger
     * above the spell. Otherwise no-op (the {R} was spent on something else).
     *
     * The trigger resolves *before* the spell it copies, which is the printed behavior — the copy
     * is put onto the stack above the original and resolves first (CR 707.10). The copy's controller
     * may choose new targets, handled by [CopyTargetSpellEffect]'s own retarget pause.
     *
     * The spell is matched with [PredicateEvaluator] against its stack characteristics — projected
     * *battlefield* state doesn't apply to an object on the stack, but the evaluator still reads
     * color/type off the spell's [CardComponent], which is what "a red instant or sorcery spell"
     * needs. Matching happens now, at payment time, not at trigger resolution.
     */
    private fun buildCopySpellRiderTrigger(
        state: GameState,
        action: CastSpell,
        cardComponent: CardComponent,
        spellFilter: com.wingedsheep.sdk.scripting.GameObjectFilter,
    ): Pair<GameState, List<PendingTrigger>> {
        val matches = predicateEvaluator.matches(
            state,
            state.projectedState,
            action.cardId,
            spellFilter,
            PredicateContext(controllerId = action.playerId)
        )
        if (!matches) return state to emptyList()

        val copyAbility = TriggeredAbility(
            id = AbilityId.generate(),
            trigger = SdkGameEvent.SpellCastEvent(player = Player.You),
            binding = TriggerBinding.SELF,
            effect = com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect(
                target = com.wingedsheep.sdk.scripting.targets.EffectTarget.TriggeringEntity
            ),
            activeZones = setOf(Zone.STACK),
            descriptionOverride = "Copy ${cardComponent.name}. You may choose new targets for the copy."
        )
        val pending = PendingTrigger(
            ability = copyAbility,
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
            activeZones = setOf(Zone.STACK),
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
