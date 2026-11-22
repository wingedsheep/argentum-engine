package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cartel Aristocrat
 * {W}{B}
 * Creature — Human Advisor
 * 2/2
 * Sacrifice another creature: This creature gains protection from the color of your choice until
 * end of turn.
 *
 * The color is chosen on resolution (Mother of Runes' shape), granted to the Aristocrat itself.
 */
val CartelAristocrat = card("Cartel Aristocrat") {
    manaCost = "{W}{B}"
    colorIdentity = "WB"
    typeLine = "Creature — Human Advisor"
    oracleText = "Sacrifice another creature: This creature gains protection from the color of your choice until end of turn."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.SacrificeAnother(GameObjectFilter.Creature)
        effect = Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "150"
        artist = "James Ryman"
        flavorText = "\"Your life serves a higher purpose now: ensuring the continuation of my own.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25bcfbc0-1401-4e5e-8145-c8936c4ff725.jpg"
    }
}
