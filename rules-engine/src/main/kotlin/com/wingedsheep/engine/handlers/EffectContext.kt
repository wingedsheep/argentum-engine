package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.PermanentSnapshot
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Context for effect execution.
 *
 * Core fields (sourceId, controllerId, targets, etc.) are always relevant.
 * Pipeline-specific state (collections, named values, iteration targets) lives
 * in [pipeline] to keep the two concerns separate and make pipeline extensions
 * self-contained.
 */
@Serializable
data class EffectContext(
    // --- Core ---
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val targets: List<ChosenTarget> = emptyList(),
    /**
     * The X chosen for an X-cost spell/ability. Also reused by `ChooseNumberThenEffect` to
     * carry a "choose a number" value into the inner effect (read via `CardPredicate.ManaValueEqualsX`,
     * Void). These two uses share one slot, so a future card that both pays `{X}` *and* chooses a
     * number would collide here — split the slot before authoring such a card.
     */
    val xValue: Int? = null,
    /**
     * Total mana paid from the pool to cast this spell — sum of every `manaSpent{Color}`
     * bucket on the [com.wingedsheep.engine.state.components.stack.SpellOnStackComponent].
     * For `{X}` spells the X portion is already included in those buckets, so this is
     * not the same as [xValue]. Used by `DynamicAmount.TotalManaSpent`.
     */
    val totalManaSpent: Int = 0,
    /**
     * Per-color mana spent on the `{X}` portion of the spell or activated ability, for a
     * color-restricted X (e.g. Soul Burn's "spend only black and/or red mana on X").
     * Read by `DynamicAmount.ManaSpentOnX`. Empty when X was unrestricted.
     */
    val manaSpentOnXByColor: Map<Color, Int> = emptyMap(),
    val wasKicked: Boolean = false,
    /** True if the spell's optional Blight additional cost was paid (BlightOrPay path chosen). */
    val wasBlightPaid: Boolean = false,
    // --- Cast-time state ---
    /**
     * Projected snapshots of permanents sacrificed as part of the cost (Rule 112.7a /
     * 608.2h — "as it last existed on the battlefield"). Captured before the zone change
     * so downstream effects can read power, toughness, and subtypes after the permanent
     * has left the battlefield.
     */
    val sacrificedPermanents: List<PermanentSnapshot> = emptyList(),
    /** Pre-chosen damage distribution for DividedDamageEffect spells (target ID -> damage amount) */
    val damageDistribution: Map<EntityId, Int>? = null,
    /**
     * Pre-chosen modes for modal spells/abilities (700.2). Populated at stack resolution
     * from either [SpellOnStackComponent] or [TriggeredAbilityOnStackComponent]. When
     * non-empty, [ModalEffectExecutor] iterates these modes with per-mode targets from
     * [modeTargetsOrdered] instead of prompting for a mode decision.
     */
    val chosenModes: List<Int> = emptyList(),
    val modeTargetsOrdered: List<List<ChosenTarget>> = emptyList(),
    val modeTargetRequirements: Map<Int, List<TargetRequirement>> = emptyMap(),
    /** Number of cards exiled as an additional cost (for ExileVariableCards) */
    val exiledCardCount: Int = 0,
    /** X chosen for [com.wingedsheep.sdk.scripting.AdditionalCost.BlightVariable] */
    val additionalCostBlightAmount: Int = 0,
    /** Permanents tapped as part of an activated ability's cost (e.g., Cryptic Gateway) */
    val tappedPermanents: List<EntityId> = emptyList(),
    /** LKI snapshots for [tappedPermanents] (Rule 112.7a). See [PermanentSnapshot]. */
    val tappedPermanentSnapshots: List<PermanentSnapshot> = emptyList(),
    /**
     * LKI snapshots (Rule 112.7a) for entities chosen via an additional cost
     * step like [com.wingedsheep.sdk.scripting.AdditionalCost.ChooseEntity]
     * with `captureSnapshot = true`. Indexed by entity id via
     * [com.wingedsheep.engine.state.components.stack.snapshotFor]. Read by
     * [DynamicAmountEvaluator] when the `EntityProperty` path resolves an
     * [com.wingedsheep.sdk.scripting.values.EntityReference.FromCostStorage].
     */
    val chosenEntitySnapshots: List<PermanentSnapshot> = emptyList(),
    // --- Trigger state ---
    /** Amount of damage from a trigger context (e.g., "Whenever ~ is dealt damage") */
    val triggerDamageAmount: Int? = null,
    /**
     * Counter count from the triggering event payload.
     * - Death triggers: last-known +1/+1 counter count when the source left the battlefield (Hooded Hydra).
     * - CountersPlacedEvent triggers: number of counters placed in the triggering event (Simic Ascendancy).
     */
    val triggerCounterCount: Int? = null,
    /** Last known total counter count (all types) from a death trigger context (e.g., Shadow Urchin) */
    val triggerTotalCounterCount: Int? = null,
    /** Last known -1/-1 counter count from a death trigger context (e.g., Retched Wretch) */
    val triggerMinusOneMinusOneCounterCount: Int? = null,
    /** The entity that caused the trigger to fire (e.g., creature that dealt damage for Aurification) */
    val triggeringEntityId: EntityId? = null,
    /** The player associated with the trigger event (e.g., the player who cast a spell for SpellCastEvent) */
    val triggeringPlayerId: EntityId? = null,
    /** The spell or ability that targeted a permanent (for ward triggers) */
    val targetingSourceEntityId: EntityId? = null,
    /** Power of the triggering entity the moment it left the battlefield (dies/leaves triggers) */
    val triggerLastKnownPower: Int? = null,
    /** Toughness of the triggering entity the moment it left the battlefield (dies/leaves triggers) */
    val triggerLastKnownToughness: Int? = null,
    /**
     * Power of the creature an Aura/Equipment was attached to, captured when its triggered
     * ability fired. Read by [EntityReference.EnchantedCreature] power reads as last-known
     * information (CR 608.2g) when the attached creature — and the aura — have left the
     * battlefield before the ability resolves (e.g. the creature is removed in response to
     * the aura's enters-the-battlefield trigger). Null for non-attached sources.
     */
    val enchantedCreatureLastKnownPower: Int? = null,
    /**
     * Last-known counter map (counter-type-string → count) of the trigger's source the
     * moment it left the battlefield. Read by `MoveAllLastKnownCountersEffect` when a
     * dies/leaves trigger needs to put every counter — not just +1/+1 — onto another
     * permanent (e.g., Essence Channeler).
     */
    val triggerLastKnownCounters: Map<String, Int>? = null,
    /**
     * Per-player damage dealt to the trigger's source the moment it left the battlefield.
     * Read by Grothama's LTB effect: "each player draws cards equal to the damage dealt
     * to ~ this turn by sources they controlled."
     */
    val triggerLastKnownDamageDealtByPlayers: Map<EntityId, Int>? = null,
    /**
     * Number of mode picks the triggering spell-cast recorded. Read by
     * `ContextPropertyKey.MODES_CHOSEN_ON_TRIGGERING_SPELL` (Riku of Many Paths).
     */
    val triggerModesChosenCount: Int? = null,
    // --- Choice state ---
    /** Color chosen for "add one mana of any color" abilities */
    val manaColorChoice: Color? = null,
    /**
     * Color chosen during a [com.wingedsheep.sdk.scripting.effects.ChooseColorThenEffect]
     * resolution. Set by the resumer before dispatching the inner effect; read by atomic
     * "...FromChosenColor" executors.
     */
    val chosenColor: Color? = null,
    /** Creature type chosen during casting (e.g., Aphetto Dredging) */
    val chosenCreatureType: String? = null,
    // --- Zone state ---
    /** Zone the spell was cast from (e.g., HAND, GRAVEYARD for flashback) */
    val castFromZone: Zone? = null,
    // --- Projection state ---
    /** The entity being modified during continuous effect projection (for DynamicAmount evaluation) */
    val affectedEntityId: EntityId? = null,
    // --- Pipeline state ---
    val pipeline: PipelineState = PipelineState.EMPTY
) {
    /**
     * Resolve a symbolic effect target to a concrete entity id using just the context.
     *
     * Stateless resolution — handles self, controller, context targets, bound variables,
     * pipeline targets, triggering entity, etc. For relational references that need to look
     * up components (e.g., [EffectTarget.EnchantedCreature], [EffectTarget.TargetController]),
     * use the overload that also takes [GameState].
     */
    fun resolveTarget(target: EffectTarget): EntityId? =
        TargetResolutionUtils.resolveTarget(target, this)

    /**
     * Resolve a symbolic effect target to a concrete entity id, consulting [state] for
     * relational references (attachments, controllers, owners).
     */
    fun resolveTarget(target: EffectTarget, state: GameState): EntityId? =
        TargetResolutionUtils.resolveTarget(target, this, state)

    /**
     * Resolve a symbolic effect target and throw if it cannot be resolved.
     * Use when the caller knows by construction that the target must exist.
     */
    fun requireTarget(target: EffectTarget): EntityId =
        resolveTarget(target) ?: error("Cannot resolve target: $target")

    /**
     * Resolve a symbolic effect target using [state] and throw if it cannot be resolved.
     */
    fun requireTarget(target: EffectTarget, state: GameState): EntityId =
        resolveTarget(target, state) ?: error("Cannot resolve target: $target")

    /**
     * Resolve a player reference target (e.g., "target player", "each opponent") to a
     * single player entity id. Stateless overload — see [resolvePlayerTargets] for
     * multi-player results.
     */
    fun resolvePlayerTarget(target: EffectTarget): EntityId? =
        TargetResolutionUtils.resolvePlayerTarget(target, this)

    /**
     * Resolve a player reference target, consulting [state] for relational references
     * like [com.wingedsheep.sdk.scripting.references.Player.OwnerOf] /
     * [com.wingedsheep.sdk.scripting.references.Player.ControllerOf].
     */
    fun resolvePlayerTarget(target: EffectTarget, state: GameState): EntityId? =
        TargetResolutionUtils.resolvePlayerTarget(target, this, state)

    /**
     * Resolve a player reference target to a list of player ids (for multi-player effects
     * like "each player" / "each opponent").
     */
    fun resolvePlayerTargets(target: EffectTarget, state: GameState): List<EntityId> =
        TargetResolutionUtils.resolvePlayerTargets(target, state, this)

    companion object {
        /**
         * Build a named targets map from target requirements and chosen targets.
         *
         * For each requirement with a non-null `id`:
         * - If count == 1: maps `id` -> chosenTarget
         * - If count > 1: maps `id[0]` -> target0, `id[1]` -> target1, etc.
         *
         * Requirements with `id == null` are skipped (backward compat with ContextTarget).
         */
        fun buildNamedTargets(
            requirements: List<TargetRequirement>,
            targets: List<ChosenTarget>
        ): Map<String, ChosenTarget> {
            val result = mutableMapOf<String, ChosenTarget>()
            var targetIndex = 0
            for (req in requirements) {
                val id = req.id
                if (id != null) {
                    if (req.count == 1) {
                        targets.getOrNull(targetIndex)?.let { result[id] = it }
                    } else {
                        for (i in 0 until req.count) {
                            targets.getOrNull(targetIndex + i)?.let { result["$id[$i]"] = it }
                        }
                    }
                }
                targetIndex += req.count
            }
            return result
        }
    }
}
