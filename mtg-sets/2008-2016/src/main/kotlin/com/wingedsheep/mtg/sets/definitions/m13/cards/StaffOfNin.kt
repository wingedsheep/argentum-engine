package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Staff of Nin
 * {6}
 * Artifact
 * At the beginning of your upkeep, draw a card.
 * {T}: This artifact deals 1 damage to any target.
 */
val StaffOfNin = card("Staff of Nin") {
    manaCost = "{6}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of your upkeep, draw a card.\n" +
        "{T}: This artifact deals 1 damage to any target."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Tap
        val t = target(Targets.Any)
        effect = Effects.DealDamage(1, t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "217"
        artist = "Dan Murayama Scott"
        flavorText = "\"I have attuned the staff to your screams so that only I may benefit from your pain.\"\n—Volux, disciple of Nin"
        imageUri = "https://cards.scryfall.io/normal/front/6/9/69b7381a-ec4a-4f1b-b81c-bdf9f9d64f31.jpg"
    }
}
