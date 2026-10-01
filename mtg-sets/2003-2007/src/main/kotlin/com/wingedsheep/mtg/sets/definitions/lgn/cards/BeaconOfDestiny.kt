package com.wingedsheep.mtg.sets.definitions.lgn.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Beacon of Destiny
 * {1}{W}
 * Creature — Human Cleric
 * 1/3
 * {T}: The next time a source of your choice would deal damage to you this turn,
 * that damage is dealt to this creature instead.
 */
val BeaconOfDestiny = card("Beacon of Destiny") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 3
    oracleText = "{T}: The next time a source of your choice would deal damage to you this turn, that damage is dealt to this creature instead."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.RedirectDamageFromChosenSource(
            protectedTarget = EffectTarget.Controller,
            redirectTo = EffectTarget.Self
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "5"
        artist = "Tim Hildebrandt"
        flavorText = "\"We all borrowed life from the Ancestor to exist in this world. Today, I repay that debt.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30b1cad7-4e96-4ebe-8c99-4ed9217becf3.jpg?1562904924"
    }
}
