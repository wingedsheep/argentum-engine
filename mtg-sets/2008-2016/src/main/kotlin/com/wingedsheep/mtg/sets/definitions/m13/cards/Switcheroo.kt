package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Switcheroo
 * {4}{U}
 * Sorcery
 * Exchange control of two target creatures.
 *
 * Same atom as Shifting Grift's creature mode: [Effects.ExchangeControl] over two independent
 * creature targets (any controller). The exchange is a permanent control override and doesn't
 * happen if either target is illegal on resolution.
 */
val Switcheroo = card("Switcheroo") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Exchange control of two target creatures."

    spell {
        val firstCreature = target(TargetFilter.Creature)
        val secondCreature = target(TargetFilter.Creature)
        effect = Effects.ExchangeControl(firstCreature, secondCreature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Kev Walker"
        flavorText = "Say hello to your new friend."
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d62aaf3-0fd4-44ba-8eeb-18ac759dfe84.jpg?1783940501"

        ruling("2018-07-13", "If one of the target creatures is an illegal target when Switcheroo resolves, the exchange won't happen.")
        ruling("2018-07-13", "You don't have to control either target.")
        ruling("2018-07-13", "If the same player controls both creatures when Switcheroo resolves, nothing happens.")
        ruling("2018-07-13", "Gaining control of a creature doesn't cause you to gain control of any Auras or Equipment attached to it.")
    }
}
