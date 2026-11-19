package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Fleetfeather Cockatrice
 * {3}{G}{U}
 * Creature — Cockatrice
 * 3/3
 * Flash
 * Flying, deathtouch
 * {5}{G}{U}: Monstrosity 3.
 */
val FleetfeatherCockatrice = card("Fleetfeather Cockatrice") {
    manaCost = "{3}{G}{U}"
    typeLine = "Creature — Cockatrice"
    power = 3
    toughness = 3
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Flying, deathtouch\n" +
        "{5}{G}{U}: Monstrosity 3. (If this creature isn't monstrous, put three +1/+1 counters on it and it becomes monstrous.)"

    keywords(Keyword.FLASH, Keyword.FLYING, Keyword.DEATHTOUCH)

    activatedAbility {
        cost = Costs.Mana("{5}{G}{U}")
        effect = Effects.Monstrosity(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/1/1/1139236e-5d71-4fc1-937d-ba1efadee031.jpg?1783939404"
        ruling("2014-04-26", "Once a creature becomes monstrous, it can't become monstrous again. If the creature is already monstrous when the monstrosity ability resolves, nothing happens.")
        ruling("2014-04-26", "Monstrous isn't an ability that a creature has. It's just something true about that creature. If the creature stops being a creature or loses its abilities, it will continue to be monstrous.")
        ruling("2014-04-26", "An ability that triggers when a creature becomes monstrous won't trigger if that creature isn't on the battlefield when its monstrosity ability resolves.")
    }
}
