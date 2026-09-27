package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Konda, Lord of Eiganjo
 * {5}{W}{W}
 * Legendary Creature — Human Samurai
 * 3/3
 * Vigilance, indestructible
 * Bushido 5 (Whenever this creature blocks or becomes blocked, it gets +5/+5 until end of turn.)
 *
 * Vigilance and indestructible are engine-live keywords, so they stay a plain `keywords(…)`
 * declaration. Legendary is carried by the type line — `TypeLine.parse` reads the supertype.
 */
val KondaLordOfEiganjo = card("Konda, Lord of Eiganjo") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Samurai"
    power = 3
    toughness = 3
    oracleText = "Vigilance, indestructible\n" +
        "Bushido 5 (Whenever this creature blocks or becomes blocked, it gets +5/+5 until end of turn.)"

    keywords(Keyword.VIGILANCE, Keyword.INDESTRUCTIBLE)
    keywordAbility(KeywordAbility.bushido(5))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "30"
        artist = "John Bolton"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5edab171-94b9-4e5e-ab61-bd8c6c8cfc38.jpg?1783944335"
    }
}
