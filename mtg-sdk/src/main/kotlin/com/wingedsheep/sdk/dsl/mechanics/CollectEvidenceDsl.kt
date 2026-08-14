package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Add the **linked** optional collect-evidence additional cost (CR 701.59, Murders at Karlov Manor)
 * — "As an additional cost to cast this spell, you may collect evidence [amount]."
 *
 * Collect evidence is a keyword *action*, not a keyword ability: "to collect evidence N" means to
 * exile any number of cards from your graveyard with total mana value N or greater (CR 701.59a).
 * The payable thing itself is context-free and lives on the shared cost vocabulary
 * ([com.wingedsheep.sdk.scripting.costs.CostAtom.CollectEvidence]), so it is equally available as an
 * activated-ability cost, a ward cost, and a resolution-time effect. **This helper is only for the
 * one shape that carries a linkage** — the optional cast-time cost whose declaration a later ability
 * on the same card asks about (CR 701.59c makes those abilities linked, CR 607).
 *
 * Wired entirely by the shared optional-additional-cost rail
 * ([KeywordAbility.OptionalAdditionalCost]) with `declaredSlot = `[ChoiceSlot.EVIDENCE_COLLECTED],
 * exactly as `bargain()` does: the enumerator offers a second "Cast … (collect evidence N)" action
 * whenever the caster's graveyard can actually reach [amount] (CR 701.59b — when it can't, the
 * option is *absent*, not offered and refused), the cast handler pays it through the ordinary
 * additional-cost flow, and the engine stamps the slot on the spell and on the permanent it becomes.
 * Because the slot — not a shared boolean — carries the mechanic's identity, a spell cast with
 * evidence collected never satisfies a "whenever you cast a kicked spell" payoff, and a kicked or
 * bargained spell never satisfies [Conditions.WasEvidenceCollected].
 *
 * The card supplies its own payoff; this helper derives nothing:
 * - A spell rider — gate the alternate clause on [Conditions.WasEvidenceCollected] (Extract a
 *   Confession: "Each opponent sacrifices a creature of their choice. If evidence was collected,
 *   instead each opponent sacrifices a creature with the greatest power …").
 * - A permanent — an enters-the-battlefield trigger with an intervening-if on
 *   [Conditions.WasEvidenceCollected] (Vitu-Ghazi Inspector: "When this creature enters, if evidence
 *   was collected, put a +1/+1 counter on target creature and you gain 2 life"), which per CR 603.4
 *   never goes on the stack when no evidence was collected.
 * - A cheaper cast — `ModifySpellCost(SelfCast, ReduceGeneric(n),
 *   CostGating.OnlyIf(Conditions.WasEvidenceCollected))` (Bite Down on Crime: "This spell costs {2}
 *   less to cast if evidence was collected").
 *
 * @param amount The mana-value floor N the exiled cards' total must meet or exceed.
 */
fun CardBuilder.collectEvidence(amount: Int) {
    keywordAbilityList.add(
        KeywordAbility.OptionalAdditionalCost(
            additionalCost = Costs.additional.CollectEvidence(amount),
            declaredSlot = ChoiceSlot.EVIDENCE_COLLECTED,
        )
    )
}
