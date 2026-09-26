package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.handlers.CostHandler
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.handlers.costs.GraveyardTotalExileResolver
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils.toEntityId
import com.wingedsheep.engine.legalactions.utils.CastPermissionUtils
import com.wingedsheep.engine.legality.LegalityKernel
import com.wingedsheep.engine.mechanics.SplitSecond
import com.wingedsheep.engine.mechanics.SummoningSicknessRules
import com.wingedsheep.engine.mechanics.cost.VariablePermanentsCost
import com.wingedsheep.engine.mechanics.mana.AlternativePaymentHandler
import com.wingedsheep.engine.mechanics.mana.IntrinsicManaAbilities
import com.wingedsheep.engine.mechanics.mana.ManaPaymentWindow
import com.wingedsheep.engine.mechanics.mana.ManaPool
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.mechanics.mana.buildAbilityPaymentContext
import com.wingedsheep.engine.mechanics.targeting.TargetValidator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.player.CantActivateLoyaltyAbilitiesComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.manaCostOrNull
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.targets.TargetChooser
import com.wingedsheep.sdk.scripting.targets.TargetRequirement

/**
 * The authoritative legality check for an [ActivateAbility] (CR 602.5 and the activation
 * procedure's legality checks), split into one function per legality question. Each returns the
 * player-facing reason the activation is illegal, or `null` when that question doesn't forbid it.
 * [validate] asks them in the order the handler always has, so the first failing question's reason
 * is the one reported.
 */
internal class ActivationValidator(
    private val cardRegistry: CardRegistry,
    private val turnManager: TurnManager,
    private val costHandler: CostHandler,
    private val manaSolver: ManaSolver,
    private val alternativePaymentHandler: AlternativePaymentHandler,
    private val targetValidator: TargetValidator,
    private val castPermissionUtils: CastPermissionUtils,
    private val abilityResolver: ActivatedAbilityResolver,
    private val costTotaller: ActivationCostTotaller,
    private val legality: LegalityKernel,
    private val predicateEvaluator: PredicateEvaluator
) {

    fun validate(state: GameState, action: ActivateAbility): String? {
        rejectClientResumeFlag(action)?.let { return it }
        // CR 605.3a — a mana ability may also be activated "whenever a rule or effect asks for a
        // mana payment". While such a window is open the paying player holds no priority, so defer
        // the priority verdict until the ability is known to be a mana ability (checked below).
        val manaPaymentWindow = ManaPaymentWindow.openFor(state, action.playerId)
        if (!state.hasPriority(action.playerId) && manaPaymentWindow == null) {
            return "You don't have priority"
        }

        val container = state.getEntity(action.sourceId)
            ?: return "Source not found: ${action.sourceId}"

        val cardComponent = container.get<CardComponent>()
            ?: return "Source is not a card"

        // Tokens (and other entities without a registered CardDefinition) can still have abilities
        // via static grants (e.g., Brightcap Badger granting "{T}: Add {G}" to Saproling tokens),
        // emblems, intrinsic mana abilities (basic-land subtypes), or temporary grants. Don't bail
        // out before the provenance-aware lookup has checked those sources.
        // Keep lookup provenance: only definition-owned results can later form AbilityIdentity.
        val abilityLookup = abilityResolver.lookup(state, action.sourceId, action.abilityId)
            ?: return "Ability not found on this card"
        val ability = abilityLookup.ability

        checkManaPaymentWindow(state, action, ability, manaPaymentWindow)?.let { return it }
        checkSplitSecond(state, ability)?.let { return it }
        checkPowerUpActivation(state, ability)?.let { return it }
        checkSourceZoneAndController(state, action, container, cardComponent, ability)?.let { return it }

        // Apply text-changing effects to cost and target filters
        val textReplacement = com.wingedsheep.engine.state.components.identity.TextChanges.of(state, action.sourceId)
        val effectiveCost = costTotaller.totalForValidation(state, action, ability, textReplacement)
        val effectiveTargetReqs = if (textReplacement != null) {
            ability.targetRequirements.map { it.applyTextReplacement(textReplacement) }
        } else {
            ability.targetRequirements
        }

        return checkBatchActivation(action, effectiveCost, effectiveTargetReqs)
            ?: checkSubmittedTotalExileSelection(state, action, effectiveCost)
            ?: checkLoyaltyAbilityTiming(state, action, container, ability)
            ?: checkSorcerySpeedTiming(state, action, ability)
            ?: checkAttachedCreatureSummoningSickness(state, container, effectiveCost)
            ?: checkExplicitPaymentSources(state, action)
            ?: checkAlternativePayment(state, action, ability)
            ?: checkCostPayable(state, action, cardComponent, abilityLookup, effectiveCost)
            ?: checkTapSymbolSummoningSickness(state, action, container, effectiveCost)
            ?: checkActivationRestrictions(state, action, ability)
            ?: checkTargets(state, action, cardComponent, effectiveCost, effectiveTargetReqs)
            ?: checkDamageDistribution(action, ability)
    }

    /**
     * `opponentTargetsChosen` is an internal resume marker for "… of an opponent's choice"
     * targets (Cuombajj Witches). Only the engine's resumer sets it, and the resumer re-enters
     * via execute() directly — never through validate() — so any action carrying it here came
     * from a player/client. Reject it: otherwise a client could set it to skip the
     * opponent-target pause and resolve the opponent-chosen damage with no target. See
     * [com.wingedsheep.sdk.scripting.targets.TargetChooser].
     */
    private fun rejectClientResumeFlag(action: ActivateAbility): String? =
        if (action.opponentTargetsChosen) "Internal resume flag cannot be set by a player" else null

    /**
     * Split second (CR 702.61a): while a spell with it is on the stack, only mana abilities may be
     * activated.
     */
    private fun checkSplitSecond(state: GameState, ability: ActivatedAbility): String? =
        if (!ability.isManaAbility && SplitSecond.isLocked(state, cardRegistry)) SplitSecond.REJECTION else null

    /**
     * The mana-payment window (CR 605.3a) opens the door for mana abilities only — everything
     * else still needs priority.
     */
    private fun checkManaPaymentWindow(
        state: GameState,
        action: ActivateAbility,
        ability: ActivatedAbility,
        manaPaymentWindow: SelectManaSourcesDecision?,
    ): String? =
        if (manaPaymentWindow != null && !state.hasPriority(action.playerId) && !ability.isManaAbility) {
            "Only mana abilities can be activated while paying a cost"
        } else null

    /**
     * "During that turn, power-up abilities can't be activated" (Kang the Conqueror). Global and
     * zone-independent, so it is checked before the battlefield/other-zone split.
     */
    private fun checkPowerUpActivation(state: GameState, ability: ActivatedAbility): String? =
        if (castPermissionUtils.isPowerUpActivationRestricted(state, ability)) {
            "Power-up abilities can't be activated this turn"
        } else null

    /**
     * Check that the card is in the correct zone for this ability, and that [action]'s player may
     * activate it there.
     */
    private fun checkSourceZoneAndController(
        state: GameState,
        action: ActivateAbility,
        container: ComponentContainer,
        cardComponent: CardComponent,
        ability: ActivatedAbility,
    ): String? {
        if (ability.activateFromZone != Zone.BATTLEFIELD) {
            val ownerId = container.get<OwnerComponent>()?.playerId ?: return "Card has no owner"
            val inZone = state.getZone(ownerId, ability.activateFromZone).contains(action.sourceId)
            if (!inZone) return "This ability can only be activated from the ${ability.activateFromZone.name.lowercase()}"
            if (ownerId != action.playerId) return "You don't own this card"
            // An unqualified "players can't activate abilities" (Yuriko, Blade of the Mighty)
            // reaches abilities of cards in every zone, not just permanents.
            if (castPermissionUtils.isActivationPreventedForPlayer(
                    state, action.sourceId, action.playerId, abilityIsManaAbility = ability.isManaAbility
                )
            ) {
                return "An effect prevents you from activating that ability right now"
            }
            return null
        }
        return checkBattlefieldController(state, action, container, ability)
            ?: checkBattlefieldAbilityAvailable(state, action, container, cardComponent, ability)
    }

    /**
     * Check if any player may activate this ability (e.g., Lethal Vapors). Recursive
     * through `All`, because the permission is routinely *narrowed* by a companion
     * restriction rather than standing alone — Merseine's "only the controller of the
     * enchanted creature may activate this ability" is AnyPlayerMay + a condition.
     */
    private fun checkBattlefieldController(
        state: GameState,
        action: ActivateAbility,
        container: ComponentContainer,
        ability: ActivatedAbility,
    ): String? {
        val anyPlayerMay = LegalityKernel.anyPlayerMay(ability)
        if (anyPlayerMay) return null
        // Use projected controller to account for control-changing effects (e.g., Annex)
        val projected = state.projectedState
        val controller = projected.getController(action.sourceId)
            ?: container.get<ControllerComponent>()?.playerId
        return if (controller != action.playerId) "You don't control this permanent" else null
    }

    /**
     * Whether the permanent can use this ability at all: face-down, activation-preventing statics,
     * and lost-all-abilities.
     */
    private fun checkBattlefieldAbilityAvailable(
        state: GameState,
        action: ActivateAbility,
        container: ComponentContainer,
        cardComponent: CardComponent,
        ability: ActivatedAbility,
    ): String? {
        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
        val classLevel = container.get<ClassLevelComponent>()?.currentLevel

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
        if (castPermissionUtils.isActivationPreventedForPlayer(
                state, action.sourceId, action.playerId, abilityIsManaAbility = ability.isManaAbility
            )
        ) {
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
        return null
    }

    /**
     * Station-style multi-select batch (CR 702.184a): repeatCount > 1 over a tap-permanents
     * cost means "queue one activation per chosen creature". Validate the batch is well-formed
     * so a malformed action can't, e.g., tap one creature for three activations or reuse the
     * same creature twice. Per-creature legality (untapped/controlled/filter) is re-checked at
     * payment time in CostHandler.payTapPermanents for every slice.
     */
    private fun checkBatchActivation(
        action: ActivateAbility,
        effectiveCost: AbilityCost,
        effectiveTargetReqs: List<TargetRequirement>,
    ): String? {
        if (action.repeatCount <= 1) return null
        val tapAtom = effectiveCost.firstTapPermanentsAtomOrNull() ?: return null
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
        return null
    }

    /**
     * A client-supplied selection for a sum-gated graveyard exile cost is rejected outright when
     * it doesn't pay: every `GameAction` field is client-supplied, and silently substituting the
     * engine's own pick would exile cards the player never chose. An *empty* selection is not an
     * error — that is the AI / engine-direct path asking the resolver to choose.
     */
    private fun checkSubmittedTotalExileSelection(
        state: GameState,
        action: ActivateAbility,
        effectiveCost: AbilityCost,
    ): String? {
        val atom = effectiveCost.firstExileForTotalAtomOrNull() ?: return null
        val submitted = action.costPayment?.exiledCards ?: emptyList()
        if (submitted.isEmpty()) return null
        val resolver = GraveyardTotalExileResolver
        val candidates = resolver.candidates(
            state, action.playerId, atom.measure, atom.filter,
            predicateEvaluator = predicateEvaluator
        )
        return if (!resolver.isLegalSelection(candidates, atom.minTotal, submitted)) {
            "Those cards don't pay this cost: ${atom.description}"
        } else null
    }

    /** Check timing for planeswalker abilities. */
    private fun checkLoyaltyAbilityTiming(
        state: GameState,
        action: ActivateAbility,
        container: ComponentContainer,
        ability: ActivatedAbility,
    ): String? {
        if (!ability.isPlaneswalkerAbility) return null
        // Revel in Silence etc.: "can't activate planeswalkers' loyalty abilities this turn"
        if (state.getEntity(action.playerId)?.has<CantActivateLoyaltyAbilitiesComponent>() == true) {
            return "You can't activate loyalty abilities this turn"
        }
        // CR 606.3 — sorcery timing, unless an instant-speed grant (Jace's Machinations) covers
        // this planeswalker. Priority itself is already required to activate at all.
        if (!turnManager.canPlaySorcerySpeed(state, action.playerId) &&
            !castPermissionUtils.canActivateLoyaltyAtInstantSpeed(state, action.playerId, action.sourceId)
        ) {
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
        return null
    }

    /**
     * Check timing for sorcery-speed abilities ("Activate only as a sorcery").
     * Equip abilities are exempt while the controller has an active instant-speed-equip
     * permission (Forge Anew, Leonin Shikari) — CR 702.6e timing lifted. Mirror of the
     * ActivatedAbilityEnumerator gate so the validate() path agrees with what's offered.
     */
    private fun checkSorcerySpeedTiming(
        state: GameState,
        action: ActivateAbility,
        ability: ActivatedAbility,
    ): String? {
        if (ability.timing != TimingRule.SorcerySpeed || ability.isPlaneswalkerAbility) return null
        val instantSpeedEquip = ability.isEquipAbility && castPermissionUtils.canEquipAtInstantSpeed(state, action.playerId)
        return if (!instantSpeedEquip && !turnManager.canPlaySorcerySpeed(state, action.playerId)) {
            "This ability can only be activated as a sorcery"
        } else null
    }

    /**
     * Check summoning sickness for TapAttachedCreature cost (before general cost check
     * to give a specific error message). Read creature-ness and haste from projected
     * state so a Vehicle / animated land currently being a creature is gated correctly.
     */
    private fun checkAttachedCreatureSummoningSickness(
        state: GameState,
        container: ComponentContainer,
        effectiveCost: AbilityCost,
    ): String? {
        if (!effectiveCost.hasTapAttachedCreatureCost()) return null
        val attachedId = container.get<AttachedToComponent>()?.targetId ?: return null
        val attachedContainer = state.getEntity(attachedId)
        return if (attachedContainer != null && state.projectedState.isCreature(attachedId) &&
            SummoningSicknessRules.blocksTapOrUntapCost(
                attachedId, attachedContainer, state.projectedState
            )
        ) {
            "Enchanted creature has summoning sickness"
        } else null
    }

    /** Validate explicit payment sources. */
    private fun checkExplicitPaymentSources(state: GameState, action: ActivateAbility): String? {
        if (action.paymentStrategy !is PaymentStrategy.Explicit) return null
        for (sourceId in action.paymentStrategy.manaAbilitiesToActivate) {
            val sourceContainer = state.getEntity(sourceId)
                ?: return "Mana source not found: $sourceId"
            if (sourceContainer.has<TappedComponent>()) {
                return "Mana source is already tapped: $sourceId"
            }
        }
        return null
    }

    /**
     * An alternative payment is only worth what the engine says it is: reject a choice the
     * ability can't use (no convoke/waterbend, a tapped or foreign permanent, a colour the
     * creature isn't) before it is priced in [checkCostPayable].
     */
    private fun checkAlternativePayment(
        state: GameState,
        action: ActivateAbility,
        ability: ActivatedAbility,
    ): String? {
        val payment = action.alternativePayment?.takeUnless { it.isEmpty } ?: return null
        return alternativePaymentHandler.validateForAbility(
            state, payment, action.playerId, ability.hasConvoke, ability.hasWaterbend
        )
    }

    /**
     * Check cost requirements (using ManaSolver for mana costs to consider untapped sources).
     * If the ability has convoke or waterbend and the player provided alternative payment,
     * account for the reduced cost.
     */
    private fun checkCostPayable(
        state: GameState,
        action: ActivateAbility,
        cardComponent: CardComponent,
        abilityLookup: ActivatedAbilityLookup,
        effectiveCost: AbilityCost,
    ): String? {
        if (action.paymentStrategy is PaymentStrategy.Explicit) return null
        val ability = abilityLookup.ability
        val costAfterConvokeReduction = if ((ability.hasConvoke || ability.hasWaterbend) && action.alternativePayment != null && !action.alternativePayment.isEmpty) {
            val mc = effectiveCost.extractManaCost() ?: effectiveCost
            if (mc is ManaCost || effectiveCost.manaCostOrNull != null || effectiveCost is AbilityCost.Composite) {
                val reducedManaCost = effectiveCost.extractManaCost()?.let {
                    var reduced = it
                    if (ability.hasConvoke) reduced = alternativePaymentHandler.calculateReducedCostForAbility(reduced, action.alternativePayment)
                    if (ability.hasWaterbend) reduced = alternativePaymentHandler.calculateReducedCostForWaterbend(reduced, action.alternativePayment)
                    reduced
                }
                if (reducedManaCost != null) effectiveCost.withManaPortion(reducedManaCost) else effectiveCost
            } else effectiveCost
        } else effectiveCost

        val abilityPaymentContext = buildAbilityPaymentContext(cardComponent, state.projectedState, action.sourceId, ability)

        // The granter of a statically-granted ability, so AbilityCost.TapGrantingPermanent can be
        // checked against the *Equipment's* tap state rather than the host creature's.
        val validationGranterId = abilityLookup.staticGranterId

        if (canPayAbilityCostWithSources(state, costAfterConvokeReduction, action.sourceId, action.playerId, abilityPaymentContext, validationGranterId)) {
            return null
        }
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

    /**
     * Check summoning sickness for tap/untap abilities. CR 302.6 restricts a *creature's*
     * activated ability whose cost includes the tap symbol **or the untap symbol** — read
     * creature-ness and haste from projected state so a Vehicle or animated permanent that
     * became a creature this turn is gated correctly. Gating on `isCreature` alone (no
     * `!typeLine.isLand` carve-out) is already correct for plain lands — a land that isn't
     * also a creature never satisfies `isCreature`, so this is a no-op for every ordinary
     * land's mana ability — and it's the only way to catch a land that *is* also a creature
     * (Dryad Arbor: "This land ... is affected by summoning sickness"), which the old
     * land-wide carve-out silently exempted.
     *
     * `ActivatedAbilityEnumerator` mirrors this for `AbilityCost.Untap` both bare and inside a
     * `Composite`, so the two agree on `{Q}` in either shape and the enumerator never offers an
     * activation this re-check then rejects. This one stays authoritative regardless.
     */
    private fun checkTapSymbolSummoningSickness(
        state: GameState,
        action: ActivateAbility,
        container: ComponentContainer,
        effectiveCost: AbilityCost,
    ): String? {
        if (!effectiveCost.touchesTapSymbol()) return null
        return if (state.projectedState.isCreature(action.sourceId) &&
            SummoningSicknessRules.blocksTapOrUntapCost(action.sourceId, container, state.projectedState)
        ) {
            "This creature has summoning sickness"
        } else null
    }

    /** Check activation restrictions, through the shared [LegalityKernel]. */
    private fun checkActivationRestrictions(
        state: GameState,
        action: ActivateAbility,
        ability: ActivatedAbility,
    ): String? = legality.activationRestrictionsFailure(state, action.playerId, action.sourceId, ability)

    /**
     * Validate targets. Only the controller-chosen requirements are validated here — any
     * "… of an opponent's choice" requirement (Cuombajj Witches) is picked by an opponent in
     * a separate decision the handler raises at announcement, so it isn't on `action.targets`
     * yet at submission time (the opponent's pick is validated when it's made). See
     * [com.wingedsheep.sdk.scripting.targets.TargetChooser].
     */
    private fun checkTargets(
        state: GameState,
        action: ActivateAbility,
        cardComponent: CardComponent,
        effectiveCost: AbilityCost,
        effectiveTargetReqs: List<TargetRequirement>,
    ): String? {
        val controllerTargetReqs = effectiveTargetReqs.filter { it.chooser == TargetChooser.Controller }
        // For a variable-count "exile/sacrifice one or more permanents you control" cost, X is
        // defined by the payer's cost choice (CR 601.2b) — the chosen set's total mana value
        // (Fabrication Foundry, whose reanimation target's "mana value X or less" legality is
        // measured against it) or simply how many were chosen (Radiant Lotus). Derive X here so
        // target validation sees the right cap; when the cost is paid via the two-step pause flow
        // the chosen set already rides on the action's costPayment.
        val variablePermanentsCost = effectiveCost.extractVariablePermanentsCost()
        val chosenForCost = action.costPayment?.variableCostPermanents ?: emptyList()
        val effectiveXValue = if (variablePermanentsCost != null && chosenForCost.isNotEmpty()) {
            VariablePermanentsCost.measure(state, variablePermanentsCost.xMeasure, chosenForCost)
        } else {
            action.xValue
        }
        if (controllerTargetReqs.isNotEmpty() && action.targets.isNotEmpty()) {
            return targetValidator.validateTargets(
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
        return null
    }

    /**
     * Validate a client-supplied divided-damage division (Chandra, Flameshaper's −4). The
     * division is chosen as the ability is activated (CR 601.2d), so it arrives on the action
     * rather than being asked for at resolution. Absence is legal — the executor then raises a
     * resolution-time DistributeDecision, which is how non-interactive controllers divide — but
     * anything present must be a well-formed division of exactly the printed total.
     */
    private fun checkDamageDistribution(action: ActivateAbility, ability: ActivatedAbility): String? {
        val distribution = action.damageDistribution ?: return null
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
        return null
    }

    /**
     * Check if an ability cost can be paid, using ManaSolver for mana costs
     * to consider both floating mana and untapped mana sources.
     */
    private fun canPayAbilityCostWithSources(
        state: GameState,
        cost: AbilityCost,
        sourceId: EntityId,
        playerId: EntityId,
        abilityContext: SpellPaymentContext? = null,
        granterId: EntityId? = null,
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
                val excludeSources = if (cost.hasTapCost()) setOf(sourceId) else emptySet()
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
}
