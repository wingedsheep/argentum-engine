package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hellkite Punisher
 * {5}{R}{R}
 * Creature — Dragon
 * 6/6
 * Flying
 * {R}: This creature gets +1/+0 until end of turn.
 */
val HellkitePunisher = card("Hellkite Punisher") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 6
    toughness = 6
    oracleText = "Flying\n{R}: This creature gets +1/+0 until end of turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "This creature gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Andrey Kuzinskiy"
        flavorText = "\"We'll pay no more tribute to that tyrannical lizard. A new age starts today!\"\n" +
            "—Mayor Kadrim, now deceased"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7bf663d3-850b-4a24-8e4b-08311adf4ed0.jpg?1783930688"
    }
}
