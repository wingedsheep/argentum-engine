package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Murder Investigation
 * {1}{W}
 * Enchantment — Aura
 * Enchant creature you control
 * When enchanted creature dies, create X 1/1 white Soldier creature tokens, where X is its power.
 *
 * "Its power" is the enchanted creature's last-known power on the battlefield (2013-01-24
 * ruling), read via `DynamicAmounts.triggeringPower()` off the dying creature.
 */
val MurderInvestigation = card("Murder Investigation") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "When enchanted creature dies, create X 1/1 white Soldier creature tokens, where X is its power."

    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.CreateToken(
            count = DynamicAmounts.triggeringPower(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Igor Kieryluk"
        flavorText = "\"Every death has a web of consequences. Our job is to find the spider.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1f3bb284-d10e-4265-92a4-8dcaf118f3c8.jpg?1783940141"
        ruling(
            "2013-01-24",
            "To determine how many Soldier tokens are created, use the power of the enchanted creature as it last existed on the battlefield."
        )
    }
}
