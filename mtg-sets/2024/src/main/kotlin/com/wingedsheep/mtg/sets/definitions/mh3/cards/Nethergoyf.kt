package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nethergoyf
 * {B}
 * Creature — Lhurgoyf
 * * / 1+*
 * Nethergoyf's power is equal to the number of card types among cards in your graveyard and its
 * toughness is equal to that number plus 1.
 * Escape—{2}{B}, Exile any number of other cards from your graveyard with four or more card types
 * among them.
 *
 * The escape cost is a union, not a count: the exiled cards must show four card types *between
 * them* (an artifact creature is two), which is `Costs.additional.ExileOtherCardsWithCardTypes(4)`.
 */
val Nethergoyf = card("Nethergoyf") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Lhurgoyf"
    dynamicStats(
        DynamicAmounts.zone(Player.You, Zone.GRAVEYARD).distinctTypes(),
        toughnessOffset = 1,
    )
    oracleText = "Nethergoyf's power is equal to the number of card types among cards in your graveyard " +
        "and its toughness is equal to that number plus 1.\n" +
        "Escape—{2}{B}, Exile any number of other cards from your graveyard with four or more card " +
        "types among them. (You may cast this card from your graveyard for its escape cost.)"

    keywordAbility(KeywordAbility.escape("{2}{B}", Costs.additional.ExileOtherCardsWithCardTypes(4)))

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "103"
        artist = "Xavier Ribeiro"
        imageUri = "https://cards.scryfall.io/normal/front/3/e/3ee3945e-5089-4751-b7b3-5961c39d2a33.jpg?1783911277"

        ruling(
            "2024-06-07",
            "The ability that defines Nethergoyf's power and toughness works in all zones, not just the battlefield."
        )
        ruling(
            "2024-06-07",
            "Nethergoyf's first ability counts card types, not cards. If the only card in your graveyard is a " +
                "single artifact creature card, Nethergoyf will be a 2/3. If your graveyard consists of ten " +
                "artifact cards and ten creature cards, Nethergoyf will still be a 2/3."
        )
        ruling(
            "2024-06-07",
            "Card types that can appear on cards in a graveyard are artifact, battle, creature, enchantment, " +
                "instant, kindred, land, planeswalker, and sorcery. Legendary, basic, and snow are supertypes, " +
                "not card types; Lhurgoyf, Forest, and Siege are subtypes, not card types."
        )
        ruling(
            "2024-06-07",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. It can escape again."
        )
    }
}
