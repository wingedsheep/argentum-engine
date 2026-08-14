package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rhovanion Rampager
 * {2}{B}
 * Creature — Wolf
 * 3/2
 *
 * Whenever this creature attacks, you may sacrifice another creature. If you do, put a
 * number of +1/+1 counters on this creature equal to the sacrificed creature's power.
 * When this creature dies, amass Goblins X, where X is this creature's power.
 *
 * Modeling notes:
 *  - The attack trigger is the Gitrog, Ravenous Ride idiom: an inline [Effects.Pipeline]
 *    gathers the other creatures you control, offers **up to one** ("you may") on the
 *    battlefield-targeting UI, and sacrifices it. Declining leaves the collection empty, so
 *    [DynamicAmounts.sacrificedPower] reads 0 and no counters are placed — which is exactly
 *    what "If you do" gates. The sacrifice is a resolution-time action, not a cost.
 *  - "The sacrificed creature's power" is last-known information (CR 608.2h): the sacrifice
 *    records an `EntitySnapshot`, and `EntityReference.Sacrificed` is a `LIVE_THEN_LKI`
 *    reference, so the amount reads the power the creature had as it left the battlefield —
 *    including any counters or pumps it was carrying.
 *  - The dies trigger's X is likewise this creature's *last-known* power, so counters it had
 *    accumulated from its own attack trigger still count. [DynamicAmounts.sourcePower] over
 *    `EntityReference.Source` is `LIVE_THEN_LKI` for the same reason.
 */
val RhovanionRampager = card("Rhovanion Rampager") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Wolf"
    power = 3
    toughness = 2
    oracleText = "Whenever this creature attacks, you may sacrifice another creature. If you do, " +
        "put a number of +1/+1 counters on this creature equal to the sacrificed creature's " +
        "power.\n" +
        "When this creature dies, amass Goblins X, where X is this creature's power. (Put X " +
        "+1/+1 counters on an Army you control. It's also a Goblin. If you don't control an " +
        "Army, create a 0/0 black Goblin Army creature token first.)"

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.Pipeline {
            val others = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                    excludeSelf = true,
                )
            )
            val chosen = chooseUpTo(
                1,
                from = others,
                useTargetingUI = true,
                prompt = "You may sacrifice another creature",
                selectedLabel = "Sacrifice",
            )
            sacrifice(chosen)
            run(
                Effects.AddDynamicCounters(
                    Counters.PLUS_ONE_PLUS_ONE,
                    DynamicAmounts.sacrificedPower(),
                    EffectTarget.Self,
                )
            )
        }
        description = "Whenever this creature attacks, you may sacrifice another creature. If " +
            "you do, put a number of +1/+1 counters on this creature equal to the sacrificed " +
            "creature's power."
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.Amass(DynamicAmounts.sourcePower(), "Goblin")
        description = "When this creature dies, amass Goblins X, where X is this creature's power."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "82"
        artist = "Kevin Sidharta"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5ee45a5e-3650-47b3-8d31-6b1de9e27a14.jpg?1785412699"
    }
}
