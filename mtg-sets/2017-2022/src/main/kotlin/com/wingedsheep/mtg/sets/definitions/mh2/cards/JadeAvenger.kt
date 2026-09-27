package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Jade Avenger — Modern Horizons 2 #167
 * {1}{G} · Creature — Frog Samurai · 2 / 2
 *
 * Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.)
 */
val JadeAvenger = card("Jade Avenger") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Frog Samurai"
    power = 2
    toughness = 2
    oracleText = "Bushido 2 (Whenever this creature blocks or becomes blocked, it gets +2/+2 until end of turn.)"

    keywordAbility(KeywordAbility.bushido(2))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "167"
        artist = "Chris Seaman"
        flavorText = "\"Froggy fighter at the gate.\nDraw your sword and meet your fate.\"\n—Traditional children's rhyme"
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f81500be-c959-4f38-bcf2-d63519168f67.jpg?1783926829"
    }
}
