package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rocketeer Boostbuggy
 * {R}{G}
 * Artifact — Vehicle
 * 3/2
 * Whenever this Vehicle attacks, create a Treasure token.
 * Exhaust — {3}: This Vehicle becomes an artifact creature. Put a +1/+1 counter on it.
 * (Activate each exhaust ability only once.)
 * Crew 1
 *
 * The exhaust ability animates the Vehicle **permanently** — there is no "until end of turn"
 * clause — so it takes `Duration.Permanent` with the printed 3/2 as the base P/T, exactly as
 * Marshals' Pathcruiser does; the +1/+1 counter then layers on top in 7d.
 */
val RocketeerBoostbuggy = card("Rocketeer Boostbuggy") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Artifact — Vehicle"
    power = 3
    toughness = 2
    oracleText = "Whenever this Vehicle attacks, create a Treasure token. (It's an artifact with " +
        "\"{T}, Sacrifice this token: Add one mana of any color.\")\n" +
        "Exhaust — {3}: This Vehicle becomes an artifact creature. Put a +1/+1 counter on it. " +
        "(Activate each exhaust ability only once.)\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle " +
        "becomes an artifact creature until end of turn.)"

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.CreateTreasure(1)
    }

    activatedAbility {
        cost = Costs.Mana("{3}")
        isExhaust = true
        effect = Effects.Composite(
            Effects.BecomeCreature(
                target = EffectTarget.Self,
                power = 3,
                toughness = 2,
                duration = Duration.Permanent
            ),
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        )
        description = "Exhaust — {3}: This Vehicle becomes an artifact creature. " +
            "Put a +1/+1 counter on it."
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "220"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c80c91e-dd3d-4c7b-89e4-bfb253eeaee2.jpg?1783907853"
        ruling("2025-02-07", "Exhaust abilities can be activated any time you could normally activate an ability.")
        ruling(
            "2025-02-07",
            "If an exhaust ability of a permanent is activated, and then that permanent leaves the " +
                "battlefield and returns to the battlefield, it becomes a new object so its exhaust " +
                "ability can be activated again."
        )
    }
}
