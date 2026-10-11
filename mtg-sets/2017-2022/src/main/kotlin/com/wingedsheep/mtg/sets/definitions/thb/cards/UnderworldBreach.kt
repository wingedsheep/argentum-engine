package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GraveyardCardsHaveEscape
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect

/**
 * Underworld Breach
 * {1}{R}
 * Enchantment
 * Each nonland card in your graveyard has escape. The escape cost is equal to the card's mana cost
 * plus exile three other cards from your graveyard.
 * At the beginning of the end step, sacrifice this enchantment.
 *
 * The escape grant is a [GraveyardCardsHaveEscape] group grant: a printed escape behaves the same,
 * so an escaped instant or sorcery goes back to the graveyard and can escape again.
 */
val UnderworldBreach = card("Underworld Breach") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Each nonland card in your graveyard has escape. The escape cost is equal to the " +
        "card's mana cost plus exile three other cards from your graveyard. (You may cast cards " +
        "from your graveyard for their escape cost.)\n" +
        "At the beginning of the end step, sacrifice this enchantment."

    staticAbility {
        ability = GraveyardCardsHaveEscape(
            filter = GameObjectFilter.Nonland,
            additionalCost = Costs.additional.ExileOtherCards(3),
        )
    }

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        effect = SacrificeSelfEffect
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "161"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e51d796-7279-4c06-87f0-37adbdaa41df.jpg?1783931543"

        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so " +
                "hard to come by these days."
        )
        ruling("2020-01-24", "Escape's permission doesn't change when you may cast the spell from your graveyard.")
        ruling("2020-01-24", "If a card has no mana cost, its escape cost is an unpayable cost, so you can't cast it for that cost.")
    }
}
