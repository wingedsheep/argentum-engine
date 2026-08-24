package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cavern Crawler
 * {2}{R}
 * Creature — Insect
 * 0 / 3
 */
val CavernCrawler = card("Cavern Crawler") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Insect"
    oracleText = "Mountainwalk (This creature can't be blocked as long as defending player controls a Mountain.)\n" +
        "{R}: This creature gets +1/-1 until end of turn."
    power = 0
    toughness = 3

    keywords(Keyword.MOUNTAINWALK)

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(1, -1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd0a8af9-2e86-4639-a6c9-209f115e95f8.jpg"
    }
}
