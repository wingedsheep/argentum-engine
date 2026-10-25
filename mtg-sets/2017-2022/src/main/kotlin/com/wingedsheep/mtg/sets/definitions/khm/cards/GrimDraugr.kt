package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grim Draugr
 * {2}{B}
 * Snow Creature — Zombie Berserker
 * 3/2
 * {1}{S}: This creature gets +1/+0 and gains menace until end of turn.
 */
val GrimDraugr = card("Grim Draugr") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Snow Creature — Zombie Berserker"
    oracleText = "{1}{S}: This creature gets +1/+0 and gains menace until end of turn. (It can't be " +
        "blocked except by two or more creatures. {S} can be paid with one mana from a snow source.)"
    power = 3
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{1}{S}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.MENACE, EffectTarget.Self)
        description = "{1}{S}: This creature gets +1/+0 and gains menace until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "96"
        artist = "Grzegorz Rutkowski"
        flavorText = "She was a ragged shadow of her living form, but she had lost none of her deadly prowess."
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54340c59-03a9-4d9d-bd27-9aed337ce75a.jpg?1783928247"
    }
}
