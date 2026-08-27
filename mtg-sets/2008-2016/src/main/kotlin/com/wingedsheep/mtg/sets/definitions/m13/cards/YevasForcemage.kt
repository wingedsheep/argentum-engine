package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Yeva's Forcemage
 * {2}{G}
 * Creature — Elf Shaman
 * 2/2
 * When this creature enters, target creature gets +2/+2 until end of turn.
 */
val YevasForcemage = card("Yeva's Forcemage") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Shaman"
    power = 2
    toughness = 2
    oracleText = "When this creature enters, target creature gets +2/+2 until end of turn."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(2, 2, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "198"
        artist = "Eric Deschamps"
        flavorText = "\"Nature can't be stopped. It rips and tears at Ravnica's tallest buildings to claim its place in the sun.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f9ebf02-56b3-492e-88fb-2e95f13f5764.jpg?1783940464"

        ruling("2012-07-01", "Yeva's Forcemage's ability is mandatory, although you can choose Yeva's Forcemage as the target.")
    }
}
