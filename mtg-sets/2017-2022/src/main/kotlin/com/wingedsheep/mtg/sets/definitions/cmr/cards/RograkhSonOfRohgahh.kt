package com.wingedsheep.mtg.sets.definitions.cmr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Rograkh, Son of Rohgahh — Commander Legends #197 (canonical printing)
 * {0} · Legendary Creature — Kobold Warrior · 0/1
 *
 * First strike, menace, trample
 * Partner (You can have two commanders if both have partner.)
 *
 * A zero-cost card with a red color indicator (CR 204): the indicator makes it red and puts red
 * in its color identity, though the {0} mana cost has no colored symbols. Partner is a
 * deck-construction ability (CR 702.124h) and does nothing during play.
 */
val RograkhSonOfRohgahh = card("Rograkh, Son of Rohgahh") {
    manaCost = "{0}"
    colorIdentity = "R"
    colorIndicator = "R"
    typeLine = "Legendary Creature — Kobold Warrior"
    power = 0
    toughness = 1
    oracleText = "First strike, menace, trample\n" +
        "Partner (You can have two commanders if both have partner.)"

    keywords(Keyword.FIRST_STRIKE, Keyword.MENACE, Keyword.TRAMPLE)
    keywordAbility(KeywordAbility.partner())

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "197"
        artist = "Chris Seaman"
        flavorText = "Strength is relative."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a4fab67f-00c2-4125-9262-d21a29411797.jpg?1783928807"
    }
}
