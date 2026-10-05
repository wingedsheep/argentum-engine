package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glory Bearers
 * {3}{W}
 * Enchantment Creature — Human Cleric
 * 3/4
 * Whenever another creature you control attacks, it gets +0/+1 until end of turn.
 *
 * "it" is the attacking creature, bound as [EffectTarget.TriggeringEntity].
 */
val GloryBearers = card("Glory Bearers") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Human Cleric"
    power = 3
    toughness = 4
    oracleText = "Whenever another creature you control attacks, it gets +0/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).attacks()
        effect = Effects.ModifyStats(0, 1, EffectTarget.TriggeringEntity)
        description = "Whenever another creature you control attacks, it gets +0/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Tyler Walpole"
        flavorText = "\"We are bearers of the ancient light, keepers of sun-crowned Heliod's blessings. " +
            "Fight, and the light will protect you!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a70e1be-f04a-43c1-8fca-205a24e3db38.jpg?1783931596"
    }
}
