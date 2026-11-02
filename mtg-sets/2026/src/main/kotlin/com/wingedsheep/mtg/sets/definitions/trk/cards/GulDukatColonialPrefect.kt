package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Gul Dukat, Colonial Prefect
 * {4}{B}{B}{B}
 * Legendary Creature — Cardassian Rogue
 * 6/6
 *
 * Menace
 * At the beginning of each end step, each opponent loses life equal to the life that player lost
 * this turn. You gain life equal to the life lost this way. (Damage causes loss of life.)
 *
 * The amount is measured per opponent (`amountPerLoser`: `Player.You` inside it is the losing
 * player), and the gain is one event for the total actually lost.
 */
val GulDukatColonialPrefect = card("Gul Dukat, Colonial Prefect") {
    manaCost = "{4}{B}{B}{B}"
    typeLine = "Legendary Creature — Cardassian Rogue"
    colorIdentity = "B"
    oracleText = "Menace\nAt the beginning of each end step, each opponent loses life equal to the life " +
        "that player lost this turn. You gain life equal to the life lost this way. " +
        "(Damage causes loss of life.)"
    power = 6
    toughness = 6
    keywords(Keyword.MENACE)
    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = Effects.DrainLife(
            amount = DynamicAmounts.lifeLostThisTurn(),
            amountPerLoser = true
        )
    }
    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "113"
        artist = "Joel Chaim Holtzman"
        flavorText = "\"I'm well aware that there is a . . . perception among you . . . that we are cruel.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7adc6373-1196-448f-8aee-cee7a890c724.jpg?1793359426"
    }
}
