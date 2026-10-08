package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.dsl.DynamicAmounts

/**
 * Wave of Indifference
 * {X}{R}
 * Sorcery
 * X target creatures can't block this turn.
 */
val WaveOfIndifference = card("Wave of Indifference") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "X target creatures can't block this turn."

    spell {
        targets(TargetFilter.Creature, exactly = DynamicAmounts.xValue())
        effect = Effects.ForEachTarget(Effects.CantBlock(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "243"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        flavorText = "\"Darius?\" \"Yeah?\" \"There's a goblin sneaking up on you.\" \"So?\" \"Just sayin'.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/c/2c88b942-06d5-45d8-a4d8-6ca864f65516.jpg?1593017317"
    }
}
