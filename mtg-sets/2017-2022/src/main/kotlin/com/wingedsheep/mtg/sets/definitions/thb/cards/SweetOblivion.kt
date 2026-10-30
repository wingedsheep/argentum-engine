package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Sweet Oblivion
 * {1}{U}
 * Sorcery
 * Target player mills four cards.
 * Escape—{3}{U}, Exile four other cards from your graveyard.
 *
 * Dampen Thought's targeted [Patterns.Library.mill] plus Sleep of the Dead's escape, here with a
 * four-card exile. The escaped sorcery returns to the graveyard after resolving (ruling).
 */
val SweetOblivion = card("Sweet Oblivion") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target player mills four cards.\n" +
        "Escape—{3}{U}, Exile four other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    spell {
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(4, player)
    }

    keywordAbility(KeywordAbility.escape("{3}{U}", Costs.additional.ExileOtherCards(4)))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Clint Cearley"
        flavorText = "When memory is torment, oblivion is bliss."
        imageUri = "https://cards.scryfall.io/normal/front/5/1/51ac77d4-f918-4bbc-b023-8117a8c401ee.jpg?1783931576"
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
    }
}
