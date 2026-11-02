package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithDynamicCounters

/**
 * Golgari Raiders
 * {3}{G}
 * Creature — Elf Warrior
 * 0/0
 * Haste
 * Undergrowth — This creature enters with a +1/+1 counter on it for each creature card in your graveyard.
 */
val GolgariRaiders = card("Golgari Raiders") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Warrior"
    oracleText = "Haste\nUndergrowth — This creature enters with a +1/+1 counter on it for each creature card in your graveyard."
    power = 0
    toughness = 0

    keywords(Keyword.HASTE)

    replacementEffect(
        EntersWithDynamicCounters(count = DynamicAmounts.creatureCardsInYourGraveyard())
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "130"
        artist = "David Palumbo"
        flavorText = "What the Golgari can't claim through patient undercity expansion, they seize in brazen topside sorties."
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37284ba8-441c-4cf6-95c6-6a0f52e6d36d.jpg?1783934153"
        ruling("2018-10-05", "If you return Golgari Raiders from your graveyard to the battlefield, its undergrowth ability counts itself.")
        ruling("2018-10-05", "Creature cards with other types, such as artifact creature cards, count for undergrowth abilities.")
        ruling("2018-10-05", "Because tokens aren't cards, they never count for undergrowth abilities.")
    }
}
