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
