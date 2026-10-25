package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hailstorm Valkyrie
 * {3}{B}
 * Snow Creature — Angel Wizard
 * 2/2
 * Flying, trample
 * {S}{S}: This creature gets +2/+2 until end of turn.
 */
val HailstormValkyrie = card("Hailstorm Valkyrie") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Snow Creature — Angel Wizard"
    oracleText = "Flying, trample\n{S}{S}: This creature gets +2/+2 until end of turn. ({S} can be " +
        "paid with one mana from a snow source.)"
    power = 2
    toughness = 2

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.Mana("{S}{S}")
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        description = "{S}{S}: This creature gets +2/+2 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Bram Sels"
        flavorText = "When the path to Starnheim was opened, vengeance rained down upon the enemies of Bretagard."
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a5a272b3-aaab-4c98-86a8-85c67a5f3d4d.jpg?1783928246"
    }
}
