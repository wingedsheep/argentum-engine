package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Axgard Artisan
 * {1}{R}
 * Creature — Dwarf Artificer
 * 2/1
 * Whenever one or more +1/+1 counters are put on this creature for the first time each turn, create a
 * Treasure token.
 */
val AxgardArtisan = card("Axgard Artisan") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Artificer"
    oracleText = "Whenever one or more +1/+1 counters are put on this creature for the first time each turn, " +
        "create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.getsCounters(type = CounterType.PLUS_ONE_PLUS_ONE, firstTimeEachTurn = true)
        effect = Effects.CreateTreasure()
        description = "Whenever one or more +1/+1 counters are put on this creature for the first time each " +
            "turn, create a Treasure token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "332"
        artist = "Quintin Gleim"
        flavorText = "Skodreg knew the Phyrexians wouldn't appreciate his craftsmanship, but they'd get an up-close look just the same."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af130b37-d3e0-4839-8193-5c62a91f0218.jpg?1783916901"
    }
}
