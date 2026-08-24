package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Alabaster Wall
 * {2}{W}
 * Creature — Wall
 * 0 / 4
 *
 * The plain Samite Healer shield ([Effects.PreventNextDamage]) on a Defender body — the
 * `{T}` cost and an "any target" requirement, nothing else.
 */
val AlabasterWall = card("Alabaster Wall") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Wall"
    oracleText = "Defender (This creature can't attack.)\n" +
        "{T}: Prevent the next 1 damage that would be dealt to any target this turn."
    power = 0
    toughness = 4

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", Targets.Any)
        effect = Effects.PreventNextDamage(1, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "2"
        artist = "Randy Gallegos"
        flavorText = "Its mortar is mixed with waters straight from the Fountain of Cho."
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cf393a3-831e-4d3a-8404-ee83f60970aa.jpg"
    }
}
