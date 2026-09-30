package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Blightbelly Rat
 * {1}{B}
 * Creature — Phyrexian Rat
 * 2/2
 *
 * Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)
 * When this creature dies, proliferate.
 */
val BlightbellyRat = card("Blightbelly Rat") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Phyrexian Rat"
    power = 2
    toughness = 2
    oracleText = "Toxic 1 (Players dealt combat damage by this creature also get a poison counter.)\n" +
        "When this creature dies, proliferate. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    keywordAbility(KeywordAbility.Numeric(Keyword.TOXIC, 1))

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.Proliferate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "85"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/9255cd01-a611-4fec-b9ec-b271687740ba.jpg?1783918051"
    }
}
