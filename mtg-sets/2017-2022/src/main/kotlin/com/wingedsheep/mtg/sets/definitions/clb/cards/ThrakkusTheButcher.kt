package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Thrakkus the Butcher
 * {3}{R}{G}
 * Legendary Creature — Dragon Peasant
 * 3/4
 *
 * Trample
 * Whenever Thrakkus attacks, double the power of each Dragon you control until end of turn.
 *
 * The attack trigger is the power-only half of [Patterns.Group.doublePowerAndToughnessForAll]:
 * each Dragon gets +X/+0 where X is its own power as the ability resolves (read per entity off
 * [EffectTarget.IterationEntity]), the "double" ruling. Thrakkus is itself a Dragon, so it
 * doubles too.
 */
val ThrakkusTheButcher = card("Thrakkus the Butcher") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Dragon Peasant"
    power = 3
    toughness = 4
    oracleText = "Trample\n" +
        "Whenever Thrakkus attacks, double the power of each Dragon you control until end of turn."

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Group.modifyStatsForAll(
            power = DynamicAmounts.powerOf(EffectTarget.IterationEntity),
            toughness = DynamicAmounts.fixed(0),
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.DRAGON).youControl()),
        )
        description = "Whenever Thrakkus attacks, double the power of each Dragon you control until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "295"
        artist = "Néstor Ossandón Leal"
        flavorText = "Somehow, he never seems to run out of fresh meat."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/2255d1e4-f387-4187-8c10-7d0009f6ec79.jpg?1783922685"
        ruling("2022-06-10", "To double a creature's power, that creature gets +X/+0, where X is the power of that creature as the triggered ability resolves.")
    }
}
