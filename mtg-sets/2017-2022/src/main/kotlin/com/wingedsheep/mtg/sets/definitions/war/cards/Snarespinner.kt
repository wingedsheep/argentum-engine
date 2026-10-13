package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Snarespinner — War of the Spark #176 (canonical printing; reprinted in M21 and DMU)
 * {1}{G}
 * Creature — Spider
 * 1/3
 * Reach
 * Whenever this creature blocks a creature with flying, this creature gets +2/+0 until end of turn.
 */
val Snarespinner = card("Snarespinner") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spider"
    power = 1
    toughness = 3
    oracleText = "Reach\nWhenever this creature blocks a creature with flying, this creature gets +2/+0 until end of turn."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.blocks(attackerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "176"
        artist = "Yeong-Hao Han"
        flavorText = "\"Let's lure the drakes to the webs over the Samok Stand. Keep the spiders content.\"\n—Aurelia"
        imageUri = "https://cards.scryfall.io/normal/front/4/8/48a0fcc5-57d3-43ab-9e3d-0e015792239a.jpg?1783933407"
    }
}
