package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.references.Player
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Collect evidence [amount] as an **effect** — exile any number of cards from [player]'s graveyard
 * with total mana value [amount] or greater (CR 701.59a).
 *
 * The resolution-time counterpart of the
 * [com.wingedsheep.sdk.scripting.costs.CostAtom.CollectEvidence] *cost*. Both do the same thing to
 * the same cards; they differ only in when and why, so the engine routes both through one payment
 * implementation and both emit the same
 * [com.wingedsheep.engine.core.EvidenceCollectedEvent] that "whenever you collect evidence" payoffs
 * (Surveillance Monitor, Evidence Examiner) trigger from.
 *
 * Designed as the `action` half of a [ReflexiveTriggerEffect] ("you may collect evidence 3. **When
 * you do**, put a +1/+1 counter on target creature you control" — Sample Collector), mirroring how
 * [com.wingedsheep.sdk.scripting.effects.PayFixedCountersEffect] serves the energy shape: the outer
 * "may" *is* the decision to collect, so this effect performs no yes/no of its own — it prompts only
 * for *which* cards. `ReflexiveTriggerEffectExecutor.isActionFeasible` checks reachability before
 * offering the prompt, which is what makes CR 701.59b hold: a player who cannot reach [amount] is
 * never *given* the choice, rather than being offered it and forced to decline.
 *
 * Also usable as a bare gated action — "you may collect evidence 4. **If you do**, create two 2/1
 * Spiders" (Izoni, Center of the Web) — by composing it under the ordinary
 * [com.wingedsheep.sdk.scripting.effects.GatedEffect] / "if you do" machinery rather than a
 * reflexive trigger; the difference matters because a reflexive trigger uses the stack and can be
 * responded to (CR 603.12) while an "if you do" continuation cannot.
 *
 * @property amount The mana-value floor N — the total the exiled cards must meet or exceed.
 * @property player Whose graveyard is spent. Defaults to the effect's controller; Axebane Ferox's
 *   ward makes the *opponent* collect evidence from their own graveyard.
 */
@SerialName("CollectEvidence")
@Serializable
data class CollectEvidenceEffect(
    val amount: Int,
    val player: Player = Player.You
) : Effect {
    override val description: String = "collect evidence $amount"
}

/**
 * Collect evidence **X**, where X is a number [player] chooses as this resolves — "you may collect
 * evidence X. When you do, this creature deals X damage to each creature and planeswalker that
 * player controls" (Incinerator of the Guilty).
 *
 * The sibling of [CollectEvidenceEffect] for the one printed shape whose threshold isn't fixed.
 * Kept as its own effect rather than widening [CollectEvidenceEffect.amount] to a
 * `DynamicAmount`: every other collect-evidence card in the corpus names a literal N, and a
 * *player-chosen* number isn't a `DynamicAmount` at all — it's a decision, and it has to be
 * bounded by what the graveyard can actually pay before it is asked.
 *
 * The choice is a single `ChooseNumberDecision` over `0 .. <total mana value in the graveyard>`.
 * That upper bound is CR 701.59b applied to the *choice* rather than to the payment: a player
 * can't choose an X they couldn't then reach, so the prompt never offers one. The lower bound is 0
 * because collecting evidence 0 is legal and exiles nothing — per the 2024-02-02 ruling it still
 * counts as having collected evidence, so "whenever you collect evidence" payoffs (Surveillance
 * Monitor, Evidence Examiner) trigger off it. That also makes this effect *always* feasible, which
 * is why an enclosing "may" is always offered.
 *
 * X is republished under [storeAmountAs] and read downstream via
 * `DynamicAmount.VariableReference(storeAmountAs)` — the same convention [PayFixedCountersEffect]'s
 * "pay any amount" sibling and `DrawUpToEffect.storeAs` use. Stored numbers survive the reflexive
 * trigger's stack round-trip (CR 603.12), so the "when you do" half can spend X.
 *
 * @property player Whose graveyard is spent, and who chooses X. Defaults to the effect's controller.
 * @property storeAmountAs Pipeline variable name the chosen X is stored under.
 */
@SerialName("CollectEvidenceChosenAmount")
@Serializable
data class CollectEvidenceChosenAmountEffect(
    val player: Player = Player.You,
    val storeAmountAs: String,
) : Effect {
    override val description: String = "collect evidence X"
}
