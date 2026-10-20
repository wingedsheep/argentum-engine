package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Celestial Unicorn
 * {2}{W}
 * Creature — Unicorn
 * 3/2
 * Whenever you gain life, put a +1/+1 counter on this creature.
 */
val CelestialUnicorn = card("Celestial Unicorn") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Unicorn"
    oracleText = "Whenever you gain life, put a +1/+1 counter on this creature."
    power = 3
    toughness = 2
    triggeredAbility {
        trigger = Triggers.you.gainsLife()
        effect = Effects.AddCounters(counterType = CounterType.PLUS_ONE_PLUS_ONE, count = 1, target = EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "5"
        artist = "Johannes Voss"
        flavorText = "When all hope fails, its light remains."
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f2d68812-8862-4e34-a992-7d6bbadaf316.jpg?1783926539"
        ruling("2021-07-23", "The ability of Celestial Unicorn triggers just once for each life-gaining event, whether it's 1 life from Dazzling Angel or 4 life from Apothecary Stomper.")
        ruling("2021-07-23", "If Celestial Unicorn is dealt lethal damage at the same time that you gain life, it won't receive a counter from its ability in time to save it.")
        ruling("2021-07-23", "If you gain an amount of life \"for each\" of something or \"equal to the number\" of something, that life is gained as one event and Celestial Unicorn's ability triggers only once.")
    }
}
