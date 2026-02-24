package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Heedless One
 * {3}{G}
 * Creature — Elf Avatar
 * *|*
 * Trample
 * Heedless One's power and toughness are each equal to the number of Elves on the battlefield.
 */
val HeedlessOne = card("Heedless One") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Elf Avatar"
    oracleText = "Trample\nHeedless One's power and toughness are each equal to the number of Elves on the battlefield."

    dynamicStats(DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature.withSubtype("Elf")))

    keywords(Keyword.TRAMPLE)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "265"
        artist = "Mark Zug"
        flavorText = "\"The concerns of Wirewood are the concerns of all elves. They just don't know it yet.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea338499-26a0-44e5-8999-f264644184d1.jpg?1562950789"
    }
}
