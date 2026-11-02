package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.MustBeBlocked

/**
 * Ochran Assassin
 * {1}{B}{G}
 * Creature — Elf Assassin
 * 1/1
 * Deathtouch
 * All creatures able to block this creature do so.
 */
val OchranAssassin = card("Ochran Assassin") {
    manaCost = "{1}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Creature — Elf Assassin"
    power = 1
    toughness = 1
    oracleText = "Deathtouch\nAll creatures able to block this creature do so."

    keywords(Keyword.DEATHTOUCH)

    staticAbility {
        ability = MustBeBlocked(allCreatures = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "194"
        artist = "Anna Steinbauer"
        flavorText = "\"The dagger is just the distraction. I carry the real killer in a vial at my side.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/4/541ef933-64f8-4075-9bd3-97149ac84b2b.jpg?1783934126"
    }
}
