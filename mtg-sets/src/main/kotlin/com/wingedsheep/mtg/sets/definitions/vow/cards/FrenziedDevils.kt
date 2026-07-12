package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frenzied Devils
 * {4}{R}
 * Creature — Devil
 * 3/3
 * Haste
 * Whenever you cast a noncreature spell, this creature gets +2/+2 until end of turn.
 */
val FrenziedDevils = card("Frenzied Devils") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Devil"
    oracleText = "Haste\nWhenever you cast a noncreature spell, this creature gets +2/+2 until end of turn."
    power = 3
    toughness = 3
    keywords(Keyword.HASTE)
    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"Devils need no reason to stir up chaos. The chaos itself is their reward.\"\n—Rem Karolus, Fiendslayer"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7912206-6de3-4085-b5f6-a2e90ea55b90.jpg?1782703077"
    }
}
