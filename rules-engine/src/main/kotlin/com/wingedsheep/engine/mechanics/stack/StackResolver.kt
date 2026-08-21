package com.wingedsheep.engine.mechanics.stack
import com.wingedsheep.sdk.dsl.Patterns

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PipelineState
import com.wingedsheep.engine.handlers.EffectHandler
import com.wingedsheep.engine.handlers.effects.composite.PreTargetedEffectContext
import com.wingedsheep.engine.handlers.effects.composite.processPreTargetedEffectQueue
import com.wingedsheep.engine.mechanics.ControllerGrants
import com.wingedsheep.engine.mechanics.FlashbackGrants
import com.wingedsheep.engine.mechanics.HarmonizeGrants
import com.wingedsheep.engine.mechanics.SpliceCasts
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.mechanics.daynight.DayNightService
import com.wingedsheep.engine.mechanics.layers.ContinuousEffectSourceComponent
import com.wingedsheep.engine.mechanics.layers.StaticAbilityHandler
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TargetedByControllerThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.CastFromHandComponent
import com.wingedsheep.engine.state.components.battlefield.WarpedComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.identity.CantBeCounteredComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.DoubleFacedComponent
import com.wingedsheep.engine.handlers.effects.FaceDownTurnUp
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.engine.state.components.identity.HasMorphAbilityComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.state.components.identity.AfterResolveDestinationComponent
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.state.permissions.removeMayPlayPermissionsForCard
import com.wingedsheep.sdk.scripting.conditions.SourcePlottedOnPriorTurn
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GrantCantBeCountered
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.event.DelayedTriggeredAbility
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.WarpExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveTrackedBattlefieldObjectEffect
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.engine.handlers.effects.EntersWithReplacements
import com.wingedsheep.engine.handlers.effects.permanent.types.buildCardComponentForDfcFace
import com.wingedsheep.engine.handlers.effects.permanent.types.dfcBackFaceManaValue
import com.wingedsheep.engine.handlers.effects.permanent.types.returnDfcFace
import com.wingedsheep.engine.handlers.effects.permanent.types.withDfcFaceSelfRedirects
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.mechanics.targeting.HexproofSuppression
import com.wingedsheep.engine.mechanics.targeting.PlayerTargetRestriction
import com.wingedsheep.engine.handlers.SourceTypeTargeting
import com.wingedsheep.engine.state.components.battlefield.CantBeTargetedByOpponentAbilitiesComponent
import com.wingedsheep.engine.state.components.battlefield.ReplacementEffectSourceComponent
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Manages the stack: casting spells, activating abilities, and resolution.
 *
 * Handles:
 * - Putting spells on the stack
 * - Putting triggered abilities on the stack
 * - Putting activated abilities on the stack
 * - Resolving the top item
 * - Target validation on resolution
 * - Countering spells
 */
class StackResolver(
    private val cardRegistry: CardRegistry,
    private val effectHandler: EffectHandler = EffectHandler(cardRegistry = cardRegistry),
    private val staticAbilityHandler: StaticAbilityHandler = StaticAbilityHandler(cardRegistry),
    private val predicateEvaluator: PredicateEvaluator = PredicateEvaluator()
) {

    /**
     * Re-validates a spliced card's own targets as the spell resolves (CR 608.2b via 702.47d): the
     * spliced text is skipped when its targets have become illegal, exactly as a modal spell's
     * pre-chosen mode is.
     */
    private val spliceTargetValidator = TargetValidator()

    /** Evaluates a triggered ability's intervening-"if" as it resolves (CR 603.4). */
    private val conditionEvaluator = com.wingedsheep.engine.handlers.ConditionEvaluator()

    /**
     * The spliced text of [spellComponent]'s spell as a drain queue (CR 702.47b) — one entry per
     * spliced card, in the caster's chosen order, each carrying its own target slice and requirements.
     *
     * A spliced card contributes its *rules text*, so what is queued is its `spellEffect`; a splice
     * card with no spell effect (nothing splice-able) simply drops out.
     */
    private fun buildSpliceEntries(spellComponent: SpellOnStackComponent): List<PreTargetedEffectEntry> =
        spellComponent.splicedCardNames.mapIndexedNotNull { index, name ->
            val splicedDef = cardRegistry.getCard(name) ?: return@mapIndexedNotNull null
            val effect = splicedDef.script.spellEffect ?: return@mapIndexedNotNull null
            PreTargetedEffectEntry(
                effect = effect,
                targets = spellComponent.splicedTargetsOrdered.getOrNull(index) ?: emptyList(),
                targetRequirements = splicedDef.script.targetRequirements
            )
        }

    // =========================================================================
    // Casting Spells
    // =========================================================================

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
        wasBlightPaid: Boolean = false,
        wasWaterbendPaid: Boolean = false,
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
        discardedAsCostCards: List<EntityId> = emptyList(),
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
        alternativeCost: com.wingedsheep.engine.core.AlternativeCostType? = null
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
            newState = newState.updateEntity(cardId) { c ->
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
                withDfcFaceSelfRedirects(updated, transformedBackDef)
            }
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
        val boundXValue = xValue ?: run {
            val castCost = faceIndex
                ?.let { cardRegistry.getCard(cardComponent.cardDefinitionId)?.cardFaces?.getOrNull(it)?.manaCost }
                ?: transformedBackDef?.manaCost
                ?: cardComponent.manaCost
            if (castCost.hasX) 0 else null
        }

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
        newState = newState.updateEntity(cardId) { c ->
            var updated = c.with(SpellOnStackComponent(
                casterId = casterId,
                xValue = boundXValue,
                declaredCostSlot = declaredCostSlot,
                wasBlightPaid = wasBlightPaid,
                wasWaterbendPaid = wasWaterbendPaid,
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
                discardedAsCostCards = discardedAsCostCards,
                chosenEntitySnapshots = chosenEntitySnapshots,
                manaSpentWhite = manaSpentWhite,
                manaSpentBlue = manaSpentBlue,
                manaSpentBlack = manaSpentBlack,
                manaSpentRed = manaSpentRed,
                manaSpentGreen = manaSpentGreen,
                manaSpentColorless = manaSpentColorless,
                manaSpentBySubtype = spentManaProvenance.bySubtype,
                manaSpentOnXByColor = manaSpentOnXByColor,
                faceIndex = faceIndex,
                castTimeFlags = castTimeFlags
            ))
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
            updated
        }

        // Commander tax bookkeeping (CR 903.8): increment castsFromCommandZone on cast-commit so
        // that countered commanders still pay an escalating tax next time. Done after payment is
        // complete (the handler has already settled the mana cost) but before the spell is pushed
        // onto the stack — i.e. the cast is "committed" the moment the spell becomes a real
        // game object on the stack.
        if (castFromZone == Zone.COMMAND) {
            newState = newState.updateEntity(cardId) { c ->
                val commander = c.get<com.wingedsheep.engine.state.components.identity.CommanderComponent>()
                if (commander != null) {
                    c.with(commander.copy(castsFromCommandZone = commander.castsFromCommandZone + 1))
                } else {
                    c
                }
            }
        }

        // Push to stack and reset priority passes (new stack item requires fresh round of passes)
        newState = newState.pushToStack(cardId)
            .copy(priorityPassedBy = emptySet())

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
        // For face-down creatures, use a generic name in the event
        val eventName = if (castFaceDown) "Face-down creature" else spellName

        // Collect target names for the cast event log
        val targetNames = effectiveTargets.mapNotNull { target ->
            when (target) {
                is ChosenTarget.Permanent -> newState.getEntity(target.entityId)?.get<CardComponent>()?.name
                is ChosenTarget.Player -> if (target.playerId == casterId) "themselves" else "opponent"
                is ChosenTarget.Spell -> newState.getEntity(target.spellEntityId)?.get<CardComponent>()?.name
                    ?: "spell"
                is ChosenTarget.Card -> newState.getEntity(target.cardId)?.get<CardComponent>()?.name
            }
        }

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
        val reportedChosenModesCount = if (countsAsModalForTriggers) chosenModes.size else 0

        val events = mutableListOf<GameEvent>(
            SpellCastEvent(
                spellEntityId = cardId,
                cardName = eventName,
                casterId = casterId,
                targetNames = targetNames,
                xValue = boundXValue,
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

        // Crime detection (CR Outlaws of Thunder Junction). Emit at most once per cast,
        // regardless of how many opponent-controlled targets the spell chose.
        if (CrimeDetector.isCrime(newState, casterId, effectiveTargets)) {
            events.add(CommitCrimeEvent(casterId, cardId, eventName))
            newState = recordCrime(newState, casterId)
        }

        // "Whenever a player chooses one or more targets" (Psychic Battle). Emit once per cast
        // when the spell chose at least one target.
        if (effectiveTargets.isNotEmpty()) {
            events.add(TargetsChosenEvent(casterId, cardId, eventName))
        }

        // Emit BecomesTargetEvent for each permanent, spell, or player target (Rule 601.2c)
        // Also track targeting for Valiant ("first time each turn")
        for (target in effectiveTargets) {
            newState = emitBecomesTarget(newState, target, cardId, casterId, events, sourceIsSpell = true)
        }

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

        return ExecutionResult.success(
            newState.tick(),
            events
        )
    }

    /**
     * Record that [playerId] committed a crime this turn (CR Outlaws of Thunder Junction). Folded
     * in at every [CommitCrimeEvent] emit site so the `PlayerCommittedCrimeThisTurn` condition (e.g.
     * Seize the Secrets' cost reduction) can read it. Cleared at each turn boundary by `TurnManager`.
     */
    private fun recordCrime(state: GameState, playerId: EntityId): GameState =
        if (playerId in state.playersWhoCommittedCrimeThisTurn) state
        else state.copy(playersWhoCommittedCrimeThisTurn = state.playersWhoCommittedCrimeThisTurn + playerId)

    /**
     * Emit a [BecomesTargetEvent] for a permanent, spell, or player target (CR 601.2c — "The chosen
     * objects and/or players each become a target of that spell"). A [ChosenTarget.Card] (a card
     * targeted in a non-battlefield zone) still emits nothing: no printed "becomes the target"
     * trigger reaches into those zones, and the trigger side has no vocabulary to ask for it.
     * Returns the updated state.
     *
     * Spell targets are left out of the "targeted by this controller this turn" tracking (Valiant's
     * "first time each turn") and always carry `firstTime = true`: a spell's stack entity can be
     * reused as the resolved permanent's entity, so marking it would leak a stale flag onto the
     * permanent. Permanents and players are tracked; `CleanupPhaseManager` clears the component for
     * every entity, players included.
     *
     * [sourceIsSpell] is required rather than defaulted so every call site has to state whether a
     * spell or an ability did the targeting — `spellsOnly` / `abilitiesOnly` read nothing else.
     */
    private fun emitBecomesTarget(
        state: GameState,
        target: ChosenTarget,
        sourceEntityId: EntityId,
        controllerId: EntityId,
        events: MutableList<GameEvent>,
        sourceIsSpell: Boolean
    ): GameState {
        val isSpell = target is ChosenTarget.Spell
        val isPlayer = target is ChosenTarget.Player
        val targetEntityId = when (target) {
            is ChosenTarget.Permanent -> target.entityId
            is ChosenTarget.Spell -> target.spellEntityId
            is ChosenTarget.Player -> target.playerId
            is ChosenTarget.Card -> return state
        }
        val targetName = if (isPlayer) {
            state.getEntity(targetEntityId)?.get<PlayerComponent>()?.name ?: "Unknown"
        } else {
            state.getEntity(targetEntityId)?.get<CardComponent>()?.name ?: "Unknown"
        }
        val firstTime = isSpell || !hasBeenTargetedByController(state, targetEntityId, controllerId)
        events.add(
            BecomesTargetEvent(
                targetEntityId,
                targetName,
                sourceEntityId,
                controllerId,
                firstTime,
                targetIsSpell = isSpell,
                sourceIsSpell = sourceIsSpell,
                targetIsPlayer = isPlayer
            )
        )
        return if (isSpell) state else markTargetedByController(state, targetEntityId, controllerId)
    }

    /**
     * Put a triggered ability on the stack.
     */
    fun putTriggeredAbility(
        state: GameState,
        ability: TriggeredAbilityOnStackComponent,
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList(),
        /**
         * True when this ability fired because its own source creature was declared as an attacker
         * (a SELF-bound attacks trigger). Stamped onto the emitted [AbilityTriggeredEvent] so
         * Firebender Ascension's "attacking causes a triggered ability of that creature to trigger"
         * meta-trigger can key on it.
         */
        causedByAttack: Boolean = false
    ): ExecutionResult {
        // Create a new entity for the ability on the stack
        val (abilityId, stateWithId) = state.newEntity()

        var container = ComponentContainer.of(ability)
        if (targets.isNotEmpty()) {
            container = container.with(TargetsComponent.capture(state, targets, targetRequirements))
        }

        var newState = stateWithId.withEntity(abilityId, container)
        newState = newState.pushToStack(abilityId)
            .copy(priorityPassedBy = emptySet())

        val events = mutableListOf<GameEvent>(
            AbilityTriggeredEvent(
                ability.sourceId,
                ability.sourceName,
                ability.controllerId,
                ability.description,
                abilityEntityId = abilityId,
                causedByAttack = causedByAttack
            )
        )

        if (CrimeDetector.isCrime(newState, ability.controllerId, targets)) {
            events.add(CommitCrimeEvent(ability.controllerId, abilityId, ability.sourceName))
            newState = recordCrime(newState, ability.controllerId)
        }

        if (targets.isNotEmpty()) {
            events.add(TargetsChosenEvent(ability.controllerId, abilityId, ability.sourceName))
        }

        // Emit BecomesTargetEvent for each permanent, spell, or player target
        // Use abilityId (the entity on the stack) as source so ward can counter it
        for (target in targets) {
            newState = emitBecomesTarget(
                newState, target, abilityId, ability.controllerId, events, sourceIsSpell = false
            )
        }

        return ExecutionResult.success(
            newState.tick(),
            events
        )
    }

    /**
     * Put a copy of a spell on the stack.
     *
     * Per rule 707.10, a copy of an instant or sorcery spell is itself a spell on the
     * stack with the original's characteristics. We clone the source's [CardComponent] and
     * [SpellOnStackComponent] onto a new entity, tag it with [CopyOfComponent], and push it.
     *
     * Per rule 707.10 a copy isn't cast — this emits a [SpellCopiedEvent], not a
     * [SpellCastEvent], so "whenever you cast a spell" triggers don't fire.
     *
     * Targets and modal choices default to inheriting from the source. Callers may override
     * them (e.g., Storm's per-copy retargeting).
     */
    fun putSpellCopy(
        state: GameState,
        sourceSpellId: EntityId,
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList(),
        chosenModes: List<Int>? = null,
        modeTargetsOrdered: List<List<ChosenTarget>>? = null,
        modeTargetRequirements: Map<Int, List<TargetRequirement>>? = null,
        copyIndex: Int? = null,
        copyTotal: Int? = null,
        controllerId: EntityId? = null
    ): ExecutionResult {
        val sourceContainer = state.getEntity(sourceSpellId)
            ?: return ExecutionResult.error(state, "Source spell not found: $sourceSpellId")
        // CR 707.10: a spell that can't be copied yields no copy. Succeed without change.
        if (sourceContainer.has<com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent>()) {
            return ExecutionResult.success(state)
        }
        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source is not a card: $sourceSpellId")
        val sourceSpell = sourceContainer.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Source is not a spell on stack: $sourceSpellId")
        val sourceTargets = sourceContainer.get<TargetsComponent>()

        val (copyId, stateWithId) = state.newEntity()
        val copyController = controllerId ?: sourceSpell.casterId

        val effectiveModes = chosenModes ?: sourceSpell.chosenModes
        val effectiveModeTargets = modeTargetsOrdered ?: sourceSpell.modeTargetsOrdered
        val effectiveModeRequirements = modeTargetRequirements ?: sourceSpell.modeTargetRequirements

        // Determine final flat targets/requirements for the copy's TargetsComponent.
        val effectiveTargets = when {
            targets.isNotEmpty() -> targets
            effectiveModes.isNotEmpty() -> effectiveModeTargets.flatten()
            else -> sourceTargets?.targets ?: emptyList()
        }
        val effectiveRequirements = when {
            targetRequirements.isNotEmpty() -> targetRequirements
            effectiveModes.isNotEmpty() ->
                effectiveModes.flatMap { effectiveModeRequirements[it] ?: emptyList() }
            else -> sourceTargets?.targetRequirements ?: emptyList()
        }

        // Clone the card characteristics. The CardComponent keeps the same cardDefinitionId,
        // name, types, colors, mana cost, and spellEffect (707.10).
        val copiedCardComp = sourceCard.copy(ownerId = copyController)

        // Clone cast-time state; per 707.10 the copy inherits every decision made for
        // the original. The data-class copy preserves: xValue, declaredCostSlot, wasBlightPaid,
        // wasWarped, wasEvoked, sacrificedPermanents (snapshots of P/T + subtypes), damageDistribution,
        // chosenCreatureType, exiledCardCount, castFromZone, beheldCards, and the
        // manaSpent{White,Blue,Black,Red,Green,Colorless} colors. Only the caster
        // (copy controller) and modal fields (which the caller may retarget) are
        // overridden explicitly. Payment events (ManaSpentEvent, SpellCastEvent) are
        // deliberately not re-emitted — a copy isn't cast (707.10).
        val copiedSpellComp = sourceSpell.copy(
            casterId = copyController,
            chosenModes = effectiveModes,
            modeTargetsOrdered = effectiveModeTargets,
            modeTargetRequirements = effectiveModeRequirements
        )

        var container = ComponentContainer.of(copiedCardComp, copiedSpellComp)
        if (effectiveTargets.isNotEmpty()) {
            container = container.with(TargetsComponent.capture(state, effectiveTargets, effectiveRequirements))
        }
        container = container.with(
            CopyOfComponent(
                originalCardDefinitionId = sourceCard.cardDefinitionId,
                copiedCardDefinitionId = sourceCard.cardDefinitionId
            )
        )

        var newState = stateWithId.withEntity(copyId, container)
        newState = newState.pushToStack(copyId).copy(priorityPassedBy = emptySet())

        val events = mutableListOf<GameEvent>(
            SpellCopiedEvent(
                copyEntityId = copyId,
                cardName = sourceCard.name,
                controllerId = copyController,
                originalSpellId = sourceSpellId,
                copyIndex = copyIndex,
                copyTotal = copyTotal
            )
        )

        // Emit BecomesTargetEvent for each permanent, spell, or player target — the copy is its own
        // source on the stack (ward on the target can counter the copy independently).
        for (target in effectiveTargets) {
            newState = emitBecomesTarget(newState, target, copyId, copyController, events, sourceIsSpell = true)
        }

        return ExecutionResult.success(newState.tick(), events)
    }

    /**
     * Put an activated ability on the stack.
     *
     * [emitActivationEvent] is true for a genuine activation. A **copy** of an activated ability is
     * *not* activated (CR 707.10), so the copy paths pass false to suppress the
     * [AbilityActivatedEvent] — otherwise placing the copy would itself re-fire
     * "whenever you activate an ability" triggers (e.g. Ertha Jo, Frontier Mentor would copy its own
     * copies endlessly). The copy still becomes a stack object with its own targets, so
     * `BecomesTargetEvent`/`TargetsChosenEvent` are still emitted below.
     */
    fun putActivatedAbility(
        state: GameState,
        ability: ActivatedAbilityOnStackComponent,
        targets: List<ChosenTarget> = emptyList(),
        targetRequirements: List<TargetRequirement> = emptyList(),
        emitActivationEvent: Boolean = true,
        costsTap: Boolean = false,
        isExhaust: Boolean = false,
        cantBeCopied: Boolean = false
    ): ExecutionResult {
        val (abilityId, stateWithId) = state.newEntity()

        var container = ComponentContainer.of(ability)
        if (targets.isNotEmpty()) {
            container = container.with(TargetsComponent.capture(state, targets, targetRequirements))
        }
        // CR 707.10e — "This ability can't be copied": tag the ability instance on the stack so a
        // copy-ability effect (e.g. Gogo, Master of Mimicry) makes no copy of it.
        if (cantBeCopied) {
            container = container.with(
                com.wingedsheep.engine.state.components.identity.CantBeCopiedComponent
            )
        }

        var newState = stateWithId.withEntity(abilityId, container)
        newState = newState.pushToStack(abilityId)
            .copy(priorityPassedBy = emptySet())

        val events = mutableListOf<GameEvent>()
        if (emitActivationEvent) {
            // Abilities reaching the stack are never mana abilities (CR 605.3 — mana abilities
            // resolve without the stack). costsTap lets the {T}-in-cost trigger family distinguish
            // tap-cost abilities (which it must skip) from non-tap ones.
            events.add(
                AbilityActivatedEvent(
                    ability.sourceId,
                    ability.sourceName,
                    ability.controllerId,
                    abilityEntityId = abilityId,
                    costsTap = costsTap,
                    isManaAbility = false,
                    isExhaust = isExhaust,
                )
            )
        }

        if (CrimeDetector.isCrime(newState, ability.controllerId, targets)) {
            events.add(CommitCrimeEvent(ability.controllerId, abilityId, ability.sourceName))
            newState = recordCrime(newState, ability.controllerId)
        }

        if (targets.isNotEmpty()) {
            events.add(TargetsChosenEvent(ability.controllerId, abilityId, ability.sourceName))
        }

        // Emit BecomesTargetEvent for each permanent, spell, or player target
        // Use abilityId (the entity on the stack) as source so ward can counter it
        for (target in targets) {
            newState = emitBecomesTarget(
                newState, target, abilityId, ability.controllerId, events, sourceIsSpell = false
            )
        }

        return ExecutionResult.success(
            newState.tick(),
            events
        )
    }

    // =========================================================================
    // Resolution
    // =========================================================================

    /**
     * Resolve the top item on the stack.
     */
    fun resolveTop(state: GameState): ExecutionResult {
        val topId = state.getTopOfStack()
            ?: return ExecutionResult.error(state, "Stack is empty")

        val container = state.getEntity(topId)
            ?: return ExecutionResult.error(state, "Stack item not found: $topId")

        // Pop from stack
        val (_, poppedState) = state.popFromStack()

        // Determine what type of item this is
        return when {
            container.has<SpellOnStackComponent>() ->
                resolveSpell(poppedState, topId, container)

            container.has<TriggeredAbilityOnStackComponent>() ->
                resolveTriggeredAbility(poppedState, topId, container)

            container.has<ActivatedAbilityOnStackComponent>() ->
                resolveActivatedAbility(poppedState, topId, container)

            else ->
                ExecutionResult.error(state, "Unknown stack item type")
        }
    }

    /**
     * Resolve a spell.
     */
    private fun resolveSpell(
        state: GameState,
        spellId: EntityId,
        container: ComponentContainer
    ): ExecutionResult {
        val cardComponent = container.get<CardComponent>()
        val spellComponent = container.get<SpellOnStackComponent>()!!
        val targetsComponent = container.get<TargetsComponent>()

        // Validate targets if spell has any (including protection check - Rule 702.16)
        val sourceColors = cardComponent?.colors ?: emptySet()
        val sourceSubtypes = cardComponent?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
        // `resolvedTargets` is the compacted (drop-illegal) list used as `context.targets`
        // — same shape every executor has always seen. `alignedResolvedTargets` is a parallel
        // list the same length as the originally-chosen targets, with `null` in slots whose
        // target was dropped by 608.2b validation. It is forwarded to `buildNamedTargets`
        // so a sub-effect that references a now-illegal target through its declared
        // [EffectTarget.BoundVariable] (e.g. Diplomatic Relations' `myCreature` after its
        // FROM creature dies in response) resolves to `null` and fizzles, instead of
        // silently consuming the NEXT still-valid target whose position shifted forward
        // in the compacted list.
        val resolvedTargets: List<ChosenTarget>
        val alignedResolvedTargets: List<ChosenTarget?>
        if (targetsComponent != null && targetsComponent.targets.isNotEmpty()) {
            val validTargets = validateTargets(
                state, targetsComponent.targets, sourceColors, sourceSubtypes,
                spellComponent.casterId, targetsComponent.targetRequirements,
                sourceId = spellId,
                targetingSourceType = TargetingSourceType.SPELL,
                xValue = spellComponent.xValue,
                targetEntryStamps = targetsComponent.targetEntryStamps
            )
            if (validTargets.isEmpty()) {
                // All targets invalid - spell fizzles
                return fizzleSpell(state, spellId, cardComponent, spellComponent)
            }
            resolvedTargets = validTargets
            alignedResolvedTargets = buildAlignedValidated(targetsComponent.targets, validTargets)
        } else {
            resolvedTargets = targetsComponent?.targets ?: emptyList()
            alignedResolvedTargets = resolvedTargets
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        // Check if permanent or non-permanent.
        // Adventure / split face cast (CR 715 / 709) — when the spell was cast as a face, route
        // resolution by the face's type line. An Adventure (instant/sorcery) face on a creature
        // card must take the non-permanent path even though the card's primary characteristics
        // describe a creature.
        val faceTypeLine = spellComponent.faceIndex?.let { idx ->
            val def = cardComponent?.let { cardRegistry.getCard(it.name) }
            def?.cardFaces?.getOrNull(idx)?.typeLine
        }
        val resolvedTypeLine = faceTypeLine ?: cardComponent?.typeLine
        val isPermanent = resolvedTypeLine?.isPermanent ?: false

        if (isPermanent) {
            // Put permanent on battlefield
            val permanentResult = resolvePermanentSpell(newState, spellId, spellComponent, cardComponent)
            if (permanentResult.isPaused) {
                return ExecutionResult.paused(
                    permanentResult.state,
                    permanentResult.pendingDecision!!,
                    events + permanentResult.events
                )
            }
            newState = permanentResult.state
            events.addAll(permanentResult.events)
            val permanentName = cardComponent?.name ?: "Unknown"
            events.add(ResolvedEvent(spellId, permanentName))
            events.add(
                ZoneChangeEvent(
                    spellId,
                    permanentName,
                    null, // Was on stack
                    Zone.BATTLEFIELD,
                    cardComponent?.ownerId ?: spellComponent.casterId,
                    xValue = spellComponent.xValue
                )
            )
        } else {
            // Execute effects and put in graveyard
            val effectResult = resolveNonPermanentSpell(
                newState, spellId, spellComponent, cardComponent,
                resolvedTargets,
                alignedResolvedTargets
            )
            if (effectResult.isPaused) {
                // Effect paused for a decision (e.g., draw replacement prompt).
                // resolveNonPermanentSpell already moved spell to graveyard.
                val allEvents = events + effectResult.events +
                    ResolvedEvent(spellId, cardComponent?.name ?: "Unknown")
                return ExecutionResult.paused(
                    effectResult.state,
                    effectResult.pendingDecision!!,
                    allEvents
                )
            }
            newState = effectResult.newState
            events.addAll(effectResult.events)
            events.add(ResolvedEvent(spellId, cardComponent?.name ?: "Unknown"))
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Resolve a permanent spell - put it on the battlefield.
     * May pause for player input (e.g., Clone choosing a creature to copy).
     */
    private fun resolvePermanentSpell(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?
    ): ExecutionResult {
        val controllerId = spellComponent.casterId
        val ownerId = cardComponent?.ownerId ?: controllerId

        // Check for EntersAsCopy replacement effect before entering the battlefield
        val cardDef = cardComponent?.cardDefinitionId?.let { cardRegistry.getCard(it) }
        if (cardDef != null && !spellComponent.castFaceDown) {
            val entersAsCopy = cardDef.script.replacementEffects.filterIsInstance<EntersAsCopy>().firstOrNull()
            if (entersAsCopy != null) {
                // Find candidates to copy. Battlefield copies (Clone) read permanents in play;
                // graveyard copies (Superior Spider-Man) read creature cards across every graveyard.
                val copyFilter = entersAsCopy.copyFilter
                val copyFromGraveyard = entersAsCopy.copyFromZone == Zone.GRAVEYARD
                val candidatePool = if (copyFromGraveyard) {
                    state.turnOrder.flatMap { state.getGraveyard(it) }
                } else {
                    state.getBattlefield()
                }
                var candidates = candidatePool.filter { entityId ->
                    predicateEvaluator.matches(
                        state, state.projectedState, entityId, copyFilter,
                        PredicateContext(controllerId = controllerId)
                    )
                }

                // Filter by mana value ≤ total mana spent (for Mockingbird-style effects)
                if (entersAsCopy.filterByTotalManaSpent) {
                    val xValue = spellComponent.xValue ?: 0
                    // Total mana spent = X + non-X portion of mana cost
                    val baseNonXCost = cardComponent.manaCost.symbols
                        .filterNot { it is com.wingedsheep.sdk.core.ManaSymbol.X }
                        .sumOf { it.cmc }
                    val totalManaSpent = xValue + baseNonXCost
                    candidates = candidates.filter { entityId ->
                        val targetCard = state.getEntity(entityId)?.get<CardComponent>()
                        (targetCard?.manaValue ?: 0) <= totalManaSpent
                    }
                }

                if (candidates.isNotEmpty()) {
                    // Present the selection decision
                    val filterDesc = copyFilter.description
                    val whereDesc = if (copyFromGraveyard) "$filterDesc card in a graveyard" else "$filterDesc"
                    val decisionId = "clone-enters-${spellId.value}"
                    val decision = SelectCardsDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = if (entersAsCopy.optional) {
                            "You may choose a $whereDesc to copy"
                        } else {
                            "Choose a $whereDesc to copy"
                        },
                        context = DecisionContext(
                            sourceId = spellId,
                            sourceName = cardComponent.name,
                            phase = DecisionPhase.RESOLUTION
                        ),
                        options = candidates,
                        minSelections = if (entersAsCopy.optional) 0 else 1,
                        maxSelections = 1,
                        // Battlefield copies click permanents in-place; graveyard copies use the
                        // modal card-list overlay (graveyards aren't on the battlefield).
                        useTargetingUI = !copyFromGraveyard
                    )

                    // Push continuation
                    val continuation = CloneEntersContinuation(
                        decisionId = decisionId,
                        spellId = spellId,
                        controllerId = controllerId,
                        ownerId = ownerId,
                        castFaceDown = spellComponent.castFaceDown,
                        additionalSubtypes = entersAsCopy.additionalSubtypes,
                        additionalKeywords = entersAsCopy.additionalKeywords,
                        nameOverride = entersAsCopy.nameOverride,
                        powerOverride = entersAsCopy.powerOverride,
                        toughnessOverride = entersAsCopy.toughnessOverride,
                        exileCopiedCard = entersAsCopy.exileCopiedCard,
                        additionalCounters = entersAsCopy.additionalCounters
                    )

                    val pausedState = state
                        .pushContinuation(continuation)
                        .withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision)
                }
                // No matching permanents on battlefield - fall through to enter as itself (0/0)
            }

            // Check for EntersWithChoice replacement effects (color first, then creature type, then creature)
            // Process in priority order: COLOR → CREATURE_TYPE → CREATURE_ON_BATTLEFIELD
            // When a card has multiple choices (e.g., Riptide Replicator: color + creature type),
            // the first one pauses; its continuation resumer chains to the next.
            val printedChoices = cardDef.script.replacementEffects.filterIsInstance<EntersWithChoice>()
            // Granted Riot ("Other Spiders you control have riot") is not printed on the entering
            // spell, so synthesize its enters-with choice — one per granting lord (CR 702.136b) —
            // when a battlefield lord grants RIOT to it.
            val grantedRiotCount = com.wingedsheep.engine.mechanics.RiotSynthesis
                .grantedRiotInstanceCount(state, spellId, cardRegistry, predicateEvaluator)
            val syntheticRiotChoice = if (grantedRiotCount > 0) {
                com.wingedsheep.engine.mechanics.RiotSynthesis.RIOT_CHOICE
            } else null
            val entersWithChoices = printedChoices + listOfNotNull(syntheticRiotChoice)
            val firstChoice = entersWithChoices
                .sortedBy { it.choiceType.ordinal }
                .firstOrNull()
            if (firstChoice != null) {
                val isSynthetic = firstChoice === syntheticRiotChoice
                val result = pauseForEntersWithChoice(
                    state, spellId, controllerId, ownerId, cardComponent, firstChoice,
                    syntheticRiot = isSynthetic,
                    syntheticRiotRemaining = if (isSynthetic) grantedRiotCount - 1 else 0
                )
                if (result != null) return result
                // null means choice couldn't be presented (e.g., no creatures on battlefield) — fall through
            }

            // Check for EntersWithRevealCounters replacement effect (Amplify mechanic)
            val revealCountersEffect = cardDef.script.replacementEffects.filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithRevealCounters>().firstOrNull()
            if (revealCountersEffect != null) {
                // Find cards in the reveal source zone that match the effect's filter
                val revealZone = ZoneKey(controllerId, revealCountersEffect.revealSource)
                val predicateContext = PredicateContext(controllerId = controllerId, sourceId = spellId)
                val validCards = state.getZone(revealZone).filter { cardId ->
                    predicateEvaluator.matches(state, state.projectedState, cardId, revealCountersEffect.filter, predicateContext)
                }

                if (validCards.isNotEmpty()) {
                    val decisionId = "reveal-counters-enters-${spellId.value}"
                    val decision = SelectCardsDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = "Reveal cards from your ${revealCountersEffect.revealSource.name.lowercase()} that match ${cardComponent.name} (${revealCountersEffect.countersPerReveal} ${revealCountersEffect.counterType} counter${if (revealCountersEffect.countersPerReveal > 1) "s" else ""} each)",
                        context = DecisionContext(
                            sourceId = spellId,
                            sourceName = cardComponent.name,
                            phase = DecisionPhase.RESOLUTION
                        ),
                        options = validCards,
                        minSelections = 0,
                        maxSelections = validCards.size
                    )

                    val continuation = RevealCountersContinuation(
                        decisionId = decisionId,
                        spellId = spellId,
                        controllerId = controllerId,
                        ownerId = ownerId,
                        counterType = revealCountersEffect.counterType,
                        countersPerReveal = revealCountersEffect.countersPerReveal
                    )

                    val pausedState = state
                        .pushContinuation(continuation)
                        .withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision)
                }
                // No valid cards — enter normally without counters
            }

            val exileCountersEffect = cardDef.script.replacementEffects
                .filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithExileCounters>()
                .firstOrNull()
            if (exileCountersEffect != null) {
                val predicateContext = PredicateContext(controllerId = controllerId, sourceId = spellId)
                val candidates = state.getZone(ZoneKey(controllerId, exileCountersEffect.sourceZone)).filter { cardId ->
                    predicateEvaluator.matches(
                        state, state.projectedState, cardId, exileCountersEffect.filter, predicateContext
                    )
                }
                val maxCards = com.wingedsheep.engine.handlers.DynamicAmountEvaluator().evaluate(
                    state,
                    exileCountersEffect.maxCards,
                    EffectContext(
                        sourceId = spellId,
                        controllerId = controllerId,
                        xValue = spellComponent.xValue ?: 0
                    )
                ).coerceAtLeast(0).coerceAtMost(candidates.size)
                if (candidates.isNotEmpty() && maxCards > 0) {
                    val decisionId = "exile-counters-enters-${spellId.value}"
                    val decision = SelectCardsDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = "Exile up to $maxCards ${exileCountersEffect.filter.description} cards from your ${exileCountersEffect.sourceZone.name.lowercase()} for ${cardComponent.name}",
                        context = DecisionContext(
                            sourceId = spellId,
                            sourceName = cardComponent.name,
                            phase = DecisionPhase.RESOLUTION
                        ),
                        options = candidates,
                        minSelections = 0,
                        maxSelections = maxCards
                    )
                    val continuation = ExileCountersContinuation(
                        decisionId = decisionId,
                        spellId = spellId,
                        controllerId = controllerId,
                        ownerId = ownerId,
                        counterType = exileCountersEffect.counterType.description,
                        countersPerCard = exileCountersEffect.countersPerCard
                    )
                    val pausedState = state.pushContinuation(continuation).withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision)
                }
            }

            // Check for EntersWithDevour replacement effect (CR 702.82, Devour variants).
            // Pauses for the controller to pick which permanents to sacrifice; the resumer
            // sacrifices them, places multiplier × count counters on the entering spell
            // entity, then completes the entry.
            val devourEffect = cardDef.script.replacementEffects
                .filterIsInstance<com.wingedsheep.sdk.scripting.EntersWithDevour>().firstOrNull()
            if (devourEffect != null) {
                val predicateContext = PredicateContext(controllerId = controllerId, sourceId = spellId)
                val candidates = state.getBattlefield().filter { entityId ->
                    if (entityId == spellId) return@filter false
                    // Use projected controller — control-changing effects (e.g. an opponent's
                    // Act of Treason on one of your lands) must be respected; you can only
                    // sacrifice permanents you currently control (CR 701.21a).
                    if (state.projectedState.getController(entityId) != controllerId) return@filter false
                    predicateEvaluator.matches(state, state.projectedState, entityId, devourEffect.sacrificeFilter, predicateContext)
                }

                if (candidates.isNotEmpty()) {
                    val devourLabel = devourEffect.description.substringBefore(" (")
                    val decisionId = "devour-enters-${spellId.value}"
                    val decision = SelectCardsDecision(
                        id = decisionId,
                        playerId = controllerId,
                        prompt = "$devourLabel: sacrifice any number of ${devourEffect.sacrificeFilter.description}s for ${cardComponent.name}",
                        context = DecisionContext(
                            sourceId = spellId,
                            sourceName = cardComponent.name,
                            phase = DecisionPhase.RESOLUTION
                        ),
                        options = candidates,
                        minSelections = 0,
                        maxSelections = candidates.size,
                        useTargetingUI = true
                    )

                    val continuation = DevourEntersContinuation(
                        decisionId = decisionId,
                        spellId = spellId,
                        controllerId = controllerId,
                        ownerId = ownerId,
                        multiplier = devourEffect.multiplier,
                        counterType = devourEffect.counterType.description
                    )

                    val pausedState = state
                        .pushContinuation(continuation)
                        .withPendingDecision(decision)
                    return ExecutionResult.paused(pausedState, decision)
                }
                // No valid permanents to sacrifice — enter with zero devour counters
            }
        }

        // Check for "pay life or enter tapped" (shock lands) before entering the battlefield
        if (cardDef != null && !spellComponent.castFaceDown) {
            val entersTapped = cardDef.script.replacementEffects.filterIsInstance<EntersTapped>().firstOrNull()
            if (entersTapped?.payLifeCost != null) {
                val decisionId = "pay-life-or-enter-tapped-spell-${spellId.value}"
                val decision = YesNoDecision(
                    id = decisionId,
                    playerId = controllerId,
                    prompt = "Pay ${entersTapped.payLifeCost} life to have ${cardComponent.name} enter untapped?",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    )
                )
                val continuation = PayLifeOrEnterTappedSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    lifeCost = entersTapped.payLifeCost!!
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                return ExecutionResult.paused(pausedState, decision)
            }
        }

        // Normal permanent entry
        val (newState, enterEvents) = enterPermanentOnBattlefield(state, spellId, spellComponent, cardComponent, cardDef)
        val sagaEvents = if (cardDef != null && !spellComponent.castFaceDown && cardDef.isSaga) {
            listOf(CountersAddedEvent(spellId, "LORE", 1, cardDef.name))
        } else {
            emptyList()
        }
        return ExecutionResult.success(newState, enterEvents + sagaEvents)
    }

    /**
     * Complete the permanent entry to the battlefield (shared between normal resolution and clone continuation).
     */
    internal fun enterPermanentOnBattlefield(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?
    ): Pair<GameState, List<GameEvent>> {
        val controllerId = spellComponent.casterId
        // The Tomb of Aclazotz: a graveyard-cast entry rider frozen on this spell at cast time.
        // Captured before the `updateEntity` block below strips it; applied on entry further down.
        val graveyardCastRider =
            state.getEntity(spellId)?.get<com.wingedsheep.engine.state.components.stack.GraveyardCastRiderComponent>()

        // For Auras: get the target before removing TargetsComponent. The target is usually a
        // permanent, but "enchant player" Auras (Grievous Wound) attach to a player — both are
        // entities, so AttachedToComponent holds either id (CR 303.4).
        val auraTargetId = if (cardComponent?.isAura == true) {
            state.getEntity(spellId)?.get<TargetsComponent>()?.targets?.firstOrNull()?.let { target ->
                when (target) {
                    is ChosenTarget.Permanent -> target.entityId
                    is ChosenTarget.Player -> target.playerId
                    else -> null
                }
            }
        } else null

        // Update entity: remove spell components, add permanent components.
        // CR 707.10f: a copy of a permanent spell becomes a token as it resolves.
        // Distinguish from "enters as a copy" effects (Clone, Mockingbird) which set
        // originalCardComponent for the revert-on-leave rule; those produce real
        // permanents, not tokens.
        val copyOf = state.getEntity(spellId)
            ?.get<com.wingedsheep.engine.state.components.identity.CopyOfComponent>()
        val resolvingAsSpellCopy = copyOf != null && copyOf.originalCardComponent == null
        var newState = state.updateEntity(spellId) { c ->
            var updated = c.without<SpellOnStackComponent>()
                .without<TargetsComponent>()
                .without<com.wingedsheep.engine.state.components.stack.GraveyardCastRiderComponent>()
                .with(ControllerComponent(controllerId))

            if (resolvingAsSpellCopy) {
                updated = updated.with(TokenComponent)
            }

            // If cast face-down (morph / disguise), add FaceDownComponent and strip any
            // RevealedToComponent from hand-peek effects (zone change = new object).
            // MorphDataComponent was already added when the spell was cast; the mode marker is
            // what carries disguise's ward {2} (CR 702.168a) and the face-down art.
            if (spellComponent.castFaceDown) {
                updated = updated.with(FaceDownComponent)
                    .without<RevealedToComponent>()
                val castDef = state.getEntity(spellId)?.get<CardComponent>()
                    ?.let { cardRegistry.getCard(it.cardDefinitionId) }
                faceDownCastMode(castDef)?.let { updated = updated.with(FaceDownModeComponent(it)) }
            }

            // All permanents enter summoning sick (CR 302.6 / 508.1a — the control-continuity
            // check is about the permanent, not whether it was a creature the whole turn). Vehicles
            // and animated lands that become creatures mid-turn must inherit the marker too.
            // Downstream checks gate on isCreature/{T}-cost so this is harmless for lands and
            // non-creature artifacts until they become creatures (Crew, animate-land, etc.).
            updated = updated.with(SummoningSicknessComponent)

            // Track that this permanent entered the battlefield this turn
            updated = updated.with(EnteredThisTurnComponent)

            // Track if this permanent was cast from hand (for cards like Phage the Untouchable)
            if (spellComponent.castFromZone == Zone.HAND) {
                updated = updated.with(CastFromHandComponent)
            }

            // Track if this permanent was cast from a graveyard (for triggers that care about
            // creatures cast from graveyard — e.g., Twilight Diviner).
            if (spellComponent.castFromZone == Zone.GRAVEYARD) {
                updated = updated.with(com.wingedsheep.engine.state.components.battlefield.CastFromGraveyardComponent)
            }

            // Track if this permanent was cast from a library (e.g. "cast from the top of your
            // library" permissions — Mikey & Don's +1/+1 rider on creatures cast this way).
            if (spellComponent.castFromZone == Zone.LIBRARY) {
                updated = updated.with(com.wingedsheep.engine.state.components.battlefield.CastFromLibraryComponent)
            }

            // Track if this permanent was cast from exile (impulse draws, plot/foretell, an
            // adventurer's permanent half, linked-exile grants) — Extraordinary Journey.
            if (spellComponent.castFromZone == Zone.EXILE) {
                updated = updated.with(com.wingedsheep.engine.state.components.battlefield.CastFromExileComponent)
            }

            // Carry the cast-time choices durably onto the permanent (CR 601.2b choices ride the
            // stable entity onto the battlefield) so triggered/activated abilities can read "the X
            // / color / type / kicked-ness this was cast with" via DynamicAmount.CastX /
            // DynamicAmount.CastChoice / Conditions.CastChoice* for its whole life on the
            // battlefield, with no counter laundering. The bag is stripped when the permanent leaves
            // the battlefield (new object, CR 400.7) — see ZoneMovementUtils.stripBattlefieldComponents.
            //
            // Merge the *as-it-enters* choices already written by the EntersWithChoice resumers
            // (color/type/mode/…) with the *as-it-was-cast* choices carried on the stack object
            // (X / kicked / blight) into one CastChoicesComponent.
            run {
                val entered = updated.get<com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent>()
                var bag = entered ?: com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent()
                spellComponent.xValue?.let { bag = bag.copy(x = it) }
                // The optional additional cost declared while casting (kicker → KICKED, bargain →
                // BARGAINED, CR 702.166b) marks the permanent under its own slot, so a bargained
                // permanent's "if it was bargained" enters trigger reads true while a kicker payoff
                // reading KICKED still reads false.
                spellComponent.declaredCostSlot?.let { slot ->
                    bag = bag.withChoice(
                        slot,
                        com.wingedsheep.engine.state.components.battlefield.ChoiceValue.Flag
                    )
                }
                // Sneak (CR 702.190): durably mark the permanent so Conditions.SneakCostWasPaid
                // reads "its sneak cost was paid" for its whole life on the battlefield.
                if (spellComponent.wasSneaked) {
                    bag = bag.withChoice(
                        com.wingedsheep.sdk.scripting.ChoiceSlot.SNEAK,
                        com.wingedsheep.engine.state.components.battlefield.ChoiceValue.Flag
                    )
                }
                // Web-slinging (CR 702.188): durably mark the permanent so Conditions.WebSlungCostWasPaid
                // reads "it was cast using web-slinging" for its whole life, and carry the returned
                // creature's mana value (CR 118.9c) so a rider like Scarlet Spider, Ben Reilly can enter
                // with that many +1/+1 counters via DynamicAmount.CastChoice(WEB_SLUNG_RETURNED_MV).
                if (spellComponent.wasWebSlung) {
                    bag = bag.withChoice(
                        com.wingedsheep.sdk.scripting.ChoiceSlot.WEB_SLUNG,
                        com.wingedsheep.engine.state.components.battlefield.ChoiceValue.Flag
                    ).withChoice(
                        com.wingedsheep.sdk.scripting.ChoiceSlot.WEB_SLUNG_RETURNED_MV,
                        com.wingedsheep.engine.state.components.battlefield.ChoiceValue.NumberChoice(
                            spellComponent.webSlungReturnedManaValue
                        )
                    )
                }
                // Mayhem (CR 702.187): durably mark a permanent cast from the graveyard for its
                // mayhem cost so Conditions.MayhemCostWasPaid reads it for the permanent's whole
                // life. (Note: mayhem does NOT exile the spell on resolution — a permanent just
                // enters the battlefield here via the normal permanent-resolution path.)
                if (spellComponent.wasMayhem) {
                    bag = bag.withChoice(
                        com.wingedsheep.sdk.scripting.ChoiceSlot.MAYHEM_CAST,
                        com.wingedsheep.engine.state.components.battlefield.ChoiceValue.Flag
                    )
                }
                // Waterbend (Avatar): durably mark a permanent cast with its (optional) waterbend
                // cost paid so Conditions.WaterbendWasPaid reads it for the permanent's whole life.
                if (spellComponent.wasWaterbendPaid) {
                    bag = bag.withChoice(
                        com.wingedsheep.sdk.scripting.ChoiceSlot.WATERBEND_PAID,
                        com.wingedsheep.engine.state.components.battlefield.ChoiceValue.Flag
                    )
                }
                // Gift (CR 702.174a–b): the promise was elected as an additional cost while casting,
                // so the permanent carries both the flag and the promised opponent durably. Its gift
                // trigger ("when this permanent enters, if its gift cost was paid, …") and any
                // "if the gift was(n't) promised" rider read them back through
                // Conditions.GiftWasPromised / Player.ChosenOpponent — no resolution-time question.
                spellComponent.giftRecipient?.let { recipient ->
                    bag = bag.withChoice(
                        com.wingedsheep.sdk.scripting.ChoiceSlot.GIFT_PROMISED,
                        com.wingedsheep.engine.state.components.battlefield.ChoiceValue.Flag
                    ).withChoice(
                        com.wingedsheep.sdk.scripting.ChoiceSlot.OPPONENT,
                        com.wingedsheep.engine.state.components.battlefield.ChoiceValue.EntityChoice(recipient)
                    )
                }
                if (spellComponent.additionalCostBlightAmount > 0) {
                    bag = bag.withChoice(
                        com.wingedsheep.sdk.scripting.ChoiceSlot.BLIGHT_AMOUNT,
                        com.wingedsheep.engine.state.components.battlefield.ChoiceValue.NumberChoice(
                            spellComponent.additionalCostBlightAmount
                        )
                    )
                }
                if (bag.x != null || bag.chosen.isNotEmpty()) {
                    updated = updated.with(bag)
                }
            }

            // Track if this permanent was cast for its warp cost
            if (spellComponent.wasWarped) {
                updated = updated.with(WarpedComponent)
            }

            // Track if this permanent was cast for its dash cost (CR 702.109a — grants haste
            // live off this marker; see DashedComponent's doc).
            if (spellComponent.wasDashed) {
                updated = updated.with(com.wingedsheep.engine.state.components.battlefield.DashedComponent)
            }

            // Track if this permanent was cast for its evoke cost
            if (spellComponent.wasEvoked) {
                updated = updated.with(com.wingedsheep.engine.state.components.battlefield.EvokedComponent)
            }

            // Impending (CR 702.176a): a permanent cast for its impending cost enters with
            // N time counters. The "isn't a creature" static and the end-step removal trigger
            // both gate on impending-cost-paid AND has-time-counter, so we stamp a
            // CastForImpendingComponent marker that survives the countdown — without it, a
            // normally-cast permanent that gained a time counter from some other effect
            // would incorrectly stop being a creature.
            if (spellComponent.wasImpending) {
                val impendingTime = cardDef?.keywordAbilities
                    ?.filterIsInstance<KeywordAbility.Impending>()
                    ?.firstOrNull()?.time ?: 0
                if (impendingTime > 0) {
                    val existingCounters = updated.get<CountersComponent>() ?: CountersComponent()
                    updated = updated
                        .with(existingCounters.withAdded(CounterType.TIME, impendingTime))
                        .with(com.wingedsheep.engine.state.components.battlefield.CastForImpendingComponent)
                }
            }

            // For split-layout cards (CR 709), attach a RoomComponent recording every face's
            // unlock data and the door-state designation set. The cast face enters unlocked
            // (709.5d); other halves are locked. Cards put on the battlefield by an effect
            // other than casting (reanimation, Replenish, etc.) reach this code with
            // `spellComponent.faceIndex == null` and enter with both halves locked.
            if (cardDef != null && cardDef.layout == com.wingedsheep.sdk.model.CardLayout.SPLIT && cardDef.cardFaces.isNotEmpty()) {
                val roomFaces = cardDef.cardFaces.map { face ->
                    com.wingedsheep.engine.state.components.identity.RoomFace(
                        id = com.wingedsheep.engine.state.components.identity.RoomFaceId(face.name),
                        name = face.name,
                        manaCost = face.manaCost,
                    )
                }
                val unlockedFaceId = spellComponent.faceIndex
                    ?.let { roomFaces.getOrNull(it)?.id }
                updated = updated.with(
                    com.wingedsheep.engine.state.components.identity.RoomComponent(
                        faces = roomFaces,
                        unlocked = unlockedFaceId?.let { setOf(it) } ?: emptySet(),
                    )
                )
            }

            // Record mana colors and provenance spent to cast (for mana-spent-gated triggers and
            // enters-the-battlefield "for each mana from a [subtype] spent to cast it" payoffs).
            if (spellComponent.manaSpentWhite > 0 || spellComponent.manaSpentBlue > 0 ||
                spellComponent.manaSpentBlack > 0 || spellComponent.manaSpentRed > 0 ||
                spellComponent.manaSpentGreen > 0 || spellComponent.manaSpentColorless > 0 ||
                spellComponent.manaSpentBySubtype.isNotEmpty()) {
                updated = updated.with(com.wingedsheep.engine.state.components.battlefield.CastRecordComponent(
                    whiteSpent = spellComponent.manaSpentWhite,
                    blueSpent = spellComponent.manaSpentBlue,
                    blackSpent = spellComponent.manaSpentBlack,
                    redSpent = spellComponent.manaSpentRed,
                    greenSpent = spellComponent.manaSpentGreen,
                    colorlessSpent = spellComponent.manaSpentColorless,
                    manaSpentBySubtype = spellComponent.manaSpentBySubtype
                ))
            }

            // Add continuous effects from static abilities (but not for face-down creatures)
            if (!spellComponent.castFaceDown) {
                updated = staticAbilityHandler.addContinuousEffectComponent(updated)
                updated = staticAbilityHandler.addReplacementEffectComponent(updated)
            }

            // Aura attachment: add AttachedToComponent pointing to the target
            if (auraTargetId != null) {
                updated = updated.with(
                    com.wingedsheep.engine.state.components.battlefield.AttachedToComponent(auraTargetId)
                )
            }

            // CR 707.10f token-copy riders: a copy of a permanent spell that carried added keywords
            // (e.g. "the copy gains haste", Choreographed Sparks) bakes them onto the resulting
            // token's base keywords for its whole life on the battlefield.
            val copyRiders = updated.get<com.wingedsheep.engine.state.components.stack.SpellCopyTokenRidersComponent>()
            if (copyRiders != null && copyRiders.addedKeywords.isNotEmpty()) {
                val card = updated.get<CardComponent>()
                if (card != null) {
                    updated = updated.with(card.copy(baseKeywords = card.baseKeywords + copyRiders.addedKeywords))
                }
            }

            updated
        }

        // "A face-down creature entered the battlefield under your control this turn" (Tunnel
        // Tipster, Oblivious Bookworm). The per-player counter is also bumped by the
        // MoveToZone/MoveCollection face-down paths (manifest, cloak) in ZoneTransitionService;
        // a morph/disguise *cast* resolves through here instead and never touches that service,
        // so without this the tracker would miss the most common face-down entry of all.
        // Cleared at the turn boundary by CleanupPhaseManager.
        if (spellComponent.castFaceDown) {
            newState = newState.updateEntity(controllerId) { playerContainer ->
                val existing = playerContainer
                    .get<com.wingedsheep.engine.state.components.player.PermanentEnteredFaceDownThisTurnComponent>()
                    ?: com.wingedsheep.engine.state.components.player.PermanentEnteredFaceDownThisTurnComponent()
                playerContainer.with(
                    com.wingedsheep.engine.state.components.player
                        .PermanentEnteredFaceDownThisTurnComponent(existing.count + 1)
                )
            }
        }

        // CR 707.10f token-copy riders: register the delayed "sacrifice this token" trigger after
        // the permanent enters. The token shares the resolving spell-copy's entity id, so the
        // delayed trigger targets `spellId` directly.
        run {
            val copyRiders = newState.getEntity(spellId)
                ?.get<com.wingedsheep.engine.state.components.stack.SpellCopyTokenRidersComponent>()
            val sacrificeStep = copyRiders?.sacrificeAtStep
            if (sacrificeStep != null) {
                val sourceName = newState.getEntity(spellId)?.get<CardComponent>()?.name ?: "Unknown"
                newState = newState.addDelayedTrigger(
                    DelayedTriggeredAbility(
                        id = java.util.UUID.randomUUID().toString(),
                        effect = com.wingedsheep.sdk.scripting.effects.SacrificeTargetEffect(
                            com.wingedsheep.sdk.scripting.targets.EffectTarget.SpecificEntity(spellId)
                        ),
                        fireAtStep = sacrificeStep,
                        sourceId = spellId,
                        sourceName = sourceName,
                        controllerId = controllerId,
                        fireOnPlayerId = if (copyRiders.sacrificeOnlyOnControllersTurn) controllerId else null
                    )
                )
                // The rider component has done its job; strip it so it doesn't linger on the permanent.
                newState = newState.updateEntity(spellId) { c ->
                    c.without<com.wingedsheep.engine.state.components.stack.SpellCopyTokenRidersComponent>()
                }
            }
        }

        // Aura: add reverse AttachmentsComponent on the enchanted permanent
        if (auraTargetId != null) {
            newState = newState.updateEntity(auraTargetId) { container ->
                val existing = container.get<com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent>()
                val updatedIds = (existing?.attachedIds ?: emptyList()) + spellId
                container.with(com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent(updatedIds))
            }
        }

        // Handle "enters the battlefield tapped" replacement effect
        // Note: payLifeCost shock lands are handled in resolvePermanentSpell before this method is called.
        if (cardDef != null && !spellComponent.castFaceDown) {
            val entersTapped = cardDef.script.replacementEffects.filterIsInstance<EntersTapped>().firstOrNull()
            if (entersTapped != null && entersTapped.payLifeCost == null) {
                val shouldEnterTapped = if (entersTapped.unlessCondition != null) {
                    val context = EffectContext(
                        sourceId = spellId,
                        controllerId = controllerId,
                    )
                    !com.wingedsheep.engine.handlers.ConditionEvaluator().evaluate(
                        newState, entersTapped.unlessCondition!!, context
                    )
                } else {
                    true
                }
                if (shouldEnterTapped) {
                    newState = newState.updateEntity(spellId) { c -> c.with(TappedComponent) }
                }
            }
        }

        // Handle "enters with counters" replacement effects (before adding to battlefield)
        val counterEvents = mutableListOf<GameEvent>()

        // CR 603.2e — an Aura entering attached to its enchant target "becomes attached"; emit the
        // event so attachment triggers (Eriette, the Beguiler) fire.
        if (auraTargetId != null) {
            counterEvents.add(
                com.wingedsheep.engine.core.PermanentAttachedEvent(
                    attachmentId = spellId,
                    attachmentName = cardComponent?.name ?: "Aura",
                    attachedToId = auraTargetId,
                    controllerId = controllerId,
                )
            )
        }

        if (cardDef != null && !spellComponent.castFaceDown) {
            val totalManaSpent = spellComponent.manaSpentWhite + spellComponent.manaSpentBlue +
                spellComponent.manaSpentBlack + spellComponent.manaSpentRed +
                spellComponent.manaSpentGreen + spellComponent.manaSpentColorless
            val (counterState, events) = applyEntersWithReplacements(
                newState, spellId, cardDef, controllerId, spellComponent.xValue, totalManaSpent
            )
            newState = counterState
            counterEvents.addAll(events)
        }

        // The Tomb of Aclazotz cast-this-way entry rider: a creature cast from the graveyard under
        // its grant enters with a finality counter and gains "Vampire" in addition to its other
        // types. Applied after the printed enters-with replacements so both stack cleanly (CR 614).
        if (graveyardCastRider != null && !spellComponent.castFaceDown) {
            val (riderState, riderEvents) =
                com.wingedsheep.engine.handlers.effects.EntersWithReplacements.applyCastFromGraveyardRider(
                    newState, spellId, controllerId,
                    graveyardCastRider.entersWithCounter, graveyardCastRider.addedSubtype
                )
            newState = riderState
            counterEvents.addAll(riderEvents)
        }

        // Handle the intrinsic entry counters of a planeswalker (starting loyalty, CR 306.5b) or a
        // battle (printed defense, CR 310.4b). This is the cast pipeline's entry point for those
        // intrinsic entry replacements — it runs here, while the permanent is still on the stack,
        // because resolution places permanents via addToZone rather than
        // ZoneTransitionService.moveToZone. Every other entry reaches the same shared
        // placeEntryCounters call through ZoneMovementUtils.applyIntrinsicEntryCountersIfNeeded.
        val intrinsicEntryCounters = if (cardDef != null && !spellComponent.castFaceDown) {
            when {
                cardDef.startingLoyalty != null ->
                    CounterTypeFilter.Loyalty to cardDef.startingLoyalty!!
                cardDef.startingDefense != null ->
                    com.wingedsheep.engine.mechanics.battle.Battles.DEFENSE_COUNTER to cardDef.startingDefense!!
                else -> null
            }
        } else null
        if (intrinsicEntryCounters != null) {
            val (entryCounterState, entryCounterEvents) = EntersWithReplacements.placeEntryCounters(
                newState, spellId, intrinsicEntryCounters.first, intrinsicEntryCounters.second,
                controllerId, cardComponent?.name ?: ""
            )
            newState = entryCounterState
            counterEvents.addAll(entryCounterEvents)
        }

        // Handle Class entering the battlefield (Rule 716)
        // Add ClassLevelComponent starting at level 1
        if (cardDef != null && !spellComponent.castFaceDown && cardDef.isClass) {
            newState = newState.updateEntity(spellId) { c ->
                c.with(ClassLevelComponent(currentLevel = 1))
            }
        }

        // Handle double-faced cards entering the battlefield (Rule 712)
        // A resolving DFC spell enters with the same face that was up on the stack (Rule 712.13).
        if (cardDef != null && !spellComponent.castFaceDown && cardDef.isDoubleFaced) {
            val backFace = cardDef.backFace!!
            newState = newState.updateEntity(spellId) { c ->
                c.with(
                    com.wingedsheep.engine.state.components.identity.DoubleFacedComponent(
                        frontCardDefinitionId = cardDef.name,
                        backCardDefinitionId = backFace.name,
                        currentFace = com.wingedsheep.engine.state.components.identity.DoubleFacedComponent.Face.FRONT
                    )
                )
            }

            newState = DayNightService.applyDayboundEntry(newState, cardRegistry, spellId)
        }

        // Handle Saga entering the battlefield (Rule 714.3a)
        // Add SagaComponent and initial lore counter (triggers chapter I detection)
        if (cardDef != null && !spellComponent.castFaceDown && cardDef.isSaga) {
            val current = newState.getEntity(spellId)?.get<CountersComponent>() ?: CountersComponent()
            // Mark chapter 1 as triggered since lore count will be 1
            val sagaComponent = SagaComponent(triggeredChapters = setOf(1))
            newState = newState.updateEntity(spellId) { c ->
                c.with(sagaComponent)
                    .with(current.withAdded(CounterType.LORE, 1))
            }
        }

        // Add to battlefield — clean up any may-play permission first (mirrors the same
        // cleanup done in resolveNonPermanentSpell before the card goes to the graveyard).
        newState = newState.removeMayPlayPermissionsForCard(spellId)
        newState = com.wingedsheep.engine.handlers.effects.BattlefieldEntry
            .place(newState, controllerId, spellId)

        // Global "[filter] enter tapped" replacements sourced from OTHER battlefield permanents
        // (Authority of the Consuls — "Creatures your opponents control enter tapped"). The
        // self-only EntersTapped handled earlier covers a permanent's own printed clause; a
        // permanent cast normally must ALSO be tapped by another permanent's global
        // PermanentsEnterTapped, matching the PlayLand (PlayLandHandler) and moveToZone /
        // reanimation (ZoneTransitionService) paths that already consult it. Checked after the
        // entity is on the battlefield so its controller/type resolve for the filter. CR 614: an
        // applicable "enters untapped" replacement still wins, and a self-EntersTapped that already
        // tapped it stands. Sneak sets its own tapped-and-attacking state below, so skip it here.
        if (cardDef != null && !spellComponent.castFaceDown && !spellComponent.wasSneaked) {
            val alreadyTapped = newState.getEntity(spellId)?.has<TappedComponent>() == true
            val entersUntapped = com.wingedsheep.engine.handlers.effects.EnterUntappedReplacements
                .entersUntapped(newState, spellId, controllerId)
            if (!alreadyTapped && !entersUntapped &&
                com.wingedsheep.engine.handlers.effects.EnterTappedReplacements
                    .entersTapped(newState, spellId, controllerId)
            ) {
                newState = newState.updateEntity(spellId) { c -> c.with(TappedComponent) }
            }
        }

        // Sneak (CR 702.190b / 506.3a): a permanent spell whose sneak cost was paid enters
        // tapped and attacking the same player or planeswalker the returned unblocked creature
        // was attacking. A non-creature permanent can't attack, so it just enters tapped (506.3a).
        if (spellComponent.wasSneaked) {
            newState = newState.updateEntity(spellId) { c -> c.with(TappedComponent) }
            val projected = newState.projectedState
            // CR 506.3c: the creature only enters attacking if the carried defender is still a
            // legal attack target — an opponent still in the game, or an opponent's planeswalker
            // still on the battlefield (mirrors the defender check in AttackPhaseManager). If it's
            // no longer valid, the creature enters but is never attacking — no redirect.
            val legalDefender = spellComponent.sneakAttackDefenderId?.takeIf { d ->
                (d in newState.turnOrder && d != controllerId) ||
                    (projected.isPlaneswalker(d) &&
                        d in newState.getBattlefield() &&
                        projected.getController(d) != controllerId)
            }
            if (legalDefender != null && projected.isCreature(spellId)) {
                newState = newState.updateEntity(spellId) { c ->
                    c.with(AttackingComponent(legalDefender))
                }
            }
        }

        // For Rooms cast a half (CR 709.5d/h): the cast face's door becomes unlocked
        // on ETB. Emit a DoorUnlockedEvent so face-scoped "When you unlock this door"
        // triggers fire from the cast-time unlock too.
        val castFaceRoomComp = newState.getEntity(spellId)
            ?.get<com.wingedsheep.engine.state.components.identity.RoomComponent>()
        if (castFaceRoomComp != null && castFaceRoomComp.unlocked.size == 1) {
            val unlockedFace = castFaceRoomComp.faces.first { it.id in castFaceRoomComp.unlocked }
            counterEvents.add(
                com.wingedsheep.engine.core.DoorUnlockedEvent(
                    roomId = spellId,
                    roomName = cardComponent?.name ?: unlockedFace.name,
                    faceId = unlockedFace.id,
                    faceName = unlockedFace.name,
                    controllerId = controllerId,
                    becameFullyUnlocked = castFaceRoomComp.isFullyUnlocked
                )
            )
            if (castFaceRoomComp.isFullyUnlocked) {
                counterEvents.add(
                    com.wingedsheep.engine.core.RoomFullyUnlockedEvent(
                        roomId = spellId,
                        roomName = cardComponent?.name ?: unlockedFace.name,
                        controllerId = controllerId
                    )
                )
            }
        }

        // Warp: create delayed trigger to exile at beginning of next end step. Snapshot the
        // permanent's battlefield-entry timestamp so the exile only affects this battlefield
        // object — if the permanent leaves and re-enters before the trigger resolves (blink),
        // it's a new object the delayed trigger no longer tracks (CR 603.7c / 400.7).
        if (spellComponent.wasWarped) {
            val entryTimestamp = newState.getEntity(spellId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent>()
                ?.timestamp
            val delayedTrigger = DelayedTriggeredAbility(
                id = java.util.UUID.randomUUID().toString(),
                effect = WarpExileEffect(
                    target = EffectTarget.SpecificEntity(spellId),
                    enteredBattlefieldTimestamp = entryTimestamp
                ),
                fireAtStep = Step.END,
                sourceId = spellId,
                sourceName = cardComponent?.name ?: "Unknown",
                controllerId = controllerId
            )
            newState = newState.addDelayedTrigger(delayedTrigger)
        }

        // Dash (CR 702.109a): create delayed trigger to return this permanent to its owner's
        // hand at the beginning of the next end step. Same blink-safety shape as warp above.
        if (spellComponent.wasDashed) {
            val entryTimestamp = newState.getEntity(spellId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent>()
                ?.timestamp
            val delayedTrigger = DelayedTriggeredAbility(
                id = java.util.UUID.randomUUID().toString(),
                effect = MoveTrackedBattlefieldObjectEffect(
                    target = EffectTarget.SpecificEntity(spellId),
                    destination = Zone.HAND,
                    enteredBattlefieldTimestamp = entryTimestamp
                ),
                fireAtStep = Step.END,
                sourceId = spellId,
                sourceName = cardComponent?.name ?: "Unknown",
                controllerId = controllerId
            )
            newState = newState.addDelayedTrigger(delayedTrigger)
        }

        // Prepared (Secrets of Strixhaven): a preparation creature whose face carries the PREPARED
        // keyword ("This creature enters prepared") enters prepared. Becoming prepared creates a
        // copy of the card's prepare spell in exile that its controller may cast (paying that
        // spell's cost); casting it unprepares the creature. PREPARE-layout creatures that lack the
        // keyword (e.g. Leech Collector, which only becomes prepared via a trigger) do not enter
        // prepared.
        if (cardDef != null && cardDef.layout == com.wingedsheep.sdk.model.CardLayout.PREPARE &&
            cardDef.keywords.contains(com.wingedsheep.sdk.core.Keyword.PREPARED) &&
            !spellComponent.castFaceDown
        ) {
            newState = PreparationLogic.makePrepared(newState, spellId, cardDef, controllerId)
        }

        return newState to counterEvents
    }


    /**
     * Rebound (CR 702.88): a spell has rebound if the printed keyword is on its card definition
     * or the keyword was granted to this stack object (Ojer Pakpatiq via GrantKeywordToSpellEffect,
     * stored on [SpellGrantedKeywordsComponent]). Only matters for a spell cast from hand — the
     * caller gates on [com.wingedsheep.engine.state.components.stack.SpellOnStackComponent.castFromZone].
     */
    private fun spellHasRebound(
        state: GameState,
        spellId: EntityId,
        cardDef: com.wingedsheep.sdk.model.CardDefinition?
    ): Boolean {
        if (cardDef?.keywords?.contains(com.wingedsheep.sdk.core.Keyword.REBOUND) == true) return true
        val granted = state.getEntity(spellId)
            ?.get<com.wingedsheep.engine.state.components.stack.SpellGrantedKeywordsComponent>()
        return granted?.keywords?.contains(com.wingedsheep.sdk.core.Keyword.REBOUND.name) == true
    }

    /**
     * Schedule rebound's delayed triggered ability (CR 702.88a): at the beginning of the caster's
     * next upkeep, they may cast the just-exiled card from exile without paying its mana cost.
     * A one-shot step-based delayed trigger gated to the caster's turn ([fireOnPlayerId]) and to a
     * later turn ([notBeforeTurn]); it is consumed the first time it fires. The free cast reuses the
     * suspend/Shiko cast-from-exile pipeline ([CastFromCollectionWithoutPayingCostEffect]).
     */
    private fun scheduleReboundRecast(
        state: GameState,
        exiledCardId: EntityId,
        casterId: EntityId,
        sourceName: String
    ): GameState = state.addDelayedTrigger(
        com.wingedsheep.engine.event.DelayedTriggeredAbility(
            id = java.util.UUID.randomUUID().toString(),
            effect = com.wingedsheep.sdk.scripting.effects.MayEffect(
                com.wingedsheep.sdk.scripting.effects.CompositeEffect(
                    listOf(
                        com.wingedsheep.sdk.scripting.effects.GatherCardsEffect(
                            source = com.wingedsheep.sdk.scripting.effects.CardSource.Self,
                            storeAs = "rebound_recast",
                        ),
                        com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect(
                            from = "rebound_recast",
                        ),
                    )
                ),
                descriptionOverride = "cast this card from exile without paying its mana cost",
            ),
            fireAtStep = com.wingedsheep.sdk.core.Step.UPKEEP,
            fireOnPlayerId = casterId,
            notBeforeTurn = state.turnNumber + 1,
            sourceId = exiledCardId,
            sourceName = sourceName,
            controllerId = casterId,
        )
    )

    /**
     * Resolve a non-permanent spell - execute effects, put in graveyard.
     */
    private fun resolveNonPermanentSpell(
        state: GameState,
        spellId: EntityId,
        spellComponent: SpellOnStackComponent,
        cardComponent: CardComponent?,
        targets: List<ChosenTarget>,
        // Parallel to the originally-chosen targets, with `null` in slots whose target
        // was dropped by 608.2b validation. Used for [EffectContext.buildNamedTargets]
        // so BoundVariable lookups for now-illegal targets resolve to null and fizzle,
        // rather than shifting onto a later still-valid target.
        alignedTargets: List<ChosenTarget?> = targets,
    ): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        // Execute the spell effect if present, applying text replacement if the spell
        // was modified by a text-changing effect (e.g., Artificial Evolution)
        // Use kickerSpellEffect when the spell was kicked and an alternate effect is defined.
        // Adventure / split face cast (CR 715 / 709) — when the spell was cast as a face, read
        // the face's spell effect from `cardDef.cardFaces[faceIndex].script.spellEffect`.
        val resolvedCardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
        val faceSpellEffect = spellComponent.faceIndex?.let { idx ->
            resolvedCardDef?.cardFaces?.getOrNull(idx)?.script?.spellEffect
        }
        val baseSpellEffect = when {
            faceSpellEffect != null -> faceSpellEffect
            spellComponent.declaredCostSlot != null && cardComponent != null ->
                resolvedCardDef?.script?.kickerSpellEffect ?: cardComponent.spellEffect
            // Cleave (CR 702.148): a spell cast for its cleave cost resolves with its
            // brackets-removed effect variant, applied structurally at cast time rather than by
            // editing text — so e.g. a bracketed delayed-trigger clause is never created.
            spellComponent.wasCleaved && cardComponent != null ->
                resolvedCardDef?.script?.cleaveSpellEffect ?: cardComponent.spellEffect
            else -> cardComponent?.spellEffect
        }
        val rawSpellEffect = baseSpellEffect
        val textReplacement = state.getEntity(spellId)?.get<TextReplacementComponent>()
        val spellEffect = if (rawSpellEffect != null && textReplacement != null) {
            rawSpellEffect.applyTextReplacement(textReplacement)
        } else {
            rawSpellEffect
        }
        // Splice (CR 702.47): the spliced cards' text is a tail that runs after the main spell's own
        // effects (CR 702.47b). Its targets were appended to the end of the flat list at cast time, so
        // the same tail is peeled off here — the main spell must see only its own targets, or an effect
        // that consumes "all targets" would swallow the spliced card's as well.
        // A spell with no effect of its own can't be a splice host in practice (a splice card is
        // spliced onto a spell that has text), so the tail lives inside the `spellEffect != null` guard.
        val spliceEntries = buildSpliceEntries(spellComponent)
        val splicedRequirementCount = spellComponent.splicedCardNames.sumOf { name ->
            cardRegistry.getCard(name)?.script?.targetRequirements?.size ?: 0
        }
        val splicedSlotCount = SpliceCasts
            .splicedTargetSlotCounts(spellComponent.splicedCardNames, cardRegistry).sum()

        if (spellEffect != null) {
            val allTargetRequirements = state.getEntity(spellId)?.get<TargetsComponent>()?.targetRequirements ?: emptyList()
            // Requirements are never filtered, so the tail comes straight off the end.
            val targetRequirements = allTargetRequirements.dropLast(splicedRequirementCount)
            // The tail is dropped from `alignedTargets`, NOT from `targets`: only the aligned list is
            // position-preserving (null wherever 608.2b dropped a target), so it is the one whose last
            // `splicedSlotCount` entries are reliably the spliced cards'. `targets` is the already-
            // filtered, shorter list — dropping from *it* would eat a main-spell target whenever any
            // target had been dropped, silently shifting positional references like ContextTarget(n).
            // The main spell's own live targets are then just its aligned slots that survived.
            // Both expressions are deliberately identity when nothing was spliced.
            val mainAlignedTargets =
                if (splicedSlotCount == 0) alignedTargets else alignedTargets.dropLast(splicedSlotCount)
            val mainTargets =
                if (splicedSlotCount == 0) targets else mainAlignedTargets.filterNotNull()
            val context = EffectContext(
                sourceId = spellId,
                controllerId = spellComponent.casterId,
                targets = mainTargets,
                // Position-preserving view (null in slots dropped by 608.2b) so positional
                // references — ContextTarget(n), EntityReference.Target(n), ContextPlayer(n) —
                // resolve by ORIGINAL slot and don't shift onto a later still-valid target.
                alignedTargets = mainAlignedTargets,
                // A pay-X-life additional cost (AdditionalCost.PayXLife, e.g. Vicious Rivalry) feeds
                // its declared X through the same X slot read by DynamicAmount.XValue and the
                // ManaValue*X predicates. Such a card never also carries an {X} mana cost, so
                // coalescing is unambiguous (CR 601.2b — the value is locked in as the spell is cast).
                xValue = spellComponent.xValue ?: spellComponent.additionalCostPayXLifeAmount,
                totalManaSpent = spellComponent.manaSpentWhite + spellComponent.manaSpentBlue +
                    spellComponent.manaSpentBlack + spellComponent.manaSpentRed +
                    spellComponent.manaSpentGreen + spellComponent.manaSpentColorless,
                manaSpentOnXByColor = spellComponent.manaSpentOnXByColor,
                declaredCostSlot = spellComponent.declaredCostSlot,
                wasBlightPaid = spellComponent.wasBlightPaid,
                wasWaterbendPaid = spellComponent.wasWaterbendPaid,
                wasSneaked = spellComponent.wasSneaked,
                wasWebSlung = spellComponent.wasWebSlung,
                wasMayhem = spellComponent.wasMayhem,
                sacrificedPermanents = spellComponent.sacrificedPermanents,
                discardedAsCostCards = spellComponent.discardedAsCostCards,
                chosenEntitySnapshots = spellComponent.chosenEntitySnapshots,
                damageDistribution = spellComponent.damageDistribution,
                chosenModes = spellComponent.chosenModes,
                modeTargetsOrdered = spellComponent.modeTargetsOrdered,
                modeTargetRequirements = spellComponent.modeTargetRequirements,
                chosenCreatureType = spellComponent.chosenCreatureType,
                exiledCardCount = spellComponent.exiledCardCount,
                additionalCostBlightAmount = spellComponent.additionalCostBlightAmount,
                castFromZone = spellComponent.castFromZone,
                pipeline = PipelineState(
                    // Use the positionally-aligned validated list so a sub-effect that
                    // references a target dropped by 608.2b through its BoundVariable id
                    // resolves to null and fizzles (CR 608.2b).
                    namedTargets = EffectContext.buildNamedTargets(targetRequirements, mainAlignedTargets),
                    storedCollections = buildBeheldStoredCollections(spellComponent.beheldCards, resolvedCardDef)
                )
            )

            // Pre-push the splice tail so it runs whether the main spell's effect finishes here or
            // pauses for a decision of its own — the frame sits beneath the inner decision's frames
            // and auto-resumes once they finish (CR 702.47b: main spell first, then the spliced text).
            val stateForMainEffect = if (spliceEntries.isNotEmpty()) {
                newState.pushContinuation(
                    SpliceTailContinuation(
                        decisionId = "splice-tail-${java.util.UUID.randomUUID()}",
                        controllerId = spellComponent.casterId,
                        sourceId = spellId,
                        sourceName = cardComponent?.name,
                        remainingEntries = spliceEntries
                    )
                )
            } else newState

            var effectResult = effectHandler.execute(stateForMainEffect, spellEffect, context)

            // Main spell done and nothing paused — pop the pre-pushed frame and run the spliced text
            // inline, so the whole resolution stays one ExecutionResult.
            if (spliceEntries.isNotEmpty() && !effectResult.isPaused && effectResult.error == null) {
                val (_, afterPop) = effectResult.state.popContinuation()
                val tail = processPreTargetedEffectQueue(
                    state = afterPop,
                    entries = spliceEntries,
                    ctx = PreTargetedEffectContext(
                        controllerId = spellComponent.casterId,
                        sourceId = spellId,
                        sourceName = cardComponent?.name,
                        xValue = null,
                        triggeringEntityId = null
                    ),
                    effectExecutor = { s, e, c -> effectHandler.execute(s, e, c) },
                    targetValidator = spliceTargetValidator,
                    accumulatedEvents = effectResult.events
                )
                effectResult = tail
            }

            // If effect is paused awaiting a decision, we still need to move the spell
            // to graveyard/exile (it has already resolved from the stack). The decision only
            // determines how the effect completes.
            if (effectResult.isPaused) {
                val pausedIsCopy = effectResult.state.getEntity(spellId)?.has<CopyOfComponent>() == true
                if (pausedIsCopy) {
                    // Rule 112.3b — copies cease to exist when they leave the stack.
                    val pausedState = effectResult.state.removeEntity(spellId)
                    return ExecutionResult.paused(
                        pausedState,
                        effectResult.pendingDecision!!,
                        events + effectResult.events
                    )
                }

                val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
                val pausedCardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
                // For a cast face (Adventure / modal DFC), "Exile <name>." lives on the face's script.
                val pausedResolvedScript = spellComponent.faceIndex?.let { pausedCardDef?.cardFaces?.getOrNull(it)?.script }
                    ?: pausedCardDef?.script

                // Esper Origins: a graveyard-cast that returns itself to the battlefield transformed
                // does so even when its resolution paused mid-way (e.g. the Surveil earlier in the
                // same resolution). The card leaves the stack and enters transformed now; the paused
                // continuation still resolves the remaining effects. Precedence over flashback exile.
                val pausedReturnTransformed = pausedResolvedScript?.returnTransformedFromGraveyardOnResolve
                if (pausedReturnTransformed != null && spellComponent.castFromZone == Zone.GRAVEYARD) {
                    val transformEvents = mutableListOf<GameEvent>()
                    val transformed = resolveSelfToBattlefieldTransformed(
                        effectResult.state, spellId, pausedReturnTransformed.counters, transformEvents
                    )
                    if (transformed != null) {
                        return ExecutionResult.paused(
                            transformed,
                            effectResult.pendingDecision!!,
                            events + effectResult.events + transformEvents
                        )
                    }
                }

                val pausedSelfExile = pausedResolvedScript?.selfExileOnResolve == true
                // Flashback (printed or granted — Archmage's Newt) or Harmonize (printed or granted
                // — Songcrafter Mage): a graveyard cast exiles on resolution instead of returning
                // to the graveyard.
                val pausedFlashbackExile = spellComponent.castFromZone == Zone.GRAVEYARD &&
                    (FlashbackGrants.effectiveFlashback(
                        state, spellId, pausedCardDef, spellComponent.casterId, cardRegistry, predicateEvaluator
                    ) != null ||
                        HarmonizeGrants.effectiveHarmonize(state, spellId, pausedCardDef) != null)
                val pausedExileAfterResolveComp = effectResult.state.getEntity(spellId)?.get<AfterResolveDestinationComponent>()
                val pausedAdventureFaceExile = pausedCardDef?.layout == com.wingedsheep.sdk.model.CardLayout.ADVENTURE &&
                    spellComponent.faceIndex != null
                val pausedOmenFaceShuffle = pausedCardDef?.layout == com.wingedsheep.sdk.model.CardLayout.OMEN &&
                    spellComponent.faceIndex != null
                val pausedReboundExile = spellComponent.castFromZone == Zone.HAND &&
                    spellHasRebound(effectResult.state, spellId, pausedCardDef)
                val pausedIntended = when {
                    // The cast-this-way rider is the most specific instruction on this one spell,
                    // so it outranks the card-intrinsic exile reasons below rather than being
                    // OR'd into them — it is the only one that can name a zone other than exile.
                    pausedExileAfterResolveComp != null -> pausedExileAfterResolveComp.zone
                    pausedSelfExile || pausedFlashbackExile || pausedAdventureFaceExile || pausedReboundExile -> Zone.EXILE
                    pausedOmenFaceShuffle -> Zone.LIBRARY
                    else -> Zone.GRAVEYARD
                }

                // Apply RedirectZoneChange replacement effects (e.g., Festival of Embers).
                val pausedRedirect = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.checkZoneChangeRedirect(
                    effectResult.state, spellId, Zone.STACK, pausedIntended
                )
                val pausedDestZone = pausedRedirect.destinationZone
                val pausedDestZoneKey = ZoneKey(ownerId, pausedDestZone)

                // Move spell to graveyard/exile even though effect is paused
                var pausedState = effectResult.state.updateEntity(spellId) { c ->
                    c.without<SpellOnStackComponent>().without<TargetsComponent>()
                }
                pausedState = pausedState.addToZone(pausedDestZoneKey, spellId)

                // Paradigm: tag the just-exiled spell even when its effect paused mid-resolution.
                if (pausedDestZone == Zone.EXILE && pausedResolvedScript?.paradigm == true) {
                    pausedState = pausedState.updateEntity(spellId) { c ->
                        c.with(com.wingedsheep.engine.state.components.battlefield.ParadigmComponent)
                    }
                }

                // Rebound: arm the next-upkeep free recast even when the effect paused mid-resolution.
                if (pausedReboundExile && pausedDestZone == Zone.EXILE) {
                    pausedState = scheduleReboundRecast(
                        pausedState, spellId, spellComponent.casterId, cardComponent?.name ?: "Unknown"
                    )
                }

                // Link an opponent's resolving spell exiled by a RedirectZoneChange(linkToSource)
                // replacement (Valgavoth) even when the effect paused mid-resolution.
                if (pausedDestZone == Zone.EXILE && pausedRedirect.linkSourceId != null) {
                    pausedState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                        .linkExiledToSource(pausedState, spellId, pausedRedirect.linkSourceId)
                }

                // CR 715.3d — Adventure exiled by its own resolution: re-grant cast-from-exile.
                if (pausedAdventureFaceExile && pausedDestZone == Zone.EXILE) {
                    val (permId, stateWithPerm) = pausedState.newEntity()
                    pausedState = stateWithPerm.addMayPlayPermission(
                        com.wingedsheep.engine.state.permissions.MayPlayPermission(
                            id = permId,
                            cardIds = setOf(spellId),
                            controllerId = spellComponent.casterId,
                            permanent = true,
                            timestamp = state.timestamp,
                        )
                    )
                }

                val pausedCounterEvents = mutableListOf<GameEvent>()
                if (pausedDestZone == Zone.EXILE && pausedExileAfterResolveComp != null && pausedExileAfterResolveComp.withCounters.isNotEmpty()) {
                    pausedState = applyExileCounters(pausedState, spellId, pausedExileAfterResolveComp.withCounters, pausedCounterEvents)
                }

                // Omen (Tarkir: Dragonstorm): shuffle the just-added card into its owner's library.
                if (pausedOmenFaceShuffle && pausedDestZone == Zone.LIBRARY) {
                    pausedState = shuffleOwnerLibrary(pausedState, ownerId)
                    pausedCounterEvents.add(LibraryShuffledEvent(ownerId))
                }

                pausedRedirect.additionalEffect?.let { extra ->
                    val (updatedState, extraEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.applyReplacementAdditionalEffect(
                        pausedState, extra, pausedRedirect.effectControllerId, spellId,
                        sourceId = pausedRedirect.effectSourceId
                    )
                    pausedState = updatedState
                    pausedCounterEvents.addAll(extraEvents)
                }

                // Include the zone change event along with effect events
                val allEvents = events + effectResult.events + ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    pausedDestZone,
                    ownerId
                ) + pausedCounterEvents

                return ExecutionResult.paused(
                    pausedState,
                    effectResult.pendingDecision!!,
                    allEvents
                )
            }

            // Always apply state changes from effect execution, even on partial
            // failure. Per MTG rules, when a spell resolves, you do as much as
            // possible. Partial state changes (e.g., first target destroyed but
            // second target missing) should be preserved.
            newState = effectResult.newState
            events.addAll(effectResult.events)
        }

        // Rule 112.3b: a copy of a spell ceases to exist when it leaves the stack —
        // it does not go to a graveyard or exile.
        val isCopy = newState.getEntity(spellId)?.has<CopyOfComponent>() == true
        if (isCopy) {
            newState = newState.removeEntity(spellId)
            return ExecutionResult.success(newState, events)
        }

        // Move to graveyard (or exile if selfExileOnResolve, flashback, or AfterResolveDestinationComponent)
        val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
        val cardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
        // For a cast face (Adventure / modal DFC), "Exile <name>." lives on the face's script.
        val resolvedScript = spellComponent.faceIndex?.let { cardDef?.cardFaces?.getOrNull(it)?.script }
            ?: cardDef?.script

        // Esper Origins: a spell cast from a graveyard is put onto the battlefield transformed
        // instead of going to the graveyard. Gated on the same graveyard cast as the flashback
        // exile below and takes precedence over it. Falls through to the normal destination if the
        // card can't enter transformed (non-DFC or non-permanent back face — official ruling).
        val returnTransformedSpec = resolvedScript?.returnTransformedFromGraveyardOnResolve
        if (returnTransformedSpec != null && spellComponent.castFromZone == Zone.GRAVEYARD) {
            val transformed = resolveSelfToBattlefieldTransformed(
                newState, spellId, returnTransformedSpec.counters, events
            )
            if (transformed != null) {
                return ExecutionResult.success(transformed, events)
            }
        }

        val selfExile = resolvedScript?.selfExileOnResolve == true
        // Flashback (printed or granted — Archmage's Newt) or Harmonize (printed or granted —
        // Songcrafter Mage): a graveyard cast exiles on resolution instead of returning to the
        // graveyard.
        val flashbackExile = spellComponent.castFromZone == Zone.GRAVEYARD &&
            (FlashbackGrants.effectiveFlashback(
                state, spellId, cardDef, spellComponent.casterId, cardRegistry, predicateEvaluator
            ) != null ||
                HarmonizeGrants.effectiveHarmonize(state, spellId, cardDef) != null)
        val exileAfterResolveComp = newState.getEntity(spellId)?.get<AfterResolveDestinationComponent>()
        // Adventure face (CR 715.3d): when an Adventure resolves, exile it instead of putting
        // it in its owner's graveyard, and grant the caster permission to cast it as the
        // creature spell while it remains exiled.
        val adventureFaceExile = cardDef?.layout == com.wingedsheep.sdk.model.CardLayout.ADVENTURE &&
            spellComponent.faceIndex != null
        // Omen face (Tarkir: Dragonstorm): when an Omen resolves, shuffle it into its owner's
        // library instead of putting it in the graveyard. No cast-from-exile linkage.
        val omenFaceShuffle = cardDef?.layout == com.wingedsheep.sdk.model.CardLayout.OMEN &&
            spellComponent.faceIndex != null
        // Rebound (CR 702.88): a spell cast from hand that has rebound (printed or granted) exiles
        // on resolution instead of going to the graveyard, and arms a next-upkeep free recast.
        val reboundExile = spellComponent.castFromZone == Zone.HAND &&
            spellHasRebound(newState, spellId, cardDef)
        val intendedDestination = when {
            // See the paused-resolve twin above: the rider wins over the intrinsic exile reasons
            // because it is the only one that can send the card somewhere other than exile
            // (Kylox's Voltstrider — "put it on the bottom of its owner's library instead").
            exileAfterResolveComp != null -> exileAfterResolveComp.zone
            selfExile || flashbackExile || adventureFaceExile || reboundExile -> Zone.EXILE
            omenFaceShuffle -> Zone.LIBRARY
            else -> Zone.GRAVEYARD
        }

        // Apply RedirectZoneChange replacement effects (e.g., Festival of Embers
        // exiles cards that would go to your graveyard from anywhere).
        val redirect = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.checkZoneChangeRedirect(
            newState, spellId, Zone.STACK, intendedDestination
        )
        val destinationZone = redirect.destinationZone
        val destZoneKey = ZoneKey(ownerId, destinationZone)

        newState = newState.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>()
                .without<TargetsComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent>()
                .without<AfterResolveDestinationComponent>()
        }
        newState = newState.removeMayPlayPermissionsForCard(spellId)
        newState = newState.addToZone(destZoneKey, spellId)

        // Paradigm (Secrets of Strixhaven): tag the just-exiled spell so the engine synthesizes its
        // recurring precombat-main free-recast ability (Paradigm.recastAbility). The marker is the
        // gate — a Lesson exiled by any other path carries no marker and so never recurs.
        if (destinationZone == Zone.EXILE && resolvedScript?.paradigm == true) {
            newState = newState.updateEntity(spellId) { c ->
                c.with(com.wingedsheep.engine.state.components.battlefield.ParadigmComponent)
            }
        }

        // Rebound (CR 702.88a): arm the caster's next-upkeep free recast of the just-exiled card.
        if (reboundExile && destinationZone == Zone.EXILE) {
            newState = scheduleReboundRecast(
                newState, spellId, spellComponent.casterId, cardComponent?.name ?: "Unknown"
            )
        }

        // CR 715.3d — an Adventure card exiled by its own resolution may be cast as the creature
        // by the spell's controller while it remains in exile. Re-add the permission after
        // the prior removeMayPlayPermissionsForCard so the cast-from-exile enumerator picks
        // it up on the next priority pass.
        if (adventureFaceExile && destinationZone == Zone.EXILE) {
            val (permId, stateWithPerm) = newState.newEntity()
            newState = stateWithPerm.addMayPlayPermission(
                com.wingedsheep.engine.state.permissions.MayPlayPermission(
                    id = permId,
                    cardIds = setOf(spellId),
                    controllerId = spellComponent.casterId,
                    permanent = true,
                    timestamp = state.timestamp,
                )
            )
        }

        // Omen (Tarkir: Dragonstorm): the card was just added to the bottom of its owner's
        // library above — now shuffle that library and announce it.
        if (omenFaceShuffle && destinationZone == Zone.LIBRARY) {
            newState = shuffleOwnerLibrary(newState, ownerId)
            events.add(LibraryShuffledEvent(ownerId))
        }

        // Add counters granted by AfterResolveDestinationComponent (e.g., Goliath Daydreamer's dream counter).
        if (destinationZone == Zone.EXILE && exileAfterResolveComp != null && exileAfterResolveComp.withCounters.isNotEmpty()) {
            newState = applyExileCounters(newState, spellId, exileAfterResolveComp.withCounters, events)
        }

        // Make the exiled card plotted (Lilah, Undefeated Slickshot): "exile that spell instead of
        // putting it into your graveyard as it resolves. If you do, it becomes plotted."
        if (destinationZone == Zone.EXILE && exileAfterResolveComp?.makePlotted == true) {
            newState = applyPlottedToExiledCard(newState, spellId, ownerId, cardComponent?.name ?: "Unknown", events)
        }

        // Link the exiled spell back to the source permanent (Goliath Daydreamer)
        // so the UI can display it tethered under the source and so the attack-trigger
        // free-cast ability can find it via the linked-exile pile.
        if (destinationZone == Zone.EXILE && exileAfterResolveComp?.linkedSourceId != null) {
            val sourceId = exileAfterResolveComp.linkedSourceId
            if (newState.getEntity(sourceId) != null) {
                newState = newState.updateEntity(sourceId) { c ->
                    val existing = c.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
                    val updated = (existing?.exiledIds ?: emptyList()) + spellId
                    c.with(com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent(updated))
                }
            }
        }

        // Link an opponent's resolving spell exiled by a RedirectZoneChange(linkToSource)
        // replacement (Valgavoth, Terror Eater) so its controller may later play it.
        if (destinationZone == Zone.EXILE && redirect.linkSourceId != null) {
            newState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .linkExiledToSource(newState, spellId, redirect.linkSourceId)
        }

        redirect.additionalEffect?.let { extra ->
            val (updatedState, extraEvents) = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils.applyReplacementAdditionalEffect(
                newState, extra, redirect.effectControllerId, spellId,
                sourceId = redirect.effectSourceId
            )
            newState = updatedState
            events.addAll(extraEvents)
        }

        events.add(
            ZoneChangeEvent(
                spellId,
                cardComponent?.name ?: "Unknown",
                null,
                destinationZone,
                ownerId
            )
        )

        return ExecutionResult.success(newState, events)
    }

    /**
     * Shuffle [ownerId]'s library after an Omen spell has been added to it on resolution
     * (Tarkir: Dragonstorm — "then shuffle this card into its owner's library"). Mirrors
     * [com.wingedsheep.engine.handlers.effects.library.ShuffleLibraryExecutor]: clears any
     * known top-of-library positions before shuffling, then advances the deterministic RNG.
     * The caller is responsible for emitting the [LibraryShuffledEvent].
     */
    private fun shuffleOwnerLibrary(state: GameState, ownerId: EntityId): GameState {
        val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
        val cleared = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
            .clearLibraryReveals(state, ownerId)
        val (library, advanced) = cleared.nextRandom { shuffle(cleared.getZone(libraryZone)) }
        return advanced.copy(zones = advanced.zones + (libraryZone to library))
    }

    /**
     * Resolution destination for [com.wingedsheep.sdk.model.CardScript.returnTransformedFromGraveyardOnResolve]
     * (Esper Origins): a spell cast from a graveyard is put onto the battlefield **transformed**
     * (its back face up) under its owner's control, entering with [counters], instead of going to
     * the graveyard/exile.
     *
     * Faithful to "exile it, then put it onto the battlefield transformed ... with a finality counter":
     * the resolved card leaves the stack and a brand-new back-face object enters the battlefield
     * (leaves/enters triggers fire, a Saga back enters with a fresh lore counter). The intermediate
     * exile is invisible — no effect keys on it — so the stack → battlefield move is done directly.
     *
     * Per the official ruling, a card that is not double-faced (or whose back face is not a permanent)
     * "will not enter at all"; [returnDfcFace] no-ops in that case and the caller must fall
     * back to the normal graveyard/exile destination.
     */
    private fun resolveSelfToBattlefieldTransformed(
        state: GameState,
        spellId: EntityId,
        counters: List<CounterType>,
        events: MutableList<GameEvent>
    ): GameState? {
        val container = state.getEntity(spellId) ?: return null
        val cardComponent = container.get<CardComponent>() ?: return null
        val ownerId = cardComponent.ownerId ?: return null
        val cardDef = cardRegistry.getCard(cardComponent.name) ?: return null
        val backFace = cardDef.backFace ?: return null
        // A non-permanent back face can't be put onto the battlefield — no-op, caller falls back.
        if (!backFace.isPermanent) return null

        // Strip the on-stack bookkeeping (and any alternative-cost permissions) before the card
        // becomes a permanent, mirroring the normal resolved-spell cleanup.
        var working = state.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>()
                .without<TargetsComponent>()
                .without<PlayWithoutPayingCostComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithCostIncreaseComponent>()
                .without<com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent>()
                .without<AfterResolveDestinationComponent>()
        }
        working = working.removeMayPlayPermissionsForCard(spellId)

        // "Exile it, then put it onto the battlefield transformed": the resolving spell was already
        // popped off the stack (it is in no zone), so place it in its owner's exile — the source
        // zone [returnDfcFace] is built to flip-and-return from.
        working = working.addToZone(ZoneKey(ownerId, Zone.EXILE), spellId)

        // A DFC spell on the stack carries no DoubleFacedComponent yet (it's stamped on ETB); add
        // one on its front face so returnDfcFace can flip it to the back face.
        if (working.getEntity(spellId)?.get<DoubleFacedComponent>() == null) {
            working = working.updateEntity(spellId) { c ->
                c.with(
                    DoubleFacedComponent(
                        frontCardDefinitionId = cardDef.name,
                        backCardDefinitionId = backFace.name,
                        currentFace = DoubleFacedComponent.Face.FRONT
                    )
                )
            }
        }

        val transition = returnDfcFace(working, cardRegistry, spellId, DoubleFacedComponent.Face.BACK)
        working = transition.state
        events.addAll(transition.events)

        // The finality counter (and any others) land on the new back-face permanent.
        if (counters.isNotEmpty()) {
            working = applyExileCounters(working, spellId, counters, events)
        }
        return working
    }

    /**
     * Add counters to a card that was just exiled because of AfterResolveDestinationComponent.
     * Used by Goliath Daydreamer to put a dream counter on cast spells as they're exiled.
     */
    private fun applyExileCounters(
        state: GameState,
        cardId: EntityId,
        counters: List<com.wingedsheep.sdk.core.CounterType>,
        events: MutableList<GameEvent>
    ): GameState {
        val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: ""
        var updated = state
        for (counterType in counters) {
            updated = updated.updateEntity(cardId) { c ->
                val current = c.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                    ?: com.wingedsheep.engine.state.components.battlefield.CountersComponent()
                c.with(current.withAdded(counterType, 1))
            }
            events.add(CountersAddedEvent(cardId, counterType.name, 1, cardName))
        }
        return updated
    }

    /**
     * Spell fizzles because all targets are invalid.
     */
    private fun fizzleSpell(
        state: GameState,
        spellId: EntityId,
        cardComponent: CardComponent?,
        spellComponent: SpellOnStackComponent
    ): ExecutionResult {
        // Rule 112.3b — a copy that fizzles ceases to exist rather than moving to graveyard/exile.
        val isCopy = state.getEntity(spellId)?.has<CopyOfComponent>() == true
        if (isCopy) {
            val newState = state.removeEntity(spellId)
            return ExecutionResult.success(
                newState,
                listOf(
                    SpellFizzledEvent(spellId, cardComponent?.name ?: "Unknown", "All targets are invalid")
                )
            )
        }

        val ownerId = cardComponent?.ownerId ?: spellComponent.casterId
        val cardDef = cardComponent?.let { cardRegistry.getCard(it.name) }
        // Flashback (printed or granted — Archmage's Newt) or Harmonize (printed or granted —
        // Songcrafter Mage): a graveyard cast exiles on resolution instead of returning to the
        // graveyard.
        val flashbackExile = spellComponent.castFromZone == Zone.GRAVEYARD &&
            (FlashbackGrants.effectiveFlashback(
                state, spellId, cardDef, spellComponent.casterId, cardRegistry, predicateEvaluator
            ) != null ||
                HarmonizeGrants.effectiveHarmonize(state, spellId, cardDef) != null)
        val exileAfterResolveComp = state.getEntity(spellId)?.get<AfterResolveDestinationComponent>()
        // Goliath Daydreamer-style components only redirect on actual resolution; if the spell
        // fizzles or is countered they go to graveyard normally.
        val riderOnFizzle = exileAfterResolveComp?.takeIf { !it.onlyIfResolved }
        // A fizzled spell heading to its owner's graveyard is a card put into a graveyard
        // "from anywhere" — honor RedirectZoneChange replacements (Valgavoth, Leyline).
        val fizzleRedirect = if (flashbackExile || riderOnFizzle != null) {
            com.wingedsheep.engine.handlers.effects.ZoneChangeRedirectResult(
                riderOnFizzle?.zone ?: Zone.EXILE
            )
        } else {
            com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .checkZoneChangeRedirect(state, spellId, Zone.STACK, Zone.GRAVEYARD)
        }
        val destZone = fizzleRedirect.destinationZone
        val destZoneKey = ZoneKey(ownerId, destZone)

        var newState = state.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }
        newState = newState.addToZone(destZoneKey, spellId)
        // A card-intrinsic redirect into the library shuffles the card in (Progenitus).
        if (destZone == Zone.LIBRARY && fizzleRedirect.shuffleIntoLibrary) {
            newState = shuffleOwnerLibrary(newState, ownerId)
        }
        if (destZone == Zone.EXILE && fizzleRedirect.linkSourceId != null) {
            newState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .linkExiledToSource(newState, spellId, fizzleRedirect.linkSourceId)
        }

        return ExecutionResult.success(
            newState,
            listOf(
                SpellFizzledEvent(spellId, cardComponent?.name ?: "Unknown", "All targets are invalid"),
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    destZone,
                    ownerId
                )
            )
        )
    }

    /**
     * Resolve a triggered ability.
     */
    private fun resolveTriggeredAbility(
        state: GameState,
        abilityId: EntityId,
        container: ComponentContainer
    ): ExecutionResult {
        val abilityComponent = container.get<TriggeredAbilityOnStackComponent>()!!
        val targetsComponent = container.get<TargetsComponent>()

        // The resolution-time context the two checks below and the effect itself all read: trigger
        // payload, last-known info, captured batch and all. Built up front because CR 608.2a's
        // intervening-"if" is evaluated against it *before* CR 608.2b touches the targets. The
        // targets it carries are the stored ones — legality is 608.2b's business, not the context's.
        val resolvedTargets2 = targetsComponent?.targets ?: emptyList()
        val targetReqs = targetsComponent?.targetRequirements ?: emptyList()
        val context = EffectContext.forTriggeredAbility(
            abilityComponent,
            targets = resolvedTargets2,
            targetRequirements = targetReqs
        )

        // CR 608.2a, then CR 608.2b — in that lettered order. 608.2a: "If a triggered ability has
        // an intervening 'if' clause, it checks whether the clause's condition is true. If it
        // isn't, the ability is removed from the stack and does nothing. Otherwise, it continues to
        // resolve." Only a *continuing* resolution reaches 608.2b's target-legality check, so when
        // both have gone false the intervening-"if" is what ends the resolution and what the fizzle
        // reports.

        // CR 608.2a / CR 603.4's second check. Fizzles through the same event as an illegal-target
        // fizzle rather than silently doing nothing.
        //
        // Only [TriggeredAbilityOnStackComponent.interveningIf] reaches here. A
        // `triggerRestriction` ("...attacks *while* you control a Dinosaur") is a CR 603.2
        // restriction on the trigger event and was already spent when the ability triggered;
        // re-checking it would fizzle abilities that must resolve.
        abilityComponent.interveningIf?.let { condition ->
            if (!conditionEvaluator.evaluate(state, condition, context)) {
                return ExecutionResult.success(
                    state.removeEntity(abilityId),
                    listOf(
                        AbilityFizzledEvent(
                            abilityComponent.sourceId,
                            abilityComponent.description,
                            "Intervening-if condition is no longer true"
                        )
                    )
                )
            }
        }

        // CR 608.2b — validate targets (including protection check, CR 702.16)
        val sourceCard = state.getEntity(abilityComponent.sourceId)?.get<CardComponent>()
        val sourceColors = sourceCard?.colors ?: emptySet()
        val sourceSubtypes = sourceCard?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
        if (targetsComponent != null && targetsComponent.targets.isNotEmpty()) {
            val validTargets = validateTargets(
                state, targetsComponent.targets, sourceColors, sourceSubtypes,
                abilityComponent.controllerId, targetsComponent.targetRequirements,
                sourceId = abilityComponent.sourceId,
                targetingSourceType = TargetingSourceType.ABILITY,
                xValue = abilityComponent.xValue,
                triggeringEntityId = abilityComponent.triggeringEntityId,
                triggeringPlayerId = abilityComponent.triggeringPlayerId,
                targetEntryStamps = targetsComponent.targetEntryStamps,
                storedCollections = abilityComponent.carriedPipeline?.storedCollections ?: emptyMap(),
            )
            if (validTargets.isEmpty()) {
                // Fizzle - remove ability entity
                val newState = state.removeEntity(abilityId)
                return ExecutionResult.success(
                    newState,
                    listOf(
                        AbilityFizzledEvent(
                            abilityComponent.sourceId,
                            abilityComponent.description,
                            "All targets are invalid"
                        )
                    )
                )
            }
        }

        // Execute the effect
        val effectResult = effectHandler.execute(state, abilityComponent.effect, context)

        // If effect is paused awaiting a decision, return paused state
        // The ability entity stays removed (it's off the stack), but the decision must resolve
        if (effectResult.isPaused) {
            val pausedState = effectResult.state.removeEntity(abilityId)
            return ExecutionResult.paused(
                pausedState,
                effectResult.pendingDecision!!,
                effectResult.events
            )
        }

        var newState = effectResult.newState

        // Remove the ability entity
        newState = newState.removeEntity(abilityId)

        // A Saga chapter ability resolving emits SagaChapterResolvedEvent so "whenever the final
        // chapter ability of a Saga you control resolves" triggers (Tom Bombadil) can detect it.
        val sagaEvents = abilityComponent.sagaChapterInfo?.let { info ->
            listOf(
                SagaChapterResolvedEvent(
                    sagaId = abilityComponent.sourceId,
                    controllerId = abilityComponent.controllerId,
                    chapterNumber = info.chapterNumber,
                    finalChapterNumber = info.finalChapterNumber,
                    isFinalChapter = info.isFinalChapter
                )
            )
        } ?: emptyList()

        return ExecutionResult.success(
            newState,
            effectResult.events + AbilityResolvedEvent(
                abilityComponent.sourceId,
                abilityComponent.description
            ) + sagaEvents
        )
    }

    /**
     * Resolve an activated ability.
     */
    private fun resolveActivatedAbility(
        state: GameState,
        abilityId: EntityId,
        container: ComponentContainer
    ): ExecutionResult {
        val abilityComponent = container.get<ActivatedAbilityOnStackComponent>()!!
        val targetsComponent = container.get<TargetsComponent>()

        // Validate targets (including protection check - Rule 702.16)
        val sourceCard = state.getEntity(abilityComponent.sourceId)?.get<CardComponent>()
        val sourceColors = sourceCard?.colors ?: emptySet()
        val sourceSubtypes = sourceCard?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
        val activatedReqs = targetsComponent?.targetRequirements ?: emptyList()
        // Resolution-time legality (CR 608.2b): drop individually-illegal targets, not just the
        // all-invalid fizzle. `activatedTargets` is the compacted list every executor reads as
        // `context.targets`; `alignedActivatedTargets` keeps a `null` in each dropped slot so a
        // sub-effect referencing a now-illegal target through its [EffectTarget.BoundVariable]
        // resolves to `null` and fizzles (mirrors resolveSpell) instead of silently consuming a
        // still-legal later target whose position shifted forward in the compacted list — e.g.
        // Stiltzkin's "If they do, you draw" when the donated permanent left in response.
        val activatedTargets: List<ChosenTarget>
        val alignedActivatedTargets: List<ChosenTarget?>
        if (targetsComponent != null && targetsComponent.targets.isNotEmpty()) {
            val validTargets = validateTargets(
                state, targetsComponent.targets, sourceColors, sourceSubtypes,
                abilityComponent.controllerId, targetsComponent.targetRequirements,
                sourceId = abilityComponent.sourceId,
                xValue = abilityComponent.xValue,
                targetEntryStamps = targetsComponent.targetEntryStamps
            )
            if (validTargets.isEmpty()) {
                val newState = state.removeEntity(abilityId)
                return ExecutionResult.success(
                    newState,
                    listOf(
                        AbilityFizzledEvent(
                            abilityComponent.sourceId,
                            abilityComponent.sourceName,
                            "All targets are invalid"
                        )
                    )
                )
            }
            activatedTargets = validTargets
            alignedActivatedTargets = buildAlignedValidated(targetsComponent.targets, validTargets)
        } else {
            activatedTargets = targetsComponent?.targets ?: emptyList()
            alignedActivatedTargets = activatedTargets
        }

        // Execute the effect
        val context = EffectContext(
            sourceId = abilityComponent.sourceId,
            controllerId = abilityComponent.controllerId,
            granterId = abilityComponent.granterId,
            abilityIdentity = abilityComponent.abilityIdentity,
            targets = activatedTargets,
            alignedTargets = alignedActivatedTargets,
            sacrificedPermanents = abilityComponent.sacrificedPermanents,
            xValue = abilityComponent.xValue,
            tappedPermanents = abilityComponent.tappedPermanents,
            tappedEntitySnapshots = abilityComponent.tappedEntitySnapshots,
            exiledAsCostCards = abilityComponent.exiledAsCostCards,
            lastKnownSourceCounters = abilityComponent.lastKnownSourceCounters,
            lastKnownSourceSnapshot = abilityComponent.lastKnownSourceSnapshot,
            lastKnownSourceAttachments = abilityComponent.lastKnownSourceAttachments,
            damageDistribution = abilityComponent.damageDistribution,
            pipeline = PipelineState(namedTargets = EffectContext.buildNamedTargets(activatedReqs, alignedActivatedTargets))
        )

        val effectResult = effectHandler.execute(state, abilityComponent.effect, context)

        // If effect is paused awaiting a decision, return paused state
        // The ability entity stays removed (it's off the stack), but the decision must resolve
        if (effectResult.isPaused) {
            val pausedState = effectResult.state.removeEntity(abilityId)
            return ExecutionResult.paused(
                pausedState,
                effectResult.pendingDecision!!,
                effectResult.events
            )
        }

        var newState = effectResult.newState

        // Remove the ability entity
        newState = newState.removeEntity(abilityId)

        return ExecutionResult.success(
            newState,
            effectResult.events + AbilityResolvedEvent(
                abilityComponent.sourceId,
                abilityComponent.sourceName
            )
        )
    }

    // =========================================================================
    // Enters With Replacements ("enters with counters / keywords", CR 614.1c)
    // =========================================================================

    /**
     * Apply the resolving permanent's "enters with …" replacement effects — counters
     * (EntersWithCounters / EntersWithDynamicCounters) and keywords (EntersWithKeywords) —
     * plus any global ones sourced from other battlefield permanents (e.g., Gev, Scaled
     * Scorch: "Other creatures you control enter with additional +1/+1 counters").
     * Thin wrapper over [EntersWithReplacements] carrying the cast context (X, mana spent).
     */
    internal fun applyEntersWithReplacements(
        state: GameState,
        entityId: EntityId,
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        controllerId: EntityId,
        xValue: Int? = null,
        totalManaSpent: Int = 0
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()

        val (ownState, ownEvents) = EntersWithReplacements.applyFromDefinition(
            newState, entityId, cardDef, controllerId, xValue, totalManaSpent
        )
        newState = ownState
        events.addAll(ownEvents)

        val (globalState, globalEvents) = EntersWithReplacements.applyGlobal(
            newState, entityId, controllerId
        )
        newState = globalState
        events.addAll(globalEvents)

        return newState to events
    }

    // =========================================================================
    // Countering
    // =========================================================================

    /**
     * Counter whatever stack object [entityId] is, spell or ability.
     *
     * A countered spell goes to its owner's graveyard; a countered ability simply ceases to
     * exist. "Counter it unless you pay …" effects — ward above all — can end up pointed at
     * either kind, so they route through here instead of assuming a spell: [counterSpell] on a
     * triggered ability finds no card/spell component and errors out, leaving the ability on the
     * stack to resolve as though the cost had been paid.
     */
    fun counterSpellOrAbility(state: GameState, entityId: EntityId): ExecutionResult {
        val container = state.getEntity(entityId)
            ?: return ExecutionResult.error(state, "Stack object not found: $entityId")
        return if (container.has<SpellOnStackComponent>()) counterSpell(state, entityId)
        else counterAbility(state, entityId)
    }

    /**
     * Counter a spell on the stack.
     */
    fun counterSpell(state: GameState, spellId: EntityId): ExecutionResult {
        if (spellId !in state.stack) {
            return ExecutionResult.error(state, "Spell not on stack: $spellId")
        }

        val container = state.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell not found: $spellId")

        val cardComponent = container.get<CardComponent>()

        // Check if the spell can't be countered (tag component)
        if (container.has<CantBeCounteredComponent>()) {
            return ExecutionResult.success(state)
        }

        // Check if any permanent on the battlefield grants "can't be countered" to this spell
        if (isGrantedCantBeCountered(state, spellId)) {
            return ExecutionResult.success(state)
        }

        val spellComponent = container.get<SpellOnStackComponent>()
        val ownerId = cardComponent?.ownerId
            ?: spellComponent?.casterId
            ?: return ExecutionResult.error(state, "Cannot determine spell owner")

        // Remove from stack
        var newState = state.removeFromStack(spellId)

        // Put in graveyard (or exile if AfterResolveDestinationComponent is present)
        // Goliath Daydreamer-style components only exile on actual resolution; if the spell
        // is countered they go to graveyard normally.
        val riderOnCounter = container.get<AfterResolveDestinationComponent>()
            ?.takeIf { !it.onlyIfResolved }
        // A countered spell heading to its owner's graveyard is still a card being put into a
        // graveyard "from anywhere" — honor RedirectZoneChange replacements (Valgavoth, Leyline).
        val counterRedirect = if (riderOnCounter != null) {
            com.wingedsheep.engine.handlers.effects.ZoneChangeRedirectResult(riderOnCounter.zone)
        } else {
            com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .checkZoneChangeRedirect(state, spellId, Zone.STACK, Zone.GRAVEYARD)
        }
        val destZone = counterRedirect.destinationZone
        val destZoneKey = ZoneKey(ownerId, destZone)
        newState = newState.addToZone(destZoneKey, spellId)
        // A card-intrinsic redirect into the library shuffles the card in (Progenitus).
        if (destZone == Zone.LIBRARY && counterRedirect.shuffleIntoLibrary) {
            newState = shuffleOwnerLibrary(newState, ownerId)
        }
        if (destZone == Zone.EXILE && counterRedirect.linkSourceId != null) {
            newState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .linkExiledToSource(newState, spellId, counterRedirect.linkSourceId)
        }

        // Remove stack components
        newState = newState.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }

        return ExecutionResult.success(
            newState,
            listOf(
                SpellCounteredEvent(spellId, cardComponent?.name ?: "Unknown"),
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    destZone,
                    ownerId
                )
            )
        )
    }

    /**
     * Counter a spell on the stack and exile it instead of putting it into
     * its owner's graveyard. If the spell can't be countered, nothing happens.
     *
     * @param grantFreeCast If true, the controller of this effect may cast the
     *   exiled card without paying its mana cost for as long as it remains exiled.
     * @param controllerId The player who gains permission to cast the exiled card.
     * @return ExecutionResult with a boolean flag indicating if the spell was actually countered.
     */
    fun counterSpellToExile(
        state: GameState,
        spellId: EntityId,
        grantFreeCast: Boolean,
        controllerId: EntityId
    ): ExecutionResult {
        if (spellId !in state.stack) {
            return ExecutionResult.error(state, "Spell not on stack: $spellId")
        }

        val container = state.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell not found: $spellId")

        val cardComponent = container.get<CardComponent>()

        // Check if the spell can't be countered
        if (container.has<CantBeCounteredComponent>() || isGrantedCantBeCountered(state, spellId)) {
            return ExecutionResult.success(state)
        }

        val spellComponent = container.get<SpellOnStackComponent>()
        val ownerId = cardComponent?.ownerId
            ?: spellComponent?.casterId
            ?: return ExecutionResult.error(state, "Cannot determine spell owner")

        // Remove from stack
        var newState = state.removeFromStack(spellId)

        // Put in exile (instead of graveyard)
        val exileZone = ZoneKey(ownerId, Zone.EXILE)
        newState = newState.addToZone(exileZone, spellId)

        // Remove stack components and optionally grant the counter's controller a free recast
        // (Kheru Spellsnatcher).
        newState = newState.updateEntity(spellId) { c ->
            var updated = c.without<SpellOnStackComponent>().without<TargetsComponent>()
            if (grantFreeCast) {
                updated = updated
                    .with(PlayWithoutPayingCostComponent(controllerId = controllerId, permanent = true))
            }
            updated
        }
        if (grantFreeCast) {
            val (permId, stateWithPerm) = newState.newEntity()
            newState = stateWithPerm.addMayPlayPermission(
                com.wingedsheep.engine.state.permissions.MayPlayPermission(
                    id = permId,
                    cardIds = setOf(spellId),
                    controllerId = controllerId,
                    permanent = true,
                    timestamp = state.timestamp,
                )
            )
        }

        return ExecutionResult.success(
            newState,
            listOf(
                SpellCounteredEvent(spellId, cardComponent?.name ?: "Unknown"),
                ZoneChangeEvent(
                    spellId,
                    cardComponent?.name ?: "Unknown",
                    null,
                    Zone.EXILE,
                    ownerId
                )
            )
        )
    }

    /**
     * Exile a spell on the stack (CR 718 "exile target spell" — Aven Interrupter), optionally
     * making it *plotted* for its owner.
     *
     * Unlike [counterSpellToExile] this is **not** a counter: it ignores can't-be-countered
     * (the spell is exiled regardless — Aven Interrupter's ruling: "Spells that can't be
     * countered can still be exiled"), and it emits no [SpellCounteredEvent] (so "whenever a
     * spell is countered" triggers don't fire). The spell still ceases to resolve because it
     * leaves the stack. A [ZoneChangeEvent] from [Zone.STACK] to [Zone.EXILE] is emitted.
     *
     * When [makePlotted] is true the exiled card gets the plotted designation and a permanent
     * free-cast-on-a-later-turn permission gated by [SourcePlottedOnPriorTurn], granted to the
     * card's **owner** (CR 718.2 / the reminder text: "Its owner may cast it as a sorcery on a
     * later turn without paying its mana cost"), and a [CardPlottedEvent] is emitted.
     *
     * When [fixedAlternativeManaCost] is non-null the exiled card's **owner** gets a permanent
     * may-play permission and a [PlayWithFixedAlternativeManaCostComponent], letting them recast it
     * for that fixed cost instead of its printed cost for as long as it stays exiled — the
     * spell-on-stack form of the **Airbend** keyword (Aang, Swift Savior). Mutually exclusive with
     * [makePlotted].
     */
    fun exileSpell(
        state: GameState,
        spellId: EntityId,
        makePlotted: Boolean,
        fixedAlternativeManaCost: com.wingedsheep.sdk.core.ManaCost? = null,
        linkToSourceId: EntityId? = null
    ): ExecutionResult {
        if (spellId !in state.stack) {
            return ExecutionResult.error(state, "Spell not on stack: $spellId")
        }
        val container = state.getEntity(spellId)
            ?: return ExecutionResult.error(state, "Spell not found: $spellId")
        val cardComponent = container.get<CardComponent>()
        val spellComponent = container.get<SpellOnStackComponent>()
        val ownerId = cardComponent?.ownerId
            ?: spellComponent?.casterId
            ?: return ExecutionResult.error(state, "Cannot determine spell owner")

        // Remove from the stack and put the card into its owner's exile.
        var newState = state.removeFromStack(spellId)
        val exileZone = ZoneKey(ownerId, Zone.EXILE)
        newState = newState.addToZone(exileZone, spellId)
        newState = newState.updateEntity(spellId) { c ->
            c.without<SpellOnStackComponent>().without<TargetsComponent>()
        }

        val events = mutableListOf<GameEvent>(
            ZoneChangeEvent(spellId, cardComponent?.name ?: "Unknown", Zone.STACK, Zone.EXILE, ownerId)
        )

        if (makePlotted) {
            newState = applyPlottedToExiledCard(newState, spellId, ownerId, cardComponent?.name ?: "Unknown", events)
        } else if (fixedAlternativeManaCost != null) {
            newState = applyFixedAltCostToExiledCard(newState, spellId, ownerId, fixedAlternativeManaCost)
        }

        // "Exile it with this permanent" (Spell Queller): record the card in the source's
        // linked-exile pile so a later ability of that source can say "the exiled card". Only a
        // handle — nothing returns or becomes castable on its own.
        if (linkToSourceId != null) {
            newState = com.wingedsheep.engine.handlers.effects.ZoneMovementUtils
                .linkExiledToSource(newState, spellId, linkToSourceId)
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Grant the **owner** of a card already sitting in their exile a permanent may-play permission
     * plus a [PlayWithFixedAlternativeManaCostComponent], so they may recast it for [fixedCost]
     * instead of its printed cost for as long as it stays exiled. The spell-on-stack tail of the
     * **Airbend** keyword; mirrors [applyPlottedToExiledCard] but with a fixed alternative cost
     * rather than a free, plotted-on-a-later-turn cast.
     */
    private fun applyFixedAltCostToExiledCard(
        state: GameState,
        cardId: EntityId,
        ownerId: EntityId,
        fixedCost: com.wingedsheep.sdk.core.ManaCost,
    ): GameState {
        var newState = state.updateEntity(cardId) { c ->
            c.with(
                com.wingedsheep.engine.state.components.identity.PlayWithFixedAlternativeManaCostComponent(
                    controllerId = ownerId,
                    fixedCost = fixedCost
                )
            )
        }
        val (permId, stateWithPerm) = newState.newEntity()
        newState = stateWithPerm.addMayPlayPermission(
            MayPlayPermission(
                id = permId,
                cardIds = setOf(cardId),
                controllerId = ownerId,
                permanent = true,
                timestamp = newState.timestamp,
            )
        )
        return newState
    }

    /**
     * Make a card that already sits in [ownerId]'s exile *plotted* (CR 718): tag it with
     * [PlottedComponent] + [PlayWithoutPayingCostComponent], grant a permanent may-play
     * permission gated on [SourcePlottedOnPriorTurn] (a plotted card can't be cast the turn it
     * was plotted), and emit [CardPlottedEvent]. Shared by [ExileTargetSpellEffect]'s
     * `makePlotted` path and the [AfterResolveDestinationComponent].`makePlotted` self-cast path
     * (Lilah, Undefeated Slickshot).
     */
    private fun applyPlottedToExiledCard(
        state: GameState,
        cardId: EntityId,
        ownerId: EntityId,
        cardName: String,
        events: MutableList<GameEvent>,
    ): GameState {
        val turnPlotted = state.turnNumber
        var newState = state.updateEntity(cardId) { c ->
            c.with(PlottedComponent(controllerId = ownerId, turnPlotted = turnPlotted))
                .with(PlayWithoutPayingCostComponent(controllerId = ownerId, permanent = true))
        }
        val (permId, stateWithPerm) = newState.newEntity()
        newState = stateWithPerm.addMayPlayPermission(
            MayPlayPermission(
                id = permId,
                cardIds = setOf(cardId),
                controllerId = ownerId,
                sourceId = cardId,
                condition = SourcePlottedOnPriorTurn,
                permanent = true,
                timestamp = newState.timestamp,
            )
        )
        events.add(CardPlottedEvent(ownerId, cardId, cardName))
        return newState
    }

    /**
     * Counter an activated or triggered ability on the stack.
     * Unlike countering a spell, the ability is simply removed from the stack
     * without going to any zone (abilities are not cards).
     */
    fun counterAbility(state: GameState, abilityId: EntityId): ExecutionResult {
        if (abilityId !in state.stack) {
            return ExecutionResult.error(state, "Ability not on stack: $abilityId")
        }

        val container = state.getEntity(abilityId)
            ?: return ExecutionResult.error(state, "Ability not found: $abilityId")

        val description = container.get<TriggeredAbilityOnStackComponent>()?.description
            ?: container.get<ActivatedAbilityOnStackComponent>()?.let { "${it.sourceName}'s ability" }
            ?: "Unknown ability"

        // "Abilities can't be countered" (Spider-Punk): a battlefield GrantCantBeCountered with
        // includesAbilities = true whose filter matches this ability makes the counter fizzle — the
        // ability stays on the stack and resolves normally.
        if (isAbilityGrantedCantBeCountered(state, abilityId)) {
            return ExecutionResult.success(state)
        }

        // Remove from stack — abilities don't go to any zone
        val newState = state.removeFromStack(abilityId)

        return ExecutionResult.success(
            newState,
            listOf(AbilityCounteredEvent(abilityId, description))
        )
    }

    // =========================================================================
    // Target Validation
    // =========================================================================

    /**
     * Validate targets and return only valid ones.
     *
     * Checks zone existence, protection (Rule 702.16), and target filter matching
     * (Rule 608.2b — targets must still be legal when the spell/ability resolves).
     */
    private fun validateTargets(
        state: GameState,
        targets: List<ChosenTarget>,
        sourceColors: Set<Color> = emptySet(),
        sourceSubtypes: Set<String> = emptySet(),
        controllerId: EntityId,
        targetRequirements: List<TargetRequirement> = emptyList(),
        sourceId: EntityId? = null,
        targetingSourceType: TargetingSourceType = TargetingSourceType.ANY,
        xValue: Int? = null,
        triggeringEntityId: EntityId? = null,
        triggeringPlayerId: EntityId? = null,
        /**
         * The object-identity stamps captured when these targets were chosen
         * ([TargetsComponent.targetEntryStamps]) — a permanent that left the battlefield and came
         * back in the meantime is a different object and no longer a legal target (CR 400.7).
         */
        targetEntryStamps: Map<EntityId, Long> = emptyMap(),
        /**
         * Pipeline collections available at resolution time (e.g. the amassed Army under
         * `EntityReference.AmassedArmy`, from a `ReflexiveTriggerEffect`'s carried pipeline) — the
         * CR 608.2b re-validation below re-checks the target filter, and a filter like Grishnákh's
         * "power <= the amassed Army's power" needs this to resolve the referenced entity, or every
         * target wrongly fails re-validation as unresolvable.
         */
        storedCollections: Map<String, List<EntityId>> = emptyMap()
    ): List<ChosenTarget> {
        // Always project state for shroud/hexproof checks (Rule 702.18, 702.11)
        val projected = state.projectedState
        val predicateContext = PredicateContext(
            controllerId = controllerId,
            sourceId = sourceId,
            xValue = xValue,
            triggeringEntityId = triggeringEntityId,
            triggeringPlayerId = triggeringPlayerId,
            storedCollections = storedCollections,
        )

        return targets.filterIndexed { index, target ->
            when (target) {
                is ChosenTarget.Player -> {
                    // Player is valid if they exist and haven't lost...
                    if (!state.hasEntity(target.playerId)) return@filterIndexed false
                    // ...and (CR 608.2b) the player-target restriction still holds. A player who
                    // gained life above the threshold, or whose "lost life this turn" never
                    // happened, is removed at resolution.
                    val requirement = getRequirementForTargetIndex(index, targetRequirements)
                    val restriction = when (requirement) {
                        is TargetPlayer -> requirement.restriction
                        is TargetOpponent -> requirement.restriction
                        else -> null
                    }
                    PlayerTargetRestriction.isSatisfied(state, restriction, target.playerId, controllerId, sourceId)
                }

                is ChosenTarget.Permanent -> {
                    // Permanent is valid if still on battlefield
                    if (target.entityId !in state.getBattlefield()) return@filterIndexed false

                    // ...and if it's still the same object. A permanent blinked in response
                    // (Personify, Cloudshift) reuses its entity id here, but it returned as a new
                    // object (CR 400.7) that was never targeted, so the target is illegal.
                    if (TargetsComponent.isDifferentObject(state, target.entityId, targetEntryStamps)) {
                        return@filterIndexed false
                    }

                    // Check shroud — can't be targeted by anyone (Rule 702.18)
                    if (projected.hasKeyword(target.entityId, "SHROUD")) return@filterIndexed false

                    // Check hexproof — can't be targeted by opponents (Rule 702.11)
                    val entityController = projected.getController(target.entityId)
                        ?: state.getEntity(target.entityId)?.get<ControllerComponent>()?.playerId
                    val hexproofSuppressed = HexproofSuppression.isSuppressedForCaster(state, projected, target.entityId, controllerId)
                    if (!hexproofSuppressed && projected.hasKeyword(target.entityId, "HEXPROOF") && entityController != controllerId) return@filterIndexed false

                    // Check hexproof from color (Rule 702.11b)
                    if (!hexproofSuppressed && entityController != controllerId) {
                        for (color in sourceColors) {
                            if (projected.hasKeyword(target.entityId, "HEXPROOF_FROM_${color.name}")) {
                                return@filterIndexed false
                            }
                        }
                        // ...and from the source's card types, e.g. "hexproof from instants"
                        // (Elenda, Saint of Dusk). Same source-type resolution as protection.
                        if (sourceId != null) {
                            for (cardType in SourceTypeTargeting.sourceCardTypes(state, sourceId)) {
                                if (projected.hasKeyword(
                                        target.entityId,
                                        "HEXPROOF_FROM_CARDTYPE_${cardType.uppercase()}"
                                    )
                                ) {
                                    return@filterIndexed false
                                }
                            }
                        }
                    }

                    // Check can't-be-targeted-by-abilities (Shanna, Sisay's Legacy)
                    if (targetingSourceType != TargetingSourceType.SPELL && entityController != controllerId) {
                        if (ControllerGrants.isActiveOn<CantBeTargetedByOpponentAbilitiesComponent>(
                                state,
                                target.entityId,
                            )
                        ) {
                            return@filterIndexed false
                        }
                    }

                    // Artifact Ward family: can't be the target of abilities from sources of a
                    // given card type. Keys off the ability's source (CR 113.7) by card type, not
                    // controller — applies even to the warded creature's own controller's sources.
                    // Spells bypass (abilities-only).
                    if (SourceTypeTargeting.cantBeTargetedBySourceTypeAbility(
                            state, target.entityId, sourceId, targetingSourceType
                        )
                    ) {
                        return@filterIndexed false
                    }

                    // Check protection from source colors/subtypes (Rule 702.16)
                    for (color in sourceColors) {
                        if (projected.hasKeyword(target.entityId, "PROTECTION_FROM_${color.name}")) {
                            return@filterIndexed false
                        }
                    }
                    for (subtype in sourceSubtypes) {
                        if (projected.hasKeyword(target.entityId, "PROTECTION_FROM_SUBTYPE_${subtype.uppercase()}")) {
                            return@filterIndexed false
                        }
                    }
                    // Check protection from the source's card type, e.g. "protection from creatures"
                    // (Rule 702.16). Prefer projected types (permanent sources); fall back to the
                    // card's printed card types for spell/ability sources not in the projection.
                    if (sourceId != null) {
                        val projectedTypes = projected.getTypes(sourceId)
                        val sourceCardTypes = if (projectedTypes.isNotEmpty()) {
                            projectedTypes
                        } else {
                            state.getEntity(sourceId)?.get<CardComponent>()
                                ?.typeLine?.cardTypes?.map { it.name }?.toSet() ?: emptySet()
                        }
                        for (cardType in sourceCardTypes) {
                            if (projected.hasKeyword(target.entityId, "PROTECTION_FROM_CARDTYPE_${cardType.uppercase()}")) {
                                return@filterIndexed false
                            }
                        }
                    }

                    // Check protection from each opponent (Rule 702.16e)
                    if (projected.hasKeyword(target.entityId, "PROTECTION_FROM_EACH_OPPONENT") &&
                        entityController != null && entityController != controllerId) {
                        return@filterIndexed false
                    }

                    // Re-validate target filter (Rule 608.2b)
                    val requirement = getRequirementForTargetIndex(index, targetRequirements)
                    val filter = extractTargetFilter(requirement)
                    if (filter != null) {
                        if (!predicateEvaluator.matches(
                                state, projected, target.entityId, filter.baseFilter, predicateContext
                            )
                        ) {
                            return@filterIndexed false
                        }
                    }

                    true
                }

                is ChosenTarget.Card -> {
                    // Card is valid if in expected zone
                    val zoneKey = ZoneKey(target.ownerId, target.zone)
                    target.cardId in state.getZone(zoneKey)
                }

                is ChosenTarget.Spell -> {
                    // Spell is valid if still on stack
                    target.spellEntityId in state.stack
                }
            }
        }
    }

    /**
     * Project [validTargets] (the compacted output of [validateTargets]) back onto
     * [originalTargets] positions, returning a list parallel to [originalTargets] with
     * `null` in slots whose target was dropped by 608.2b validation. Walks both lists
     * in order — [validateTargets] preserves the relative ordering of survivors — so the
     * mapping is unambiguous even when two original targets compare structurally equal.
     */
    private fun buildAlignedValidated(
        originalTargets: List<ChosenTarget>,
        validTargets: List<ChosenTarget>
    ): List<ChosenTarget?> {
        var v = 0
        return originalTargets.map { orig ->
            if (v < validTargets.size && validTargets[v] === orig) {
                v++
                orig
            } else {
                null
            }
        }
    }

    /**
     * Find the TargetRequirement that corresponds to a given target index.
     * Requirements are matched to targets in order, with each requirement
     * consuming `count` targets.
     */
    private fun getRequirementForTargetIndex(
        targetIndex: Int,
        requirements: List<TargetRequirement>
    ): TargetRequirement? {
        var idx = 0
        for (req in requirements) {
            val end = idx + req.count
            if (targetIndex in idx until end) return req
            idx = end
        }
        return null
    }

    /**
     * Extract the TargetFilter from a TargetRequirement, if it has one.
     */
    private fun extractTargetFilter(requirement: TargetRequirement?): TargetFilter? {
        return when (requirement) {
            is TargetObject -> requirement.filter
            else -> null
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

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
        // Check all players' exile zones (cards may be in another player's exile,
        // e.g., Villainous Wealth exiles from opponent's library)
        for (pid in state.turnOrder) {
            if (cardId in state.getZone(ZoneKey(pid, Zone.EXILE))) {
                return Zone.EXILE
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
     * disguise (CR 702.168a) — or null when it can't be cast face down at all. A card never prints
     * both; morph wins if one somehow did.
     */
    fun faceDownCastMode(cardDef: com.wingedsheep.sdk.model.CardDefinition?): FaceDownMode? = when {
        cardDef == null -> null
        cardDef.keywordAbilities.any { it is KeywordAbility.Morph } -> FaceDownMode.MORPH
        cardDef.keywordAbilities.any { it is KeywordAbility.Disguise } -> FaceDownMode.DISGUISE
        else -> null
    }

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

    /**
     * Check if a spell on the stack is granted "can't be countered" by any permanent
     * on the battlefield with a GrantCantBeCountered static ability.
     *
     * The predicate context's `controllerId` is set to the source permanent's controller
     * so filters using `youControl()` correctly mean "the granter's controller controls X"
     * (e.g., Hexing Squelcher's "Spells you control can't be countered" should only protect
     * its own controller's spells, not every player's spells).
     */
    private fun isGrantedCantBeCountered(state: GameState, spellId: EntityId): Boolean {
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                val sourceControllerId =
                    state.getEntity(entityId)?.get<ControllerComponent>()?.playerId ?: playerId
                val context = PredicateContext(controllerId = sourceControllerId, sourceId = entityId)
                for (ability in def.staticAbilities) {
                    if (ability is GrantCantBeCountered) {
                        if (predicateEvaluator.matches(state, state.projectedState, spellId, ability.filter, context)) {
                            return true
                        }
                    }
                }
            }
        }

        // Player-scoped grant: "Creature spells you cast this turn can't be countered" (Domri,
        // Anarch of Bolas). The granter is the spell's controller, so we evaluate filters from
        // their SpellsCantBeCounteredComponent against the spell on the stack.
        val spellController = state.getEntity(spellId)
            ?.get<SpellOnStackComponent>()
            ?.casterId
            ?: state.getEntity(spellId)?.get<ControllerComponent>()?.playerId
        if (spellController != null) {
            val component = state.getEntity(spellController)
                ?.get<com.wingedsheep.engine.state.components.player.SpellsCantBeCounteredComponent>()
            if (component != null) {
                val context = PredicateContext(controllerId = spellController, sourceId = spellController)
                for (filter in component.filters) {
                    if (predicateEvaluator.matches(state, state.projectedState, spellId, filter, context)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Whether an activated/triggered ability on the stack ([abilityId]) can't be countered because a
     * battlefield [GrantCantBeCountered] with `includesAbilities = true` covers it (its filter matches
     * the ability — an unrestricted `GameObjectFilter.Any` matches every ability). Spider-Punk's
     * "Spells and abilities can't be countered."
     */
    private fun isAbilityGrantedCantBeCountered(state: GameState, abilityId: EntityId): Boolean {
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                val sourceControllerId =
                    state.getEntity(entityId)?.get<ControllerComponent>()?.playerId ?: playerId
                val context = PredicateContext(controllerId = sourceControllerId, sourceId = entityId)
                for (ability in def.staticAbilities) {
                    if (ability is GrantCantBeCountered && ability.includesAbilities) {
                        if (predicateEvaluator.matches(state, state.projectedState, abilityId, ability.filter, context)) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    // =========================================================================
    // Valiant / "first time targeted" tracking
    // =========================================================================

    /**
     * Check if the target entity has already been targeted by the given controller this turn.
     */
    private fun hasBeenTargetedByController(state: GameState, targetId: EntityId, controllerId: EntityId): Boolean {
        val component = state.getEntity(targetId)?.get<TargetedByControllerThisTurnComponent>()
        return component?.hasBeenTargetedBy(controllerId) == true
    }

    /**
     * Mark the target entity as having been targeted by the given controller this turn.
     */
    private fun markTargetedByController(state: GameState, targetId: EntityId, controllerId: EntityId): GameState {
        return state.updateEntity(targetId) { container ->
            val existing = container.get<TargetedByControllerThisTurnComponent>()
                ?: TargetedByControllerThisTurnComponent()
            container.with(existing.withController(controllerId))
        }
    }


    /**
     * Create the appropriate decision and continuation for an EntersWithChoice replacement effect.
     * Returns null if the choice cannot be presented (e.g., no creatures on battlefield for CREATURE_ON_BATTLEFIELD).
     */
    internal fun pauseForEntersWithChoice(
        state: GameState,
        spellId: EntityId,
        controllerId: EntityId,
        ownerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice,
        syntheticRiot: Boolean = false,
        syntheticRiotRemaining: Int = 0
    ): ExecutionResult? {
        val chooserId = when (choice.chooser) {
            com.wingedsheep.sdk.scripting.references.Player.AnOpponent ->
                state.getOpponents(controllerId).firstOrNull() ?: controllerId
            else -> controllerId
        }

        return when (choice.choiceType) {
            ChoiceType.COLOR -> {
                val decisionId = "choose-color-enters-${spellId.value}"
                val decision = ChooseColorDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a color",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    )
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.COLOR
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.CREATURE_TYPE -> {
                val creatureTypeOptions = choice.allowedCreatureTypes
                    ?: com.wingedsheep.sdk.core.Subtype.ALL_CREATURE_TYPES
                val decisionId = "choose-creature-type-enters-${spellId.value}"
                val decision = ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a creature type",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = creatureTypeOptions,
                    defaultSearch = ""
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.CREATURE_TYPE,
                    creatureTypes = creatureTypeOptions
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.CREATURE_ON_BATTLEFIELD -> {
                val battlefieldCreatures = state.getBattlefield().filter { entityId ->
                    entityId != spellId &&
                        state.projectedState.getController(entityId) == controllerId &&
                        state.projectedState.isCreature(entityId)
                }
                if (battlefieldCreatures.isEmpty()) return null // No creatures — enter without choice
                val decisionId = "choose-creature-enters-${spellId.value}"
                val decision = SelectCardsDecision(
                    id = decisionId,
                    playerId = controllerId,
                    prompt = "Choose another creature you control",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = battlefieldCreatures,
                    minSelections = 1,
                    maxSelections = 1,
                    useTargetingUI = true
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.CREATURE_ON_BATTLEFIELD
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.MODE -> {
                if (choice.modeOptions.isEmpty()) {
                    return null
                }
                // A permanent granted multiple riot instances re-pauses on the same spell; suffix the
                // id with the remaining count so each instance's decision is distinct (CR 702.136b).
                val decisionId = "choose-mode-enters-${spellId.value}" +
                    if (syntheticRiot) "-riot$syntheticRiotRemaining" else ""
                val decision = ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose for ${cardComponent.name}",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = choice.modeOptions.map { it.label },
                    optionMetadata = choice.modeOptions.map {
                        OptionMetadata(id = it.id, description = it.description, iconKey = it.iconKey)
                    }
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.MODE,
                    modeOptionIds = choice.modeOptions.map { it.id },
                    syntheticRiot = syntheticRiot,
                    syntheticRiotRemaining = syntheticRiotRemaining
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.BASIC_LAND_TYPE -> {
                val landTypeOptions = com.wingedsheep.sdk.core.Subtype.ALL_BASIC_LAND_TYPES.toList()
                val decisionId = "choose-land-type-enters-${spellId.value}"
                val decision = ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a basic land type",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = landTypeOptions,
                    defaultSearch = ""
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.BASIC_LAND_TYPE,
                    landTypes = landTypeOptions
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.OPPONENT -> {
                // CR 614.12a — replacement-effect choices that modify how a permanent enters
                // are made before the permanent enters. We surface the opponent prompt now so
                // the chosen opponent is durably recorded in [CastChoicesComponent]. In a 1v1
                // game this collapses to a forced choice but the prompt is still surfaced.
                val opponentIds = state.turnOrder.filter { it != chooserId }
                if (opponentIds.isEmpty()) return null
                val opponentNames = opponentIds.map { pid ->
                    state.getEntity(pid)
                        ?.get<com.wingedsheep.engine.state.components.identity.PlayerComponent>()?.name
                        ?: "Player ${pid.value}"
                }
                val decisionId = "choose-opponent-enters-${spellId.value}"
                val decision = ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose an opponent",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = opponentNames
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.OPPONENT,
                    opponentIds = opponentIds
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }

            ChoiceType.CARD_NAME -> {
                // "Choose a land card name" (Petrified Hamlet) or "choose any card name"
                // (Sorcerous Spyglass / Pithing Needle) as the permanent spell resolves. The pool
                // is land names or every registered card name per [EntersWithChoice.cardNamePool];
                // the chosen name is stored durably under [ChoiceSlot.CARD_NAME] by the resumer.
                val cardNames = cardRegistry.cardNamesIn(choice.cardNamePool).sorted()
                if (cardNames.isEmpty()) return null
                // "As this enters, look at an opponent's hand, then …": reveal the opponent's hand
                // to the controller before presenting the name choice.
                val (baseState, lookEvents) = if (choice.lookAtOpponentHand) {
                    com.wingedsheep.engine.handlers.effects.PermanentEntryReplacements
                        .revealOpponentHandForEntersChoice(state, controllerId)
                } else state to emptyList()
                val prompt = choice.cardNamePool.prompt
                val decisionId = "choose-card-name-enters-${spellId.value}"
                val decision = ChooseOptionDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = prompt,
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    options = cardNames
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.CARD_NAME,
                    cardNames = cardNames
                )
                val pausedState = baseState
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision, lookEvents)
            }

            ChoiceType.NUMBER -> {
                // "As this creature enters, choose a number between [min] and [max]" (Shapeshifter).
                // The chosen number is stored durably under [ChoiceSlot.CHOSEN_NUMBER] by the resumer.
                val decisionId = "choose-number-enters-${spellId.value}"
                val decision = ChooseNumberDecision(
                    id = decisionId,
                    playerId = chooserId,
                    prompt = "Choose a number between ${choice.minValue} and ${choice.maxValue}",
                    context = DecisionContext(
                        sourceId = spellId,
                        sourceName = cardComponent.name,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    minValue = choice.minValue,
                    maxValue = choice.maxValue
                )
                val continuation = EntersWithChoiceSpellContinuation(
                    decisionId = decisionId,
                    spellId = spellId,
                    controllerId = controllerId,
                    ownerId = ownerId,
                    choiceType = ChoiceType.NUMBER
                )
                val pausedState = state
                    .pushContinuation(continuation)
                    .withPendingDecision(decision)
                ExecutionResult.paused(pausedState, decision)
            }
        }
    }

}

/**
 * Build pipeline `storedCollections` for cost-chosen card IDs.
 *
 * The chosen IDs (from [AdditionalCost.Behold] — on its own or as an [AdditionalCost.OrPay] leg —
 * or [AdditionalCost.ChooseEntity]) are stored on the stack object as
 * [SpellOnStackComponent.beheldCards]. Each of those costs declares its own
 * `storeAs` key that the card's resolution-time effects reference (e.g. via
 * `EntityReference.FromCostStorage`). To keep the effect's reference
 * stable across cost variants, expose the IDs under every relevant `storeAs`
 * key plus a default `"beheld"` key for backward compatibility with
 * pre-existing Behold-using cards.
 *
 * Top-level so non-stack consumers (e.g. the client-side preview text builder
 * in `ClientStateTransformer`) can populate the same pipeline view of the
 * spell's cost-chosen state without re-implementing the lookup.
 */
internal fun buildBeheldStoredCollections(
    beheldCards: List<EntityId>,
    cardDef: com.wingedsheep.sdk.model.CardDefinition?
): Map<String, List<EntityId>> {
    if (beheldCards.isEmpty()) return emptyMap()
    val keys = mutableSetOf("beheld")
    fun collect(cost: AdditionalCost) {
        when (cost) {
            is AdditionalCost.Behold -> keys += cost.storeAs
            is AdditionalCost.ChooseEntity -> keys += cost.storeAs
            is AdditionalCost.Composite -> cost.steps.forEach(::collect)
            is AdditionalCost.OrPay -> collect(cost.cost)
            else -> {}
        }
    }
    cardDef?.script?.additionalCosts?.forEach(::collect)
    return keys.associateWith { beheldCards }
}
