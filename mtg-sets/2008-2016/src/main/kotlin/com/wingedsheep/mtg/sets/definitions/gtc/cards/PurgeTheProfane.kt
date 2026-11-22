package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Purge the Profane
 * {2}{W}{B}
 * Sorcery
 * Target opponent discards two cards and you gain 2 life.
 */
val PurgeTheProfane = card("Purge the Profane") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards and you gain 2 life."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Patterns.Hand.discardCards(2, opponent) then Effects.GainLife(2)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "190"
        artist = "Michael C. Hayes"
        flavorText = "The Orzhov hear the plans of the other guilds through the guilty thoughts of the disloyal."
        imageUri = "https://cards.scryfall.io/normal/front/9/3/937ee156-0105-4291-8c67-d03a59c24679.jpg"
    }
}
