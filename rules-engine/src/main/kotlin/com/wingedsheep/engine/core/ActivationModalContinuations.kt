package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import kotlinx.serialization.Serializable

/** Activation choices are collected before payment, so either pause can be cancelled safely. */
@Serializable
data class ActivationModalModeContinuation(
    val action: ActivateAbility,
    val modal: ModalEffect,
    val offeredIndices: List<Int>,
    val selectedIndices: List<Int> = emptyList(),
    val doneOffered: Boolean = false
) : AnswerContinuation

@Serializable
data class ActivationModalTargetContinuation(
    val action: ActivateAbility,
    val modal: ModalEffect,
    val selectedIndices: List<Int>,
    val resolvedTargets: List<List<ChosenTarget>>,
    val ordinal: Int
) : AnswerContinuation
