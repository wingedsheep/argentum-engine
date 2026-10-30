package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Escape Velocity
 * {R}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets +1/+0 and has haste.
 * Escape—{1}{R}, Exile two other cards from your graveyard.
 *
 * Hammerhand's static pair (+1/+0 and haste on the enchanted creature) with Cling to Dust's escape.
 * Escaped from the graveyard it is an Aura spell like any other: it targets a creature as it is
 * cast, and after it dies it goes back to the graveyard, ready to escape again.
 */
val EscapeVelocity = card("Escape Velocity") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets +1/+0 and has haste.\n" +
        "Escape—{1}{R}, Exile two other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(+1, 0)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.HASTE)
    }

    keywordAbility(KeywordAbility.escape("{1}{R}", Costs.additional.ExileOtherCards(2)))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Mathias Kollros"
        flavorText = "\"Not even death can catch me now!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90c2a3da-2458-4921-8574-9eb9d5925ae8.jpg?1783931554"
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
    }
}
