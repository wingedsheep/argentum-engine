package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MustBeBlocked

/**
 * Riveteers Decoy
 * {1}{G}
 * Creature — Human Warrior
 * 3 / 1
 * This creature must be blocked if able.
 * Blitz {3}{G} (If you cast this spell for its blitz cost, it gains haste and "When this creature dies, draw a card." Sacrifice it at the beginning of the next end step.)
 *
 * "Must be blocked if able" is the at-least-one form of [MustBeBlocked] (`allCreatures = false`).
 */
val RiveteersDecoy = card("Riveteers Decoy") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Warrior"
    oracleText = "This creature must be blocked if able.\nBlitz {3}{G} (If you cast this spell for its blitz cost, it gains haste and \"When this creature dies, draw a card.\" Sacrifice it at the beginning of the next end step.)"
    power = 3
    toughness = 1

    staticAbility {
        ability = MustBeBlocked()
    }

    keywordAbility(KeywordAbility.blitz("{3}{G}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "156"
        artist = "Inka Schulz"
        flavorText = "He keeps the heat off his allies—by drawing it all to himself."
        imageUri = "https://cards.scryfall.io/normal/front/2/d/2d21701b-6a99-4e1c-82b2-0219f170b09f.jpg?1783923100"
    }
}
