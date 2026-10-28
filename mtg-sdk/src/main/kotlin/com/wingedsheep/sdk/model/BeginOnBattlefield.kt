package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * "If this card is in your opening hand, you may begin the game with it on the battlefield"
 * (CR 103.6a) — the opening-hand action behind the Leyline cycles, and, with its axes filled in,
 * Gemstone Caverns: "If this card is in your opening hand **and you're not the starting player**,
 * you may begin the game with Gemstone Caverns on the battlefield **with a luck counter on it**.
 * **If you do, exile a card from your hand.**"
 *
 * Carried by [CardScript.beginOnBattlefield]; the engine offers it in the post-mulligan
 * opening-hand walk, beside [CardScript.openingHandReveal] (CR 103.6b).
 *
 * @property onlyIf The action is offered only while this holds, evaluated with the card's owner as
 *   controller before the first turn. While the game is starting, the starting player is
 *   considered the active player (CR 101.4e), so "you're not the starting player" is
 *   `Conditions.IsNotYourTurn`. `null` = always offered.
 * @property counters Counters the card begins the game with, applied as part of its entry.
 * @property then Runs after the card is on the battlefield ("If you do, …"), with the card as
 *   source and its owner as controller. It may ask a question; the opening-hand walk waits for it.
 */
@Serializable
data class BeginOnBattlefield(
    val onlyIf: Condition? = null,
    val counters: Map<CounterType, Int> = emptyMap(),
    val then: Effect? = null,
)
