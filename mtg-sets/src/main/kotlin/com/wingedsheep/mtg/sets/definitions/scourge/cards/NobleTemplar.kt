package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Noble Templar
 * {5}{W}
 * Creature — Human Cleric Soldier
 * 3/6
 * Vigilance
 * Plainscycling {2} ({2}, Discard this card: Search your library for a Plains card,
 * reveal it, put it into your hand, then shuffle.)
 */
val NobleTemplar = card("Noble Templar") {
    manaCost = "{5}{W}"
    typeLine = "Creature — Human Cleric Soldier"
    power = 3
    toughness = 6
    oracleText = "Vigilance\nPlainscycling {2}"

    keywords(Keyword.VIGILANCE)

    keywordAbility(KeywordAbility.Typecycling("Plains", ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "19"
        artist = "Arnie Swekel"
        flavorText = "His faith in Ixidor's dream inspired others to follow him into the unknown."
        imageUri = "https://cards.scryfall.io/large/front/6/a/6a9ede92-e64f-44a5-afb6-c7495077fb0b.jpg?1562530174"
    }
}
