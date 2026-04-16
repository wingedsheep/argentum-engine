package com.wingedsheep.gym.trainer.spi

/**
 * Engine state → feature representation the evaluator consumes.
 *
 * `T` is user-chosen: a sparse `LongArray` of hashed feature indices for a
 * MageZero-style `EmbeddingBag` network, a dense `FloatArray` for a
 * conventional MLP, a `ByteArray` tensor for a CNN, anything.
 *
 * The [TrainerContext] is passed in so the featurizer can branch on
 * decision kind — the same board often wants different features depending
 * on whether the agent is choosing an action, choosing a target, or
 * answering a yes/no.
 */
fun interface StateFeaturizer<T> {
    fun featurize(ctx: TrainerContext): T
}
