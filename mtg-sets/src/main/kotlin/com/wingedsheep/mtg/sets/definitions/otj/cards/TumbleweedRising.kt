package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tumbleweed Rising
 * {1}{G}
 * Sorcery
 *
 * Create an X/X green Elemental creature token, where X is the greatest power
 * among creatures you control.
 * Plot {2}{G}
 */
val TumbleweedRising = card("Tumbleweed Rising") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Create an X/X green Elemental creature token, where X is the greatest power " +
        "among creatures you control.\n" +
        "Plot {2}{G} (You may pay {2}{G} and exile this card from your hand. Cast it as a sorcery " +
        "on a later turn without paying its mana cost. Plot only as a sorcery.)"

    keywordAbility(KeywordAbility.plot("{2}{G}"))

    spell {
        // X is the greatest power among creatures you control (token created with that P/T).
        val greatestPower = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxPower()
        effect = Effects.CreateDynamicToken(
            dynamicPower = greatestPower,
            dynamicToughness = greatestPower,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elemental"),
            imageUri = "https://cards.scryfall.io/normal/front/0/0/008695e6-6d6f-4c16-bf05-377e8cc5f5ff.jpg?1712316611"
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "187"
        artist = "Jason Smith"
        flavorText = "When weeds tumble against the wind, *run*."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/275d2d2a-ef85-48c9-919d-bc62cdad8a10.jpg?1712356021"
    }
}
