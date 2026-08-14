package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kraven's Cats
 * {1}{G}
 * Creature — Cat Villain, 2/2
 * {2}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn.
 */
val KravensCats = card("Kraven's Cats") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cat Villain"
    power = 2
    toughness = 2
    oracleText = "{2}{G}: This creature gets +2/+2 until end of turn. Activate only once each turn."

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Kevin Glint"
        flavorText = "\"This time the hunter shall get his prey—with the aid of my two little pets!\"\n—Kraven the Hunter"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86c415d8-1d2d-4339-955b-0f2aebeb3c95.jpg?1757377449"
    }
}
