package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CreaturesDamagedBySourceAreDoomed
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Runesword
 * {6}
 * Artifact
 * {3}, {T}: Target attacking creature gets +2/+0 until end of turn. When that creature leaves the
 * battlefield this turn, sacrifice this artifact. If the creature deals damage to a creature this
 * turn, the creature dealt damage can't be regenerated this turn. If a creature dealt damage by
 * the targeted creature would die this turn, exile that creature instead.
 *
 * Four sentences, one activation. The pump is ordinary; the other three are riders that have to
 * outlive the ability's resolution, and they split into two mechanisms:
 *
 *  - "When that creature leaves the battlefield this turn, sacrifice this artifact" is an
 *    *event-based* delayed trigger — `CreateDelayedTriggerEffect` with a `trigger` and a
 *    `watchedTarget` pinning it to the creature that was pumped, rather than the step-based form
 *    Venom uses. It expires at end of turn on its own, which is the printed "this turn".
 *  - The two death riders are one granted **static**, [CreaturesDamagedBySourceAreDoomed], read by
 *    the damage pipeline as it applies damage. They cannot be a triggered ability: a trigger for
 *    "whenever this deals damage to a creature" resolves only after state-based actions have
 *    already put the dying creature into its graveyard (CR 704.3), so marking it for exile then
 *    changes nothing. Carbonize gets away with composing the same two marks after its own damage
 *    because that is all one resolution; combat damage gives no such window.
 *
 * Both riders hang off the *pumped creature* rather than the Sword, which is what makes "dealt
 * damage by the targeted creature" fall out for free instead of needing its own bookkeeping.
 */
val Runesword = card("Runesword") {
    manaCost = "{6}"
    typeLine = "Artifact"
    oracleText = "{3}, {T}: Target attacking creature gets +2/+0 until end of turn. When that " +
        "creature leaves the battlefield this turn, sacrifice this artifact. If the creature " +
        "deals damage to a creature this turn, the creature dealt damage can't be regenerated " +
        "this turn. If a creature dealt damage by the targeted creature would die this turn, " +
        "exile that creature instead."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}"), Costs.Tap)
        target = Targets.AttackingCreature

        effect = Effects.Composite(
            Effects.ModifyStats(2, 0, EffectTarget.ContextTarget(0)),
            // The two death riders are one granted static, not a trigger: a trigger for "whenever
            // this deals damage to a creature" resolves only after state-based actions have binned
            // the dying creature (CR 704.3), too late to send it to exile instead.
            Effects.GrantStaticAbility(
                CreaturesDamagedBySourceAreDoomed(),
                EffectTarget.ContextTarget(0),
            ),
            CreateDelayedTriggerEffect(
                trigger = Triggers.LeavesBattlefield,
                watchedTarget = EffectTarget.ContextTarget(0),
                effect = SacrificeSelfEffect,
            ),
        )
        description = "{3}, {T}: Target attacking creature gets +2/+0 until end of turn. When " +
            "that creature leaves the battlefield this turn, sacrifice this artifact."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/7/4/741dbcf2-3372-45a8-b66f-d2ae12b4aac6.jpg?1783947924"
    }
}
