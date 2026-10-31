package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Waterkin Shaman (M20 #288)
 * {1}{U}  Creature — Elemental Shaman  2/1
 *
 * Whenever a creature you control with flying enters, this creature gets +1/+1 until end of turn.
 */
val WaterkinShaman = card("Waterkin Shaman") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental Shaman"
    oracleText = "Whenever a creature you control with flying enters, this creature gets +1/+1 until end of turn."
    power = 2
    toughness = 1

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).youControl()).enters()
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "288"
        artist = "Yeong-Hao Han"
        flavorText = "\"They come to drink, to bathe, and—I believe—to play.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/8/184bfbc4-1c9b-44f7-b4a7-068290db3bac.jpg"
    }
}
