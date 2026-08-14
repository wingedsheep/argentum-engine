package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Waxen Shapethief — Aetherdrift #74
 * {3}{U} · Creature — Shapeshifter · 0/0
 *
 * Flash
 * You may have this creature enter as a copy of an artifact or creature you control.
 * Cycling {2}
 *
 * A self-only clone: the copy pool is narrowed to permanents **you** control (CR 707.2), so it
 * duplicates your own best artifact or creature rather than stealing a stat line from across the
 * table. `optional = true` keeps the "you may" — declining leaves a printed 0/0 that dies to state-
 * based actions immediately, which is the card's real downside when you have nothing worth copying.
 *
 * Flash plus a copy of a creature you already control means the copy's own enters-the-battlefield
 * triggers fire at instant speed. Cycling is the escape hatch for the same empty board.
 */
val WaxenShapethief = card("Waxen Shapethief") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 0
    toughness = 0
    oracleText = "Flash\n" +
        "You may have this creature enter as a copy of an artifact or creature you control.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    keywords(Keyword.FLASH)

    replacementEffect(
        EntersAsCopy(
            optional = true,
            copyFilter = GameObjectFilter.CreatureOrArtifact.youControl(),
        )
    )

    keywordAbility(KeywordAbility.cycling("{2}"))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "74"
        artist = "Helge C. Balzer"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/412aaa30-b9cd-4cf8-beb8-1c1229667b31.jpg?1783907899"
    }
}
