package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Axis of Mortality
 * {4}{W}{W}
 * Enchantment
 * At the beginning of your upkeep, you may have two target players exchange life totals.
 *
 * "Two target players" is one count-2 requirement, so the two chosen players are distinct; the
 * exchange runs between them (positional targets 0 and 1) and leaves the controller's own total
 * alone unless the controller is one of them.
 */
val AxisOfMortality = card("Axis of Mortality") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your upkeep, you may have two target players exchange life totals."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        val (firstPlayer, secondPlayer) = targets(TargetPlayer(count = 2))
        effect = Effects.May(Effects.ExchangeLifeTotals(target = secondPlayer, player = firstPlayer))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "3"
        artist = "Bastien L. Deharme"
        flavorText = "The Church of Dusk teaches there is no gain without loss, no triumph without sacrifice, " +
            "no drink of exquisite crimson without a dying gasp."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b9d72a8-8acf-42c6-8adf-cbaecb4a985a.jpg?1783935805"
    }
}
