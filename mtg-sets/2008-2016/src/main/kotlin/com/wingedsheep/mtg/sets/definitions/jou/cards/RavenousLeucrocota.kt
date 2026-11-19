package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ravenous Leucrocota
 * {3}{G}
 * Creature — Beast
 * 2/4
 * Vigilance
 * {6}{G}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)
 */
val RavenousLeucrocota = card("Ravenous Leucrocota") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Beast"
    power = 2
    toughness = 4
    oracleText = "Vigilance\n{6}{G}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)"

    keywords(Keyword.VIGILANCE)

    activatedAbility {
        cost = Costs.Mana("{6}{G}")
        effect = Effects.Monstrosity(3)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Christopher Burdett"
        flavorText = "Hunger makes a leucrocota dangerous. A full belly makes it angry and dangerous."
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e91524b-4885-45fc-b22d-f9e5ee55845d.jpg?1783939410"
        ruling("2014-04-26", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2014-04-26", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2014-04-26", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
