package com.wingedsheep.engine.state.components.stack

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.wingedsheep.sdk.dsl.sneak

/**
 * Marks an entity as a spell on the stack.
 */
@Serializable
data class SpellOnStackComponent(
    val casterId: EntityId,
    val xValue: Int? = null,  // For X spells
    val wasKicked: Boolean = false,  // For kicker costs
    val wasBlightPaid: Boolean = false,  // For BlightOrPay additional cost — true if blight path was taken
    val chosenModes: List<Int> = emptyList(),  // For modal spells (700.2). Ordered; same index may repeat when allowRepeat.
    val modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),  // Per-mode chosen targets, aligned 1:1 with chosenModes
    val modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),  // Per-mode TargetRequirements for 608.2b re-validation at resolution
    val modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap(),  // Per-mode DividedDamageEffect allocations (future)
    /** Snapshots of permanents sacrificed as additional cost (Rule 112.7a — last known info). */
    val sacrificedPermanents: List<PermanentSnapshot> = emptyList(),
    val castFaceDown: Boolean = false,  // For morph - creature enters face-down
    val damageDistribution: Map<EntityId, Int>? = null,  // For DividedDamageEffect - pre-chosen damage allocation
    val chosenCreatureType: String? = null,  // For spells that choose a creature type during casting (e.g., Aphetto Dredging)
    val exiledCardCount: Int = 0,  // For variable exile additional costs (e.g., Chill Haunting)
    val additionalCostBlightAmount: Int = 0,  // For variable blight additional costs (e.g., Soul Immolation)
    val castFromZone: Zone? = null,  // Zone the spell was cast from (e.g., HAND for normal casting)
    val wasWarped: Boolean = false,  // For warp - permanent is exiled at end step
    val wasEvoked: Boolean = false,  // For evoke - permanent is sacrificed on ETB
    val wasImpending: Boolean = false,  // For impending - permanent enters with time counters and isn't a creature until they're gone
    /** For sneak (CR 702.190) - permanent spell enters tapped and attacking; the flag is readable via SneakCostWasPaid. */
    val wasSneaked: Boolean = false,
    /**
     * For sneak (CR 702.190b): the player/planeswalker the returned unblocked attacker was
     * attacking. A permanent spell whose sneak cost was paid enters attacking this same
     * defender. Null when not cast for sneak. If the defender is no longer legal at
     * resolution, the resolver enters the creature not attacking (CR 506.3c) — no redirect.
     */
    val sneakAttackDefenderId: EntityId? = null,
    val beheldCards: List<EntityId> = emptyList(),  // Cards chosen via Behold (stored in pipeline as named collection)
    /**
     * Last-known-info snapshots (Rule 112.7a) for entities chosen at cost-pay time
     * that may later leave the battlefield before the spell resolves. Populated
     * when an [com.wingedsheep.sdk.scripting.AdditionalCost.ChooseEntity] step
     * has `captureSnapshot = true` — freezes the chosen entity's projected
     * power / toughness / subtypes / controller so downstream effects (e.g.
     * `DynamicAmount.EntityProperty(EntityReference.FromCostStorage(…), …)`)
     * can read "values as they last existed on the battlefield" at resolution.
     */
    val chosenEntitySnapshots: List<PermanentSnapshot> = emptyList(),
    val manaSpentWhite: Int = 0,  // Mana colors spent for mana-spent-gated triggers
    val manaSpentBlue: Int = 0,
    val manaSpentBlack: Int = 0,
    val manaSpentRed: Int = 0,
    val manaSpentGreen: Int = 0,
    val manaSpentColorless: Int = 0,
    /**
     * Per-color mana spent on the `{X}` portion of this spell, for a color-restricted X
     * (e.g. Soul Burn's "spend only black and/or red mana on X"). Read at resolution via
     * `DynamicAmount.ManaSpentOnX`. Empty when X was unrestricted or the spell has no X.
     */
    val manaSpentOnXByColor: Map<Color, Int> = emptyMap(),
    /**
     * For split-layout cards (CR 709), the index of the face that was cast into
     * [com.wingedsheep.sdk.model.CardDefinition.cardFaces]. Threaded from
     * [com.wingedsheep.engine.core.CastSpell.faceIndex] so the resolution-time handler
     * can attach a [com.wingedsheep.engine.state.components.identity.RoomComponent] with
     * the correct face unlocked. `null` for normal single-face cards.
     */
    val faceIndex: Int? = null,
    /**
     * Names of the "as you cast this spell" condition captures (CR 601.2i) whose condition was true
     * the moment this spell finished being cast. Frozen here so the resolving effect can branch on
     * the cast-time board via [com.wingedsheep.sdk.scripting.conditions.CastTimeFlagSet] even after
     * the board has changed (e.g. Steer Clear's "if you controlled a Mount as you cast this spell").
     * Declared on the card via the `captureAtCast` DSL.
     */
    val castTimeFlags: Set<String> = emptySet()
) : Component

/**
 * Marks an entity as a triggered ability on the stack.
 */
@Serializable
data class TriggeredAbilityOnStackComponent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val effect: Effect,
    val description: String,
    /**
     * Definition-scoped identity of the ability that put this object on the stack, shared by every
     * copy of the same card and every future instance of it. Drives batch decisions and persistent
     * yields (see [com.wingedsheep.sdk.scripting.AbilityIdentity]). Null for synthesized sources
     * (e.g. spell copies on a fresh entity) that have no card definition behind them.
     */
    val abilityIdentity: com.wingedsheep.sdk.scripting.AbilityIdentity? = null,
    /** Optional human-readable description from `TriggeredAbility.descriptionOverride`,
     *  used when displaying the ability on the stack instead of the auto-generated effect text. */
    val descriptionOverride: String? = null,
    val triggerDamageAmount: Int? = null,
    val triggeringEntityId: EntityId? = null,
    val triggeringPlayerId: EntityId? = null,
    val xValue: Int? = null,
    val triggerCounterCount: Int? = null,
    val triggerTotalCounterCount: Int? = null,
    /** Last-known counter map (counter-type-string → count) of the trigger's source on leave-battlefield. */
    val triggerLastKnownCounters: Map<String, Int>? = null,
    /** Per-player damage dealt to the trigger's source this turn, captured at LTB time (Grothama). */
    val triggerLastKnownDamageDealtByPlayers: Map<EntityId, Int>? = null,
    /** Creatures blocking/blocked by the trigger's source on leave-battlefield (CR 509 LKI, Abu Ja'far). */
    val triggerLastKnownBlockingOrBlockedByIds: List<EntityId>? = null,
    val targetingSourceEntityId: EntityId? = null,  // The spell/ability that targeted this permanent (for ward)
    val damageDistribution: Map<EntityId, Int>? = null,  // For DividedDamageEffect - pre-chosen damage allocation
    val copyIndex: Int? = null,    // Which copy number this is (1, 2, 3...) for storm/copy effects
    val copyTotal: Int? = null,    // Total number of copies being created
    val lastKnownPower: Int? = null,    // Power at the moment the triggering entity left the battlefield (dies/leaves)
    val lastKnownToughness: Int? = null, // Toughness at the moment the triggering entity left the battlefield (dies/leaves)
    /** Number of mode picks recorded by the spell-cast that fired this trigger (Riku of Many Paths). */
    val triggerModesChosenCount: Int? = null,
    /** Power of the aura/equipment's attached creature, captured at trigger time; LKI for
     *  "enchanted creature ... its power" reads when the creature has left (CR 608.2h). */
    val enchantedCreatureLastKnownPower: Int? = null,
    /** Cards looked at by the scry that fired this trigger (CR 701.18). Null for non-scry triggers. */
    val triggerScryCount: Int? = null,
    /** Damage past lethal dealt to the trigger's creature recipient (CR 120.4a). Null for non-damage triggers. */
    val triggerExcessDamageAmount: Int? = null,
    /** Total mana spent to cast the spell that fired this trigger (Aberrant Manawurm, Expressive
     *  Firedancer). Read via `ContextPropertyKey.MANA_SPENT_ON_TRIGGERING_SPELL`. Null for non-cast triggers. */
    val triggerManaSpentOnTriggeringSpell: Int? = null,
    // Modal fields — populated when this triggered ability is a copy of a modal spell (700.2g).
    // Copies inherit the original's chosen modes; targets either inherit too (StormCopy default)
    // or are re-chosen by the copy controller while modes stay fixed.
    val chosenModes: List<Int> = emptyList(),
    val modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),
    val modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),
    val modeDamageDistribution: Map<Int, Map<EntityId, Int>> = emptyMap()
) : Component {
    val hasTargets: Boolean = false  // Will be updated based on effect
}

/**
 * Marks an entity as an activated ability on the stack.
 */
@Serializable
data class ActivatedAbilityOnStackComponent(
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val effect: Effect,
    /** Snapshots of permanents sacrificed as additional cost (Rule 112.7a — last known info). */
    val sacrificedPermanents: List<PermanentSnapshot> = emptyList(),
    val xValue: Int? = null,
    val tappedPermanents: List<EntityId> = emptyList(),
    /** LKI snapshots for [tappedPermanents] — see [sacrificedPermanents]. */
    val tappedPermanentSnapshots: List<PermanentSnapshot> = emptyList(),
    /** Optional human-readable description from `ActivatedAbility.descriptionOverride`,
     *  used when displaying the ability on the stack instead of the auto-generated effect text. */
    val descriptionOverride: String? = null,
    /**
     * Definition-scoped identity of the activated ability, shared by every copy of the same card
     * and every future instance of it. Drives batch decisions and persistent yields (see
     * [com.wingedsheep.sdk.scripting.AbilityIdentity]). Null for synthesized abilities with no
     * stable [com.wingedsheep.sdk.scripting.AbilityId] behind them (e.g. crew/saddle).
     */
    val abilityIdentity: com.wingedsheep.sdk.scripting.AbilityIdentity? = null
) : Component {
    val hasTargets: Boolean = false  // Will be updated based on effect
}

/**
 * Legacy ability on stack component (for backwards compatibility).
 */
@Serializable
data class AbilityOnStackComponent(
    val sourceId: EntityId,
    val controllerId: EntityId,
    val abilityId: AbilityId,
    val effect: Effect
) : Component

/**
 * Targets chosen for a spell or ability.
 *
 * @property targets The chosen targets
 * @property targetRequirements The original target requirements, used for re-validation
 *           on resolution (Rule 608.2b — targets must still be legal when the spell/ability resolves)
 */
@Serializable
data class TargetsComponent(
    val targets: List<ChosenTarget>,
    val targetRequirements: List<TargetRequirement> = emptyList()
) : Component

/**
 * Represents a chosen target for a spell or ability.
 */
@Serializable
sealed interface ChosenTarget {
    @Serializable
    @SerialName("Player")
    data class Player(val playerId: EntityId) : ChosenTarget

    @Serializable
    @SerialName("Permanent")
    data class Permanent(val entityId: EntityId) : ChosenTarget

    @Serializable
    @SerialName("Card")
    data class Card(
        val cardId: EntityId,
        val ownerId: EntityId,
        val zone: Zone
    ) : ChosenTarget

    @Serializable
    @SerialName("Spell")
    data class Spell(val spellEntityId: EntityId) : ChosenTarget
}

/**
 * Additional context for spell resolution.
 */
@Serializable
data class SpellContextComponent(
    val additionalData: Map<String, String> = emptyMap()
) : Component

/**
 * Marks a spell or ability on the stack as having been granted extra keywords (by enum name)
 * while it remains on the stack. Used by effects like Spinerock Tyrant that grant wither to
 * a copied or original spell. The component disappears with the spell entity when it leaves
 * the stack.
 */
@Serializable
data class SpellGrantedKeywordsComponent(
    val keywords: Set<String> = emptySet()
) : Component
