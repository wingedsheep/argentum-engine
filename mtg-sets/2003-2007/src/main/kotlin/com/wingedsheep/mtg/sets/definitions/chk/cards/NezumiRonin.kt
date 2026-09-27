package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Nezumi Ronin
 * {2}{B}
 * Creature — Rat Samurai
 * 3/1
 * Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)
 */
val NezumiRonin = card("Nezumi Ronin") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat Samurai"
    power = 3
    toughness = 1
    oracleText = "Bushido 1 (Whenever this creature blocks or becomes blocked, it gets +1/+1 until end of turn.)"

    keywordAbility(KeywordAbility.bushido(1))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Scott M. Fischer"
        flavorText = "\"Some nezumi became as skilled in the samurai arts as the humans and kitsune. Yet no lord would have them, so they sold their swords to the highest bidder.\"\n—*The History of Kamigawa*"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c3b8d6f-c60a-4107-b931-31b10f497237.jpg?1783944311"
    }
}
