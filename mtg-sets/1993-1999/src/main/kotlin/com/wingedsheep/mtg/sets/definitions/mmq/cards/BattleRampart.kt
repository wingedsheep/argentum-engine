package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Battle Rampart
 * {2}{R}
 * Creature — Wall
 * 1 / 3
 *
 * A Wall that hands out haste — [Effects.GrantKeyword] at the default end-of-turn duration,
 * so nothing but the keyword and the bound target is written.
 */
val BattleRampart = card("Battle Rampart") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Wall"
    oracleText = "Defender\n" +
        "{T}: Target creature gains haste until end of turn."
    power = 1
    toughness = 3

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", Targets.Creature)
        effect = Effects.GrantKeyword(Keyword.HASTE, target = t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "173"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f27f6658-0f00-4934-8d12-cd0dda3958c9.jpg"
    }
}
