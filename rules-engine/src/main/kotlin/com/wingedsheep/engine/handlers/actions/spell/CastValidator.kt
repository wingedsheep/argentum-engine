package com.wingedsheep.engine.handlers.actions.spell

import com.wingedsheep.engine.core.AdditionalCostSelectionKind
import com.wingedsheep.engine.core.AlternativeCostType
import com.wingedsheep.engine.core.CardsDiscardedEvent
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CastSpellAdditionalCostContinuation
import com.wingedsheep.engine.core.CastWithCreatureTypeContinuation
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.core.LifeChangedEvent
import com.wingedsheep.engine.core.ManaSpentEvent
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PermanentsSacrificedEvent
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.event.TriggerContext
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.costs.ForageCostResolver
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.handlers.effects.bend.BendEvents
import com.wingedsheep.engine.handlers.effects.life.LifePaymentService
import com.wingedsheep.engine.mechanics.DisturbCasts
import com.wingedsheep.engine.mechanics.EmergeCasts
import com.wingedsheep.engine.mechanics.EscalateCosts
import com.wingedsheep.engine.mechanics.FlashbackGrants
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.mechanics.MayhemGrants
import com.wingedsheep.engine.mechanics.MiracleGrants
import com.wingedsheep.engine.mechanics.ModalChooseCounts
import com.wingedsheep.engine.mechanics.SneakWindow
import com.wingedsheep.engine.mechanics.SpliceCasts
import com.wingedsheep.engine.mechanics.WarpGrants
import com.wingedsheep.engine.mechanics.WebSlinging
import com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost
import com.wingedsheep.engine.mechanics.cost.spell.SpellCostCheck
import com.wingedsheep.engine.mechanics.cost.spell.SpellCostLedger
import com.wingedsheep.engine.mechanics.cost.spell.SpellCosts
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.TapForGeneric
import com.wingedsheep.engine.mechanics.mana.paymentSubtypesOf
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TextChanges
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.PlayWithAdditionalCostComponent
import com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent
import com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.player.CantCastFromNonHandZonesComponent
import com.wingedsheep.engine.state.components.player.GrantedSpellKeywordsComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.ManaSpentOnSpellsThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerCantPlayFromHandComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.engine.state.permissions.activeMayPlayFor
import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.giftKeyword
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern as SdkGameEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.TapReason
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PermanentCostAction
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.StormCopyEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlin.reflect.KClass

/**
 * Where a cast is being made from, and on what authority (CR 601.2a — the card moves from where it
 * is to the stack; casting it from anywhere but a hand needs a permission). The first route that
 * applies wins, in the order [CastValidator.castSource] tries them.
 */
internal enum class CastSourceRoute {
    HAND,
    TOP_OF_LIBRARY,
    EXILE_PERMISSION,
    SELF_ZONE_PERMISSION,
    PERMANENT_FROM_GRAVEYARD,
    FLASHBACK,
    HARMONIZE,
    MAYHEM,
    GRAVEYARD_PERMISSION,
    FORAGE_FROM_GRAVEYARD,
    WARP_FROM_GRAVEYARD,
    COMMANDER,
    GRAVEYARD_SNEAK,
    DISTURB,
}

/** The resolved source of a cast, and the face it puts on the stack when it is cast transformed. */
internal class CastSource(
    val route: CastSourceRoute,
    /**
     * The face this cast puts on the stack when it is cast **transformed** (CR 712.8c / 712.8f) —
     * it drives timing, targeting, the aura target, colors and subtypes, which must all read this
     * face rather than the printed front. Three sources, all meaning "back face up on the stack":
     * disturb's printed keyword, the modal-DFC face choice, and a may-play permission granted with
     * `castTransformed` (CR 310.12b — "exile it, then you may cast it transformed").
     */
    val transformedFace: CardDefinition?
) {
    val inHand: Boolean get() = route == CastSourceRoute.HAND
}

/**
 * Whether a [CastSpell] is legal (CR 601.2, with the illegal-cast rewind of CR 601.2e/733 as the
 * rejection) — one function per question, asked in the order the casting procedure raises them:
 * may the card be cast from where it is, at this time, with these announcements, for this cost,
 * at these targets.
 *
 * A `GameAction` is client-supplied, so every field is checked here rather than trusted because the
 * enumerator offered it.
 */
internal class CastValidator(
    private val cardRegistry: CardRegistry,
    private val turnManager: TurnManager,
    private val costCalculator: CostCalculator,
    private val alternativePaymentHandler: AlternativePaymentHandler,
    private val costHandler: CostHandler,
    private val targetValidator: TargetValidator,
    private val conditionEvaluator: ConditionEvaluator,
    private val zoneResolver: CastZoneResolver,
    private val castPermissionUtils: com.wingedsheep.engine.legalactions.utils.CastPermissionUtils,
    private val castCostTotaller: CastCostTotaller,
    private val castCostPayer: CastCostPayer,
    private val grantedKeywordResolver: com.wingedsheep.engine.mechanics.mana.GrantedKeywordResolver,
    private val predicateEvaluator: PredicateEvaluator,
    private val legality: com.wingedsheep.engine.legality.LegalityKernel
) {

    fun validate(state: GameState, action: CastSpell): String? {
        if (!state.hasPriority(action.playerId)) {
            return "You don't have priority"
        }
        val container = state.getEntity(action.cardId)
            ?: return "Card not found: ${action.cardId}"
        val cardComponent = container.get<CardComponent>()
            ?: return "Not a card: ${action.cardId}"
        val source = castSource(state, action, cardComponent)
            ?: return "Card is not in your hand"
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        validateAuthority(state, action, cardComponent, cardDef, source)?.let { return it }
        if (action.castFaceDown) return validateFaceDownCast(state, action, cardDef)
        validateTiming(state, action, cardComponent, cardDef, source)?.let { return it }
        validateAlternativeCostSelections(state, action, cardDef)?.let { return it }
        validateAnnouncements(state, action, cardDef)?.let { return it }
        validateOwedCosts(state, action, cardDef)?.let { return it }
        validateOptionalCostKeywords(state, action, cardComponent, cardDef, source)?.let { return it }
        validateTotalCost(state, action, cardComponent, cardDef, source)?.let { return it }
        validateTargets(state, action, cardDef, source)?.let { return it }
        validateDamageDistribution(action, cardDef)?.let { return it }
        return validateTargetLifeTaxes(state, action)
    }

    /**
     * The route this card is being cast by (CR 601.2a), or null when none applies. A card outside
     * the caster's hand needs a permission, and the routes are tried in a fixed order so a card two
     * of them could authorize is attributed to the first.
     */
    private fun castSource(state: GameState, action: CastSpell, cardComponent: CardComponent): CastSource? {
        val playerId = action.playerId
        val cardId = action.cardId
        val routes: List<Pair<CastSourceRoute, () -> Boolean>> = listOf(
            CastSourceRoute.HAND to { cardId in state.getZone(ZoneKey(playerId, Zone.HAND)) },
            CastSourceRoute.TOP_OF_LIBRARY to { zoneResolver.isOnTopOfLibraryWithPermission(state, playerId, cardId) },
            CastSourceRoute.EXILE_PERMISSION to { zoneResolver.isInExileWithPlayPermission(state, playerId, cardId) },
            CastSourceRoute.SELF_ZONE_PERMISSION to { zoneResolver.hasMayCastSelfFromZonePermission(state, playerId, cardId) },
            CastSourceRoute.PERMANENT_FROM_GRAVEYARD to {
                zoneResolver.hasMayPlayPermanentFromGraveyardPermission(state, playerId, cardId, cardComponent)
            },
            CastSourceRoute.FLASHBACK to { zoneResolver.hasFlashbackPermission(state, playerId, cardId) },
            // Harmonize (e.g., Channeled Dragonfire) — cast from graveyard for its harmonize cost;
            // `hasHarmonizePermission` checks the graveyard zone + Harmonize keyword.
            CastSourceRoute.HARMONIZE to { zoneResolver.hasHarmonizePermission(state, playerId, cardId) },
            // Mayhem (CR 702.187, e.g. Swarm, Being of Bees) — cast from graveyard for its mayhem
            // cost if you discarded it this turn; `hasMayhemPermission` checks the keyword + gate.
            CastSourceRoute.MAYHEM to {
                action.useAlternativeCost && action.altAllows(AlternativeCostType.MAYHEM) &&
                    zoneResolver.hasMayhemPermission(state, playerId, cardId)
            },
            CastSourceRoute.GRAVEYARD_PERMISSION to {
                zoneResolver.hasMayCastFromGraveyardPermission(state, playerId, cardId, cardComponent)
            },
            CastSourceRoute.FORAGE_FROM_GRAVEYARD to {
                zoneResolver.hasMayCastCreaturesFromGraveyardWithForage(state, playerId, cardId, cardComponent)
            },
            // Warp from graveyard (e.g., Timeline Culler) — `hasWarpPermission` checks both hand and
            // graveyard; this route covers the graveyard case.
            CastSourceRoute.WARP_FROM_GRAVEYARD to {
                action.useAlternativeCost && zoneResolver.hasWarpPermission(state, playerId, cardId)
            },
            CastSourceRoute.COMMANDER to { zoneResolver.hasCommanderCastPermission(state, playerId, cardId) },
            // Granted graveyard sneak (Ninja Teen): a creature card in the player's graveyard while
            // they control an active "creature cards in your graveyard have sneak {cost}" grant.
            CastSourceRoute.GRAVEYARD_SNEAK to {
                action.useAlternativeCost && action.altAllows(AlternativeCostType.SNEAK) &&
                    cardComponent.typeLine.isCreature &&
                    cardId in state.getGraveyard(playerId) &&
                    SneakWindow.graveyardSneakGrantCost(state, playerId, cardRegistry) != null
            },
        )
        val route = routes.firstOrNull { (_, applies) -> applies() }?.first

        // Disturb (CR 702.146a) — cast transformed from your graveyard for the disturb cost. The
        // face this cast puts on the stack is the back face, and it drives timing and targeting
        // (CR 712.8c), so the permission check hands back the face itself. Checked only once the
        // hand, library, exile and self-zone routes are ruled out.
        val disturbFace = if (
            (route == null || route > CastSourceRoute.SELF_ZONE_PERMISSION) &&
            action.useAlternativeCost && action.altAllows(AlternativeCostType.DISTURB)
        ) {
            zoneResolver.disturbCastFace(state, playerId, cardId)
        } else null
        val resolvedRoute = route ?: (if (disturbFace != null) CastSourceRoute.DISTURB else return null)

        // Modal DFC back face (CR 712.11b) — the hand-side counterpart of disturb. The caster chose
        // the back face, so the card goes on the stack transformed for that face's own mana cost. No
        // zone guard is needed beyond the resolver's own (it only looks in hand).
        val modalBackFace = if (action.useAlternativeCost && action.altAllows(AlternativeCostType.MODAL_BACK_FACE)) {
            zoneResolver.modalBackCastFace(state, playerId, cardId)
        } else null
        // The zone legality of a `castTransformed` permission was already settled by the exile /
        // self-zone routes, so that lookup only answers *which face*.
        val transformedFace = disturbFace
            ?: modalBackFace
            ?: zoneResolver.permissionTransformedCastFace(state, playerId, cardId)
        return CastSource(resolvedRoute, transformedFace)
    }

    /**
     * Whether this caster may cast this card by this route at all: the gift promise, hand- and
     * zone-scoped casting bans, the single cast-legality chokepoint, and what a may-play permission
     * authorizes.
     */
    private fun validateAuthority(
        state: GameState,
        action: CastSpell,
        cardComponent: CardComponent,
        cardDef: CardDefinition?,
        source: CastSource,
    ): String? {
        // Gift (CR 702.174a): the promise is an additional cost whose "payment" is choosing an
        // opponent, so the recipient must be an opponent of the caster and the card must actually
        // have gift.
        action.giftRecipient?.let { recipient ->
            if (cardDef?.giftKeyword() == null) {
                return "${cardComponent.name} has no gift cost to promise"
            }
            if (recipient !in state.getOpponents(action.playerId)) {
                return "A gift can only be promised to an opponent"
            }
        }

        // Memory Vessel: "they can't play cards from their hand" — hand-scoped, so casts from
        // exile/graveyard granted by a may-play permission still resolve.
        if (source.inHand && state.getEntity(action.playerId)?.has<PlayerCantPlayFromHandComponent>() == true) {
            return "You can't play cards from your hand"
        }

        // Avatar's Wrath: "your opponents can't cast spells from anywhere other than their hands."
        // A per-player, duration-bounded restriction to hand-only casting — any non-hand cast
        // (flashback/escape from graveyard, foretell/plot/may-play from exile, library top, command
        // zone) is illegal while the component is present. Ordinary hand casts are untouched.
        if (!source.inHand && state.getEntity(action.playerId)?.has<CantCastFromNonHandZonesComponent>() == true) {
            return "You can't cast spells from anywhere other than your hand right now"
        }

        // Single cast-legality chokepoint: per-turn spell limit (Yawgmoth's Agenda), Silence-style
        // can't-cast, Mana Maze color sharing, and PlayersCantCastSpells (Voice of Victory, …) all
        // resolve to a reason here, or null if the cast is allowed.
        castPermissionUtils.reasonCannotCast(state, action.playerId, action.cardId)?.let { return it }

        // The spell being cast can't be one of the three cards it exiles to pay for itself, so it's
        // excluded from the forage exile pool here just as it is at payment time.
        if (source.route == CastSourceRoute.FORAGE_FROM_GRAVEYARD &&
            !ForageCostResolver.canPay(state, action.playerId, excludeCardId = action.cardId)
        ) {
            return "Cannot forage: need 3 other cards in graveyard or a Food"
        }

        return if (source.route == CastSourceRoute.EXILE_PERMISSION) {
            validatePermittedFace(state, action, cardComponent, cardDef)
        } else null
    }

    /**
     * A may-play permission authorizes exactly one set of characteristics. By default that is the
     * card's primary face; a prepare-spell copy (Secrets of Strixhaven) or a permission carrying
     * `castFaceIndex` ("cast it from your graveyard as an Adventure" — Mosswood Dreadknight,
     * CR 715.3) authorizes an alternative face instead. `faceIndex` is client-supplied, so reject any
     * face the permission doesn't cover — otherwise a hand-constructed action could cast the cheap
     * Adventure half of a card that was only granted its creature half, or vice versa.
     *
     * Only permissions constrain faces. The exile route also covers a linked-exile static grant
     * (Valgavoth, Maralen), which carries no permission and no face notion — an empty permission
     * list means the authorization came from elsewhere, so it is left alone.
     */
    private fun validatePermittedFace(
        state: GameState,
        action: CastSpell,
        cardComponent: CardComponent,
        cardDef: CardDefinition?,
    ): String? {
        val permissions = state.activeMayPlayFor(action.cardId, action.playerId, conditionEvaluator, cardRegistry)
        if (permissions.isEmpty()) return null
        val isPrepareCopy = state.getEntity(action.cardId)?.has<PreparedSpellCopyComponent>() == true &&
            cardDef?.layout == com.wingedsheep.sdk.model.CardLayout.PREPARE
        val authorizedFaces: Set<Int?> =
            if (isPrepareCopy) setOf(0) else permissions.map { it.castFaceIndex }.toSet()
        if (action.faceIndex !in authorizedFaces) {
            val faceName = action.faceIndex
                ?.let { cardDef?.cardFaces?.getOrNull(it)?.name }
                ?: cardComponent.name
            return "You don't have permission to cast $faceName from there"
        }
        // "You may cast red spells from among them" (Chandra, Dressed to Kill −7). The colour
        // restriction is on the *spell*, so it is checked against the face being cast — a red
        // MDFC's blue back face is not castable through such a permission even though the exiled
        // card is red. Authoritative: the enumerator applies the same rule, but the action is
        // client-supplied.
        val castColors = action.faceIndex
            ?.let { cardDef?.cardFaces?.getOrNull(it)?.manaCost?.colors }
            ?: cardDef?.colors
            ?: cardComponent.manaCost.colors
        if (permissions.none { it.castColorRestriction == null || it.castColorRestriction in castColors }) {
            val required = permissions.firstNotNullOfOrNull { it.castColorRestriction }
            return "You may only cast ${required?.name?.lowercase()} spells from there"
        }
        return null
    }

    /**
     * Face-down casting — morph (CR 702.37a) or disguise (CR 702.168a). Both are "cast this card
     * face down as a 2/2 for {3}" at sorcery speed; the mode only decides what the resulting
     * permanent looks like and costs to turn up. Nothing printed on the card applies, so this is the
     * whole check.
     */
    private fun validateFaceDownCast(state: GameState, action: CastSpell, cardDef: CardDefinition?): String? {
        val castableFaceDown = cardDef?.keywordAbilities?.any {
            it is KeywordAbility.Morph || it is KeywordAbility.Disguise
        } == true
        if (!castableFaceDown) {
            return "This card cannot be cast face down (no morph or disguise ability)"
        }
        if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
            return "You can only cast face-down creatures at sorcery speed"
        }
        return castCostPayer.validateManaPayment(state, action, costCalculator.calculateFaceDownCost(state, action.playerId))
    }

    /**
     * Timing (CR 307.1 / 304.1 / 702.8a). For Adventure / split faces the face's type line decides
     * (CR 715 / 709.4); a transformed cast is timed by the back face it puts on the stack
     * (CR 712.8c).
     */
    private fun validateTiming(
        state: GameState,
        action: CastSpell,
        cardComponent: CardComponent,
        cardDef: CardDefinition?,
        source: CastSource,
    ): String? {
        val transformedFace = source.transformedFace
        val effectiveTypeLine = action.faceIndex
            ?.let { cardDef?.cardFaces?.getOrNull(it)?.typeLine }
            ?: transformedFace?.typeLine
            ?: cardComponent.typeLine
        if (effectiveTypeLine.isInstant) return null
        // Printed flash comes off the same face as the type line above, for the same reason:
        // CR 712.11c evaluates only the face being cast, so a modal DFC whose *front* has flash
        // grants none to a sorcery-speed back. `transformedFace` is null for an ordinary cast, which
        // leaves this reading the card's own keywords. A *granted* flash below is a property of the
        // card object, not of a face, so it is unaffected.
        val faceKeywords = transformedFace?.keywords ?: cardDef?.keywords ?: emptySet()
        val grantedFlash = faceKeywords.contains(Keyword.FLASH) || zoneResolver.hasGrantedFlash(state, action.cardId)
        // A from-exile may-play permission with an "as though it had flash" rider (Azula, Cunning
        // Usurper) lets a non-instant exiled card be cast at instant speed (CR 702.8).
        val mayPlayFlash = state.activeMayPlayFor(action.cardId, action.playerId, conditionEvaluator, cardRegistry)
            .any { it.asThoughFlash }
        // A flash-timing kicker unlocks instant-speed casting when paid — whether the optional cost
        // is mana (Ghitu Fire) or a non-mana cost like Behold (Molten Exhale).
        val flashTimingKicker = declaredOptionalCosts(action, cardDef).any { it.grantsFlashTiming }
        if (!grantedFlash && !mayPlayFlash && !flashTimingKicker && !isCastingForSneak(state, action, cardDef) &&
            !turnManager.canPlaySorcerySpeed(state, action.playerId)
        ) {
            return "You can only cast sorcery-speed spells during your main phase with an empty stack"
        }
        return null
    }

    /**
     * Sneak (CR 702.190a) grants an instant-speed casting permission during the active player's
     * declare blockers step — bypassing the normal sorcery-speed timing.
     */
    private fun isCastingForSneak(state: GameState, action: CastSpell, cardDef: CardDefinition?): Boolean =
        action.useAlternativeCost &&
            action.altAllows(AlternativeCostType.SNEAK) &&
            cardDef != null &&
            SneakWindow.effectiveSneakCost(state, cardDef, action.cardId, action.playerId, cardRegistry) != null

    /**
     * The selections an alternative cost's non-mana portion names: sneak and web-slinging each
     * return one creature, emerge sacrifices one.
     */
    private fun validateAlternativeCostSelections(state: GameState, action: CastSpell, cardDef: CardDefinition?): String? {
        // Sneak (CR 702.190a): legal only during the active player's declare blockers step, and the
        // player must return exactly one unblocked attacker they control to its owner's hand as the
        // non-mana portion of the cost.
        if (isCastingForSneak(state, action, cardDef)) {
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
        // control to its owner's hand as the non-mana portion of the alternative cost. Timing is the
        // spell's normal timing — web-slinging grants no extra permission.
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

        // Emerge (CR 702.119a/c): the player must sacrifice exactly one creature they control as the
        // non-mana portion of the alternative cost, chosen as they choose to pay the emerge cost
        // (CR 601.2b). Timing is the spell's normal timing — emerge grants no extra permission. The
        // chosen creature also fixes the generic reduction, so the total cost is priced against
        // exactly this selection.
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
        return null
    }

    /**
     * The card's own cast restrictions, and the choose-N modal shape (rules 700.2a / 700.2d) when
     * the action arrives with modes chosen — the cast-time continuation flow starts with none, which
     * falls through to the pause in execute().
     */
    private fun validateAnnouncements(state: GameState, action: CastSpell, cardDef: CardDefinition?): String? {
        if (cardDef == null) return null
        if (cardDef.script.castRestrictions.isNotEmpty()) {
            legality.castRestrictionsFailure(state, action.playerId, cardDef.script.castRestrictions)?.let { return it }
        }
        if (action.chosenModes.isNotEmpty()) {
            val modalEffect = cardDef.script.spellEffect as? ModalEffect
            if (modalEffect != null) {
                validateChosenModeShape(state, modalEffect, action)?.let { return it }
            }
        }
        // The declared optional additional cost (kicker/offspring/bargain): the card must actually
        // have a keyword declaring that slot, so a hand-built action can't claim to have bargained a
        // kicker spell (or bargained a card with no bargain at all).
        if (action.declaredCostSlot != null && declaredOptionalCosts(action, cardDef).isEmpty()) {
            val mechanic = when (action.declaredCostSlot) {
                ChoiceSlot.BARGAINED -> "bargain"
                ChoiceSlot.KICKED -> "kicker"
                else -> action.declaredCostSlot.name.lowercase()
            }
            return "This card does not have $mechanic"
        }
        // "…rather than pay this spell's mana cost **if** <condition>" (Blasphemous Edict). Mirrors
        // the availability gate in CastSpellEnumerator so an authorization can't outlive the
        // enumeration that offered it.
        if (action.useAlternativeCost && action.altAllows(AlternativeCostType.SELF_ALTERNATIVE)) {
            val selfAltCondition = cardDef.script.selfAlternativeCost?.condition
            if (selfAltCondition != null && !conditionEvaluator.evaluate(
                    state, selfAltCondition, EffectContext(sourceId = action.cardId, controllerId = action.playerId)
                )
            ) {
                return "Alternative cost is not available: ${selfAltCondition.description}"
            }
        }
        return null
    }

    /**
     * Every additional cost the cast owes — the same list execute() pays
     * ([CastCostPayer.owedAdditionalCosts]) — against the submitted payment (CR 601.2h).
     */
    private fun validateOwedCosts(state: GameState, action: CastSpell, cardDef: CardDefinition?): String? =
        validateAdditionalCosts(state, castCostPayer.owedAdditionalCosts(state, action, cardDef), action)

    /** The first reason the submitted payment can't pay [additionalCosts] (CR 601.2h), or null. */
    private fun validateAdditionalCosts(state: GameState, additionalCosts: List<AdditionalCost>, action: CastSpell): String? {
        val check = SpellCostCheck(state, action, costHandler, predicateEvaluator)
        return SpellCosts.reduceAlternatives(additionalCosts, state, action.playerId, action.additionalCostPayment, costHandler)
            .firstNotNullOfOrNull { SpellCosts.validate(check, it) }
    }

    /**
     * The keyword-driven optional additional costs whose selection rides on its own action field:
     * conspire, casualty and splice. Each must be printed or granted on the spell.
     */
    private fun validateOptionalCostKeywords(
        state: GameState,
        action: CastSpell,
        cardComponent: CardComponent,
        cardDef: CardDefinition?,
        source: CastSource,
    ): String? {
        // Conspire (CR 702.78). Two untapped creatures the caster controls, each sharing a color
        // with the spell. The spell must have Conspire either printed or granted (e.g., Raiding
        // Schemes: "Each noncreature spell you cast has conspire").
        if (action.conspiredCreatures.isNotEmpty()) {
            if (cardDef == null) return "Conspire requires a card definition"
            validateConspire(state, action, cardDef)?.let { return it }
        }
        // Casualty (CR 702.153). One creature the caster controls with projected power >= the
        // spell's casualty threshold. The spell must have Casualty either printed or granted (e.g.,
        // Silverquill: "Each instant and sorcery spell you cast has casualty 1").
        if (action.casualtyCreature != null) {
            if (cardDef == null) return "Casualty requires a card definition"
            validateCasualty(state, action, cardDef)?.let { return it }
        }
        // Splice (CR 702.47). Each revealed card must be in the caster's hand, carry splice, splice
        // onto a quality this spell actually has, and appear at most once. Checked before the cost
        // is computed, because each splice cost is folded into the total cost (CR 601.2b/f).
        if (action.splicedCardIds.isNotEmpty()) {
            validateSplice(state, action, cardDef, cardComponent, source.transformedFace)?.let { return it }
        }
        return null
    }

    /**
     * The total cost (CR 601.2f), less what the declared tap/exile payments cover, can be paid the
     * way the caster said they'd pay it.
     */
    private fun validateTotalCost(
        state: GameState,
        action: CastSpell,
        cardComponent: CardComponent,
        cardDef: CardDefinition?,
        source: CastSource,
    ): String? {
        // "As an additional cost to cast creature spells, you may pay any amount of mana" (Chorus of
        // the Conclave). The amount is client-supplied, so it must be non-negative and backed by a
        // grant that applies to this very spell. A face-down cast is excluded: the grant's filter
        // can't be checked against the hidden card, and the enumerator never offers it.
        if (action.additionalManaForCounters < 0) return "Additional mana paid can't be negative"
        if (action.additionalManaForCounters > 0) {
            if (action.castFaceDown) return "Additional mana for counters can't be paid for a face-down spell"
            com.wingedsheep.engine.mechanics.mana.AdditionalManaForCounters.applicableGrant(state, action.playerId, action.cardId, cardRegistry, predicateEvaluator = predicateEvaluator)
                ?: return "No permanent you control lets you pay additional mana for this spell"
        }

        // Free if PlayWithoutPayingCostComponent is present, or if a MayCastWithoutPayingManaCost
        // battlefield source (e.g. Weftwalking) is the chosen alt.
        if (action.useWithoutPayingManaCost) {
            // CR 118.9a — only one alternative cost can apply to a given cast.
            if (action.useAlternativeCost) {
                return "Cannot combine 'without paying its mana cost' with another alternative cost"
            }
            // Pass the spell's origin zone so a `fromExileOnly` source (Warped Space) validates an
            // exile cast while staying withheld from hand casts.
            if (!costCalculator.hasFreeCastPermission(state, action.playerId, cardDef, castCostTotaller.castSourceZone(state, action.cardId))) {
                return "'Without paying its mana cost' is not available (gate closed or no source on the battlefield)"
            }
            // CR 107.3b — casting an {X} spell without paying its mana cost leaves 0 as the only
            // legal choice for X. The enumerator never asks for X on this variant; a client that
            // announces one anyway is refused rather than handed a free X.
            if ((action.xValue ?: 0) > 0 && cardComponent.manaCost.hasX) {
                return "X must be 0 when casting a spell without paying its mana cost"
            }
        }
        val playForFree = zoneResolver.hasPlayWithoutPayingCost(state, action.playerId, action.cardId) ||
            action.useWithoutPayingManaCost
        // The engine, not the client, decides what a convoke/delve/improvise choice is worth: every
        // chosen permanent or card must be one the payment could actually use, or the cost stage
        // would price a payment `execute` then silently declines to apply. A free cast has no
        // generic to pay, so `execute` ignores tap-for-generic permanents on one; validation ignores
        // them the same way rather than rejecting a cast whose taps simply do nothing.
        val alternativePayment = action.alternativePayment
            ?.let { if (playForFree) it.copy(tapForGenericPermanents = emptySet()) else it }
        if (alternativePayment != null && !alternativePayment.isEmpty && cardDef != null) {
            val waterbendCap = castCostTotaller.spellWaterbendAmount(cardDef, action) +
                castCostTotaller.fixedAltWaterbendAmount(state, action, playForFree)
            val tapForGeneric = when {
                waterbendCap > 0 -> TapForGeneric.WATERBEND
                grantedKeywordResolver.hasKeyword(state, action.playerId, cardDef, Keyword.IMPROVISE) -> TapForGeneric.IMPROVISE
                else -> null
            }
            alternativePaymentHandler.validateForSpell(
                state, alternativePayment, action.playerId, cardDef, action.cardId, tapForGeneric
            )?.let { return it }
        }
        val computedCost = castCostTotaller.validationCost(
            state, action, cardDef, cardComponent, playForFree,
            castingFromCommandZone = source.route == CastSourceRoute.COMMANDER,
        ) ?: return "No alternative casting cost available"
        return castCostPayer.validateManaPayment(state, action, computedCost.cost, computedCost.paymentXValue)
    }

    /**
     * The chosen targets (CR 601.2c), against the requirements of the face and modes being cast —
     * the aura target included, and each spliced card's own after the spell's.
     */
    private fun validateTargets(state: GameState, action: CastSpell, cardDef: CardDefinition?, source: CastSource): String? {
        if (cardDef == null) return null
        val transformedFace = source.transformedFace
        // Adventure / split face cast (CR 715 / 709) — read targets from the face's script. A
        // disturb cast reads the back face's script instead (CR 712.8c): the Innistrad disturb
        // cycle's Aura backs choose what to enchant as the spell is cast.
        val faceScript = action.faceIndex?.let { cardDef.cardFaces.getOrNull(it)?.script }
            ?: transformedFace?.script
        val effectiveScript = faceScript ?: cardDef.script
        val modalEffect = effectiveScript.spellEffect as? ModalEffect
        // A choose-N modal cast that arrives with modes chosen but targets deferred (the
        // single-panel client mode selector submits `chosenModes` only) is target-validated later by
        // the cast-time per-mode target pause in execute(); skip the top-level target check here so
        // the deferred-targets action isn't rejected.
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
            // Cleave (CR 702.148): removing bracketed text can change the legal target set (e.g.
            // Fierce Retribution's "target [attacking] creature" → "target creature").
            cardDef.script.cleaveTargetRequirements
        } else {
            effectiveScript.targetRequirements
        }
        // Read through text-changing effects in force (CR 613.1c): the spell exists from 601.2a,
        // before its targets are chosen, so the enumerator offered targets against the changed text.
        val castText = TextChanges.forSpell(state, action.cardId)
        val targetRequirements = buildList {
            addAll(baseTargetReqs)
            // The cast-time choice: Dream Leash narrows it to a tapped permanent. The stack captures
            // the plain enchant restriction instead (see execute()), so 608.2b doesn't re-check it.
            (transformedFace ?: cardDef).script.castAuraTarget?.let { add(it) }
            // Splice (CR 702.47d): targets for the added text are chosen normally, as part of casting
            // this spell. They sit after the main spell's own requirements, so the flat target list
            // splits into the main slice followed by one slice per spliced card.
            addAll(SpliceCasts.targetRequirementsFor(state, action.splicedCardIds, cardRegistry))
        }.map { req -> castText?.let { req.applyTextReplacement(it) } ?: req }
        if (targetRequirements.isEmpty()) return null
        // Reject casting if spell requires targets but none were provided
        if (action.targets.isEmpty() && targetRequirements.sumOf { it.effectiveMinCount } > 0) {
            return "No valid targets available"
        }
        return targetValidator.validateTargets(
            state,
            action.targets,
            targetRequirements,
            action.playerId,
            sourceColors = (transformedFace ?: cardDef).colors,
            sourceSubtypes = (transformedFace ?: cardDef).typeLine.subtypes.map { it.value }.toSet(),
            sourceId = action.cardId,
            xValue = action.xValue,
            targetingSourceType = TargetingSourceType.SPELL
        )
    }

    /**
     * A divided-damage spell aimed at more than one target (CR 601.2d): the distribution names
     * exactly the chosen targets, sums to the spell's damage, and gives each at least 1. The kicked
     * or cleaved effect is the one divided when that variant is cast.
     */
    private fun validateDamageDistribution(action: CastSpell, cardDef: CardDefinition?): String? {
        val spellEffect = if (action.declaredCostSlot != null && cardDef?.script?.kickerSpellEffect != null) {
            cardDef.script.kickerSpellEffect
        } else if (cardDef != null && isCleaveCast(action, cardDef) && cardDef.script.cleaveSpellEffect != null) {
            cardDef.script.cleaveSpellEffect
        } else {
            cardDef?.script?.spellEffect
        }
        if (spellEffect !is DividedDamageEffect || action.targets.size <= 1) return null
        val distribution = action.damageDistribution
            ?: return "Damage distribution required for this spell when targeting multiple creatures"
        if (distribution.keys != action.targets.map { it.toEntityId() }.toSet()) {
            return "Damage distribution targets must match chosen targets"
        }
        val totalDistributed = distribution.values.sum()
        if (totalDistributed != spellEffect.totalDamage) {
            return "Total distributed damage ($totalDistributed) must equal ${spellEffect.totalDamage}"
        }
        // Each target gets at least 1 damage (CR 601.2d)
        val minPerTarget = 1
        if (distribution.values.any { it < minPerTarget }) {
            return "Each target must receive at least $minPerTarget damage"
        }
        return null
    }

    /**
     * The caster can afford any additional life cost opponents' permanents impose for what the
     * spell targets (ModifySpellCost + OpponentsCastTargeting + IncreaseLife — Terror of the Peaks:
     * "Spells your opponents cast that target this creature cost an additional 3 life to cast.").
     */
    private fun validateTargetLifeTaxes(state: GameState, action: CastSpell): String? {
        if (action.targets.isEmpty()) return null
        val additionalLifeCost = costCalculator.calculateAdditionalLifeCost(state, action.playerId, action.targets)
        if (additionalLifeCost > 0 && state.lifeTotal(action.playerId) < additionalLifeCost) { // CR 810.9a — team's shared total
            return "Not enough life to pay additional life cost ($additionalLifeCost life required)"
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
    internal fun effectiveModalChooseCounts(
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
}
