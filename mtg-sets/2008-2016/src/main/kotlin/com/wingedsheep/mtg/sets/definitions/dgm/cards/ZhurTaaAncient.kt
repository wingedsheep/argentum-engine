package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalManaOnSourceTap
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Zhur-Taa Ancient
 * {3}{R}{G}
 * Creature — Beast
 * 7/5
 * Whenever a player taps a land for mana, that player adds one mana of any type that land produced.
 *
 * Canonical printing: Dragon's Maze (2013), the card's only printing. The global Mana Flare shape
 * shared with Dictate of Karametra: [AdditionalManaOnSourceTap] over every land, any controller,
 * with `color = null` so the bonus mirrors the type the land produced for the tapping player.
 */
val ZhurTaaAncient = card("Zhur-Taa Ancient") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Beast"
    power = 7
    toughness = 5
    oracleText = "Whenever a player taps a land for mana, that player adds one mana of any type that land produced."

    staticAbility {
        ability = AdditionalManaOnSourceTap(
            sourceFilter = GameObjectFilter.Land,
            color = null // mirror the produced type ("any type that land produced")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "119"
        artist = "Adam Paquette"
        flavorText = "Many Ravnicans think of nature as orderly groves and elf-tended rot farms. They haven't seen the Gruul wilds."
        imageUri = "https://cards.scryfall.io/normal/front/2/0/2076308f-0f4e-4b31-9e75-c2965942e7d1.jpg?1783940018"
    }
}
