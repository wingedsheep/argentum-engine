package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

val PhyrexianEspionage = card("Phyrexian Espionage") {
    manaCost = "{2}{U}"
    colorIdentity = "BU"
    typeLine = "Sorcery"
    oracleText = "Kicker {1}{B} (You may pay an additional {1}{B} as you cast this spell.)\nDraw two cards. If this spell was kicked, each opponent discards a card."

    keywordAbility(KeywordAbility.kicker("{1}{B}"))

    spell {
        effect = Effects.DrawCards(2) then Effects.If(
            Conditions.WasKicked,
            Patterns.Hand.eachOpponentDiscards(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Allen Williams"
        flavorText = "\"Even if they're hunting for sleeper agents, who looks twice at a bird?\"\n—Rona"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b983366-385f-41a3-aa46-20151e68d140.jpg?1783921347"
    }
}
