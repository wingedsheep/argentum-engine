package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Satyr's Cunning
 * {R}
 * Sorcery
 * Create a 1/1 red Satyr creature token with "This token can't block."
 * Escape—{2}{R}, Exile two other cards from your graveyard.
 *
 * Anax's Satyr token (a [CantBlock] static on the token) plus Glimpse of Freedom's escape keyword
 * with a two-card exile. The escaped sorcery returns to the graveyard after resolving, ready to
 * escape again (ruling).
 */
val SatyrsCunning = card("Satyr's Cunning") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Create a 1/1 red Satyr creature token with \"This token can't block.\"\n" +
        "Escape—{2}{R}, Exile two other cards from your graveyard. " +
        "(You may cast this card from your graveyard for its escape cost.)"

    spell {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Satyr"),
            staticAbilities = listOf(CantBlock()),
            imageUri = "https://cards.scryfall.io/normal/front/9/0/903e30f3-580e-4a14-989b-ae0632363407.jpg?1783931453",
        )
    }

    keywordAbility(KeywordAbility.escape("{2}{R}", Costs.additional.ExileOtherCards(2)))

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "152"
        artist = "Deruchenko Alexander"
        flavorText = "It's hard to keep a satyr down, even in the Underworld."
        imageUri = "https://cards.scryfall.io/normal/front/6/4/646d8201-be68-42fa-bcfd-42d81a2ad195.jpg?1783931547"
        ruling(
            "2020-01-24",
            "After an escaped spell resolves, it returns to its owner's graveyard if it's not a permanent " +
                "spell. If it is a permanent spell, it enters the battlefield and will return to its owner's " +
                "graveyard if it dies later. Perhaps it will escape again—good underworld security is so hard " +
                "to come by these days."
        )
    }
}
