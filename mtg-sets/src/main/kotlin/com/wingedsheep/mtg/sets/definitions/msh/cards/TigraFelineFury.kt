package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tigra, Feline Fury — Marvel Super Heroes #191
 * {1}{G} · Legendary Creature — Cat Human Hero · Uncommon
 * 2/1
 *
 * Flash
 * Trample
 * Whenever you gain life, put a +1/+1 counter on Tigra.
 *
 * [Triggers.YouGainLife] fires once per life-gain event, however much life that event gained,
 * so the counter accrues per instance rather than per point of life.
 */
val TigraFelineFury = card("Tigra, Feline Fury") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Cat Human Hero"
    power = 2
    toughness = 1
    oracleText = "Flash\n" +
        "Trample\n" +
        "Whenever you gain life, put a +1/+1 counter on Tigra."

    keywords(Keyword.FLASH, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.YouGainLife
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Whenever you gain life, put a +1/+1 counter on Tigra."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "191"
        artist = "Ben Harvey"
        flavorText = "\"Say 'Here, kitty kitty' one more time. I dare you.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73c11aea-4fa4-438d-b886-2fafa60c82f9.jpg?1783902911"
    }
}
