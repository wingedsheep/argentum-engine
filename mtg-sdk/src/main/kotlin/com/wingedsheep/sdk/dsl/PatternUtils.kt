package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Translate an [EffectTarget] to a [Player] reference for use in pipeline effects.
 */
internal fun effectTargetToPlayer(target: EffectTarget): Player = when (target) {
    EffectTarget.Controller -> Player.You
    is EffectTarget.ContextTarget -> Player.ContextPlayer(target.index)
    is EffectTarget.BoundVariable -> Player.ContextPlayer(0)
    is EffectTarget.PlayerRef -> target.player
    else -> Player.You
}

/**
 * Translate an [EffectTarget] to a [Chooser] for [SelectFromCollectionEffect].
 * The chooser determines which player makes the selection decision.
 */
internal fun effectTargetToChooser(target: EffectTarget): Chooser = when (target) {
    EffectTarget.Controller -> Chooser.Controller
    is EffectTarget.ContextTarget -> Chooser.TargetPlayer
    is EffectTarget.BoundVariable -> Chooser.TargetPlayer
    is EffectTarget.PlayerRef -> when (target.player) {
        is Player.You -> Chooser.Controller
        is Player.TargetOpponent -> Chooser.TargetPlayer
        is Player.AnOpponent, is Player.EachOpponent -> Chooser.Opponent
        is Player.TriggeringPlayer -> Chooser.TriggeringPlayer
        // "Its owner" (e.g. Recoil: "Return target permanent to its owner's hand. Then that
        // player discards a card.") resolves to whoever owns the targeted permanent, which may be
        // the controller or an opponent. The discarding player chooses from their own hand, so
        // derive the chooser from the gathered cards rather than the spell's controller.
        is Player.OwnerOf -> Chooser.ControllerOfSelection
        else -> Chooser.Controller
    }
    else -> Chooser.Controller
}
