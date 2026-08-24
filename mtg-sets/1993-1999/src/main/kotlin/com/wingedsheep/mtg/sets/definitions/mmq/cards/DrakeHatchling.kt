package com.wingedsheep.mtg.sets.definitions.mmq.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Drake Hatchling
 * {2}{U}
 * Creature — Drake
 * 1 / 3
 * Flying
 * {U}: This creature gets +1/+0 until end of turn. Activate only once each turn.
 *
 * "Activate only once each turn" is [ActivationRestriction.OncePerTurn] — a per-turn activation
 * cap, not a timing rule.
 */
val DrakeHatchling = card("Drake Hatchling") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Drake"
    oracleText = "Flying\n" +
        "{U}: This creature gets +1/+0 until end of turn. Activate only once each turn."
    power = 1
    toughness = 3
    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Bradley Williams"
        flavorText = "There is beauty in the space between learning to fly and taking it for granted."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64ee32f9-6120-4f15-a692-89a4cd8167c6.jpg"
    }
}
