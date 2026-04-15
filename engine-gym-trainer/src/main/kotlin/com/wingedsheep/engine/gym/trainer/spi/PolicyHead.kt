package com.wingedsheep.engine.gym.trainer.spi

/**
 * Declares a single policy head the user's network exposes.
 *
 * An AlphaZero-for-chess setup has one head; MageZero-style MTG setups
 * typically have four (priority-player, priority-opponent, target,
 * binary). Keeping heads as data means the trainer can validate slot
 * assignments and the built-in PUCT knows how to look up priors.
 *
 * @property name   must match the key [Evaluator] returns in its prior map
 * @property size   logical head width — slot indices must fall in `[0, size)`
 */
data class PolicyHead(val name: String, val size: Int) {
    init {
        require(name.isNotBlank()) { "PolicyHead name must not be blank" }
        require(size > 0) { "PolicyHead size must be positive (got $size)" }
    }
}

/**
 * The (head, slot) pair an [ActionFeaturizer] assigns to a single action.
 *
 * Using a pair instead of a flat int means multi-head networks can share
 * slot indices across heads without collision, and the trainer can validate
 * that the slot falls inside the declared head.
 */
data class SlotEncoding(val head: String, val slot: Int) {
    init {
        require(slot >= 0) { "slot must be non-negative (got $slot)" }
    }
}
