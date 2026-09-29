package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.handlers.effects.permanent.types.buildCardComponentForDfcFace
import com.wingedsheep.engine.handlers.effects.permanent.types.dfcBackFaceManaValue
import com.wingedsheep.engine.handlers.effects.permanent.types.withFaceIntrinsicComponents
import com.wingedsheep.engine.mechanics.SpliceCasts
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.FACE_DOWN_DISPLAY_NAME
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ObjectRef
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.HasMorphAbilityComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.permissions.consumeSingleUseMayPlayFor
import com.wingedsheep.engine.state.permissions.removeMayPlayPermissionsForCard
import com.wingedsheep.engine.view.EventPresentationFactory
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Puts a *cast* spell on the stack (CR 601.2): moves the card from the zone it was cast from, applies
 * the face it was cast as, records every cast-time choice on its [SpellOnStackComponent], consumes
 * the one-shot permissions that let it be cast, and emits the cast, crime and "becomes the target"
 * events.
 */
internal class SpellCaster(
    private val cardRegistry: CardRegistry,
    private val staticAbilityHandler: StaticAbilityHandler,
    private val eventPresentationFactory: EventPresentationFactory
) {
    /**
     * Put a spell on the stack.
     *
     * @param castFaceDown If true, cast as a face-down 2/2 creature (morph). The spell
     *                     will resolve as a face-down creature with FaceDownComponent
     *                     and MorphDataComponent.
     * @param castTransformed If true, the card goes on the stack **back face up** (CR 712.8c) —
     *                     disturb (CR 702.146). The face swap happens here, before the push, so
     *                     every downstream read (resolution, targeting, the client view) sees the
     *                     back face's characteristics without a special case.
     * @param damageDistribution Pre-chosen damage distribution for DividedDamageEffect spells
     *
     * Runs the tail of the casting procedure (CR 601.2a, 601.2i): the card leaves the zone it was
     * cast from, turns to the face it was cast as, and becomes a spell object carrying every
     * cast-time choice; the cast is then committed (commander tax, consumed permissions) and
     * announced (cast, crime, targeting and permission-rider events).
     */
    fun castSpell(
        state: GameState,
        cardId: EntityId,
        casterId: EntityId,
        targets: List<ChosenTarget> = emptyList(),
        xValue: Int? = null,
        sacrificedPermanents: List<EntitySnapshot> = emptyList(),
        castFaceDown: Boolean = false,
        castTransformed: Boolean = false,
        damageDistribution: Map<EntityId, Int>? = null,
        targetRequirements: List<TargetRequirement> = emptyList(),
        chosenCreatureType: String? = null,
        exiledCardCount: Int = 0,
        additionalCostBlightAmount: Int = 0,
        additionalCostPayXLifeAmount: Int? = null,
        declaredCostSlot: ChoiceSlot? = null,
        additionalCostChoices: Map<ChoiceSlot, Int> = emptyMap(),
        wasBlightPaid: Boolean = false,
        wasWaterbendPaid: Boolean = false,
        additionalEntryCounters: com.wingedsheep.engine.state.components.stack.AdditionalEntryCounters? = null,
        giftRecipient: EntityId? = null,
        wasWarped: Boolean = false,
        wasDashed: Boolean = false,
        wasEvoked: Boolean = false,
        wasImpending: Boolean = false,
        wasCleaved: Boolean = false,
        wasSneaked: Boolean = false,
        sneakAttackDefenderId: EntityId? = null,
        wasWebSlung: Boolean = false,
        webSlungReturnedManaValue: Int = 0,
        wasMayhem: Boolean = false,
        chosenModes: List<Int> = emptyList(),
        modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),
        modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),
        modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap(),
        /** Card-definition names spliced onto this spell, in splice order (CR 702.47a). */
        splicedCardNames: List<String> = emptyList(),
        totalManaSpent: Int = 0,
        beheldCards: List<EntityId> = emptyList(),
        convokedCreatures: Map<EntityId, Long> = emptyMap(),
        discardedAsCostCards: List<EntityId> = emptyList(),
        exiledAsCostCards: List<EntityId> = emptyList(),
        exiledAsCostSnapshots: List<EntitySnapshot> = emptyList(),
        chosenEntitySnapshots: List<EntitySnapshot> = emptyList(),
        manaSpentWhite: Int = 0,
        manaSpentBlue: Int = 0,
        manaSpentBlack: Int = 0,
        manaSpentRed: Int = 0,
        manaSpentGreen: Int = 0,
        manaSpentColorless: Int = 0,
        manaSpentOnXByColor: Map<Color, Int> = emptyMap(),
        faceIndex: Int? = null,
        spentManaProvenance: com.wingedsheep.engine.mechanics.mana.SpentManaProvenance =
            com.wingedsheep.engine.mechanics.mana.SpentManaProvenance(),
        castTimeFlags: Set<String> = emptySet(),
        alternativeCost: com.wingedsheep.engine.core.AlternativeCostType? = null,
        // Payment can tap or remove the source that made the origin visible. This immutable
        // input is consulted only while capturing the event; the event retains no GameState.
        castOriginState: GameState = state
    ): ExecutionResult {
        val container = state.getEntity(cardId)
            ?: return ExecutionResult.error(state, "Card not found: $cardId")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card: $cardId")

        // Determine which zone the spell is being cast from (before removal)
        val castFromZone = findCastFromZone(state, cardId, casterId)

        // Remove from current zone (typically hand)
        var newState = removeFromCurrentZone(state, cardId, casterId)
        if (castFaceDown) {
            newState = clearRevealedMorphsInHand(newState, casterId)
        }

        // Cast transformed (CR 712.8c, disturb; CR 712.11b, a modal DFC's permanent back face): flip
        // the card to its back face *before* it becomes a spell, so the stack object — and the
        // permanent it resolves into — has only the back face's characteristics. The front-face
        // CardComponent is stashed on the DoubleFacedComponent so Rule 712.8a restores it if the
        // spell is countered or the permanent later leaves the battlefield (ZoneTransitionService
        // does that restore, and it deliberately exempts the stack).
        val transformedFrontDef = if (castTransformed) {
            cardRegistry.getCard(cardComponent.cardDefinitionId)
        } else null
        val transformedBackDef = transformedFrontDef?.backFace
        // CR 712.8c: a *nonmodal* transformed spell keeps the front face's mana value, which
        // `cardComponent` still holds. CR 712.8f gives a modal one the face that's up, so the back's
        // own cost stands and no override is needed. Null when this isn't a transformed cast at all.
        val backFaceManaValue = transformedBackDef
            ?.let { dfcBackFaceManaValue(transformedFrontDef, cardComponent.manaValue) }
        if (transformedFrontDef != null && transformedBackDef != null) {
            newState = turnToBackFace(
                newState, cardId, cardComponent, transformedFrontDef, transformedBackDef, backFaceManaValue
            )
        }

        // The spell's mana value (CR 202.3), reported by the SpellCastEvent below — which feeds
        // ContextPropertyKey.TRIGGERING_SPELL_MANA_VALUE and every "a spell with mana value N"
        // payoff. It is the same number the stack object now carries, so it comes from the same
        // decision: a disturb cast keeps the front's (CR 712.8c, `backFaceManaValue` non-null),
        // while a modal DFC cast as its back face has that face's own — The Sensational She-Hulk is
        // 6, not Jennifer Walters' 2. CastSpellHandler mirrors this for its CastSpellRecord.
        val spellManaValue = backFaceManaValue
            ?: transformedBackDef?.manaCost?.cmc
            ?: cardComponent.manaValue

        // CR 601.2b — X is announced as the spell is cast; see [bindAnnouncedX].
        val boundXValue = bindAnnouncedX(xValue, faceIndex, cardComponent, transformedBackDef)

        // Build the flat target union for choose-N modal spells (Rule 700.2 / 601.2c).
        // TargetsComponent holds the union so existing target-arrow rendering and resolution-time
        // re-validation keep working; per-mode breakdown lives on SpellOnStackComponent.
        val effectiveTargets = if (modeTargetsOrdered.isNotEmpty()) {
            modeTargetsOrdered.flatten()
        } else {
            targets
        }
        val effectiveTargetRequirements = if (modeTargetRequirements.isNotEmpty() && targetRequirements.isEmpty()) {
            chosenModes.flatMap { modeTargetRequirements[it] ?: emptyList() }
        } else {
            targetRequirements
        }

        // Splice (CR 702.47d): the cast's flat target list runs main-spell targets first, then one
        // group per spliced card in splice order. Slice the tail off now so resolution can hand each
        // spliced card its own targets — its `ContextTarget(0)` means its own first target, not the
        // main spell's. TargetsComponent keeps the flat union, so target arrows and the 608.2b
        // re-validation pass keep working unchanged.
        val splicedTargetsOrdered: List<List<ChosenTarget>> = if (splicedCardNames.isEmpty()) {
            emptyList()
        } else {
            SpliceCasts.sliceSplicedTargets(effectiveTargets, splicedCardNames, cardRegistry)
        }

        // Add spell components
        val spellOnStack = SpellOnStackComponent(
            casterId = casterId,
            xValue = boundXValue,
            declaredCostSlot = declaredCostSlot,
            additionalCostChoices = additionalCostChoices,
            wasBlightPaid = wasBlightPaid,
            wasWaterbendPaid = wasWaterbendPaid,
            additionalEntryCounters = additionalEntryCounters,
            giftRecipient = giftRecipient,
            splicedCardNames = splicedCardNames,
            splicedTargetsOrdered = splicedTargetsOrdered,
            chosenModes = chosenModes,
            modeTargetsOrdered = modeTargetsOrdered,
            modeTargetRequirements = modeTargetRequirements,
            modeDamageDistribution = modeDamageDistribution,
            sacrificedPermanents = sacrificedPermanents,
            castFaceDown = castFaceDown,
            damageDistribution = damageDistribution,
            chosenCreatureType = chosenCreatureType,
            exiledCardCount = exiledCardCount,
            additionalCostBlightAmount = additionalCostBlightAmount,
            additionalCostPayXLifeAmount = additionalCostPayXLifeAmount,
            castFromZone = castFromZone,
            alternativeCost = alternativeCost,
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
            beheldCards = beheldCards,
            convokedCreatures = convokedCreatures,
            discardedAsCostCards = discardedAsCostCards,
            exiledAsCostCards = exiledAsCostCards,
            exiledAsCostSnapshots = exiledAsCostSnapshots,
            chosenEntitySnapshots = chosenEntitySnapshots,
            manaSpentWhite = manaSpentWhite,
            manaSpentBlue = manaSpentBlue,
            manaSpentBlack = manaSpentBlack,
            manaSpentRed = manaSpentRed,
            manaSpentGreen = manaSpentGreen,
            manaSpentColorless = manaSpentColorless,
            manaSpentBySubtype = spentManaProvenance.bySubtype,
            manaSpentByCardType = spentManaProvenance.byCardType,
            manaSpentOnXByColor = manaSpentOnXByColor,
            faceIndex = faceIndex,
            castTimeFlags = castTimeFlags
        )
        newState = newState.updateEntity(cardId) { c ->
            withSpellComponents(
                state, c, cardComponent, spellOnStack, effectiveTargets, effectiveTargetRequirements, castFaceDown
            )
        }

        // Commander tax bookkeeping (CR 903.8): increment castsFromCommandZone on cast-commit so
        // that countered commanders still pay an escalating tax next time. Done after payment is
        // complete (the handler has already settled the mana cost) but before the spell is pushed
        // onto the stack — i.e. the cast is "committed" the moment the spell becomes a real
        // game object on the stack.
        if (castFromZone == Zone.COMMAND) {
            newState = incrementCommanderTax(newState, cardId)
        }

        val objectBeforeCast = state.objectRef(cardId)
        // Push to stack and reset priority passes (new stack item requires fresh round of passes)
        newState = newState.pushToStack(cardId)
            .copy(priorityPassedBy = emptySet())
        val objectOnStack = newState.objectRef(cardId)

        newState = consumeCastPermissions(newState, cardId, casterId, castFaceDown)
        if (castFromZone == Zone.EXILE && objectBeforeCast != null) {
            newState = endGrantsUntilCastFromExile(newState, objectBeforeCast)
        }
        newState = unprepareSourceOfPrepareCopy(state, newState, cardId)

        // A cast-transformed spell is on the stack back face up (CR 712.8c), so its *name* is the
        // back face's — `cardComponent` was captured before the face swap above and still holds the
        // front face's. The log used to announce a disturb cast as "cast Covetous Castaway" while
        // the stack showed Ghostly Castigator. The mana value is `spellManaValue`, resolved with the
        // face swap above because the two routes differ (CR 712.8c vs 712.8f); every card-definition
        // lookup keeps using `cardComponent.cardDefinitionId`, which addresses the whole card.
        val spellName = if (castTransformed) {
            newState.getEntity(cardId)?.get<CardComponent>()?.name ?: cardComponent.name
        } else {
            cardComponent.name
        }
        // Preserve SpellCastEvent.cardName's historical public-name contract. The trusted
        // presentation snapshot below separately retains semantic identity and private audiences.
        val eventName = if (castFaceDown) FACE_DOWN_DISPLAY_NAME else spellName
        val targetNames = castTargetNames(newState, effectiveTargets, casterId)
        val reportedChosenModesCount = chosenModesCountForTriggers(cardComponent, chosenModes)

        val events = mutableListOf<GameEvent>(
            ZoneChangeEvent(cardId, eventName, castFromZone, Zone.STACK, cardComponent.ownerId ?: casterId,
                oldObject = objectBeforeCast, newObject = objectOnStack),
            SpellCastEvent(
                spellEntityId = cardId,
                cardName = eventName,
                casterId = casterId,
                targetNames = targetNames,
                xValue = boundXValue,
                cardPresentation = eventPresentationFactory.castSpellIdentity(
                    beforeCast = castOriginState,
                    onStack = newState,
                    castFromZone = castFromZone,
                    entityId = cardId,
                    semanticName = spellName,
                ),
                declaredCostSlot = declaredCostSlot,
                totalManaSpent = totalManaSpent,
                distinctColorsSpent =
                    com.wingedsheep.engine.handlers.ManaSpentReader.distinctColorsSpent(newState, cardId),
                spentManaSubtypes = spentManaProvenance.spentSubtypes,
                spentManaSourceIds = spentManaProvenance.sourceIds,
                chosenModesCount = reportedChosenModesCount,
                manaValue = spellManaValue,
                castFromZone = castFromZone,
                alternativeCost = alternativeCost,
                // Last-known names of the bodies the cost ate, so an emerge cast's reduced
                // `totalManaSpent` reads as a consequence rather than a mystery (CR 702.119a).
                sacrificedAsCostNames = sacrificedPermanents.mapNotNull { it.name }
            )
        )

        newState = announceTargets(newState, cardId, casterId, spellName, effectiveTargets, events)
        addPermissionRiderEvents(state, castFromZone, cardId, casterId, events)

        return ExecutionResult.success(
            newState.tick(),
            events
        )
    }

    /**
     * Cast transformed (CR 712.8c, disturb; CR 712.11b, a modal DFC's permanent back face): turn the
     * card to [transformedBackDef] before it becomes a spell, stashing the front face so CR 712.8a
     * can restore it.
     */
    private fun turnToBackFace(
        state: GameState,
        cardId: EntityId,
        cardComponent: CardComponent,
        transformedFrontDef: com.wingedsheep.sdk.model.CardDefinition,
        transformedBackDef: com.wingedsheep.sdk.model.CardDefinition,
        backFaceManaValue: Int?
    ): GameState =
        state.updateEntity(cardId) { c ->
            var updated = c
                .with(buildCardComponentForDfcFace(cardComponent, transformedBackDef, backFaceManaValue))
                .with(
                    DoubleFacedComponent(
                        frontCardDefinitionId = transformedFrontDef.name,
                        backCardDefinitionId = transformedBackDef.name,
                        currentFace = DoubleFacedComponent.Face.BACK,
                        frontFaceCard = cardComponent
                    )
                )
                .without<ContinuousEffectSourceComponent>()
                .without<ReplacementEffectSourceComponent>()
            // Register the back face's static and replacement effects (the "if this would
            // be put into a graveyard from anywhere, exile it instead" clause the disturb
            // cycle prints on its back faces is one of these, and it must function from the
            // moment the card is a back-face object — CR 614.12).
            updated = staticAbilityHandler.addContinuousEffectComponent(updated, transformedBackDef)
            updated = staticAbilityHandler.addReplacementEffectComponent(updated, transformedBackDef)
            withFaceIntrinsicComponents(updated, transformedBackDef)
        }

    /** The X the spell carries onto the stack. */
    private fun bindAnnouncedX(
        xValue: Int?,
        faceIndex: Int?,
        cardComponent: CardComponent,
        transformedBackDef: com.wingedsheep.sdk.model.CardDefinition?
    ): Int? {
        // CR 601.2b — a spell with `{X}` in its cost has X *announced as it is cast*; there is no
        // such thing as a spell on the stack whose X is undetermined. A caller that announced
        // nothing (the AI's CastSpell carries no xValue) paid nothing for X, so X is 0. For the
        // other caller — a synthesized cast that pays no mana cost at all — CR 107.3b is directly
        // on point: "the only legal choice for X is 0."
        //
        // Binding it here rather than leaving null is load-bearing, not cosmetic: the resolution-time
        // `CardPredicate.ManaValueAtMostX` fails *open* on an unbound X — deliberately, so an X spell
        // is still offered during legal-action enumeration, which runs before X is chosen. Left null
        // all the way to resolution, "each creature with mana value X or less" matches *every*
        // creature, and Day of Black Sun cast for X=0 wipes the board. It is also what puts the
        // "(X=0)" in the game log's cast line, which is otherwise silently absent.
        return xValue ?: run {
            val castCost = faceIndex
                ?.let { cardRegistry.getCard(cardComponent.cardDefinitionId)?.cardFaces?.getOrNull(it)?.manaCost }
                ?: transformedBackDef?.manaCost
                ?: cardComponent.manaCost
            if (castCost.hasX) 0 else null
        }
    }

    /**
     * The stack object's components: [spellOnStack], its captured targets, and the turn-up data of a
     * card castable face down.
     */
    private fun withSpellComponents(
        state: GameState,
        c: ComponentContainer,
        cardComponent: CardComponent,
        spellOnStack: SpellOnStackComponent,
        effectiveTargets: List<ChosenTarget>,
        effectiveTargetRequirements: List<TargetRequirement>,
        castFaceDown: Boolean
    ): ComponentContainer {
        var updated = c.with(spellOnStack)
        if (effectiveTargets.isNotEmpty()) {
            updated = updated.with(
                TargetsComponent.capture(state, effectiveTargets, effectiveTargetRequirements)
            )
        }
        // Add turn-up data for cards castable face down (needed for face-down casting and
        // for effects like Backslide that target "creature with a morph ability"). The mode
        // decides which keyword's cost applies — FaceDownTurnUp is the single place that
        // knows that mapping.
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        val castFaceDownMode = faceDownCastMode(cardDef)
        if (castFaceDownMode != null) {
            FaceDownTurnUp.dataFor(cardDef, cardComponent.cardDefinitionId, castFaceDownMode)
                ?.let { updated = updated.with(it) }
        }
        if (castFaceDown) {
            updated = updated.without<RevealedToComponent>()
        }
        return updated
    }

    private fun incrementCommanderTax(state: GameState, cardId: EntityId): GameState =
        state.updateEntity(cardId) { c ->
            val commander = c.get<com.wingedsheep.engine.state.components.identity.CommanderComponent>()
            if (commander != null) {
                c.with(commander.copy(castsFromCommandZone = commander.castsFromCommandZone + 1))
            } else {
                c
            }
        }

    private fun consumeCastPermissions(
        state: GameState,
        cardId: EntityId,
        casterId: EntityId,
        castFaceDown: Boolean
    ): GameState {
        // "Cast a spell from among those cards" (Chandra, Hope's Beacon): a single-use grant is
        // spent for its whole group by this cast — permanent or not, and before the per-card
        // pruning below, which would otherwise leave the rest of the group castable.
        var newState = state.consumeSingleUseMayPlayFor(cardId, casterId)
        // Consume one-shot free-cast permissions used to play this spell. If the
        // spell is later countered or fizzles and AfterResolveDestinationComponent sends
        // it back to exile, the permission must already be gone — otherwise the
        // controller could re-cast the same card repeatedly (e.g. Daring Waverider's
        // free cast resurfacing every time the granted spell is countered).
        // "Permanent" permissions (e.g. Kheru Spellsnatcher's "for as long as it
        // remains exiled" grant) are left intact.
        newState = newState.updateEntity(cardId) { c ->
            var updated = c
            val payCost = c.get<PlayWithoutPayingCostComponent>()
            if (payCost != null && !payCost.permanent) {
                updated = updated.without<PlayWithoutPayingCostComponent>()
            }
            updated = updated.without<com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent>()
            updated = updated.without<com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent>()
            // The madness offer (CR 702.35a) is spent the moment the card is cast; drop the marker
            // with the fixed madness cost it published so the two never outlive each other.
            updated = updated.without<com.wingedsheep.engine.state.components.identity.MadnessExiledComponent>()
            // A card cast face up is revealed as it goes on the stack. Foretold cards (and any
            // other hidden-in-exile card) carry a FaceDownComponent for opponent masking while
            // exiled; strip it here so the spell isn't masked on the stack (CR 702.143 — casting a
            // foretold card reveals it). Morph/manifest casts (castFaceDown) re-add it on resolve.
            if (!castFaceDown) {
                updated = updated.without<FaceDownComponent>()
            }
            updated
        }
        // Drop this card from one-shot may-play grants. Permanent grants survive
        // (e.g. Adventure / Warp / Possibility Technician) and are stripped on resolve.
        // Multi-card permissions (Etali / Narset / Mind's Desire) keep authorising the
        // remaining cards — only the cast card loses its grant.
        newState = newState.copy(
            mayPlayPermissions = newState.mayPlayPermissions.mapNotNull { permission ->
                if (permission.permanent || cardId !in permission.cardIds) {
                    permission
                } else {
                    val remaining = permission.cardIds - cardId
                    if (remaining.isEmpty()) null else permission.copy(cardIds = remaining)
                }
            }
        )
        return newState
    }

    /**
     * End every grant lasting "until this card is cast from exile" (Emrakul, the Exigent Doom's
     * "{T}: Add {C}{C}") whose source was exactly [castObject] — the exile object being cast now.
     * Matching the object, not just the entity, is what keeps a card that left exile some other way
     * and came back from ending a grant that named its earlier incarnation (CR 400.7).
     */
    private fun endGrantsUntilCastFromExile(state: GameState, castObject: ObjectRef): GameState {
        fun ends(grant: com.wingedsheep.engine.event.GrantedActivatedAbility) =
            grant.duration == Duration.UntilSourceCastFromExile && grant.sourceObject == castObject
        if (state.grantedActivatedAbilities.none(::ends)) return state
        return state.copy(grantedActivatedAbilities = state.grantedActivatedAbilities.filterNot(::ends))
    }

    /** [state] is the pre-cast state; [current] the state the source is unprepared in. */
    private fun unprepareSourceOfPrepareCopy(state: GameState, current: GameState, cardId: EntityId): GameState {
        var newState = current
        // Prepared (Secrets of Strixhaven): casting the prepare-spell copy unprepares its source
        // creature. Strip the source's PreparedComponent and consume the (permanent) cast-from-exile
        // permission for this copy so it can't be cast again — the copy itself is on the stack and
        // ceases to exist on resolution (CopyOfComponent), or the source's leave-battlefield cleanup
        // removes it if it never resolves.
        val prepareCopyComp = state.getEntity(cardId)
            ?.get<com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent>()
        if (prepareCopyComp != null) {
            newState = newState.updateEntity(prepareCopyComp.sourceId) { c ->
                c.without<com.wingedsheep.engine.state.components.battlefield.PreparedComponent>()
            }
            newState = newState.removeMayPlayPermissionsForCard(cardId)
        }
        return newState
    }

    /** Target names for the cast event log. */
    private fun castTargetNames(
        newState: GameState,
        effectiveTargets: List<ChosenTarget>,
        casterId: EntityId
    ): List<String> =
        effectiveTargets.mapNotNull { target ->
            when (target) {
                // A face-down permanent is no more nameable as a *target* than as a source —
                // "cast Igneous Inspiration targeting Aurelia's Vindicator" gave away a disguised
                // creature just as completely as the enters line did.
                is ChosenTarget.Permanent -> newState.getEntity(target.entityId)?.get<CardComponent>()?.name
                    ?.let { nameVisibleToAll(newState, target.entityId, it) }
                is ChosenTarget.Player -> if (target.playerId == casterId) "themselves" else "opponent"
                is ChosenTarget.Spell -> newState.getEntity(target.spellEntityId)?.get<CardComponent>()?.name
                    ?.let { nameVisibleToAll(newState, target.spellEntityId, it) }
                    ?: "spell"
                // A card lying face down in exile is hidden too, and reads as "Face-down card"
                // rather than "Face-down creature" — it has no characteristics to show.
                is ChosenTarget.Card -> newState.getEntity(target.cardId)?.get<CardComponent>()?.name
                    ?.let { nameVisibleToAll(newState, target.cardId, it) }
            }
        }

    private fun chosenModesCountForTriggers(cardComponent: CardComponent, chosenModes: List<Int>): Int {
        // Only count modes for triggers (Riku of Many Paths' "Whenever you cast a
        // modal spell" → IsModal predicate + MODES_CHOSEN_ON_TRIGGERING_SPELL) when
        // the spell's effect is a *true* modal — printed "Choose one — • X • Y"
        // wording. Mechanics like Gift use [ModalEffect] as an implementation
        // shortcut for a yes/no cost choice but are not modal in MTG terms; those
        // construct via `Patterns.Mechanic.giftSpell` (or set `countsAsModalSpell =
        // false` directly), which zeroes the count here.
        val countsAsModalForTriggers = run {
            val script = cardRegistry.getCard(cardComponent.cardDefinitionId)?.script
            val modal = script?.spellEffect as? com.wingedsheep.sdk.scripting.effects.ModalEffect
            modal?.countsAsModalSpell ?: false
        }
        return if (countsAsModalForTriggers) chosenModes.size else 0
    }

    /** Crime, "chooses targets" and "becomes the target" events for the spell's targets (CR 601.2c). */
    private fun announceTargets(
        state: GameState,
        cardId: EntityId,
        casterId: EntityId,
        spellName: String,
        effectiveTargets: List<ChosenTarget>,
        events: MutableList<GameEvent>
    ): GameState {
        var newState = state
        // Crime detection (CR Outlaws of Thunder Junction). Emit at most once per cast,
        // regardless of how many opponent-controlled targets the spell chose.
        if (CrimeDetector.isCrime(newState, casterId, effectiveTargets)) {
            events.add(CommitCrimeEvent(casterId, cardId, spellName))
            newState = StackPlacement.recordCrime(newState, casterId)
        }

        // "Whenever a player chooses one or more targets" (Psychic Battle). Emit once per cast
        // when the spell chose at least one target.
        if (effectiveTargets.isNotEmpty()) {
            events.add(TargetsChosenEvent(casterId, cardId, spellName))
        }

        // Emit BecomesTargetEvent for each permanent, spell, or player target (Rule 601.2c)
        // Also track targeting for Valiant ("first time each turn")
        for (target in effectiveTargets) {
            newState = StackPlacement.emitBecomesTarget(newState, target, cardId, casterId, events, sourceIsSpell = true)
        }
        return newState
    }

    private fun addPermissionRiderEvents(
        state: GameState,
        castFromZone: Zone?,
        cardId: EntityId,
        casterId: EntityId,
        events: MutableList<GameEvent>
    ) {
        // "When you play a card this way, …" rider (Fires of Mount Doom). If this spell was cast
        // from exile via a may-play permission that carries a rider, emit the linked event so the
        // rider's delayed triggered ability fires on the stack. Read off the pre-removal [state] —
        // the permission survives until the spell resolves, but its cardIds is most reliably
        // inspected before any of this method's zone churn.
        if (castFromZone == Zone.EXILE) {
            for (permission in state.mayPlayPermissions) {
                if (permission.riderLinkId != null &&
                    permission.controllerId == casterId &&
                    cardId in permission.cardIds &&
                    permission.sourceId != null
                ) {
                    events.add(
                        com.wingedsheep.engine.core.CardPlayedFromPermissionEvent(
                            cardId = cardId,
                            controllerId = casterId,
                            sourceId = permission.sourceId,
                            linkId = permission.riderLinkId
                        )
                    )
                }
            }
        }
    }

    /**
     * Determine which zone a card is being cast from. Called internally by [castSpell] (before the
     * card is removed from its origin zone) and by `CastSpellHandler` to stamp `castFromZone` on the
     * turn's [com.wingedsheep.engine.state.CastSpellRecord]; both invoke it while the card is still
     * in its origin zone so they agree on the result.
     */
    internal fun findCastFromZone(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId
    ): Zone? {
        val zones = listOf(Zone.HAND, Zone.GRAVEYARD, Zone.LIBRARY, Zone.COMMAND)
        for (zone in zones) {
            if (cardId in state.getZone(ZoneKey(playerId, zone))) {
                return zone
            }
        }
        // Check all players' exile and graveyard zones — the caster isn't always the owner:
        // Villainous Wealth exiles from an opponent's library, Jetsam and The Great Work cast out of
        // another player's graveyard. It is still a graveyard cast (flashback-style exile riders,
        // "cast from a graveyard" triggers) even though the graveyard isn't the caster's.
        for (pid in state.turnOrder) {
            if (cardId in state.getZone(ZoneKey(pid, Zone.EXILE))) {
                return Zone.EXILE
            }
            if (cardId in state.getZone(ZoneKey(pid, Zone.GRAVEYARD))) {
                return Zone.GRAVEYARD
            }
        }
        return null
    }

    /**
     * Remove a card from its current zone (for casting).
     */
    private fun removeFromCurrentZone(
        state: GameState,
        cardId: EntityId,
        playerId: EntityId
    ): GameState {
        // Every zone below is owner-keyed, and the caster is not always the owner: Jetsam casts a
        // spell out of *each opponent's* graveyard, Sen Triplets out of an opponent's hand. Look in
        // the caster's own copy of the zone first (the overwhelmingly common case, and the one whose
        // semantics the special handling below was written for), then in every other player's. A
        // card left behind here would be on the stack and in a graveyard at the same time.
        fun ownerOf(zone: Zone): ZoneKey? =
            listOf(playerId).plus(state.turnOrder.filter { it != playerId })
                .map { ZoneKey(it, zone) }
                .firstOrNull { cardId in state.getZone(it) }

        // Try removing from hand first
        val handZone = ownerOf(Zone.HAND)
        if (handZone != null) {
            return state.removeFromZone(handZone, cardId)
        }

        // Also check graveyard (for flashback etc.)
        val graveyardZone = ownerOf(Zone.GRAVEYARD)
        if (graveyardZone != null) {
            // A static ability granted to the *card while it sat in the graveyard* — Case of the
            // Uneaten Feast's "creature cards in your graveyard gain 'You may cast this card from
            // your graveyard'" — ends the moment the card leaves that zone (CR 400.7: the spell,
            // and anything the card later becomes, is a new object). Dropping it here is what stops
            // a countered graveyard cast from being recastable off the same grant; the battlefield
            // exit in ZoneTransitionService covers the spell that does resolve. Every read of the
            // grant (CastSpellHandler's rider freeze, the once-per-turn source) happens against the
            // pre-cast state, so this prune can't strip a permission out from under its own cast.
            return state.removeFromZone(graveyardZone, cardId)
                .copy(
                    grantedStaticAbilities = state.grantedStaticAbilities
                        .filter { it.entityId != cardId }
                )
        }

        // Check all players' exile zones (cards may be in another player's exile,
        // e.g., Villainous Wealth exiles from opponent's library)
        for (pid in state.turnOrder) {
            val exileZone = ZoneKey(pid, Zone.EXILE)
            if (cardId in state.getZone(exileZone)) {
                // A suspended card cast out of exile is no longer suspended (CR 702.62) — drop
                // the marker so it doesn't ride along onto the resulting permanent (which reuses
                // this entity id). The exile-side countdown trigger is gated on time counters,
                // so a leftover marker would be inert, but this keeps the permanent clean.
                // The "which zone was this exiled from" stamp is only meaningful while the object
                // is in exile; this path reuses the entity id, so leaving it on would put an
                // ExiledFromZoneComponent on the resulting permanent.
                val removed = state.removeFromZone(exileZone, cardId)
                    .updateEntity(cardId) {
                        it.without<com.wingedsheep.engine.state.components.battlefield.SuspendedComponent>()
                            .without<com.wingedsheep.engine.state.components.identity.ExiledFromZoneComponent>()
                    }
                return com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                    .unlinkFromAllLinkedExiles(removed, cardId)
            }
        }

        // Check library (for Future Sight / play from top of library)
        val libraryZone = ownerOf(Zone.LIBRARY)
        if (libraryZone != null) {
            return state.removeFromZone(libraryZone, cardId)
        }

        // Check the command zone (Commander format casts).
        val commandZone = ownerOf(Zone.COMMAND)
        if (commandZone != null) {
            return state.removeFromZone(commandZone, cardId)
        }

        return state
    }

    /**
     * Which face-down mechanic lets [cardDef] be cast face down for {3} — morph (CR 702.37a) or
     * disguise (CR 702.168a) — or null when it can't be cast face down at all. Delegates to
     * [FaceDownTurnUp.castMode], which owns the keyword-to-mode mapping.
     */
    fun faceDownCastMode(cardDef: com.wingedsheep.sdk.model.CardDefinition?): FaceDownMode? =
        FaceDownTurnUp.castMode(cardDef)

    /**
     * Once a player casts a card face down, opponents can no longer know whether any previously
     * revealed card that could have been the one cast is still in that player's hand — which
     * covers every card castable face down, morph and disguise alike.
     */
    private fun clearRevealedMorphsInHand(state: GameState, playerId: EntityId): GameState {
        var newState = state
        for (handCardId in state.getZone(ZoneKey(playerId, Zone.HAND))) {
            val container = newState.getEntity(handCardId) ?: continue
            val castableFaceDown = container.has<HasMorphAbilityComponent>() ||
                faceDownCastMode(
                    container.get<CardComponent>()?.let { cardRegistry.getCard(it.cardDefinitionId) }
                ) != null
            if (!castableFaceDown) continue
            if (container.get<RevealedToComponent>() == null) continue

            newState = newState.updateEntity(handCardId) { c ->
                c.without<RevealedToComponent>()
            }
        }
        return newState
    }
}
