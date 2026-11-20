package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ill-Tempered Cyclops — Theros #125
 * {3}{R} · Creature — Cyclops · 3/3
 *
 * Trample
 * {5}{R}: Monstrosity 3.
 */
val IllTemperedCyclops = card("Ill-Tempered Cyclops") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Cyclops"
    power = 3
    toughness = 3
    oracleText = "Trample\n" +
        "{5}{R}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)"

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Mana("{5}{R}")
        effect = Effects.Monstrosity(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Peter Mohrbacher"
        flavorText = "A cyclops has two moods: angry and asleep."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d1f7f757-31ac-43e6-8be0-c5948d478d2f.jpg?1783939761"
        ruling("2013-09-15", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2013-09-15", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2013-09-15", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
