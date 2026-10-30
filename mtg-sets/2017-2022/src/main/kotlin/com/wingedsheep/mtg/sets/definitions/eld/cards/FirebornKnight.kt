package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fireborn Knight
 * {R/W}{R/W}{R/W}{R/W}
 * Creature — Human Knight
 * 2/3
 *
 * Double strike
 * {R/W}{R/W}{R/W}{R/W}: This creature gets +1/+1 until end of turn.
 */
val FirebornKnight = card("Fireborn Knight") {
    manaCost = "{R/W}{R/W}{R/W}{R/W}"
    colorIdentity = "RW"
    typeLine = "Creature — Human Knight"
    oracleText = "Double strike\n" +
        "{R/W}{R/W}{R/W}{R/W}: This creature gets +1/+1 until end of turn."
    power = 2
    toughness = 3

    keywords(Keyword.DOUBLE_STRIKE)

    activatedAbility {
        cost = Costs.Mana("{R/W}{R/W}{R/W}{R/W}")
        effect = Effects.ModifyStats(1, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "210"
        artist = "Ben Maier"
        flavorText = "He endured the white-hot blaze of the Circle and the sweltering heat of the Irencrag, and he emerged victorious."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/6716f46c-806d-4b3e-8a1c-fd6dcedacf8e.jpg?1783932590"
    }
}
