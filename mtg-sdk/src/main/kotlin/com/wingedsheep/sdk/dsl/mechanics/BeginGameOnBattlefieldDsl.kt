package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.BeginOnBattlefield
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * "If this card is in your opening hand, you may begin the game with it on the battlefield."
 * (CR 103.6a). This is the reusable ability, not a card cycle: it powers the Leyline
 * enchantment cycles (Future Sight / M11 / M20 / Duskmourn) but also non-"Leyline of X"
 * cards such as Leyline Axe. The engine resolves all such choices after mulligans and
 * bottoming complete, walking each player in turn order starting with the active player.
 *
 * The optional axes cover the printed variations, Gemstone Caverns being all three at once:
 *
 * ```
 * mayBeginGameOnBattlefield(
 *     onlyIf = Conditions.IsNotYourTurn,                // "and you're not the starting player"
 *     counters = mapOf(CounterType.LUCK to 1),          // "with a luck counter on it"
 *     then = Patterns.Hand.exileFromHand(1, EffectTarget.Controller),  // "If you do, exile a card from your hand."
 * )
 * ```
 *
 * @param onlyIf Offered only while this holds — see [BeginOnBattlefield.onlyIf].
 * @param counters Counters the card begins the game with, applied as part of its entry.
 * @param then The "if you do" rider, run once the card is on the battlefield (source = the card,
 *   controller = its owner).
 *
 * A set-mechanic [CardBuilder] extension (SDK plan §2.2): it composes onto the builder via
 * the public `beginOnBattlefield` field and lives here rather than on the core builder.
 */
fun CardBuilder.mayBeginGameOnBattlefield(
    onlyIf: Condition? = null,
    counters: Map<CounterType, Int> = emptyMap(),
    then: Effect? = null,
) {
    beginOnBattlefield = BeginOnBattlefield(onlyIf = onlyIf, counters = counters, then = then)
}
