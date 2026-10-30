package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Cling to Dust
 * {B}
 * Instant
 * Exile target card from a graveyard. If it was a creature card, you gain 3 life. Otherwise, you draw
 * a card.
 * Escape—{3}{B}, Exile five other cards from your graveyard.
 *
 * Scavenging Ooze's "exile, then check the exiled card's printed type" ([Conditions.TargetIsCreatureCard]
 * reads the card in exile), with the draw as the `otherwise` branch — an enchantment creature card
 * gains life only (ruling). An illegal target fizzles the whole spell, so neither branch runs. Escape
 * on a non-permanent spell sends it back to the graveyard after it resolves (CR 702.138), ready to
 * escape again.
 */
val ClingToDust = card("Cling to Dust") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Exile target card from a graveyard. If it was a creature card, you gain 3 life. " +
        "Otherwise, you draw a card.\n" +
        "Escape—{3}{B}, Exile five other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    spell {
        val exiled = target(TargetFilter.CardInGraveyard)
        effect = Effects.Exile(exiled) then
            Effects.If(
                condition = Conditions.TargetIsCreatureCard(0),
                then = Effects.GainLife(3),
                otherwise = Effects.DrawCards(1),
            )
    }

    keywordAbility(KeywordAbility.escape("{3}{B}", Costs.additional.ExileOtherCards(5)))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "87"
        artist = "Caio Monteiro"
        flavorText = "In bleak Tizerus, all hope crumbles in your grasp."
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52c2de5f-e486-4cfe-9fb6-be0078ce5f93.jpg?1783931570"
        ruling(
            "2020-01-24",
            "If the target card is an illegal target by the time Cling to Dust tries to resolve, the spell " +
                "won't resolve. You won't gain 3 life or draw a card."
        )
        ruling(
            "2020-01-24",
            "If the exiled card is a creature card that's also another card type (such as an enchantment " +
                "creature), you gain 3 life. You don't also draw a card."
        )
        ruling(
            "2020-01-24",
            "Cling to Dust can target one of the five cards you'll exile to pay its escape cost. In this " +
                "case, the spell won't resolve."
        )
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
    }
}
