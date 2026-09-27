package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Samurai Enforcers
 * {4}{W}{W}
 * Creature — Human Samurai
 * 4/4
 * Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.)
 */
val SamuraiEnforcers = card("Samurai Enforcers") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Samurai"
    power = 4
    toughness = 4
    oracleText = "Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.)"

    keywordAbility(KeywordAbility.bushido(2))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Mitch Cotie"
        flavorText = "From the moment they swore their oaths, they belonged to their lord, sword and soul."
        imageUri = "https://cards.scryfall.io/normal/front/8/b/8b2be3fe-87a2-47b2-8af1-b99a48622c7b.jpg?1783944332"
    }
}
