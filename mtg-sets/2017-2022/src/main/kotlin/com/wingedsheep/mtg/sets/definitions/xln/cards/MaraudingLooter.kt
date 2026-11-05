package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Marauding Looter
 * {2}{U}{R}
 * Creature — Human Pirate
 * 4/3
 * Raid — At the beginning of your end step, if you attacked this turn, you may draw a card. If you
 * do, discard a card.
 *
 * The end-step twin of [ShipwreckLooter]: raid is the intervening-if
 * [Conditions.YouAttackedThisTurn], and the optional loot is [Effects.May] over
 * `Patterns.Hand.loot()`, so declining draws nothing and discards nothing.
 */
val MaraudingLooter = card("Marauding Looter") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Creature — Human Pirate"
    power = 4
    toughness = 3
    oracleText = "Raid — At the beginning of your end step, if you attacked this turn, you may draw a card. " +
        "If you do, discard a card."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouAttackedThisTurn
        effect = Effects.May(Patterns.Hand.loot())
        description = "Raid — At the beginning of your end step, if you attacked this turn, you may draw a card. " +
            "If you do, discard a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "225"
        artist = "Jason A. Engle"
        flavorText = "\"I'll be taking that.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c41fee8-7b03-4dc8-babb-f8c8369a74c2.jpg?1783935711"
    }
}
