package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.PreventNextDamageEffect
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Aven Redeemer
 * {3}{W}
 * Creature — Bird Cleric
 * 2/2
 * Flying
 * {T}: Prevent the next 2 damage that would be dealt to any target this turn.
 */
val AvenRedeemer = card("Aven Redeemer") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Bird Cleric"
    power = 2
    toughness = 2
    oracleText = "Flying\n{T}: Prevent the next 2 damage that would be dealt to any target this turn."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Tap
        val t = target("target", AnyTarget())
        effect = PreventNextDamageEffect(
            amount = DynamicAmount.Fixed(2),
            target = t
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Tim Hildebrandt"
        flavorText = "Redeemers rise with the sun, and the spirits of the people rise with them."
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a2fa0a3-e40f-49e4-a4fd-427e7e808afd.jpg?1562922946"
    }
}
