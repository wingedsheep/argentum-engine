package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Glintwing Invoker
 * {4}{U}
 * Creature — Human Wizard Mutant
 * 3/3
 * {7}{U}: Glintwing Invoker gets +3/+3 and gains flying until end of turn.
 */
val GlintwingInvoker = card("Glintwing Invoker") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Human Wizard Mutant"
    power = 3
    toughness = 3
    oracleText = "{7}{U}: This creature gets +3/+3 and gains flying until end of turn."

    activatedAbility {
        cost = Costs.Mana("{7}{U}")
        effect = Effects.ModifyStats(3, 3, EffectTarget.Self)
            .then(Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "40"
        artist = "Jim Nelson"
        flavorText = "The Mirari flares in his mind."
        imageUri = "https://cards.scryfall.io/normal/front/1/6/16184709-f370-40cc-91f2-849a44ac451a.jpg?1562899418"
    }
}
