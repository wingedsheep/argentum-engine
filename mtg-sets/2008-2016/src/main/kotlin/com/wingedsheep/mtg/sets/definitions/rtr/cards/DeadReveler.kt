package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.unleash
import com.wingedsheep.sdk.model.Rarity

/**
 * Dead Reveler — Return to Ravnica #62 (canonical printing)
 * {2}{B} · Creature — Zombie · 2/3
 *
 * Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as
 * it has a +1/+1 counter on it.)
 */
val DeadReveler = card("Dead Reveler") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 3
    oracleText = "Unleash (You may have this creature enter with a +1/+1 counter on it. It can't block as long as it has a +1/+1 counter on it.)"

    unleash()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "62"
        artist = "David Palumbo"
        flavorText = "He's the unlife of the party."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/909a0a38-2c22-4b49-8938-1a8162c077e6.jpg?1783940363"
    }
}
