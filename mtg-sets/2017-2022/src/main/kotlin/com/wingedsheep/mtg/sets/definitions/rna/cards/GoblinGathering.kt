package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Goblin Gathering
 * {2}{R}
 * Sorcery
 * Create a number of 1/1 red Goblin creature tokens equal to two plus the number of cards named
 * Goblin Gathering in your graveyard.
 *
 * The count is taken on resolution, while this spell is still on the stack, so it never counts
 * itself — the same `2 + count(NameEquals)` shape as Ancestral Anger.
 */
val GoblinGathering = card("Goblin Gathering") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create a number of 1/1 red Goblin creature tokens equal to two plus the number of " +
        "cards named Goblin Gathering in your graveyard."

    spell {
        effect = Effects.CreateToken(
            count = 2 + DynamicAmounts.count(
                Player.You,
                Zone.GRAVEYARD,
                GameObjectFilter(cardPredicates = listOf(CardPredicate.NameEquals("Goblin Gathering"))),
            ),
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Goblin"),
            imageUri = "https://cards.scryfall.io/normal/front/9/9/993613a6-09a9-45a5-b35f-5a5fdb6d14ec.jpg?1783933603",
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "104"
        artist = "Svetlin Velinov"
        flavorText = "Two's a party. Three's a felony."
        imageUri = "https://cards.scryfall.io/normal/front/1/4/147bef05-4497-44d5-9dd6-fb5dc08e78f7.jpg?1783933679"
    }
}
