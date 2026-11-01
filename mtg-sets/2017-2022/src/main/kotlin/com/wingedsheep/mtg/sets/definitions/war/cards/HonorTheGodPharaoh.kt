package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Honor the God-Pharaoh — War of the Spark #132 (canonical printing)
 * {2}{R}
 * Sorcery
 * As an additional cost to cast this spell, discard a card.
 * Draw two cards. Amass Zombies 1.
 */
val HonorTheGodPharaoh = card("Honor the God-Pharaoh") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, discard a card.\n" +
        "Draw two cards. Amass Zombies 1. (Put a +1/+1 counter on an Army you control. It's also a " +
        "Zombie. If you don't control an Army, create a 0/0 black Zombie Army creature token first.)"

    additionalCost(Costs.additional.DiscardCards(1))

    spell {
        effect = Effects.DrawCards(2) then Effects.Amass(1, "Zombie")
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "132"
        artist = "David Palumbo"
        flavorText = "In life, they revered a diverse pantheon of gods. Now they revere only the God-Pharaoh."
        imageUri = "https://cards.scryfall.io/normal/front/7/5/75844d13-74f5-4008-9bc6-2f6d04f378aa.jpg"
    }
}
