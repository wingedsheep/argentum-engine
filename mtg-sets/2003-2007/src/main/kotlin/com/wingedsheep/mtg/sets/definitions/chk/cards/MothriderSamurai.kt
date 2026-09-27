package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Mothrider Samurai
 * {3}{W}
 * Creature — Human Samurai
 * 2/2
 * Flying
 * Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)
 *
 * Flying is an engine-live keyword, so it stays a plain `keywords(…)` declaration.
 */
val MothriderSamurai = card("Mothrider Samurai") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Samurai"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)"

    keywords(Keyword.FLYING)
    keywordAbility(KeywordAbility.bushido(1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Mark Zug"
        flavorText = "When the night blossoms open, the wings of Eiganjo take flight."
        imageUri = "https://cards.scryfall.io/normal/front/3/5/35a236f7-f008-4eb8-91d9-31ea8589cf0c.jpg?1783944334"
    }
}
