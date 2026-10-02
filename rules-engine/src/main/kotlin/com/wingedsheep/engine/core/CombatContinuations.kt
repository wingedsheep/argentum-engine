package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * Resume combat damage assignment for creatures with DivideCombatDamageFreely.
 *
 * @property attackerId The attacking creature assigning damage
 * @property defendingPlayerId The defending player
 * @property firstStrike Whether this is during the first strike combat damage step
 */
@Serializable
data class DamageAssignmentContinuation(
    val attackerId: EntityId,
    val defendingPlayerId: EntityId,
    val firstStrike: Boolean = false
) : AnswerContinuation

/**
 * Resume a [CombatResolutionDecision] (the bipartite combat-damage board).
 *
 * @property firstStrike Whether this is the first-strike combat damage step.
 * @property pendingChoosers The choosers still to confirm, in CR 510.1c order (attacker-side
 *   editors first, then blocker-side). The head is the current chooser; the resumer filters
 *   the response to edges they own and re-pauses for the next chooser until the queue empties.
 *   For the two-actor banding case this carries both players (CR 702.22j + 702.22k).
 * The paired [CombatResolutionDecision] supplies represented edges and their ownership to the
 * resumer; this answer payload retains only the work needed after those choices are submitted.
 */
@Serializable
data class CombatResolutionContinuation(
    val firstStrike: Boolean,
    val pendingChoosers: List<EntityId>,
) : AnswerContinuation

/**
 * Resume combat damage after player decides whether to assign damage as though unblocked.
 * Used for creatures with AssignCombatDamageAsUnblocked (e.g. Thorn Elemental).
 *
 * @property attackerId The attacking creature with the ability
 * @property defendingPlayerId The defending player
 * @property firstStrike Whether this is during the first strike combat damage step
 */
@Serializable
data class AssignAsUnblockedContinuation(
    val attackerId: EntityId,
    val defendingPlayerId: EntityId,
    val firstStrike: Boolean = false
) : AnswerContinuation

/**
 * Resume combat damage after the player picks (or declines to pick) a creature for an unblocked
 * attacker with AssignUnblockedCombatDamageToDefendingCreature (Cunning Giant) to assign its
 * combat damage to.
 *
 * @property attackerId The unblocked attacking creature with the ability
 * @property firstStrike Whether this is during the first strike combat damage step
 */
@Serializable
data class AssignUnblockedToCreatureContinuation(
    val attackerId: EntityId,
    val firstStrike: Boolean = false
) : AnswerContinuation

/**
 * Resume after player has distributed damage among targets.
 *
 * Used for effects like Forked Lightning where the player divides damage
 * among multiple targets. The continuation is pushed when there are multiple
 * targets, and the response contains the damage distribution.
 *
 * @property sourceId The spell/ability that is dealing the damage
 * @property controllerId The player who controls the effect
 * @property targets The targets that damage can be distributed among
 */
@Serializable
data class DistributeDamageContinuation(
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val targets: List<EntityId>,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AnswerContinuation

/**
 * Resume after defending player distributes damage prevention among multiple combat damage sources.
 *
 * Per CR 615.7, when a prevention effect can't prevent all simultaneous damage from multiple
 * sources, the affected player chooses how to distribute the prevention.
 *
 * @property recipientId The player/creature receiving damage
 * @property shieldEffectId The floating effect ID of the PreventNextDamage shield
 * @property shieldAmount Total prevention available from the shield
 * @property damageBySource Map of attacker entity ID → raw damage amount
 * @property firstStrike Whether this is during the first strike combat damage step
 */
@Serializable
data class DamagePreventionContinuation(
    val recipientId: EntityId,
    val shieldEffectId: EntityId,
    val shieldAmount: Int,
    val damageBySource: Map<EntityId, Int>,
    val firstStrike: Boolean
) : AnswerContinuation

/**
 * Resume after a player chooses a source of damage for Deflecting Palm-style effects.
 *
 * Creates a floating effect that prevents the next time the chosen source would deal
 * damage to the controller this turn, and deals that much damage to the source's controller.
 *
 * @property controllerId The player who controls the spell
 * @property sourceId The Deflecting Palm card entity (source of reflected damage)
 * @property sourceName Name of the source for display
 */
@Serializable
data class DeflectDamageSourceChoiceContinuation(
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    /** Arbitrary follow-up effect run when the chosen source's damage is prevented (null = pure prevention). */
    val onPrevented: Effect? = null,
    /** When false, the chosen source's damage is not prevented — it still hits, the reaction still fires (Eye for an Eye). */
    val preventDamage: Boolean = true,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AnswerContinuation

/**
 * Continuation for a [com.wingedsheep.sdk.scripting.effects.PreventDamageEffect] over a chosen source.
 *
 * Resume after a player chooses a damage source. Creates a prevention shield
 * on the target that only prevents damage from the chosen source.
 *
 * @property controllerId The player who controls the spell
 * @property targetId The entity receiving the prevention shield
 * @property amount The amount of damage to prevent; null means prevent all damage from the chosen
 *   source for the rest of the turn (Samite Ministration)
 * @property gainLifeFromColors Color enum names whose prevented damage gives the affected player
 *   that much life (only used when [amount] is null)
 * @property sourceId The spell/ability that created this effect
 * @property sourceName Name of the source for display
 */
@Serializable
data class PreventDamageFromChosenSourceContinuation(
    val controllerId: EntityId,
    val targetId: EntityId,
    val amount: Int?,
    val gainLifeFromColors: Set<String> = emptySet(),
    val sourceId: EntityId?,
    val sourceName: String?,
    /**
     * When true and [amount] is null, install a single-instance "prevent the next time that source
     * would deal damage" shield (Circle of Protection family) rather than the all-damage-from-source
     * shield. Ignored when [amount] is non-null.
     */
    val nextInstanceOnly: Boolean = false,
    /**
     * When true and [amount] is null, prevent all damage the chosen source would deal **to
     * anything** for the rest of the turn, rather than shielding a single recipient. This is the
     * `PreventionDirection.FromTarget` reading of a chosen source — "prevent all damage that would
     * be dealt this turn by a source of your choice" with no recipient clause (Mourner's Shield).
     * [targetId] is unused in that case, since the shield is keyed to the source instead.
     */
    val silenceChosenSource: Boolean = false,
    /**
     * When true (with [nextInstanceOnly]), the single-instance shield prevents only *half* the
     * damage, rounded down — Dark Sphere. Ignored otherwise.
     */
    val halvePreventedDamage: Boolean = false,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment(),
) : AnswerContinuation

/**
 * Resume the combat damage step after the controller of an optional damage-redirection shield has
 * answered one "you may have that damage dealt to you instead" question (Blood of the Martyr).
 *
 * The answer is recorded under [choiceKey] and the whole step is re-run: it re-proposes the same
 * assignments, finds this instance already answered, and asks about the next one — so a batch that
 * covers several creatures is settled question by question before any of its damage is dealt
 * (CR 510.2).
 *
 * @property choiceKey Identity of the (shield, damage instance) pair being answered — see
 *   [com.wingedsheep.engine.handlers.effects.damage.OptionalDamageRedirect.choiceKey].
 * @property firstStrike Whether this is the first-strike combat damage step.
 */
@Serializable
data class CombatOptionalRedirectContinuation(
    val choiceKey: String,
    val firstStrike: Boolean = false
) : AnswerContinuation

/**
 * The non-combat counterpart of [CombatOptionalRedirectContinuation]: record the answer, then re-run
 * the damage [effect] that asked.
 *
 * Re-running is safe because the question is raised *before* the effect deals any damage, so nothing
 * has happened yet that the re-run would repeat. Each pass answers one more instance until the effect
 * runs to completion.
 */
@Serializable
data class OptionalRedirectEffectContinuation(
    val choiceKey: String,
    val effect: Effect,
    val effectContext: com.wingedsheep.engine.handlers.EffectContext
) : AnswerContinuation

/** Captured recipients and source choices survive the resolution-time source decision. */
@Serializable
data class RedirectDamageSourceContinuation(
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val protectedId: EntityId,
    val protectedRef: com.wingedsheep.engine.state.ObjectRef?,
    val redirectToId: EntityId,
    val redirectToRef: com.wingedsheep.engine.state.ObjectRef?,
    val duration: com.wingedsheep.sdk.scripting.Duration,
    val choices: List<com.wingedsheep.engine.handlers.effects.combat.ChosenDamageSource>,
    val objectReferences: com.wingedsheep.engine.handlers.ObjectReferenceEnvironment = com.wingedsheep.engine.handlers.ObjectReferenceEnvironment()
) : AnswerContinuation

@Serializable
data class BlockerPilesContinuation(
    val blockingPlayer: EntityId,
    val attackers: List<EntityId>,
) : AnswerContinuation

@Serializable
data class BlockerPileRestrictionChoiceContinuation(
    val blockingPlayer: EntityId,
    val attackers: List<EntityId>,
    val candidates: Map<EntityId, List<EntityId>>,
    val assignmentCount: Int,
) : AnswerContinuation

/** Carries the resolved amount and source object identities across the source-choice decision. */
@Serializable
data class PreventNextDamageLeavingAmountContinuation(
    val context: com.wingedsheep.engine.handlers.EffectContext,
    val targetId: EntityId,
    val amountToLeave: Int,
    val eligibleSource: com.wingedsheep.sdk.scripting.GameObjectFilter,
    val scope: com.wingedsheep.sdk.scripting.effects.PreventionScope,
    val duration: com.wingedsheep.sdk.scripting.Duration,
    val choices: List<com.wingedsheep.engine.handlers.effects.combat.ChosenDamageSource>
) : AnswerContinuation
