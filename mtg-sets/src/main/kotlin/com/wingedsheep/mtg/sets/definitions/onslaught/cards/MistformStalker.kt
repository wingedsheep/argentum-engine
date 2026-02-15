package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.BecomeCreatureTypeEffect
import com.wingedsheep.sdk.scripting.EffectTarget

/**
 * Mistform Stalker
 * {1}{U}
 * Creature — Illusion
 * 1/1
 * {1}: Mistform Stalker becomes the creature type of your choice until end of turn.
 * {2}{U}{U}: Mistform Stalker gets +2/+2 and gains flying until end of turn.
 */
val MistformStalker = card("Mistform Stalker") {
    manaCost = "{1}{U}"
    typeLine = "Creature — Illusion"
    power = 1
    toughness = 1
    oracleText = "{1}: Mistform Stalker becomes the creature type of your choice until end of turn.\n{2}{U}{U}: Mistform Stalker gets +2/+2 and gains flying until end of turn."

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = BecomeCreatureTypeEffect(
            target = EffectTarget.Self
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{U}{U}")
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
            .then(Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "98"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/large/front/9/e/9e80d109-b73f-4b5d-b9e4-534e8d69633f.jpg?1562932494"
    }
}
