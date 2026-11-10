package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Havoc Sower
 * {3}{B}
 * Creature — Eldrazi Drone
 * 3/3
 * Devoid (This card has no color.)
 * {1}{C}: This creature gets +2/+1 until end of turn. ({C} represents colorless mana.)
 */
val HavocSower = card("Havoc Sower") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi Drone"
    power = 3
    toughness = 3
    oracleText = "Devoid (This card has no color.)\n" +
        "{1}{C}: This creature gets +2/+1 until end of turn. ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Mana("{1}{C}")
        effect = Effects.ModifyStats(2, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Svetlin Velinov"
        flavorText = "It grew with a terrible swiftness."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8585871-24a2-41d7-ae12-29aabfbc9ccc.jpg"
    }
}
