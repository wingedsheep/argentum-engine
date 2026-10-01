package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * "You may reveal this card from your opening hand. If you do, [effect]." (CR 103.6b). The
 * reusable opening-hand reveal action behind Devourer of Destiny and the New Phyrexia
 * Chancellor cycle — not a card-specific hook.
 *
 * The engine offers the reveal in the same post-mulligan walk as [mayBeginGameOnBattlefield]
 * (starting player first, then each other player in turn order). A "yes" reveals the card to
 * every player and runs [effect] with this card as its source and its owner as controller; the
 * card stays in hand. [effect] is normally a delayed trigger, since "if you do, at the beginning
 * of …" is one (CR 603.7a):
 *
 * ```
 * revealFromOpeningHand(
 *     Effects.CreateDelayedTrigger(
 *         step = Step.UPKEEP,
 *         fireOnPlayer = EffectTarget.PlayerRef(Player.You),   // "your first upkeep"
 *         effect = …,
 *     )
 * )
 * ```
 *
 * Omit `fireOnPlayer` for "at the beginning of **the** first upkeep" (whoever's turn it is).
 */
fun CardBuilder.revealFromOpeningHand(effect: Effect) {
    openingHandReveal = effect
}
