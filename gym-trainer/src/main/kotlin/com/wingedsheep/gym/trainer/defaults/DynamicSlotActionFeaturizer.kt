package com.wingedsheep.gym.trainer.defaults

import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.gym.trainer.spi.ActionFeaturizer
import com.wingedsheep.gym.trainer.spi.PolicyHead
import com.wingedsheep.gym.trainer.spi.SlotEncoding
import com.wingedsheep.gym.trainer.spi.TrainerContext

/**
 * Simplest action encoder: a single head named `"actions"` with a large
 * slot space, where each call returns a slot computed from the action's
 * identity hash.
 *
 * Because the slot is derived from `action.hashCode()` modulo head size,
 * different actions can collide — fine for the *default* path (masked
 * policy heads), not fine for serious training runs. Projects that care
 * about slot stability should supply their own [ActionFeaturizer].
 *
 * @param headSize how many slots the policy head exposes; pick a power of
 *                 two large enough to make collisions rare for the expected
 *                 action count
 */
class DynamicSlotActionFeaturizer(
    headSize: Int = 1024
) : ActionFeaturizer {

    override val heads: List<PolicyHead> = listOf(PolicyHead(HEAD, headSize))

    private val modulus = headSize

    override fun slot(action: GameAction, ctx: TrainerContext): SlotEncoding {
        val raw = action.hashCode()
        val slot = ((raw % modulus) + modulus) % modulus
        return SlotEncoding(HEAD, slot)
    }

    companion object { const val HEAD: String = "actions" }
}
