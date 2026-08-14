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
