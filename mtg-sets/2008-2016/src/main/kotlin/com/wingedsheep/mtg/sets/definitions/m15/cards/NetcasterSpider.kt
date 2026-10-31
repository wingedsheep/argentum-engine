package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Netcaster Spider — Magic 2015 #186 (canonical printing; reprinted in OGW, CN2 and M20)
 * {2}{G}
 * Creature — Spider
 * 2/3
 * Reach
 * Whenever this creature blocks a creature with flying, this creature gets +2/+0 until end of turn.
 */
val NetcasterSpider = card("Netcaster Spider") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 2
    toughness = 3
    oracleText = "Reach (This creature can block creatures with flying.)\nWhenever this creature blocks a creature with flying, this creature gets +2/+0 until end of turn."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.blocks(attackerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "186"
        artist = "Yohann Schepacz"
        flavorText = "It is an expert at culling individuals who stray too far from the herd."
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8702c15-5f32-453e-8d58-cadfcdaeb3dc.jpg?1783939165"
    }
}
