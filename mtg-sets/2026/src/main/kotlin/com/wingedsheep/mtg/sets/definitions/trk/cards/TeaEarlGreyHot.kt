package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity

/**
 * Tea, Earl Grey, Hot
 * {2}
 * Artifact — Food
 *
 * When this artifact enters, draw a card.
 * {2}, {T}, Sacrifice this artifact: Choose one —
 * • This artifact deals 2 damage to target opponent.
 * • You gain 3 life.
 */
val TeaEarlGreyHot = card("Tea, Earl Grey, Hot") {
    manaCost = "{2}"
    typeLine = "Artifact — Food"
    oracleText = "When this artifact enters, draw a card.\n{2}, {T}, Sacrifice this artifact: Choose one —\n• This artifact deals 2 damage to target opponent.\n• You gain 3 life."
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.Modal(
            modes = listOf(
                mode("Deal 2 damage to target opponent") {
                    val opponent = target(Targets.Opponent)
                    effect = Effects.DealDamage(2, opponent)
                },
                mode("You gain 3 life") {
                    effect = Effects.GainLife(3)
                }
            )
        )
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "269"
        artist = "Raymond Bonilla"
        flavorText = "Lapsang souchong for smoky body. Bergamot for sharpness. An insightful captain's cup."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/289e8ebe-ab0f-466b-a4e6-d5cf8411e724.jpg?1792591098"
    }
}
