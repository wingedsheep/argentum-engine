package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Translate an [EffectTarget] to a [Player] reference for use in pipeline effects.
 *
 * Deliberately **not** total: an unmapped [EffectTarget] throws rather than falling back to
 * [Player.You]. The fallback this replaced was a silent one, and it shipped a wrong card —
 * Mesmeric Orb passed [EffectTarget.ControllerOfTriggeringEntity] and milled the Orb's own
 * controller on every untap instead of the untapping permanent's. A pipeline keyed to the wrong
 * player looks identical to a correct one in the golden snapshot, so nothing downstream can catch
 * it; failing here, at card-construction time, is the only place it *is* catchable.
 *
 * Every caller passes a compile-time-known target, so this throws during `CardDiscovery`/snapshot
 * construction — never mid-game.
 */
internal fun effectTargetToPlayer(target: EffectTarget): Player = when (target) {
    EffectTarget.Controller -> Player.You
    is EffectTarget.ContextTarget -> Player.ContextPlayer(target.index)
    is EffectTarget.BoundVariable -> Player.BoundVariable(target.name, target.description)
    is EffectTarget.PlayerRef -> target.player
    EffectTarget.ControllerOfTriggeringEntity -> Player.ControllerOfTriggeringEntity
    else -> error(
        "No Player reference for EffectTarget $target. Pipeline effects (mill, exileTop, the " +
            "Hand patterns) are keyed by Player, so a target with no mapping would silently " +
            "become 'you'. Add the mapping here rather than letting it default."
    )
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
        // "Defending player discards three cards" (Mindstab Thrull): the discard is the defending
        // player's own choice from their own hand, so the chooser follows the acting player rather
        // than defaulting to the ability's controller — who is, by definition, the attacker.
        is Player.DefendingPlayer -> Chooser.DefendingPlayer
        else -> Chooser.Controller
    }
    else -> Chooser.Controller
}
