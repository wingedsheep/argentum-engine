package com.wingedsheep.engine.core

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Resume after the attacking player selects which mana sources to tap for an attack
 * tax (Propaganda, Ghostly Prison, Windborn Muse, Collective Restraint, etc.).
 *
 * The engine pauses *before* tapping anything and shows a [SelectManaSourcesDecision]
 * with the auto-pay suggestion pre-selected, mirroring the cast / counter-unless-pays
 * UX. On confirm the resumer taps the selected sources (or runs `ManaSolver.solve` when
 * `autoPay = true`) and commits the attack declaration. An empty manual selection
 * (`autoPay = false` + `selectedSources = []`) is treated as "cancel attack" — a clean
 * no-op that leaves the player in `DECLARE_ATTACKERS`.
 *
 * @property attackingPlayer Player who declared the attack.
 * @property attackers Original [attacker → defender] map from the [DeclareAttackers] action.
 * @property manaCost Total tax to pay, encoded as a generic-mana cost.
 * @property availableSources Mana sources the player chooses from in the prompt.
 * @property autoPaySuggestion Pre-computed entity IDs the solver would tap on auto-pay.
 * @property bands Validated band groupings (CR 702.22) carried across the tax pause so the
 *   resumer can stamp band ids when it commits the attack.
 */
@Serializable
data class AttackTaxManaSelectionContinuation(
    override val decisionId: String,
    val attackingPlayer: EntityId,
    val attackers: Map<EntityId, EntityId>,
    val manaCost: ManaCost,
    val availableSources: List<ManaSourceOption>,
    val autoPaySuggestion: List<EntityId>,
    val bands: List<Set<EntityId>> = emptyList(),
) : ContinuationFrame

/**
 * Resume after the attacking player chooses which permanents to sacrifice to declare an attacker
 * carrying [com.wingedsheep.sdk.scripting.CantAttackUnlessSacrifice] (Leviathan). One frame per
 * paying attacker: [remaining] holds the attackers still owing a sacrifice, so a board with two
 * such creatures asks twice and each choice is made knowing the previous one.
 *
 * Declining is not offered. Affordability was checked before the declaration was accepted, and a
 * player who no longer wants to pay should not have declared the attack — the same contract the
 * generic-mana attack tax has, except that tax *can* be declined because its own pause happens
 * before anything is committed.
 *
 * @property attackingPlayer Player who declared the attack and pays the cost.
 * @property attackers The full declared [attacker → defender] map, replayed on commit.
 * @property payingAttacker The attacker whose cost this decision pays.
 * @property count How many permanents must be sacrificed for [payingAttacker].
 * @property remaining Attackers after this one that still owe a sacrifice, with their counts.
 * @property bands Validated band groupings, carried through to the commit.
 */
@Serializable
data class AttackSacrificeSelectionContinuation(
    override val decisionId: String,
    val attackingPlayer: EntityId,
    val attackers: Map<EntityId, EntityId>,
    val payingAttacker: EntityId,
    val count: Int,
    val remaining: List<PendingAttackSacrifice> = emptyList(),
    val bands: List<Set<EntityId>> = emptyList(),
) : ContinuationFrame

/** One still-unpaid sacrifice cost in an [AttackSacrificeSelectionContinuation]'s queue. */
@Serializable
data class PendingAttackSacrifice(
    val attackerId: EntityId,
    val count: Int,
)

/**
 * Block-tax mirror of [AttackTaxManaSelectionContinuation]. Used for per-creature-type
 * block taxes (Whipgrass Entangler's `AttackBlockTaxPerCreatureType`).
 *
 * @property blockingPlayer Player who declared the blocks.
 * @property blockers Original [blocker → attackers it blocks] map from [DeclareBlockers].
 */
@Serializable
data class BlockTaxManaSelectionContinuation(
    override val decisionId: String,
    val blockingPlayer: EntityId,
    val blockers: Map<EntityId, List<EntityId>>,
    val manaCost: ManaCost,
    val availableSources: List<ManaSourceOption>,
    val autoPaySuggestion: List<EntityId>,
) : ContinuationFrame
