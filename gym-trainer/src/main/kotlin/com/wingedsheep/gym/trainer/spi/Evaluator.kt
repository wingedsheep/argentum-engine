package com.wingedsheep.gym.trainer.spi

/**
 * Produces policy priors + a scalar value estimate for a game state.
 *
 * The typical implementation is a thin HTTP client calling an external
 * inference server (PyTorch / JAX / ONNX). A heuristic implementation is
 * also supported — see `HeuristicEvaluator` for a no-NN default.
 */
fun interface Evaluator<T> {

    /**
     * @param features  the output of [StateFeaturizer.featurize] for this state
     * @param legalSlots the `(head, slot)` of every child edge MCTS is about
     *                   to expand — implementations can use this to mask or
     *                   skip priors for unreachable slots
     * @param ctx        the same [TrainerContext] used for featurization
     */
    fun evaluate(
        features: T,
        legalSlots: List<SlotEncoding>,
        ctx: TrainerContext
    ): EvaluationResult
}

/**
 * Policy priors (keyed by head name) plus a scalar value in `[-1, +1]` from
 * the acting player's perspective.
 *
 * Each `priors[headName]` array is indexed by slot — `priors[head][slot]` is
 * the prior probability assigned to the action at `(head, slot)`. Arrays
 * do not need to sum to 1: the trainer normalises over the legal slots
 * before applying them to the MCTS tree.
 */
data class EvaluationResult(
    val priors: Map<String, FloatArray>,
    val value: Float
) {
    init {
        require(value in -1f..1f) { "value must be in [-1, 1] (got $value)" }
    }
}
