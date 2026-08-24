package com.wingedsheep.engine.handlers.actions.ability
import com.wingedsheep.engine.handlers.TargetingSourceType
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
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.handlers.effects.bend.BendEvents
import com.wingedsheep.engine.mechanics.SummoningSicknessRules
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.IntrinsicManaAbilities
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow
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
import com.wingedsheep.sdk.core.BendType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PermanentCostAction
import com.wingedsheep.sdk.scripting.costs.VariableCostMeasure
import com.wingedsheep.sdk.scripting.costs.manaCostOrNull
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.LevelUpClassEffect
import com.wingedsheep.sdk.scripting.effects.AddAnyColorManaSpendOnChosenTypeEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.AddColorlessManaEffect
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.MultiplyManaOnSourceTap
import com.wingedsheep.sdk.scripting.ReplaceLandManaColor
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ManaColorSet
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
    private val manaAbilitySideEffectExecutor:
        com.wingedsheep.engine.mechanics.mana.ManaAbilitySideEffectExecutor,
) : ActionHandler<ActivateAbility> {
    override val actionType: KClass<ActivateAbility> = ActivateAbility::class

    /** The first [CostAtom.TapPermanents] atom anywhere in this cost, or null if it has none. */
    private fun AbilityCost.firstTapPermanentsAtomOrNull(): CostAtom.TapPermanents? = when (this) {
        is AbilityCost.Atom -> atom as? CostAtom.TapPermanents
        is AbilityCost.Composite -> costs.firstNotNullOfOrNull { it.firstTapPermanentsAtomOrNull() }
        else -> null
    }

    /**
     * The first [CostAtom.ExileFromGraveyardForTotal] atom anywhere in this cost, or null.
     * A cost carries at most one — its selection is what `CardSource.ExiledAsCost` reads back.
     *
     * Callers additionally assume it is the cost's **only** exile atom: `exileChoices` is a single
     * flat channel shared by every exile atom in a composite cost, so a cost pairing this atom with
     * an `ExileFrom` would need the two selections split per atom before either could be trusted.
     * No printed card has that shape; a card that does must fix the channel, not the call site.
     */
    private fun AbilityCost.firstExileForTotalAtomOrNull(): CostAtom.ExileFromGraveyardForTotal? =
        when (this) {
            is AbilityCost.Atom -> atom as? CostAtom.ExileFromGraveyardForTotal
            is AbilityCost.Composite -> costs.firstNotNullOfOrNull { it.firstExileForTotalAtomOrNull() }
            else -> null
        }

    /**
     * Whether this cost exiles anything at all — either the sum-gated
     * [CostAtom.ExileFromGraveyardForTotal] or a plain counted [CostAtom.ExileFrom].
     *
     * Both feed the same flat `exileChoices` channel, and both produce cards the resolving effect
     * may need to name via `CardSource.ExiledAsCost` — Necropolis reads the mana value of the very
     * creature card its "Exile a creature card from your graveyard:" cost just exiled. Gating the
     * record on the *sum-gated* atom alone left the plain counted form with an empty list, so an
     * effect reading it back saw nothing.
     */
    private fun AbilityCost.hasExileAtom(): Boolean = when (this) {
        is AbilityCost.Atom -> atom is CostAtom.ExileFrom || atom is CostAtom.ExileFromGraveyardForTotal
        is AbilityCost.Composite -> costs.any { it.hasExileAtom() }
        else -> false
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
        // CR 605.3a — a mana ability may also be activated "whenever a rule or effect asks for a
        // mana payment". While such a window is open the paying player holds no priority, so defer
        // the priority verdict until the ability is known to be a mana ability (checked below).
        val manaPaymentWindow = ManaPaymentWindow.openFor(state, action.playerId)
        if (state.priorityPlayerId != action.playerId && manaPaymentWindow == null) {
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

        // The mana-payment window (CR 605.3a) opens the door for mana abilities only — everything
        // else still needs priority.
        if (manaPaymentWindow != null && state.priorityPlayerId != action.playerId && !ability.isManaAbility) {
            return "Only mana abilities can be activated while paying a cost"
        }

        // "During that turn, power-up abilities can't be activated" (Kang the Conqueror). Global and
        // zone-independent, so it is checked before the battlefield/other-zone split below.
        if (castPermissionUtils.isPowerUpActivationRestricted(state, ability)) {
            return "Power-up abilities can't be activated this turn"
        }

        // Check that the card is in the correct zone for this ability
        if (ability.activateFromZone != Zone.BATTLEFIELD) {
            val ownerId = container.get<OwnerComponent>()?.playerId ?: return "Card has no owner"
            val inZone = state.getZone(ownerId, ability.activateFromZone).contains(action.sourceId)
            if (!inZone) return "This ability can only be activated from the ${ability.activateFromZone.name.lowercase()}"
            if (ownerId != action.playerId) return "You don't own this card"
        } else {
            // Check if any player may activate this ability (e.g., Lethal Vapors). Recursive
            // through `All`, because the permission is routinely *narrowed* by a companion
            // restriction rather than standing alone — Merseine's "only the controller of the
            // enchanted creature may activate this ability" is AnyPlayerMay + a condition.
            val anyPlayerMay = ability.restrictions.any { anyPlayerMayIn(it) }

            if (!anyPlayerMay) {
                // Use projected controller to account for control-changing effects (e.g., Annex)
                val projected = state.projectedState
                val controller = projected.getController(action.sourceId)
                    ?: container.get<ControllerComponent>()?.playerId
                if (controller != action.playerId) {
                    return "You don't control this permanent"
                }
            }

            // A face-down permanent has no characteristics beyond those the rules that made it face
            // down list (CR 708.2), so none of its *card's* abilities are activatable. Abilities
            // another effect grants it are a different thing entirely — they apply in Layer 6 to the
            // object on the battlefield, not to the hidden card — so they stay activatable
            // (Etrata, Deadly Fugitive: "Face-down creatures you control have '{2}{U}{B}: Turn this
            // creature face up …'"). Same own-vs-granted split as the lost-all-abilities check below.
            if (container.has<FaceDownComponent>()) {
                val isOwnAbility =
                    cardDef?.script?.effectiveActivatedAbilities(classLevel)?.any { it.id == action.abilityId } == true ||
                        action.abilityId.value.startsWith("class_level_up_") ||
                        IntrinsicManaAbilities.lookup(action.abilityId) != null
                if (isOwnAbility) {
                    return "Face-down creatures have no abilities"
                }
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
        // Resolve a *defined* {X} (CR 107.3c) before anything else reads the cost, so validation
        // sees the same fixed cost enumeration offered and payment will charge. Then apply
        // ability-specific generic cost reduction (e.g., The Dominion Bracelet's
        // "{X} less, where X is this creature's power"). Per Scryfall ruling, the reduced
        // cost is locked in here, before costs are paid. Then apply generic equip-cost reduction
        // (Éowyn) and finally Forge Anew's free-first-equip.
        val equipTargetIdForCost = action.targets.filterIsInstance<ChosenTarget.Permanent>().firstOrNull()?.entityId
        val costWithDefinedX =
            castPermissionUtils.applyDefinedXValue(rawCost, ability, state, action.sourceId, action.playerId)
        // Order matters: each step prices the cost the previous one produced. The last one lowers
        // an attached-permanent mana cost (Merseine) to a plain mana atom, so nothing downstream
        // has to know that shape existed.
        val costAfterGenericReduction = applyGenericCostReduction(
            costWithDefinedX, ability, state, action.sourceId, action.playerId, action.targets
        )
        val costAfterAbilityReduction = castPermissionUtils.applyActivatedAbilityCostReduction(
            costAfterGenericReduction, state, action.sourceId, ability.isExhaust, ability.isPowerUp
        )
        val costAfterEquipReduction = castPermissionUtils.applyEquipCostReduction(
            costAfterAbilityReduction, ability, state, action.playerId, equipTargetIdForCost,
            abilitySourceId = action.sourceId
        )
        val costAfterEquipDiscount = castPermissionUtils.applyFreeFirstEquipDiscount(
            costAfterEquipReduction, ability, state, action.playerId
        )
        val costAfterColorRelaxation = castPermissionUtils.relaxAbilityCostColorsIfAny(
            state, action.sourceId, costAfterEquipDiscount
        )
        val effectiveCost = castPermissionUtils.lowerAttachedManaCost(
            state, action.sourceId, costAfterColorRelaxation
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

        // A client-supplied selection for a sum-gated graveyard exile cost is rejected outright when
        // it doesn't pay: every `GameAction` field is client-supplied, and silently substituting the
        // engine's own pick would exile cards the player never chose. An *empty* selection is not an
        // error — that is the AI / engine-direct path asking the resolver to choose.
        effectiveCost.firstExileForTotalAtomOrNull()?.let { atom ->
            val submitted = action.costPayment?.exiledCards ?: emptyList()
            if (submitted.isNotEmpty()) {
                val resolver = com.wingedsheep.engine.handlers.costs.GraveyardTotalExileResolver
                val candidates = resolver.candidates(
                    state, action.playerId, atom.measure, atom.filter
                )
                if (!resolver.isLegalSelection(candidates, atom.minTotal, submitted)) {
                    return "Those cards don't pay this cost: ${atom.description}"
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
                if (attachedContainer != null && state.projectedState.isCreature(attachedId) &&
                    SummoningSicknessRules.blocksTapOrUntapCost(
                        attachedId, attachedContainer, state.projectedState
                    )
                ) {
                    return "Enchanted creature has summoning sickness"
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

        val abilityPaymentContext = buildAbilityPaymentContext(cardComponent, state.projectedState, action.sourceId, ability)

        // The granter of a statically-granted ability, so AbilityCost.TapGrantingPermanent can be
        // checked against the *Equipment's* tap state rather than the host creature's.
        val validationGranterId = staticGrants.firstOrNull { it.first.id == action.abilityId }?.second

        if (action.paymentStrategy !is PaymentStrategy.Explicit && !canPayAbilityCostWithSources(state, costAfterConvokeReduction, action.sourceId, action.playerId, abilityPaymentContext, validationGranterId)) {
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

        // Check summoning sickness for tap/untap abilities. CR 302.6 restricts a *creature's*
        // activated ability whose cost includes the tap symbol **or the untap symbol** — read
        // creature-ness and haste from projected state so a Vehicle or animated permanent that
        // became a creature this turn is gated correctly. Gating on `isCreature` alone (no
        // `!typeLine.isLand` carve-out) is already correct for plain lands — a land that isn't
        // also a creature never satisfies `isCreature`, so this is a no-op for every ordinary
        // land's mana ability — and it's the only way to catch a land that *is* also a creature
        // (Dryad Arbor: "This land ... is affected by summoning sickness"), which the old
        // land-wide carve-out silently exempted.
        //
        // `ActivatedAbilityEnumerator` mirrors this for `AbilityCost.Untap` both bare and inside a
        // `Composite`, so the two agree on `{Q}` in either shape and the enumerator never offers an
        // activation this re-check then rejects. This one stays authoritative regardless.
        val costTouchesTapSymbol = { c: AbilityCost -> c is AbilityCost.Tap || c is AbilityCost.Untap }
        if (costTouchesTapSymbol(effectiveCost) ||
            (effectiveCost is AbilityCost.Composite && effectiveCost.costs.any(costTouchesTapSymbol))
        ) {
            if (state.projectedState.isCreature(action.sourceId) &&
                SummoningSicknessRules.blocksTapOrUntapCost(action.sourceId, container, state.projectedState)
            ) {
                return "This creature has summoning sickness"
            }
        }

        // Check activation restrictions
        for (restriction in ability.restrictions) {
            val error = checkActivationRestriction(
                state, action.playerId, action.sourceId, restriction, ability
            )
            if (error != null) return error
        }

        // Validate targets. Only the controller-chosen requirements are validated here — any
        // "… of an opponent's choice" requirement (Cuombajj Witches) is picked by an opponent in
        // a separate decision the handler raises at announcement, so it isn't on `action.targets`
        // yet at submission time (the opponent's pick is validated when it's made). See
        // [com.wingedsheep.sdk.scripting.targets.TargetChooser].
        val controllerTargetReqs = effectiveTargetReqs.filter { it.chooser == TargetChooser.Controller }
        // For a variable-count "exile/sacrifice one or more permanents you control" cost, X is
        // defined by the payer's cost choice (CR 601.2b) — the chosen set's total mana value
        // (Fabrication Foundry, whose reanimation target's "mana value X or less" legality is
        // measured against it) or simply how many were chosen (Radiant Lotus). Derive X here so
        // target validation sees the right cap; when the cost is paid via the two-step pause flow
        // the chosen set already rides on the action's costPayment.
        val variablePermanentsCost = extractVariablePermanentsCost(effectiveCost)
        val chosenForCost = action.costPayment?.variableCostPermanents ?: emptyList()
        val effectiveXValue = if (variablePermanentsCost != null && chosenForCost.isNotEmpty()) {
            variableCostX(state, variablePermanentsCost, chosenForCost)
        } else {
            action.xValue
        }
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
                // and X-bounded "mana value X or less" reanimation targets (Fabrication Foundry)
                // need the chosen X to validate — mirror the spell path.
                xValue = effectiveXValue,
                targetingSourceType = TargetingSourceType.ABILITY
            )
            if (targetError != null) {
                return targetError
            }
        } else if (controllerTargetReqs.isNotEmpty() && action.targets.isEmpty()) {
            // An empty target list is only illegal when at least one controller-chosen
            // requirement is mandatory. For an ability whose controller targets are all
            // optional ("up to one target …", e.g. Boom Box), choosing no targets is a
            // legal activation, so don't reject it here. An VariablePermanents cost drives the
            // target choice *after* the exile selection (X isn't known until then), so the
            // bare initial submission legitimately arrives with no target — the engine pauses
            // for it during execute(); don't reject that here either.
            if (variablePermanentsCost == null && controllerTargetReqs.any { it.effectiveMinCount > 0 }) {
                return "This ability requires a target"
            }
        }

        // Validate a client-supplied divided-damage division (Chandra, Flameshaper's −4). The
        // division is chosen as the ability is activated (CR 601.2d), so it arrives on the action
        // rather than being asked for at resolution. Absence is legal — the executor then raises a
        // resolution-time DistributeDecision, which is how non-interactive controllers divide — but
        // anything present must be a well-formed division of exactly the printed total.
        val distribution = action.damageDistribution
        if (distribution != null) {
            val dividedDamage = ability.effect as? DividedDamageEffect
                ?: return "This ability does not divide damage among its targets"
            val chosenTargetIds = action.targets.map { it.toEntityId() }.toSet()
            if (distribution.keys != chosenTargetIds) {
                return "Damage distribution targets must match chosen targets"
            }
            val totalDistributed = distribution.values.sum()
            if (totalDistributed != dividedDamage.totalDamage) {
                return "Total distributed damage ($totalDistributed) must equal ${dividedDamage.totalDamage}"
            }
            // CR 601.2d: each target in the division must be assigned at least 1 damage.
            if (distribution.values.any { it < 1 }) {
                return "Each target must receive at least 1 damage"
            }
        }

        return null
    }

    override fun execute(state: GameState, action: ActivateAbility): ExecutionResult {
        val window = ManaPaymentWindow.openFor(state, action.playerId)
            ?: return executeActivation(state, action)
        return executeInManaPaymentWindow(state, action, window)
    }

    /**
     * Runs a mana ability activated while the engine is asking [action]'s player for a mana payment
     * (CR 605.3a — see [ManaPaymentWindow]).
     *
     * The window is set aside for the duration so the ability resolves against a decision-free
     * state, then re-raised. Two things must survive the round trip:
     *  - **Priority.** The mana-ability path ends with `withPriority(activatingPlayer)` when the
     *    activation cost fired a trigger. That's right at priority and wrong here — the payment
     *    resumer will hand priority back itself once the payment completes — so it's restored.
     *  - **The window.** If the ability paused for a decision of its own, the
     *    [ReopenManaPaymentDecisionContinuation] pushed by [ManaPaymentWindow.suspend] re-raises it
     *    afterwards; otherwise it's re-raised here.
     */
    private fun executeInManaPaymentWindow(
        state: GameState,
        action: ActivateAbility,
        window: SelectManaSourcesDecision
    ): ExecutionResult {
        val result = executeActivation(ManaPaymentWindow.suspend(state, window), action)

        // A failed activation must not eat the window — roll all the way back.
        result.error?.let { return ExecutionResult.error(state, it) }

        val restored = if (result.state.priorityPlayerId == state.priorityPlayerId) result.state
        else result.state.copy(
            priorityPlayerId = state.priorityPlayerId,
            priorityPassedBy = state.priorityPassedBy
        )
        if (result.isPaused) {
            return ExecutionResult.paused(restored, result.pendingDecision!!, result.events)
        }
        return ManaPaymentWindow.resumeIfPending(restored, result.events, cardRegistry)
            ?: ExecutionResult.success(restored, result.events)
    }

    private fun executeActivation(state: GameState, action: ActivateAbility): ExecutionResult {
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

        // "X can't be 0" abilities (Gogo, Master of Mimicry): reject an engine-direct activation that
        // pre-fills an X below the ability's minimum. The legal-actions submission path enforces the
        // same bound via the X-choice decision's lower value.
        if (action.xValue != null && action.xValue < ability.minimumXValue) {
            return ExecutionResult.error(
                state,
                "X must be at least ${ability.minimumXValue} for ${cardComponent.name}"
            )
        }

        // Apply text-changing effects to cost
        val textReplacement = container.get<TextReplacementComponent>()
        val rawCost = if (textReplacement != null) {
            ability.cost.applyTextReplacement(textReplacement)
        } else {
            ability.cost
        }
        // Resolve a *defined* {X} (CR 107.3c) before the reductions, matching validate() and the
        // enumerator so all three paths charge the same number. Then apply ability-specific generic
        // cost reduction (e.g., The Dominion Bracelet's
        // "{X} less, where X is this creature's power"). Locked in before payment. Then apply
        // generic equip-cost reduction (Éowyn) and Forge Anew's free-first-equip discount.
        // Finally relax colored requirements when "mana of any type can be spent" applies (Sharkey).
        val equipTargetIdForCost = action.targets.filterIsInstance<ChosenTarget.Permanent>().firstOrNull()?.entityId
        val definedXValue = castPermissionUtils.definedXValue(state, ability, action.sourceId, action.playerId)
        val costWithDefinedX =
            castPermissionUtils.applyDefinedXValue(rawCost, ability, state, action.sourceId, action.playerId)
        val effectiveCost = castPermissionUtils.relaxAbilityCostColorsIfAny(
            state, action.sourceId,
            castPermissionUtils.applyFreeFirstEquipDiscount(
                castPermissionUtils.applyEquipCostReduction(
                    castPermissionUtils.applyActivatedAbilityCostReduction(
                        applyGenericCostReduction(costWithDefinedX, ability, state, action.sourceId, action.playerId, action.targets),
                        state, action.sourceId, ability.isExhaust, ability.isPowerUp
                    ),
                    ability, state, action.playerId, equipTargetIdForCost,
                    abilitySourceId = action.sourceId
                ),
                ability, state, action.playerId
            )
        )

        // Variable-count "exile/sacrifice one or more permanents you control" cost: X is defined by
        // the payer's cost choice (CR 601.2b — a variable defined by a cost choice is announced as
        // the ability is activated), measured as the chosen set's total mana value (Fabrication
        // Foundry) or its size (Radiant Lotus). It bounds any X-limited target and is stored on the
        // stack for 608.2b re-validation and for `DynamicAmount.XValue` reads at resolution. When
        // the cost is being paid, the chosen set already rides on the action's costPayment.
        val variablePermanentsCost = extractVariablePermanentsCost(effectiveCost)
        val chosenForCost = action.costPayment?.variableCostPermanents ?: emptyList()
        // An X the ability's own text defines (CR 107.3c) outranks both: it is not the payer's to
        // choose, and it is the value every other instance of X on this activation uses (CR 107.3i)
        // — the X-linked non-mana costs and any `DynamicAmount.XValue` read at resolution.
        val effectiveXValue: Int? = definedXValue
            ?: if (variablePermanentsCost != null && chosenForCost.isNotEmpty())
                variableCostX(state, variablePermanentsCost, chosenForCost)
            else action.xValue

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
            // "X can't be 0" abilities (Gogo, Master of Mimicry) set a minimum; clamp it to what the
            // player can actually pay so the decision bounds stay valid.
            val minX = ability.minimumXValue.coerceAtMost(maxX)
            val decisionId = java.util.UUID.randomUUID().toString()
            val decision = com.wingedsheep.engine.core.ChooseNumberDecision(
                id = decisionId,
                playerId = action.playerId,
                prompt = "Choose X for ${cardComponent.name} ($minX-$maxX)",
                context = com.wingedsheep.engine.core.DecisionContext(
                    sourceId = action.sourceId,
                    sourceName = cardComponent.name,
                    phase = com.wingedsheep.engine.core.DecisionPhase.CASTING
                ),
                minValue = minX,
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
        // ExileXFromGraveyard pause (legal-actions submission path).
        //
        // "Exile X cards from your graveyard" needs exactly one decision, because X *is* the size
        // of the graveyard selection: pick the cards, and X is how many you picked. So there is no
        // number picker — the engine pauses for the cards and derives X from the count.
        //
        // Winter, Cursed Rider ("{2}{U}{B}, {T}, Exile X artifact cards from your graveyard: Each
        // other nonartifact creature gets -X/-X") has no `{X}` mana at all, so without this block
        // the handler falls through to `action.xValue ?: 0` — paying nothing and resolving a no-op.
        // Necropolis Fiend ("{X}, {T}, Exile X cards from your graveyard") pays X in mana too, so
        // the mana-X pause above has already bound `xValue`; here the selection is then pinned to
        // exactly that many rather than being free.
        //
        // Skipped when `exiledCards` is pre-filled (engine-direct path / resumed replay) or when
        // there is no real choice — X == candidates, which CostHandler pays without a prompt.
        // -------------------------------------------------------------------
        // Settled already when cards are pre-filled (engine-direct path, or the resume after this
        // very pause) or when X is a bound zero — a zero selection is a legal answer, and
        // re-pausing on it would spin forever.
        val exileXCost = extractExileXFromGraveyardCost(effectiveCost)
        val exileXSettled = (action.costPayment?.exiledCards?.isNotEmpty() == true) || action.xValue == 0
        if (exileXCost != null && !exileXSettled && tapXCost == null) {
            val exileXCandidates = costHandler.findMatchingCardsUnified(
                state,
                state.getZone(com.wingedsheep.engine.state.ZoneKey(action.playerId, Zone.GRAVEYARD)),
                exileXCost.filter,
                action.playerId
            )
            // A mana `{X}` already fixed the count; otherwise the player is free to exile any
            // number of matching cards (including none) and that count becomes X.
            val fixedCount = action.xValue
            val minSelections = fixedCount ?: 0
            val maxSelections = fixedCount ?: exileXCandidates.size
            val isRealChoice = exileXCandidates.size > minSelections
            if (isRealChoice) {
                val decisionId = java.util.UUID.randomUUID().toString()
                val prompt = if (fixedCount != null) {
                    "Select $fixedCount card${if (fixedCount > 1) "s" else ""} to exile from " +
                        "graveyard for ${cardComponent.name}"
                } else {
                    "Select any number of cards to exile from graveyard for ${cardComponent.name} " +
                        "(X is the number you choose)"
                }
                val decision = com.wingedsheep.engine.core.SelectCardsDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = prompt,
                    context = com.wingedsheep.engine.core.DecisionContext(
                        sourceId = action.sourceId,
                        sourceName = cardComponent.name,
                        phase = com.wingedsheep.engine.core.DecisionPhase.CASTING
                    ),
                    options = exileXCandidates,
                    minSelections = minSelections,
                    maxSelections = maxSelections
                )
                val continuation = com.wingedsheep.engine.core.ActivateAbilityExileXFromGraveyardContinuation(
                    decisionId = decisionId,
                    action = action,
                    exileCandidates = exileXCandidates,
                    fixedCount = fixedCount
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
            // Not a real choice, so no prompt: either the graveyard has nothing matching (X = 0,
            // legal) or a mana-fixed X consumes every candidate, which CostHandler pays as-is.
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
            // The pool follows the atom's own flags, not "the activator's graveyard": Night Soil
            // exiles "two creature cards from a single graveyard", so every player's graveyard is
            // in the pool, and a graveyard holding fewer than `count` matches is dropped because
            // it can't legally supply the whole payment on its own.
            val exileOwners =
                if (exileFromGraveyardCost.anyPlayersZone) state.turnOrder else listOf(action.playerId)
            val exileCandidatesByOwner = exileOwners.map { owner ->
                costHandler.findMatchingCardsUnified(
                    state,
                    state.getZone(com.wingedsheep.engine.state.ZoneKey(owner, Zone.GRAVEYARD)),
                    exileFromGraveyardCost.filter,
                    action.playerId
                )
            }
            val exileCandidates =
                if (exileFromGraveyardCost.singleZone) {
                    exileCandidatesByOwner.filter { it.size >= exileFromGraveyardCost.count }.flatten()
                } else {
                    exileCandidatesByOwner.flatten()
                }
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
                .findMatchingCardsUnified(
                    state, state.getBattlefield(action.playerId), sacrificeCost.filter, action.playerId,
                    // Source-relative filters ("an Equipment attached to this creature") need the
                    // ability's own source to resolve; without it they match nothing.
                    sourceId = action.sourceId,
                )
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

        // -------------------------------------------------------------------
        // VariablePermanents cost-choice pause (legal-actions submission path).
        //
        // "Exile/sacrifice one or more [filter] you control" (Fabrication Foundry, Radiant Lotus).
        // The player picks which permanents to pay with — a variable-count choice (at least
        // minCount). The bare ActivateAbility arrives with no selection; pause and raise a
        // SelectCardsDecision over the eligible permanents. The resumer fills the selection and
        // re-enters, which computes X from it and — for an ability whose target wasn't gathered up
        // front — pauses again for that target (block below).
        //
        // Skipped when variableCostPermanents is already filled (engine-direct path / resumed replay).
        // -------------------------------------------------------------------
        if (variablePermanentsCost != null && chosenForCost.isEmpty()) {
            val verb = com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost.verb(variablePermanentsCost.action)
            val candidates = costHandler
                .findMatchingCardsUnified(
                    state, state.getBattlefield(action.playerId), variablePermanentsCost.filter, action.playerId,
                    // Same source-relative resolution as the sacrifice pause above, so the choices
                    // offered here are exactly the ones payment will accept.
                    sourceId = action.sourceId,
                )
                .let { if (variablePermanentsCost.excludeSelf) it.filter { id -> id != action.sourceId } else it }
            val minCount = variablePermanentsCost.minCount
            if (candidates.size < minCount) {
                return ExecutionResult.error(state, "Not enough permanents to $verb for ${cardComponent.name}")
            }
            val decisionId = java.util.UUID.randomUUID().toString()
            val prompt = "Choose one or more ${variablePermanentsCost.filter.description}s to $verb for ${cardComponent.name}"
            val decision = com.wingedsheep.engine.core.SelectCardsDecision(
                id = decisionId,
                playerId = action.playerId,
                prompt = prompt,
                context = com.wingedsheep.engine.core.DecisionContext(
                    sourceId = action.sourceId,
                    sourceName = cardComponent.name,
                    phase = com.wingedsheep.engine.core.DecisionPhase.CASTING
                ),
                options = candidates,
                minSelections = minCount,
                maxSelections = candidates.size
            )
            val continuation = com.wingedsheep.engine.core.ActivateAbilityVariablePermanentsContinuation(
                decisionId = decisionId,
                action = action,
                candidates = candidates,
                minCount = minCount
            )
            val pausedState = state.withPendingDecision(decision).pushContinuation(continuation)
            val event = com.wingedsheep.engine.core.DecisionRequestedEvent(
                decisionId = decisionId,
                playerId = action.playerId,
                decisionType = "SELECT_CARDS",
                prompt = prompt
            )
            return ExecutionResult.paused(pausedState, decision, listOf(event))
        }

        // -------------------------------------------------------------------
        // Target pause for a VariablePermanents ability (Fabrication Foundry, Radiant Lotus).
        //
        // The enumerator deliberately surfaces these abilities with no target gathered up front,
        // because a target may be bounded by X ("mana value X or less") and X isn't known until the
        // cost choice is made. Once that selection is known (block above resumed → X computed),
        // raise the controller's target choice with X threaded through the predicate context, so an
        // over-X target can't be picked and then fizzle. The resumer fills action.targets and
        // re-enters to pay + resolve. Skipped on the engine-direct path (targets already supplied)
        // and for abilities with no controller target.
        // -------------------------------------------------------------------
        if (variablePermanentsCost != null && chosenForCost.isNotEmpty() && action.targets.isEmpty()) {
            val execTargetReqs = if (textReplacement != null) {
                ability.targetRequirements.map { it.applyTextReplacement(textReplacement) }
            } else {
                ability.targetRequirements
            }
            val controllerTargetReqsExec = execTargetReqs.filter { it.chooser == TargetChooser.Controller }
            if (controllerTargetReqsExec.any { it.effectiveMinCount > 0 }) {
                val xForTargets = effectiveXValue ?: 0
                val finder = com.wingedsheep.engine.handlers.TargetFinder()
                val pipelineContext = com.wingedsheep.engine.handlers.PredicateContext(
                    controllerId = action.playerId,
                    sourceId = action.sourceId,
                    xValue = xForTargets
                )
                val legalTargets = mutableMapOf<Int, List<EntityId>>()
                val requirementInfos = controllerTargetReqsExec.mapIndexed { index, req ->
                    val legal = finder.findLegalTargets(
                        state, req, action.playerId, action.sourceId, pipelineContext = pipelineContext
                    )
                    if (legal.isEmpty() && req.effectiveMinCount > 0) {
                        return ExecutionResult.error(state, "No legal target for ${cardComponent.name}")
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
                val prompt = "Choose ${controllerTargetReqsExec.joinToString(" and ") { it.description }} for ${cardComponent.name}"
                val decision = com.wingedsheep.engine.core.ChooseTargetsDecision(
                    id = decisionId,
                    playerId = action.playerId,
                    prompt = prompt,
                    context = com.wingedsheep.engine.core.DecisionContext(
                        sourceId = action.sourceId,
                        sourceName = cardComponent.name,
                        phase = com.wingedsheep.engine.core.DecisionPhase.CASTING
                    ),
                    targetRequirements = requirementInfos,
                    legalTargets = legalTargets
                )
                val continuation = com.wingedsheep.engine.core.ActivateAbilityControllerTargetContinuation(
                    decisionId = decisionId,
                    action = action,
                    requirements = controllerTargetReqsExec
                )
                val pausedState = state.withPendingDecision(decision).pushContinuation(continuation)
                val event = com.wingedsheep.engine.core.DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = action.playerId,
                    decisionType = "CHOOSE_TARGETS",
                    prompt = prompt
                )
                return ExecutionResult.paused(pausedState, decision, listOf(event))
            }
        }

        val executeAbilityContext = buildAbilityPaymentContext(cardComponent, state.projectedState, action.sourceId, ability)

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
            restrictedMana = poolComponent.restrictedMana,
            // Carry mana-source provenance through the activation. pay()/spend() decrement colours
            // but leave these maps untouched; the writeback below consumes them proportional to the
            // floating mana actually spent, so tags for mana floated from earlier sources survive an
            // ability activation instead of being wiped (mirrors CastPaymentProcessor's threading).
            manaBySubtype = poolComponent.manaBySubtype,
            manaBySource = poolComponent.manaBySource
        )

        // Pay mana costs before paying other costs
        var effectiveManaCost = extractManaCost(effectiveCost)
        // For an VariablePermanents cost, X is the exiled permanents' total mana value (computed above);
        // otherwise it's the action's chosen X. Identical to `action.xValue ?: 0` for every other card.
        val xValue = effectiveXValue ?: 0

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
        // CR 701.67c: paying an ability's waterbend cost (however paid — taps above and/or the mana
        // paid below) fires "whenever you waterbend". The waterbend cost is applied before mana
        // payment, so this reaches every hasWaterbend activation; a later mana failure rolls the
        // whole activation (and this event) back.
        if (ability.hasWaterbend) {
            val (bendState, bendEvent) = BendEvents.record(currentState, action.playerId, BendType.WATER)
            currentState = bendState
            events.add(bendEvent)
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
                    // Spend floating mana first, then tap only the minimum subset of chosen
                    // sources required to cover what the pool can't — parity with the auto-tap
                    // branch below (autoTapForManaCost) and CastPaymentProcessor.autoPay. Without
                    // the payPartial, mana already in the pool is stranded: the solver would tap
                    // sources for the whole cost and the pool deduction is skipped (Mana stripped
                    // in costForPayment below), so pre-floated mana is never spent. This bit
                    // waterbend/convoke abilities in particular — the client always routes them
                    // through Explicit payment, and the enumerator deems them affordable counting
                    // pool + sources, so ignoring the pool here made a legal activation fail
                    // ("Selected mana sources cannot pay this ability's cost") or over-tap lands.
                    // The reduced [manaPool] flows into payAbilityCost and is persisted afterward.
                    val partialResult = manaPool.payPartial(manaCost, executeAbilityContext)
                    manaPool = partialResult.newPool
                    val remainingCost = partialResult.remainingCost
                    if (!remainingCost.isEmpty() || manaXValue > 0) {
                        // Solve the remainder against the chosen sources only (non-chosen excluded),
                        // matching CastPaymentProcessor.explicitPay so we never tap more than needed.
                        // The client's auto-tap preview is computed against the full cost and may
                        // over-select; excluding the rest keeps validation and execution in sync.
                        val chosen = action.paymentStrategy.manaAbilitiesToActivate.toSet()
                        val excluded = manaSolver.findAvailableManaSources(currentState, action.playerId)
                            .map { it.entityId }
                            .filter { it !in chosen }
                            .toSet() + selfExcludedSources
                        val solution = manaSolver.solve(
                            currentState, action.playerId, remainingCost, manaXValue, excludeSources = excluded, xManaRestriction = ability.xManaRestriction
                        ) ?: return ExecutionResult.error(state, "Selected mana sources cannot pay this ability's cost")
                        for (source in solution.sources) {
                            val (tappedState, tapEvent) = tap(currentState, source.entityId)
                            currentState = tappedState
                            tapEvent?.let(events::add)
                        }
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

        // A sum-gated graveyard exile cost (`ExileFromGraveyardForTotal`) decides *here* which cards
        // it will exile, rather than leaving CostHandler to fall back on its own pick during
        // payment. Two things need the same answer and would otherwise diverge: the payment, and
        // the record of "those exiled cards" that rides the stack to resolution. Resolving it once
        // and feeding it into `exileChoices` makes them the same list by construction.
        //
        // This *replaces* the submitted list rather than merging into it, which is only correct
        // because such a cost is the ability's sole exile atom — see
        // [firstExileForTotalAtomOrNull]. A composite pairing it with another exile atom would
        // drop that atom's selection here.
        val totalExileAtom = effectiveCost.firstExileForTotalAtomOrNull()
        val exileChoices = if (totalExileAtom == null) {
            action.costPayment?.exiledCards ?: emptyList()
        } else {
            val resolver = com.wingedsheep.engine.handlers.costs.GraveyardTotalExileResolver
            resolver.resolveSelection(
                resolver.candidates(
                    currentState, action.playerId, totalExileAtom.measure, totalExileAtom.filter
                ),
                totalExileAtom.minTotal,
                action.costPayment?.exiledCards ?: emptyList(),
            )
        }

        // Build cost payment choices from the action
        val costChoices = CostPaymentChoices(
            sacrificeChoices = action.costPayment?.sacrificedPermanents ?: emptyList(),
            discardChoices = action.costPayment?.discardedCards ?: emptyList(),
            exileChoices = exileChoices,
            variablePermanentChoices = action.costPayment?.variableCostPermanents ?: emptyList(),
            tapChoices = firstTapSlice,
            bounceChoices = action.costPayment?.bouncedPermanents ?: emptyList(),
            xValue = xValue,
            distributedCounterRemovals = action.costPayment?.distributedCounterRemovals ?: emptyList(),
            blightChoices = action.costPayment?.blightTargets ?: emptyList(),
            granterId = staticGranterId
        )

        // Snapshot projected subtypes and P/T of sacrifice targets before zone change
        // (Rule 113.7a / 608.2h — "as it last existed on the battlefield"). Covers both the
        // fixed-count sacrifice cost and a variable-count one, which moves permanents just the same.
        val sacrificeTargetIds = (action.costPayment?.sacrificedPermanents ?: emptyList()) +
            (action.costPayment?.variableCostPermanents ?: emptyList())
        val sacrificedSnapshots = captureEntitySnapshots(sacrificeTargetIds, currentState.projectedState)

        // Mirror sacrifice snapshots for tapped-as-cost permanents — they may leave the
        // battlefield in response while the ability is on the stack.
        val tappedTargetIds = firstTapSlice
        val tappedSnapshots = captureEntitySnapshots(tappedTargetIds, currentState.projectedState)

        // Snapshot the source's counters before a self-exile / self-sacrifice cost wipes them
        // (CR 113.7a / 122.2), so the effect can read the pre-cost count via
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

        // Snapshot the source's projected characteristics before a self-exile / self-sacrifice cost
        // moves it off the battlefield (CR 113.7a / 608.2h), so an effect that reads its own power —
        // e.g. "Sacrifice this creature: it deals damage equal to its power" (Ghitu Fire-Eater,
        // Cinder Shade, Blazing Bomb's Blow Up) — sees the pre-sacrifice power rather than zero.
        // Mirrors lastKnownSourceCounters above.
        //
        // The projected *type line* and token-ness ride along because for a **token** source this
        // snapshot is the only surviving record of the object at all: CR 704.5d sweeps a token out
        // of any non-battlefield zone as a state-based action and the entity is deleted outright,
        // so by the time the ability sits on the stack `state.getEntity(sourceId)` is null. That is
        // what lets "copy target activated ability you control from an artifact source" (Scientist
        // Supreme of A.I.M.) still see a cracked Clue as an artifact source — see
        // `CardPredicate.AbilitySourceMatches` in PredicateEvaluator. Reading the *projected* type
        // line here also gets the animated-artifact / crewed-Vehicle source right.
        //
        // The state-aware `captureEntitySnapshots` overload already freezes token-ness and the
        // name; only the projected type line, keywords and card-definition id are layered on top.
        val lastKnownSourceSnapshot: com.wingedsheep.engine.state.components.stack.EntitySnapshot? =
            if (costExilesOrSacrificesSelf(effectiveCost)) {
                captureEntitySnapshots(listOf(action.sourceId), currentState)
                    .firstOrNull()
                    ?.copy(
                        typeLine = com.wingedsheep.engine.state.components.stack.projectedTypeLine(
                            currentState, action.sourceId
                        ),
                        keywords = currentState.projectedState.getKeywords(action.sourceId),
                        cardDefinitionId = currentState.getEntity(action.sourceId)
                            ?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()
                            ?.cardDefinitionId,
                    )
            } else null

        // Snapshot the entity ids attached to the source before a self-exile / self-sacrifice cost
        // moves it off the battlefield (CR 113.7a). The host's live AttachmentsComponent is gone by
        // resolution, so capture it now — read via CardSource.LastKnownEquipmentAttachedToSource to
        // re-attach "an Equipment that was attached to it" (Zack Fair). Mirrors lastKnownSourceCounters.
        val lastKnownSourceAttachments: List<EntityId> =
            if (costExilesOrSacrificesSelf(effectiveCost)) {
                currentState.getEntity(action.sourceId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.AttachmentsComponent>()
                    ?.attachedIds
                    ?: emptyList()
            } else emptyList()

        // Snapshot the creature type this permanent's controller secretly noted, before the same
        // cost's self-sacrifice takes the permanent — and the note with it — off the battlefield
        // (CR 113.7a). Read at resolution as chosenValues["chosenCreatureType"], which is what lets
        // A Killer Among Us still ask "is the target the chosen type?" after it is in the graveyard.
        // Mirrors lastKnownSourceCounters above.
        val revealedNotedCreatureType: String? =
            if (costRevealsNotedCreatureType(effectiveCost)) {
                currentState.getEntity(action.sourceId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.NotedCreatureTypesComponent>()
                    ?.types
                    ?.firstOrNull()
            } else null

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
        // handling ([ManaAbilityResolutionPipeline] etc.).
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
        // Consume mana-source provenance for the floating mana this activation spent (the maps ride
        // `manaPool` untouched by pay()/spend()), so the remaining tags reflect only the mana still
        // in the pool — a mana ability that only adds mana consumes nothing and keeps prior tags.
        val originalUnrestricted = poolComponent.white + poolComponent.blue + poolComponent.black +
            poolComponent.red + poolComponent.green + poolComponent.colorless
        val finalUnrestricted = manaPool.white + manaPool.blue + manaPool.black +
            manaPool.red + manaPool.green + manaPool.colorless
        val (poolAfterProvenance, _) = manaPool.consumeProvenance(maxOf(0, originalUnrestricted - finalUnrestricted))
        currentState = currentState.updateEntity(action.playerId) { c ->
            c.with(ManaPoolComponent(
                white = manaPool.white,
                blue = manaPool.blue,
                black = manaPool.black,
                red = manaPool.red,
                green = manaPool.green,
                colorless = manaPool.colorless,
                restrictedMana = manaPool.restrictedMana,
                manaBySubtype = poolAfterProvenance.manaBySubtype,
                manaBySource = poolAfterProvenance.manaBySource
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
        // `trackActivations` opts an unrestricted ability into the same tally so its own effect can
        // read the count back (Farrelite Priest's burnout clause).
        if (ability.trackActivations || ability.restrictions.any { isPerTurnTracked(it) }) {
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

        // Track exhaust activations this turn (CR 702.177). Elvish Refueler's waiver of the
        // once-only memory holds only "as long as you haven't activated an exhaust ability this
        // turn", so the count has to advance for the very activation that used the waiver too —
        // which is why this is unconditional on whether the waiver applied.
        if (ability.isExhaust) {
            currentState = currentState.updateEntity(action.playerId) { c ->
                val tracker = c.get<com.wingedsheep.engine.state.components.player.ExhaustAbilitiesActivatedThisTurnComponent>()
                    ?: com.wingedsheep.engine.state.components.player.ExhaustAbilitiesActivatedThisTurnComponent()
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
            // A mana ability is still an *activated* ability (CR 605.3), so activating one is an
            // activation event like any other — Elrond, Moon-Reader's "whenever you activate an
            // ability of a creature" fires off a creature's "{T}: Add {G}" per its ruling. Mana
            // abilities resolve off the stack, so StackResolver never emits AbilityActivatedEvent
            // for them; emit it here for every mana ability, {T}-costed or not. Consumers pick
            // their own semantic off the flags: the default "isn't a mana ability" wording rejects
            // `isManaAbility`, the Antiquities "without {T} in its activation cost" template
            // (Haunting Wind / Powerleech / Artifact Possession) rejects `costsTap`, and the
            // unqualified wording accepts both.
            //
            // Emitted here rather than after resolution because the branch below has two pause
            // exits — an any-color effect (Birds of Paradise) and an any-color tap bonus (Fertile
            // Ground) — and the ability is already activated by this point: its costs are paid.
            // Adding it to [events] now carries it out through those exits too.
            val manaAbilityActivatedEvent = AbilityActivatedEvent(
                sourceId = action.sourceId,
                sourceName = cardComponent.name,
                controllerId = action.playerId,
                abilityEntityId = null,
                costsTap = hasTapCost(effectiveCost),
                isManaAbility = true,
                isExhaust = ability.isExhaust
            )
            events.add(manaAbilityActivatedEvent)

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
            val colorReplacement = manaColorReplacementFor(currentState, action.sourceId)
            if (colorReplacement != null) {
                val fixed = colorReplacement.color
                finalEffect = when (val fe = finalEffect) {
                    // A *fixed* colour (Deep Water: "it produces {U} instead of any other type")
                    // needs no choice at all — rewrite the produced mana directly.
                    is AddManaEffect ->
                        if (fixed != null) fe.copy(color = fixed)
                        else AddManaOfChoiceEffect(ManaColorSet.AnyColor, fe.amount)
                    is AddColorlessManaEffect ->
                        if (fixed != null) AddManaEffect(fixed, fe.amount)
                        else AddManaOfChoiceEffect(ManaColorSet.AnyColor, fe.amount)
                    else -> finalEffect
                }
            }
            // Multiplicative mana replacement (Virtue of Strength: "If you tap a basic land for
            // mana, it produces three times as much of that mana instead"). Scaling the resolving
            // effect's amount — rather than the pool afterwards — keeps restricted mana, riders and
            // per-source provenance intact, and makes the ManaAddedEvent below report the real
            // amount for free. Gated on {T} in the cost: you are only "tapping a permanent for
            // mana" when the mana ability's cost includes the tap symbol.
            if (hasTapCost(effectiveCost)) {
                val manaMultiplier = manaProductionMultiplierFor(currentState, action.sourceId)
                if (manaMultiplier > 1) {
                    finalEffect = multiplyManaProduced(finalEffect, manaMultiplier)
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
                // A mana ability resolves off the stack, so nothing else hands it the last-known
                // information its cost captured. Priest of Yawgmoth ("{T}, Sacrifice an artifact:
                // Add an amount of {B} equal to the sacrificed artifact's mana value") reads the
                // sacrificed permanent's mana value through EntityReference.Sacrificed after that
                // permanent is already in the graveyard (CR 113.7a); without the snapshots the
                // amount resolves to 0 and the ability produces nothing.
                sacrificedPermanents = sacrificedSnapshots,
                manaColorChoice = action.manaColorChoice,
                // The tally above was already incremented for this activation, so a burnout clause
                // reading "four or more times this turn" sees the fourth activation as the fourth.
                activatedAbilityId = if (ability.trackActivations) ability.id else null,
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
                val deferred = triggerDetector.detectTriggers(
                    effectResult.state, costPaymentEvents + manaAbilityActivatedEvent
                )
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
            val dampening = manaPipeline.applyLandManaDampening(
                state, currentState, cardComponent, action.playerId
            )
            currentState = dampening.state
            val manaDampened = dampening.dampened

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

            // Aura bonuses (Elvish Guidance), global "whenever a matching source is tapped for
            // mana" statics (Lavaleaper, Badgermole Cub, Overabundance), the land-tapped event, and
            // the any-color tap bonuses (Fertile Ground) — the last of which may pause for a color
            // decision. Shared with the color-choice resume path so both agree.
            val bonusResult = manaPipeline.finishTapBonuses(
                currentState, action.sourceId, cardComponent, action.playerId,
                manaEvent, events + effectResult.events
            )
            if (bonusResult.isPaused) return bonusResult

            // Detect and queue any triggered abilities from the activation — the cost-side events
            // (a sacrificed source's dies trigger, the {T} TappedEvent for an artifact-tap trigger),
            // the mana-ability activation event from the top of this branch, and the mana ability's OWN effect
            // resolution events (e.g. a `ReflexiveTriggerEffect`'s `ReflexiveAbilityTriggeredEvent` —
            // Rubble Rouser's "Add {R}. When you do, deal 1 damage to each opponent": the reflexive
            // half is NOT itself a mana ability (CR 605.1a requires it produce mana), so it must go
            // on the stack normally even though the ability that caused it resolved off it). Such
            // triggered abilities still use the stack even though the mana ability itself resolves
            // off it.
            // `activationCostEvents` was snapshotted before `manaAbilityActivatedEvent` was added,
            // so naming it here adds it exactly once. `bonusResult.events` already carries it —
            // it flowed in through `events` — so `resultEvents` must not append it again.
            val activationTriggerEvents =
                activationCostEvents + manaAbilityActivatedEvent + effectResult.events
            val resultEvents = bonusResult.events
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
            return bonusResult
        }

        // Non-mana abilities go on the stack
        val abilityOnStack = ActivatedAbilityOnStackComponent(
            sourceId = action.sourceId,
            sourceName = cardComponent.name,
            controllerId = action.playerId,
            effect = finalEffect,
            sacrificedPermanents = sacrificedSnapshots,
            // VariablePermanents X (exiled total mana value) is stored so 608.2b re-validation of the
            // "mana value X or less" target and any XValue read resolve against it; else action.xValue.
            xValue = effectiveXValue,
            tappedPermanents = firstTapSlice,
            tappedEntitySnapshots = tappedSnapshots,
            // An exile cost records its selection so the resolving effect can refer back to the
            // cards it exiled (`CardSource.ExiledAsCost`) — the sum-gated form (Baron Helmut Zemo)
            // and the plain counted form (Necropolis) alike. Empty for an ability whose cost exiles
            // nothing, so nothing else changes.
            exiledAsCostCards = if (effectiveCost.hasExileAtom()) exileChoices else emptyList(),
            lastKnownSourceCounters = lastKnownSourceCounters,
            lastKnownSourceSnapshot = lastKnownSourceSnapshot,
            lastKnownSourceAttachments = lastKnownSourceAttachments,
            revealedNotedCreatureType = revealedNotedCreatureType,
            descriptionOverride = ability.descriptionOverride,
            abilityIdentity = com.wingedsheep.sdk.scripting.AbilityIdentity(
                cardComponent.cardDefinitionId, ability.id
            ),
            granterId = staticGranterId,
            // Lock in the activation-time damage division (CR 601.2d) so removal in response
            // can't hand the controller a fresh division at resolution.
            damageDistribution = action.damageDistribution
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
            costsTap = hasTapCost(effectiveCost),
            isExhaust = ability.isExhaust,
            cantBeCopied = ability.cantBeCopied
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
                    colorless = repeatPoolComponent.colorless,
                    manaBySubtype = repeatPoolComponent.manaBySubtype,
                    manaBySource = repeatPoolComponent.manaBySource
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
                // mana as before. Snapshot the creature before it's tapped (Rule 113.7a) so
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

                // Update mana pool on state (consuming provenance for the floating mana this repeat
                // spent, same rule as the primary writeback above).
                val repeatOriginalUnrestricted = repeatPoolComponent.white + repeatPoolComponent.blue +
                    repeatPoolComponent.black + repeatPoolComponent.red + repeatPoolComponent.green +
                    repeatPoolComponent.colorless
                val repeatFinalUnrestricted = repeatPool.white + repeatPool.blue + repeatPool.black +
                    repeatPool.red + repeatPool.green + repeatPool.colorless
                val (repeatPoolAfterProvenance, _) =
                    repeatPool.consumeProvenance(maxOf(0, repeatOriginalUnrestricted - repeatFinalUnrestricted))
                currentState = currentState.updateEntity(action.playerId) { c ->
                    c.with(ManaPoolComponent(
                        white = repeatPool.white,
                        blue = repeatPool.blue,
                        black = repeatPool.black,
                        red = repeatPool.red,
                        green = repeatPool.green,
                        colorless = repeatPool.colorless,
                        manaBySubtype = repeatPoolAfterProvenance.manaBySubtype,
                        manaBySource = repeatPoolAfterProvenance.manaBySource
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
                    targetRequirements = effectiveTargetReqs,
                    isExhaust = ability.isExhaust,
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

        val opponentIds = state.getOpponents(action.playerId)
        if (opponentIds.isEmpty()) {
            return ExecutionResult.error(state, "No opponent available to choose a target")
        }
        if (opponentIds.size > 1) {
            return pauseForOpponentTargetChooser(
                state, action, sourceName, fullTargetReqs, opponentReqs, opponentIds
            )
        }

        return pauseForOpponentChosenTargetsForDecider(
            state = state,
            action = action,
            sourceName = sourceName,
            fullTargetReqs = fullTargetReqs,
            opponentReqs = opponentReqs,
            deciderId = opponentIds.single()
        )
    }

    private fun pauseForOpponentTargetChooser(
        state: GameState,
        action: ActivateAbility,
        sourceName: String,
        fullTargetReqs: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
        opponentReqs: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
        opponentIds: List<EntityId>
    ): ExecutionResult {
        val opponentNames = opponentIds.map { opponentId ->
            state.getEntity(opponentId)
                ?.get<com.wingedsheep.engine.state.components.identity.PlayerComponent>()?.name
                ?: "Player ${opponentId.value}"
        }
        val decisionId = java.util.UUID.randomUUID().toString()
        val prompt = "Choose an opponent to choose a target for $sourceName"
        val decision = com.wingedsheep.engine.core.ChooseOptionDecision(
            id = decisionId,
            playerId = action.playerId,
            prompt = prompt,
            context = com.wingedsheep.engine.core.DecisionContext(
                sourceId = action.sourceId,
                sourceName = sourceName,
                phase = com.wingedsheep.engine.core.DecisionPhase.CASTING
            ),
            options = opponentNames
        )
        val continuation = com.wingedsheep.engine.core.ActivateAbilityOpponentChooserContinuation(
            decisionId = decisionId,
            action = action,
            sourceName = sourceName,
            opponentRequirements = opponentReqs,
            fullRequirements = fullTargetReqs,
            opponentIds = opponentIds
        )
        val pausedState = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)
        val event = com.wingedsheep.engine.core.DecisionRequestedEvent(
            decisionId = decisionId,
            playerId = action.playerId,
            decisionType = "CHOOSE_OPTION",
            prompt = prompt
        )
        return ExecutionResult.paused(pausedState, decision, listOf(event))
    }

    internal fun pauseForOpponentChosenTargetsForDecider(
        state: GameState,
        action: ActivateAbility,
        sourceName: String,
        fullTargetReqs: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
        opponentReqs: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement>,
        deciderId: EntityId
    ): ExecutionResult {
        if (!state.getOpponents(action.playerId).contains(deciderId)) {
            return ExecutionResult.error(state, "Chosen player is not an opponent")
        }

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
        granterId: com.wingedsheep.sdk.model.EntityId? = null,
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
                else costHandler.canPayAbilityCost(state, cost, sourceId, playerId, manaPool, abilityContext, granterId)
            }
            is AbilityCost.Composite -> {
                // If composite cost includes Tap, the source itself can't also be used as a mana source
                val excludeSources = if (hasTapCost(cost)) setOf(sourceId) else emptySet()
                cost.costs.all { subCost ->
                    val subMana = subCost.manaCostOrNull
                    if (subMana != null) manaSolver.canPay(state, playerId, subMana, excludeSources = excludeSources, spellContext = abilityContext)
                    else costHandler.canPayAbilityCost(state, subCost, sourceId, playerId, manaPool, abilityContext, granterId)
                }
            }
            else -> costHandler.canPayAbilityCost(state, cost, sourceId, playerId, manaPool, abilityContext, granterId)
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
     * Whether [cost] removes the source from its current zone — a self-exile, self-sacrifice, or
     * self-bounce. Used to decide whether to snapshot the source's counters before payment so the
     * resolving effect can read the pre-cost count (DynamicAmount.LastKnownSourceCounters).
     */
    private fun costExilesOrSacrificesSelf(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.ExileSelf, is AbilityCost.SacrificeSelf, is AbilityCost.ReturnSelfToHand -> true
        is AbilityCost.Composite -> cost.costs.any { costExilesOrSacrificesSelf(it) }
        else -> false
    }

    /**
     * Whether [cost] includes "Reveal the creature type you chose" — the signal to capture the
     * source's noted type before the cost is paid. The same activation typically sacrifices the
     * source (A Killer Among Us), so by resolution the permanent and its note are gone; this is
     * the CR 113.7a capture that keeps the ability's "if the target is the chosen type" answerable.
     */
    private fun costRevealsNotedCreatureType(cost: AbilityCost): Boolean = when (cost) {
        is AbilityCost.Atom -> cost.atom is CostAtom.RevealNotedCreatureType
        is AbilityCost.Composite -> cost.costs.any { costRevealsNotedCreatureType(it) }
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
            // Auto-tapping a source to pay an ability's mana cost activates that source's mana
            // ability just as a manual tap would (CR 605.3) — emit the same activation event the
            // shared cast/cycling/plot auto-tap path emits, so "whenever you activate an ability"
            // triggers (Elrond, Moon-Reader) don't silently miss the fast path.
            manaAbilitySideEffectExecutor.activationEvent(
                currentState,
                source.entityId,
                solution.manaProduced[source.entityId]?.color,
                playerId
            )?.let(events::add)
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

        // Update state with enriched pool — carry restrictedMana and mana-source provenance through
        // so the ability-payment context can spend (and the leftover can stay) restricted, and so the
        // caller's final writeback still sees tags for mana floated before this auto-tap. This write
        // is transient (the caller overwrites the post-payment pool), but keeps intermediate state
        // consistent for anything that reads the pool between auto-tap and payment.
        currentState = currentState.updateEntity(playerId) { c ->
            c.with(ManaPoolComponent(
                white = currentPool.white,
                blue = currentPool.blue,
                black = currentPool.black,
                red = currentPool.red,
                green = currentPool.green,
                colorless = currentPool.colorless,
                restrictedMana = currentPool.restrictedMana,
                manaBySubtype = currentPool.manaBySubtype,
                manaBySource = currentPool.manaBySource,
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

    /** Whether [restriction] opens the ability to players other than the source's controller. */
    private fun anyPlayerMayIn(restriction: ActivationRestriction): Boolean = when (restriction) {
        is ActivationRestriction.AnyPlayerMay -> true
        is ActivationRestriction.All -> restriction.restrictions.any { anyPlayerMayIn(it) }
        else -> false
    }

    private fun checkActivationRestriction(
        state: GameState,
        playerId: com.wingedsheep.sdk.model.EntityId,
        sourceId: com.wingedsheep.sdk.model.EntityId,
        restriction: ActivationRestriction,
        // Required, with no default: a defaulted `ability` would let a forgetful call site silently
        // disable the ExtraOnceOnlyActivations permission on this path while the enumerators kept
        // honouring it. Omission must be a compile error, not a behaviour difference. It is also
        // the only source of the ability id the turn trackers key on, so that id can't disagree
        // with the ability whose flags the `Once` branch reads.
        ability: com.wingedsheep.sdk.scripting.ActivatedAbility
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
                if (tracker != null && tracker.hasActivated(ability.id)) {
                    "This ability can only be activated once each turn"
                } else null
            }
            is ActivationRestriction.MaxPerTurn -> {
                val tracker = state.getEntity(sourceId)?.get<AbilityActivatedThisTurnComponent>()
                if ((tracker?.activationCount(ability.id) ?: 0) >= restriction.count) {
                    "This ability can't be activated more than ${restriction.count} times each turn"
                } else null
            }
            is ActivationRestriction.Once -> {
                // An exhaust or power-up ability's once-only memory can be raised or waived by an
                // ExtraOnceOnlyActivations permission (Elvish Refueler, Wonder Man); a plain Once
                // restriction on an ordinary ability never is.
                val allowed = castPermissionUtils.mayActivateOnceOnlyAbility(state, playerId, sourceId, ability)
                if (!allowed) "This ability can only be activated once" else null
            }
            is ActivationRestriction.ControlledSinceYourMostRecentTurn -> {
                if (state.getEntity(sourceId)
                        ?.has<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>() == true
                ) "You must have controlled this permanent continuously since your most recent turn began"
                else null
            }
            is ActivationRestriction.All -> {
                restriction.restrictions.firstNotNullOfOrNull {
                    checkActivationRestriction(state, playerId, sourceId, it, ability)
                }
            }
        }
    }

    private val dynamicAmountEvaluator = DynamicAmountEvaluator()
    private val predicateEvaluator = PredicateEvaluator()

    /**
     * Everything that happens after a mana ability's effect resolves. Shared with
     * [com.wingedsheep.engine.handlers.continuations.ColorChoiceContinuationResumer], which
     * finishes the mana abilities that paused for a color choice.
     */
    private val manaPipeline = com.wingedsheep.engine.handlers.effects.mana.ManaAbilityResolutionPipeline(
        cardRegistry = cardRegistry,
        conditionEvaluator = conditionEvaluator,
        effectExecutorRegistry = effectExecutorRegistry,
        predicateEvaluator = predicateEvaluator,
        dynamicAmountEvaluator = dynamicAmountEvaluator,
    )

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
     * The [ReplaceLandManaColor] static the land [landId] is subject to, if any — some permanent on
     * the battlefield has that static and its filter matches the tapped land from the static
     * controller's projected perspective. The land's produced mana is then replaced: with one mana
     * of a color of its controller's choice (Pulse of Llanowar), or with the static's fixed `color`
     * when it names one (Deep Water). Returns the static rather than a Boolean so the caller can
     * tell those two apart.
     */
    private fun manaColorReplacementFor(
        state: GameState,
        landId: EntityId
    ): ReplaceLandManaColor? {
        val grantsByEntity = state.grantedStaticAbilities.groupBy { it.entityId }
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val printed = cardRegistry.getCard(card.cardDefinitionId)?.script?.staticAbilities.orEmpty()
            // Granted statics too — a durational "{U}: … until end of turn" mana rule (Deep Water)
            // lives only in `grantedStaticAbilities`, since the layer projector doesn't carry them.
            val granted = grantsByEntity[entityId]?.map { it.ability }.orEmpty()
            for (staticAbility in if (granted.isEmpty()) printed else printed + granted) {
                val replacement = staticAbility as? ReplaceLandManaColor ?: continue
                val staticController = state.projectedState.getController(entityId) ?: continue
                val filterContext = PredicateContext(controllerId = staticController, sourceId = entityId)
                if (predicateEvaluator.matches(state, state.projectedState, landId, replacement.filter, filterContext)) {
                    return replacement
                }
            }
        }
        return null
    }

    /**
     * The combined [MultiplyManaOnSourceTap] factor applying to [sourceId] being tapped for mana
     * (Virtue of Strength: 3). Returns 1 when nothing on the battlefield multiplies this source.
     *
     * Instances stack **multiplicatively** — two Virtues of Strength make a basic land produce nine
     * times as much, per the printed ruling — so the factors are folded with `*`.
     *
     * Mirrors [manaColorReplacementFor]: each static's filter is evaluated from the
     * *static's own* projected controller, so `.youControl()` means "controlled by the player who
     * controls the Virtue", which for a mana ability is necessarily the tapping player.
     */
    private fun manaProductionMultiplierFor(
        state: GameState,
        sourceId: EntityId
    ): Int {
        var multiplier = 1
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (staticAbility in cardDef.script.staticAbilities) {
                val static = staticAbility as? MultiplyManaOnSourceTap ?: continue
                if (static.multiplier <= 1) continue
                val staticController = state.projectedState.getController(entityId) ?: continue
                val filterContext = PredicateContext(controllerId = staticController, sourceId = entityId)
                if (predicateEvaluator.matches(
                        state, state.projectedState, sourceId, static.sourceFilter, filterContext
                    )
                ) {
                    multiplier *= static.multiplier
                }
            }
        }
        return multiplier
    }

    /**
     * Scales the mana [effect] produces by [multiplier], leaving everything else about it — color,
     * restriction, riders, expiry — untouched. Recurses into a [CompositeEffect] so a mana ability
     * bundled with a side effect (pain, a counter) scales its mana half only.
     *
     * [AddOneManaOfEachColorAmongEffect] has no amount to scale (it is "one of each colour among
     * …"), so it is deliberately left alone rather than silently mis-scaled.
     */
    private fun multiplyManaProduced(effect: Effect, multiplier: Int): Effect = when (effect) {
        is AddManaEffect -> effect.copy(amount = DynamicAmount.Multiply(effect.amount, multiplier))
        is AddColorlessManaEffect -> effect.copy(amount = DynamicAmount.Multiply(effect.amount, multiplier))
        is AddManaOfChoiceEffect -> effect.copy(amount = DynamicAmount.Multiply(effect.amount, multiplier))
        is AddAnyColorManaSpendOnChosenTypeEffect ->
            effect.copy(amount = DynamicAmount.Multiply(effect.amount, multiplier))
        is AddDynamicManaEffect ->
            effect.copy(amountSource = DynamicAmount.Multiply(effect.amountSource, multiplier))
        is CompositeEffect -> effect.copy(effects = effect.effects.map { multiplyManaProduced(it, multiplier) })
        else -> effect
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
            for (rawAbility in cardDef.script.effectiveStaticAbilities(classLevel)) {
                // A grant can be gated by a ConditionalStaticAbility (Nature's Embrace: the land host
                // gains "{T}: Add two mana of any one color" only while it is a land). Unwrap the
                // condition against the granter here so the actual-activation path agrees with the
                // enumerator; skip when the gate is currently false.
                val ability = when (rawAbility) {
                    is com.wingedsheep.sdk.scripting.ConditionalStaticAbility -> {
                        val granterController = state.projectedState.getController(permanentId)
                            ?: container.get<ControllerComponent>()?.playerId
                            ?: continue
                        val ctx = EffectContext(sourceId = permanentId, controllerId = granterController)
                        if (!conditionEvaluator.evaluate(state, rawAbility.condition, ctx)) continue
                        rawAbility.ability
                    }
                    else -> rawAbility
                }
                // "[receivedBy] have all activated abilities of the [cardFilter] cards exiled with/to
                // craft this / in your graveyard" (Territory Forge / Locus of Enlightenment /
                // Thranduil = Self; Agatha's Soul Cauldron = creatures you control with a +1/+1
                // counter). Mirror CastPermissionUtils.getStaticGrantedAbilitiesWithGranter: grant each
                // donor ability (per `ability.donors`) to every matching permanent, recording the
                // *receiver* as the granter so `{T}`/self-references bind to the permanent that gained
                // the ability. When `oncePerTurnEach` is set (Locus), the util re-stamps each ability
                // with a donor-derived AbilityId + once-per-turn cap.
                if (ability is com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfCards) {
                    val receives = when (val scope = ability.receivedBy.scope) {
                        is Scope.Self -> permanentId == entityId
                        is Scope.Specific -> scope.entityId == entityId
                        is Scope.AttachedTo -> container.get<AttachedToComponent>()?.targetId == entityId
                        is Scope.SoulbondPair ->
                            com.wingedsheep.engine.mechanics.SoulbondPairing.isInPairOf(state, permanentId, entityId)
                        is Scope.Battlefield -> {
                            if (ability.receivedBy.excludeSelf && permanentId == entityId) false
                            else {
                                val granterController = state.projectedState.getController(permanentId)
                                granterController != null && predicateEvaluator.matches(
                                    state, state.projectedState, entityId, ability.receivedBy.baseFilter,
                                    PredicateContext(controllerId = granterController, sourceId = permanentId)
                                )
                            }
                        }
                    }
                    if (receives) {
                        for (granted in com.wingedsheep.engine.legalactions.utils.donorCardsActivatedAbilities(
                            state, permanentId, cardRegistry, predicateEvaluator,
                            ability.donors, ability.cardFilter, ability.oncePerTurnEach
                        )) {
                            result.add(granted to entityId)
                        }
                    }
                    continue
                }
                // "This permanent has all activated and triggered abilities of the last chosen card
                // exiled with it" (Koh, the Face Stealer). Self-scoped: only the source receives the
                // chosen card's *activated* abilities here (triggered ones flow through
                // TriggerAbilityResolver), with the source recorded as granter so `{T}`/self-references
                // bind to it.
                if (ability is com.wingedsheep.sdk.scripting.HasAbilitiesOfChosenLinkedExiledCard) {
                    if (ability.grantActivated && permanentId == entityId) {
                        for (granted in com.wingedsheep.engine.legalactions.utils.chosenLinkedExiledActivatedAbilities(state, permanentId, cardRegistry)) {
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
                    // Soulbond payoff (CR 702.95b) — must mirror the enumerator's SoulbondPair
                    // branch in CastPermissionUtils exactly, or the ability shows as a button on the
                    // paired creature and then fails legality when clicked.
                    is Scope.SoulbondPair -> {
                        if (com.wingedsheep.engine.mechanics.SoulbondPairing.isInPairOf(state, permanentId, entityId)) {
                            result.add(ability.ability to permanentId)
                        }
                    }
                    is Scope.Specific -> {
                        if ((ability.filter.scope as Scope.Specific).entityId == entityId) {
                            result.add(ability.ability to permanentId)
                        }
                    }
                }
            }
        }

        // Granted GrantActivatedAbility statics (CR 611): a permanent that was itself *granted* an
        // ability-granting static — e.g. Roar of the Fifth People chapter II. Resolved by the shared
        // helper so this handler and the enumerators agree on the granted set.
        castPermissionUtils.getGrantedStaticGrantActivatedAbilities(entityId, state)
            .forEach { result.add(it.ability to it.granterId) }

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
                services.castPermissionUtils,
                services.manaAbilitySideEffectExecutor
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
     * Pull the [AbilityCost.ExileXFromGraveyard] sub-cost out of an ability cost, or null if none.
     * Used by the legal-actions submission path to bind X (Winter, Cursed Rider — X with no `{X}`
     * mana symbol) and to pause for *which* graveyard cards the player exiles.
     */
    private fun extractExileXFromGraveyardCost(cost: AbilityCost): AbilityCost.ExileXFromGraveyard? =
        when (cost) {
            is AbilityCost.ExileXFromGraveyard -> cost
            is AbilityCost.Composite -> cost.costs.filterIsInstance<AbilityCost.ExileXFromGraveyard>().firstOrNull()
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

    /**
     * Pull the [CostAtom.VariablePermanents] variable-count sub-cost out of an ability cost, or null if
     * none. Drives the two-step activation flow for "Exile one or more other [filter] you control
     * with total mana value X" costs (Fabrication Foundry): the handler pauses to let the player pick
     * which permanents to exile, then — because the target's legality depends on the resulting X —
     * pauses again for the target choice.
     */
    private fun extractVariablePermanentsCost(cost: AbilityCost): CostAtom.VariablePermanents? = when (cost) {
        is AbilityCost.Atom -> cost.atom as? CostAtom.VariablePermanents
        is AbilityCost.Composite -> cost.costs.firstNotNullOfOrNull {
            (it as? AbilityCost.Atom)?.atom as? CostAtom.VariablePermanents
        }
        else -> null
    }

    /**
     * The ability's X value for a [CostAtom.VariablePermanents] cost, measured from the permanents
     * the payer chose (CR 601.2b — a variable defined by a cost choice is announced at activation).
     * Read at target validation and stored on the stack for resolution re-validation and
     * `DynamicAmount.XValue`.
     *
     * Delegates to the shared [com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost.measure]
     * so the activated-ability path, the cast path, and the enumerators all measure a selection the
     * same way.
     */
    private fun variableCostX(
        state: GameState,
        atom: CostAtom.VariablePermanents,
        chosenIds: List<EntityId>,
    ): Int = com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost
        .measure(state, atom.xMeasure, chosenIds)
}
