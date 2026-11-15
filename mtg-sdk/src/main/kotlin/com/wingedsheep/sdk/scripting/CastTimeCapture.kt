package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.Serializable

/**
 * One "as you cast this spell" capture (CR 601.2i).
 *
 * A spell may carry a list of these. The engine evaluates each [value] against the game state the
 * moment the spell finishes being cast (after costs are paid, before it becomes cast) with the caster
 * as the controller, and records the number under [captureName] onto the spell on the stack. The spell's
 * resolving effect then reads the frozen number, so a later board change can't alter it:
 *
 * - a *numeric* capture ("for each modified creature you controlled as you cast this spell",
 *   Lethal Exploit) is read with [com.wingedsheep.sdk.scripting.values.DynamicAmount.CapturedAtCast];
 * - a *condition* capture ("if you controlled a Mount as you cast this spell", Steer Clear) is the
 *   same thing with a 1/0 value — `DynamicAmount.Conditional(condition, 1, 0)` — read with the
 *   `Conditions.CapturedAtCast(flag)` facade (a `Compare` of the captured number against 0).
 *
 * A *permanent* spell's captures ride onto the permanent it becomes, so its enters trigger or
 * enters-with replacement reads them the same way (Orator of Ojutai's "if you revealed a Dragon card
 * or controlled a Dragon as you cast this spell"); a permanent card declares it in a `spell { }` block too.
 * A copy of a spell wasn't cast (CR 707.10), so it reads every capture as absent (0) — the DTK
 * Dragon-bonus ruling: "the copy wasn't cast, so whether you controlled a Dragon won't matter" — and
 * a copied permanent spell's token gets none. The one exception is a capture that is the pool of a
 * division announced at cast (Monstrous Onslaught's "X damage divided as you choose"): the division
 * is a decision the copy inherits, and so is the amount it divides.
 *
 * This is the cast-time sibling of the player-choice [ChoiceSlot] bag: those record what the player
 * *chose* as they cast; this records what the *game state* was as they cast. Declared with the
 * `captureAtCast(name, amount)` / `captureAtCast(flag, condition)` DSL in a card's `spell { }` block.
 *
 * @property captureName The capture name; the same name is read back by `CapturedAtCast`.
 * @property value The amount to evaluate at cast time (e.g. "the number of modified creatures you control").
 */
@Serializable
data class CastTimeCapture(
    val captureName: String,
    val value: DynamicAmount
)
