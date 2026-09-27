package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shimatsu the Bloodcloaked {3}{R}
 * Legendary Creature — Demon Spirit
 * 0/0
 * As Shimatsu enters, sacrifice any number of permanents. Shimatsu enters with that many +1/+1
 * counters on it.
 *
 * Not Devour: Devour is optional and filter-scoped, and `EntersWithDevour` only runs on the
 * spell-resolution and token paths. This is a plain as-enters replacement, so it is an
 * [OnEnterRun] — which also runs when Shimatsu is reanimated or blinked — over the Mana Seism
 * shape: gather your permanents (minus Shimatsu itself), choose any number, sacrifice them, and put
 * that many +1/+1 counters on Shimatsu. The whole thing runs inside the entry, before state-based
 * actions are checked, so the 0/0 body survives as long as at least one permanent was sacrificed.
 */
val ShimatsuTheBloodcloaked = card("Shimatsu the Bloodcloaked") {
    manaCost = "{3}{R}"
    typeLine = "Legendary Creature — Demon Spirit"
    power = 0
    toughness = 0
    oracleText = "As Shimatsu enters, sacrifice any number of permanents. Shimatsu enters with " +
        "that many +1/+1 counters on it."

    replacementEffect(
        OnEnterRun(
            Effects.Pipeline {
                val permanents = gather(GameObjectFilter.Permanent, player = Player.You, excludeSelf = true)
                val sacrificed = chooseAnyNumber(
                    from = permanents,
                    useTargetingUI = true,
                    prompt = "Sacrifice any number of permanents for Shimatsu",
                )
                sacrifice(sacrificed)
                run(
                    Effects.AddDynamicCounters(
                        CounterType.PLUS_ONE_PLUS_ONE,
                        DynamicAmounts.distinctEntitiesIn(sacrificed),
                        EffectTarget.Self,
                    )
                )
            }
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "186"
        artist = "Dave Allsop"
        flavorText = "Their dominion over dark and destructive forces twisted the oni into beings " +
            "of pure malevolence."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71de0e2c-61ca-496e-8990-ed0e6f6521a6.jpg?1783944296"
        ruling(
            "2004-12-01",
            "You sacrifice the permanents before Shimatsu enters, so you can't sacrifice any " +
                "creatures that enter at the same time it does."
        )
    }
}
