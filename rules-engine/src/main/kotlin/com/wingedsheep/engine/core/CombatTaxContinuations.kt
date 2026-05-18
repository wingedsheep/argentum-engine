package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Resume after the attacking player decides whether to pay an attack tax (Propaganda,
 * Ghostly Prison, Windborn Muse, Collective Restraint, etc.).
 *
 * The engine pauses *before* tapping mana so the player isn't surprised by sources
 * vanishing from their pool. On `Yes`, the resumer pays the tax (auto-tap is fine after
 * consent) and commits the attack declaration. On `No`, the attack declaration is
 * cancelled and the player remains in `DECLARE_ATTACKERS` to re-declare with different
 * attackers (or pass the step).
 *
 * @property attackingPlayer Player who declared the attack.
 * @property attackers Original [attacker → defender] map from the [DeclareAttackers] action.
 * @property totalTax Total generic-mana tax owed (sum of per-attacker amounts).
 */
@Serializable
data class AttackTaxConfirmationContinuation(
    override val decisionId: String,
    val attackingPlayer: EntityId,
    val attackers: Map<EntityId, EntityId>,
    val totalTax: Int,
) : ContinuationFrame

/**
 * Resume after the blocking player decides whether to pay a block tax (e.g. Whipgrass
 * Entangler's per-creature block tax on a specific creature type).
 *
 * Same shape as [AttackTaxConfirmationContinuation] but for the block step. On `No`, the
 * block declaration is cancelled and the player remains in `DECLARE_BLOCKERS`.
 *
 * @property blockingPlayer Player who declared the blocks.
 * @property blockers Original [blocker → attackers it blocks] map from [DeclareBlockers].
 * @property totalTax Total generic-mana tax owed.
 */
@Serializable
data class BlockTaxConfirmationContinuation(
    override val decisionId: String,
    val blockingPlayer: EntityId,
    val blockers: Map<EntityId, List<EntityId>>,
    val totalTax: Int,
) : ContinuationFrame
