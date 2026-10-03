package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Frostpeak Yeti
 * {3}{U}
 * Snow Creature — Yeti
 * 3/3
 * {1}{S}: This creature can't be blocked this turn.
 *
 * The `{S}` in the activation cost is payable only with mana from a snow source (CR 107.4h).
 */
val FrostpeakYeti = card("Frostpeak Yeti") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Snow Creature — Yeti"
    oracleText = "{1}{S}: This creature can't be blocked this turn. ({S} can be paid with one mana from a snow source.)"
    power = 3
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{1}{S}")
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, EffectTarget.Self)
        description = "{1}{S}: This creature can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "57"
        artist = "Chris Rahn"
        flavorText = "Some heroes line their coats with yeti fur. More yetis line their bellies with heroes."
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f406ce3-dce1-42d3-b76c-8e8c4b2770cb.jpg?1783928264"
    }
}
