package com.wingedsheep.engine.view

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.core.GameAction
import kotlinx.serialization.Serializable

@Serializable
data class ManaSourceInfo(
    val entityId: EntityId,
    val name: String,
    val imageUri: String? = null,
    val producesColors: List<String> = emptyList(),
    val producesColorless: Boolean = false,
    val manaAmount: Int = 1
)

@Serializable
data class LegalActionTargetInfo(
    val index: Int,
    val description: String,
    val minTargets: Int,
    val maxTargets: Int,
    val validTargets: List<EntityId>,
    val targetZone: String? = null,
    /**
     * True when this target requirement filters by "mana value X or less"
     * (CardPredicate.ManaValueAtMostX). The client must re-filter [validTargets]
     * by the chosen X after X selection — the engine builds [validTargets]
     * permissively because X is unbound at legal-action enumeration time.
     */
    val xConstrainsManaValue: Boolean = false,
    /**
     * True when this target requirement filters by "mana value X" *exactly*
     * (CardPredicate.ManaValueEqualsX). The equality sibling of [xConstrainsManaValue]: the client
     * narrows [validTargets] to cards whose mana value equals the chosen X after X selection
     * (Likeness Looter, Rydia, Summoner of Mist).
     */
    val xConstrainsManaValueExactly: Boolean = false,
    /**
     * True when this target requirement filters by "power X" (CardPredicate.PowerEqualsX).
     * The client must re-filter [validTargets] to creatures whose power equals the chosen X
     * after X selection (Ent-Draught Basin) — the engine builds [validTargets] permissively
     * because X is unbound at legal-action enumeration time.
     */
    val xConstrainsPower: Boolean = false,
    /**
     * True when this requirement's max-count is dynamically driven by the chosen X
     * (`TargetObject.dynamicMaxCount == DynamicAmount.XValue`). The client must
     * cap selectable targets at the chosen X after the cast-time `xSelection` phase.
     */
    val xConstrainsCount: Boolean = false
)

@Serializable
data class LegalActionInfo(
    val actionType: String,
    val description: String,
    val action: GameAction,
    val isAffordable: Boolean = true,
    val validTargets: List<EntityId>? = null,
    val requiresTargets: Boolean = false,
    val targetCount: Int = 1,
    val minTargets: Int = targetCount,
    val targetDescription: String? = null,
    val targetRequirements: List<LegalActionTargetInfo>? = null,
    /**
     * True when the (single) target requirement filters by "mana value X or less".
     * For multi-requirement spells the per-requirement flag on
     * [LegalActionTargetInfo.xConstrainsManaValue] is used instead. The client
     * must re-filter [validTargets] by the chosen X after X selection.
     */
    val xConstrainsTargetManaValue: Boolean = false,
    /**
     * True when the (single) target requirement filters by "mana value X" *exactly*
     * (CardPredicate.ManaValueEqualsX — Likeness Looter). For multi-requirement abilities the
     * per-requirement flag on [LegalActionTargetInfo.xConstrainsManaValueExactly] is used instead.
     * The client narrows [validTargets] to cards whose mana value equals the chosen X.
     */
    val xConstrainsTargetManaValueExactly: Boolean = false,
    /**
     * True when the (single) target requirement filters by "power X" (CardPredicate.PowerEqualsX).
     * The client must re-filter [validTargets] to creatures whose power equals the chosen X
     * after X selection (Ent-Draught Basin). For multi-requirement abilities the per-requirement
     * flag on [LegalActionTargetInfo.xConstrainsPower] is used instead.
     */
    val xConstrainsTargetPower: Boolean = false,
    /**
     * True when the (single) target requirement's max-count is dynamically driven by the
     * chosen X. The client must cap selectable targets at the chosen X after the cast-time
     * `xSelection` phase. For multi-requirement spells the per-requirement flag on
     * [LegalActionTargetInfo.xConstrainsCount] is used instead.
     */
    val xConstrainsTargetCount: Boolean = false,
    val validAttackers: List<EntityId>? = null,
    val mandatoryAttackers: List<EntityId>? = null,
    val validAttackTargets: List<EntityId>? = null,
    val validBlockers: List<EntityId>? = null,
    val hasXCost: Boolean = false,
    val maxAffordableX: Int? = null,
    val minX: Int = 0,
    val isManaAbility: Boolean = false,
    val additionalCostInfo: AdditionalCostInfo? = null,
    val hasConvoke: Boolean = false,
    val validConvokeCreatures: List<ConvokeCreatureInfo>? = null,
    /**
     * Tap-for-generic payment (improvise CR 702.126, waterbend): tap untapped permanents you
     * control, each paying {1} of the generic mana in the cost.
     */
    val hasTapForGeneric: Boolean = false,
    val validTapForGenericPermanents: List<TapForGenericPermanentInfo>? = null,
    /**
     * Tap cap for a spell-level waterbend cost; null when the cap is just the generic in the cost
     * (improvise, ability waterbend, the "waterbend {X}" shape).
     */
    val tapForGenericAmount: Int? = null,
    /** Player-facing verb for the tap payment — `"improvise"` / `"waterbend"`. */
    val tapForGenericLabel: String? = null,
    val hasDelve: Boolean = false,
    val validDelveCards: List<DelveCardInfo>? = null,
    val minDelveNeeded: Int? = null,
    val hasHarmonize: Boolean = false,
    val validHarmonizeCreatures: List<HarmonizeCreatureInfo>? = null,
    val manaCostString: String? = null,
    /**
     * Mana added to this spell's cost per target beyond the first, so [manaCostString] above is
     * only the one-target minimum and the real price is settled by targeting. Mirrors
     * [com.wingedsheep.engine.legalactions.LegalAction.manaCostPerExtraTarget]; the client uses it
     * to run targeting before a manual mana-source pick, and to price that pick.
     */
    val manaCostPerExtraTarget: String? = null,
    /**
     * The cheapest [manaCostString] can end up being once the alternative payments this action
     * already offers are used to the maximum — convoke taps (CR 702.51a), delve exiles (CR 702.66a),
     * waterbend taps, a harmonize tap. Null when nothing can move the cost.
     *
     * For those keywords [manaCostString] is the *pre-reduction* price: the enumerator folds the
     * reduction into affordability but never into the cost it advertises, so a convoke spell shows
     * the one number the player never actually pays. Carrying both lets the client render the real
     * span ("{5}{G} → as low as {G}") instead of just its top end. [AdditionalCostInfo.costAfterSacrifice]
     * does the same job for emerge, per candidate, where the reduction depends on *which* creature
     * is sacrificed rather than on how many resources are spent.
     *
     * A display floor, not a promise: spending every resource is the player's choice, and whatever
     * is left still has to be paid with mana.
     */
    val minimumManaCostString: String? = null,
    val requiresDamageDistribution: Boolean = false,
    val totalDamageToDistribute: Int? = null,
    val minDamagePerTarget: Int? = null,
    val autoTapPreview: List<EntityId>? = null,
    val availableManaSources: List<ManaSourceInfo>? = null,
    /**
     * The player's floating *restricted* ("spend this mana only to …") mana that is eligible to
     * pay for **this** action, one entry per mana unit. Populated alongside
     * [availableManaSources] — i.e. for the surfaces where the client does its own cost math
     * (convoke / waterbend / harmonize / delve selectors, X-cost picker).
     *
     * The client can't judge eligibility itself: the pool payload only carries a human-readable
     * restriction string. Without this, a convoke bar (say) would ignore Ashling, Rimebound's
     * MV4+ mana and grey out a cast the server would happily accept.
     */
    val eligibleRestrictedMana: List<ClientRestrictedManaEntry>? = null,
    val requiresManaColorChoice: Boolean = false,
    /**
     * Restricted color names ("WHITE", "BLUE", ...) when the ability can only produce a
     * subset (Mox Amber, Fellwar Stone, Reflecting Pool). Null = all five colors are valid
     * (Gilded Lotus, Birds of Paradise). The client must hide unproducible colors from the
     * picker. See [LegalAction.availableManaColors].
     */
    val availableManaColors: List<String>? = null,
    val sourceZone: String? = null,
    val blockerMaxBlockCounts: Map<EntityId, Int>? = null,
    val mandatoryBlockerAssignments: Map<EntityId, List<EntityId>>? = null,
    val maxRepeatableActivations: Int? = null,
    val tapForPower: Boolean = false,
    val tapForPowerRequired: Int? = null,
    val tapForPowerCreatures: List<TapForPowerCreatureInfo>? = null,
    val modalEnumeration: ModalLegalEnumerationInfo? = null,
    val holdPriority: Boolean = false
)

/**
 * DTO for a choose-N modal spell's cast-time enumeration payload (rules 700.2).
 *
 * Mirrors [com.wingedsheep.engine.legalactions.ModalLegalEnumeration]; the client
 * uses this to drive the mode/target decision loop.
 */
@Serializable
data class ModalLegalEnumerationInfo(
    val chooseCount: Int,
    val minChooseCount: Int,
    val allowRepeat: Boolean,
    val additionalManaCostPerExtraMode: String? = null,
    /** Non-mana escalate (CR 702.120a): the cost of one extra mode — see
     *  [com.wingedsheep.engine.legalactions.ModalLegalEnumeration.additionalCostPerExtraMode]. */
    val additionalCostPerExtraMode: AdditionalCostInfo? = null,
    val modes: List<ModalEnumerationModeInfo>,
    val unavailableIndices: List<Int>
)

@Serializable
data class ModalEnumerationModeInfo(
    val index: Int,
    val description: String,
    val available: Boolean,
    val additionalManaCost: String? = null,
    val additionalCostInfo: AdditionalCostInfo? = null,
    val targetRequirements: List<LegalActionTargetInfo> = emptyList()
)

@Serializable
data class ConvokeCreatureInfo(
    val entityId: EntityId,
    val name: String,
    val colors: Set<Color>
)

@Serializable
data class TapForGenericPermanentInfo(
    val entityId: EntityId,
    val name: String,
    val isCreature: Boolean
)

@Serializable
data class DelveCardInfo(
    val entityId: EntityId,
    val name: String,
    val imageUri: String? = null
)

@Serializable
data class HarmonizeCreatureInfo(
    val entityId: EntityId,
    val name: String,
    val power: Int
)

@Serializable
data class TapForPowerCreatureInfo(
    val entityId: EntityId,
    val name: String,
    /** What this creature contributes toward the cost — for Crew and Saddle that can exceed its
     * printed power (a "crews as though its power were 2 greater" static), and it is the number the
     * handler charges against, so the client's progress bar must sum this and not power. */
    val power: Int,
    /**
     * Whether this creature could legally attack right now (CR 508.1a per-creature restrictions).
     * Paying with it taps it, which takes it out of combat — the client spends the creatures that
     * couldn't attack anyway first, and flags the ones that could.
     */
    val canAttack: Boolean = true
)

@Serializable
data class AdditionalCostInfo(
    val description: String,
    val costType: String,
    val validSacrificeTargets: List<EntityId> = emptyList(),
    val sacrificeCount: Int = 1,
    /** Emerge (CR 702.119): the mana cost left after sacrificing each candidate — see
     *  [com.wingedsheep.engine.legalactions.AdditionalCostData.costAfterSacrifice]. Empty otherwise. */
    val costAfterSacrifice: Map<EntityId, String> = emptyMap(),
    val validTapTargets: List<EntityId> = emptyList(),
    val tapCount: Int = 0,
    /** Station-style shortcut: when > 1, up to this many single-creature tap activations may be
     *  queued in one gesture (select 1..N distinct creatures, one activation each). 1 = no batch. */
    val tapBatchMaxActivations: Int = 1,
    val validDiscardTargets: List<EntityId> = emptyList(),
    val discardCount: Int = 0,
    val validBounceTargets: List<EntityId> = emptyList(),
    val bounceCount: Int = 0,
    val validExileTargets: List<EntityId> = emptyList(),
    val exileMinCount: Int = 0,
    val exileMaxCount: Int = 0,
    /**
     * Sum gate for a graveyard exile cost measured by a total rather than a count — collect
     * evidence N (CR 701.59a) and `ExileForTotal` alike; see
     * [com.wingedsheep.engine.legalactions.AdditionalCostData.exileMinTotalWeight]. The client sums
     * [exileCardWeights] over its selection, labels the tally with [exileWeightUnit] and enables
     * Confirm at [exileMinTotalWeight]; the server re-validates the submitted selection either way.
     * All three are 0 / empty for every other cost type.
     */
    val exileMinTotalWeight: Int = 0,
    val exileCardWeights: Map<EntityId, Int> = emptyMap(),
    val exileWeightUnit: String = "",
    val validBeholdTargets: List<EntityId> = emptyList(),
    val beholdCount: Int = 0,
    val counterRemovalCreatures: List<CounterRemovalCreatureInfo> = emptyList(),
    val validBlightTargets: List<EntityId> = emptyList(),
    val blightAmount: Int = 0,
    /** For BlightVariable: cap on X (greatest toughness among creatures you control). */
    val blightVariableMaxX: Int = 0,
    /** For PayXLife: cap on X (your current life total). */
    val payXLifeMaxX: Int = 0,
    /** Total counters to remove across creatures for an any-type counter-removal cost. */
    val distributedCounterRemovalTotal: Int = 0,

    /**
     * Combined battlefield + graveyard candidate pool for an `AbilityCost.Craft` sub-cost
     * (CR 702.167a-b). The activator picks [craftMinCount]+ of these to exile alongside the
     * source. The client renders both zones side-by-side; chosen IDs are submitted as
     * `ActivateAbility.costPayment.exiledCards`.
     */
    val validCraftMaterials: List<EntityId> = emptyList(),
    val craftMinCount: Int = 1,
    /** Cap on material count for exact-count crafts ("Craft with artifact"); null = unbounded. */
    val craftMaxCount: Int? = null,
    /**
     * Candidate creatures for a `TapForTotalPower` additional cost (Teamwork N, CR 702.194a) —
     * "tap any number of creatures you control with total power N or more". The count is free;
     * [tapForPowerRequired] is the constraint. Same payload shape as the crew/saddle
     * [LegalActionInfo.tapForPowerCreatures]. Chosen ids go back as
     * `additionalCostPayment.variableCostPermanents`.
     */
    val tapForPowerCreatures: List<TapForPowerCreatureInfo> = emptyList(),
    /** Total projected power the [tapForPowerCreatures] selection must reach. 0 = no such cost. */
    val tapForPowerRequired: Int = 0
)

@Serializable
data class CounterRemovalCreatureInfo(
    val entityId: EntityId,
    val name: String,
    val availableCounters: Int,
    /**
     * Counter-type breakdown so the UI can render per-type +/- rows when a
     * creature carries more than one type. Keys are canonical counter-type
     * symbols (e.g. "+1/+1", "-1/-1", "stun"); sum equals [availableCounters].
     */
    val availableCountersByType: Map<String, Int> = emptyMap(),
    val imageUri: String? = null
)
