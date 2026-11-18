package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bearer of the Heavens
 * {7}{R}
 * Creature — Giant
 * 10/10
 *
 * When this creature dies, destroy all permanents at the beginning of the next end step.
 *
 * The dies trigger installs a delayed trigger for the next end step; the board wipe itself happens
 * when that delayed trigger resolves, so permanents that arrive in between are destroyed too.
 */
val BearerOfTheHeavens = card("Bearer of the Heavens") {
    manaCost = "{7}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Giant"
    power = 10
    toughness = 10
    oracleText = "When this creature dies, destroy all permanents at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateDelayedTrigger(
            step = Step.END,
            effect = Effects.DestroyAll(GameObjectFilter.Permanent),
        )
        description = "When this creature dies, destroy all permanents at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Ryan Alexander Lee"
        flavorText = "To hold the heavens from the earth is no curse, but a titanic responsibility."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/061d9fa5-a8e0-4263-9804-22d648554eba.jpg?1783939428"
    }
}
