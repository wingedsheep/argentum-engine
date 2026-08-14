package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mayhem
import com.wingedsheep.sdk.model.Rarity

/**
 * Raging Goblinoids — Marvel's Spider-Man #85
 * {4}{R} · Creature — Goblin Berserker Villain · 5/4
 *
 * Haste
 * Mayhem {2}{R}
 */
val RagingGoblinoids = card("Raging Goblinoids") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Berserker Villain"
    power = 5
    toughness = 4
    oracleText = "Haste\n" +
        "Mayhem {2}{R} (You may cast this card from your graveyard for {2}{R} if you discarded it " +
        "this turn. Timing rules still apply.)"

    keywords(Keyword.HASTE)
    mayhem("{2}{R}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "85"
        artist = "Filipe Pagliuso"
        flavorText = "\"As if one goblin wasn't bad enough.\"\n—Spider-Man"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/8519598f-ab7f-49b0-90cc-c0b6422ebdf8.jpg?1783905334"
    }
}
