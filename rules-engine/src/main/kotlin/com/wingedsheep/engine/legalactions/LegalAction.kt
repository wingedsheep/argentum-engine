package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId

/**
 * Engine-level representation of a legal action a player can take.
 *
 * This is the rules engine's output — it carries structured data about the action,
 * NOT presentation/DTO concerns. The game-server enriches this into LegalActionInfo
 * for the client.
 */
data class LegalAction(
    val action: GameAction,
    override val actionType: String,
    val description: String,
    val affordable: Boolean = true,

    // Targeting
    override val validTargets: List<EntityId>? = null,
    override val requiresTargets: Boolean = false,
    /**
     * Maximum number of targets the player may pick for the *first* requirement. Always the
     * resolved cap — i.e. [TargetInfo.maxTargets] of that requirement — never the
     * requirement's static `count`. `count` is only a placeholder for "any number of
     * target ..." (`unlimited`, where the real cap is how many legal targets exist) and for a
     * board-state `dynamicMaxCount`; reading it here silently clamped such spells to a single
     * target, because [targetRequirements] is populated only for multi-requirement actions, so
     * neither the client nor the AI could recover the real maximum.
     *
     * The one exception is an X-driven cap ([xConstrainsTargetCount]): X is unbound at
     * enumeration time, so this stays a placeholder the client replaces with the chosen X.
     */
    val targetCount: Int = 1,
    val minTargets: Int = targetCount,
    val targetDescription: String? = null,
    val targetRequirements: List<TargetInfo>? = null,
    /**
     * True when the (single) target requirement filters by "mana value X or less"
     * (i.e. the requirement's filter contains [CardPredicate.ManaValueAtMostX]).
     *
     * The enumerator builds [validTargets] permissively because X is unbound at
     * enumeration time. The client must re-filter [validTargets] by the chosen X
     * once the player picks it, so it cannot click an over-MV target that the
     * server would later reject. For multi-requirement spells, see
     * [TargetInfo.xConstrainsManaValue] on each requirement instead.
     */
    val xConstrainsTargetManaValue: Boolean = false,
    /**
     * True when the (single) target requirement filters by "mana value X" *exactly* (i.e. the
     * requirement's filter contains [CardPredicate.ManaValueEqualsX] — Likeness Looter, Rydia,
     * Summoner of Mist). The equality sibling of [xConstrainsTargetManaValue]: the enumerator is
     * likewise permissive at X-unbound, and the client narrows [validTargets] to cards whose mana
     * value *equals* the chosen X. For multi-requirement abilities, see
     * [TargetInfo.xConstrainsManaValueExactly].
     */
    val xConstrainsTargetManaValueExactly: Boolean = false,
    /**
     * True when the (single) target requirement filters by "power X" (i.e. the requirement's
     * filter contains [CardPredicate.PowerEqualsX] — Ent-Draught Basin). The enumerator builds
     * [validTargets] permissively because X is unbound at enumeration time; the client must
     * re-filter [validTargets] to creatures whose power equals the chosen X once the player
     * picks it. For multi-requirement abilities, see [TargetInfo.xConstrainsPower].
     */
    val xConstrainsTargetPower: Boolean = false,
    /**
     * True when the (single) target requirement's max-count is dynamically driven by the
     * chosen X (i.e. [TargetObject.dynamicMaxCount] is [DynamicAmount.XValue]).
     *
     * At enumeration time X is unbound, so [targetCount] is a placeholder (the static
     * `count` field, typically 1 or 20 from the legacy workaround). After the player
     * picks X via the cast-time `xSelection` phase, the client must clamp selectable
     * targets to that X. For multi-requirement spells, see [TargetInfo.xConstrainsCount]
     * on each requirement.
     */
    val xConstrainsTargetCount: Boolean = false,

    // Combat
    override val validAttackers: List<EntityId>? = null,
    val mandatoryAttackers: List<EntityId>? = null,
    val validAttackTargets: List<EntityId>? = null,
    override val validBlockers: List<EntityId>? = null,
    val blockerMaxBlockCounts: Map<EntityId, Int>? = null,
    val mandatoryBlockerAssignments: Map<EntityId, List<EntityId>>? = null,

    // Costs
    val manaCostString: String? = null,
    val hasXCost: Boolean = false,
    val maxAffordableX: Int? = null,
    val minX: Int = 0,
    val additionalCostInfo: AdditionalCostData? = null,

    // Convoke / Delve
    val hasConvoke: Boolean = false,
    val convokeCreatures: List<ConvokeCreatureData>? = null,
    val hasDelve: Boolean = false,
    val delveCards: List<DelveCardData>? = null,
    val minDelveNeeded: Int? = null,

    // Tap-for-generic payment — tap untapped permanents you control, each paying {1} of the
    // generic mana in the cost. Shared by Improvise (CR 702.126, artifacts only) and Waterbend
    // (Avatar: The Last Airbender, artifacts or creatures); [tapForGenericLabel] names which.
    val hasTapForGeneric: Boolean = false,
    val tapForGenericPermanents: List<TapForGenericPermanentData>? = null,
    /**
     * For a spell-level waterbend cost, the tap cap N — the number of permanents that may be
     * tapped (one per generic in the waterbend {N}). Null when the cap is simply the generic mana
     * in the cost: an activated-ability waterbend (whose whole cost is the waterbend cost),
     * improvise (CR 702.126a, "for each generic mana in this spell's total cost"), and the
     * "waterbend {X}" shape (the client caps at the chosen xValue).
     */
    val tapForGenericAmount: Int? = null,
    /**
     * The player-facing verb for this tap payment — [com.wingedsheep.engine.mechanics.mana.TapForGeneric.label],
     * e.g. `"improvise"` or `"waterbend"`. Null when [hasTapForGeneric] is false. The client shows
     * it in the tap HUD so the two mechanics don't read as each other.
     */
    val tapForGenericLabel: String? = null,
    /**
     * Whether the tap payment is what makes this cast affordable: `true` when the cost is **not**
     * payable with mana alone, `false` when it is and the taps are purely optional. Null when the
     * enumerator didn't compute it (the waterbend paths, whose taps pay a cost that is mandatory
     * in its own right, so an automatic payer should always fill them).
     *
     * Engine-side only — deliberately not mapped onto `LegalActionInfo`, because the client never
     * fills this payment on its own: a human picks the taps in the HUD. Its one consumer is the
     * built-in AI (`Strategist.withAutomaticTapForGeneric`), which reads `LegalAction` directly.
     *
     * Exists for automatic payers. Filling an *optional* improvise payment can cost you the cast:
     * a tapped artifact stops being a mana source but only credits {1}, so tapping Arc Reactor
     * ({T}: Add {C}{C}{C}) for improvise trades three mana for one and can leave the rest of the
     * cost unpayable — the case the Whir of Invention rulings warn about ("if an artifact you
     * control has a mana ability with {T} … you won't be able to tap it again for improvise").
     * When this is `true`, `CostEnumerationUtils.canAffordWithTapForGeneric` has already validated
     * the configuration where *every* offered permanent is tapped, so filling is safe.
     */
    val tapForGenericRequired: Boolean? = null,

    // Harmonize (cast from graveyard; optionally tap one creature to reduce the generic
    // cost by its power). The client may pick at most one of [harmonizeCreatures].
    val hasHarmonize: Boolean = false,
    val harmonizeCreatures: List<HarmonizeCreatureData>? = null,

    // Mana abilities
    override val isManaAbility: Boolean = false,
    val requiresManaColorChoice: Boolean = false,
    /**
     * Constrained set of colors the ability can produce, when known.
     *
     * Null means the ability accepts all five colors (e.g. plain "Add one mana of any color"
     * — Gilded Lotus, Birds of Paradise). When non-null, the UI must restrict the color
     * picker to these colors (Mox Amber, Fellwar Stone, Reflecting Pool). An empty list
     * means no color is producible right now and the action effectively no-ops if activated.
     */
    val availableManaColors: List<Color>? = null,

    // Auto-tap (engine computes the solution; server enriches with full mana source info)
    val autoTapPreview: List<EntityId>? = null,

    // Damage distribution
    val requiresDamageDistribution: Boolean = false,
    val totalDamageToDistribute: Int? = null,
    val minDamagePerTarget: Int? = null,

    // Source zone
    val sourceZone: String? = null,

    // Tap-creatures-for-total-power selection (shared by Crew N and Saddle N: the player taps
    // any number of eligible creatures whose combined power meets [tapForPowerRequired]).
    val tapForPower: Boolean = false,
    val tapForPowerRequired: Int? = null,
    val tapForPowerCreatures: List<TapForPowerCreatureData>? = null,

    // Repetition
    val maxRepeatableActivations: Int? = null,

    // Forage (graveyard casting with forage cost, applies finality counter)
    val requiresForage: Boolean = false,

    // Additional life cost (e.g., Festival of Embers graveyard casting)
    val additionalLifeCost: Int = 0,

    // Modal cast-time enumeration payload (rules 700.2). Only populated when
    // [actionType] is "CastSpellModal" and the spell has [ModalEffect.chooseCount] > 1.
    // The client uses this to drive the cast-time mode/target decision loop.
    val modalEnumeration: ModalLegalEnumeration? = null,

    // When true, prevents auto-pass whenever this action is available
    override val holdPriority: Boolean = false
) : PriorityAction {
    /** [PriorityAction]'s name for [affordable]. The DTO calls the same thing `isAffordable`. */
    override val isAffordableAction: Boolean get() = affordable

    override val additionalCostType: String? get() = additionalCostInfo?.costType

    override val hasUnfillableTargetRequirement: Boolean
        get() = targetRequirements?.any { it.minTargets > 0 && it.validTargets.isEmpty() } ?: false
}

/**
 * Per-mode enumeration data for a choose-N modal spell.
 *
 * Carries everything the client needs to offer mode selection at cast time:
 * which modes exist, which are unavailable (700.2a — can't target, can't pay
 * additional cost), and the per-mode cost deltas and target requirements.
 *
 * Modes marked `available = false` must not be pickable.
 *
 * @property chooseCount Maximum number of modes the player may pick.
 * @property minChooseCount Minimum number of modes required (< chooseCount for
 *           "choose one or both" / "choose one or more").
 * @property allowRepeat When true, the same mode index may be chosen more than
 *           once (rules 700.2d — Escalate / Spree).
 * @property additionalCostPerExtraMode Non-mana escalate (CR 702.120a — Collective Brutality's
 *           "discard a card"): the cost of **one** extra mode. The client scales it by the number
 *           of modes chosen beyond the first and drives the matching picker. [chooseCount] is
 *           already capped by what the caster can pay, so the picker can never run dry.
 * @property modes One entry per declared mode, in printed order.
 * @property unavailableIndices Convenience list of mode indices flagged
 *           unavailable. Equal to `modes.filterNot { it.available }.map { it.index }`.
 */
data class ModalLegalEnumeration(
    val chooseCount: Int,
    val minChooseCount: Int,
    val allowRepeat: Boolean,
    val additionalManaCostPerExtraMode: String? = null,
    val additionalCostPerExtraMode: AdditionalCostData? = null,
    val modes: List<ModalEnumerationMode>,
    val unavailableIndices: List<Int>
)

/**
 * A single mode offered for cast-time selection on a choose-N modal spell.
 *
 * @property index Printed mode index (0-based).
 * @property description Rendered mode text, e.g. "Target creature gets +3/+3 until end of turn".
 * @property available False when the mode has no legal targets (700.2a) or when
 *           the caster cannot pay this mode's [additionalManaCost] / additional costs.
 * @property additionalManaCost Extra mana this mode adds to the spell's cost, if any
 *           (rendered string form — pure-add deltas such as "{B}").
 * @property additionalCostInfo Per-mode non-mana additional cost info when the mode
 *           overrides the card-level [AdditionalCost]s (rules 700.2h). Null otherwise.
 * @property targetRequirements Target slots for this mode; empty if the mode has none.
 */
data class ModalEnumerationMode(
    val index: Int,
    val description: String,
    val available: Boolean,
    val additionalManaCost: String? = null,
    val additionalCostInfo: AdditionalCostData? = null,
    val targetRequirements: List<TargetInfo> = emptyList()
)

/**
 * Target requirement info for a single target slot.
 */
data class TargetInfo(
    val index: Int,
    val description: String,
    val minTargets: Int,
    val maxTargets: Int,
    val validTargets: List<EntityId>,
    val targetZone: String? = null,
    /** A target in this slot must differ from every target chosen for an earlier slot. */
    val mustDifferFromEarlier: Boolean = false,
    /**
     * True when this requirement's filter contains [CardPredicate.ManaValueAtMostX].
     * The client re-filters [validTargets] by the chosen X after X selection.
     */
    val xConstrainsManaValue: Boolean = false,
    /**
     * True when this requirement's filter contains [CardPredicate.ManaValueEqualsX].
     * The client narrows [validTargets] to cards whose mana value *equals* the chosen X
     * after X selection (Likeness Looter, Rydia, Summoner of Mist).
     */
    val xConstrainsManaValueExactly: Boolean = false,
    /**
     * True when this requirement's filter contains [CardPredicate.PowerEqualsX].
     * The client re-filters [validTargets] to creatures whose power equals the chosen X
     * after X selection (Ent-Draught Basin).
     */
    val xConstrainsPower: Boolean = false,
    /**
     * True when this requirement's max-count is dynamically driven by the chosen X
     * (`TargetObject.dynamicMaxCount == DynamicAmount.XValue`). The client should
     * clamp selectable targets to the chosen X after X selection.
     */
    val xConstrainsCount: Boolean = false
)

/**
 * Information about a creature that can be tapped for Convoke.
 */
data class ConvokeCreatureData(
    val entityId: EntityId,
    val name: String,
    val colors: Set<Color>
)

/**
 * Information about a permanent that can be tapped for a **tap-for-generic** payment (Improvise
 * CR 702.126, Waterbend). Generic-only, so no color is carried (each tapped permanent always pays
 * {1} generic). [isCreature] lets the client distinguish creatures from artifacts for
 * highlight/labelling only.
 */
data class TapForGenericPermanentData(
    val entityId: EntityId,
    val name: String,
    val isCreature: Boolean
)

/**
 * Information about a creature that can be tapped for Harmonize. [power] is the
 * projected power — the amount of generic mana the cost is reduced by if tapped.
 */
data class HarmonizeCreatureData(
    val entityId: EntityId,
    val name: String,
    val power: Int
)

/**
 * Information about a card in graveyard that can be exiled for Delve.
 */
data class DelveCardData(
    val entityId: EntityId,
    val name: String,
    val imageUri: String? = null
)

/**
 * Information about a creature that can be tapped to pay a "tap creatures with total power N"
 * cost — shared by Crew N (crewing a Vehicle) and Saddle N (saddling a Mount).
 */
data class TapForPowerCreatureData(
    val entityId: EntityId,
    val name: String,
    val power: Int
)

/**
 * Information about additional costs for a spell or ability.
 */
data class AdditionalCostData(
    val description: String,
    val costType: String,
    val validSacrificeTargets: List<EntityId> = emptyList(),
    val sacrificeCount: Int = 1,
    /**
     * The spell's remaining mana cost after sacrificing each candidate in [validSacrificeTargets],
     * keyed by that candidate — emerge (CR 702.119), where the emerge cost is reduced by generic
     * mana equal to the sacrificed creature's mana value.
     *
     * Emerge is the only cost whose *mana* half depends on which permanent pays its *non-mana*
     * half, so `LegalAction.manaCostString` alone can't tell the player what a given choice will
     * actually cost, and the client must not re-derive it (the generic-only clamp is a rule, and
     * rules live server-side). The client shows `manaCostString → costAfterSacrifice[candidate]`
     * live as the player picks, and prices manual mana-source selection off the chosen entry.
     *
     * Empty for every other sacrifice cost, where the mana is fixed regardless of the choice.
     */
    val costAfterSacrifice: Map<EntityId, String> = emptyMap(),
    val validTapTargets: List<EntityId> = emptyList(),
    val tapCount: Int = 0,
    /**
     * Station-style multi-select shortcut (CR 702.184a). When > 1, this single-creature
     * (`tapCount == 1`) tap cost belongs to a no-target, stacking activated ability that may be
     * activated several times in one gesture: the player selects 1..[tapBatchMaxActivations]
     * distinct creatures and the engine queues one activation per creature (each taps exactly its
     * creature). 1 means "no batch" — the cost is paid by tapping exactly [tapCount] creatures for
     * a single activation, the prior behaviour.
     */
    val tapBatchMaxActivations: Int = 1,
    val validDiscardTargets: List<EntityId> = emptyList(),
    val discardCount: Int = 0,
    val validBounceTargets: List<EntityId> = emptyList(),
    val bounceCount: Int = 0,
    val validExileTargets: List<EntityId> = emptyList(),
    val exileMinCount: Int = 0,
    val exileMaxCount: Int = 0,
    /**
     * For a collect-evidence cost (CR 701.59a): the **floor on the combined mana value** of the
     * exiled cards. Non-zero only for `costType == "CollectEvidence"`.
     *
     * Its own field because collect evidence is the one exile cost whose constraint is a sum rather
     * than a count — [exileMinCount] / [exileMaxCount] bound the selection at 1 and the whole
     * graveyard, which is all a counted picker can say about it. The client gates its confirm
     * button on the running total reaching this number and shows that total as the player selects.
     * The server re-validates the submitted selection against it regardless
     * ([com.wingedsheep.engine.handlers.costs.CollectEvidenceResolver.isLegalSelection]).
     */
    val exileMinTotalManaValue: Int = 0,
    val validBeholdTargets: List<EntityId> = emptyList(),
    val beholdCount: Int = 0,
    val counterRemovalCreatures: List<CounterRemovalCreatureData> = emptyList(),
    val validBlightTargets: List<EntityId> = emptyList(),
    val blightAmount: Int = 0,
    /**
     * For [AdditionalCost.BlightVariable]: the cap on X — the greatest toughness
     * among creatures the caster controls at cast-enumeration time. The client
     * uses this to bound the X slider (0..blightVariableMaxX).
     */
    val blightVariableMaxX: Int = 0,
    /**
     * For [AdditionalCost.PayXLife]: the cap on X — the caster's current life total at
     * cast-enumeration time. The client uses this to bound the X slider (0..payXLifeMaxX).
     */
    val payXLifeMaxX: Int = 0,
    /**
     * For an any-type [CostAtom.RemoveCounters] additional cost: total counters to
     * remove across all matching permanents.
     */
    val distributedCounterRemovalTotal: Int = 0,

    /**
     * Combined battlefield + graveyard candidate pool for an [AbilityCost.Craft] sub-cost
     * (CR 702.167a-b). The activator selects [craftMinCount]+ of these to exile alongside
     * the source. Battlefield candidates are permanents the activator controls matching the
     * Craft filter; graveyard candidates are cards in their graveyard matching the same filter.
     * The client renders both side-by-side and submits chosen IDs as
     * `ActivateAbility.costPayment.exiledCards`.
     */
    val validCraftMaterials: List<EntityId> = emptyList(),
    val craftMinCount: Int = 1,
    /** Cap on material count for exact-count crafts ("Craft with artifact"); null = unbounded. */
    val craftMaxCount: Int? = null,

    /**
     * Candidate creatures for a `TapForTotalPower` cost — "tap any number of creatures you control
     * with total power N or more" (Teamwork N, CR 702.194a), where the count is free and the
     * *sum of projected power* is the constraint. The same payload crew and saddle advertise
     * through [LegalAction.tapForPowerCreatures], reused here so the client can price a selection
     * without re-deriving power (rules stay server-side).
     *
     * Chosen ids are submitted as `CastSpell.additionalCostPayment.variableCostPermanents`.
     */
    val tapForPowerCreatures: List<TapForPowerCreatureData> = emptyList(),
    /** Total projected power the [tapForPowerCreatures] selection must reach. 0 = no such cost. */
    val tapForPowerRequired: Int = 0
)

/**
 * Information about a creature that has +1/+1 counters available for removal.
 */
data class CounterRemovalCreatureData(
    val entityId: EntityId,
    val name: String,
    val availableCounters: Int,
    /**
     * Counter-type breakdown so the UI can offer a per-type +/- when a creature
     * carries more than one type. Keyed by the canonical counter-type name
     * (e.g. "+1/+1", "-1/-1", "stun"). Sum of values equals [availableCounters].
     */
    val availableCountersByType: Map<String, Int> = emptyMap(),
    val imageUri: String? = null
)
