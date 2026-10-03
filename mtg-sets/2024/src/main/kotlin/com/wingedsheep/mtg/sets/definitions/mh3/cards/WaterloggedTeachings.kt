package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Waterlogged Teachings {3}{U/B} // Inundated Archive — Modern Horizons 3 #261 (uncommon)
 * Instant
 * Search your library for an instant card or a card with flash, reveal it, put it into your hand,
 * then shuffle.
 * //
 * Land
 * This land enters tapped.
 * {T}: Add {U} or {B}.
 */
private val WaterloggedTeachingsFront = card("Waterlogged Teachings") {
    manaCost = "{3}{U/B}"
    colorIdentity = "UB"
    typeLine = "Instant"
    oracleText = "Search your library for an instant card or a card with flash, reveal it, put it into your hand, then shuffle."

    spell {
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Instant or GameObjectFilter.Any.withKeyword(Keyword.FLASH),
            count = 1,
            destination = SearchDestination.HAND,
            shuffleAfter = true,
            reveal = true,
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "261"
        artist = "Douglas Shuler"
        flavorText = "The long-dead librarians are still on hand to recommend selected titles."
        imageUri = "https://cards.scryfall.io/normal/front/0/6/060f9675-4921-4cbb-bae2-54c85c679fd4.jpg?1783911223"
    }
}

private val InundatedArchiveBack = card("Inundated Archive") {
    typeLine = "Land"
    colorIdentity = "UB"
    oracleText = "This land enters tapped.\n{T}: Add {U} or {B}."

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "261"
        artist = "Douglas Shuler"
        flavorText = "Moldering tomes line rotting shelves. Generations of priceless knowledge fade from memory beneath the waves."
        imageUri = "https://cards.scryfall.io/normal/back/0/6/060f9675-4921-4cbb-bae2-54c85c679fd4.jpg?1783911223"
    }
}

val WaterloggedTeachings: CardDefinition = CardDefinition.modalDoubleFacedLand(
    frontFace = WaterloggedTeachingsFront,
    backFace = InundatedArchiveBack,
)
