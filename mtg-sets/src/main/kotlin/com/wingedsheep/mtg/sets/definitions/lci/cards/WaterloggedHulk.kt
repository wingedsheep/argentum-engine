package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.craft
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Waterlogged Hulk // Watertight Gondola (CR 702.167, The Lost Caverns of Ixalan)
 * {U}
 * Artifact // Artifact — Vehicle
 *
 * Front face — Waterlogged Hulk ({U}, Artifact)
 *   {T}: Mill a card.
 *   Craft with Island {3}{U} ({3}{U}, Exile this artifact, Exile an Island you
 *   control or an Island card from your graveyard: Return this card transformed
 *   under its owner's control. Craft only as a sorcery.)
 *
 * Back face — Watertight Gondola (Artifact — Vehicle, 4/4)
 *   Vigilance
 *   Descend 8 — This Vehicle can't be blocked as long as there are eight or more
 *   permanent cards in your graveyard.
 *   Crew 1
 *
 * Implementation:
 *  - The front face's tap ability is [Patterns.Library.mill] (Gather → Move mill
 *    pipeline) with [Costs.Tap].
 *  - The `craft(...)` helper wires the activated ability: a
 *    [com.wingedsheep.sdk.scripting.AbilityCost.Craft] with an exactly-one Island
 *    material filter (`minCount = maxCount = 1`; an Island you control or an Island
 *    card in your graveyard, CR 702.167a-b) plus the {3}{U} mana cost, resolving via
 *    [com.wingedsheep.sdk.scripting.effects.ReturnSelfFromExileTransformedEffect]
 *    (return transformed as the back face). Sorcery-only timing is enforced by the
 *    helper.
 *  - Descend 8 is an ability word: a [ConditionalStaticAbility] wrapping
 *    [CantBeBlocked] on the source, gated on
 *    [Conditions.CardsInGraveyardMatchingAtLeast] (8, [GameObjectFilter.Permanent])
 *    — the Nightwhorl Hermit / The Ancient One idiom.
 *  - Vigilance is a plain keyword; Crew 1 is [KeywordAbility.crew].
 */

private val IslandFilter: GameObjectFilter = GameObjectFilter.Any.withSubtype(Subtype.ISLAND)

private val WaterloggedHulkFront = card("Waterlogged Hulk") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{T}: Mill a card. (Put the top card of your library into your graveyard.)\n" +
        "Craft with Island {3}{U} ({3}{U}, Exile this artifact, Exile an Island you control or an Island card from your graveyard: Return this card transformed under its owner's control. Craft only as a sorcery.)"

    // {T}: Mill a card.
    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Library.mill(1)
    }

    craft(filter = IslandFilter, cost = "{3}{U}", materialDescription = "Island", minCount = 1, maxCount = 1)

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Artur Treffner"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7ea89d3c-8d47-4bbb-9271-8cdfd9296e2f.jpg?1782694543"
    }
}

private val WatertightGondola = card("Watertight Gondola") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 4
    oracleText = "Vigilance\n" +
        "Descend 8 — This Vehicle can't be blocked as long as there are eight or more permanent cards in your graveyard.\n" +
        "Crew 1 (Tap any number of creatures you control with total power 1 or more: This Vehicle becomes an artifact creature until end of turn.)"

    keywords(Keyword.VIGILANCE)

    // Descend 8 — can't be blocked as long as there are eight or more permanent
    // cards in your graveyard.
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeBlocked(),
            condition = Conditions.CardsInGraveyardMatchingAtLeast(8, GameObjectFilter.Permanent)
        )
    }

    keywordAbility(KeywordAbility.crew(1))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Artur Treffner"
        imageUri = "https://cards.scryfall.io/normal/back/7/e/7ea89d3c-8d47-4bbb-9271-8cdfd9296e2f.jpg?1782694543"
    }
}

val WaterloggedHulk: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = WaterloggedHulkFront,
    backFace = WatertightGondola
)
