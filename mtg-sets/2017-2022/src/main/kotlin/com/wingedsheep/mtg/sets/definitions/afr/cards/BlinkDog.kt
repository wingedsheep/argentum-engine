package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Blink Dog
 * {2}{W}
 * Creature — Dog
 * 1/1
 * Double strike
 * Teleport — {3}{W}: This creature phases out. (Treat it and anything attached to it as though
 * they don't exist until your next turn.)
 */
val BlinkDog = card("Blink Dog") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dog"
    oracleText = "Double strike\nTeleport — {3}{W}: This creature phases out. (Treat it and anything attached to it as though they don't exist until your next turn.)"
    power = 1
    toughness = 1

    keywords(Keyword.DOUBLE_STRIKE)

    activatedAbility {
        cost = Costs.Mana("{3}{W}")
        effect = Effects.PhaseOut(EffectTarget.Self)
        description = "Teleport — {3}{W}: This creature phases out."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "3"
        artist = "Oriana Menendez"
        flavorText = "Blink dogs protect the wild places of the world, driving away evil influences."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc040492-7fb7-4fdb-aafe-8179e5f2b28c.jpg?1783926539"
    }
}
