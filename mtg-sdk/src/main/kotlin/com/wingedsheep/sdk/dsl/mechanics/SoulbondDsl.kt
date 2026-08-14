package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Add Soulbond (CR 702.95, Avacyn Restored) — the keyword plus the two triggered abilities it
 * represents.
 *
 * > **CR 702.95a** — "Soulbond is a keyword that represents two triggered abilities. 'Soulbond'
 * > means 'When this creature enters, if you control both this creature and another creature and
 * > both are unpaired, you may pair this creature with another unpaired creature you control for as
 * > long as both remain creatures on the battlefield under your control' and 'Whenever another
 * > creature you control enters, if you control both that creature and this one and both are
 * > unpaired, you may pair that creature with this creature for as long as both remain creatures on
 * > the battlefield under your control.'"
 *
 * Neither half needs new decision plumbing — both are gather → select → pair pipelines over
 * existing primitives, with `PairWithSourceEffect` as the only soulbond-specific step:
 *
 * - **This creature enters.** Gather the unpaired creatures its controller controls (excluding
 *   itself), then `chooseUpTo(1)`. The `upTo` *is* the "you may": declining selects nothing and the
 *   pair step no-ops. It also subsumes the intervening-if's "if you control … another creature" —
 *   an empty gather asks nothing and does nothing. [Conditions.SourceIsUnpaired] carries the rest of
 *   the intervening-if, and it is not redundant even though the source just entered unpaired: with
 *   two soulbond creatures on the board, the *other* one's second ability can resolve first and pair
 *   them, and CR 603.4 re-checks this condition on resolution — so without it the source would ask
 *   for a second partner it can't legally take.
 *
 * - **Another creature you control enters.** [Triggers.OtherCreatureEnters] already carries the
 *   "another creature **you control**" half of the intervening-if, and a creature that just entered
 *   can never already be paired, so the only clause left to check is that *this* creature is
 *   unpaired — [Conditions.SourceIsUnpaired] as the `triggerCondition`. `optional = true` supplies
 *   the "you may" as a plain yes/no, which reads better than a one-candidate selection.
 *
 * The "for as long as both remain creatures on the battlefield under your control" duration is not
 * modelled here: it is the pairing *state's* own lifetime, enforced by the engine's CR 702.95e
 * check, and CR 702.95c (either half gone by resolution → neither becomes paired) is enforced by
 * the effect's executor.
 *
 * The payoff clause is a separate, ordinary static ability on the card, scoped to
 * [com.wingedsheep.sdk.scripting.filters.unified.Scope.SoulbondPair] via
 * `GroupFilter.soulbondPair()` — that scope is empty while unpaired, so "as long as this creature
 * is paired with another creature" needs no condition gate of its own.
 */
fun CardBuilder.soulbond() {
    keywordSet.add(Keyword.SOULBOND)

    // "When this creature enters, … you may pair this creature with another unpaired creature you
    // control."
    triggeredAbilities.add(
        TriggeredAbility.create(
            trigger = Triggers.EntersBattlefield.event,
            binding = Triggers.EntersBattlefield.binding,
            effect = Effects.Pipeline {
                val candidates = gather(
                    filter = GameObjectFilter.Creature.unpaired(),
                    player = Player.You,
                    excludeSelf = true
                )
                val partner = chooseUpTo(
                    1,
                    from = candidates,
                    prompt = "You may pair this creature with an unpaired creature you control",
                    useTargetingUI = true
                )
                pairWithSource(partner)
            },
            triggerCondition = Conditions.SourceIsUnpaired,
            descriptionOverride = "Soulbond (You may pair this creature with another unpaired " +
                "creature when either enters. They remain paired for as long as you control both of them.)"
        )
    )

    // "Whenever another creature you control enters, … you may pair that creature with this creature."
    triggeredAbilities.add(
        TriggeredAbility.create(
            trigger = Triggers.OtherCreatureEnters.event,
            binding = Triggers.OtherCreatureEnters.binding,
            effect = Effects.Pipeline {
                val partner = gather(CardSource.TriggeringEntity)
                pairWithSource(partner)
            },
            optional = true,
            triggerCondition = Conditions.SourceIsUnpaired,
            descriptionOverride = "You may pair that creature with this creature"
        )
    )
}
