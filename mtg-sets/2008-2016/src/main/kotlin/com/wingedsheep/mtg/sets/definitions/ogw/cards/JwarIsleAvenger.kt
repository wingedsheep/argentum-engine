package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Jwar Isle Avenger
 * {4}{U}
 * Creature — Sphinx
 * 3/3
 * Surge {2}{U} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)
 * Flying
 */
val JwarIsleAvenger = card("Jwar Isle Avenger") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Sphinx"
    power = 3
    toughness = 3
    oracleText = "Surge {2}{U} (You may cast this spell for its surge cost if you or a teammate has cast another spell this turn.)\n" +
        "Flying"

    keywordAbility(KeywordAbility.surge("{2}{U}"))
    keywords(Keyword.FLYING)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "Ryan Pancoast"
        flavorText = "Though solitary by nature, the sphinxes did not hesitate to join in the fight against the Eldrazi."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a5b059f-2fa3-474f-9c74-6d4021703add.jpg?1783937918"
    }
}
