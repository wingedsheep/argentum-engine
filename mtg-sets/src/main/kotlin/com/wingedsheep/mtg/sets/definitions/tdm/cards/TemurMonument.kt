package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Temur Monument
 * {2}
 * Artifact
 *
 * When this artifact enters, search your library for a basic Forest, Island, or Mountain card,
 * reveal it, put it into your hand, then shuffle.
 * {3}{G}{U}{R}, {T}, Sacrifice this artifact: Create a 5/5 green Elephant creature token.
 * Activate only as a sorcery.
 *
 * The ETB search is a non-optional library search to hand (oracle text has no "you may"); modeled
 * via [LibraryPatterns.searchLibrary] with the basic-land subtype restriction. The token-making
 * ability combines a mana cost, tap, and self-sacrifice, and is sorcery-speed-only.
 */
val TemurMonument = card("Temur Monument") {
    manaCost = "{2}"
    colorIdentity = "GUR"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, search your library for a basic Forest, Island, or " +
        "Mountain card, reveal it, put it into your hand, then shuffle.\n" +
        "{3}{G}{U}{R}, {T}, Sacrifice this artifact: Create a 5/5 green Elephant creature token. " +
        "Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand.withAnyOfSubtypes(
                listOf(Subtype.FOREST, Subtype.ISLAND, Subtype.MOUNTAIN)
            ),
            count = 1,
            destination = SearchDestination.HAND,
            shuffleAfter = true,
            reveal = true
        )
        description = "search your library for a basic Forest, Island, or Mountain card, reveal it, " +
            "put it into your hand, then shuffle."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{G}{U}{R}"), Costs.Tap, Costs.SacrificeSelf)
        timing = TimingRule.SorcerySpeed
        effect = Effects.CreateToken(
            power = 5,
            toughness = 5,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elephant"),
            count = 1,
            imageUri = "https://cards.scryfall.io/normal/front/2/4/243bcfa9-0310-4d68-9864-df46069906fa.jpg?1743176747"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "248"
        artist = "Sam Burley"
        imageUri = "https://cards.scryfall.io/normal/front/5/5/55e97b40-d898-4da5-8159-cca48eb298eb.jpg?1743204983"
    }
}
