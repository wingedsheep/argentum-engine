package com.wingedsheep.engine.handlers.actions.ability
import com.wingedsheep.engine.state.components.battlefield.chosenColor

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.AbilityActivatedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LoyaltyChangedEvent
import com.wingedsheep.engine.core.ManaAddedEvent
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.tap
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutorRegistry
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.IntrinsicManaAbilities
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.buildAbilityPaymentContext
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedEverComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.TextReplacementComponent
import com.wingedsheep.engine.state.components.player.CantActivateLoyaltyAbilitiesComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.captureEntitySnapshots
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.manaCostOrNull
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.DampLandManaProduction
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.LevelUpClassEffect
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.ReplaceLandManaColor
import com.wingedsheep.sdk.scripting.values.ManaColorSet
import com.wingedsheep.engine.core.LandTappedForManaEvent
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.CostPaymentChoices
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Zone
import kotlin.reflect.KClass

/**
 * Handler for the ActivateAbility action.
 *
 * Handles activating abilities on permanents, including:
 * - Mana abilities (immediate resolution)
 * - Non-mana abilities (go on stack)
 * - Planeswalker loyalty abilities
 */
class ActivateAbilityHandler(
    private val cardRegistry: CardRegistry,
    private val turnManager: TurnManager,
    private val costHandler: CostHandler,
    private val manaSolver: ManaSolver,
    private val alternativePaymentHandler: AlternativePaymentHandler,
    private val effectExecutorRegistry: EffectExecutorRegistry,
    private val stackResolver: StackResolver,
    private val targetValidator: TargetValidator,
    private val conditionEvaluator: ConditionEvaluator,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
    private val castPermissionUtils: CastPermissionUtils,
) : ActionHandler<ActivateAbility> {
    override val actionType: KClass<ActivateAbility> = ActivateAbility::class

    /** The first [CostAtom.TapPermanents] atom anywhere in this cost, or null if it has none. */
    private fun AbilityCost.firstTapPermanentsAtomOrNull(): CostAtom.TapPermanents? = when (this) {
        is AbilityCost.Atom -> atom as? CostAtom.TapPermanents
        is AbilityCost.Composite -> costs.firstNotNullOfOrNull { it.firstTapPermanentsAtomOrNull() }
        else -> null
    }

    override fun validate(state: GameState, action: ActivateAbility): String? {
        // `opponentTargetsChosen` is an internal resume marker for "… of an opponent's choice"
        // targets (Cuombajj Witches). Only the engine's resumer sets it, and the resumer re-enters
        // via execute() directly — never through validate() — so any action carrying it here came
        // from a player/client. Reject it: otherwise a client could set it to skip the
        // opponent-target pause and resolve the opponent-chosen damage with no target. See
        // [com.wingedsheep.sdk.scripting.targets.TargetChooser].
        if (action.opponentTargetsChosen) {
            return "Internal resume flag cannot be set by a player"
        }
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }

        val container = state.getEntity(action.sourceId)
            ?: return "Source not found: ${action.sourceId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Source is not a card"

        // Tokens (and other entities without a registered CardDefinition) only have abilities
        // via static grants (e.g., Brightcap Badger granting "{T}: Add {G}" to Saproling tokens),
        // intrinsic mana abilities (basic-land subtypes), or temporarily granted abilities. Don't
        // bail out when the lookup fails — fall through to those sources instead.
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        // Look up ability from card definition (including class-level abilities), granted abilities, or static grants
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel
        val staticGrants = getStaticGrantedAbilitiesWithGranter(action.sourceId, state)
        val ability = cardDef?.script?.effectiveActivatedAbilities(classLevel)?.find { it.id == action.abilityId }
            ?: cardDef?.let { findClassLevelUpAbility(it, container, action.abilityId) }
            ?: state.grantedActivatedAbilities
                .filter { it.entityId == action.sourceId }
                .map { it.ability }
                .find { it.id == action.abilityId }
            ?: staticGrants.firstOrNull { it.first.id == action.abilityId }?.first
            ?: resolveIntrinsicManaAbility(state, action.sourceId, action.abilityId)
            ?: return "Ability not found on this card"

        // Check that the card is in the correct zone for this ability
        if (ability.activateFromZone != Zone.BATTLEFIELD) {
            val ownerId = container.get<OwnerComponent>()?.playerId ?: return "Card has no owner"
            val inZone = state.getZone(ownerId, ability.activateFromZone).contains(action.sourceId)
            if (!inZone) return "This ability can only be activated from the ${ability.activateFromZone.name.lowercase()}"
            if (ownerId != action.playerId) return "You don't own this card"
        } else {
            // Check if any player may activate this ability (e.g., Lethal Vapors)
            val anyPlayerMay = ability.restrictions.any { it is ActivationRestriction.AnyPlayerMay }

            if (!anyPlayerMay) {
                // Use projected controller to account for control-changing effects (e.g., Annex)
                val projected = state.projectedState
                val controller = projected.getController(action.sourceId)
                    ?: container.get<ControllerComponent>()?.playerId
                if (controller != action.playerId) {
                    return "You don't control this permanent"
                }
            }

            // Face-down creatures have no abilities (Rule 708.2)
            if (container.has<FaceDownComponent>()) {
                return "Face-down creatures have no abilities"
            }

            // PreventActivatedAbilities (Cursed Totem etc.) blocks activated abilities of
            // matching permanents — mana and non-mana alike. Loyalty abilities of
            // planeswalkers and Crew-style animation abilities are not blocked because the
            // filter (typically `Creature`) is matched in projected state.
            if (castPermissionUtils.isActivationPrevented(state, action.sourceId, abilityIsManaAbility = ability.isManaAbility)) {
                return "Activated abilities of this permanent can't be activated"
            }

            // PlayersCantActivateAbilities (Grand Abolisher etc.) blocks abilities by *who* is
            // activating and *when* — "During your turn, your opponents can't activate abilities
            // of artifacts, creatures, or enchantments." Scoped to the activating player.
            if (castPermissionUtils.isActivationPreventedForPlayer(state, action.sourceId, action.playerId)) {
                return "An effect prevents you from activating that ability right now"
            }

            // Creatures that have lost all abilities cannot activate them (e.g., Deep Freeze)
            if (state.projectedState.hasLostAllAbilities(action.sourceId)) {
                // Only block the permanent's own abilities, not granted ones. Intrinsic
                // basic-land-subtype abilities (CR 305.7) count as "own" here too — a land hit
                // by Imprisoned in the Moon keeps its land subtype (only card types/abilities
                // are overwritten, not subtypes) but per ruling loses the mana ability that
                // subtype would otherwise imply.
                //
                // Exception: when an effect SET this land's basic types (Blood Moon / Zhao's
                // "nonbasic lands are Mountains"), the new type's intrinsic mana ability is
                // granted by that same effect (CR 305.7) and survives its ability removal —
                // so it stays activatable. Mirrors ManaAbilityEnumerator's `ownManaAbilities`.
                val isIntrinsicMana = IntrinsicManaAbilities.lookup(action.abilityId) != null
                val intrinsicSurvives = isIntrinsicMana &&
                    state.projectedState.hasBasicLandTypesSetByEffect(action.sourceId)
                val isOwnAbility = (cardDef?.script?.effectiveActivatedAbilities(classLevel)?.any { it.id == action.abilityId } == true)
                    || action.abilityId.value.startsWith("class_level_up_")
                    || isIntrinsicMana
                if (isOwnAbility && !intrinsicSurvives) {
                    return "This permanent has lost all abilities"
                }
            }
        }

        // Apply text-changing effects to cost and target filters
        val textReplacement = container.get<TextReplacementComponent>()
        val rawCost = if (textReplacement != null) {
            ability.cost.applyTextReplacement(textReplacement)
        } else {
            ability.cost
        }
        // Apply ability-specific generic cost reduction (e.g., The Dominion Bracelet's
        // "{X} less, where X is this creature's power"). Per Scryfall ruling, the reduced
        // cost is locked in here, before costs are paid. Then apply generic equip-cost reduction
        // (Éowyn) and finally Forge Anew's free-first-equip.
        val effectiveCost = castPermissionUtils.relaxAbilityCostColorsIfAny(
            state, action.sourceId,
            castPermissionUtils.applyFreeFirstEquipDiscount(
                castPermissionUtils.applyEquipCostReduction(
                    castPermissionUtils.applyActivatedAbilityCostReduction(
                        applyGenericCostReduction(rawCost, ability, state, action.sourceId, action.playerId, action.targets),
                        state, action.sourceId
                    ),
                    ability, state, action.playerId
                ),
                ability, state, action.playerId
            )
        )
        val effectiveTargetReqs = if (textReplacement != null) {
            ability.targetRequirements.map { it.applyTextReplacement(textReplacement) }
        } else {
            ability.targetRequirements
        }

        // Station-style multi-select batch (CR 702.184a): repeatCount > 1 over a tap-permanents
        // cost means "queue one activation per chosen creature". Validate the batch is well-formed
        // so a malformed action can't, e.g., tap one creature for three activations or reuse the
        // same creature twice. Per-creature legality (untapped/controlled/filter) is re-checked at
        // payment time in CostHandler.payTapPermanents for every slice.
        if (action.repeatCount > 1) {
            val tapAtom = effectiveCost.firstTapPermanentsAtomOrNull()
            if (tapAtom != null) {
                if (tapAtom.count != 1) {
                    return "Batch activation is only supported for single-creature tap costs"
                }
                if (effectiveTargetReqs.isNotEmpty()) {
                    return "Batch activation is not supported for abilities that require targets"
                }
                val tapped = action.costPayment?.tappedPermanents ?: emptyList()
                if (tapped.size != action.repeatCount) {
                    return "Batch tap-cost activation needs ${action.repeatCount} creatures, got ${tapped.size}"
                }
                if (tapped.toSet().size != tapped.size) {
                    return "Cannot tap the same creature for more than one activation"
                }
            }
        }

        // Check timing for planeswalker abilities
        if (ability.isPlaneswalkerAbility) {
            // Revel in Silence etc.: "can't activate planeswalkers' loyalty abilities this turn"
            if (state.getEntity(action.playerId)?.has<CantActivateLoyaltyAbilitiesComponent>() == true) {
                return "You can't activate loyalty abilities this turn"
            }
            if (!turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "Loyalty abilities can only be activated at sorcery speed"
            }
            // Rule 606.3: Only one loyalty ability per planeswalker per turn
            // (Oath of Teferi allows two activations per turn)
            val tracker = container.get<AbilityActivatedThisTurnComponent>()
            if (tracker != null && tracker.loyaltyActivationCount > 0) {
                val maxActivations = getMaxLoyaltyActivations(state, action.playerId)
                if (tracker.hasReachedLoyaltyLimit(maxActivations)) {
                    return if (maxActivations > 1) {
                        "Loyalty abilities can only be activated $maxActivations times per planeswalker each turn"
                    } else {
                        "Only one loyalty ability can be activated per planeswalker each turn"
                    }
                }
            }
        }

        // Check timing for sorcery-speed abilities ("Activate only as a sorcery").
        // Equip abilities are exempt while the controller has an active instant-speed-equip
        // permission (Forge Anew, Leonin Shikari) — CR 702.6e timing lifted. Mirror of the
        // ActivatedAbilityEnumerator gate so the validate() path agrees with what's offered.
        if (ability.timing == TimingRule.SorcerySpeed && !ability.isPlaneswalkerAbility) {
            val instantSpeedEquip = ability.isEquipAbility && castPermissionUtils.canEquipAtInstantSpeed(state, action.playerId)
            if (!instantSpeedEquip && !turnManager.canPlaySorcerySpeed(state, action.playerId)) {
                return "This ability can only be activated as a sorcery"
            }
        }

        // Check summoning sickness for TapAttachedCreature cost (before general cost check
        // to give a specific error message). Read creature-ness and haste from projected
        // state so a Vehicle / animated land currently being a creature is gated correctly.
        if (effectiveCost is AbilityCost.TapAttachedCreature ||
            (effectiveCost is AbilityCost.Composite && effectiveCost.costs.any { it is AbilityCost.TapAttachedCreature })) {
            val attachedId = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId
            if (attachedId != null) {
                val attachedContainer = state.getEntity(attachedId)
                if (attachedContainer != null && state.projectedState.isCreature(attachedId)) {
                    val hasSummoningSickness = attachedContainer.has<SummoningSicknessComponent>()
                    val hasHaste = state.projectedState.hasKeyword(attachedId, Keyword.HASTE)
                    if (hasSummoningSickness && !hasHaste) {
                        return "Enchanted creature has summoning sickness"
                    }
                }
            }
        }

        // Validate explicit payment sources
        if (action.paymentStrategy is PaymentStrategy.Explicit) {
            for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
                val sourceContainer = state.getEntity(sourceId)
                    ?: return "Mana source not found: $sourceId"
                if (sourceContainer.has<TappedComponent>()) {
                    return "Mana source is already tapped: $sourceId"
                }
            }
        }

        // Check cost requirements (using ManaSolver for mana costs to consider untapped sources)
        // If the ability has convoke or waterbend and the player provided alternative payment,
        // account for the reduced cost.
        val costAfterConvokeReduction = if ((ability.hasConvoke || ability.hasWaterbend) && action.alternativePayment != null && !action.alternativePayment.isEmpty) {
            val mc = extractManaCost(effectiveCost) ?: effectiveCost
            if (mc is ManaCost || effectiveCost.manaCostOrNull != null || effectiveCost is AbilityCost.Composite) {
                val reducedManaCost = extractManaCost(effectiveCost)?.let {
                    var reduced = it
                    if (ability.hasConvoke) reduced = alternativePaymentHandler.calculateReducedCostForAbility(reduced, action.alternativePayment)
                    if (ability.hasWaterbend) reduced = alternativePaymentHandler.calculateReducedCostForWaterbend(reduced, action.alternativePayment)
                    reduced
                }
                if (reducedManaCost != null) {
                    when (effectiveCost) {
                        is AbilityCost.Atom -> AbilityCost.Atom(CostAtom.Mana(reducedManaCost))
                        is AbilityCost.Composite -> AbilityCost.Composite(effectiveCost.costs.map { subCost ->
                            if (subCost.manaCostOrNull != null) AbilityCost.Atom(CostAtom.Mana(reducedManaCost)) else subCost
                        })
                        else -> effectiveCost
                    }
                } else effectiveCost
            } else effectiveCost
        } else effectiveCost

        val abilityPaymentContext = buildAbilityPaymentContext(cardComponent, state.projectedState, action.sourceId)

        if (action.paymentStrategy !is PaymentStrategy.Explicit && !canPayAbilityCostWithSources(state, costAfterConvokeReduction, action.sourceId, action.playerId, abilityPaymentContext)) {
            return when (effectiveCost) {
                is AbilityCost.Tap -> "This permanent is already tapped"
                is AbilityCost.TapAttachedCreature -> "Enchanted creature is tapped"
                is AbilityCost.Loyalty -> {
                    if (effectiveCost.change < 0) {
                        "Not enough loyalty to activate this ability"
                    } else {
                        "Cannot pay loyalty cost"
                    }
                }
                is AbilityCost.Atom -> when (effectiveCost.atom) {
                    is CostAtom.Mana -> "Not enough mana to activate this ability"
                    is CostAtom.PayLife -> "Not enough life to activate this ability"
                    else -> "Cannot pay ability cost"
                }
                else -> "Cannot pay ability cost"
            }
        }

        // Check summoning sickness for tap abilities. CR 302.6 restricts a *creature's* {T}/{Q}
        // activated ability — read creature-ness and haste from projected state so a Vehicle
        // or animated permanent that became a creature this turn is gated correctly. The
        // `!typeLine.isLand` carve-out is preserved (basic-land mana abilities are not
        // restricted by summoning sickness).
        if (effectiveCost is AbilityCost.Tap ||
            (effectiveCost is AbilityCost.Composite && effectiveCost.costs.any { it is AbilityCost.Tap })) {
            if (!cardComponent.typeLine.isLand && state.projectedState.isCreature(action.sourceId)) {
                val hasSummoningSickness = container.has<SummoningSicknessComponent>()
                val hasHaste = state.projectedState.hasKeyword(action.sourceId, Keyword.HASTE)
                if (hasSummoningSickness && !hasHaste) {
                    return "This creature has summoning sickness"
                }
            }
        }

        // Check activation restrictions
        for (restriction in ability.restrictions) {
            val error = checkActivationRestriction(state, action.playerId, action.sourceId, action.abilityId, restriction)
            if (error != null) return error
        }

        // Validate targets. Only the controller-chosen requirements are validated here — any
        // "… of an opponent's choice" requirement (Cuombajj Witches) is picked by an opponent in
        // a separate decision the handler raises at announcement, so it isn't on `action.targets`
        // yet at submission time (the opponent's pick is validated when it's made). See
        // [com.wingedsheep.sdk.scripting.targets.TargetChooser].
        val controllerTargetReqs = effectiveTargetReqs.filter { it.chooser == TargetChooser.Controller }
        if (controllerTargetReqs.isNotEmpty() && action.targets.isNotEmpty()) {
            val targetError = targetValidator.validateTargets(
                state,
                action.targets,
                controllerTargetReqs,
                action.playerId,
                sourceColors = cardComponent.colors,
                sourceSubtypes = cardComponent.typeLine.subtypes.map { it.value }.toSet(),
                sourceId = action.sourceId,
                // X-clamped target counts (e.g. Rot-Curse Rakshasa's Renew "X target creatures")
                // need the chosen X to validate more than one target — mirror the spell path.
                xValue = action.xValue
            )
            if (targetError != null) {
                return targetError
            }
        } else if (controllerTargetReqs.isNotEmpty() && action.targets.isEmpty()) {
            // An empty target list is only illegal when at least one controller-chosen
            // requirement is mandatory. For an ability whose controller targets are all
            // optional ("up to one target …", e.g. Boom Box), choosing no targets is a
            // legal activation, so don't reject it here.
            if (controllerTargetReqs.any { it.effectiveMinCount > 0 }) {
                return "This ability requires a target"
            }
        }

        return null
    }

    override fun execute(state: GameState, action: ActivateAbility): ExecutionResult {
        val container = state.getEntity(action.sourceId)
            ?: return ExecutionResult.error(state, "Source not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source is not a card")

        // Tokens (no registered CardDefinition) reach this path when activating granted abilities;
        // fall through with a null cardDef and let the granted-ability lookup succeed.
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)

        // Look up ability from card definition (including class-level abilities), granted abilities, or static grants
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel
        val staticGrants = getStaticGrantedAbilitiesWithGranter(action.sourceId, state)
        val staticGrantMatch = staticGrants.firstOrNull { it.first.id == action.abilityId }
        val ability = cardDef?.script?.effectiveActivatedAbilities(classLevel)?.find { it.id == action.abilityId }
            ?: cardDef?.let { findClassLevelUpAbility(it, container, action.abilityId) }
            ?: state.grantedActivatedAbilities
                .filter { it.entityId == action.sourceId }
                .map { it.ability }
                .find { it.id == action.abilityId }
            ?: staticGrantMatch?.first
            ?: resolveIntrinsicManaAbility(state, action.sourceId, action.abilityId)
            ?: return ExecutionResult.error(state, "Ability not found")
        val staticGranterId = staticGrantMatch?.second

        // Apply text-changing effects to cost
        val textReplacement = container.get<TextReplacementComponent>()
        val rawCost = if (textReplacement != null) {
            ability.cost.applyTextReplacement(textReplacement)
        } else {
            ability.cost
        }
        // Apply ability-specific generic cost reduction (e.g., The Dominion Bracelet's
        // "{X} less, where X is this creature's power"). Locked in before payment. Then apply
        // generic equip-cost reduction (Éowyn) and Forge Anew's free-first-equip discount.
        // Finally relax colored requirements when "mana of any type can be spent" applies (Sharkey).
        val effectiveCost = castPermissionUtils.relaxAbilityCostColorsIfAny(
            state, action.sourceId,
            castPermissionUtils.applyFreeFirstEquipDiscount(
                castPermissionUtils.applyEquipCostReduction(
                    castPermissionUtils.applyActivatedAbilityCostReduction(
                        applyGenericCostReduction(rawCost, ability, state, action.sourceId, action.playerId, action.targets),
                        state, action.sourceId
                    ),
                    ability, state, action.playerId
                ),
                ability, state, action.playerId
            )
        )

        // -------------------------------------------------------------------
        // "… of an opponent's choice" target selection (Cuombajj Witches).
        //
        // CR 601.2c (choose targets) precedes 601.2g–h (pay costs), so this runs before any cost
        // work. The controller's own targets already ride on `action.targets`; any opponent-chosen
        // requirement is selected here by routing a ChooseTargetsDecision to an opponent. The
        // resumer merges that pick into `action.targets` and re-enters with
        // `opponentTargetsChosen = true`, so this block is skipped on the second pass.
        // -------------------------------------------------------------------
        if (!action.opponentTargetsChosen) {
            val fullTargetReqs = if (textReplacement != null) {
                ability.targetRequirements.map { it.applyTextReplacement(textReplacement) }
            } else {
                ability.targetRequirements
            }
            val opponentReqs = fullTargetReqs.filter { it.chooser == TargetChooser.Opponent }
            if (opponentReqs.isNotEmpty()) {
                return pauseForOpponentChosenTargets(
                    state, action, cardComponent.name, fullTargetReqs, opponentReqs
                )
            }
        }

        // -------------------------------------------------------------------
        // TapXPermanents two-step UI flow (legal-actions submission path).
        //
        // When the legal-actions list surfaces an X-variable tap cost (Secluded
        // Starforge: "Tap X untapped artifacts you control"), the frontend
        // submits the bare `ActivateAbility` (xValue/costPayment empty) and
        // expects the engine to pause for two follow-up decisions: pick X,
        // then pick the X permanents to tap. Without this branch the engine
        // silently treats X=0, pays no cost, and resolves a no-op activation
        // — see SecludedStarforgeTest's "UI flow: choosing X=3 …" case.
        //
        // The engine-direct path (pre-filling xValue and tappedPermanents on
        // the action — used by the prior passing test and most server-side
        // composite flows) is untouched: both `xValue != null` and a non-empty
        // `tappedPermanents` skip past this fast-path.
        // -------------------------------------------------------------------
        val tapXCost = extractTapXPermanentsCost(effectiveCost)
        val alreadyTapping = (action.costPayment?.tappedPermanents?.isNotEmpty() == true)
        if (tapXCost != null && action.xValue == null && !alreadyTapping) {
            val tapTargets = costHandler.findUntappedMatchingPermanentsUnified(state, action.playerId, tapXCost.filter)
            val maxX = tapTargets.size
            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = com.wingedsheep.engine.core.ChooseNumberDecision(
                id = decisionId,
                playerId = action.playerId,
                prompt = "Choose X for ${cardComponent.name} (0-$maxX)",
                context = com.wingedsheep.engine.core.DecisionContext(
                    sourceId = action.sourceId,
                    sourceName = cardComponent.name,
                    phase = com.wingedsheep.engine.core.DecisionPhase.CASTING
                ),
                minValue = 0,
                maxValue = maxX
            )
            val continuation = com.wingedsheep.engine.core.ActivateAbilityChooseXContinuation(
                decisionId = decisionId,
                action = action,
                tapTargets = tapTargets
            )
            val pausedState = state
                .withPendingDecision(decision)
                .pushContinuation(continuation)
            val event = com.wingedsheep.engine.core.DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = action.playerId,
                decisionType = "CHOOSE_NUMBER",
                prompt = decision.prompt
            )
            return ExecutionResult.paused(pausedState, decision, listOf(event))
        }

        // -------------------------------------------------------------------
        // {X} *mana* cost pause (legal-actions submission path).
        //
        // When the cost contains `{X}` mana (Wizard's Rockets: "{X}, {T}, Sacrifice this artifact:
        // Add X mana...") the frontend submits the bare `ActivateAbility` with no xValue, expecting
        // the engine to ask which X to pay. Without this the handler defaults X to 0
        // (`action.xValue ?: 0`), pays nothing, and the ability produces no mana — the player never
        // gets to choose X. The engine-direct path (xValue pre-filled) skips this.
        // -------------------------------------------------------------------
        val manaXCost = extractManaCost(effectiveCost)
        if (manaXCost?.hasX == true && action.xValue == null && tapXCost == null) {
            val fixedMana = manaXCost.cmc // the non-X portion ({X} alone is 0; {1}{X} is 1)
            val maxX = (manaSolver.getAvailableManaCount(state, action.playerId) - fixedMana).coerceAtLeast(0)
            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = com.wingedsheep.engine.core.ChooseNumberDecision(
                id = decisionId,
                playerId = action.playerId,
                prompt = "Choose X for ${cardComponent.name} (0-$maxX)",
                context = com.wingedsheep.engine.core.DecisionContext(
                    sourceId = action.sourceId,
                    sourceName = cardComponent.name,
                    phase = com.wingedsheep.engine.core.DecisionPhase.CASTING
                ),
                minValue = 0,
                maxValue = maxX
            )
            val continuation = com.wingedsheep.engine.core.ActivateAbilityChooseManaXContinuation(
                decisionId = decisionId,
                action = action
            )
            val pausedState = state
                .withPendingDecision(decision)
                .pushContinuation(continuation)
            val event = com.wingedsheep.engine.core.DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = action.playerId,
                decisionType = "CHOOSE_NUMBER",
                prompt = decision.prompt
            )
            return ExecutionResult.paused(pausedState, decision, listOf(event))
        }

        // -------------------------------------------------------------------
        // ExileFromGraveyard cost-choice pause (legal-actions submission path).
        //
        // When the cost is `ExileFromGraveyard(count, filter)` (Rust Harvester:
        // "{2}, {T}, Exile an artifact card from your graveyard: ...") and the
        // player has more matching graveyard cards than the count, this is a
        // real choice — the engine must pause and ask which card(s) to exile,
        // not silently take the first N (CostHandler.exileCardsFromGraveyard
        // used to auto-pick when `exileChoices` was empty, dropping the
        // player's choice on the floor).
        //
        // Skipped when `exiledCards` is already pre-filled (engine-direct path
        // and resumed-replay case) or when candidates <= count (no real
        // choice).
        // -------------------------------------------------------------------
        val exileFromGraveyardCost = extractExileFromGraveyardCost(effectiveCost)
        val alreadyExiling = (action.costPayment?.exiledCards?.isNotEmpty() == true)
        if (exileFromGraveyardCost != null && !alreadyExiling) {
            val exileCandidates = costHandler.findMatchingCardsUnified(
                state,
                state.getZone(com.wingedsheep.engine.state.ZoneKey(action.playerId, Zone.GRAVEYARD)),
                exileFromGraveyardCost.filter,
                action.playerId
            )
            if (exileCandidates.size > exileFromGraveyardCost.count) {
                val decisionId = java.util.UUID.randomUUID().toString()
                val prompt = "Select ${exileFromGraveyardCost.count} card${if (exileFromGraveyardCost.count > 1) "s" else ""} to exile from graveyard for ${cardComponent.name}"
                val decision = com.wingedsheep.engine.core.SelectCardsDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = prompt,
                    context = com.wingedsheep.engine.core.DecisionContext(
                        sourceId = action.sourceId,
                        sourceName = cardComponent.name,
                        phase = com.wingedsheep.engine.core.DecisionPhase.CASTING
                    ),
                    options = exileCandidates,
                    minSelections = exileFromGraveyardCost.count,
                    maxSelections = exileFromGraveyardCost.count
                )
                val continuation = com.wingedsheep.engine.core.ActivateAbilityExileFromGraveyardContinuation(
                    decisionId = decisionId,
                    action = action,
                    exileCandidates = exileCandidates,
                    exileCount = exileFromGraveyardCost.count
                )
                val pausedState = state
                    .withPendingDecision(decision)
                    .pushContinuation(continuation)
                val event = com.wingedsheep.engine.core.DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = action.playerId,
                    decisionType = "SELECT_CARDS",
                    prompt = prompt
                )
                return ExecutionResult.paused(pausedState, decision, listOf(event))
            }
        }

        // -------------------------------------------------------------------
        // Sacrifice cost-choice pause (legal-actions submission path).
        //
        // When the cost is `Sacrifice(filter, count, excludeSelf)` (Sage of
        // Lat-Nam: "{T}, Sacrifice an artifact: Draw a card", Atog, Ashnod's
        // Altar, …) and the player controls more matching permanents than the
        // count, this is a real choice — the engine must pause and ask which
        // permanent(s) to sacrifice, not fail with "Not enough sacrifice
        // targets chosen" (an AI submitting a bare ActivateAbility with no
        // sacrifice chosen would otherwise spin forever).
        //
        // Skipped when `sacrificedPermanents` is already pre-filled
        // (engine-direct path and resumed-replay case) or when candidates <=
        // count (no real choice — Part 2 / CostHandler auto-picks). Mirrors the
        // ExileFromGraveyard pause block above.
        // -------------------------------------------------------------------
        val sacrificeCost = extractSacrificeCost(effectiveCost)
        val alreadySacrificing = (action.costPayment?.sacrificedPermanents?.isNotEmpty() == true)
        if (sacrificeCost != null && !alreadySacrificing) {
            val sacrificeCandidates = costHandler
                .findMatchingCardsUnified(state, state.getBattlefield(action.playerId), sacrificeCost.filter, action.playerId)
                .let { if (sacrificeCost.excludeSelf) it.filter { id -> id != action.sourceId } else it }
            // Normally we only pause when there's a real choice (candidates > count); the forced
            // case auto-picks. But "with different names" is always a real choice — the player must
            // pick a distinctly-named set even when candidates == count — so always pause for it.
            if (sacrificeCandidates.size > sacrificeCost.count || sacrificeCost.distinctNames) {
                val decisionId = java.util.UUID.randomUUID().toString()
                val prompt = "Select ${sacrificeCost.count} permanent${if (sacrificeCost.count > 1) "s" else ""} to sacrifice for ${cardComponent.name}"
                val decision = com.wingedsheep.engine.core.SelectCardsDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = prompt,
                    context = com.wingedsheep.engine.core.DecisionContext(
                        sourceId = action.sourceId,
                        sourceName = cardComponent.name,
                        phase = com.wingedsheep.engine.core.DecisionPhase.CASTING
                    ),
                    options = sacrificeCandidates,
                    minSelections = sacrificeCost.count,
                    maxSelections = sacrificeCost.count
                )
                val continuation = com.wingedsheep.engine.core.ActivateAbilitySacrificeContinuation(
                    decisionId = decisionId,
                    action = action,
                    sacrificeCandidates = sacrificeCandidates,
                    sacrificeCount = sacrificeCost.count,
                    distinctNames = sacrificeCost.distinctNames
                )
                val pausedState = state
                    .withPendingDecision(decision)
                    .pushContinuation(continuation)
                val event = com.wingedsheep.engine.core.DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = action.playerId,
                    decisionType = "SELECT_CARDS",
                    prompt = prompt
                )
                return ExecutionResult.paused(pausedState, decision, listOf(event))
            }
        }

        val executeAbilityContext = buildAbilityPaymentContext(cardComponent, state.projectedState, action.sourceId)

        var currentState = state
        val events = mutableListOf<GameEvent>()

        // Get player's mana pool
        val poolComponent = state.getEntity(action.playerId)?.get<ManaPoolComponent>()
            ?: ManaPoolComponent()
        var manaPool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless,
            restrictedMana = poolComponent.restrictedMana
        )

        // Pay mana costs before paying other costs
        var effectiveManaCost = extractManaCost(effectiveCost)
        val xValue = action.xValue ?: 0

        // Apply convoke payment for abilities with hasConvoke (e.g., Heirloom Epic)
        if (effectiveManaCost != null && ability.hasConvoke && action.alternativePayment != null && !action.alternativePayment.isEmpty) {
            val convokeResult = alternativePaymentHandler.applyConvokeForAbility(
                currentState, effectiveManaCost, action.alternativePayment, action.playerId
            )
            effectiveManaCost = convokeResult.reducedCost
            currentState = convokeResult.newState
            events.addAll(convokeResult.events)
        }

        // Apply waterbend payment for abilities with hasWaterbend (Avatar: The Last Airbender) —
        // tap untapped artifacts/creatures you control, each paying {1} of the generic cost.
        if (effectiveManaCost != null && ability.hasWaterbend && action.alternativePayment != null && !action.alternativePayment.isEmpty) {
            val waterbendResult = alternativePaymentHandler.applyWaterbendForAbility(
                currentState, effectiveManaCost, action.alternativePayment, action.playerId
            )
            effectiveManaCost = waterbendResult.reducedCost
            currentState = waterbendResult.newState
            events.addAll(waterbendResult.events)
        }

        val manaCost = effectiveManaCost
        // Only pass xValue to auto-tap when X is in the mana cost itself (not in a non-mana cost like counter removal)
        val manaXValue = if (manaCost?.hasX == true) xValue else 0
        // If the outer ability's cost includes Tap, the source itself cannot also be used
        // as a mana source — the single "tap" it has is already consumed by the outer cost.
        val selfExcludedSources = if (hasTapCost(effectiveCost)) setOf(action.sourceId) else emptySet()
        if (manaCost != null) {
            when (action.paymentStrategy) {
                is PaymentStrategy.Explicit -> {
                    // Tap only the minimum subset of chosen sources required to cover the
                    // (already convoke-reduced) mana cost — the client's auto-tap preview
                    // is computed against the full cost and may over-select. Solving with
                    // the non-chosen sources excluded matches the behavior in
                    // CastPaymentProcessor.explicitPay and keeps validation and execution
                    // in sync. Mana pool deduction is skipped by stripping the Mana cost
                    // below; tapping the solved subset is the payment.
                    val chosen = action.paymentStrategy.manaAbilitiesToActivate.toSet()
                    val excluded = manaSolver.findAvailableManaSources(currentState, action.playerId)
                        .map { it.entityId }
                        .filter { it !in chosen }
                        .toSet() + selfExcludedSources
                    val solution = manaSolver.solve(
                        currentState, action.playerId, manaCost, manaXValue, excludeSources = excluded, xManaRestriction = ability.xManaRestriction
                    ) ?: return ExecutionResult.error(state, "Selected mana sources cannot pay this ability's cost")
                    for (source in solution.sources) {
                        val (tappedState, tapEvent) = tap(currentState, source.entityId)
                        currentState = tappedState
                        tapEvent?.let(events::add)
                    }
                }
                else -> {
                    val autoTapResult = autoTapForManaCost(currentState, action.playerId, manaPool, manaCost, cardComponent.name, manaXValue, selfExcludedSources, executeAbilityContext, ability.xManaRestriction)
                        ?: return ExecutionResult.error(state, "Not enough mana to activate this ability")
                    currentState = autoTapResult.newState
                    manaPool = autoTapResult.newPool
                    events.addAll(autoTapResult.events)
                }
            }
        }

        // Station-style multi-select batch (CR 702.184a): when repeatCount > 1 over a single-
        // creature tap cost, `tappedPermanents` holds one creature per queued activation. Each
        // activation taps exactly its own creature, so slice the list — this activation gets the
        // first creature; the repeat loop below consumes the rest one at a time. For every other
        // ability (no tap cost, or repeatCount == 1) the slice is the whole list, unchanged.
        val tapBatchAtom = if (action.repeatCount > 1) effectiveCost.firstTapPermanentsAtomOrNull() else null
        val isTapBatch = tapBatchAtom != null && tapBatchAtom.count == 1 &&
            (action.costPayment?.tappedPermanents?.size ?: 0) == action.repeatCount
        val firstTapSlice = if (isTapBatch) {
            listOf(action.costPayment!!.tappedPermanents.first())
        } else {
            action.costPayment?.tappedPermanents ?: emptyList()
        }

        // Build cost payment choices from the action
        val costChoices = CostPaymentChoices(
            sacrificeChoices = action.costPayment?.sacrificedPermanents ?: emptyList(),
            discardChoices = action.costPayment?.discardedCards ?: emptyList(),
            exileChoices = action.costPayment?.exiledCards ?: emptyList(),
            tapChoices = firstTapSlice,
            bounceChoices = action.costPayment?.bouncedPermanents ?: emptyList(),
            xValue = xValue,
            counterRemovalChoices = action.costPayment?.counterRemovals ?: emptyMap(),
            blightChoices = action.costPayment?.blightTargets ?: emptyList(),
            granterId = staticGranterId
        )

        // Snapshot projected subtypes and P/T of sacrifice targets before zone change
        // (Rule 112.7a / 608.2h — "as it last existed on the battlefield")
        val sacrificeTargetIds = action.costPayment?.sacrificedPermanents ?: emptyList()
        val sacrificedSnapshots = captureEntitySnapshots(sacrificeTargetIds, currentState.projectedState)

        // Mirror sacrifice snapshots for tapped-as-cost permanents — they may leave the
        // battlefield in response while the ability is on the stack.
        val tappedTargetIds = firstTapSlice
        val tappedSnapshots = captureEntitySnapshots(tappedTargetIds, currentState.projectedState)

        // Snapshot the source's counters before a self-exile / self-sacrifice cost wipes them
        // (CR 112.7a / 122.2), so the effect can read the pre-cost count via
        // DynamicAmount.LastKnownSourceCounters (Lost Isle Calling).
        val lastKnownSourceCounters: Map<String, Int> =
            if (costExilesOrSacrificesSelf(effectiveCost)) {
                currentState.getEntity(action.sourceId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.CountersComponent>()
                    ?.counters
                    ?.filterValues { it > 0 }
                    ?.mapKeys { (type, _) ->
                        com.wingedsheep.engine.handlers.effects.permanent.counters
                            .counterTypeToString(type)
                    } ?: emptyMap()
            } else emptyMap()

        // Snapshot the source's projected P/T before a self-exile / self-sacrifice cost moves it
        // off the battlefield (CR 112.7a / 608.2h), so an effect that reads its own power — e.g.
        // "Sacrifice this creature: it deals damage equal to its power" (Ghitu Fire-Eater, Cinder
        // Shade, Blazing Bomb's Blow Up) — sees the pre-sacrifice power rather than zero. Mirrors
        // lastKnownSourceCounters above.
        val lastKnownSourceSnapshot: com.wingedsheep.engine.state.components.stack.EntitySnapshot? =
            if (costExilesOrSacrificesSelf(effectiveCost)) {
                captureEntitySnapshots(listOf(action.sourceId), currentState.projectedState).firstOrNull()
            } else null

        // Snapshot the entity ids attached to the source before a self-exile / self-sacrifice cost
        // moves it off the battlefield (CR 112.7a). The host's live AttachmentsComponent is gone by
        // resolution, so capture it now — read via CardSource.LastKnownEquipmentAttachedToSource to
        // re-attach "an Equipment that was attached to it" (Zack Fair). Mirrors lastKnownSourceCounters.
        val lastKnownSourceAttachments: List<EntityId> =
            if (costExilesOrSacrificesSelf(effectiveCost)) {
                currentState.getEntity(action.sourceId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent>()
                    ?.attachedIds
                    ?: emptyList()
            } else emptyList()

        // When using Explicit payment, mana sources were already tapped above —
        // strip the Mana portion so payAbilityCost doesn't try to deduct from the pool.
        // When convoke was applied, replace the mana portion with the reduced cost.
        val costForPayment = if (action.paymentStrategy is PaymentStrategy.Explicit) {
            stripManaCost(effectiveCost)
        } else if ((ability.hasConvoke || ability.hasWaterbend) && action.alternativePayment != null && !action.alternativePayment.isEmpty && manaCost != null) {
            // Convoke/waterbend reduced the mana cost — update the cost structure so payAbilityCost
            // deducts the reduced amount from the pool instead of the original full amount
            when (effectiveCost) {
                is AbilityCost.Atom -> AbilityCost.Atom(CostAtom.Mana(manaCost))
                is AbilityCost.Composite -> AbilityCost.Composite(effectiveCost.costs.map { subCost ->
                    if (subCost.manaCostOrNull != null) AbilityCost.Atom(CostAtom.Mana(manaCost)) else subCost
                })
                else -> effectiveCost
            }
        } else {
            effectiveCost
        }

        // Pay the cost (using effective cost with text replacements applied)
        val costResult = costHandler.payAbilityCost(
            currentState,
            costForPayment,
            action.sourceId,
            action.playerId,
            manaPool,
            costChoices,
            executeAbilityContext,
        )

        if (!costResult.success) {
            return ExecutionResult.error(state, costResult.error ?: "Failed to pay ability cost")
        }

        currentState = costResult.newState!!
        manaPool = costResult.newManaPool!!

        // Collect events from cost payment (e.g., sacrifice events)
        events.addAll(costResult.events)

        // Cost-payment events drive triggered abilities (e.g., a mana ability whose cost
        // sacrifices the source — Wizard's Rockets: "{X}, {T}, Sacrifice this artifact: ..."
        // — fires its dies/leaves-the-battlefield trigger). The mana-ability path resolves off
        // the stack and returns early, so capture these now to detect triggers before returning.
        // Scoped to cost-payment events so mana-production events keep their existing inline
        // handling (resolveAdditionalManaOnSourceTap etc.).
        val costPaymentEvents = costResult.events

        // Deduct X mana from the pool. ManaPool.pay() skips X symbols ("handled by caller"),
        // so we must explicitly spend the X portion here (same pattern as CastSpellHandler.autoPay).
        // Skip for Explicit payment — sources were already tapped to cover the full cost including X.
        if (action.paymentStrategy !is PaymentStrategy.Explicit && manaCost != null && manaCost.hasX && xValue > 0) {
            val xSymbolCount = manaCost.xCount.coerceAtLeast(1)
            var xRemainingToPay = xValue * xSymbolCount
            val xManaRestriction = ability.xManaRestriction
            val xColorsAllowed: Set<Color> =
                if (xManaRestriction.isEmpty()) Color.entries.toSet() else xManaRestriction

            // Spend colorless first for X — never allowed when X is color-restricted ("spend only [colors] on X").
            if (xManaRestriction.isEmpty()) {
                while (xRemainingToPay > 0 && manaPool.colorless > 0) {
                    manaPool = manaPool.spendColorless()!!
                    xRemainingToPay--
                }
            }

            // Spend colored mana for remaining X (restricted to allowed colors).
            for (color in Color.entries) {
                if (color !in xColorsAllowed) continue
                while (xRemainingToPay > 0 && manaPool.get(color) > 0) {
                    manaPool = manaPool.spend(color)!!
                    xRemainingToPay--
                }
            }
        }

        // Always update mana pool on state after cost payment.
        // autoTapForManaCost writes the enriched (pre-payment) pool to state,
        // so we must unconditionally write the post-payment pool.
        currentState = currentState.updateEntity(action.playerId) { c ->
            c.with(ManaPoolComponent(
                white = manaPool.white,
                blue = manaPool.blue,
                black = manaPool.black,
                red = manaPool.red,
                green = manaPool.green,
                colorless = manaPool.colorless,
                restrictedMana = manaPool.restrictedMana
            ))
        }

        // Emit events for cost types. Tap/TapAttachedCreature/TapXPermanents taps are emitted by
        // the tap atom inside costHandler.payAbilityCost (folded in via costResult.events above), so
        // only the loyalty change — which payAbilityCost mutates without an event — is emitted here.
        val abilityCost = ability.cost
        if (abilityCost is AbilityCost.Loyalty) {
            events.add(LoyaltyChangedEvent(action.sourceId, cardComponent.name, abilityCost.change))
        }

        // Snapshot of the activation's cost-side events (cost payment + the {T}/tap/loyalty events
        // emitted just above) before any mana-production event is appended. The mana-ability path
        // resolves off the stack and returns early, so it must run trigger detection over this set
        // — including the {T} TappedEvent — so an ANY-binding "whenever an artifact becomes tapped"
        // trigger (Powerleech, Tap Watcher) fires when a {T} mana ability is activated.
        val activationCostEvents = events.toList()

        // Track per-turn activation if the ability has an OncePerTurn or MaxPerTurn restriction
        fun isPerTurnTracked(r: ActivationRestriction): Boolean =
            r is ActivationRestriction.OncePerTurn || r is ActivationRestriction.MaxPerTurn ||
                (r is ActivationRestriction.All && r.restrictions.any { isPerTurnTracked(it) })
        if (ability.restrictions.any { isPerTurnTracked(it) }) {
            // Only track if source is still on the battlefield (it might have been bounced as cost)
            if (currentState.getEntity(action.sourceId) != null) {
                currentState = currentState.updateEntity(action.sourceId) { c ->
                    val tracker = c.get<AbilityActivatedThisTurnComponent>() ?: AbilityActivatedThisTurnComponent()
                    c.with(tracker.withActivated(ability.id))
                }
            }
        }

        // Track once-ever activation if the ability has an Once restriction
        if (ability.restrictions.any { it is ActivationRestriction.Once || (it is ActivationRestriction.All && it.restrictions.any { r -> r is ActivationRestriction.Once }) }) {
            if (currentState.getEntity(action.sourceId) != null) {
                currentState = currentState.updateEntity(action.sourceId) { c ->
                    val tracker = c.get<AbilityActivatedEverComponent>() ?: AbilityActivatedEverComponent()
                    c.with(tracker.withActivated(ability.id))
                }
            }
        }

        // Track planeswalker loyalty ability activation (Rule 606.3: once per planeswalker per turn)
        if (ability.isPlaneswalkerAbility) {
            if (currentState.getEntity(action.sourceId) != null) {
                currentState = currentState.updateEntity(action.sourceId) { c ->
                    val tracker = c.get<AbilityActivatedThisTurnComponent>() ?: AbilityActivatedThisTurnComponent()
                    c.with(tracker.withLoyaltyActivated())
                }
            }
        }

        // Track equip activations this turn (Forge Anew's free-first-equip keys off count == 0).
        if (ability.isEquipAbility) {
            currentState = currentState.updateEntity(action.playerId) { c ->
                val tracker = c.get<com.wingedsheep.engine.state.components.player.EquipActivationsThisTurnComponent>()
                    ?: com.wingedsheep.engine.state.components.player.EquipActivationsThisTurnComponent()
                c.with(tracker.copy(count = tracker.count + 1))
            }
        }

        // Apply text replacement if the source has a TextReplacementComponent
        var finalEffect = if (textReplacement != null) {
            ability.effect.applyTextReplacement(textReplacement)
        } else {
            ability.effect
        }

        // Mana abilities don't use the stack
        if (ability.isManaAbility) {
            // Check for an attached aura that overrides the produced mana color
            // (e.g., Shimmerwilds Growth: "Enchanted land is the chosen color").
            val overrideColor = findEnchantedLandManaColorOverride(currentState, action.sourceId)
            if (overrideColor != null && finalEffect is AddManaEffect) {
                finalEffect = finalEffect.copy(color = overrideColor)
            }
            // Filter-based mana-color replacement (Pulse of Llanowar): a matched land produces
            // one mana of a color of its controller's choice instead of its normal mana. Swapping
            // the base effect for AddManaOfChoiceEffect routes the choice through the existing
            // any-color machinery (action.manaColorChoice on a manual tap, or a resolution-time
            // color decision if none was supplied).
            if (landMatchesManaColorReplacement(currentState, action.sourceId, action.playerId)) {
                finalEffect = when (val fe = finalEffect) {
                    is AddManaEffect -> AddManaOfChoiceEffect(ManaColorSet.AnyColor, fe.amount)
                    is AddColorlessManaEffect -> AddManaOfChoiceEffect(ManaColorSet.AnyColor, fe.amount)
                    else -> finalEffect
                }
            }
            val context = EffectContext(
                sourceId = action.sourceId,
                controllerId = action.playerId,
                granterId = staticGranterId,
                targets = action.targets,
                // Thread the chosen X so X-based mana abilities produce the right amount
                // ("{X}, {T}, Sacrifice this: Add X mana..." — Wizard's Rockets). Without
                // this, DynamicAmount.XValue resolves to 0 and the ability adds no mana.
                xValue = action.xValue,
                manaColorChoice = action.manaColorChoice
            )

            val effectResult = effectExecutorRegistry.execute(currentState, finalEffect, context).toExecutionResult()
            if (effectResult.isPaused) {
                // The mana ability's effect paused for a decision (e.g. choosing colors for
                // "add X mana in any combination of colors"). Any triggered ability that fired
                // from the cost payment (e.g. the source's dies trigger when sacrificed —
                // Wizard's Rockets: "When this artifact is put into a graveyard..., draw a card")
                // must survive that pause. Queue it as a PendingTriggersContinuation beneath the
                // in-flight decision so it's put on the stack once the ability finishes resolving
                // (mirrors PassPriorityHandler / SubmitDecisionHandler mid-resolution handling).
                val deferred = triggerDetector.detectTriggers(effectResult.state, costPaymentEvents)
                if (deferred.isNotEmpty()) {
                    val pending = com.wingedsheep.engine.core.PendingTriggersContinuation(
                        decisionId = "mana-ability-cost-triggers-${java.util.UUID.randomUUID()}",
                        remainingTriggers = deferred
                    )
                    // Insert at the BOTTOM of the continuation stack so the cost trigger is put on
                    // the stack only after the whole mana ability finishes resolving — including a
                    // multi-step "any combination of colors" effect that pauses once per mana. The
                    // stack here holds only frames pushed by this activation's effect, so bottom
                    // insertion can't jump ahead of unrelated work.
                    val newStack = listOf(pending) + effectResult.state.continuationStack
                    return ExecutionResult.paused(
                        effectResult.state.copy(continuationStack = newStack),
                        effectResult.pendingDecision!!,
                        events + effectResult.events
                    )
                }
                return effectResult
            }
            if (!effectResult.isSuccess) {
                return effectResult
            }

            currentState = effectResult.newState

            // Check for Damping Sphere-style mana dampening on lands
            var manaDampened = false
            if (cardComponent.typeLine.isLand && hasDampLandManaProduction(currentState)) {
                val oldPool = state.getEntity(action.playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
                val newPool = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
                val totalManaProduced = (newPool.white - oldPool.white) +
                    (newPool.blue - oldPool.blue) +
                    (newPool.black - oldPool.black) +
                    (newPool.red - oldPool.red) +
                    (newPool.green - oldPool.green) +
                    (newPool.colorless - oldPool.colorless)

                if (totalManaProduced >= 2) {
                    // Replace with 1 colorless mana: revert to old pool + 1 colorless.
                    // Restricted mana the player had floating before this activation
                    // is preserved — Damping Sphere only replaces what the land just
                    // produced, not what was already in the pool.
                    val dampenedPool = ManaPoolComponent(
                        white = oldPool.white,
                        blue = oldPool.blue,
                        black = oldPool.black,
                        red = oldPool.red,
                        green = oldPool.green,
                        colorless = oldPool.colorless + 1,
                        restrictedMana = oldPool.restrictedMana
                    )
                    currentState = currentState.updateEntity(action.playerId) { container ->
                        container.with(dampenedPool)
                    }
                    manaDampened = true
                }
            }

            // Emit ManaAddedEvent — if dampened, always emit 1 colorless
            val manaEvent: ManaAddedEvent? = if (manaDampened) {
                ManaAddedEvent(
                    playerId = action.playerId,
                    sourceId = action.sourceId,
                    sourceName = cardComponent.name,
                    colorless = 1
                )
            } else when (val effect = finalEffect) {
                is AddManaEffect -> {
                    val amount = dynamicAmountEvaluator.evaluate(state, effect.amount, context)
                    ManaAddedEvent(
                        playerId = action.playerId,
                        sourceId = action.sourceId,
                        sourceName = cardComponent.name,
                        white = if (effect.color == Color.WHITE) amount else 0,
                        blue = if (effect.color == Color.BLUE) amount else 0,
                        black = if (effect.color == Color.BLACK) amount else 0,
                        red = if (effect.color == Color.RED) amount else 0,
                        green = if (effect.color == Color.GREEN) amount else 0,
                        colorless = 0
                    )
                }
                is AddColorlessManaEffect -> {
                    val amount = dynamicAmountEvaluator.evaluate(state, effect.amount, context)
                    ManaAddedEvent(
                        playerId = action.playerId,
                        sourceId = action.sourceId,
                        sourceName = cardComponent.name,
                        colorless = amount
                    )
                }
                is AddManaOfChoiceEffect -> manaAddedEventFromPoolDelta(
                    state, currentState, action, cardComponent
                )
                is AddAnyColorManaSpendOnChosenTypeEffect -> {
                    val chosenColor = action.manaColorChoice ?: Color.GREEN
                    val amount = dynamicAmountEvaluator.evaluate(state, effect.amount, context)
                    ManaAddedEvent(
                        playerId = action.playerId,
                        sourceId = action.sourceId,
                        sourceName = cardComponent.name,
                        white = if (chosenColor == Color.WHITE) amount else 0,
                        blue = if (chosenColor == Color.BLUE) amount else 0,
                        black = if (chosenColor == Color.BLACK) amount else 0,
                        red = if (chosenColor == Color.RED) amount else 0,
                        green = if (chosenColor == Color.GREEN) amount else 0,
                        colorless = 0
                    )
                }
                is CompositeEffect -> {
                    when (val manaEffect = effect.effects.firstOrNull {
                        it is AddManaEffect ||
                            it is AddColorlessManaEffect ||
                            it is AddManaOfChoiceEffect ||
                            it is AddAnyColorManaSpendOnChosenTypeEffect
                    }) {
                        is AddManaEffect -> {
                            val amount = dynamicAmountEvaluator.evaluate(state, manaEffect.amount, context)
                            ManaAddedEvent(
                                playerId = action.playerId,
                                sourceId = action.sourceId,
                                sourceName = cardComponent.name,
                                white = if (manaEffect.color == Color.WHITE) amount else 0,
                                blue = if (manaEffect.color == Color.BLUE) amount else 0,
                                black = if (manaEffect.color == Color.BLACK) amount else 0,
                                red = if (manaEffect.color == Color.RED) amount else 0,
                                green = if (manaEffect.color == Color.GREEN) amount else 0,
                                colorless = 0
                            )
                        }
                        is AddColorlessManaEffect -> {
                            val amount = dynamicAmountEvaluator.evaluate(state, manaEffect.amount, context)
                            ManaAddedEvent(
                                playerId = action.playerId,
                                sourceId = action.sourceId,
                                sourceName = cardComponent.name,
                                colorless = amount
                            )
                        }
                        is AddManaOfChoiceEffect -> manaAddedEventFromPoolDelta(
                            state, currentState, action, cardComponent
                        )
                        is AddAnyColorManaSpendOnChosenTypeEffect -> {
                            val chosenColor = action.manaColorChoice ?: Color.GREEN
                            val amount = dynamicAmountEvaluator.evaluate(state, manaEffect.amount, context)
                            ManaAddedEvent(
                                playerId = action.playerId,
                                sourceId = action.sourceId,
                                sourceName = cardComponent.name,
                                white = if (chosenColor == Color.WHITE) amount else 0,
                                blue = if (chosenColor == Color.BLUE) amount else 0,
                                black = if (chosenColor == Color.BLACK) amount else 0,
                                red = if (chosenColor == Color.RED) amount else 0,
                                green = if (chosenColor == Color.GREEN) amount else 0,
                                colorless = 0
                            )
                        }
                        else -> null
                    }
                }
                else -> null
            }

            if (manaEvent != null) {
                events.add(manaEvent)
            }

            // Check for "additional mana on tap" auras (e.g., Elvish Guidance)
            // These are triggered mana abilities that resolve immediately
            val additionalManaResult = resolveAdditionalManaOnTap(
                currentState, action.sourceId, action.playerId, events + effectResult.events
            )
            currentState = additionalManaResult.state

            // Check for global "additional mana whenever a matching source is tapped for mana"
            // (Lavaleaper: basic land mirror; Badgermole Cub: creature → +{G}).
            // Triggered mana ability — resolves immediately without the stack.
            val onSourceTapResult = resolveAdditionalManaOnSourceTap(
                currentState, action.sourceId, action.playerId, manaEvent, additionalManaResult.events
            )
            currentState = onSourceTapResult.state
            var allManaEvents = onSourceTapResult.events

            // Emit a "land tapped for mana" event so triggers like Overabundance / Mana Flare
            // ("whenever a player taps a land for mana") can fire. Manual-tap path only —
            // automatic cost payment adds mana via the solver without re-entering this handler.
            if (cardComponent.typeLine.isLand) {
                allManaEvents = allManaEvents + LandTappedForManaEvent(
                    tapperId = action.playerId,
                    landId = action.sourceId,
                    landName = cardComponent.name
                )
            }

            // Resolve "additional one mana of any color" tap bonuses (Fertile Ground). Unlike the
            // fixed/mirror bonuses above these need a per-tap color choice, so this may pause for a
            // color decision (resuming via ChooseAnyColorTapBonusContinuation).
            val anyColorBonuses = tappedForManaBonusResolver.collect(currentState, action.sourceId, action.playerId)
            val bonusResult = tappedForManaBonusResolver.drive(currentState, anyColorBonuses, allManaEvents)
            if (bonusResult.isPaused) return bonusResult

            // A mana ability whose cost lacks {T} (e.g. Ashnod's Altar's "Sacrifice a creature: Add
            // {C}{C}") still satisfies the Antiquities "activates an ability without {T} in its
            // activation cost" template (Haunting Wind / Powerleech / Artifact Possession). Mana
            // abilities resolve off the stack, so StackResolver never emits AbilityActivatedEvent
            // for them — emit it here. The common tap-for-mana case (cost has {T}) is skipped, so
            // there's no behavior change or client-log noise for ordinary mana sources.
            val manaAbilityActivatedEvents: List<GameEvent> =
                if (!hasTapCost(effectiveCost)) {
                    listOf(
                        AbilityActivatedEvent(
                            sourceId = action.sourceId,
                            sourceName = cardComponent.name,
                            controllerId = action.playerId,
                            abilityEntityId = null,
                            costsTap = false,
                            isManaAbility = true
                        )
                    )
                } else emptyList()

            // Detect and queue any triggered abilities from the activation — the cost-side events
            // (a sacrificed source's dies trigger, the {T} TappedEvent for an artifact-tap trigger)
            // plus the non-{T} mana-ability activation event above. Such triggered abilities still
            // use the stack even though the mana ability itself resolves off it.
            val activationTriggerEvents = activationCostEvents + manaAbilityActivatedEvents
            val resultEvents = bonusResult.events + manaAbilityActivatedEvents
            val costTriggers = triggerDetector.detectTriggers(bonusResult.newState, activationTriggerEvents)
            if (costTriggers.isNotEmpty()) {
                val triggerResult = triggerProcessor.processTriggers(bonusResult.newState, costTriggers)
                if (triggerResult.isPaused) {
                    return ExecutionResult.paused(
                        triggerResult.state.withPriority(action.playerId),
                        triggerResult.pendingDecision!!,
                        resultEvents + triggerResult.events
                    )
                }
                return ExecutionResult.success(
                    triggerResult.newState.withPriority(action.playerId),
                    resultEvents + triggerResult.events
                )
            }
            return if (manaAbilityActivatedEvents.isEmpty()) bonusResult
            else ExecutionResult.success(bonusResult.newState, resultEvents)
        }

        // Non-mana abilities go on the stack
        val abilityOnStack = ActivatedAbilityOnStackComponent(
            sourceId = action.sourceId,
            sourceName = cardComponent.name,
            controllerId = action.playerId,
            effect = finalEffect,
            sacrificedPermanents = sacrificedSnapshots,
            xValue = action.xValue,
            tappedPermanents = firstTapSlice,
            tappedEntitySnapshots = tappedSnapshots,
            lastKnownSourceCounters = lastKnownSourceCounters,
            lastKnownSourceSnapshot = lastKnownSourceSnapshot,
            lastKnownSourceAttachments = lastKnownSourceAttachments,
            descriptionOverride = ability.descriptionOverride,
            abilityIdentity = com.wingedsheep.sdk.scripting.AbilityIdentity(
                cardComponent.cardDefinitionId, ability.id
            ),
            granterId = staticGranterId
        )

        // Apply text-changing effects to the target requirements for resolution-time re-validation
        val effectiveTargetReqs = if (textReplacement != null) {
            ability.targetRequirements.map { it.applyTextReplacement(textReplacement) }
        } else {
            ability.targetRequirements
        }

        var stackResult = stackResolver.putActivatedAbility(
            currentState, abilityOnStack, action.targets,
            targetRequirements = effectiveTargetReqs,
            costsTap = hasTapCost(effectiveCost)
        )
        currentState = stackResult.newState
        events.addAll(stackResult.events)

        // Handle repeated activations (repeatCount > 1)
        if (action.repeatCount > 1) {
            for (i in 2..action.repeatCount) {
                // Re-read mana pool from current state
                val repeatPoolComponent = currentState.getEntity(action.playerId)?.get<ManaPoolComponent>()
                    ?: ManaPoolComponent()
                var repeatPool = ManaPool(
                    white = repeatPoolComponent.white,
                    blue = repeatPoolComponent.blue,
                    black = repeatPoolComponent.black,
                    red = repeatPoolComponent.red,
                    green = repeatPoolComponent.green,
                    colorless = repeatPoolComponent.colorless
                )

                // Auto-tap for mana cost
                if (manaCost != null) {
                    val autoTapResult = autoTapForManaCost(currentState, action.playerId, repeatPool, manaCost, cardComponent.name, 0, abilityContext = executeAbilityContext)
                        ?: break // Can't afford — stop early
                    currentState = autoTapResult.newState
                    repeatPool = autoTapResult.newPool
                    events.addAll(autoTapResult.events)
                }

                // Station-style batch: this activation taps the i-th chosen creature (1-indexed
                // list, so iteration `i` consumes element `i - 1`). Other repeatable abilities
                // (mana-only) carry no tap choices, so the slice is empty and the cost re-pays from
                // mana as before. Snapshot the creature before it's tapped (Rule 112.7a) so
                // DynamicAmount.StationCharge reads its power off this instance's own snapshot.
                val repeatTapSlice = if (isTapBatch) listOf(action.costPayment!!.tappedPermanents[i - 1]) else emptyList()
                val repeatTapSnapshots = captureEntitySnapshots(repeatTapSlice, currentState.projectedState)

                // Pay the cost
                val repeatCostResult = costHandler.payAbilityCost(
                    currentState, effectiveCost, action.sourceId, action.playerId, repeatPool, CostPaymentChoices(tapChoices = repeatTapSlice), executeAbilityContext
                )
                if (!repeatCostResult.success) break // Can't pay — stop early

                currentState = repeatCostResult.newState!!
                repeatPool = repeatCostResult.newManaPool!!
                events.addAll(repeatCostResult.events)

                // Update mana pool on state
                currentState = currentState.updateEntity(action.playerId) { c ->
                    c.with(ManaPoolComponent(
                        white = repeatPool.white,
                        blue = repeatPool.blue,
                        black = repeatPool.black,
                        red = repeatPool.red,
                        green = repeatPool.green,
                        colorless = repeatPool.colorless
                    ))
                }

                // Put another ability on the stack
                val repeatAbilityOnStack = ActivatedAbilityOnStackComponent(
                    sourceId = action.sourceId,
                    sourceName = cardComponent.name,
                    controllerId = action.playerId,
                    effect = finalEffect,
                    sacrificedPermanents = emptyList(),
                    xValue = action.xValue,
                    tappedPermanents = repeatTapSlice,
                    tappedEntitySnapshots = repeatTapSnapshots,
                    descriptionOverride = ability.descriptionOverride,
                    abilityIdentity = com.wingedsheep.sdk.scripting.AbilityIdentity(
                        cardComponent.cardDefinitionId, ability.id
                    ),
                    granterId = staticGranterId
                )
                val repeatStackResult = stackResolver.putActivatedAbility(
                    currentState, repeatAbilityOnStack, action.targets,
                    targetRequirements = effectiveTargetReqs
                )
                currentState = repeatStackResult.newState
                events.addAll(repeatStackResult.events)
            }
        }

        val allEvents = events.toList()

        // Detect and process triggers from cost payment (e.g., sacrifice death triggers)
        val triggers = triggerDetector.detectTriggers(currentState, allEvents)
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(currentState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state.withPriority(action.playerId),
                    triggerResult.pendingDecision!!,
                    allEvents + triggerResult.events
                )
            }

            return ExecutionResult.success(
                triggerResult.newState.withPriority(action.playerId),
                allEvents + triggerResult.events
            )
        }

        return ExecutionResult.success(currentState, allEvents)
    }

    /**
     * Raise a [com.wingedsheep.engine.core.ChooseTargetsDecision] routed to an opponent for an
     * activated ability's "… of an opponent's choice" target requirement(s) (Cuombajj Witches),
     * and push the continuation that resumes the activation once the opponent has chosen.
     *
     * Legal targets are computed relative to [action].playerId (the ability's controller), so
     * hexproof/protection/shroud are measured against the controller — exactly the printed ruling
     * ("an opponent can't target a creature they control with hexproof"). The pause happens before
     * any cost is paid; cancellation simply pops the frame.
     */
    private fun pauseForOpponentChosenTargets(
        state: GameState,
        action: ActivateAbility,
        sourceName: String,
        fullTargetReqs: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
        opponentReqs: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>
    ): ExecutionResult {
        // The resumer interleaves the controller's and opponent's targets back into one list by
        // consuming exactly `count` targets per requirement (the positional model
        // EffectContext.buildNamedTargets uses on resolution). That holds only for fixed-count
        // requirements; an optional/variable/unlimited one would misalign the cursors. Cuombajj is
        // the only printed use and is fixed-count — reject the unsupported shape here, before
        // bothering an opponent with a decision, rather than after the pick on the resume path.
        if (fullTargetReqs.any { it.minCount != it.count || it.optional || it.unlimited }) {
            return ExecutionResult.error(
                state,
                "Opponent-chosen targets are only supported with fixed-count requirements"
            )
        }

        // The controller chooses which opponent makes the selection (per the card's own ruling:
        // "You choose which opponent chooses the second target"). In a two-player game that's the
        // sole opponent; choosing among several opponents in multiplayer is a future extension —
        // default to the first opponent in turn order so the ability still functions.
        val deciderId = state.turnOrder.firstOrNull { it != action.playerId && state.hasEntity(it) }
            ?: return ExecutionResult.error(state, "No opponent available to choose a target")

        val finder = com.wingedsheep.engine.handlers.TargetFinder()
        val legalTargets = mutableMapOf<Int, List<EntityId>>()
        val requirementInfos = opponentReqs.mapIndexed { index, req ->
            val legal = finder.findLegalTargets(state, req, action.playerId, action.sourceId)
            if (legal.isEmpty() && req.effectiveMinCount > 0) {
                // A required target with no legal choice means the ability can't be activated
                // (the enumerator gates on this; guard the engine-direct path too).
                return ExecutionResult.error(state, "No legal target for opponent's choice")
            }
            legalTargets[index] = legal
            com.wingedsheep.engine.core.TargetRequirementInfo(
                index = index,
                description = req.description,
                minTargets = req.effectiveMinCount,
                maxTargets = req.count
            )
        }

        val decisionId = java.util.UUID.randomUUID().toString()
        // The prompt is shown to the opponent who is making the choice, so the "of an opponent's
        // choice" suffix the requirement description carries is redundant noise here — strip it.
        val prompt = "Choose ${opponentReqs.joinToString(" and ") {
            it.description.removeSuffix(" of an opponent's choice")
        }} for $sourceName"
        val decision = com.wingedsheep.engine.core.ChooseTargetsDecision(
            id = decisionId,
            playerId = deciderId,
            prompt = prompt,
            context = com.wingedsheep.engine.core.DecisionContext(
                sourceId = action.sourceId,
                sourceName = sourceName,
                phase = com.wingedsheep.engine.core.DecisionPhase.CASTING
            ),
            targetRequirements = requirementInfos,
            legalTargets = legalTargets
        )
        val continuation = com.wingedsheep.engine.core.ActivateAbilityOpponentTargetContinuation(
            decisionId = decisionId,
            action = action,
            opponentRequirements = opponentReqs,
            fullRequirements = fullTargetReqs,
            deciderId = deciderId
        )
        val pausedState = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)
        val event = com.wingedsheep.engine.core.DecisionRequestedEvent(
            decisionId = decisionId,
            playerId = deciderId,
            decisionType = "CHOOSE_TARGETS",
            prompt = prompt
        )
        return ExecutionResult.paused(pausedState, decision, listOf(event))
    }

    /**
     * Check if an ability cost can be paid, using ManaSolver for mana costs
     * to consider both floating mana and untapped mana sources.
     */
    private fun canPayAbilityCostWithSources(
        state: GameState,
        cost: AbilityCost,
        sourceId: com.wingedsheep.sdk.model.EntityId,
        playerId: com.wingedsheep.sdk.model.EntityId,
        abilityContext: SpellPaymentContext? = null,
    ): Boolean {
        val poolComponent = state.getEntity(playerId)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        val manaPool = ManaPool(
            white = poolComponent.white,
            blue = poolComponent.blue,
            black = poolComponent.black,
            red = poolComponent.red,
            green = poolComponent.green,
            colorless = poolComponent.colorless,
            restrictedMana = poolComponent.restrictedMana,
        )
        return when (cost) {
            is AbilityCost.Atom -> {
                val mana = cost.manaCostOrNull
                if (mana != null) manaSolver.canPay(state, playerId, mana, spellContext = abilityContext)
                else costHandler.canPayAbilityCost(state, cost, sourceId, playerId, manaPool, abilityContext)
            }
            is AbilityCost.Composite -> {
                // If composite cost includes Tap, the source itself can't also be used as a mana source
                val excludeSources = if (hasTapCost(cost)) setOf(sourceId) else emptySet()
                cost.costs.all { subCost ->
                    val subMana = subCost.manaCostOrNull
                    if (subMana != null) manaSolver.canPay(state, playerId, subMana, excludeSources = excludeSources, spellContext = abilityContext)
                    else costHandler.canPayAbilityCost(state, subCost, sourceId, playerId, manaPool, abilityContext)
                }
            }
            else -> costHandler.canPayAbilityCost(state, cost, sourceId, playerId, manaPool, abilityContext)
        }
    }

    /**
     * Whether the given ability cost includes a Tap sub-cost.
     * The source of a Tap-cost ability cannot also serve as a mana source during payment.
     */
    private fun hasTapCost(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Tap -> true
        is AbilityCost.Composite -> cost.costs.any { it is AbilityCost.Tap }
        else -> false
    }

    /**
     * Whether [cost] removes the source from its current zone — a self-exile or self-sacrifice.
     * Used to decide whether to snapshot the source's counters before payment so the resolving
     * effect can read the pre-cost count (DynamicAmount.LastKnownSourceCounters).
     */
    private fun costExilesOrSacrificesSelf(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.ExileSelf, is AbilityCost.SacrificeSelf -> true
        is AbilityCost.Composite -> cost.costs.any { costExilesOrSacrificesSelf(it) }
        else -> false
    }

    /**
     * Apply [ActivatedAbility.genericCostReduction] to the mana portion of [cost].
     * The reduction is evaluated against the activating entity (e.g., the equipped creature
     * for The Dominion Bracelet, whose granted ability reduces by the creature's power) and,
     * when present, the chosen [targets] — so reductions that read the target the player picked
     * (e.g. Dragonfire Blade's "costs {1} less to activate for each color of the creature it
     * targets") resolve against that target. Per Scryfall ruling, this is locked in before costs
     * are paid.
     */
    private fun applyGenericCostReduction(
        cost: AbilityCost,
        ability: ActivatedAbility,
        state: GameState,
        sourceId: EntityId,
        controllerId: EntityId,
        targets: List<ChosenTarget>
    ): AbilityCost {
        val reduction = ability.genericCostReduction ?: return cost
        val reductionContext = EffectContext(
            sourceId = sourceId,
            controllerId = controllerId,
            targets = targets
        )
        val amount = DynamicAmountEvaluator().evaluate(state, reduction, reductionContext)
        if (amount <= 0) return cost
        return reduceGenericInCost(cost, amount)
    }

    private fun reduceGenericInCost(cost: AbilityCost, amount: Int): AbilityCost = when (cost) {
        is AbilityCost.Atom -> cost.manaCostOrNull
            ?.let { AbilityCost.Atom(CostAtom.Mana(it.reduceGeneric(amount))) } ?: cost
        is AbilityCost.Composite -> {
            var applied = false
            AbilityCost.Composite(cost.costs.map { sub ->
                val subMana = sub.manaCostOrNull
                if (!applied && subMana != null) {
                    applied = true
                    AbilityCost.Atom(CostAtom.Mana(subMana.reduceGeneric(amount)))
                } else sub
            })
        }
        else -> cost
    }

    /**
     * Extract the ManaCost from an ability cost, if present.
     */
    private fun extractManaCost(cost: AbilityCost): ManaCost? = when (cost) {
        is AbilityCost.Atom -> cost.manaCostOrNull
        is AbilityCost.Composite -> cost.costs.firstNotNullOfOrNull { it.manaCostOrNull }
        else -> null
    }

    private data class AutoTapResult(
        val newState: GameState,
        val newPool: ManaPool,
        val events: List<GameEvent>
    )

    /**
     * Auto-tap mana sources to cover a mana cost that can't be fully paid from the floating pool.
     * Taps sources for the shortfall and adds their mana to the pool so costHandler can consume it.
     * Returns null if the cost cannot be paid.
     */
    private fun autoTapForManaCost(
        state: GameState,
        playerId: com.wingedsheep.sdk.model.EntityId,
        pool: ManaPool,
        cost: ManaCost,
        sourceName: String,
        xValue: Int = 0,
        excludeSources: Set<com.wingedsheep.sdk.model.EntityId> = emptySet(),
        abilityContext: SpellPaymentContext? = null,
        xManaRestriction: Set<Color> = emptySet(),
    ): AutoTapResult? {
        // Determine what the floating pool can cover (with the ability context so restricted
        // mana eligible for this activation counts toward coverage)
        val partialResult = pool.payPartial(cost, abilityContext)
        val remainingCost = partialResult.remainingCost

        // The floating pool also pays toward the {X} portion before any sources are tapped —
        // sharing the same coverage rule as CastPaymentProcessor.autoPay (ManaPool.xCoveragePlan).
        // Without this, an {X} ability whose X is solved purely by tapping sources reports "Not
        // enough mana" even when the pool already holds enough (e.g. Aladdin's Lamp activated with
        // X=4 while 4 mana float in the pool). We only reduce how much X the solver must tap for
        // here; the actual pool spend for X happens later in `payAbilityCost`.
        val xSymbolCount = cost.xCount.coerceAtLeast(1)
        var xToTap = xValue * xSymbolCount
        if (xToTap > 0) {
            xToTap -= partialResult.newPool.xCoveragePlan(xToTap, xManaRestriction).size
        }

        // If floating pool covers everything (and no X left to tap for), no tapping needed.
        // Return the original pool unchanged — `payAbilityCost` performs the actual deduction.
        if (remainingCost.isEmpty() && xToTap == 0) {
            return AutoTapResult(state, pool, emptyList())
        }

        // Tap sources for the remaining cost (xToTap is the X mana the floating pool couldn't
        // cover, treated as additional generic mana — or restricted to xManaRestriction colors
        // for "spend only [colors] on X" abilities)
        val solution = manaSolver.solve(state, playerId, remainingCost, xToTap, excludeSources = excludeSources, spellContext = abilityContext, xManaRestriction = xManaRestriction)
            ?: return null

        var currentState = state
        var currentPool = pool
        val events = mutableListOf<GameEvent>()

        for (source in solution.sources) {
            val (tappedState, tapEvent) = tap(currentState, source.entityId)
            currentState = tappedState
            tapEvent?.let(events::add)
        }

        // Add produced mana to floating pool so costHandler.payAbilityCost can consume it.
        // When the source's ability is restricted (e.g. Steelswarm Operator's
        // {T}: Add {U}{U} restricted to artifact-source ability activations), tag the
        // produced mana with that restriction. payAbilityCost will preferentially spend
        // the eligible restricted mana for the cost — and any unconsumed remainder stays
        // restricted in the pool instead of laundering into unrestricted mana.
        for (source in solution.sources) {
            // A tapped source may legitimately have no manaProduced entry: ManaSolver taps
            // extra sources to pay the *internal* activation cost of a mana ability (e.g. the
            // {1} in Hidden Grotto's "{1}, {T}: Add one mana of any color"). That mana is
            // consumed by the ability's own cost rather than flowing into the spell/ability
            // payment pool, so the solver intentionally omits it from manaProduced. Such a
            // source is still tapped above; it just contributes nothing to the pool here.
            val production = solution.manaProduced[source.entityId] ?: continue
            val color = production.color
            val restriction = if (color != null) {
                source.colorRestrictions[color] ?: source.restriction
            } else source.restriction
            currentPool = when {
                color != null && restriction != null ->
                    currentPool.addRestricted(color, production.amount, restriction)
                color != null ->
                    currentPool.add(color, production.amount)
                else ->
                    currentPool.addColorless(production.colorless)
            }
        }

        // Add per-source bonus mana from AdditionalManaOnSourceTap auras/statics (e.g.,
        // Lavaleaper: tapping a basic land adds an extra mana of its produced color).
        // Unlike the cast flow — which uses solve's internal accounting as the payment —
        // the activate flow funnels all produced mana through the pool and then deducts
        // the cost via payAbilityCost, so the *total* bonus from tapping must land in the
        // pool. solution.remainingBonusMana would drop any bonus consumed during solve.
        // (Multi-mana excess is already included via manaProduced.amount above.)
        // Aura bonus mana is unrestricted — the source's restriction belongs to the
        // printed ability, not to the aura-granted extras.
        for (source in solution.sources) {
            if (source.bonusManaPerTap > 0 && source.bonusManaColor != null) {
                currentPool = currentPool.add(source.bonusManaColor, source.bonusManaPerTap)
            }
        }

        // Update state with enriched pool — carry restrictedMana through so the
        // ability-payment context can spend (and the leftover can stay) restricted.
        currentState = currentState.updateEntity(playerId) { c ->
            c.with(ManaPoolComponent(
                white = currentPool.white,
                blue = currentPool.blue,
                black = currentPool.black,
                red = currentPool.red,
                green = currentPool.green,
                colorless = currentPool.colorless,
                restrictedMana = currentPool.restrictedMana,
            ))
        }

        return AutoTapResult(currentState, currentPool, events)
    }

    /**
     * Strip the Mana portion from an ability cost — used when Explicit payment already
     * tapped the required sources, so the mana pool deduction should be skipped.
     */
    private fun stripManaCost(cost: AbilityCost): AbilityCost = when (cost) {
        is AbilityCost.Atom -> if (cost.manaCostOrNull != null) AbilityCost.Free else cost
        is AbilityCost.Composite -> {
            val nonManaCosts = cost.costs.filter { it.manaCostOrNull == null }
            when (nonManaCosts.size) {
                0 -> AbilityCost.Free
                1 -> nonManaCosts.single()
                else -> AbilityCost.Composite(nonManaCosts)
            }
        }
        else -> cost
    }

    private fun checkActivationRestriction(
        state: GameState,
        playerId: com.wingedsheep.sdk.model.EntityId,
        sourceId: com.wingedsheep.sdk.model.EntityId,
        abilityId: com.wingedsheep.sdk.scripting.AbilityId,
        restriction: ActivationRestriction
    ): String? {
        return when (restriction) {
            is ActivationRestriction.AnyPlayerMay -> null // Not a restriction; handled in validate()
            is ActivationRestriction.OnlyDuringYourTurn -> {
                // CR 805.5a — "your turn" is the active team's turn in Two-Headed Giant.
                if (!state.isActiveTurnFor(playerId)) "This ability can only be activated during your turn"
                else null
            }
            is ActivationRestriction.BeforeStep -> {
                if (state.step.ordinal >= restriction.step.ordinal)
                    "This ability can only be activated before ${restriction.step.displayName}"
                else null
            }
            is ActivationRestriction.DuringPhase -> {
                if (state.phase != restriction.phase)
                    "This ability can only be activated during ${restriction.phase.displayName}"
                else null
            }
            is ActivationRestriction.DuringStep -> {
                if (state.step != restriction.step)
                    "This ability can only be activated during ${restriction.step.displayName}"
                else null
            }
            is ActivationRestriction.OnlyIfCondition -> {
                val context = EffectContext(
                    sourceId = sourceId,
                    controllerId = playerId,
                    targets = emptyList(),
                    xValue = 0
                )
                if (!conditionEvaluator.evaluate(state, restriction.condition, context))
                    "Activation condition not met"
                else null
            }
            is ActivationRestriction.OncePerTurn -> {
                val tracker = state.getEntity(sourceId)?.get<AbilityActivatedThisTurnComponent>()
                if (tracker != null && tracker.hasActivated(abilityId)) {
                    "This ability can only be activated once each turn"
                } else null
            }
            is ActivationRestriction.MaxPerTurn -> {
                val tracker = state.getEntity(sourceId)?.get<AbilityActivatedThisTurnComponent>()
                if ((tracker?.activationCount(abilityId) ?: 0) >= restriction.count) {
                    "This ability can't be activated more than ${restriction.count} times each turn"
                } else null
            }
            is ActivationRestriction.Once -> {
                val tracker = state.getEntity(sourceId)?.get<AbilityActivatedEverComponent>()
                if (tracker != null && tracker.hasActivated(abilityId)) {
                    "This ability can only be activated once"
                } else null
            }
            is ActivationRestriction.ControlledSinceYourMostRecentTurn -> {
                if (state.getEntity(sourceId)
                        ?.has<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>() == true
                ) "You must have controlled this permanent continuously since your most recent turn began"
                else null
            }
            is ActivationRestriction.All -> {
                restriction.restrictions.firstNotNullOfOrNull {
                    checkActivationRestriction(state, playerId, sourceId, abilityId, it)
                }
            }
        }
    }

    /**
     * After a mana ability resolves on a permanent, check for auras attached to it
     * that have AdditionalManaOnTap (e.g., Elvish Guidance). These are triggered mana
     * abilities that resolve immediately without using the stack.
     */
    private data class AdditionalManaResult(
        val state: GameState,
        val events: List<GameEvent>
    )

    private val dynamicAmountEvaluator = DynamicAmountEvaluator()
    private val predicateEvaluator = PredicateEvaluator()
    private val tappedForManaBonusResolver =
        com.wingedsheep.engine.handlers.effects.mana.TappedForManaBonusResolver(cardRegistry, dynamicAmountEvaluator)

    /**
     * Count how many [AdditionalSourceTriggers] doublers on the battlefield apply to a
     * triggered ability with source [triggerSourceId] controlled by [triggerControllerId].
     *
     * Triggered mana abilities ([AdditionalManaOnTap], [AdditionalManaOnSourceTap]) bypass
     * the stack and are resolved synchronously, so they never flow through the normal
     * `TriggerDetector` doubling pass. This helper lets the inline mana resolution paths
     * apply the same doubling logic as the trigger pipeline.
     *
     * Returns N — N additional firings on top of the natural one (so total firings = N + 1).
     */
    private fun countAdditionalSourceTriggerDoublers(
        state: GameState,
        triggerSourceId: EntityId,
        triggerControllerId: EntityId
    ): Int {
        val projected = state.projectedState
        var count = 0
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue
            val controllerId = projected.getController(permanentId) ?: continue
            if (controllerId != triggerControllerId) continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability !is AdditionalSourceTriggers) continue
                if (ability.excludeSelf && permanentId == triggerSourceId) continue
                if (!predicateEvaluator.matches(
                        state, projected, triggerSourceId, ability.sourceFilter,
                        PredicateContext(controllerId = controllerId, sourceId = permanentId)
                    )
                ) continue
                count++
            }
        }
        return count
    }

    /**
     * Check if any permanent on the battlefield has DampLandManaProduction static ability.
     */
    private fun hasDampLandManaProduction(state: GameState): Boolean {
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                if (cardDef.script.staticAbilities.any { it is DampLandManaProduction }) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * If any aura attached to [sourceId] has an [OverrideEnchantedLandManaColor]
     * static ability, return the color the enchanted land's own mana abilities
     * should produce instead. `null` means no override (mana ability produces
     * normally). Multiple auras: last-wins (same aura only applies once).
     */
    private fun findEnchantedLandManaColorOverride(
        state: GameState,
        sourceId: com.wingedsheep.sdk.model.EntityId
    ): Color? {
        var override: Color? = null
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val attachedTo = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
            if (attachedTo?.targetId != sourceId) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (staticAbility in cardDef.script.staticAbilities) {
                val o = staticAbility as? com.wingedsheep.sdk.scripting.OverrideEnchantedLandManaColor ?: continue
                override = o.color
                    ?: container.chosenColor()
                    ?: continue
            }
        }
        return override
    }

    /**
     * True if the land [landId] is subject to a [ReplaceLandManaColor] static (Pulse of Llanowar) —
     * i.e. some permanent on the battlefield has that static and its filter matches the tapped land
     * from the static controller's projected perspective. When true, the land's produced mana is
     * replaced with one mana of a color of its controller's choice.
     */
    private fun landMatchesManaColorReplacement(
        state: GameState,
        landId: EntityId,
        @Suppress("UNUSED_PARAMETER") tappingPlayerId: EntityId
    ): Boolean {
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (staticAbility in cardDef.script.staticAbilities) {
                val replacement = staticAbility as? ReplaceLandManaColor ?: continue
                val staticController = state.projectedState.getController(entityId) ?: continue
                val filterContext = PredicateContext(controllerId = staticController, sourceId = entityId)
                if (predicateEvaluator.matches(state, state.projectedState, landId, replacement.filter, filterContext)) {
                    return true
                }
            }
        }
        return false
    }

    private fun resolveAdditionalManaOnTap(
        state: GameState,
        sourceId: com.wingedsheep.sdk.model.EntityId,
        controllerId: com.wingedsheep.sdk.model.EntityId,
        existingEvents: List<GameEvent>
    ): AdditionalManaResult {
        var currentState = state
        val events = existingEvents.toMutableList()

        // Find all auras attached to the source permanent
        for (entityId in currentState.getBattlefield()) {
            val container = currentState.getEntity(entityId) ?: continue
            val attachedTo = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
            if (attachedTo?.targetId != sourceId) continue

            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            // Check each static ability for AdditionalManaOnTap
            for (staticAbility in cardDef.script.staticAbilities) {
                val additionalMana = staticAbility as? AdditionalManaOnTap ?: continue

                // The controller of the enchanted land gets the mana
                val landController = currentState.getEntity(sourceId)
                    ?.get<ControllerComponent>()?.playerId ?: controllerId

                val context = EffectContext(
                    sourceId = entityId,
                    controllerId = landController,
                    targets = emptyList(),
                    xValue = null
                )

                val amount = dynamicAmountEvaluator.evaluate(currentState, additionalMana.amount, context)
                if (amount <= 0) continue

                // Resolve the color: if the ability specifies null, read the aura's chosen color.
                // If no color is chosen (e.g., somehow on battlefield without a choice), skip.
                val manaColor = additionalMana.color
                    ?: container.chosenColor()
                    ?: continue

                // Triggered mana ability — apply AdditionalSourceTriggers doublers
                // (e.g., Twinflame Travelers) so the bonus fires N+1 times.
                val auraController = container.get<ControllerComponent>()?.playerId ?: landController
                val extraFirings = countAdditionalSourceTriggerDoublers(currentState, entityId, auraController)
                val firings = 1 + extraFirings
                repeat(firings) {
                    currentState = currentState.updateEntity(landController) { c ->
                        val pool = c.get<ManaPoolComponent>() ?: ManaPoolComponent()
                        c.with(pool.add(manaColor, amount))
                    }

                    events.add(ManaAddedEvent(
                        playerId = landController,
                        sourceId = entityId,
                        sourceName = card.name,
                        white = if (manaColor == Color.WHITE) amount else 0,
                        blue = if (manaColor == Color.BLUE) amount else 0,
                        black = if (manaColor == Color.BLACK) amount else 0,
                        red = if (manaColor == Color.RED) amount else 0,
                        green = if (manaColor == Color.GREEN) amount else 0,
                        colorless = 0
                    ))
                }
            }
        }

        return AdditionalManaResult(currentState, events)
    }

    /**
     * After a permanent's mana ability resolves, check for [AdditionalManaOnSourceTap]
     * statics anywhere on the battlefield whose `sourceFilter` matches the tapped source.
     * Each match adds bonus mana to the tapping player's pool.
     *
     * Filter matching uses projected state so animated creature-lands and typeshifted
     * lands count under their projected types (Rule 613.1). The static-ability source's
     * controller is read from projected state so control-changing effects (Annex,
     * Ray of Command) correctly transfer the "you tap" condition along with the permanent.
     *
     * Triggered mana ability — resolves immediately without using the stack (Rule 605).
     */
    private fun resolveAdditionalManaOnSourceTap(
        state: GameState,
        sourceId: EntityId,
        tappingPlayerId: EntityId,
        manaEvent: ManaAddedEvent?,
        existingEvents: List<GameEvent>
    ): AdditionalManaResult {
        state.getEntity(sourceId) ?: return AdditionalManaResult(state, existingEvents)

        // The mirror-color form (color = null) needs the actual produced color from manaEvent.
        // The fixed-color form does not.
        val producedColor: Color? = manaEvent?.let {
            when {
                it.white > 0 -> Color.WHITE
                it.blue > 0 -> Color.BLUE
                it.black > 0 -> Color.BLACK
                it.red > 0 -> Color.RED
                it.green > 0 -> Color.GREEN
                else -> null
            }
        }
        val producedColorless = manaEvent != null && producedColor == null && manaEvent.colorless > 0

        var currentState = state
        val events = existingEvents.toMutableList()

        for (entityId in currentState.getBattlefield()) {
            val container = currentState.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue

            for (staticAbility in cardDef.script.staticAbilities) {
                val onSourceTap = staticAbility as? AdditionalManaOnSourceTap ?: continue

                val staticController = currentState.projectedState.getController(entityId) ?: continue

                // Filter is evaluated from the static-ability controller's perspective so
                // `youControl` on the source filter means "controlled by you, the static
                // controller" — see AdditionalManaOnSourceTap kdoc.
                val filterContext = PredicateContext(controllerId = staticController, sourceId = entityId)
                if (!predicateEvaluator.matches(
                        currentState, currentState.projectedState, sourceId, onSourceTap.sourceFilter, filterContext
                    )) continue

                val effectContext = EffectContext(
                    sourceId = entityId,
                    controllerId = tappingPlayerId,
                    targets = emptyList(),
                    xValue = null
                )
                val bonusAmount = dynamicAmountEvaluator.evaluate(currentState, onSourceTap.amount, effectContext)
                if (bonusAmount <= 0) continue

                // Resolve the bonus color: explicit color wins; null means mirror the produced color.
                val bonusColor: Color? = onSourceTap.color ?: producedColor
                val bonusColorless = onSourceTap.color == null && bonusColor == null && producedColorless
                if (bonusColor == null && !bonusColorless) continue

                // Triggered mana abilities bypass the stack but are still triggered
                // abilities — so AdditionalSourceTriggers (Twinflame Travelers) doubles
                // them just like any other trigger. firings = 1 (natural) + N (doublers).
                val extraFirings = countAdditionalSourceTriggerDoublers(currentState, entityId, staticController)
                val firings = 1 + extraFirings
                repeat(firings) {
                    currentState = currentState.updateEntity(tappingPlayerId) { c ->
                        val pool = c.get<ManaPoolComponent>() ?: ManaPoolComponent()
                        val newPool = if (bonusColor != null) pool.add(bonusColor, bonusAmount)
                                      else pool.addColorless(bonusAmount)
                        c.with(newPool)
                    }

                    events.add(ManaAddedEvent(
                        playerId = tappingPlayerId,
                        sourceId = entityId,
                        sourceName = card.name,
                        white = if (bonusColor == Color.WHITE) bonusAmount else 0,
                        blue = if (bonusColor == Color.BLUE) bonusAmount else 0,
                        black = if (bonusColor == Color.BLACK) bonusAmount else 0,
                        red = if (bonusColor == Color.RED) bonusAmount else 0,
                        green = if (bonusColor == Color.GREEN) bonusAmount else 0,
                        colorless = if (bonusColorless) bonusAmount else 0
                    ))

                    // Inline non-mana rider (Overabundance: "deals 1 damage to the player").
                    // Resolved with controllerId = the tapping player, sourceId = this static's
                    // source, so EffectTarget.Controller is the tapper and EffectTarget.Self is the
                    // enchantment. Riders here must not require player input (no stack).
                    val rider = onSourceTap.rider
                    if (rider != null) {
                        val riderResult = effectExecutorRegistry.execute(currentState, rider, effectContext)
                        currentState = riderResult.state
                        events.addAll(riderResult.events)
                    }
                }
            }
        }

        return AdditionalManaResult(currentState, events)
    }

    /**
     * Returns the maximum number of loyalty ability activations per planeswalker per turn
     * for the given player. Normally 1, but ExtraLoyaltyActivation (Oath of Teferi) raises it to 2.
     * Multiple copies do NOT stack beyond 2.
     */
    private fun getMaxLoyaltyActivations(state: GameState, playerId: EntityId): Int {
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val controller = container.get<ControllerComponent>()?.playerId ?: continue
            if (controller != playerId) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is ExtraLoyaltyActivation }) {
                return 2
            }
        }
        return 1
    }

    /**
     * Resolve an intrinsic mana ability granted by a basic-land subtype (CR 305.7).
     * Returns the synthesized ability only if the entity currently projects the
     * matching basic-land subtype, so an `intrinsic_mana_R` request on a land that
     * isn't a Mountain in the projected state is rejected.
     */
    private fun resolveIntrinsicManaAbility(
        state: GameState,
        sourceId: EntityId,
        abilityId: AbilityId,
    ): ActivatedAbility? {
        val ability = IntrinsicManaAbilities.lookup(abilityId) ?: return null
        val color = (ability.effect as? AddManaEffect)?.color ?: return null
        val expectedSubtype = when (color) {
            Color.WHITE -> "Plains"
            Color.BLUE -> "Island"
            Color.BLACK -> "Swamp"
            Color.RED -> "Mountain"
            Color.GREEN -> "Forest"
        }
        val subtypes = state.projectedState.getSubtypes(sourceId)
        if (expectedSubtype !in subtypes) return null
        return ability
    }

    /**
     * Find a class level-up ability by its deterministic ID.
     * Returns the generated ActivatedAbility if the ID matches a valid level-up,
     * or null if this isn't a class level-up ability.
     */
    private fun findClassLevelUpAbility(
        cardDef: com.wingedsheep.sdk.model.CardDefinition,
        container: com.wingedsheep.engine.state.ComponentContainer,
        abilityId: com.wingedsheep.sdk.scripting.AbilityId
    ): ActivatedAbility? {
        if (!abilityId.value.startsWith("class_level_up_")) return null
        val classLevelComponent = container.get<ClassLevelComponent>() ?: return null
        val targetLevel = abilityId.value.removePrefix("class_level_up_").toIntOrNull() ?: return null
        if (targetLevel != classLevelComponent.currentLevel + 1) return null
        val levelAbility = cardDef.classLevels.find { it.level == targetLevel } ?: return null
        return ActivatedAbility(
            id = AbilityId.classLevelUp(targetLevel),
            cost = AbilityCost.Atom(CostAtom.Mana(levelAbility.cost)),
            effect = LevelUpClassEffect(targetLevel),
            timing = TimingRule.SorcerySpeed,
            descriptionOverride = "Level up to level $targetLevel"
        )
    }

    /**
     * Get activated abilities granted to an entity by static abilities on battlefield permanents,
     * paired with the EntityId of the permanent that granted each ability.
     * E.g., Spectral Sliver grants a pump ability to all Sliver creatures via
     * GrantActivatedAbility. The Dominion Bracelet grants its activated
     * ability to the equipped creature via GrantActivatedAbility; the
     * granter ID is needed to resolve AbilityCost.ExileGrantingPermanent.
     */
    private fun getStaticGrantedAbilitiesWithGranter(
        entityId: EntityId,
        state: GameState
    ): List<Pair<ActivatedAbility, EntityId>> {
        if (state.getEntity(entityId) == null) return emptyList()

        val result = mutableListOf<Pair<ActivatedAbility, EntityId>>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>()) continue

            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                // "[filter] have all activated abilities of the [creature] cards exiled with this"
                // (Territory Forge = Self; Agatha's Soul Cauldron = creatures you control with a
                // +1/+1 counter). Mirror CastPermissionUtils.getStaticGrantedAbilitiesWithGranter:
                // grant each pile ability to every matching permanent, recording the *receiver* as
                // the granter so `{T}`/self-references bind to the permanent that gained the ability.
                if (ability is com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfLinkedExiledCard) {
                    val receives = when (val scope = ability.filter.scope) {
                        is Scope.Self -> permanentId == entityId
                        is Scope.Specific -> scope.entityId == entityId
                        is Scope.AttachedTo -> container.get<AttachedToComponent>()?.targetId == entityId
                        is Scope.Battlefield -> {
                            if (ability.filter.excludeSelf && permanentId == entityId) false
                            else {
                                val granterController = state.projectedState.getController(permanentId)
                                granterController != null && predicateEvaluator.matches(
                                    state, state.projectedState, entityId, ability.filter.baseFilter,
                                    PredicateContext(controllerId = granterController, sourceId = permanentId)
                                )
                            }
                        }
                    }
                    if (receives) {
                        for (granted in com.wingedsheep.engine.legalactions.utils.linkedExiledActivatedAbilities(state, permanentId, cardRegistry, ability.creatureCardsOnly)) {
                            result.add(granted to entityId)
                        }
                    }
                    continue
                }
                if (ability !is GrantActivatedAbility) continue
                when (ability.filter.scope) {
                    is Scope.Battlefield -> {
                        if (ability.filter.excludeSelf && permanentId == entityId) continue
                        val granterController = state.projectedState.getController(permanentId) ?: continue
                        val matches = predicateEvaluator.matches(
                            state,
                            state.projectedState,
                            entityId,
                            ability.filter.baseFilter,
                            PredicateContext(controllerId = granterController, sourceId = permanentId)
                        )
                        if (matches) {
                            result.add(ability.ability to permanentId)
                        }
                    }
                    is Scope.AttachedTo -> {
                        val attachedTo = container.get<AttachedToComponent>()
                        if (attachedTo != null && attachedTo.targetId == entityId) {
                            result.add(ability.ability to permanentId)
                        }
                    }
                    is Scope.Self -> {
                        if (permanentId == entityId) result.add(ability.ability to permanentId)
                    }
                    is Scope.Specific -> {
                        if ((ability.filter.scope as Scope.Specific).entityId == entityId) {
                            result.add(ability.ability to permanentId)
                        }
                    }
                }
            }
        }

        // GainActivatedAbilitiesOfPermanents (Sharkey): copies of opponents' lands' abilities, etc.
        // Resolved by the shared helper so the enumerator and this handler agree on the gained set.
        castPermissionUtils.getGainedAbilitiesOfPermanents(entityId, state)
            .forEach { result.add(it.ability to it.granterId) }

        return result
    }

    private fun getStaticGrantedActivatedAbilities(
        entityId: EntityId,
        state: GameState
    ): List<ActivatedAbility> = getStaticGrantedAbilitiesWithGranter(entityId, state).map { it.first }

    companion object {
        fun create(services: EngineServices): ActivateAbilityHandler {
            return ActivateAbilityHandler(
                services.cardRegistry,
                services.turnManager,
                services.costHandler,
                services.manaSolver,
                services.alternativePaymentHandler,
                services.effectExecutorRegistry,
                services.stackResolver,
                services.targetValidator,
                services.conditionEvaluator,
                services.triggerDetector,
                services.triggerProcessor,
                services.castPermissionUtils
            )
        }
    }

    /**
     * Build a [ManaAddedEvent] by diffing the controller's mana pool before and after
     * the effect executed. Used for [AddManaOfChoiceEffect]: the executor already
     * resolved the color set, picked the color, and added the mana — we just need to
     * report what changed for client display.
     */
    private fun manaAddedEventFromPoolDelta(
        oldState: GameState,
        newState: GameState,
        action: ActivateAbility,
        cardComponent: CardComponent,
    ): ManaAddedEvent? {
        val oldPool = oldState.getEntity(action.playerId)
            ?.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
        val newPool = newState.getEntity(action.playerId)
            ?.get<com.wingedsheep.engine.state.components.player.ManaPoolComponent>()
            ?: return null
        return ManaAddedEvent(
            playerId = action.playerId,
            sourceId = action.sourceId,
            sourceName = cardComponent.name,
            white = newPool.white - (oldPool?.white ?: 0),
            blue = newPool.blue - (oldPool?.blue ?: 0),
            black = newPool.black - (oldPool?.black ?: 0),
            red = newPool.red - (oldPool?.red ?: 0),
            green = newPool.green - (oldPool?.green ?: 0),
            colorless = newPool.colorless - (oldPool?.colorless ?: 0),
        ).takeIf { it.white + it.blue + it.black + it.red + it.green + it.colorless > 0 }
    }

    /**
     * Pull the [AbilityCost.TapXPermanents] sub-cost out of an ability cost (top-level or
     * inside a [AbilityCost.Composite]), or null if none. Used by the legal-actions submission
     * path to detect that an activation needs to pause for an X choice + tap-target selection.
     */
    private fun extractTapXPermanentsCost(cost: AbilityCost): AbilityCost.TapXPermanents? = when (cost) {
        is AbilityCost.TapXPermanents -> cost
        is AbilityCost.Composite -> cost.costs.filterIsInstance<AbilityCost.TapXPermanents>().firstOrNull()
        else -> null
    }

    /**
     * Pull the graveyard-exile [CostAtom.ExileFrom] sub-cost out of an ability cost (top-level or
     * inside a [AbilityCost.Composite]), or null if none. Used by the legal-actions submission
     * path to detect that an activation needs to pause for a card-selection decision when the
     * player has more matching graveyard cards than the cost requires.
     */
    private fun extractExileFromGraveyardCost(cost: AbilityCost): CostAtom.ExileFrom? = when (cost) {
        is AbilityCost.Atom -> (cost.atom as? CostAtom.ExileFrom)?.takeIf { it.zone == Zone.GRAVEYARD }
        is AbilityCost.Composite -> cost.costs.firstNotNullOfOrNull {
            ((it as? AbilityCost.Atom)?.atom as? CostAtom.ExileFrom)?.takeIf { ex -> ex.zone == Zone.GRAVEYARD }
        }
        else -> null
    }

    /**
     * Pull the [CostAtom.Sacrifice] sub-cost out of an ability cost (top-level [AbilityCost.Atom] or
     * inside a [AbilityCost.Composite]), or null if none. Used by the legal-actions submission path
     * to detect that an activation needs to pause for a sacrifice-target selection when the player
     * controls more matching permanents than the cost requires (Sage of Lat-Nam, Atog, …).
     */
    private fun extractSacrificeCost(cost: AbilityCost): CostAtom.Sacrifice? = when (cost) {
        is AbilityCost.Atom -> cost.atom as? CostAtom.Sacrifice
        is AbilityCost.Composite -> cost.costs.firstNotNullOfOrNull {
            (it as? AbilityCost.Atom)?.atom as? CostAtom.Sacrifice
        }
        else -> null
    }
}
