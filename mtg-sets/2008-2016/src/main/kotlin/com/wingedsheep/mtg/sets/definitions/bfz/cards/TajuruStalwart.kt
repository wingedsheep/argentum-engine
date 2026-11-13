package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.convergeEntersWithCounters
import com.wingedsheep.sdk.model.Rarity

/**
 * Tajuru Stalwart {2}{G} — Battle for Zendikar #194
 * Creature — Elf Scout Ally 0/1
 *
 * "Converge — This creature enters with a +1/+1 counter on it for each color of mana spent to cast it."
 *
 * Converge is an ability word: modelled via [convergeEntersWithCounters], i.e.
 * `EntersWithDynamicCounters(DynamicAmount.DistinctColorsManaSpent)`.
 */
val TajuruStalwart = card("Tajuru Stalwart") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Scout Ally"
    power = 0
    toughness = 1
    oracleText = "Converge — This creature enters with a +1/+1 counter on it for each color of mana spent to cast it."

    convergeEntersWithCounters()

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "194"
        artist = "Wesley Burt"
        flavorText = "In its time of hardship, all of Zendikar cries out in pain. Are you listening?"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f916220b-a942-41af-8949-aa384fa7857d.jpg?1783938184"
        ruling("2015-08-25", "If you cast a spell with converge without spending any mana to cast it (perhaps because an effect allowed you to cast it without paying its mana cost), then the number of colors spent to cast it will be zero.")
    }
}
